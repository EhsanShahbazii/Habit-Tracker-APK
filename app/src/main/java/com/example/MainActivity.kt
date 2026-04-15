package com.example

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.HabitLog
import com.example.data.Quote
import com.example.ui.theme.MyApplicationTheme
import com.example.util.ShareUtils
import com.example.viewmodel.DashboardMetrics
import com.example.viewmodel.HabitViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val DeepOnyx: Color
    @Composable
    get() = MaterialTheme.colorScheme.background

val CarbonGray: Color
    @Composable
    get() = MaterialTheme.colorScheme.surface

class MainActivity : ComponentActivity() {
    private val viewModel: HabitViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val accentHex by viewModel.selectedAccentHex.collectAsStateWithLifecycle()
            val accentColor = remember(accentHex) {
                try {
                    Color(android.graphics.Color.parseColor(accentHex))
                } catch (e: Exception) {
                    Color(0xFFCCFF00)
                }
            }
            MyApplicationTheme(themeMode = themeMode, accentColor = accentColor) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: HabitViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ViewModel State Flow Collections
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val habitName by viewModel.habitName.collectAsStateWithLifecycle()
    val activeQuoteIdx by viewModel.activeQuoteIndex.collectAsStateWithLifecycle()
    val accentHex by viewModel.selectedAccentHex.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    val accentColor = remember(accentHex) {
        try {
            Color(android.graphics.Color.parseColor(accentHex))
        } catch (e: Exception) {
            Color(0xFFCCFF00) // Default Lime
        }
    }

    // Local States
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showShareCustomizer by remember { mutableStateOf(false) }
    var showCheckInDialog by remember { mutableStateOf(false) }

    val activeQuote = remember(activeQuoteIdx, viewModel.quotes) {
        if (viewModel.quotes.isNotEmpty()) {
            viewModel.quotes[activeQuoteIdx % viewModel.quotes.size]
        } else {
            Quote(1, "The obsessed always outperform the talented.", "Mindset")
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepOnyx),
        containerColor = DeepOnyx,
        floatingActionButton = {
            Button(
                onClick = { showCheckInDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .testTag("check_in_button")
                    .padding(8.dp)
                    .height(54.dp)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Check-in",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LOG DAY",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // 1. Top Bar / App Identity Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SOVEREIGN",
                        style = MaterialTheme.typography.labelLarge,
                        color = accentColor,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "PROTOCOL",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
                
                Row {
                    IconButton(
                        onClick = { showShareCustomizer = true },
                        modifier = Modifier
                            .testTag("open_share_customizer")
                            .background(CarbonGray, RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share custom achievement",
                            tint = accentColor
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .testTag("open_settings")
                            .background(CarbonGray, RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Open Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 2. Top Motivation Quote Card (Double tap triggers slide transition)
            QuoteCard(
                quote = activeQuote,
                accentColor = accentColor,
                onDoubleTap = {
                    viewModel.nextQuote()
                }
            )

            // 3. Hero Tracker Metric Panel (Days clean and max streak)
            HeroTrackerPanel(
                metrics = metrics,
                habitName = habitName,
                accentColor = accentColor,
                userName = userName
            )

            // 4. Fit-to-Window Grid Matrix
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CarbonGray, RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "90-DAY MATRIX",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Rest", fontSize = 9.sp, color = Color.Gray)
                        Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(1.dp)))
                        Box(Modifier.size(8.dp).background(accentColor.copy(alpha = 0.3f), RoundedCornerShape(1.dp)))
                        Box(Modifier.size(8.dp).background(accentColor.copy(alpha = 0.6f), RoundedCornerShape(1.dp)))
                        Box(Modifier.size(8.dp).background(accentColor, RoundedCornerShape(1.dp)))
                        Text("Peak", fontSize = 9.sp, color = Color.Gray)
                    }
                }

                // Mathematical Contribution Grid
                ContributionGrid(
                    logs = logs,
                    accentColor = accentColor
                )
            }

            // 5. Gamification Progress Footer & Badge Row
            GamificationFooter(
                metrics = metrics,
                accentColor = accentColor
            )

            // Developer Attribution Footer
            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            uriHandler.openUri("https://github.com/EhsanShahbazii")
                        } catch (e: Exception) {
                            // Fallback
                        }
                    }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Developed with ",
                        style = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = "❤️",
                        style = TextStyle(
                            color = Color(0xFFFF2D55),
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = " by ",
                        style = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = "@EhsanShahbazi",
                        style = TextStyle(
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(72.dp)) // Avoid content overlapping with the floating check-in button
        }
    }

