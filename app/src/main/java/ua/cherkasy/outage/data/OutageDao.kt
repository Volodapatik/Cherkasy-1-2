package ua.cherkasy.outage.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OutageDao {

    @Query("SELECT * FROM schedules ORDER BY publishedAt DESC")
    fun getAll(): Flow<List<OutageEntity>>

    @Query("SELECT * FROM schedules ORDER BY publishedAt DESC LIMIT 1")
    suspend fun getLatest(): OutageEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: OutageEntity): Long

    @Query("SELECT messageId FROM schedules")
    suspend fun getAllMessageIds(): List<String>
}
