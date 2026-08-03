# Flashcard App

A modern, multitasking-focused Android flashcard application built with Jetpack Compose. Study efficiently while using other apps with our advanced floating UI.

## 🚀 Features

- **Floating Study UI**: Overlay flashcards on top of any app. Includes draggable, resizable cards with auto-close timers.
- **Quick-Create Sidebar**: Swipe from the edge of your screen to quickly add a new flashcard without switching apps.
- **Recycle Bin**: Safety first! Swipe cards to the trash and restore them later if needed.
- **Study Mode**: Immersive full-screen study experience with 3D flip animations and progress tracking.
- **Organization**: Categorize cards into study groups with real-time badges and keyword search.
- **Customizable**: Adjust appearance intervals, sidebar positions, and more in the modern Settings dashboard.
- **Dark Mode**: Beautiful Material 3 Dark Theme with glassmorphism effects.

## 🛠 Tech Stack

- **UI**: Jetpack Compose (Material 3)
- **Architecture**: MVVM with Clean Architecture principles
- **Dependency Injection**: Hilt (Dagger)
- **Database**: Room Persistence Library
- **Persistence**: Jetpack DataStore (User Preferences)
- **Concurrency**: Kotlin Coroutines & Flow
- **Background**: LifecycleService for persistent overlays

## 📂 Project Structure

- `app/src/main/java/com/example/flashcardapp/data`: Room entities, DAOs, and Database.
- `app/src/main/java/com/example/flashcardapp/repository`: Data source management and preferences.
- `app/src/main/java/com/example/flashcardapp/service`: Floating UI and Sidebar logic.
- `app/src/main/java/com/example/flashcardapp/viewmodel`: Application logic and state management.
- `app/src/main/java/com/example/flashcardapp/MainActivity.kt`: UI screens (Dashboard, Study, Settings, Trash).

## 📥 Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/nezumikait-max/FlashcardApp.git
   ```
2. Open the project in Android Studio (Iguana or newer recommended).
3. Build and run the app on an Android device or emulator (Android 8.0+ required for overlays).

## 🛠 Setup

- **Overlay Permission**: Ensure you grant "Display over other apps" permission when prompted to enable the Floating UI and Sidebar features.

---
*Developed as a demonstration of modern Android development practices, multitasking services, and Jetpack Compose.*