    // 1. Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            currentUserName = userName,
            currentHabitName = habitName,
            currentAccentHex = accentHex,
            currentThemeMode = themeMode,
            onDismiss = { showSettingsDialog = false },
            onSave = { name, habit, hex, mode ->
                viewModel.updateUserName(name)
                viewModel.updateHabitName(habit)
                viewModel.updateAccentHex(hex)
                viewModel.updateThemeMode(mode)
                showSettingsDialog = false
            },
            onResetAll = {
                viewModel.resetTracker()
                Toast.makeText(context, "Reset complete.", Toast.LENGTH_SHORT).show()
                showSettingsDialog = false
            },
            onSetInitialStreak = { days ->
                viewModel.setInitialStreak(days)
                Toast.makeText(context, "Starting streak initialized to $days days!", Toast.LENGTH_LONG).show()
                showSettingsDialog = false
            }
        )
    }

    // 2. Add Log Check-In Dialog
    if (showCheckInDialog) {
        CheckInDialog(
            accentColor = accentColor,
            onDismiss = { showCheckInDialog = false },
            onConfirm = { intensity, note ->
                viewModel.checkIn(intensity, note)
                Toast.makeText(context, "Logged.", Toast.LENGTH_SHORT).show()
                showCheckInDialog = false
            }
        )
    }

    // 3. Share Achievement Card Real-Time Customizer Dialog
    if (showShareCustomizer) {
        ShareCustomizerDialog(
            metrics = metrics,
            activeQuote = activeQuote,
            currentUserName = userName,
            habitName = habitName,
            accentColor = accentColor,
            quotes = viewModel.quotes,
            onDismiss = { showShareCustomizer = false }
        )
    }
}

// ==================== COMPOSE SUBCOMPONENTS ====================

