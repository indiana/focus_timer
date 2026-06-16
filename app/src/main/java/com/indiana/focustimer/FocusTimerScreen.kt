package com.indiana.focustimer

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.animation.animateColorAsState
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
    val isPomodoroMode = viewModel.isPomodoroMode.collectAsState().value
    val currentStage = viewModel.currentStage.collectAsState().value
    val completedFocusCount = viewModel.completedFocusCount.collectAsState().value

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
        isPomodoroMode = isPomodoroMode,
        currentStage = currentStage,
        completedFocusCount = completedFocusCount,
        onStartClick = { viewModel.startTimer() },
        onStopClick = { viewModel.stopTimer() },
        onResetClick = { viewModel.resetTimer() },
        onPresetSelect = { index -> viewModel.selectPreset(index) },
        onAdjustTime = { mins, secs -> viewModel.setCustomTime(mins, secs) },
        onSavePresetClick = { viewModel.saveCurrentAsPreset() },
        onIntentionChange = { text -> viewModel.setIntention(text) },
        onModeToggle = { enabled -> viewModel.setPomodoroMode(enabled) },
        onSkipClick = { viewModel.skipPomodoroStage() },
        onResetCycleClick = { viewModel.resetPomodoroCycle() }
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
    isPomodoroMode: Boolean,
    currentStage: PomodoroStage,
    completedFocusCount: Int,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onResetClick: () -> Unit,
    onPresetSelect: (Int) -> Unit,
    onAdjustTime: (Int, Int) -> Unit,
    onSavePresetClick: () -> Unit,
    onIntentionChange: (String) -> Unit,
    onModeToggle: (Boolean) -> Unit,
    onSkipClick: () -> Unit,
    onResetCycleClick: () -> Unit
) {
    var showAdjustDialog by remember { mutableStateOf(false) }

    val themePrimaryColor = if (isPomodoroMode && (currentStage == PomodoroStage.SHORT_BREAK || currentStage == PomodoroStage.LONG_BREAK)) {
        Color(0xFF2DD4BF) // Teal 400
    } else {
        Color(0xFF6366F1) // Indigo 500
    }

    val animatedPrimaryColor by animateColorAsState(
        targetValue = themePrimaryColor,
        label = "themePrimaryColor"
    )

    val dynamicColorScheme = PremiumDarkColorScheme.copy(
        primary = animatedPrimaryColor,
        primaryContainer = if (isPomodoroMode && (currentStage == PomodoroStage.SHORT_BREAK || currentStage == PomodoroStage.LONG_BREAK)) {
            Color(0xFF0F766E) // Teal 700 / Dark teal container
        } else {
            Color(0xFF312E81) // Indigo 900
        }
    )

    MaterialTheme(colorScheme = dynamicColorScheme) {
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
                            text = "Set a custom duration for this session:",
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

                // 1.5 Mode Switcher (Timer / Pomodoro)
                if (!isRunning) {
                    TabRow(
                        selectedTabIndex = if (isPomodoroMode) 1 else 0,
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .padding(vertical = 4.dp),
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = {}
                    ) {
                        val selectedTabColor = MaterialTheme.colorScheme.primary
                        val unselectedTabColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        
                        Tab(
                            selected = !isPomodoroMode,
                            onClick = { onModeToggle(false) },
                            text = { 
                                Text(
                                    text = stringResource(R.string.mode_custom_timer), 
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ) 
                            },
                            selectedContentColor = selectedTabColor,
                            unselectedContentColor = unselectedTabColor
                        )
                        Tab(
                            selected = isPomodoroMode,
                            onClick = { onModeToggle(true) },
                            text = { 
                                Text(
                                    text = stringResource(R.string.mode_pomodoro), 
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ) 
                            },
                            selectedContentColor = selectedTabColor,
                            unselectedContentColor = unselectedTabColor
                        )
                    }
                } else {
                    Text(
                        text = if (isPomodoroMode) stringResource(R.string.mode_pomodoro).uppercase() else stringResource(R.string.mode_custom_timer).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            letterSpacing = 1.5.sp
                        ),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // 2. Intention Setter / Break Prompts
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    val showIntentionInput = !isPomodoroMode || currentStage == PomodoroStage.FOCUS
                    
                    if (showIntentionInput) {
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
                    } else {
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
                                    text = stringResource(
                                        if (currentStage == PomodoroStage.SHORT_BREAK) R.string.pomodoro_short_break_title 
                                        else R.string.pomodoro_long_break_title
                                    ).uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 1.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(
                                        if (currentStage == PomodoroStage.SHORT_BREAK) R.string.pomodoro_break_prompt 
                                        else R.string.pomodoro_long_break_prompt
                                    ),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // 3. Circular Timer Progress
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(250.dp)
                        .padding(8.dp)
                ) {
                    CircularProgressIndicator(
                        progress = 1.0f,
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 12.dp,
                        strokeCap = StrokeCap.Round
                    )
                    CircularProgressIndicator(
                        progress = animatedProgress,
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 12.dp,
                        strokeCap = StrokeCap.Round
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(enabled = !isRunning) {
                                showAdjustDialog = true
                            }
                    ) {
                        if (isPomodoroMode) {
                            val stageText = when (currentStage) {
                                PomodoroStage.FOCUS -> stringResource(R.string.pomodoro_focus_title, (completedFocusCount % 4) + 1)
                                PomodoroStage.SHORT_BREAK -> stringResource(R.string.pomodoro_short_break_title)
                                PomodoroStage.LONG_BREAK -> stringResource(R.string.pomodoro_long_break_title)
                            }
                            Text(
                                text = stageText.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        Text(
                            text = "%02d:%02d".format(minutes, seconds),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 52.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        if (isRunning) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val runningIndicator = if (isPomodoroMode && currentStage != PomodoroStage.FOCUS) "RESTING" else stringResource(R.string.focusing_indicator)
                            Text(
                                text = runningIndicator,
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

                // 3.5 Pomodoro Cycle Dots (Progress indicators)
                if (isPomodoroMode) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        val activeFocusSessionIndex = completedFocusCount % 4
                        for (i in 0 until 4) {
                            val isCompleted = i < activeFocusSessionIndex
                            val isActive = i == activeFocusSessionIndex && currentStage == PomodoroStage.FOCUS
                            
                            val dotColor = if (isCompleted) {
                                MaterialTheme.colorScheme.primary
                            } else if (isActive) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                            
                            val dotWidth = if (isActive) 16.dp else 8.dp
                            
                            Surface(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(dotWidth),
                                shape = RoundedCornerShape(4.dp),
                                color = dotColor
                            ) {}
                        }
                    }
                }

                // 4. Control Buttons (Start / Pause, Reset, Skip, Reset Cycle)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
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
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
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
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        OutlinedButton(
                            onClick = onResetClick,
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.reset_btn),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        if (isPomodoroMode) {
                            Spacer(modifier = Modifier.width(12.dp))
                            OutlinedButton(
                                onClick = onSkipClick,
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.btn_skip),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }

                    if (isPomodoroMode && !isRunning && completedFocusCount > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = onResetCycleClick
                        ) {
                            Text(
                                text = stringResource(R.string.btn_reset_cycle),
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. Presets Row & Save Option (Only shown in Custom Timer mode)
                    if (!isPomodoroMode) {
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
        isPomodoroMode = true,
        currentStage = PomodoroStage.FOCUS,
        completedFocusCount = 1,
        onStartClick = {},
        onStopClick = {},
        onResetClick = {},
        onPresetSelect = {},
        onAdjustTime = { _, _ -> },
        onSavePresetClick = {},
        onIntentionChange = {},
        onModeToggle = {},
        onSkipClick = {},
        onResetCycleClick = {}
    )
}
}

