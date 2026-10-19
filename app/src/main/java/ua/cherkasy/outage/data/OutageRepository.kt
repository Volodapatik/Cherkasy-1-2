package ua.cherkasy.outage.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class OutageRepository(context: Context) {

    private val dao = OutageDatabase.getInstance(context).outageDao()

    fun getAllSchedules(): Flow<List<OutageEntity>> = dao.getAll()

    suspend fun getLatest(): OutageEntity? = dao.getLatest()

    suspend fun insertFromFcm(entity: OutageEntity) {
        dao.insert(entity)
    }

    /**
     * @return number of newly inserted schedules
     * @throws Exception on network/parse failure so UI can show it
     */
    suspend fun checkForNewSchedules(): Int {
        val parsed = TelegramParser.fetchLatestSchedules()
        if (parsed.isEmpty()) {
            // Still success HTTP-wise but nothing matched queue 1.2
            return 0
        }

        val existingIds = dao.getAllMessageIds().toSet()
        var insertedCount = 0

        for (p in parsed) {
            if (p.messageId !in existingIds) {
                val entity = OutageEntity(
                    messageId = p.messageId,
                    dateText = p.dateText,
                    timesText = p.timesText,
                    fullMessage = p.fullText,
                    publishedAt = p.publishedAt
                )
                val inserted = dao.insert(entity)
                if (inserted != -1L) insertedCount++
            }
        }

        // If everything already in DB, ensure latest is still visible (DB not empty)
        return insertedCount
    }
}
