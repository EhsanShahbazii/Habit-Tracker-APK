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
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
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

class ActivityGridWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ActivityGridWidget()
}

class ActivityGridWidget : GlanceAppWidget() {

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
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left status block (Streak)
                        Column(
                            modifier = GlanceModifier.width(75.dp),
                            horizontalAlignment = Alignment.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "STREAK",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFFA0A0A5)),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "$currentStreak",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFFCCFF00)),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "DAYS",
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.width(8.dp))

                        // Vertical separator
                        Box(
                            modifier = GlanceModifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(ColorProvider(Color(0xFF202022)))
                        ) {}

                        Spacer(modifier = GlanceModifier.width(8.dp))

                        // Right Grid Block
                        Column(
                            modifier = GlanceModifier.defaultWeight(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "8 WEEKS",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFFA0A0A5)),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            CompressedGrid(logs = logs)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun CompressedGrid(logs: List<HabitLog>) {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val logMap = logs.associate { it.dateString to it.intensity }

        // Render 10 columns of 5 rows for standard 4x1 grid compatibility
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            for (col in 0 until 10) {
                Column(
                    modifier = GlanceModifier.padding(horizontal = 1.5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (row in 0 until 5) {
                        val daysAgo = (9 - col) * 5 + (4 - row)
                        val date = today.minusDays(daysAgo.toLong())
                        val dateStr = date.format(formatter)
                        val intensity = logMap[dateStr] ?: 0

                        val boxColor = when (intensity) {
                            0 -> ColorProvider(Color(0xFF202022))
                            1 -> ColorProvider(Color(0x33CCFF00))
                            2 -> ColorProvider(Color(0x66CCFF00))
                            3 -> ColorProvider(Color(0xBBCCFF00))
                            else -> ColorProvider(Color(0xFFCCFF00))
                        }

                        Box(
                            modifier = GlanceModifier
                                .size(6.dp)
                                .background(boxColor)
                                .cornerRadius(1.dp)
                                .padding(vertical = 0.5.dp)
                        ) {}
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
