# How the Toy Database Manager Works

This document provides a technical overview of the architecture and operation of **Gepetto's Toy Database Manager (ToyDb)**. It is based on source code version **1.0.28 (code 28)**.

Related documentation files:
- `.agents/AGENTS.md`: Workspace rules, coding standards, and project mapping.
- `README.md`: Targets, architecture summary, initial setup, and execution commands.
- `SCHEMA.md`: Comprehensive relational database schema documentation.
- `.agents/TODO.txt`: Backlog and completed development tasks.
- `../ToyCollection/`: Companion application that displays toy collections for public viewing.

---

## 1. System Capabilities

The Toy Database Manager is a **Kotlin Multiplatform (KMP) database and catalog management application**. It maintains, updates, imports, exports, and publishes collector toy collections and manufacturer directories:

- **Entity Database**:
  - Manages five standard toy categories: **Slot Cars** (`slot`), **Model Trains** (`train`), **Static Models** (`static`), **Model Kits** (`kit`), and **Others / Miscellaneous** (`misc`).
  - Supports dynamic creation, customization, and deletion of custom toy categories.
  - Maintains a complete directory of **Manufacturers** (`makers`) with country of origin, historical comments, and secondary logo/factory image bitmaps.
- **Toy Specifications (34 Attributes)**:
  - Tracks identifiers, physical specifications, mechanical parts, acquisition history, financial valuation, and restoration details.
  - Records condition grading (`condition`), packaging status (`boxed`), reproduction status (`repro`), and trade flags (`traded`).
- **Dynamic Category Management**:
  - Maps each category to a unique identifier (`category`), file prefix rule (`image_prefix`), navigation label (`label`), display title (`title`), and icon name (`icon`).
- **Media and Image Handling**:
  - Primary image file resolution follows prefix rules (for example, `car1444.png`, `tra56.jpg`).
  - Multi-image support: secondary image filenames are stored as space-separated lists (`bitmaps`), with synchronized file sizes (`bitmaps_size`) and modification timestamps (`bitmaps_timestamp`).
  - On-demand image hydration: the application automatically downloads missing or outdated images over HTTP from a remote web host.
- **Cascading Manufacturer Updates**:
  - Supports renaming manufacturers with safe referential integrity.
  - Automatically updates `body_maker`, `chassis_maker`, `motor_maker`, and recalculates composite `maker_combo` strings across all affected toy records.
  - Detects duplicate names and prompts for confirmation with affected record counts.
- **Backup & Restore**:
  - Exports a complete collection archive (`.zip`) containing all database records and photos.
  - Streams large archives (~1.5 GB photos and JSON data) without holding the archive in memory.
  - Restores collections cleanly with full database replacement, atomic rollback on failure, and live UI refresh.
- **Create Website Pages**:
  - Generates static website pages containing category index pages (`[prefix]maker.html`), manufacturer gallery pages (`[prefix]m_[index].html`), and single toy detail pages (`[prefix]_[ref_num].html`).
  - Automatically calculates collection statistics (factory models, reproductions, scale distribution).
- **Synchronization Subsystems**:
  - **Remote Web HTTP Synchronization**: Automatically verifies remote JSON backups via HTTP, checks server modification timestamps and SHA-256 hashes, and imports updated data.
  - **Cloud SFTP Synchronization**: Synchronizes images, JSON documents, and website files with a remote SSH/SFTP server. Computes smart differential plans (new files, modified file sizes, newer timestamps) and shows transfer progress.

---

## 2. Architecture & Modules

The application is built using **Compose Multiplatform** and **Kotlin Multiplatform (KMP)**.

### Target Platforms
- **Desktop (JVM 21)**: macOS (Intel and Apple Silicon DMG) and Windows (MSI).
- **Android**: Phones and tablets (minSdk 24, compileSdk 37, targetSdk 37).
- **Web (WasmJs)**: Modern web browsers with WebAssembly GC (Chrome, Edge, Firefox, Safari).

### Source Tree Layout

