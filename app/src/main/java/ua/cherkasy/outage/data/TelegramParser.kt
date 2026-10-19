package ua.cherkasy.outage.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ParsedSchedule(
    val messageId: String,
    val dateText: String,
    val timesText: String,
    val fullText: String,
    val publishedAt: Long
)

object TelegramParser {

    private const val CHANNEL_URL = "https://t.me/s/pat_cherkasyoblenergo"

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val scheduleKeywords = listOf(
        "графік", "ГПВ", "гпв", "відключень", "відключення",
        "години відсутності", "погодинн", "знеструмлення",
        "електропостачання", "обмеження"
    )

    private val queueLinePattern = Pattern.compile(
        """(?i)(?:^|[\n\r\s])1\.2\s+((?:\d{1,2}:\d{2}\s*[-–—]\s*\d{1,2}:\d{2})(?:\s*,\s*\d{1,2}:\d{2}\s*[-–—]\s*\d{1,2}:\d{2})*)""",
        Pattern.MULTILINE
    )

    private val datePatterns = listOf(
        Pattern.compile("""(?i)(?:на|оновлений графік.*?на)\s+(\d{1,2}\s+[а-яіїєґ]+)"""),
        Pattern.compile("""(?i)(\d{1,2}\s+(?:січня|лютого|березня|квітня|травня|червня|липня|серпня|вересня|жовтня|листопада|грудня))"""),
        Pattern.compile("""(?i)(сьогодні|завтра)""")
    )

    fun fetchLatestSchedules(): List<ParsedSchedule> {
        val request = Request.Builder()
            .url(CHANNEL_URL)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml")
            .header("Accept-Language", "uk-UA,uk;q=0.9,en;q=0.8")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code} при завантаженні каналу")
            }
            val html = response.body?.string()
                ?: throw IllegalStateException("Порожня відповідь каналу")
            if (html.length < 500) {
                throw IllegalStateException("Сторінка каналу занадто коротка")
            }
            return parseHtml(html)
        }
    }

    private fun parseHtml(html: String): List<ParsedSchedule> {
        val doc = Jsoup.parse(html)
        var messages = doc.select("div.tgme_widget_message")
        if (messages.isEmpty()) {
            messages = doc.select("[data-post]")
        }

        val result = mutableListOf<ParsedSchedule>()

        for (msg in messages) {
            val dataPost = msg.attr("data-post")
            if (dataPost.isBlank()) continue

            val messageId = dataPost.substringAfterLast("/")
            val textEl = msg.selectFirst("div.tgme_widget_message_text")
                ?: msg.selectFirst(".js-message_text")
            var fullText = textEl?.text()?.trim().orEmpty()
            if (fullText.isBlank()) {
                fullText = msg.text().trim()
            }
            if (fullText.isBlank()) continue

            val isSchedule = scheduleKeywords.any { fullText.contains(it, ignoreCase = true) }
            if (!isSchedule) continue

            val matcher = queueLinePattern.matcher(fullText)
            if (!matcher.find()) continue

            val timesRaw = matcher.group(1)?.trim() ?: continue
            val timesText = timesRaw
                .replace('–', '-')
                .replace('—', '-')
                .replace(Regex("\\s+"), " ")
                .trim()
            if (timesText.isBlank()) continue

            var dateText = "невідома дата"
            for (p in datePatterns) {
                val m = p.matcher(fullText)
                if (m.find()) {
                    dateText = m.group(1)?.trim() ?: dateText
                    break
                }
            }

            val timeEl = msg.selectFirst("time")
            val datetime = timeEl?.attr("datetime") ?: ""
            val publishedAt = try {
                java.time.Instant.parse(datetime).toEpochMilli()
            } catch (e: Exception) {
                messageId.toLongOrNull()?.let { it * 1000L } ?: System.currentTimeMillis()
            }

            result.add(
                ParsedSchedule(
                    messageId = messageId,
                    dateText = dateText,
                    timesText = timesText,
                    fullText = fullText,
                    publishedAt = publishedAt
                )
            )
        }

        return result.sortedWith(
            compareByDescending<ParsedSchedule> { it.messageId.toLongOrNull() ?: 0L }
                .thenByDescending { it.publishedAt }
        )
    }
}
