package ua.cherkasy.outage.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class OutageRepository(context: Context) {

    private val dao = OutageDatabase.getInstance(context).outageDao()

    fun getAllSchedules(): Flow<List<OutageEntity>> = dao.getAll()

    suspend fun getLatest(): OutageEntity? = dao.getLatest()

    suspend fun checkForNewSchedules(): List<OutageEntity> {
        val parsed = try {
            TelegramParser.fetchLatestSchedules()
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }

        val existingIds = dao.getAllMessageIds().toSet()
        val newOnes = mutableListOf<OutageEntity>()

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
                if (inserted != -1L) {
                    newOnes.add(entity)
                }
            }
        }

        return newOnes
    }
}
