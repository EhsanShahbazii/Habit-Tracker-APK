package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habit_logs ORDER BY dateString ASC")
    fun getAllLogs(): Flow<List<HabitLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: HabitLog)

    @Query("DELETE FROM habit_logs WHERE dateString = :dateString")
    suspend fun deleteLogByDate(dateString: String)

    @Query("DELETE FROM habit_logs")
    suspend fun clearAllLogs()
}
