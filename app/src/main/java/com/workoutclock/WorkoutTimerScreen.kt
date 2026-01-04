package com.workoutclock

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.draw.alpha
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTimerScreen(viewModel: WorkoutTimerViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    // Animated opacity for UI elements when timer is running
    val uiElementsOpacity by animateFloatAsState(
        targetValue = if (uiState.isRunning) 0.6f else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "ui_opacity"
    )
    
    // Initialize DND manager and check permissions
    val dndManager = remember { DNDManager(context) }
    var hasDNDPermission by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.setDNDManager(dndManager)
        hasDNDPermission = dndManager.hasPermission()
    }
    
    // Prompt DND permission if Focus mode selected without permission
    LaunchedEffect(uiState.appMode) {
        if (uiState.appMode == AppMode.FOCUS && !hasDNDPermission && !uiState.isRunning) {
            dndManager.requestPermission()
            hasDNDPermission = dndManager.hasPermission()
        }
    }
    
    // Vibrate and play sound when phase changes
    LaunchedEffect(uiState.currentPhase) {
        if (uiState.isRunning) {
            // Vibration
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(300)
            }
            
            // Play custom sound based on phase
            try {
                val soundResource = when (uiState.currentPhase) {
                    TimerPhase.WORKOUT -> R.raw.workout_beep
                    TimerPhase.REST -> R.raw.rest_beep
                }
                
                val mediaPlayer = MediaPlayer.create(context, soundResource)
                mediaPlayer?.start()
                mediaPlayer?.setOnCompletionListener {
                    it.release()
                }
            } catch (e: Exception) {
                // Fallback: just vibrate if sound fails
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Circular progress indicator with swipe functionality
        CircularTimerWithSwipe(viewModel, uiState)

        Spacer(modifier = Modifier.height(48.dp))

        // Control buttons
        Box(modifier = Modifier.alpha(uiElementsOpacity)) {
            ControlButtons(viewModel, uiState)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Mode selector pills with DND status for Focus mode
        Box(modifier = Modifier.alpha(uiElementsOpacity)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                ModeSelectorPills(viewModel, uiState)
                // DND permission status indicator
                if (uiState.appMode == AppMode.FOCUS) {
                    Text(
                        text = if (hasDNDPermission) "✓ DND enabled" else "⚠ DND permission required",
                        fontSize = 12.sp,
                        color = if (hasDNDPermission) Color(0xFF4CAF50) else Color(0xFFFF9800),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Custom Time Picker
        Box(modifier = Modifier.alpha(uiElementsOpacity)) {
            CustomTimePicker(viewModel, uiState)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Timer info
        Box(modifier = Modifier.alpha(uiElementsOpacity)) {
            TimerInfo(uiState)
        }
    }
}

@Composable
fun CircularTimerWithSwipe(viewModel: WorkoutTimerViewModel, uiState: WorkoutTimerState) {
    Box(
        modifier = Modifier
            .size(300.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        // No action needed on drag end
                    }
                ) { change, dragAmount ->
                    // Detect horizontal swipe
                    if (kotlin.math.abs(dragAmount.x) > kotlin.math.abs(dragAmount.y)) {
                        if (dragAmount.x > 20) {
                            // Swipe right - switch to Workout mode
                            viewModel.setAppMode(AppMode.WORKOUT)
                        } else if (dragAmount.x < -20) {
                            // Swipe left - switch to Focus mode
                            viewModel.setAppMode(AppMode.FOCUS)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val strokeWidth = 2.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            
            // Background circle - very thin gray line
            drawCircle(
                color = Color.Gray.copy(alpha = 0.3f),
                radius = radius,
                style = Stroke(1.dp.toPx())
            )
            
            // Progress arc - gradient color based on mode
            val sweepAngle = (uiState.progress * 360f)
            val gradient = if (uiState.appMode == AppMode.WORKOUT) {
                Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFFF6B35), // Orange
                        Color(0xFF9B59B6), // Purple  
                        Color(0xFFE74C3C)  // Red
                    )
                )
            } else {
                Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF4CAF50), // Green
                        Color(0xFF2196F3), // Blue
                        Color(0xFF9C27B0)  // Purple
                    )
                )
            }
            
            drawArc(
                brush = gradient,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(strokeWidth)
            )
        }
        
        // Timer display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Phase title
            Text(
                text = when (uiState.appMode) {
                    AppMode.WORKOUT -> when (uiState.currentPhase) {
                        TimerPhase.WORKOUT -> "WORKOUT"
                        TimerPhase.REST -> "REST"
                    }
                    AppMode.FOCUS -> when (uiState.currentPhase) {
                        TimerPhase.WORKOUT -> "FOCUS"
                        TimerPhase.REST -> "BREAK"
                    }
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = formatTime(uiState.currentPhaseTimeLeft),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            if (uiState.appMode == AppMode.WORKOUT) {
                Text(
                    text = "Total: ${formatTime(uiState.totalTimeLeft)}",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun ControlButtons(viewModel: WorkoutTimerViewModel, uiState: WorkoutTimerState) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        IconButton(
            onClick = { viewModel.startPause() },
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                imageVector = if (uiState.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (uiState.isRunning) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
        
        IconButton(
            onClick = { viewModel.reset() },
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Stop,
                contentDescription = "Stop",
                tint = Color(0xFFE74C3C),
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
fun ModeSelectorPills(viewModel: WorkoutTimerViewModel, uiState: WorkoutTimerState) {
    Row(
        modifier = Modifier
            .background(
                Color.Gray.copy(alpha = 0.2f),
                RoundedCornerShape(25.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Workout pill
        Box(
            modifier = Modifier
                .background(
                    if (uiState.appMode == AppMode.WORKOUT) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFF6B35),
                                Color(0xFFE74C3C)
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color.Transparent)
                        )
                    },
                    RoundedCornerShape(20.dp)
                )
                .clickable { viewModel.setAppMode(AppMode.WORKOUT) }
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Workout",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = if (uiState.appMode == AppMode.WORKOUT) FontWeight.Bold else FontWeight.Normal
            )
        }
        
        // Focus pill
        Box(
            modifier = Modifier
                .background(
                    if (uiState.appMode == AppMode.FOCUS) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF4CAF50),
                                Color(0xFF2196F3)
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color.Transparent)
                        )
                    },
                    RoundedCornerShape(20.dp)
                )
                .clickable { viewModel.setAppMode(AppMode.FOCUS) }
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Focus",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = if (uiState.appMode == AppMode.FOCUS) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun CustomTimePicker(viewModel: WorkoutTimerViewModel, uiState: WorkoutTimerState) {
    Column(
        modifier = Modifier.padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Time picker wheels
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Hours picker
            TimePickerWheel(
                label = "HH",
                value = uiState.customHours,
                range = 0..23,
                onValueChange = { viewModel.updateCustomHours(it) },
                enabled = !uiState.isRunning,
                modifier = Modifier.weight(1f)
            )
            
            Text(
                text = ":",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            // Minutes picker
            TimePickerWheel(
                label = "MM",
                value = uiState.customMinutes,
                range = 0..59,
                onValueChange = { viewModel.updateCustomMinutes(it) },
                enabled = !uiState.isRunning,
                modifier = Modifier.weight(1f)
            )
            
            Text(
                text = ":",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            // Seconds picker
            TimePickerWheel(
                label = "SS",
                value = uiState.customSeconds,
                range = 0..59,
                onValueChange = { viewModel.updateCustomSeconds(it) },
                enabled = !uiState.isRunning,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Show minimum time warning for Focus mode
        if (uiState.appMode == AppMode.FOCUS) {
            Text(
                text = "Focus mode requires minimum 30 minutes",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun TimerInfo(uiState: WorkoutTimerState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (uiState.appMode) {
            AppMode.WORKOUT -> {
                Text(
                    text = "Workout: 30s | Rest: 10s",
                    fontSize = 16.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Round ${uiState.currentRound} of ${uiState.totalRounds}",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
            AppMode.FOCUS -> {
                if (uiState.currentPhase == TimerPhase.REST) {
                    Text(
                        text = "Take a 5-minute break",
                        fontSize = 16.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = "Stay focused on your task",
                        fontSize = 16.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
                
                // Pomodoro session info
                if (uiState.totalPomodoroSessions > 1) {
                    Text(
                        text = "Session ${uiState.currentPomodoroSession} of ${uiState.totalPomodoroSessions}",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
                
                // DND Status indicator for Focus mode
                if (uiState.isDNDEnabled) {
                    Text(
                        text = "🔕 Do Not Disturb is ON",
                        fontSize = 12.sp,
                        color = Color(0xFF4CAF50),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TimePickerWheel(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val itemHeight = 40.dp
        val listState = rememberLazyListState(0)
        
        // Scroll to the given value when the composable is first launched or value changes.
        LaunchedEffect(value, range) {
            val index = value - range.first
            if (index >= 0 && index < range.count()) {
                listState.scrollToItem(index)
            }
        }

        // Snap to the nearest item when scrolling stops.
        LaunchedEffect(listState.isScrollInProgress, enabled) {
            if (!listState.isScrollInProgress && enabled) {
                val layoutInfo = listState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                if (visibleItems.isNotEmpty()) {
                    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                    val centerItem = visibleItems.minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }
                    if (centerItem != null) {
                        val newValue = range.first + centerItem.index
                        if (value != newValue) {
                            onValueChange(newValue)
                        }
                        listState.animateScrollToItem(centerItem.index)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .height(120.dp) // Show ~3 items
                .background(
                    Color.Gray.copy(alpha = 0.1f),
                    RoundedCornerShape(8.dp)
                )
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = (120.dp - itemHeight) / 2) // Center items vertically
            ) {
                items(range.count()) { index ->
                    val itemValue = range.first + index
                    val isSelected = itemValue == value

                    Text(
                        text = String.format("%02d", itemValue),
                        color = if (isSelected && enabled) Color.White else Color.Gray,
                        fontSize = if (isSelected) 24.sp else 18.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .height(itemHeight)
                            .wrapContentHeight()
                            .alpha(if (enabled) 1f else 0.5f)
                    )
                }
            }
            
            // A subtle line to indicate the center selection area
            Divider(
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(1.dp)
            )
        }
    }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}
