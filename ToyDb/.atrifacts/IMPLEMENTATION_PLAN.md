# Implementation Plan: ToyDb KMP Application

This plan outlines the design and implementation of the **ToyDb** database CRUD application as a Kotlin Multiplatform (KMP) app targeting Android and Desktop (macOS, Windows).

---

## Architecture & Tech Stack

1. **Kotlin Multiplatform (KMP)**:
   - Shared business logic, database wrappers, models, and UI components in `commonMain`.
   - Android-specific database and filesystem access in `androidMain`.
   - Desktop-specific database, JDBC connection, and file dialogs in `desktopMain`.
2. **UI Framework & Aesthetics**: 
   - Jetbrains Compose Multiplatform using Material 3 with a premium dark-themed design system.
   - Leverage `gepetto-utils` Composable wrappers:
     - `GcTheme` to wrap the theme and styles automatically.
     - `GcScaffold` for responsive navigation placement based on orientation.
     - `GcCard` to synchronize height among grid display elements.
     - `GcGenericDialog` for CRUD/Confirmation dialogs.
     - `GcSpacing` for margins and paddings.
3. **Adaptive Shell & Navigation Bar/Rail**:
   - The shell layout is built using `GcScaffold`.
   - The navigation component is implemented using `GcNavBar`, populated with a list of `GcNavButton` items.
   - **Adaptive Placement**: In portrait mode, `GcNavBar` is rendered horizontally at the bottom of the screen. In landscape mode (Desktop or landscape Tablet/Phone), `GcNavBar` is automatically rendered vertically as a side navigation rail on the left.
4. **Adaptive Multi-Pane Navigation (nav3)**:
   - Leverage the **`gepetto-utils` navigation3 wrappers** to implement adaptive inner navigation.
   - Implement `GcNavDisplay` with a mutable `backStack` of typed destinations.
   - Use `rememberGcSceneStrategy<NavKey>()` to dynamically adjust panes according to the window width size class:
     - **Compact (Portrait phone)**: Single-pane stack navigation.
     - **Medium/Expanded (Tablet/Desktop)**: Two-pane layout (List-Detail) or Three-pane layout (List-Detail-Extra).
   - Configure scene mapping using:
     - `GcSceneStrategy.listPane()` for the list view (e.g. category explorers, maker list).
     - `GcSceneStrategy.detailPane()` for detail views (e.g. toy detail card).
     - `GcSceneStrategy.extraPane()` / `GcSceneStrategy.bottomSheetPane()` for editing forms based on orientation.
5. **Database Versioning & Migrations**:
   - Database schema changes are managed via SQLite's built-in `PRAGMA user_version`.
   - On connection initialization, the app reads the current database version.
   - If version is `0` (new DB), it builds the schemas.
   - If version is less than the current schema version, it applies sequential migration scripts.
6. **JSON Serialization & Integrity Verification**:
   - `kotlinx.serialization` for parsing/writing files.
   - Export verification tasks will cross-reference the output files against the target schemas and data sizes to check file integrity.
7. **Logging**:
   - Utilizes `club.gepetto.GcLog` for all database setup and query diagnostics.

---

## Proposed Changes

### 1. Build and Project Configuration

#### [NEW] [settings.gradle.kts](file:///Users/luizvaldetaro/valdetaro/ToyDb/settings.gradle.kts)
Defines project settings and includes the `:composeApp` module.

#### [NEW] [build.gradle.kts](file:///Users/luizvaldetaro/valdetaro/ToyDb/build.gradle.kts)
Root Gradle file configuring build dependencies and plugins.

#### [NEW] [gradle.properties](file:///Users/luizvaldetaro/valdetaro/ToyDb/gradle.properties)
Gradle configuration variables.

#### [NEW] [composeApp/build.gradle.kts](file:///Users/luizvaldetaro/valdetaro/ToyDb/composeApp/build.gradle.kts)
Module-level Gradle file targeting Android and JVM/Desktop, specifying the JVM SQLite JDBC library.

---

### 2. Database Core (`commonMain`, `androidMain`, `desktopMain`)

We will implement a custom SQL execution layer to support cross-platform SQLite queries with migrations.

#### [NEW] [Database.kt](file:///Users/luizvaldetaro/valdetaro/ToyDb/composeApp/src/commonMain/kotlin/com/gepetto/toydb/database/Database.kt)
Contains common interfaces `ToyDatabase`, `SqlCursor`, and data models for `Toy` and `Maker`. Implements version upgrade strategy.

