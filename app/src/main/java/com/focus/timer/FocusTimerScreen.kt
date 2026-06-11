package com.focus.timer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FocusTimerScreen(viewModel: TimerViewModel) {
    val remainingSeconds = viewModel.remainingSeconds.collectAsState().value
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "%02d:%02d".format(minutes, seconds),
            fontSize = 72.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row {
            Button(onClick = { viewModel.startTimer() }) { Text("Start") }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = { viewModel.stopTimer() }) { Text("Stop") }
        }
    }
}
