package ua.cherkasy.outage.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class OutageEntity(
    @PrimaryKey val messageId: String,
    val dateText: String,
    val timesText: String,
    val fullMessage: String,
    val publishedAt: Long,
    val savedAt: Long = System.currentTimeMillis()
)
