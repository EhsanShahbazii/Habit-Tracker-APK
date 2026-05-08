package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
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

class HabitWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 110.dp), // Compact Layout (1x1 or 2x2)
            DpSize(250.dp, 110.dp)  // Wide Layout (3x2 or 4x2)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = HabitDatabase.getDatabase(context)
        val logs = try {
            database.habitDao().getAllLogs().first()
        } catch (e: Exception) {
            emptyList()
        }
        val totalDays = logs.count { it.intensity > 0 }

        provideContent {
            GlanceTheme {
                val size = LocalSize.current
                val isWide = size.width >= 200.dp
                WidgetContent(isWide = isWide, totalDays = totalDays, logs = logs)
            }
        }
    }

    @Composable
    private fun WidgetContent(isWide: Boolean, totalDays: Int, logs: List<HabitLog>) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF0F0F10)))
                .cornerRadius(16.dp)
                .padding(12.dp)
        ) {
            if (isWide) {
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Metric Column
                    Column(
                        modifier = GlanceModifier.width(80.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "CLEAN",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFA0A0A5)),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "$totalDays",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFCCFF00)),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "DAYS",
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(8.dp))

                    // Right Compressed GitHub Grid Column
                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                        verticalAlignment = Alignment.CenterVertically
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
            } else {
                // Compact Layout
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
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = "$totalDays",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFCCFF00)),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(2.dp))
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

    @Composable
    private fun CompressedGrid(logs: List<HabitLog>) {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val logMap = logs.associate { it.dateString to it.intensity }

        // Render 8 columns of 5 rows
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (col in 0 until 8) {
                Column(
                    modifier = GlanceModifier.padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (row in 0 until 5) {
                        val daysAgo = (7 - col) * 5 + (4 - row)
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
                                .size(8.dp)
                                .background(boxColor)
                                .cornerRadius(2.dp)
                                .padding(vertical = 1.dp)
                        ) {}
                    }
                }
            }
        }
    }
}
