# 🏋️ Workout Clock - Minimalistic Timer App

A sleek, minimalistic Android timer app built with **Kotlin** and **Jetpack Compose**, featuring both workout intervals and Pomodoro-style focus sessions with automatic Do Not Disturb management.

## ✨ Features

### 🏃 **Workout Mode**
- **Interval Training**: 30-second workout periods followed by 10-second rest periods
- **Custom Duration**: Set any total workout time using the intuitive time picker
- **Round Tracking**: Visual display of current round progress
- **Audio & Haptic Feedback**: Custom sounds (`workout_beep.mp3`, `rest_beep.mp3`) and vibration for phase transitions

### 🍅 **Focus Mode (Pomodoro)**
- **True Pomodoro Cycles**: 25-minute focus sessions followed by 5-minute breaks
- **Automatic Cycles**: Seamlessly transitions through multiple sessions until your custom duration is complete
- **Smart DND Integration**: Automatically enables Do Not Disturb during focus sessions and disables during breaks
- **Session Tracking**: Shows current session (e.g., "Session 2 of 4")
- **Minimum Duration**: Enforces 30-minute minimum to ensure proper Pomodoro cycles

### 🎨 **User Interface**
- **Black & White Theme**: Clean, distraction-free minimalistic design
- **Circular Progress**: Beautiful gradient progress indicator that changes color by mode
- **Pill-Style Mode Selector**: Smooth transitions between Workout and Focus modes
- **Swipe Gestures**: Swipe left/right on the timer to quickly switch modes
- **Animated Opacity**: UI elements fade to 30% opacity when timer is running for minimal distraction
- **Custom Time Picker**: Scrollable HH:MM:SS wheels for precise time selection

### 🔧 **Technical Features**
- **Modern Architecture**: Built with Kotlin, Jetpack Compose, and MVVM pattern
- **State Management**: Reactive UI with StateFlow and Compose state management
- **Background Processing**: Proper coroutine handling for timer functionality
- **Permission Handling**: Smart DND permission requests and management
- **Audio Management**: Custom sound files with MediaPlayer integration
- **Haptic Feedback**: Platform-appropriate vibration patterns

## 🏗️ **Architecture**

```
app/
├── src/main/java/com/workoutclock/
│   ├── MainActivity.kt              # Entry point
│   ├── WorkoutTimerScreen.kt        # Main UI components
│   ├── WorkoutTimerViewModel.kt     # Business logic & state management
│   └── DNDManager.kt               # Do Not Disturb functionality
├── res/
│   ├── raw/
│   │   ├── workout_beep.mp3        # Workout phase sound
│   │   └── rest_beep.mp3           # Rest/break phase sound
│   └── values/
│       └── themes.xml              # App theming
└── AndroidManifest.xml             # Permissions & configuration
```

## 🚀 **Getting Started**

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK API 21+ (Android 5.0+)
- Kotlin 1.8+

### Installation
1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd workout-clock
   ```

2. Open the project in Android Studio

3. Sync Gradle files

4. Run the app:
   ```bash
   ./gradlew assembleDebug
   ```

## 🎯 **Usage Examples**

### Workout Session
- Set duration (e.g., 10 minutes)
- Select **Workout** mode
- Tap play to start 30s/10s intervals
- Audio cues and haptic feedback guide your workout

### Focus Session
- Set duration (e.g., 60 minutes) 
- Select **Focus** mode
- Tap play to start first 25-minute focus session
- DND automatically activates
- After 25 minutes, enjoy a 5-minute break (DND off)
- Automatically continues to next focus session
- **Result**: 2 complete Pomodoro cycles (25+5+25+5)

### Quick Mode Switching
- **Swipe right** on timer → Switch to Workout mode
- **Swipe left** on timer → Switch to Focus mode
- Or use the pill selector buttons

## 🔐 **Permissions**

- **Do Not Disturb Access**: Required for Focus mode DND automation
- **Vibration**: For haptic feedback during phase transitions

## 🛠️ **Build Configuration**

The app uses Gradle with the following key dependencies:
- **Jetpack Compose**: Modern UI toolkit
- **Lifecycle Components**: ViewModel and state management
- **Kotlin Coroutines**: Asynchronous programming
- **Material 3**: Design system components

## 📱 **System Requirements**

- **Minimum SDK**: API 21 (Android 5.0)
- **Target SDK**: API 34 (Android 14)
- **Architecture**: Supports all Android architectures

## 🎨 **Design Philosophy**

This app follows a **minimalistic design philosophy**:
- **Distraction-free**: Clean black background with white text
- **Intuitive**: Swipe gestures and clear visual feedback
- **Focused**: UI fades during active sessions to minimize distractions
- **Efficient**: Quick mode switching and smart automation features

## 🔮 **Future Enhancements**

- [ ] Custom interval durations for workout mode
- [ ] Long break intervals (15-30 min) after multiple Pomodoro cycles
- [ ] Statistics and session history tracking
- [ ] Widget support for quick timer access
- [ ] Custom sound selection
- [ ] Dark/light theme options

## 📄 **License**

This project is open source. Feel free to use, modify, and distribute as needed.

---

**Built with ❤️ using Kotlin & Jetpack Compose**
- **Visual Progress**: Circular progress indicator showing current phase progress
- **Vibration Feedback**: Phone vibrates when switching between phases
- **Round Counter**: Shows current round and total rounds (45 total)
- **Total Timer**: Displays remaining total workout time

## Technical Details

- **Tech Stack**: Native Android app using Kotlin and Jetpack Compose
- **Architecture**: MVVM pattern with ViewModel and StateFlow
- **UI**: Material Design 3 with custom black/white theme
- **Permissions**: Wake lock and vibration permissions for better user experience

## Project Structure

```
app/
├── src/main/
│   ├── java/com/workoutclock/
│   │   ├── MainActivity.kt              # Main activity
│   │   ├── WorkoutTimerScreen.kt        # Main UI screen
│   │   ├── WorkoutTimerViewModel.kt     # Business logic and state
│   │   └── ui/theme/                    # Theme and styling
│   ├── res/                             # Resources (strings, colors, themes)
│   └── AndroidManifest.xml             # App configuration
└── build.gradle                         # App dependencies
```

## How to Run

1. **Prerequisites**: Install Android Studio and Android SDK
2. **Open Project**: Open this directory in Android Studio
3. **Sync**: Let Gradle sync the project
4. **Run**: Connect an Android device or start an emulator, then click Run

## Timer Logic

- **Total Duration**: 30 minutes (1800 seconds)
- **Cycle Duration**: 40 seconds (30s workout + 10s rest)
- **Total Rounds**: 45 cycles
- **Auto-switching**: Timer automatically alternates between workout and rest phases
- **Visual Feedback**: White circular progress bar shows current phase progress
- **Audio/Vibration**: Phone vibrates when switching phases

## Controls

- **Play/Pause Button**: Start, pause, or resume the timer
- **Reset Button**: Reset timer to initial state (30 minutes)

The app maintains state during pause and can be resumed at any time. The screen orientation is locked to portrait for consistent experience during workouts.

---
### Thank You for Reading!
If you have any questions or suggestions, feel free to open an issue or contribute to the project.

Signed: [John-Livingprooff](https://johnlivingprooff.vercel.app)