```
ToyDb/
├── composeApp/
│   ├── src/
│   │   ├── commonMain/
│   │   │   ├── kotlin/com/gepetto/toydb/
│   │   │   │   ├── database/       # Database interfaces, models, SQL schema, migrations, ToyRepository
│   │   │   │   ├── platform/       # Expect declarations (BackupArchive, BackupFileHelper, StoragePermission)
│   │   │   │   ├── service/        # BackupRestoreService, ImportExport, HTML generator, SFTP contract, HTTP sync
│   │   │   │   ├── ui/             # Compose UI screens, Nav3 navigation, BackupRestoreCard, forms, dialogs
│   │   │   │   └── utils/          # Cross-platform file/directory dialogs, image resolvers, scroll utilities
│   │   │   └── composeResources/   # Localized strings (en, pt, de, es, fr, it), icons, default SQLite database
│   │   ├── desktopMain/
│   │   │   ├── kotlin/
│   │   │   │   ├── Main.kt         # Desktop application entry point, window management, headless CLI commands
│   │   │   │   └── com/gepetto/toydb/
│   │   │   │       ├── database/   # DesktopToyDatabase (JDBC sqlite-jdbc implementation)
│   │   │   │       ├── platform/   # Desktop actuals (native zip streaming, Swing file chooser)
│   │   │   │       ├── service/    # DesktopSftpService (SSHJ implementation)
│   │   │   │       └── utils/      # Desktop platform implementations
│   │   │   └── resources/          # Application icons (.icns, .ico, .png)
│   │   ├── androidMain/
│   │   │   ├── kotlin/com/gepetto/toydb/
│   │   │   │   ├── AppMainActivity.kt # Android Activity, BouncyCastle initialization, GcAppInfo context setup
│   │   │   │   ├── database/       # AndroidToyDatabase (Android SQLite framework implementation)
│   │   │   │   ├── platform/       # Android actuals (MediaStore, scoped storage, zip streaming)
│   │   │   │   ├── service/        # AndroidSftpService (SSHJ implementation for Android)
│   │   │   │   └── utils/          # Android platform implementations
│   │   │   └── AndroidManifest.xml # Android permissions (Internet, Network State, Storage maxSdkVersion 28)
│   │   └── wasmJsMain/
│   │       ├── kotlin/
│   │       │   ├── Main.kt         # WebAssembly application entry point, tab title management, ComposeViewport
│   │       │   └── com/gepetto/toydb/
│   │       │       ├── database/   # WasmToyDatabase (sql.js + IndexedDB snapshot persistence)
│   │       │       ├── platform/   # Web actual stubs (unsupported feature placeholders)
│   │       │       ├── service/    # WebSftpService (disabled stub), ImportExportServiceWasm
│   │       │       └── utils/      # Web platform implementations (NoFileSystem, ImageResolver, etc.)
│   │       └── resources/          # index.html web shell
│   ├── packaging/                  # macOS packaging scripts and background artwork
│   ├── wix/                        # Windows WiX MSI packaging definitions
│   └── build.gradle.kts            # Multiplatform build configuration, dependencies, and packaging tasks
└── json/                           # Reference dataset for testing and headless verification
```

---

## 3. Database Layer & Schema

The persistence layer uses a custom platform-independent database abstraction (`ToyDatabase` and `SqlCursor`). It avoids heavy ORM dependencies to ensure fast startup and reliable cross-platform execution.

### Database Abstraction (`database/Database.kt`)
- `interface ToyDatabase`: Exposes `execute(sql, bindArgs)`, `query(sql, bindArgs): SqlCursor`, and atomic transaction execution `transaction(block: () -> T): T` with automatic rollback on error.
- `interface SqlCursor`: Provides `next()`, `getString(col)`, `getInt(col)`, `getDouble(col)`, and `close()`.
- **Desktop Implementation** (`DesktopDatabase.kt`): Uses `java.sql.Connection` and `org.xerial:sqlite-jdbc`.
- **Android Implementation** (`AndroidDatabase.kt`): Uses `android.database.sqlite.SQLiteDatabase`.
- **Web Implementation** (`WasmDatabase.wasmJs.kt`): Uses `sql.js` (WebAssembly SQLite) with debounced automatic snapshot exports persisted to browser `IndexedDB` (`toydb_web` database).