#### [NEW] [AndroidDatabase.kt](file:///Users/luizvaldetaro/valdetaro/ToyDb/composeApp/src/androidMain/kotlin/com/gepetto/toydb/database/Database.kt)
Implements `ToyDatabase` on Android using `SQLiteOpenHelper` with built-in version callback integration.

#### [NEW] [DesktopDatabase.kt](file:///Users/luizvaldetaro/valdetaro/ToyDb/composeApp/src/desktopMain/kotlin/com/gepetto/toydb/database/Database.kt)
Implements `ToyDatabase` on JVM using standard JDBC connection to a local `.db` file, executing version checks via `PRAGMA user_version`.

---

### 3. Import / Export Service

#### [NEW] [ImportExportService.kt](file:///Users/luizvaldetaro/valdetaro/ToyDb/composeApp/src/commonMain/kotlin/com/gepetto/toydb/service/ImportExportService.kt)
Parses the JSON data to populate the database tables, and serializes database tables back to JSON.

---

### 4. Navigation & UI Screens (`commonMain`)

We will build a rich, premium, adaptive Material 3 UI.

#### [NEW] [Destination.kt](file:///Users/luizvaldetaro/valdetaro/ToyDb/composeApp/src/commonMain/kotlin/com/gepetto/toydb/ui/Destination.kt)
Sealed interface representing all screens: `Dashboard`, `CategoryExplorer`, `MakerDirectory`, `Settings`, `ToyDetail`, `EditToy`, and `AddToy`.

#### [NEW] [ToyDbNavigation.kt](file:///Users/luizvaldetaro/valdetaro/ToyDb/composeApp/src/commonMain/kotlin/com/gepetto/toydb/ui/ToyDbNavigation.kt)
Builds the app's main view using `GcNavDisplay` inside `GcScaffold` and `GcNavBar` (which renders horizontally as a bottom bar or vertically as a side navigation rail).

#### [NEW] [DashboardScreen.kt](file:///Users/luizvaldetaro/valdetaro/ToyDb/composeApp/src/commonMain/kotlin/com/gepetto/toydb/ui/DashboardScreen.kt)
Displays collection stats in `GcCard` grids.

#### [NEW] [ExplorerScreen.kt](file:///Users/luizvaldetaro/valdetaro/ToyDb/composeApp/src/commonMain/kotlin/com/gepetto/toydb/ui/ExplorerScreen.kt)
Table explorer for Slots, Trains, Static, Kits, and Misc tables with:
- Text search and scale/condition/maker filtering.
- Picture filename prefix mapping (`car123.jpg`, `tra56.png`, etc.).
- Image gallery displaying the parsed `bitmaps` field.

#### [NEW] [CrudDialog.kt](file:///Users/luizvaldetaro/valdetaro/ToyDb/composeApp/src/commonMain/kotlin/com/gepetto/toydb/ui/CrudDialog.kt)
Add/Edit form using `GcGenericDialog` or bottom sheets.

---

### 5. Platform Entries

#### [NEW] [MainActivity.kt](file:///Users/luizvaldetaro/valdetaro/ToyDb/composeApp/src/androidMain/kotlin/com/gepetto/toydb/MainActivity.kt)
Android launcher activity.

#### [NEW] [Main.kt](file:///Users/luizvaldetaro/valdetaro/ToyDb/composeApp/src/desktopMain/kotlin/Main.kt)
Desktop main launcher configuration.

---

## Verification Plan

### Automated Verification
- Verify compilation of both targets:
  - `./gradlew :composeApp:assembleDebug` (Android)
  - `./gradlew :composeApp:jvmJar` (Desktop)

### Manual Verification
1. Launch the Desktop application: `./gradlew :composeApp:run`.
2. Inspect application startup in terminal to verify `GcLog` outputs confirming the database initialized to version `1` and the adaptive info is resolved correctly.
3. Navigate to Settings and import the local JSON files.
4. Verify that the Dashboard correctly displays total item counts and financial summaries.
5. Perform CRUD operations (Add, Edit, Delete) on a slot car, and verify the changes persist.
6. Export the database to the JSON directory.

### Export Integrity Verification Script
We will write a python script [verify_export.py](file:///Users/luizvaldetaro/valdetaro/ToyDb/verify_export.py) that:
1. Validates that the newly exported JSON files are syntactically valid JSON.
2. Compares the list of records in the exported JSON files against the original JSON files to ensure no data was lost during import/export.
3. Checks that the schema fields are preserved.
