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
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.data.HabitDatabase
import com.example.data.HabitLog
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class StreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StreakWidget()
}

class StreakWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = HabitDatabase.getDatabase(context)
        val logs = try {
            database.habitDao().getAllLogs().first()
        } catch (e: Exception) {
            emptyList()
        }
        val currentStreak = calculateCurrentStreak(logs)

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(Color(0xFF0F0F10)))
                        .cornerRadius(12.dp)
                        .padding(8.dp)
                ) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SOVEREIGN",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFA0A0A5)),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(1.dp))
                        Text(
                            text = "$currentStreak",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFCCFF00)),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(1.dp))
                        Text(
                            text = "DAYS",
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
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
}
