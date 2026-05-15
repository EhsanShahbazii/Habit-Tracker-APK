package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.data.HabitDatabase
import com.example.data.HabitLog
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class DetailedStreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DetailedStreakWidget()
}

class DetailedStreakWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = HabitDatabase.getDatabase(context)
        val logs = try {
            database.habitDao().getAllLogs().first()
        } catch (e: Exception) {
            emptyList()
        }

        val currentStreak = calculateCurrentStreak(logs)
        val bestStreak = calculateMaxStreak(logs)
        val totalClean = logs.count { it.intensity > 0 }
        
        val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val todayIntensity = logs.find { it.dateString == todayStr }?.intensity ?: -1

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(Color(0xFF0F0F10)))
                        .cornerRadius(16.dp)
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        horizontalAlignment = Alignment.Start,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Title header
                        Text(
                            text = "SOVEREIGN STATUS",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFA0A0A5)),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(8.dp))

                        // Large streak callout
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = "$currentStreak",
                                    style = TextStyle(
                                        color = ColorProvider(Color(0xFFCCFF00)),
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "CURRENT STREAK",
                                    style = TextStyle(
                                        color = ColorProvider(Color.White),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                            
                            // Today status pill
                            Box(
                                modifier = GlanceModifier
                                    .background(
                                        if (todayIntensity >= 0) ColorProvider(Color(0x22CCFF00)) 
                                        else ColorProvider(Color(0x1AFFFFFF))
                                    )
                                    .cornerRadius(6.dp)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (todayIntensity >= 0) "LOGGED" else "PENDING",
                                    style = TextStyle(
                                        color = if (todayIntensity >= 0) ColorProvider(Color(0xFFCCFF00)) 
                                                else ColorProvider(Color(0xFFA0A0A5)),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.height(12.dp))

                        // Secondary Row: Best streak & Total clean
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = "$bestStreak",
                                    style = TextStyle(
                                        color = ColorProvider(Color.White),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "BEST STREAK",
                                    style = TextStyle(
                                        color = ColorProvider(Color(0xFFA0A0A5)),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }

                            Spacer(modifier = GlanceModifier.width(8.dp))

                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = "$totalClean",
                                    style = TextStyle(
                                        color = ColorProvider(Color.White),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "TOTAL DAYS",
                                    style = TextStyle(
                                        color = ColorProvider(Color(0xFFA0A0A5)),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun calculateCurrentStreak(logs: List<HabitLog>): Int {
        if (logs.isEmpty()) return 0
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now()
        val activeDates = logs.filter { it.intensity > 0 }.mapNotNull {
            try {
                LocalDate.parse(it.dateString, formatter)
            } catch (e: Exception) {
                null
            }
        }.toSet()

        var streak = 0
        var checkDate = today
        if (!activeDates.contains(today) && activeDates.contains(today.minusDays(1))) {
            checkDate = today.minusDays(1)
        }

        while (activeDates.contains(checkDate)) {
            streak++
            checkDate = checkDate.minusDays(1)
        }
        return streak
    }

    private fun calculateMaxStreak(logs: List<HabitLog>): Int {
        if (logs.isEmpty()) return 0
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val activeDates = logs.filter { it.intensity > 0 }.mapNotNull {
            try {
                LocalDate.parse(it.dateString, formatter)
            } catch (e: Exception) {
                null
            }
        }.toSet()

        var maxStreak = 0
        var tempStreak = 0
        var prevDate: LocalDate? = null

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
        return maxStreak
    }
}
