# Gepetto's Toy Database Manager (ToyDb)

A Kotlin Multiplatform (KMP) CRUD database application designed to update, insert, delete, import, and export Gepetto's toy collections (Slot Cars, Model Trains, Static Models, Model Kits, and Miscellaneous Items) and their Manufacturers/Makers.

---

## 📱 Supported Targets

* **Desktop (JVM)**: Primary target. Native macOS (DMG) and Windows (MSI) applications.
* **Android**: Mobile application.

---

## 🛠 Tech Stack & Architecture

1. **Compose Multiplatform**: Declarative Compose UI with Material 3 styling.
2. **SQLite Database Layer**:
   - Platform-independent SQL wrapper (`ToyDatabase` and `SqlCursor`).
   - **Desktop (JVM)**: Interacts with a local SQLite database (`toydb.db`) using the JDBC driver (`org.xerial:sqlite-jdbc`).
   - **Android**: Interacts with SQLite using Android's native `SQLiteOpenHelper`.
3. **Database Migration Strategy**:
   - Manages schemas and upgrades incrementally using `PRAGMA user_version` inside SQL migrations.
4. **Adaptive Navigation & Multi-Pane Layouts**:
   - Uses `GcTheme` for automatic dark/light theme adjustments.
   - Uses `GcAdaptiveScaffold` / `GcNavBar` for dynamic shell layout adapting between a horizontal bottom bar (portrait) and vertical side rail (landscape).
   - Utilizes `androidx.navigation3` integrated with `GcSceneStrategy` for adaptive multi-pane views (single pane on mobile, List-Detail-Extra pane layout on desktop).
5. **Import / Export Service**:
   - Reads/writes JSON files conforming to the legacy database schema to ensure seamless interoperability.
   - Automatically synchronizes image metadata and sizes by scanning files directly on the local storage file system.
6. **Coil 3 & Okio**:
   - Cross-platform file manipulation and image rendering.
   - Copies manufacturer images and handles custom media directory paths configured on startup.

---

## 📂 Core Package Structure

* [**`database`**](file:///Users/luizvaldetaro/valdetaro/ToyCollection/ToyDb/composeApp/src/commonMain/kotlin/com/gepetto/toydb/database): Custom cross-platform SQL layer (`Database.kt`), migrations logic, and repository layer (`ToyRepository.kt`).
* [**`service`**](file:///Users/luizvaldetaro/valdetaro/ToyCollection/ToyDb/composeApp/src/commonMain/kotlin/com/gepetto/toydb/service): `ImportExportService` handling serialization of toy collections and manufacturer database tables.
* [**`ui`**](file:///Users/luizvaldetaro/valdetaro/ToyCollection/ToyDb/composeApp/src/commonMain/kotlin/com/gepetto/toydb/ui): App navigation and CRUD editors:
  - **Dashboard**: Stats screen summarizing count and value values.
  - **Category Explorer**: Advanced search and filter grids for toy listings.
  - **Makers Directory / Detail**: Manufacturer information and editing form.
  - **Add/Edit Toy Screens**: Complete input forms for 34 distinct toy attributes.
* [**`utils`**](file:///Users/luizvaldetaro/valdetaro/ToyCollection/ToyDb/composeApp/src/commonMain/kotlin/com/gepetto/toydb/utils): Scroll controllers and custom Coil dynamic image resolvers.

---

## ⚙️ Initial Setup & Directories

On the first launch (especially on Desktop), the app prompts the user to configure:
1. **Import / Export Directory**: Path containing the source JSON files (`makers.json`, `slots.json`, etc.).
2. **Images Directory**: Path where the catalog pictures are saved.

Once configured, the user can head to **Settings** to:
* **Import Database**: Loads all toy and maker records from the JSON files.
* **Export Database**: Serializes the current SQL database state back to JSON files.

---

## 🔍 Data Integrity Verification

The directory contains two utility python scripts to ensure data consistency and schema correctness:
* [**`verify_db.py`**](file:///Users/luizvaldetaro/valdetaro/ToyCollection/ToyDb/verify_db.py): Validates database structural sanity.
* [**`verify_export.py`**](file:///Users/luizvaldetaro/valdetaro/ToyCollection/ToyDb/verify_export.py): Validates exported JSON syntax, formats, and cross-references records with the source JSON files to prevent data loss.

---

## 🚀 Building & Running

Ensure you have Java JDK 17+ installed.

### Run Desktop (JVM)
```bash
./gradlew :composeApp:run
```

### Build Android APK (Debug)
```bash
./gradlew :composeApp:assembleDebug
```
