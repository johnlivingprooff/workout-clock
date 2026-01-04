# Workout Clock - AI Coding Agent Instructions

## Project Overview
**Workout Clock** is a minimalist Android timer app (Kotlin + Jetpack Compose) with two modes:
- **Workout Mode**: Interval training (configurable workout/rest cycles)
- **Focus Mode**: Pomodoro sessions (25min focus + 5min break) with automatic Do Not Disturb management

Targets Android API 24+, compiles to API 34. Single-screen app with reactive MVVM architecture.

## Architecture & Key Components

### Core State Management (`WorkoutTimerViewModel`)
- **Single source of truth**: `WorkoutTimerState` (sealed data class with both modes' fields)
- **StateFlow-based**: UI observes via `uiState.collectAsState()`
- **Key states**:
  - `appMode`: Toggles WORKOUT ↔ FOCUS behavior
  - `currentPhase`: Enum `TimerPhase.WORKOUT` or `.REST` (reused for both modes)
  - `totalTimeLeft` / `currentPhaseTimeLeft`: Track remaining seconds at global and phase level
  - Pomodoro-specific: `currentPomodoroSession`, `totalPomodoroSessions`, `remainingTotalTime`

### Timer Logic (`WorkoutTimerViewModel.timerTick()`)
- Runs 1-second coroutine loop via `viewModelScope.launch`
- **Workout mode**: Alternates WORKOUT (30s) ↔ REST (10s) phases until `totalTimeLeft` depletes
- **Focus mode**: Alternates FOCUS (25min) ↔ BREAK (5min) phases; calculates `totalPomodoroSessions` upfront; session boundaries trigger phase transitions
- Progress bar: `progress = 1f - (remaining / phaseDuration)`

### DND Manager (`DNDManager`)
- Only active in Focus mode during WORKOUT phases
- Checks `notificationManager.isNotificationPolicyAccessGranted` (API 31+)
- Sets `NotificationManager.INTERRUPTION_FILTER_PRIORITY` (allows alarms/media)
- **Call `resetDND()` when timer completes or user stops** (not automatic)

### UI Layer (`WorkoutTimerScreen` - 610 lines)
- **Custom Canvas**: Draws circular progress indicator with gradient (workout: blue→cyan, focus: orange→red)
- **Gesture handling**: `detectDragGestures` for swipe-to-switch-mode (left/right)
- **Audio/Haptic**: Plays custom sounds (`R.raw.workout_beep`, `R.raw.rest_beep`) on phase transitions via `MediaPlayer`; vibrates via `Vibrator` (API 26+ with `VibrationEffect`)
- **Opacity animation**: UI fades to 30% opacity when timer running (distraction-free UX)
- **Time picker**: Scrollable wheels (`LazyColumn` with momentum) for HH:MM:SS input
- **Mode selector**: Pill-style buttons with smooth transitions

### Screen Lifecycle
- `MainActivity` sets `FLAG_KEEP_SCREEN_ON` and transparent status bar
- `WorkoutTimerScreen` initializes `DNDManager` in `LaunchedEffect(Unit)`
- Phase change triggers sound + vibration in side effect (watches `currentPhase` state)

## Critical Workflows

### Building & Testing
```bash
# Sync Gradle (required after dependency changes)
./gradlew tasks

# Build APK
./gradlew assembleDebug

# Run on emulator/device
./gradlew installDebug
./gradlew connectedAndroidTest
```

### Adding Features (State Flow)
1. Add field to `WorkoutTimerState` data class
2. Add ViewModel function to mutate via `_uiState.update { ... }`
3. Observe in Compose via `uiState.collectAsState()` at component level
4. **Never** read `_uiState.value` in Compose—use `collectAsState()` for recomposition

### Modifying Timer Phases
- Edit phase durations in ViewModel: `workoutDuration` (30s), `restDuration` (10s), `pomodoroSessionDuration` (25 * 60)
- Update `resetState()` to recalculate `totalRounds` / `totalPomodoroSessions`
- Test phase transitions with short durations (e.g., 2s/1s) first

## Project-Specific Conventions

### Naming & Structure
- **Enums**: `AppMode`, `TimerPhase` (no prefix, clear intent)
- **ViewModel**: Single `WorkoutTimerViewModel` handles both modes (not separated for simplicity)
- **Composables**: Single file `WorkoutTimerScreen.kt`; reusable sub-components as nested lambdas (`@Composable` blocks within)
- **Package**: All code in `com.workoutclock.*` (no sub-packages except UI theme)

### State Mutations
- Use `_uiState.update { currentState -> ... }` (not `.value =`)
- Always return new state object; avoid in-place mutations
- Handle both modes in same `handlePhaseCompletion()` / `resetState()` functions (switch on `appMode`)

### Permissions & System Integration
- **DND (Focus mode)**: Check `hasPermission()` before calling `enableDND()`, prompt user if denied
- **Vibration**: Use `VibrationEffect` (API 26+) with fallback to deprecated `vibrate()` for older devices
- **Audio**: Load custom MP3s from `res/raw/` via `MediaPlayer.create(context, R.raw.*)`, manage lifecycle (call `release()`)
- **Screen on**: Set in `MainActivity.onCreate()` once (not repeatedly)

### Compose Patterns
- **Mutable state for UI-only**: Use `rememberSaveable {}` (survives config changes within screen)
- **ViewModel state**: Always use `StateFlow` observable patterns
- **Side effects**: `LaunchedEffect(key)` for setup/cleanup (e.g., sound playback, sensor access)
- **Canvas drawing**: Sweep angle for progress = `progress * 360f`, use `drawArc()` with stroke

## Integration Points & Data Flow

```
MainActivity
  └─ WorkoutTimerScreen
       ├─ viewModel: WorkoutTimerViewModel (holds state, logic)
       │    ├─ Observes user input (time picker, play/pause, mode switch)
       │    ├─ Manages timer loop (1s ticks)
       │    └─ Delegates DND to DNDManager
       ├─ DNDManager (injected via setDNDManager())
       │    └─ Enables/resets system DND (Focus mode only)
       └─ Canvas/Compose UI
            └─ Renders circular progress, buttons, time display
```

**No network requests, database, or persistence** (state is session-only; survives pause/resume via ViewModel).

## Common Pitfalls & Fixes

| Issue | Root Cause | Fix |
|-------|-----------|-----|
| Timer doesn't stop on back press | No `onCleared()` hook to cancel job | Already implemented; verify `timerJob?.cancel()` in viewModel |
| Sound plays twice on phase change | Multiple `LaunchedEffect` triggers | Move sound playback to single effect watching `currentPhase` |
| Progress bar stuck | Progress calc uses wrong phase duration | Verify `phaseDuration` matches current phase; recalc if mode changes |
| DND not resetting | `resetDND()` not called on timer stop | Call in `reset()`, `resetAndReturnState()`, and completion handler |
| Screen orientation breaks UI | Jetpack Compose recomposes on config change | Use `rememberSaveable()` for non-ViewModel UI state; ViewModel survives rotation |

## Testing Insights
- No unit tests currently; integration tests use `androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'`
- Compose UI tests via `androidx.compose.ui:ui-test-junit4`
- Quick iteration: Use emulator with short phase durations (2s/1s) to test full cycles in seconds
- DND testing requires API 31+ emulator and manual permission grant

## Resources
- Main files: [WorkoutTimerViewModel.kt](app/src/main/java/com/workoutclock/WorkoutTimerViewModel.kt), [WorkoutTimerScreen.kt](app/src/main/java/com/workoutclock/WorkoutTimerScreen.kt), [DNDManager.kt](app/src/main/java/com/workoutclock/DNDManager.kt)
- Audio/Haptic: [res/raw/](app/src/main/res/raw/)
- Gradle config: [app/build.gradle](app/build.gradle) (Compose BOM, Kotlin 1.9.22, API 34)
