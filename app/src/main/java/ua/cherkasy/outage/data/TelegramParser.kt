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
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val scheduleKeywords = listOf(
        "графік", "ГПВ", "відключень", "години відсутності",
        "погодинних", "знеструмлення", "відсутності електропостачання"
    )

    // Тільки рядок 1.2: "1.2 15:00 - 17:00" або кілька інтервалів через кому
    // Зупиняється перед наступною чергою (2.1, 3.1 тощо)
    private val queueLinePattern = Pattern.compile(
        """(?i)(?:^|\n|\s)1\.2\s+((?:\d{1,2}:\d{2}\s*[-–]\s*\d{1,2}:\d{2})(?:\s*,\s*\d{1,2}:\d{2}\s*[-–]\s*\d{1,2}:\d{2})*)""",
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
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val html = response.body?.string() ?: return emptyList()
            return parseHtml(html)
        }
    }

    private fun parseHtml(html: String): List<ParsedSchedule> {
        val doc = Jsoup.parse(html)
        val messages = doc.select("div.tgme_widget_message")
        val result = mutableListOf<ParsedSchedule>()

        for (msg in messages) {
            val dataPost = msg.attr("data-post")
            if (dataPost.isBlank()) continue

            val messageId = dataPost.substringAfterLast("/")
            val textEl = msg.selectFirst("div.tgme_widget_message_text")
            val fullText = textEl?.text()?.trim() ?: continue

            val isSchedule = scheduleKeywords.any { fullText.contains(it, ignoreCase = true) }
            if (!isSchedule) continue

            val matcher = queueLinePattern.matcher(fullText)
            if (!matcher.find()) continue

            val timesRaw = matcher.group(1)?.trim() ?: continue
            val timesText = timesRaw
                .replace("–", "-")
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
                System.currentTimeMillis()
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

        return result.sortedByDescending { it.publishedAt }
    }
}
