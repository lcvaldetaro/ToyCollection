# Implementation Plan - Preserve UI State during Navigation

The user reported that search queries and filter selections are lost when navigating away from and back to `ExplorerScreen` and `MakerDetailScreen`. This happens because these screens use `remember` for their UI state, which is cleared when the screen is removed from the composition during navigation.

## Proposed Changes

I will update the following screens to use `rememberSaveable` instead of `remember` for user-controlled UI state (search queries and filter selections). This will allow the state to be preserved in the navigation backstack.

### [Component: UI Screens]

#### [MODIFY] [ExplorerScreen.kt](file:///Users/luiz/valdetaro/ToyCollection/ToyDb/composeApp/src/commonMain/kotlin/com/gepetto/toydb/ui/ExplorerScreen.kt)
- Replace `remember { mutableStateOf("") }` with `rememberSaveable { mutableStateOf("") }` for `searchQuery`, `selectedScale`, and `selectedMaker`.
- I will also use `rememberSaveable(category)` to ensure that if the category changes, the filters are reset, but they are preserved when navigating to a toy and back within the same category.

#### [MODIFY] [MakerDetailScreen.kt](file:///Users/luiz/valdetaro/ToyCollection/ToyDb/composeApp/src/commonMain/kotlin/com/gepetto/toydb/ui/MakerDetailScreen.kt)
- Replace `remember { mutableStateOf("all") }` with `rememberSaveable { mutableStateOf("all") }` for `selectedCategory`.
- Use `rememberSaveable(makerName)` to ensure state is per-maker.

#### [MODIFY] [MakerDirectoryScreen.kt](file:///Users/luiz/valdetaro/ToyCollection/ToyDb/composeApp/src/commonMain/kotlin/com/gepetto/toydb/ui/MakerDirectoryScreen.kt)
- Replace `remember { mutableStateOf("") }` with `rememberSaveable { mutableStateOf("") }` for `searchQuery`.

## Verification Plan

### Manual Verification
1. Open `ExplorerScreen` for a category (e.g., Cars).
2. Enter a search query and select a filter (e.g., Scale 1:64).
3. Tap on a toy to go to `ToyDetailScreen`.
4. Tap back to return to `ExplorerScreen`.
5. Verify that the search query and filter selection are still present.
6. Repeat similar steps for `MakerDetailScreen` (category filter) and `MakerDirectoryScreen` (search query).