### Schema Version & Migrations
The database version is tracked using `PRAGMA user_version` (current: `DATABASE_VERSION = 9`).
- **Version 1**: Initial creation of `category_settings`, `makers`, and `toys` tables.
- **Version 2**: Creation of the `app_settings` key-value table.
- **Version 3**: Updated display label for category `misc` to `'Others'`.
- **Version 4**: Added `year_made`, `number`, and `my_comments` columns to table `toys`.
- **Version 5**: Added column `title` to table `category_settings`.
- **Version 6**: Ensured non-empty default titles for all pre-populated categories.
- **Version 7**: Added column `icon` to table `category_settings`.
- **Version 8**: Backfilled default vector icon names (`car`, `train`, `build`, `category`) into `category_settings`.
- **Version 9**: Seeded default `base_url` (`https://gepetto.club/database/`, matching Toy Collection's default) into `app_settings`.

### Core Database Tables

```sql
-- 1. Category Settings Table
CREATE TABLE IF NOT EXISTS category_settings (
    category TEXT PRIMARY KEY,
    image_prefix TEXT NOT NULL,
    label TEXT NOT NULL,
    title TEXT NOT NULL DEFAULT '',
    icon TEXT NOT NULL DEFAULT 'category'
);

-- 2. Manufacturers Table
CREATE TABLE IF NOT EXISTS makers (
    name TEXT PRIMARY KEY,
    country TEXT,
    bitmaps TEXT,
    bitmaps_size TEXT,
    bitmaps_timestamp TEXT,
    comments TEXT
);

-- 3. Master Toys Table
CREATE TABLE IF NOT EXISTS toys (
    ref_num INTEGER NOT NULL,
    toy_type TEXT NOT NULL REFERENCES category_settings(category),
    description TEXT NOT NULL,
    maker_combo TEXT,
    scale TEXT,
    factory_car TEXT DEFAULT 'n',
    body_maker TEXT,
    acquired TEXT,
    chassis_type TEXT,
    chassis_maker TEXT,
    condition TEXT,
    color TEXT,
    motor_maker TEXT,
    motor_details TEXT,
    catalog_number TEXT,
    comments TEXT,
    major_work TEXT,
    minor_work TEXT,
    repro TEXT,
    value REAL DEFAULT 0.0,
    amount_paid REAL DEFAULT 0.0,
    amount_sold TEXT,
    traded TEXT,
    buy TEXT,
    maintenance TEXT,
    to_make TEXT,
    detail TEXT,
    boxed TEXT DEFAULT 'n',
    picture TEXT,
    picture_size INTEGER DEFAULT 0,
    picture_timestamp INTEGER DEFAULT 0,
    has_picture TEXT DEFAULT 'n',
    bitmaps TEXT,
    bitmaps_size TEXT,
    bitmaps_timestamp TEXT,
    year_made TEXT DEFAULT '',
    number TEXT DEFAULT '',
    my_comments TEXT DEFAULT '',
    PRIMARY KEY (toy_type, ref_num)
);

-- 4. Application Settings Table
CREATE TABLE IF NOT EXISTS app_settings (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);
```

### Cascading Manufacturer Renaming
When a user renames a manufacturer in `ToyRepository.renameMaker(oldName, updatedMaker)`:
1. Verifies that the new name is not blank and does not collide with an existing manufacturer.
2. Counts all affected toys using `getAffectedToysCountForMaker(oldName)` across `body_maker`, `chassis_maker`, `motor_maker`, and `maker_combo`.
3. Updates the primary key record in the `makers` table.
4. Executes a single atomic SQL update across all matching toy records to replace old manufacturer references.
5. Recalculates and updates the composite `maker_combo` field using the standard application rule:
   - If `body_maker == chassis_maker`: uses `body_maker`.
   - If one maker is blank: uses the non-blank maker.
   - If both makers exist and differ: formats as `"{chassis_maker}/{body_maker}"`.

---

## 4. Media & Image Architecture

### Image Resolution Algorithm (`utils/ImageResolver.kt`)
The primary picture of a toy does not always use the `.jpg` extension. To resolve an image:
1. `resolveToyPictureFilename(toy, imagePrefix, targetDir, imagesDir, fs)`:
   - Cleans leading directory separators and converts to lowercase.
   - If `toy.picture` contains an extension, it checks if that file exists in the active data directory or the images directory.
   - If the exact file is not present, it checks candidate extensions (`.png`, `.jpg`, `.jpeg`, `.gif`, `.webp`) on disk.
   - If `toy.picture` has no extension or is blank, it checks disk for `{imagePrefix}{refNum}.*`.
   - If no candidate exists on disk, it falls back to `{imagePrefix}{refNum}.jpg`.

### Secondary Images (Bitmaps)
- Manufacturers and toys can reference multiple secondary pictures.
- Stored as space-separated lists in `bitmaps` (for example, `"car1444_chassis.jpg car1444_box.png"`).
- Helper method `Toy.getSecondaryImages()` parses strings into `List<ToyImage>` objects containing filename, byte size, and timestamp.
- When exporting to JSON, `ImportExportService.processBitmaps()` verifies local files, reads their filesystem metadata, and updates sizes and timestamps.

### Lazy On-Demand Image Hydration (`ui/SyncImage.kt`)
`SyncImage` displays images using `GcImage` from `gepetto-utils`. If an image is missing or outdated:
1. Compares local file modification timestamp with `pictureTimeStamp` (or `bitmapsTimeStamp`).
2. If missing or the remote timestamp is newer:
   - Reads the configured HTTP `base_url` from `app_settings`.
   - Initiates an asynchronous HTTP GET request using Ktor.
   - Writes downloaded bytes directly to the local image storage directory.
   - Triggers UI recomposition immediately upon completion.

---

## 5. Import, Export, & Publishing Pipelines

### JSON Data Serialization (`service/ImportExportService.kt`)
The application supports import and export interoperability with the legacy file format.

| Content | Export Filename | Database Table |
| :--- | :--- | :--- |
| Manufacturers | `carmaker.json` (or `makers.json`) | `makers` |
| Category Definitions | `category_settings.json` | `category_settings` |
| Slot Cars | `carlist.json` | `toys` (`toy_type = 'slot'`) |
| Model Trains | `tralist.json` | `toys` (`toy_type = 'train'`) |
| Static Models | `stalist.json` | `toys` (`toy_type = 'static'`) |
| Model Kits | `plalist.json` | `toys` (`toy_type = 'kit'`) |
| Miscellaneous Toys | `mislist.json` | `toys` (`toy_type = 'misc'`) |

Every exported JSON document contains an envelope with generation metadata:
- `date`: Formatted date string (for example, `"October 4, 2026"`).
- `buildNumber`: Application versionCode string.
- `makers`, `settings`, or `cars`: Serialized entity payload array.

### Fixed-Width LST Backfill (`backfillFromLstFiles`)
Before exporting HTML, the service inspects the export directory for legacy fixed-width `.lst` files (such as `carlist.lst` or `tralist.lst`):
1. Detects table column positions using the dashed separator line (`--- ---`).
2. Locates column bounds for `Reg.` (reference number), `Made` (year made), `#` (racing number), and `My comments`.
3. Parses each line according to column bounds and updates matching records in SQLite.

### Static HTML Website Generation (`exportHtml`)
The export function creates a complete static website:
1. **Category Index Page** (`{prefix}maker.html`):
   - Displays a grid of all manufacturers with associated toy counts.
   - Summarizes category collection statistics: total toys, count of factory models, count of reproductions, and distribution by scale (`1/28 or bigger`, `1/32`, `O scale`, `HO scale`, `N scale or smaller`).
2. **Brand Gallery Pages** (`{prefix}m_{index}.html`):
   - Lists all toys for a specific manufacturer.
   - Displays thumbnail links, descriptions, reproduction markers, and manufacturer comments.
3. **Single Toy Pages** (`{prefix}_{ref_num}.html`):
   - Detailed page containing 320px hero image, link to full image, actual vs. similar model indicator, and secondary bitmap gallery.
   - Tabular specifications: scale, catalog number, brand, chassis configuration, motor details, color, year made, racing number, boxed status, and comments.
   - Standard chassis convention legend explaining chassis coding letters.

### Backup & Restore Pipeline (`service/BackupRestoreService.kt`)

The Backup & Restore pipeline creates and restores complete collection archives (`.zip`). It streams large collections containing thousands of photos (~1.5 GB) without loading entire archive files into memory.

#### Archive File Layout
The backup archive uses the standard ZIP format with the following entry structure:

```
backup.zip
├── manifest.json            # Backup envelope metadata and entity counts
├── photos.json              # Photo modification timestamps index
├── data/
│   ├── category_settings.json # Category rules and display titles
│   ├── carmaker.json        # Manufacturer records
│   ├── app_settings.json    # Portable configuration settings
│   └── carlist.json         # Toy entities for category (named in manifest)
└── images/
    ├── car101.jpg           # Master and secondary toy photographs
    └── m_scx.jpg            # Manufacturer logo and factory pictures
```

#### Settings Filter Rules
To prevent host configuration pollution and security leaks, the backup pipeline excludes non-portable keys:
- **Paths Excluded**: `data_path`, `images_path`, `import_export_path`, `window_width`, `window_height`, `window_x`, `window_y`.
- **Secrets Excluded**: `sftp_host`, `sftp_port`, `sftp_user`, `sftp_password`, `sftp_key_path`, `sftp_passphrase`, `sftp_remote_path`.
- **Portable Settings Preserved**: `app_title`, `theme_mode`, `base_url`.

#### Concurrency and Thread Safety (`CollectionWriteLock`)
To prevent concurrent data mutation during sync or restore procedures:
- `CollectionWriteLock` (`service/CollectionWriteLock.kt`) provides a shared application mutex.
- `prepareBackup` acquires the lock to capture a consistent point-in-time database snapshot.
- `restoreBackup` and `HtmlSyncService.syncIfNewer` acquire the lock for the full duration of data replacement.

#### Restore Process & Execution Order
Restoring a collection follows a strict sequence:
1. **Archive Validation**: `readBackup` verifies `manifest.json`, `photos.json`, and all JSON documents in `data/`. If any entry is corrupt or missing, validation fails and nothing is modified.
2. **Photo Extraction**: `BackupArchive.extractPhotos` streams image entries into the image directory using temporary `.partial` files. It sets disk timestamps using `photos.json`.
3. **Atomic Database Transaction**:
   - Clears existing records from `toys`, `makers`, `category_settings`, and portable `app_settings`.
   - Imports category settings from `data/category_settings.json`.
   - Imports manufacturers from `data/carmaker.json`.
   - Imports portable settings from `data/app_settings.json`.
   - Imports toys for each category mapped in `manifest.categoryFiles`.
   - Writes sync markers (`html_sync_imported_date_*` and `html_sync_imported_hash_*`) matching the restored JSON files. This prevents the startup web sync from immediately overwriting restored data.
4. **Cache Invalidation & UI Refresh**: Clears Coil image memory and disk caches, updates runtime image paths, and increments UI refresh triggers.

#### Known Limitations
1. **Concurrent File Deletions**: If an external file manager removes an image file while a backup runs, the archive omits that missing photo and logs a warning.
2. **Coil Image Caching**: Active UI screens may hold cached image bitmaps until the user navigates away or refreshes the view.
3. **Android Storage Constraints**: Devices with less free storage space than the required extraction threshold trigger a low-storage notification.
4. **Web Platform Support**: Web browsers do not expose native local file systems for streaming large ZIP archives; this feature is hidden on WebAssembly targets.
5. **Monetary Precision**: Currency values follow standard IEEE 754 floating-point serialization in JSON documents.
6. **Unassigned Toy Categories**: Toys with unrecognized category identifiers are logged and excluded from category JSON lists.
7. **Server-Specific Categories**: Categories that exist only on remote web servers are not present in local backup archives.

---

## 6. Remote Synchronization Subsystems

### The Base URL Configuration (`base_url`)
The **Base URL** setting (configured under **Settings** $\rightarrow$ **Remote Web Synchronization**, stored in `app_settings` with the key `'base_url'`, and defaulting to `https://gepetto.club/database/`, identical to the Toy Collection companion app) defines the root HTTP/HTTPS web address hosting published collection assets and JSON database backups.

The application uses the Base URL for two distinct operations:

#### 1. Automated and Manual Database Synchronization (`service/HtmlSyncService.kt`)
`HtmlSyncService` ensures that client devices stay synchronized with the latest master collection data:
- **Execution Triggers**:
  - **Startup Synchronization**: Runs automatically in the background on application startup inside `ToyDbNavigation`.
  - **Manual Synchronization**: Triggered when the user clicks the **Run Web Sync Now** button in **Settings**.
- **Remote Files Downloaded**:
  - `category_settings.json`: Contains category rules, prefixes, and titles.
  - `carmaker.json` (or fallback `makers.json`): Contains manufacturer directory records.
  - `{prefix}list.json`: Category toy lists dynamically resolved for each active category (e.g., `carlist.json`, `tralist.json`, `stalist.json`, `plalist.json`, `mislist.json`).
- **Differential Verification**:
  - Computes the SHA-256 hash of each downloaded JSON file.
  - Parses the `date` string from the JSON envelope (e.g., `"October 4, 2026"`).
  - Compares the remote timestamp and hash against local values stored in `app_settings` (`html_sync_imported_date_*` and `html_sync_imported_hash_*`).
- **Import Execution**:
  - If any remote file is newer than the local record, if the hash differs, or if the local `toys` table has zero records (such as on a fresh installation):
  - The service holds `CollectionWriteLock` and executes a clean transaction (`db.transaction`): deletes existing records in `toys`, `makers`, and `category_settings`, imports the downloaded data, and updates the local metadata timestamps and hashes in `app_settings`. If an import error occurs, the database transaction rolls back automatically, leaving the collection unchanged.

#### 2. On-Demand Lazy Image Hydration (`ui/SyncImage.kt`)
The Base URL enables lazy media loading without requiring a full upfront download of multi-gigabyte photo archives:
- **Execution Triggers**:
  - Active whenever a toy is displayed in `ToyDetailScreen` or `ExplorerScreen`, or when manufacturer logos and factory photos are viewed in `MakerDetailScreen`.
- **Condition Check**:
  - The UI evaluates whether the required image (primary picture or secondary bitmap) exists in local disk storage.
  - If the file exists, it compares the file's last modified timestamp on disk against the expected timestamp recorded in the database (`pictureTimeStamp` or `bitmapsTimeStamp`).
- **HTTP Fetching**:
  - If the image file is missing, or if the database timestamp indicates the local file is outdated:
  - `SyncImage` constructs the target URL: `"${baseUrl}/${filename}"` (or `"${baseUrl}${filename}"` if baseUrl ends with a slash).
  - Sends an asynchronous HTTP GET request using Ktor.
  - Writes the received image bytes directly to the local image directory (`ImageResolverConfig.imagesPath`).
  - Updates UI state to render the downloaded image immediately.

### Cloud SFTP Synchronization (`service/SftpService.kt`)
Supports bidirectional synchronization of images, JSON documents, and static HTML files with a remote SSH/SFTP server.
- **Implementations**:
  - Desktop: `DesktopSftpService` using `net.schmizz.sshj`.
  - Android: `AndroidSftpService` using `net.schmizz.sshj` with BouncyCastle security provider.
- **Authentication Modes**:
  - Password authentication.
  - SSH private key authentication (with optional passphrase).
- **Host Key Verification**:
  - Computes host key fingerprint. If not in `approvedFingerprints`, pauses execution and prompts the user for verification.
- **Selective Synchronization Plan**:
  - `calculateUploadPlan` / `calculateDownloadPlan`: Scans local and remote directories.
  - Categorizes actions into: `New File`, `Size Changed`, `Newer Timestamp`, or `Overwrite (JSON)`.
  - Renders a selection dialog allowing the user to select specific files.
  - Executes batch transfers with file-by-file progress reporting.

---

## 7. User Interface & Navigation

### Jetpack Navigation 3 Integration
Navigation uses `androidx.navigation3` and `club.gepetto.composeutils.navigation3.GcNavDisplay`.

```
                  ┌────────────────────── HomeDestination ──────────────────────┐
                  │ Landing screen with Gepetto artwork, banner, and navigation │
                  └──────────────────────────────┬──────────────────────────────┘
                                                 │
         ┌───────────────────┬───────────────────┼───────────────────┬───────────────────┐
         ▼                   ▼                   ▼                   ▼                   ▼
   DashboardScreen     ExplorerScreen   MakerDirectoryScreen   SettingsScreen       InfoScreen
   Collection stats    Category browse   Makers directory      Backup/Restore       App info &
   and valuations      and search        and brand details     SFTP & Sync          user guide
         │                   │                   │
         └─────────┬─────────┴─────────┬─────────┘
                   ▼                   ▼
            ToyDetailScreen     MakerDetailScreen
            (Detail Pane)       (Detail Pane)
                   │                   │
                   ▼                   ▼
             EditToyScreen       EditMakerScreen
             (Bottom Sheet)      (Bottom Sheet)
```

### Multi-Pane Scene Strategies (`GcSceneStrategy`)
- **Detail Panes**: `ToyDetail`, `AddToy`, and `AddMaker` are registered with `GcSceneStrategy.detailPane(resizeable = true)`. On desktop and wide screens, they open alongside the primary list pane.
- **Bottom Sheets**: `EditToy` and `EditMaker` are registered with `GcSceneStrategy.bottomSheetPane()`. They open as contextual bottom sheets on top of active content.

### Adaptive Navigation Scaffold (`GcAdaptiveScaffold`)
- **Portrait Orientation**: Displays a compact bottom navigation bar (`GcNavBar`).
- **Landscape / Desktop Orientation**: Renders a vertical navigation rail.
- **Dynamic Category Buttons**: Dynamically injects navigation items for all categories registered in table `category_settings`, matching icons via `getIconByName()`.

### Settings Screen Tabs (`ui/SettingsScreen.kt`)
The **SettingsScreen** organizes configuration into four tabs (`SettingsTab`), each modularized into its own dedicated UI file:
- **General** (`ui/GeneralSettingsTab.kt`): Theme selection (Light/Dark/System), custom collection title, and data storage path (Desktop).
- **Backup & Restore** (`ui/BackupRestoreCard.kt`): Full collection archive (`.zip`) backup and restoration.
- **Server Sync** (`ui/ServerSyncSettingsTab.kt`): Web server URL configuration, static website catalog page creation, SFTP server credentials, connection test, and differential cloud synchronization.
- **Categories** (`ui/CategoriesSettingsTab.kt`): Dynamic toy categories manager (add, edit, and delete categories, image prefixes, navigation labels, and icons).

The screen header includes a help button that navigates directly to the equivalent help tab on the **InfoScreen** for the currently selected settings tab.

### Info Screen Tabs (`ui/InfoScreen.kt`)
The **InfoScreen** provides succinct, plain-language documentation without technical jargon across seven tabs (`InfoTopic`):
- **About**: System summary, key capabilities, and application metadata (`about.md`).
- **General**: Help for theme colors, collection title, and storage directory (`general.md`).
- **Backup & Restore**: Help for full collection `.zip` backup and restoration (`backup_restore.md`).
- **Server Sync**: Help for web page creation, web server URL, cloud synchronization, and server credentials (`server_sync.md`), with an expandable button to view the complete SFTP server setup guide (`sftp_setup.md`).
- **Categories**: Help for adding, editing, and organizing custom toy categories and image prefixes (`categories.md`).
- **Privacy Policy**: Data collection disclosures and privacy guarantees (`privacypolicy.md`).
- **Terms of Use**: Software license terms and liability limitations (`terms.md`).

### Localization
String resources are fully localized across six languages under `composeApp/src/commonMain/composeResources/`:
- `values/strings.xml` (English - default)
- `values-pt/strings.xml` (Portuguese)
- `values-de/strings.xml` (German)
- `values-es/strings.xml` (Spanish)
- `values-fr/strings.xml` (French)
- `values-it/strings.xml` (Italian)

---

## 8. Data Storage & Platform Locations

### SQLite Database File (`toydb.db`)
- Packed default database: `composeResources/files/default_toydb.db`.
- Unpacked automatically on first run if the target file is missing or smaller than 50 KB.
- Clears pre-populated toys on fresh install to prepare for user import, while preserving category settings and manufacturers.

### Platform Storage Paths
- **macOS**: `~/Library/Application Support/ToyDatabaseManager/toydb.db`
- **Windows**: `%APPDATA%/ToyDatabaseManager/toydb.db`
- **Linux**: `~/.local/share/ToyDatabaseManager/toydb.db`
- **Android**: Application internal storage via `context.getDatabasePath("toydb.db")`
- **Android Backup Location**: Public device Downloads folder (`Downloads/toy_collection_backup.zip`) via Android MediaStore (API 29+) or public external storage (API 24–28)

### Unified Data Directory (`data_path`)
The application prompts the user on first launch to configure a data directory for images, JSON documents, and HTML exports:
- Stored under key `data_path` in table `app_settings` (with backward-compatible mirrors `images_path` and `import_export_path`).
- Global image resolution reads this path via `ImageResolverConfig.imagesPath`.

---

## 9. Standard Operation Flows

### 1. Cataloging a New Toy
1. Select a category in the navigation rail or open **Explorer**.
2. Click the **Add Toy** floating action button (`Destination.AddToy`).
3. Enter reference number (`refNum`), description, scale, manufacturer, and mechanical specifications.
4. Save the record. The application automatically calculates `maker_combo` and records the toy in SQLite.

### 2. Renaming a Manufacturer
1. Open **Makers Directory** and select the manufacturer (`Destination.MakerDetail`).
2. Click **Edit** to open the bottom sheet form (`Destination.EditMaker`).
3. Change the name. The application checks whether associated toys exist.
4. If toys exist, a confirmation dialog shows:
   *"Renaming '<Old>' to '<New>' will also update <N> associated toy(s). Do you want to proceed?"*
5. On confirmation, the database updates the maker and cascades changes across all referencing toys.

### 3. Creating Website Pages
1. Navigate to **Settings** (`Destination.Settings`) and select the **Server Sync** tab.
2. Under **Create Website Pages**, click **Create Pages**.
3. The service parses any existing `.lst` files to backfill missing metadata.
4. The service generates `{prefix}maker.html`, brand pages, and individual toy HTML detail pages in the target data directory.

### 4. Synchronizing with Cloud Storage
1. In **Settings**, select the **Server Sync** tab and configure SFTP credentials (host, port, username, authentication method, remote directory).
2. Click **Test SFTP Connection** to verify connectivity and approve host key fingerprints.
3. Click **Upload to Cloud** or **Download from Cloud**.
4. Review the selective synchronization plan modal and confirm the file transfer.

### 5. Backing Up and Restoring Your Collection
1. Navigate to **Settings** (`Destination.Settings`) and select the **Backup & Restore** tab.
2. Under **Backup & Restore**:
   - To create a backup: click or tap **Back Up Collection**. Choose a destination file (on Android, the app writes directly to `Downloads/toy_collection_backup.zip`).
   - To restore a backup: click or tap **Restore Collection**. Confirm the warning dialog, then select the backup `.zip` file.
3. The application validates the archive, restores all photos and database records within an atomic transaction, invalidates image caches, and refreshes all active screens.

---

## 10. Headless CLI Commands

The desktop application includes command-line flags for batch execution and automated testing without launching the GUI:

```bash
# Headless JSON Import and Export verification
./gradlew :composeApp:run --args="--headless-import-export"

# Headless HTML Website Generation
./gradlew :composeApp:run --args="--headless-export-html"
```

---

## 11. Verification & Build Commands

According to workspace rules (Rule 14 in `.agents/AGENTS.md`), **never run `gradlew assemble`**. Use the following verification commands:

```bash
# Run Desktop JVM unit tests
./gradlew :composeApp:desktopTest

# Verify Desktop JVM compilation
./gradlew :composeApp:compileKotlinDesktop

# Verify Android Debug compilation
./gradlew :composeApp:compileDebugKotlinAndroid

# Verify WebAssembly compilation
./gradlew :composeApp:compileKotlinWasmJs

# Verify Android Debug packaging and merged manifest
./gradlew :composeApp:assembleDebug

# Run Desktop Application
./gradlew :composeApp:run
```

Packaging installers:
- **macOS DMG**: Build using `./globalsdkgradlew` (Apple Silicon) or `./globalsdkgradlew_intel` (Intel).
- **Windows MSI**: Build using WiX definitions in `composeApp/wix/main.wxs`.
