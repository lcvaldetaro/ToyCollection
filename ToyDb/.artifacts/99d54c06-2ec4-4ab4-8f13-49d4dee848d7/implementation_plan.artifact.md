# Fix Settings Button Label and Localize Navigation

The "Settings" button on the main screen (navigation bar) currently changes its label between "Settings" (Landscape) and "Setup" (Portrait), and its labels are hardcoded in English. This plan will unify the label to always be "Settings" and localize all navigation labels ("Stats", "Makers", "Settings", "Info") across all supported languages.

## User Review Required

> [!IMPORTANT]
> The navigation labels will be moved from hardcoded strings in `ToyDbNavigation.kt` to localized string resources in `strings.xml`.

## Proposed Changes

### [composeApp]

#### [MODIFY] [strings.xml](file:///Users/luiz/valdetaro/ToyCollection/ToyDb/composeApp/src/commonMain/composeResources/values/strings.xml)
Add `nav_stats`, `nav_makers`, `nav_settings`, and `nav_info` strings in English.

#### [MODIFY] [strings.xml](file:///Users/luiz/valdetaro/ToyCollection/ToyDb/composeApp/src/commonMain/composeResources/values-de/strings.xml)
Add localized strings in German.

#### [MODIFY] [strings.xml](file:///Users/luiz/valdetaro/ToyCollection/ToyDb/composeApp/src/commonMain/composeResources/values-es/strings.xml)
Add localized strings in Spanish.

#### [MODIFY] [strings.xml](file:///Users/luiz/valdetaro/ToyCollection/ToyDb/composeApp/src/commonMain/composeResources/values-fr/strings.xml)
Add localized strings in French.

#### [MODIFY] [strings.xml](file:///Users/luiz/valdetaro/ToyCollection/ToyDb/composeApp/src/commonMain/composeResources/values-it/strings.xml)
Add localized strings in Italian.

#### [MODIFY] [strings.xml](file:///Users/luiz/valdetaro/ToyCollection/ToyDb/composeApp/src/commonMain/composeResources/values-pt/strings.xml)
Add localized strings in Portuguese.

#### [MODIFY] [ToyDbNavigation.kt](file:///Users/luiz/valdetaro/ToyCollection/ToyDb/composeApp/src/commonMain/kotlin/com/gepetto/toydb/ui/ToyDbNavigation.kt)
Update the navigation button definitions to use the new string resources and remove the orientation-based logic for the "Settings" label.

---

## Verification Plan

### Automated Tests
- I will check the file structure and ensure all `strings.xml` files contain the new keys.
- I will verify `ToyDbNavigation.kt` uses `stringResource`.

### Manual Verification
- The user can verify the button labels in both Portrait and Landscape orientations.
- The user can verify the labels change correctly when switching device language.
