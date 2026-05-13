# Flashcard App Project Guidelines

This project is an Android application designed to help users learn with flashcards, featuring a floating overlay for quick access.

## Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Hilt (Dagger)
- **Local Database**: Room
- **Build Tool**: Gradle with Version Catalogs (`libs.versions.toml`)
## Project Structure
- `com.example.flashcardapp.data`: Room entities (including `category` field), DAOs, and database configuration (Version 2).
- `com.example.flashcardapp.di`: Hilt modules for dependency injection.
- `com.example.flashcardapp.repository`: Data source abstractions.
- `com.example.flashcardapp.service`: Background services, including the `FloatingFlashcardService`. See [service/GEMINI.md](app/src/main/java/com/example/flashcardapp/service/GEMINI.md) for detailed service guidelines.
- `com.example.flashcardapp.viewmodel`: UI state management, including category filtering.

## Features
- **Dashboard**: Full CRUD for flashcards, Keyword Search, Category filtering with real-time badges, and modern Swipe-to-Delete gestures.
- **Study Mode**: Shuffled review mode with smooth 3D flip animations and horizontal card transitions. Includes a session completion summary.
- **Floating UI**: Fully draggable overlay service that stays in sync with the app's category selection and supports multitasking.
- **Modern UX**: Full Material 3 Dark Mode support that adapts to system settings.
- **Persistence**: User preferences (like the last selected category) are persisted via Jetpack DataStore.

## Testing
- Unit tests are located in `app/src/test`.
- Run tests using `./gradlew test`.
- Includes tests for `FlashcardViewModel` (filtering, card management) and `FlashcardRepository`.

## Coding Conventions
...

- **UI**: Use Jetpack Compose for all new UI components. Follow Material 3 guidelines.
- **Dependency Injection**: Always use Hilt for providing dependencies. Avoid manual instantiation where possible.
- **Asynchronous Work**: Use Kotlin Coroutines and Flow for database operations and asynchronous tasks.
- **Resources**: Define strings, colors, and dimensions in the appropriate `res` directories, although Compose-based styling is preferred within the code where applicable.

## Workflows
- **Database Changes**: Update the `Flashcard` entity and `FlashcardDao` for any data schema changes. Ensure `AppDatabase` version is incremented if necessary (and migrations provided if required).
- **Service Management**: The `FloatingFlashcardService` handles the overlay UI. Changes to the overlay logic should be centralized here.

## References
- [Android Developers - Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Android Developers - Room](https://developer.android.com/training/data-storage/room)
- [Dagger Hilt Guide](https://dagger.dev/hilt/)
