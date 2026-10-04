# Project Rules: Toy Database Manager

## General Principles
- **Stay within the scope** Don't make changes outside of the scope of what's asked. 
- **Suggestions** If you see something outside of the scope that you think is needed, suggest to make the changes, but don't do it without confirmation.
- **Language**: Use Kotlin with functional programming patterns where applicable.
- **UI Framework**: Jetpack Compose only. No XML layouts unless specifically requested.
- **Architecture**: Follow MVVM/MVI. Keep logic out of Composables. Use ViewModels for state.

## UI & UX Standards
- **Theme Support**: Every screen MUST support both Light and Dark mode. Use `MaterialTheme.colorScheme` instead of hardcoded colors. The exception is This project uses sysColorForeground(), sysColorBackground() and isBack() for theming.
- **Previews**: Every screen or significant Composable must have `@Preview` for Light and Dark modes plus landscape.
- **Localization**: Never hardcode strings. Always use `stringResource(id = R.string.key)` and ensure strings are added to `strings.xml` and translated to all languages the app supports.
- **Adaptability**: Use Material 3 Adaptive libraries for responsive layouts (Handset, Tablet, TV).
- **Dark mode**: Every Dialog, when in dark mode, must have a border.

## Coding Standards
- **Navigation**: Use Navigation 3 (as configured in the project).
- **State Management**: Use `collectAsStateWithLifecycle()` for Flow collection in the UI.
- **Logging**: Use `GcLog` for all logging (defined in gepetto-utils library).
- **Version Catalog**: Always add new dependencies to `gradle/libs.versions.toml`.

## Performance
- **Compose Stability**: Use `@Immutable` or `@Stable` for UI data models.
- **Coroutines**: Use appropriate dispatchers (`Dispatchers.Main` for UI, `Dispatchers.IO` for disk/network).
