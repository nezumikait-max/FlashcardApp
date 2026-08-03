# Flashcard App Project Guidelines

This project is an Android application designed to help users learn with flashcards, featuring a floating overlay for quick access and a modern multitasking interface.

## Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Hilt (Dagger)
- **Local Database**: Room (Version 3)
- **Build Tool**: Gradle with Version Catalogs (`libs.versions.toml`)

## Project Structure
- `com.example.flashcardapp.data`: Room entities (including `category` field and `isInTrash` state), DAOs, and database configuration.
- `com.example.flashcardapp.di`: Hilt modules for dependency injection.
- `com.example.flashcardapp.repository`: Data source abstractions, including `UserPreferencesRepository` for DataStore.
- `com.example.flashcardapp.service`: `FloatingFlashcardService` for the overlay UI and Quick-Create Sidebar.
- `com.example.flashcardapp.viewmodel`: UI state management for both the main app and settings.

## Key Features
- **Modern Dashboard**: Full CRUD with keyword search, category filtering (with badges), and swipe-to-trash gestures.
- **Recycle Bin**: A dedicated section to restore or permanently delete flashcards (Room Version 3).
- **Study Mode**: Immersive review with 3D flip animations, horizontal transitions, and session summaries.
- **Floating Overlay**:
    - **Adaptive Floating Cards**: Draggable, resizable, and auto-closing (configurable timer).
    - **Quick-Create Sidebar**: Swipe-triggered edge handle for rapid card entry without leaving current apps.
    - **Settings-Aware Handle**: The sidebar handle automatically becomes visible when in Settings mode to facilitate positioning.
- **Scalable Settings**: Built with `LazyColumn` to ensure all sliders (Auto-close, Interval, Sidebar height/offset) are accessible on any screen size.
- **Material 3 UX**: Full Dark Mode support with glassmorphism UI elements.

## Testing
- Unit tests are located in `app/src/test`.
- Run tests using `./gradlew test`.
- Includes tests for `FlashcardViewModel` and `FlashcardRepository`.

## Coding Conventions
- **UI**: Use Jetpack Compose for all UI components. Follow Material 3 guidelines and use the established `GlassCard` and `GlassTextField` patterns.
- **Dependency Injection**: Always use Hilt.
- **Asynchronous Work**: Use Kotlin Coroutines and Flow for all data operations.
- **Overlay Management**: Use `WindowManager` via `FloatingFlashcardService`. Ensure `Lifecycle` is correctly handled for Compose views in services.

## Workflows
- **Database Changes**: Update the `Flashcard` entity. If schema changes occur, increment `AppDatabase` version and provide migrations.
- **Settings Propagation**: Preferences are managed via `UserPreferencesRepository` and reacted to in real-time by the service.

## References
- [Android Developers - Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Android Developers - Room](https://developer.android.com/training/data-storage/room)
- [Dagger Hilt Guide](https://dagger.dev/hilt/)
