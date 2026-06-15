package com.indiana.focustimer

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

private fun formatPresetCaption(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (s == 0) "${m}m" else "${m}m ${s}s"
}

@Composable
fun FocusTimerScreen(viewModel: TimerViewModel) {
    val remainingSeconds = viewModel.remainingSeconds.collectAsState().value
    val totalSeconds = viewModel.totalSeconds.collectAsState().value
    val isRunning = viewModel.isRunning.collectAsState().value
    val intention = viewModel.intention.collectAsState().value
    val presets = viewModel.presets.collectAsState().value
    val selectedPresetIndex = viewModel.selectedPresetIndex.collectAsState().value

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
        presets = presets,
        selectedPresetIndex = selectedPresetIndex,
        onStartClick = { viewModel.startTimer() },
        onStopClick = { viewModel.stopTimer() },
        onResetClick = { viewModel.resetTimer() },
        onPresetSelect = { index -> viewModel.selectPreset(index) },
        onAdjustTime = { mins, secs -> viewModel.setCustomTime(mins, secs) },
        onSavePresetClick = { viewModel.saveCurrentAsPreset() },
        onIntentionChange = { text -> viewModel.setIntention(text) }
    )
}

@Composable
fun FocusTimerContent(
    remainingSeconds: Int,
    totalSeconds: Int,
    isRunning: Boolean,
    intention: String,
    presets: List<Int>,
    selectedPresetIndex: Int,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onResetClick: () -> Unit,
    onPresetSelect: (Int) -> Unit,
    onAdjustTime: (Int, Int) -> Unit,
    onSavePresetClick: () -> Unit,
    onIntentionChange: (String) -> Unit
) {
    var showAdjustDialog by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = PremiumDarkColorScheme) {
        if (showAdjustDialog) {
            var minutesInput by remember { mutableStateOf((totalSeconds / 60).toString()) }
            var secondsInput by remember { mutableStateOf((totalSeconds % 60).toString()) }
            var isError by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showAdjustDialog = false },
                title = { Text("Adjust Focus Time") },
                text = {
                    Column {
                        Text(
                            text = "Set a custom duration for this focus session:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedTextField(
                                value = minutesInput,
                                onValueChange = { newValue ->
                                    if (newValue.all { it.isDigit() }) {
                                        minutesInput = newValue
                                    }
                                },
                                label = { Text("Minutes") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = secondsInput,
                                onValueChange = { newValue ->
                                    if (newValue.all { it.isDigit() }) {
                                        secondsInput = newValue
                                    }
                                },
                                label = { Text("Seconds") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (isError) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Please enter valid numbers (Seconds: 0-59, Minutes: 0-180)",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val mins = minutesInput.toIntOrNull() ?: 0
                            val secs = secondsInput.toIntOrNull() ?: 0
                            if (secs in 0..59 && mins in 0..180 && (mins > 0 || secs > 0)) {
                                onAdjustTime(mins, secs)
                                showAdjustDialog = false
                            } else {
                                isError = true
                            }
                        }
                    ) {
                        Text("Apply", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAdjustDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

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
                    text = stringResource(R.string.focus_flow_title),
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
                                    text = stringResource(R.string.current_task_header),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 1.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = intention.ifBlank { stringResource(R.string.staying_focused_fallback) },
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
                            label = { Text(stringResource(R.string.focus_target_label)) },
                            placeholder = { Text(stringResource(R.string.focus_target_placeholder)) },
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

                    // Text & Info inside (Clickable to adjust time when idle)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(enabled = !isRunning) {
                                showAdjustDialog = true
                            }
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
                                text = stringResource(R.string.focusing_indicator),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "TAP TO ADJUST",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.SemiBold
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
                                    text = stringResource(R.string.start_focus_btn),
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
                                    text = stringResource(R.string.pause_btn),
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
                                text = stringResource(R.string.reset_btn),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 5. Presets Row & Save Option
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            presets.forEachIndexed { index, duration ->
                                val isSelected = (index == selectedPresetIndex)
                                val caption = formatPresetCaption(duration)
                                if (isSelected) {
                                    Button(
                                        onClick = { onPresetSelect(index) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(caption, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { onPresetSelect(index) },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(caption, color = MaterialTheme.colorScheme.onBackground)
                                    }
                                }
                            }
                        }

                        // Show Save button if the current customized time is different from the saved preset value
                        val isModified = (selectedPresetIndex in presets.indices) && (totalSeconds != presets[selectedPresetIndex])
                        if (isModified && !isRunning) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onSavePresetClick,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Text("💾 Save as Preset", fontWeight = FontWeight.Bold)
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
                            adUnitId = BuildConfig.ADMOB_BANNER_UNIT_ID
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
        presets = listOf(300, 1500, 3600),
        selectedPresetIndex = 1,
        onStartClick = {},
        onStopClick = {},
        onResetClick = {},
        onPresetSelect = {},
        onAdjustTime = { _, _ -> },
        onSavePresetClick = {},
        onIntentionChange = {}
    )
}

