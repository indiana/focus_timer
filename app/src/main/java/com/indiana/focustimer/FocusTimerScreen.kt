package com.indiana.focustimer

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.launch

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

@OptIn(ExperimentalMaterial3Api::class)
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
    val currentStageIndex = viewModel.currentStageIndex.collectAsState().value
    val pomodoroSequence = viewModel.pomodoroSequence.collectAsState().value
    
    val currentScreen = viewModel.currentScreen.collectAsState().value
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.background,
                    drawerContentColor = MaterialTheme.colorScheme.onBackground
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "FOCUS FLOW",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                    )
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    NavigationDrawerItem(
                        icon = { Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null) },
                        label = { Text(stringResource(R.string.menu_timer)) },
                        selected = currentScreen == Screen.TIMER,
                        onClick = {
                            viewModel.navigateTo(Screen.TIMER)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    NavigationDrawerItem(
                        icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = null) },
                        label = { Text(stringResource(R.string.menu_settings)) },
                        selected = currentScreen == Screen.SETTINGS,
                        onClick = {
                            viewModel.navigateTo(Screen.SETTINGS)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (currentScreen == Screen.TIMER) "FOCUS FLOW" else stringResource(R.string.title_settings).uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.primary,
                            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    if (currentScreen == Screen.TIMER) {
                        FocusTimerContent(
                            remainingSeconds = remainingSeconds,
                            totalSeconds = totalSeconds,
                            isRunning = isRunning,
                            intention = intention,
                            presets = presets,
                            selectedPresetIndex = selectedPresetIndex,
                            isPomodoroMode = isPomodoroMode,
                            currentStage = currentStage,
                            currentStageIndex = currentStageIndex,
                            pomodoroSequence = pomodoroSequence,
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
                    } else {
                        SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: TimerViewModel) {
    val context = LocalContext.current
    val alarmSoundUri = viewModel.alarmSoundUri.collectAsState().value
    val alarmSoundName = viewModel.alarmSoundName.collectAsState().value
    val vibrationEnabled = viewModel.vibrationEnabled.collectAsState().value
    
    val focusDuration = viewModel.pomodoroFocusDuration.collectAsState().value
    val shortBreakDuration = viewModel.pomodoroShortBreakDuration.collectAsState().value
    val longBreakDuration = viewModel.pomodoroLongBreakDuration.collectAsState().value
    
    val pomodoroSequence = viewModel.pomodoroSequence.collectAsState().value
    val currentStageIndex = viewModel.currentStageIndex.collectAsState().value

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pickedUri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            val soundName = if (pickedUri != null) {
                val ringtone = RingtoneManager.getRingtone(context, pickedUri)
                ringtone?.getTitle(context) ?: "Unknown Sound"
            } else {
                "Silent"
            }
            viewModel.setAlarmSound(pickedUri?.toString(), soundName)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Sound Settings
        item {
            Text(
                text = stringResource(R.string.pref_category_sound).uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.5.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val currentUri = alarmSoundUri?.takeIf { it.isNotEmpty() }?.let { Uri.parse(it) }
                                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_ALARM)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Sound")
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                }
                                ringtonePickerLauncher.launch(intent)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.pref_alarm_sound),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = alarmSoundName,
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            )
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.pref_vibrate),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = stringResource(R.string.pref_vibrate_desc),
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            )
                        }
                        Switch(
                            checked = vibrationEnabled,
                            onCheckedChange = { viewModel.setVibrationEnabled(it) }
                        )
                    }
                }
            }
        }

        // 2. Pomodoro Durations
        item {
            Text(
                text = stringResource(R.string.pref_category_durations).uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.5.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DurationAdjuster(
                        label = "Focus Duration",
                        currentMinutes = focusDuration / 60,
                        onMinutesChange = { viewModel.setStageDuration(PomodoroStage.FOCUS, it) }
                    )
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
                    DurationAdjuster(
                        label = "Short Break Duration",
                        currentMinutes = shortBreakDuration / 60,
                        onMinutesChange = { viewModel.setStageDuration(PomodoroStage.SHORT_BREAK, it) }
                    )
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
                    DurationAdjuster(
                        label = "Long Break Duration",
                        currentMinutes = longBreakDuration / 60,
                        onMinutesChange = { viewModel.setStageDuration(PomodoroStage.LONG_BREAK, it) }
                    )
                }
            }
        }

        // 3. Workflow Sequencer
        item {
            Text(
                text = stringResource(R.string.pref_category_workflow).uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.5.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    pomodoroSequence.forEachIndexed { index, stage ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val stageLabel = when (stage) {
                                PomodoroStage.FOCUS -> stringResource(R.string.stage_display_focus, index + 1)
                                PomodoroStage.SHORT_BREAK -> stringResource(R.string.stage_display_short_break, index + 1)
                                PomodoroStage.LONG_BREAK -> stringResource(R.string.stage_display_long_break, index + 1)
                            }
                            Text(
                                text = stageLabel,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (index == currentStageIndex) FontWeight.Bold else FontWeight.Normal,
                                    color = if (index == currentStageIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.moveStageInSequence(index, up = true) },
                                    enabled = index > 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("▲", fontSize = 12.sp, color = if (index > 0) MaterialTheme.colorScheme.primary else Color.Gray)
                                }
                                IconButton(
                                    onClick = { viewModel.moveStageInSequence(index, up = false) },
                                    enabled = index < pomodoroSequence.size - 1,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("▼", fontSize = 12.sp, color = if (index < pomodoroSequence.size - 1) MaterialTheme.colorScheme.primary else Color.Gray)
                                }
                                IconButton(
                                    onClick = { viewModel.removeStageFromSequence(index) },
                                    enabled = pomodoroSequence.size > 1,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = if (pomodoroSequence.size > 1) MaterialTheme.colorScheme.secondary else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        if (index < pomodoroSequence.size - 1) {
                            Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.addStageToSequence(PomodoroStage.FOCUS) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(stringResource(R.string.btn_add_focus), fontSize = 11.sp, maxLines = 1, softWrap = false)
                        }
                        Button(
                            onClick = { viewModel.addStageToSequence(PomodoroStage.SHORT_BREAK) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)) // Teal 700
                        ) {
                            Text(stringResource(R.string.btn_add_short_break), fontSize = 11.sp, maxLines = 1, softWrap = false)
                        }
                        Button(
                            onClick = { viewModel.addStageToSequence(PomodoroStage.LONG_BREAK) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)) // Teal 700
                        ) {
                            Text(stringResource(R.string.btn_add_long_break), fontSize = 11.sp, maxLines = 1, softWrap = false)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { viewModel.resetSequenceToDefault() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.btn_reset_default), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
fun DurationAdjuster(
    label: String,
    currentMinutes: Int,
    onMinutesChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = { if (currentMinutes > 1) onMinutesChange(currentMinutes - 1) }) {
                Text("−", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = "$currentMinutes min",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.widthIn(min = 60.dp),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { if (currentMinutes < 180) onMinutesChange(currentMinutes + 1) }) {
                Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
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
    currentStageIndex: Int,
    pomodoroSequence: List<PomodoroStage>,
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
                    .size(240.dp)
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

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = !isRunning) {
                            showAdjustDialog = true
                        }
                ) {
                    if (isPomodoroMode && pomodoroSequence.isNotEmpty() && currentStageIndex in pomodoroSequence.indices) {
                        val stageText = when (currentStage) {
                            PomodoroStage.FOCUS -> {
                                val focusCount = pomodoroSequence.count { it == PomodoroStage.FOCUS }
                                val focusIndex = pomodoroSequence.take(currentStageIndex + 1).count { it == PomodoroStage.FOCUS }
                                stringResource(R.string.pomodoro_focus_title, focusIndex, focusCount)
                            }
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
                            fontSize = 50.sp,
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
            if (isPomodoroMode && pomodoroSequence.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    val seqSize = pomodoroSequence.size
                    for (i in 0 until seqSize) {
                        val stageType = pomodoroSequence[i]
                        val isCompleted = i < currentStageIndex
                        val isActive = i == currentStageIndex
                        
                        val dotColor = if (isCompleted) {
                            MaterialTheme.colorScheme.primary
                        } else if (isActive) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }

                        val outlineColor = if (stageType == PomodoroStage.FOCUS) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color(0xFF2DD4BF) // Teal 400
                        }
                        
                        val dotWidth = if (isActive) 16.dp else 8.dp
                        
                        Surface(
                            modifier = Modifier
                                .height(8.dp)
                                .width(dotWidth),
                            shape = RoundedCornerShape(4.dp),
                            color = dotColor,
                            border = androidx.compose.foundation.BorderStroke(1.dp, outlineColor)
                        ) {}
                    }
                }
            }

            // 4. Control Buttons (Start / Pause, Reset, Skip, Reset Cycle)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                val startButtonText = if (isPomodoroMode && currentStage != PomodoroStage.FOCUS) {
                    stringResource(R.string.start_break_btn)
                } else {
                    stringResource(R.string.start_focus_btn)
                }

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
                                text = startButtonText,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
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
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = onResetClick,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.reset_btn),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    if (isPomodoroMode) {
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = onSkipClick,
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.btn_skip),
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                if (isPomodoroMode && !isRunning && currentStageIndex > 0) {
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
        currentStageIndex = 1,
        pomodoroSequence = listOf(PomodoroStage.FOCUS, PomodoroStage.SHORT_BREAK),
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