@Composable
fun QuoteCard(
    quote: Quote,
    accentColor: Color,
    onDoubleTap: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = CarbonGray
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTap() }
                )
            }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "FOCUS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "DOUBLE-TAP TO SKIP",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    letterSpacing = 0.5.sp
                )
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = "“${quote.quote}”",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = "— ${quote.author.uppercase()}",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun HeroTrackerPanel(
    metrics: DashboardMetrics,
    habitName: String,
    accentColor: Color,
    userName: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = CarbonGray
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, if (MaterialTheme.colorScheme.background == Color(0xFFF4F4F6)) accentColor.copy(alpha = 0.35f) else accentColor.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Header showing user context and active habit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = habitName.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "AGENT: ${userName.uppercase()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 0.5.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "STATUS: ACTIVE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = accentColor,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Two-column high-density statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Left main metric: Total Days Clean
                Column(modifier = Modifier.weight(1.1f)) {
                    Text(
                        text = "DAYS CLEAN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${metrics.totalDaysClean}",
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Black,
                        color = accentColor,
                        lineHeight = 58.sp,
                        letterSpacing = (-1).sp
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .height(60.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Right secondary metrics: Streak and Max
                Column(
                    modifier = Modifier.weight(0.9f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Column {
                        Text(
                            text = "CURRENT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            letterSpacing = 1.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${metrics.currentStreak}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "days",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accentColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Column {
                        Text(
                            text = "BEST",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            letterSpacing = 1.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${metrics.maxStreak}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "days",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContributionGrid(
    logs: List<HabitLog>,
    accentColor: Color
) {
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val logMap = remember(logs) {
        logs.associate { it.dateString to it.intensity }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val totalWidth = maxWidth
        val gap = 3.dp
        val colCount = 14
        val rowCount = 7
        val cellSize = (totalWidth - (gap * (colCount - 1))) / colCount

        Row(
            horizontalArrangement = Arrangement.spacedBy(gap),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (col in 0 until colCount) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(gap)
                ) {
                    for (row in 0 until rowCount) {
                        // Chronological day mapping from 97 days ago down to today
                        val dayIndex = col * 7 + row
                        val date = today.minusDays((97 - dayIndex).toLong())
                        val dateStr = date.format(formatter)
                        val intensity = logMap[dateStr] ?: 0

                        val isLight = MaterialTheme.colorScheme.background == Color(0xFFF4F4F6)
                        val cellColor = when (intensity) {
                            0 -> if (isLight) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            1 -> if (isLight) accentColor.copy(alpha = 0.35f) else accentColor.copy(alpha = 0.22f)
                            2 -> if (isLight) accentColor.copy(alpha = 0.65f) else accentColor.copy(alpha = 0.48f)
                            3 -> if (isLight) accentColor.copy(alpha = 0.85f) else accentColor.copy(alpha = 0.76f)
                            else -> accentColor // Fully active
                        }

                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .clip(RoundedCornerShape(2.dp))
                                .background(cellColor)
                                .then(
                                    if (intensity == 0 && isLight) {
                                        Modifier.border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
                                    } else Modifier
                                )
                                .clickable {
                                    // Visual confirmation
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GamificationFooter(
    metrics: DashboardMetrics,
    accentColor: Color
) {
    val levelXpRequired = 1000
    val totalXpThisLevel = metrics.totalXp % levelXpRequired
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = CarbonGray
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // XP & Level bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GAMIFIED PROTOCOL",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "LVL ${metrics.level}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "CURRENT EXP",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "$totalXpThisLevel / $levelXpRequired XP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(metrics.xpProgress.coerceIn(0.01f, 1.0f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(accentColor)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Unlocked Minimalist Badges
            Text(
                text = "BADGES",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable row of premium badges
            val badges = remember(metrics.totalDaysClean, metrics.level) {
                listOf(
                    BadgeItem("first_blood", "DAY ONE", "Protocol started.", metrics.totalDaysClean >= 1),
                    BadgeItem("striker_3d", "STRIKER", "3-day streak.", metrics.totalDaysClean >= 3),
                    BadgeItem("iron_will", "IRON WILL", "7-day streak.", metrics.totalDaysClean >= 7),
                    BadgeItem("discipline_2w", "DISCIPLINE", "14-day streak.", metrics.totalDaysClean >= 14),
                    BadgeItem("devotion_3w", "DEVOTION", "21-day streak.", metrics.totalDaysClean >= 21),
                    BadgeItem("sovereign", "SOVEREIGN", "30-day streak.", metrics.totalDaysClean >= 30),
                    BadgeItem("zenith_60d", "ZENITH", "60-day streak.", metrics.totalDaysClean >= 60),
                    BadgeItem("legend_90d", "LEGEND", "90-day streak.", metrics.totalDaysClean >= 90),
                    BadgeItem("titan", "TITAN", "Level 3 reached.", metrics.level >= 3),
                    BadgeItem("beast_mode", "BEAST MODE", "3,000+ XP earned.", metrics.totalXp >= 3000)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                badges.forEach { badge ->
                    BadgeView(badge = badge, accentColor = accentColor)
                }
            }
        }
    }
}

data class BadgeItem(
    val id: String,
    val title: String,
    val desc: String,
    val isUnlocked: Boolean
)

@Composable
fun BadgeView(badge: BadgeItem, accentColor: Color) {
    val isLight = MaterialTheme.colorScheme.background == Color(0xFFF4F4F6)
    val bg = if (badge.isUnlocked) accentColor.copy(alpha = if (isLight) 0.22f else 0.12f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
    val border = if (badge.isUnlocked) accentColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline
    val textThemeColor = if (badge.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

    Box(
        modifier = Modifier
            .width(105.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (badge.isUnlocked) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (badge.isUnlocked) Icons.Default.Check else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (badge.isUnlocked) Color.Black else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = badge.title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = textThemeColor,
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp
            )

            Text(
                text = badge.desc,
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                color = textThemeColor.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 10.sp
            )
        }
    }
}

// ==================== DIALOGS & OVERLAYS ====================

@Composable
fun CheckInDialog(
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (intensity: Int, note: String) -> Unit
) {
    var selectedIntensity by remember { mutableIntStateOf(4) }
    var checkInNote by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp)),
            color = CarbonGray
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "LOG STATUS",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    letterSpacing = 1.5.sp
                )

                Text(
                    text = "Rate discipline (0 - 4):",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                // Grid selection of intensity
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (i in 0..4) {
                        val isSelected = selectedIntensity == i
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                                .border(
                                    1.dp,
                                    if (isSelected) accentColor else MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedIntensity = i },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$i",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Note input field
                OutlinedTextField(
                    value = checkInNote,
                    onValueChange = { checkInNote = it },
                    label = { Text("Notes") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { onDismiss() }) {
                        Text("CANCEL", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { onConfirm(selectedIntensity, checkInNote) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("SUBMIT", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    currentUserName: String,
    currentHabitName: String,
    currentAccentHex: String,
    currentThemeMode: String,
    onDismiss: () -> Unit,
    onSave: (name: String, habit: String, hex: String, themeMode: String) -> Unit,
    onResetAll: () -> Unit,
    onSetInitialStreak: (Int) -> Unit
) {
    var nameState by remember { mutableStateOf(currentUserName) }
    var habitState by remember { mutableStateOf(currentHabitName) }
    var accentHexState by remember { mutableStateOf(currentAccentHex) }
    var themeModeState by remember { mutableStateOf(currentThemeMode) }
    var startingStreakText by remember { mutableStateOf("") }

    val accentColorsList = listOf(
        "#CCFF00" to "Electric Lime",
        "#FFFF5500" to "Toxic Orange",
        "#00E5FF" to "Cyber Cyan",
        "#FFFF007F" to "Hyper Pink"
    )

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp)),
            color = CarbonGray
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "SETTINGS",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 1.5.sp
                )

                // Name Input
                OutlinedTextField(
                    value = nameState,
                    onValueChange = { nameState = it },
                    label = { Text("Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Habit Name Input
                OutlinedTextField(
                    value = habitState,
                    onValueChange = { habitState = it },
                    label = { Text("Goal") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Theme color picker
                Column {
                    Text(
                        text = "THEME",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        accentColorsList.forEach { (hex, label) ->
                            val parsedColor = Color(android.graphics.Color.parseColor(hex))
                            val isSelected = accentHexState.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(parsedColor)
                                    .border(
                                        if (isSelected) 3.dp else 1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable { accentHexState = hex }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Appearance Mode Selector
                Column {
                    Text(
                        text = "APPEARANCE MODE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f), RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val modes = listOf("LIGHT", "DARK", "SYSTEM")
                        modes.forEach { mode ->
                            val isSelected = themeModeState.equals(mode, ignoreCase = true)
                            Button(
                                onClick = { themeModeState = mode },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color(android.graphics.Color.parseColor(accentHexState)) else Color.Transparent,
                                    contentColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Text(
                                    text = mode,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Initial Progress Setup
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f), RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "STARTING PROGRESS OFFSET",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Migrating from another tracker? Set your current clean streak in days to backdate your grid matrix automatically.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        lineHeight = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = startingStreakText,
                            onValueChange = { startingStreakText = it.filter { char -> char.isDigit() } },
                            label = { Text("Streak in Days", fontSize = 11.sp) },
                            placeholder = { Text("e.g., 7, 14, 30", fontSize = 11.sp) },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                val days = startingStreakText.toIntOrNull() ?: 0
                                if (days > 0) {
                                    onSetInitialStreak(days)
                                    startingStreakText = ""
                                }
                            },
                            enabled = startingStreakText.isNotEmpty() && (startingStreakText.toIntOrNull() ?: 0) > 0,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(android.graphics.Color.parseColor(accentHexState)),
                                contentColor = Color.Black,
                                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text("APPLY", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Hazard Reset Area
                val isLightModeActive = MaterialTheme.colorScheme.background == Color(0xFFF4F4F6)
                val hazardBg = if (isLightModeActive) Color(0xFFFFF1F1) else Color(0xFF281111)
                val hazardBorder = if (isLightModeActive) Color(0xFFFFD1D1) else Color(0xFF551111)
                val hazardTitleColor = if (isLightModeActive) Color(0xFFD32F2F) else Color(0xFFFF5555)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(hazardBg, RoundedCornerShape(10.dp))
                        .border(1.dp, hazardBorder, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "HAZARD ACTION ZONE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = hazardTitleColor,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Clearing logs will reset all streaks, levels, and badging progress. This action is irreversible.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Button(
                        onClick = { onResetAll() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF3333),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("RESET ALL DATA", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { onDismiss() }) {
                        Text("CANCEL", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { onSave(nameState, habitState, accentHexState, themeModeState) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text("SAVE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ShareCustomizerDialog(
    metrics: DashboardMetrics,
    activeQuote: Quote,
    currentUserName: String,
    habitName: String,
    accentColor: Color,
    quotes: List<Quote>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Achievement card customization states
    var customName by remember { mutableStateOf(currentUserName) }
    var useGradientBg by remember { mutableStateOf(true) }
    var useSerifFont by remember { mutableStateOf(false) }
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    var activeShareQuoteIndex by remember { mutableIntStateOf(0) }

    val colorList = listOf(
        Color(0xFFCCFF00) to "Lime",
        Color(0xFFFF5500) to "Orange",
        Color(0xFF00E5FF) to "Cyan",
        Color(0xFFFF007F) to "Pink"
    )

    val customAccentColor = colorList[selectedColorIndex].first

    // Selected Quote
    val selectedQuote = remember(activeShareQuoteIndex, quotes) {
        if (quotes.isNotEmpty()) {
            quotes[activeShareQuoteIndex % quotes.size]
        } else {
            activeQuote
        }
    }

    // Graphics Layer reference to render and capture Composable Bitmap
    val graphicsLayer = rememberGraphicsLayer()

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp)),
            color = DeepOnyx
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CUSTOM EXPORT CENTER",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 1.5.sp
                    )
                    IconButton(
                        onClick = { onDismiss() },
                        modifier = Modifier.background(CarbonGray, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // ==================== SHAREABLE ACHIEVEMENT CARD PREVIEW (CAPTURED LIVE) ====================
                Text(
                    text = "CARD LIVE PREVIEW",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.sp
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .background(Color.Black, RoundedCornerShape(16.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // This is the card that will be converted into a Bitmap using GraphicsLayer!
                    Box(
                        modifier = Modifier
                            .size(width = 300.dp, height = 300.dp)
                            .drawWithContent {
                                // Draw content into graphics layer for capture
                                graphicsLayer.record {
                                    this@drawWithContent.drawContent()
                                }
                                // Draw content normally to screen
                                drawContent()
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, customAccentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        // Background Render
                        if (useGradientBg) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                customAccentColor.copy(alpha = 0.45f),
                                                Color(0xFF050505)
                                            )
                                        )
                                    )
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF0E0E10))
                            )
                        }

                        // Content Layout inside the Card
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Top branding info
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "SOVEREIGN AGENT".uppercase(),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = customAccentColor,
                                        letterSpacing = 1.sp,
                                        fontFamily = if (useSerifFont) FontFamily.Serif else FontFamily.SansSerif
                                    )
                                    Text(
                                        text = customName.uppercase(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        fontFamily = if (useSerifFont) FontFamily.Serif else FontFamily.SansSerif
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "LEVEL ${metrics.level}",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }

                            // Center core streak metric (massive!)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = habitName.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.6f),
                                    letterSpacing = 1.5.sp,
                                    fontFamily = if (useSerifFont) FontFamily.Serif else FontFamily.SansSerif
                                )
                                Text(
                                    text = "${metrics.totalDaysClean}",
                                    fontSize = 72.sp,
                                    fontWeight = FontWeight.Black,
                                    color = customAccentColor,
                                    lineHeight = 72.sp,
                                    letterSpacing = (-2).sp,
                                    fontFamily = if (useSerifFont) FontFamily.Serif else FontFamily.SansSerif
                                )
                                Text(
                                    text = "DAYS SUCCESSFUL",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    letterSpacing = 2.sp,
                                    fontFamily = if (useSerifFont) FontFamily.Serif else FontFamily.SansSerif
                                )
                            }

                            // Bottom Quote area
                            Column {
                                Text(
                                    text = "“${selectedQuote.quote}”",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontStyle = FontStyle.Italic,
                                    color = Color.White,
                                    lineHeight = 14.sp,
                                    textAlign = TextAlign.Center,
                                    fontFamily = if (useSerifFont) FontFamily.Serif else FontFamily.SansSerif,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "— ${selectedQuote.author.uppercase()}",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = customAccentColor,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // ==================== CUSTOMIZATION CONTROLS ====================
                Text(
                    text = "CARD DESIGN CONFIGURATION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 1.5.sp
                )

                // Name Input
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("NAME") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = customAccentColor,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        focusedBorderColor = customAccentColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Background toggle & typography toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("BACKGROUND", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row {
                            Button(
                                onClick = { useGradientBg = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (useGradientBg) customAccentColor else CarbonGray,
                                    contentColor = if (useGradientBg) Color.Black else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .border(1.dp, if (useGradientBg) Color.Transparent else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            ) {
                                Text("GRADIENT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = { useGradientBg = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!useGradientBg) customAccentColor else CarbonGray,
                                    contentColor = if (!useGradientBg) Color.Black else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .border(1.dp, if (!useGradientBg) Color.Transparent else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            ) {
                                Text("PHOTO", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("FONT STYLE", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row {
                            Button(
                                onClick = { useSerifFont = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!useSerifFont) customAccentColor else CarbonGray,
                                    contentColor = if (!useSerifFont) Color.Black else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .border(1.dp, if (!useSerifFont) Color.Transparent else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            ) {
                                Text("SANS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = { useSerifFont = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (useSerifFont) customAccentColor else CarbonGray,
                                    contentColor = if (useSerifFont) Color.Black else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .border(1.dp, if (useSerifFont) Color.Transparent else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            ) {
                                Text("SERIF", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Quote Picker Slider
                if (quotes.isNotEmpty()) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("QUOTE", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text("Quote ${activeShareQuoteIndex + 1} of ${quotes.size}", fontSize = 9.sp, color = customAccentColor, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    activeShareQuoteIndex = if (activeShareQuoteIndex > 0) activeShareQuoteIndex - 1 else quotes.size - 1
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CarbonGray),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(36.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text("<", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black)
                            }

                            Text(
                                text = "“${selectedQuote.quote.take(45)}...”",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                textAlign = TextAlign.Center
                            )

                            Button(
                                onClick = {
                                    activeShareQuoteIndex = (activeShareQuoteIndex + 1) % quotes.size
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CarbonGray),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(36.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text(">", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                // Accent Color Selector for Share Dialog
                Column {
                    Text("ACCENT", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colorList.forEachIndexed { idx, (color, name) ->
                            val isSelected = selectedColorIndex == idx
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(color)
                                    .border(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { selectedColorIndex = idx },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name.uppercase(),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // TRIGGER SYSTEM EXPORT
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val imageBitmap = graphicsLayer.toImageBitmap()
                                val bitmap = imageBitmap.asAndroidBitmap()
                                ShareUtils.shareBitmap(context, bitmap, "Share Achievement Card")
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = customAccentColor,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_card_button")
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GENERATE BITMAP & EXPORT", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 0.5.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}
