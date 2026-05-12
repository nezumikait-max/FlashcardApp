# Floating Service Guidelines

This directory contains services that manage the floating overlay UI for the Flashcard app.

## Key Component: `FloatingFlashcardService`
- **Purpose**: Displays a floating flashcard on top of other apps.
- **Tech Stack**: Uses `WindowManager` to add a `ComposeView` to the screen.
- **Lifecycle Management**: Inherits from `LifecycleService` and implements `SavedStateRegistryOwner` and `ViewModelStoreOwner` to support Jetpack Compose properly in a Service context.

## Maintenance Guidelines
- **Lifecycle Handling**: Ensure `ViewTreeLifecycleOwner`, `setViewTreeSavedStateRegistryOwner`, and `ViewTreeViewModelStoreOwner` are correctly set on the `ComposeView` to avoid crashes or leaks.
- **WindowManager Params**: The overlay uses `TYPE_APPLICATION_OVERLAY` (for API 26+) or `TYPE_PHONE`. Always check for the `SYSTEM_ALERT_WINDOW` permission (Overlay permission) before starting the service.
- **Memory Management**: Call `disposeComposition()` and `store.clear()` in `onDestroy()` to prevent memory leaks.
- **Compose UI**: The floating UI should be lightweight. Keep the `FloatingCard` composable simple and performant.

## Adding New Features to Overlay
1.  **State**: Pass necessary state through the `setContent` block in the service. The overlay now respects the user's selected category from `UserPreferencesRepository`.
2.  **Interactivity**: Remember that the window is currently set to `FLAG_NOT_FOCUSABLE`. If input (like typing) is needed, the flags must be adjusted dynamically.
3.  **Hilt**: The service is annotated with `@AndroidEntryPoint`, allowing injection of repositories (including `UserPreferencesRepository`) and other dependencies.
