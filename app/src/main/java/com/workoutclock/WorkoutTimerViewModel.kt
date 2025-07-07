package com.workoutclock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppMode {
    WORKOUT,
    FOCUS
}

enum class TimerPhase {
    WORKOUT, REST
}

data class WorkoutTimerState(
    val isRunning: Boolean = false,
    val currentPhase: TimerPhase = TimerPhase.WORKOUT,
    val currentPhaseTimeLeft: Int = 30, // seconds
    val totalTimeLeft: Int = 30 * 60, // Default 30 minutes
    val customHours: Int = 0,
    val customMinutes: Int = 30,
    val customSeconds: Int = 0,
    val workoutDuration: Int = 30,
    val restDuration: Int = 10,
    val totalRounds: Int = 0,
    val currentRound: Int = 0,
    val appMode: AppMode = AppMode.WORKOUT,
    val progress: Float = 0f,
    val isDNDEnabled: Boolean = false,
    // Pomodoro-specific fields
    val currentPomodoroSession: Int = 0,
    val totalPomodoroSessions: Int = 0,
    val remainingTotalTime: Int = 0 // Track remaining time across all sessions
)

class WorkoutTimerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(WorkoutTimerState())
    val uiState: StateFlow<WorkoutTimerState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private val breakDurationInSeconds = 5 * 60 // 5 minutes
    private val pomodoroSessionDuration = 25 * 60 // 25 minutes
    private var dndManager: DNDManager? = null

    init {
        reset()
    }

    fun setDNDManager(manager: DNDManager) {
        dndManager = manager
    }

    fun setAppMode(mode: AppMode) {
        _uiState.update { it.copy(appMode = mode) }
        reset()
    }

    fun updateCustomHours(hours: Int) {
        if (!_uiState.value.isRunning && hours in 0..23) {
            _uiState.update { it.copy(customHours = hours) }
            enforceMinimumDuration()
            reset()
        }
    }

    fun updateCustomMinutes(minutes: Int) {
        if (!_uiState.value.isRunning && minutes in 0..59) {
            _uiState.update { it.copy(customMinutes = minutes) }
            enforceMinimumDuration()
            reset()
        }
    }

    fun updateCustomSeconds(seconds: Int) {
        if (!_uiState.value.isRunning && seconds in 0..59) {
            _uiState.update { it.copy(customSeconds = seconds) }
            enforceMinimumDuration()
            reset()
        }
    }

    private fun enforceMinimumDuration() {
        if (_uiState.value.appMode == AppMode.FOCUS) {
            val currentState = _uiState.value
            val totalSeconds = currentState.customHours * 3600 + currentState.customMinutes * 60 + currentState.customSeconds
            val minimumSeconds = 30 * 60 // 30 minutes
            
            if (totalSeconds < minimumSeconds) {
                _uiState.update { it.copy(customHours = 0, customMinutes = 30, customSeconds = 0) }
            }
        }
    }

    fun startPause() {
        if (_uiState.value.isRunning) {
            pause()
        } else {
            start()
        }
    }

    private fun start() {
        // Prevent starting if time is zero
        if (_uiState.value.currentPhaseTimeLeft <= 0) {
            reset()
            if (_uiState.value.currentPhaseTimeLeft <= 0) return
        }
        
        // Handle DND for Focus mode
        if (_uiState.value.appMode == AppMode.FOCUS && _uiState.value.currentPhase == TimerPhase.WORKOUT) {
            val dndEnabled = dndManager?.enableDND() ?: false
            _uiState.update { it.copy(isDNDEnabled = dndEnabled) }
        }
        
        _uiState.update { it.copy(isRunning = true) }
        timerJob = viewModelScope.launch {
            while (_uiState.value.isRunning) {
                delay(1000)
                timerTick()
            }
        }
    }

    private fun pause() {
        _uiState.update { it.copy(isRunning = false) }
        timerJob?.cancel()
    }

    private fun timerTick() {
        _uiState.update { currentState ->
            if (currentState.currentPhaseTimeLeft <= 0) {
                handlePhaseCompletion(currentState)
            } else {
                updatePhaseProgress(currentState, currentState.currentPhaseTimeLeft - 1)
            }
        }
    }

    private fun handlePhaseCompletion(currentState: WorkoutTimerState): WorkoutTimerState {
        return when (currentState.appMode) {
            AppMode.WORKOUT -> {
                val newTotalTimeLeft = currentState.totalTimeLeft - 1
                if (newTotalTimeLeft < 0) {
                    return resetAndReturnState()
                }
                val nextPhase = if (currentState.currentPhase == TimerPhase.WORKOUT) TimerPhase.REST else TimerPhase.WORKOUT
                val nextPhaseDuration = if (nextPhase == TimerPhase.WORKOUT) currentState.workoutDuration else currentState.restDuration
                val nextRound = if (nextPhase == TimerPhase.WORKOUT) currentState.currentRound + 1 else currentState.currentRound
                currentState.copy(
                    currentPhase = nextPhase,
                    currentPhaseTimeLeft = nextPhaseDuration,
                    totalTimeLeft = newTotalTimeLeft,
                    currentRound = nextRound,
                    progress = 0f
                )
            }
            AppMode.FOCUS -> {
                if (currentState.currentPhase == TimerPhase.WORKOUT) { // Focus session ended
                    // Start break and disable DND
                    dndManager?.disableDND()
                    val newState = currentState.copy(
                        currentPhase = TimerPhase.REST,
                        currentPhaseTimeLeft = breakDurationInSeconds,
                        progress = 0f,
                        isDNDEnabled = false
                    )
                    _uiState.update { newState }
                    newState
                } else { // Break ended - check if we should start another focus session
                    if (currentState.remainingTotalTime > 0) {
                        // Start next Pomodoro session if there's remaining time
                        val nextSessionDuration = minOf(pomodoroSessionDuration, currentState.remainingTotalTime)
                        val dndEnabled = dndManager?.enableDND() ?: false
                        currentState.copy(
                            currentPhase = TimerPhase.WORKOUT,
                            currentPhaseTimeLeft = nextSessionDuration,
                            currentPomodoroSession = currentState.currentPomodoroSession + 1,
                            remainingTotalTime = currentState.remainingTotalTime - nextSessionDuration,
                            progress = 0f,
                            isDNDEnabled = dndEnabled
                        )
                    } else {
                        // All sessions completed
                        return resetAndReturnState()
                    }
                }
            }
        }
    }

    private fun updatePhaseProgress(currentState: WorkoutTimerState, newPhaseTimeLeft: Int): WorkoutTimerState {
        val newTotalTimeLeft = if (currentState.appMode == AppMode.WORKOUT) currentState.totalTimeLeft - 1 else newPhaseTimeLeft
        
        val phaseDuration = when(currentState.appMode) {
            AppMode.WORKOUT -> if (currentState.currentPhase == TimerPhase.WORKOUT) currentState.workoutDuration else currentState.restDuration
            AppMode.FOCUS -> {
                if (currentState.currentPhase == TimerPhase.WORKOUT) {
                    // For focus phase, calculate the current session duration
                    val totalCustomTime = currentState.customHours * 3600 + currentState.customMinutes * 60 + currentState.customSeconds
                    val sessionStartTime = totalCustomTime - currentState.remainingTotalTime - currentState.currentPhaseTimeLeft
                    minOf(pomodoroSessionDuration, currentState.remainingTotalTime + currentState.currentPhaseTimeLeft).takeIf { it > 0 } ?: pomodoroSessionDuration
                } else {
                    breakDurationInSeconds
                }
            }
        }

        return currentState.copy(
            currentPhaseTimeLeft = newPhaseTimeLeft,
            totalTimeLeft = newTotalTimeLeft,
            progress = 1f - (newPhaseTimeLeft.toFloat() / phaseDuration)
        )
    }

    fun reset() {
        timerJob?.cancel()
        
        // Reset DND when stopping the timer
        if (_uiState.value.appMode == AppMode.FOCUS) {
            dndManager?.resetDND()
        }
        
        _uiState.update { currentState ->
            resetState(currentState).copy(isDNDEnabled = false)
        }
    }
    
    private fun resetAndReturnState(): WorkoutTimerState {
        pause()
        
        // Reset DND when timer completes
        if (_uiState.value.appMode == AppMode.FOCUS) {
            dndManager?.resetDND()
        }
        
        val newState = resetState(_uiState.value).copy(isDNDEnabled = false)
        _uiState.value = newState
        return newState
    }

    private fun resetState(currentState: WorkoutTimerState): WorkoutTimerState {
        val totalSeconds = currentState.customHours * 3600 + currentState.customMinutes * 60 + currentState.customSeconds
        return if (currentState.appMode == AppMode.WORKOUT) {
            val cycleLength = currentState.workoutDuration + currentState.restDuration
            val totalRounds = if (cycleLength > 0) (totalSeconds / cycleLength).coerceAtLeast(0) else 0
            currentState.copy(
                isRunning = false,
                currentPhase = TimerPhase.WORKOUT,
                totalTimeLeft = totalSeconds,
                currentPhaseTimeLeft = currentState.workoutDuration,
                totalRounds = totalRounds,
                currentRound = if (totalRounds > 0) 1 else 0,
                progress = 0f,
                currentPomodoroSession = 0,
                totalPomodoroSessions = 0,
                remainingTotalTime = 0
            )
        } else { // FOCUS Mode
            val totalPomodoroSessions = kotlin.math.ceil(totalSeconds.toDouble() / pomodoroSessionDuration).toInt()
            val firstSessionDuration = minOf(pomodoroSessionDuration, totalSeconds)
            currentState.copy(
                isRunning = false,
                currentPhase = TimerPhase.WORKOUT, // WORKOUT represents FOCUS phase
                totalTimeLeft = totalSeconds,
                currentPhaseTimeLeft = firstSessionDuration,
                totalRounds = 0,
                currentRound = 0,
                progress = 0f,
                currentPomodoroSession = 1,
                totalPomodoroSessions = totalPomodoroSessions,
                remainingTotalTime = totalSeconds - firstSessionDuration
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
