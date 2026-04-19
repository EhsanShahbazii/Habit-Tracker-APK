package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habit_logs")
data class HabitLog(
    @PrimaryKey 
    val dateString: String, // "yyyy-MM-dd" format
    val intensity: Int,     // 0 to 4 (corresponds to neon opacity states, like GitHub grid)
    val note: String = ""   // Optional note for the day
)
