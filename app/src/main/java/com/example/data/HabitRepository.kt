package com.example.data

import kotlinx.coroutines.flow.Flow

class HabitRepository(private val habitDao: HabitDao) {
    val allLogs: Flow<List<HabitLog>> = habitDao.getAllLogs()

    suspend fun insertLog(log: HabitLog) {
        habitDao.insertLog(log)
    }

    suspend fun deleteLogByDate(dateString: String) {
        habitDao.deleteLogByDate(dateString)
    }

    suspend fun clearAll() {
        habitDao.clearAllLogs()
    }
}
