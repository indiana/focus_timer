package com.focus.timer

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

// Helper to find Activity from Context wrapper
private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

// Gorgeous, modern Dark Color Scheme for premium look
private val PremiumDarkColorScheme = darkColorScheme(
    primary = Color(0xFF6366F1), // Indigo 500
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF312E81), // Indigo 900
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFFF43F5E), // Rose 500
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF881337), // Rose 900
    onSecondaryContainer = Color(0xFFFFE4E6),
    background = Color(0xFF0B0F19), // Deep rich space dark
    surface = Color(0xFF151B2C), // Cards background
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E293B) // Circle background track
)

@Composable
fun FocusTimerScreen(viewModel: TimerViewModel) {
    val remainingSeconds = viewModel.remainingSeconds.collectAsState().value
    val totalSeconds = viewModel.totalSeconds.collectAsState().value
    val isRunning = viewModel.isRunning.collectAsState().value
    val intention = viewModel.intention.collectAsState().value

    // Keep Screen On logic
    val view = LocalView.current
    DisposableEffect(isRunning) {
        val activity = view.context.findActivity()
        if (isRunning) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    FocusTimerContent(
        remainingSeconds = remainingSeconds,
        totalSeconds = totalSeconds,
        isRunning = isRunning,
        intention = intention,
        onStartClick = { viewModel.startTimer() },
        onStopClick = { viewModel.stopTimer() },
        onResetClick = { viewModel.resetTimer() },
        onPresetClick = { minutes -> viewModel.setFocusTime(minutes) },
        onIntentionChange = { text -> viewModel.setIntention(text) }
    )
}

@Composable
fun FocusTimerContent(
    remainingSeconds: Int,
    totalSeconds: Int,
    isRunning: Boolean,
    intention: String,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onResetClick: () -> Unit,
    onPresetClick: (Int) -> Unit,
    onIntentionChange: (String) -> Unit
) {
    MaterialTheme(colorScheme = PremiumDarkColorScheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60

            // Calculate animated progress fraction
            val progressFraction = if (totalSeconds > 0) {
                remainingSeconds.toFloat() / totalSeconds.toFloat()
            } else {
                1f
            }

            val animatedProgress by animateFloatAsState(
                targetValue = progressFraction,
                animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                label = "timerProgress"
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. App Title Header
                Text(
                    text = "FOCUS FLOW",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(top = 16.dp)
                )

                // 2. Intention Setter
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    if (isRunning) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "CURRENT TASK",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 1.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = intention.ifBlank { "Staying focused" },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = intention,
                            onValueChange = onIntentionChange,
                            label = { Text("What is your focus target?") },
                            placeholder = { Text("e.g. Coding, Reading, Writing") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        )
                    }
                }

                // 3. Circular Timer Progress
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(260.dp)
                        .padding(16.dp)
                ) {
                    // Underlay Track Circle
                    CircularProgressIndicator(
                        progress = 1.0f,
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 12.dp,
                        strokeCap = StrokeCap.Round
                    )
                    // Active Progress Ring
                    CircularProgressIndicator(
                        progress = animatedProgress,
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 12.dp,
                        strokeCap = StrokeCap.Round
                    )

                    // Text & Info inside
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "%02d:%02d".format(minutes, seconds),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 56.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        if (isRunning) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "FOCUSING",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                // 4. Control Buttons (Start / Pause, Reset)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (!isRunning) {
                            Button(
                                onClick = onStartClick,
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(
                                    text = "Start Focus",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Button(
                                onClick = onStopClick,
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text(
                                    text = "Pause",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        OutlinedButton(
                            onClick = onResetClick,
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(
                                text = "Reset",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 5. Presets Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val presets = listOf(5, 25, 60)
                        presets.forEach { mins ->
                            val isSelected = (totalSeconds == mins * 60)
                            if (isSelected) {
                                Button(
                                    onClick = { onPresetClick(mins) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("${mins}m", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { onPresetClick(mins) },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("${mins}m", color = MaterialTheme.colorScheme.onBackground)
                                }
                            }
                        }
                    }
                }

                // 6. Admob Banner Spacer / Ad
                AndroidView(
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    factory = { context ->
                        AdView(context).apply {
                            setAdSize(AdSize.BANNER)
                            adUnitId = "ca-app-pub-3940256099942544/6300978111" // Test Banner
                            loadAd(AdRequest.Builder().build())
                        }
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FocusTimerContentPreview() {
    FocusTimerContent(
        remainingSeconds = 1500,
        totalSeconds = 1500,
        isRunning = false,
        intention = "Reviewing Code",
        onStartClick = {},
        onStopClick = {},
        onResetClick = {},
        onPresetClick = {},
        onIntentionChange = {}
    )
}
