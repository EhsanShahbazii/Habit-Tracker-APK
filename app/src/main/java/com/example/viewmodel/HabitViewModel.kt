package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HabitDatabase
import com.example.data.HabitLog
import com.example.data.HabitRepository
import com.example.data.Quote
import com.example.data.QuoteLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class HabitViewModel(application: Application) : AndroidViewModel(application) {

    private val database = HabitDatabase.getDatabase(application)
    private val repository = HabitRepository(database.habitDao())
    private val sharedPrefs: SharedPreferences = application.getSharedPreferences("habit_tracker_prefs", Context.MODE_PRIVATE)

    // Quotes List loaded from Assets
    val quotes: List<Quote> = QuoteLoader.loadQuotes(application)

    // Live Settings from SharedPreferences
    val userName = MutableStateFlow(sharedPrefs.getString("user_name", "ATHLETE") ?: "ATHLETE")
    val habitName = MutableStateFlow(sharedPrefs.getString("habit_name", "Clean & Focused") ?: "Clean & Focused")
    val selectedAccentHex = MutableStateFlow(sharedPrefs.getString("accent_hex", "#CCFF00") ?: "#CCFF00")
    val activeQuoteIndex = MutableStateFlow(sharedPrefs.getInt("active_quote_index", 0))
    val themeMode = MutableStateFlow(sharedPrefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM")

    // UI state for database logs
    val logs: StateFlow<List<HabitLog>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Pre-populate if database is empty on launch
        viewModelScope.launch {
            repository.allLogs.collect { currentLogs ->
                if (currentLogs.isEmpty()) {
                    generateMockHistory()
                }
            }
        }
    }

    // Helper to generate a realistic 60-day history so the Whoop-style grid looks stunning instantly
    private suspend fun generateMockHistory() {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        
        // Generate random activity with a high recovery rate (Whoop-style)
        for (i in 60 downTo 1) {
            val date = today.minusDays(i.toLong())
            val dateStr = date.format(formatter)
            // Leave some zero days for contrast, but mostly clean days (intensity 1 to 4)
            val intensity = when {
                i % 7 == 0 -> 0 // Off day
                i % 5 == 0 -> 1 // Minor streak
                i % 3 == 0 -> 3 // Strong focus
                else -> 4       // Ultimate streak day
            }
            repository.insertLog(HabitLog(dateString = dateStr, intensity = intensity, note = "Day $i focused."))
        }
        
        // Ensure today is logged with max intensity
        repository.insertLog(HabitLog(dateString = today.format(formatter), intensity = 4, note = "Day 61: Sovereign."))
    }

    // Streaks and Achievements computed reactively from active logs
    val dashboardMetrics: StateFlow<DashboardMetrics> = logs.combine(habitName) { logList, habit ->
        calculateMetrics(logList)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardMetrics()
    )

    private fun calculateMetrics(logList: List<HabitLog>): DashboardMetrics {
        if (logList.isEmpty()) return DashboardMetrics()

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now()
        val todayStr = today.format(formatter)
        val yesterdayStr = today.minusDays(1).format(formatter)

        // Count total clean days (intensity > 0)
        val totalCleanDays = logList.count { it.intensity > 0 }

        // Find the active logs mapped to set of dates
        val activeDates = logList.filter { it.intensity > 0 }.mapNotNull {
            try {
                LocalDate.parse(it.dateString, formatter)
            } catch (e: Exception) {
                null
            }
        }.toSet()

        // Calculate current consecutive active streak (consecutive days backwards from today or yesterday)
        var currentStreak = 0
        var checkDate = today
        if (!activeDates.contains(today) && activeDates.contains(today.minusDays(1))) {
            checkDate = today.minusDays(1)
        }

        while (activeDates.contains(checkDate)) {
            currentStreak++
            checkDate = checkDate.minusDays(1)
        }

        // Calculate max streak in history
        var maxStreak = 0
        var tempStreak = 0
        var prevDate: LocalDate? = null

        // Sort dates to calculate max sequential streak
        val sortedDates = activeDates.sorted()
        for (date in sortedDates) {
            if (prevDate == null) {
                tempStreak = 1
            } else {
                val daysBetween = ChronoUnit.DAYS.between(prevDate, date)
                if (daysBetween == 1L) {
                    tempStreak++
                } else if (daysBetween > 1L) {
                    if (tempStreak > maxStreak) maxStreak = tempStreak
                    tempStreak = 1
                }
            }
            prevDate = date
        }
        if (tempStreak > maxStreak) maxStreak = tempStreak

        // Calculate XP
        val totalXp = logList.sumOf { it.intensity * 45 }
        val level = 1 + (totalXp / 1000)
        val xpProgress = (totalXp % 1000) / 1000f

        return DashboardMetrics(
            totalDaysClean = totalCleanDays,
            currentStreak = currentStreak,
            maxStreak = maxStreak,
            totalXp = totalXp,
            level = level,
            xpProgress = xpProgress
        )
    }

    // Methods to interact and mutate database logs
    fun checkIn(intensity: Int, note: String = "") {
        viewModelScope.launch {
            val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            repository.insertLog(HabitLog(dateString = todayStr, intensity = intensity, note = note))
        }
    }

    fun deleteLog(dateStr: String) {
        viewModelScope.launch {
            repository.deleteLogByDate(dateStr)
        }
    }

    fun resetTracker() {
        viewModelScope.launch {
            repository.clearAll()
            // Immediately start today's clean count
            val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            repository.insertLog(HabitLog(dateString = todayStr, intensity = 4, note = "Tracker rebooted! Day 1."))
        }
    }

    fun setInitialStreak(days: Int) {
        viewModelScope.launch {
            repository.clearAll()
            val today = LocalDate.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            for (i in (days - 1) downTo 0) {
                val date = today.minusDays(i.toLong())
                val dateStr = date.format(formatter)
                val dayNum = days - i
                repository.insertLog(HabitLog(dateString = dateStr, intensity = 4, note = "Initial streak setup. Day $dayNum."))
            }
        }
    }

    // Preference mutations
    fun updateUserName(name: String) {
        userName.value = name
        sharedPrefs.edit().putString("user_name", name).apply()
    }

    fun updateHabitName(name: String) {
        habitName.value = name
        sharedPrefs.edit().putString("habit_name", name).apply()
    }

    fun updateAccentHex(hex: String) {
        selectedAccentHex.value = hex
        sharedPrefs.edit().putString("accent_hex", hex).apply()
    }

    fun nextQuote() {
        if (quotes.isNotEmpty()) {
            val nextIdx = (activeQuoteIndex.value + 1) % quotes.size
            activeQuoteIndex.value = nextIdx
            sharedPrefs.edit().putInt("active_quote_index", nextIdx).apply()
        }
    }

    fun updateThemeMode(mode: String) {
        themeMode.value = mode
        sharedPrefs.edit().putString("theme_mode", mode).apply()
    }
}

data class DashboardMetrics(
    val totalDaysClean: Int = 0,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val totalXp: Int = 0,
    val level: Int = 1,
    val xpProgress: Float = 0.0f
)
