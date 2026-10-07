# Toy Collection: Database & Asset Import/Export Implementation Plan

**Target Document**: `ToyCollection/.agents/IMPORT_EXPORT_PLAN.md`  
**Document Version**: 1.0.0  
**Target Module**: `:composeApp`  
**Target Platforms**: Desktop (macOS, Windows), Android, Web (WasmJS)  
**Document Status**: **LIVING DOCUMENT** — Update this plan during implementation. Log every change and bug.

---

## 1. Executive Summary & Objective

This document defines the complete technical plan to add database and asset import and export capabilities to **Toy Collection** (`ToyDb`).

### 1.1 Core Requirements
1. **Export Functionality**:
   - Export all database tables as JSON documents.
   - Identify all referenced file assets (primary toy pictures, secondary toy bitmaps, manufacturer logos/bitmaps).
   - Collect all referenced files that exist in local storage.
   - Package all table JSON documents and image assets into a single `.zip` archive.
2. **Import Functionality**:
   - Read a selected or downloaded `.zip` archive.
   - Extract all referenced image files into the application local data storage directory.
   - Parse all JSON documents and restore records into the SQLite database tables.
   - Refresh application state and UI caches immediately.
3. **Platform UI Logic (Race Director Model)**:
   - **Desktop (JVM - macOS, Windows)**:
     - Export: Open native system save dialog (`java.awt.FileDialog`, `SAVE` mode) so the user chooses the destination folder and filename.
     - Import: Open native system open dialog (`java.awt.FileDialog`, `LOAD` mode) so the user selects any backup `.zip` file.
   - **Android (ART)**:
     - Export: Write directly to the public device `Downloads` directory (`Environment.DIRECTORY_DOWNLOADS` / `MediaStore.Downloads`) as `toydb_backup.zip`. Do not display folder chooser dialogs.
     - Import: Read directly from the public device `Downloads` directory (`toydb_backup.zip`). If the file is not present, display an informative notification dialog.
   - **Web (WasmJS)**:
     - Provide clean `expect/actual` stubs to prevent build breakage.
4. **Safety & Confirmation**:
   - Display a modal confirmation dialog before executing an import to prevent accidental database overwrite.
   - Provide progress indicators and localized status messages for all operations.

---

## 2. Inviolable Directives & Coding Standards

All executing agents must obey the rules from `/Users/luizvaldetaro/valdetaro/.agents/AGENTS.md` and `ToyCollection/.agents/AGENTS.md`:

> [!IMPORTANT]
> ### Directive 1: Multiplatform Purity in `commonMain`
> Code in `composeApp/src/commonMain` must remain pure Kotlin Multiplatform.
> - Do not import `java.io.*`, `java.util.zip.*`, or Android SDK classes in `commonMain`.
> - All platform-specific filesystem, dialog, and archive streaming must reside behind `expect`/`actual` declarations.

> [!IMPORTANT]
> ### Directive 2: Logging Protocol
> Use `club.gepetto.GcLog` (`GcLog.d`, `GcLog.i`, `GcLog.w`, `GcLog.e`) exclusively.
> Do not use `println()`, `System.out`, `android.util.Log`, or third-party loggers.

> [!IMPORTANT]
> ### Directive 3: Git Hygiene
> Never execute `git commit` or `git push` unless the user explicitly gives that order.

> [!IMPORTANT]
> ### Directive 4: Multilingual Resources (Rule 6)
> All user-facing text strings must exist in `composeResources/values/strings.xml` and must be translated into:
> - German (`values-de/strings.xml`)
> - Spanish (`values-es/strings.xml`)
> - French (`values-fr/strings.xml`)
> - Italian (`values-it/strings.xml`)
> - Portuguese (`values-pt/strings.xml`)

> [!IMPORTANT]
> ### Directive 5: Build Verification Commands (Rule 14)
> Never run `./gradlew assemble`. Use target-specific compilation commands:
> - `./gradlew compileKotlinDesktop`
> - `./gradlew compileDebugKotlinAndroid`
> - `./gradlew wasmJsBrowserDevelopmentExecutableDistribution` (optional web verification)

> [!IMPORTANT]
> ### Directive 6: Living Document Maintenance
> - Keep this file updated during every implementation step.
> - Check off completed checklist items in Section 10.
> - Record all technical modifications in Section 11.
> - Record all encountered errors and bugs in Section 12.

---

## 3. Data Architecture & Archive Structure

### 3.1 SQLite Database Tables
The application database contains four tables:

1. **`category_settings`**:
   - Columns: `category` (TEXT PK), `image_prefix` (TEXT), `label` (TEXT), `title` (TEXT), `icon` (TEXT).
2. **`makers`**:
   - Columns: `name` (TEXT PK), `country` (TEXT), `bitmaps` (TEXT), `bitmaps_size` (TEXT), `bitmaps_timestamp` (TEXT), `comments` (TEXT).
3. **`toys`**:
   - Columns: `ref_num` (INTEGER), `toy_type` (TEXT), `description` (TEXT), `maker_combo` (TEXT), `scale` (TEXT), `factory_car` (TEXT), `body_maker` (TEXT), `acquired` (TEXT), `chassis_type` (TEXT), `chassis_maker` (TEXT), `condition` (TEXT), `color` (TEXT), `motor_maker` (TEXT), `motor_details` (TEXT), `catalog_number` (TEXT), `comments` (TEXT), `major_work` (TEXT), `minor_work` (TEXT), `repro` (TEXT), `value` (REAL), `amount_paid` (REAL), `amount_sold` (TEXT), `traded` (TEXT), `buy` (TEXT), `maintenance` (TEXT), `to_make` (TEXT), `detail` (TEXT), `boxed` (TEXT), `picture` (TEXT), `picture_size` (INTEGER), `picture_timestamp` (INTEGER), `has_picture` (TEXT), `bitmaps` (TEXT), `bitmaps_size` (TEXT), `bitmaps_timestamp` (TEXT), `year_made` (TEXT), `number` (TEXT), `my_comments` (TEXT).
   - Primary Key: `(toy_type, ref_num)`.
4. **`app_settings`**:
   - Columns: `key` (TEXT PK), `value` (TEXT).

### 3.2 ZIP Archive Specification
The generated `.zip` archive must follow this directory layout:

```
toydb_backup.zip
├── data/
│   ├── category_settings.json   # Serialized category_settings table
│   ├── makers.json              # Serialized makers table
│   ├── toys.json                # Master list of all toys across all categories
│   ├── app_settings.json        # Serialized application preferences
│   ├── carlist.json             # Backward-compatible slot cars list
│   ├── tralist.json             # Backward-compatible model trains list
│   ├── stalist.json             # Backward-compatible static models list
│   ├── plalist.json             # Backward-compatible model kits list
│   └── mislist.json             # Backward-compatible miscellaneous toys list
└── images/
    ├── car1001.jpg              # Resolved primary toy images
    ├── car1001_box.jpg          # Secondary toy bitmap images
    ├── tra52.png                # Train images
    ├── scalextric_logo.jpg      # Manufacturer logos / secondary bitmaps
    └── ...
```

### 3.3 Referenced Asset Resolution Algorithm
To determine which image files to include during export:
1. **Manufacturer Assets**:
   - Read each record from `makers`.
   - Parse space-separated filenames from `makers.bitmaps`.
   - For each filename, call `resolveBitmapUri(filename)` or inspect active image paths.
   - If the file exists on disk, add entry `images/$filename` to the ZIP map.
2. **Toy Primary Assets**:
   - Read each record from `toys`.
   - Query prefix for `toy.toy_type` from `category_settings`.
   - If `toy.picture` is not blank, resolve file via `resolveBitmapUri(toy.picture)` or `resolveImageUri(prefix, toy.refNum)`.
   - If `toy.picture` is blank and `toy.hasPicture == "y"`, resolve file via `resolveImageUri(prefix, toy.refNum)`.
   - If a matching file exists on disk, add entry `images/${file.name}` to the ZIP map.
3. **Toy Secondary Assets**:
   - Parse space-separated filenames from `toy.bitmaps`.
   - For each filename, resolve via `resolveBitmapUri(filename)`.
   - If the file exists on disk, add entry `images/$filename` to the ZIP map.
4. **Deduplication**:
   - Use a `LinkedHashSet<String>` to guarantee that no filename is inserted twice into the ZIP stream.

---

## 4. Multiplatform Architecture

### 4.1 Component Diagram

```
┌────────────────────────────────────────────────────────┐
│                   SettingsScreen.kt                    │
│   - Export Database Button                             │
│   - Import Database Button                             │
│   - Import Confirmation Modal Dialog                   │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│               ImportExportService.kt                   │
│   - exportDatabaseZip(db): ByteArray                   │
│   - importDatabaseZip(db, zipBytes): ImportSummary     │
│   - serialize/deserialize JSON tables                  │
│   - resolve referenced image disk paths                │
└─────────────┬────────────────────────────┬─────────────┘
              │                            │
              ▼                            ▼
┌───────────────────────────┐ ┌───────────────────────────┐
│     ZipHelper (expect)    │ │  FileExportHelper (expect)│
│ - createZip(...)          │ │ - exportFile(...)         │
│ - extractZip(...)         │ │ - pickFileForImport(...)  │
└───────┬───────────┬───────┘ └───────┬───────────┬───────┘
        │           │                 │           │
        ▼           ▼                 ▼           ▼
  [desktopMain] [androidMain]   [desktopMain] [androidMain]
  java.util.zip MediaStore      FileDialog    Downloads dir
```

### 4.2 FileExportHelper Contract (`commonMain`)

Create package `com.gepetto.toydb.platform` and file `FileExportHelper.kt`:

```kotlin
package com.gepetto.toydb.platform

expect object FileExportHelper {
    suspend fun exportFile(suggestedName: String, mimeType: String, bytes: ByteArray): Boolean
    suspend fun pickFileForImport(mimeType: String): ByteArray?
}

suspend fun FileExportHelper.exportZip(filename: String, zipBytes: ByteArray): Boolean {
    return exportFile(filename, "application/zip", zipBytes)
}

suspend fun FileExportHelper.openZipForImport(): Pair<String, ByteArray>? {
    val bytes = pickFileForImport("application/zip") ?: return null
    return "toydb_backup.zip" to bytes
}
```

### 4.3 Desktop FileExportHelper Implementation (`desktopMain`)
- Location: `composeApp/src/desktopMain/kotlin/com/gepetto/toydb/platform/FileExportHelper.desktop.kt`
- Implements `exportFile`:
  - Uses `java.awt.FileDialog(null as Frame?, "Save Backup File", FileDialog.SAVE)` on the Swing Event Dispatch Thread (`SwingUtilities.invokeAndWait`).
  - Sets default suggested filename `toydb_backup.zip`.
  - Writes bytes to the selected directory and file.
- Implements `pickFileForImport`:
  - Uses `java.awt.FileDialog(null as Frame?, "Open Backup File", FileDialog.LOAD)`.
  - Sets filename filter for `.zip`.
  - Reads bytes from selected file.

### 4.4 Android FileExportHelper Implementation (`androidMain`)
- Location: `composeApp/src/androidMain/kotlin/com/gepetto/toydb/platform/FileExportHelper.android.kt`
- Uses fixed filename constant: `private const val FIXED_BACKUP_FILENAME = "toydb_backup.zip"`
- Implements `exportFile`:
  - Retrieves application context from `club.gepetto.utils.GcAppInfo.application_Context as? Context`.
  - **Android 10+ (API 29+)**:
    - Queries `MediaStore.Downloads.EXTERNAL_CONTENT_URI` to overwrite existing file if present.
    - If new, inserts record with `MediaStore.MediaColumns.RELATIVE_PATH = Environment.DIRECTORY_DOWNLOADS`, `MIME_TYPE = "application/zip"`, and `IS_PENDING = 1`.
    - Writes bytes via `ContentResolver.openOutputStream(uri, "wt")`.
    - Clears `IS_PENDING = 0`.
  - **Android 9 and below**:
    - Writes to `File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FIXED_BACKUP_FILENAME)`.
- Implements `pickFileForImport`:
  - **Android 10+**: Queries `MediaStore.Downloads.EXTERNAL_CONTENT_URI` for `DISPLAY_NAME = "toydb_backup.zip"` and `RELATIVE_PATH LIKE 'Download%'`. Reads bytes from `openInputStream(uri)`.
  - **Android 9 and below**: Reads from `File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FIXED_BACKUP_FILENAME)`.
  - Returns `null` if the backup file does not exist in `Downloads`.

### 4.5 Web FileExportHelper Implementation (`wasmJsMain`)
- Location: `composeApp/src/wasmJsMain/kotlin/com/gepetto/toydb/platform/FileExportHelper.wasmJs.kt`
- Returns safe stub values (`exportFile` returns false or triggers web download anchor; `pickFileForImport` returns null).

---

## 5. Archive Processing Engine (`ZipHelper`)

### 5.1 ZipHelper Contract (`commonMain`)

Create file `composeApp/src/commonMain/kotlin/com/gepetto/toydb/platform/ZipHelper.kt`:

```kotlin
package com.gepetto.toydb.platform

data class ExtractedZipData(
    val jsonEntries: Map<String, String>,
    val imageCount: Int,
    val extractedImages: List<String>
)

expect object ZipHelper {
    fun createZip(
        textEntries: Map<String, String>,
        fileEntries: Map<String, String>
    ): ByteArray

    fun extractZip(
        zipBytes: ByteArray,
        targetImagesDir: String
    ): ExtractedZipData
}
```

### 5.2 Desktop & Android ZipHelper Implementation (`desktopMain` and `androidMain`)
- Uses standard JVM `java.util.zip.ZipOutputStream` and `java.util.zip.ZipInputStream`.
- **Packaging (`createZip`)**:
  - Writes text entries (`textEntries`) as UTF-8 streams into entries like `data/category_settings.json`.
  - Streams disk files (`fileEntries`) into entries like `images/car1.jpg` using buffered 8 KB chunks.
- **Extraction (`extractZip`)**:
  - Iterates over ZIP entries.
  - If entry ends with `.json`: reads entire text into memory using 8 KB buffer without wrapping `ZipInputStream` in closing reader.
  - If entry path starts with `images/` or `assets/`: creates output file inside `targetImagesDir` and streams bytes directly.
  - Returns `ExtractedZipData` containing all parsed JSONs and list of extracted image filenames.

### 5.3 Web ZipHelper Implementation (`wasmJsMain`)
- Provides no-op stub returning empty `ExtractedZipData`.

---

## 6. Service Layer Enhancements (`ImportExportService.kt`)

### 6.1 Unified Data Models
Add serialization models for master toy export:

```kotlin
@Serializable
data class JsonAllToysFile(
    val date: String = getCurrentDateString(),
    val buildNumber: String = CommonConfig.versionCodeString,
    val toys: List<JsonToyWithCategory>
)

@Serializable
data class JsonToyWithCategory(
    val category: String,
    val toy: JsonToy
)
```

### 6.2 New Service Methods
1. `fun exportAllToys(db: ToyDatabase): String`:
   - Queries all rows from `toys` ordered by `toy_type`, `body_maker`, `description`.
   - Serializes into `JsonAllToysFile`.
2. `fun importAllToys(db: ToyDatabase, jsonContent: String): Int`:
   - Parses `JsonAllToysFile` (or standard `JsonToysFile` if category specific).
   - Inserts or replaces records in `toys` table.
3. `suspend fun exportDatabaseZipBytes(db: ToyDatabase): ByteArray`:
   - Builds `textEntries` map:
     - `"data/category_settings.json"` -> `exportCategorySettings(db)`
     - `"data/makers.json"` -> `exportMakers(db)`
     - `"data/toys.json"` -> `exportAllToys(db)`
     - `"data/app_settings.json"` -> `exportAppSettings(db)`
     - For each category: `"data/${prefix}list.json"` -> `exportToys(db, category)`
   - Scans database records and locates all existing referenced image files on disk.
   - Builds `fileEntries` map: `"images/$filename"` -> absolute disk path.
   - Calls `ZipHelper.createZip(textEntries, fileEntries)`.
4. `suspend fun importDatabaseZipBytes(db: ToyDatabase, zipBytes: ByteArray): DatabaseRestoreResult`:
   - Resolves target local images path via `ImageResolverConfig.imagesPath` (or fallback).
   - Calls `ZipHelper.extractZip(zipBytes, imagesDir)`.
   - Executes clean database restore inside atomic block:
     ```sql
     DELETE FROM toys;
     DELETE FROM makers;
     DELETE FROM category_settings;
     ```
   - Imports `category_settings.json` first.
   - Imports `makers.json`.
   - Imports `toys.json` (or category-specific `{prefix}list.json` files).
   - Imports `app_settings.json` (preserves local database path while restoring application preferences).
   - Returns counts of restored categories, makers, toys, and images.

---

## 7. User Interface & Dialog Integration (`SettingsScreen.kt`)

### 7.1 Database Backup Card Layout
Replace the current single HTML button inside `ImportExportActions` with a comprehensive **Database Backup & Operations** card matching Race Director's design:

```
┌────────────────────────────────────────────────────────┐
│ Database Backup & Operations                           │
│ Backup your toy collection, makers, and photos.        │
├────────────────────────────────────────────────────────┤
│ [ Export Database Backup ]  [ Import Database Backup ] │
├────────────────────────────────────────────────────────┤
│ [ Export HTML Web Pages ]                              │
└────────────────────────────────────────────────────────┘
```

### 7.2 UI State Machine
- `isDbProcessing: Boolean`: Disables buttons and shows `CircularProgressIndicator` during processing.
- `dbOpStatusMessage: String?`: Displays operational result below buttons.
- `pendingImportData: Pair<String, ByteArray>?`: Holds loaded archive bytes pending user confirmation.
- `showImportConfirmDialog: Boolean`: Displays confirmation modal.
- `showMissingBackupDialog: Boolean`: Displays Android "File Not Found in Downloads" alert.

### 7.3 Import Confirmation Dialog
- **Title**: `stringResource(Res.string.settings_db_import_title)` ("Import Database Backup")
- **Message**: "Are you sure you want to restore the database from this backup? All current collection tables will be updated, and photos will be extracted into your data folder."
- **Action Buttons**:
  - `OutlinedButton`: Cancel (dismisses dialog, clears `pendingImportData`).
  - `Button` (Error/Primary color): Confirm ("Import & Restore").
    - Triggers `importDatabaseZipBytes`.
    - Triggers `onCategoriesChanged()` and repository refresh.
    - Updates `dbOpStatusMessage`.

### 7.4 Android Missing File Alert Dialog
- Triggered when `pickFileForImport` returns null on Android.
- **Title**: `stringResource(Res.string.settings_db_missing_backup_title)`
- **Message**: "The backup file 'toydb_backup.zip' was not found in your device Downloads folder. Please place a backup file named 'toydb_backup.zip' in your Downloads directory and try again."
- **Button**: "OK".

---

## 8. Multilingual Resource Dictionary

All keys must be added to all 6 `strings.xml` files.

| String Key | English (`values`) | Portuguese (`values-pt`) | German (`values-de`) | Spanish (`values-es`) | French (`values-fr`) | Italian (`values-it`) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `settings_db_backup_title` | Database Backup & Restore | Backup e Restauração do Banco | Datenbanksicherung & Wiederherstellung | Copia de Seguridad y Restauración | Sauvegarde et Restauration | Backup e Ripristino Database |
| `settings_db_backup_desc` | Export your complete collection, manufacturers, and photos to a single archive, or restore from a backup. | Exporte sua coleção completa, fabricantes e fotos para um único arquivo, ou restaure de um backup. | Exportieren Sie Ihre vollständige Sammlung, Hersteller und Fotos in ein einzelnes Archiv, oder stellen Sie aus einem Backup wieder her. | Exporte su colección completa, fabricantes y fotos en un solo archivo, o restaure desde una copia de seguridad. | Exportez votre collection complète, fabricants et photos dans une seule archive, ou restaurez depuis une sauvegarde. | Esporta la tua collezione completa, produttori e foto in un singolo archivio, o ripristina da un backup. |
| `settings_db_export` | Export Database | Exportar Banco de Dados | Datenbank Exportieren | Exportar Base de Datos | Exporter Base de Données | Esporta Database |
| `settings_db_import` | Import Database | Importar Banco de Dados | Datenbank Importieren | Importar Base de Datos | Importer Base de Données | Importa Database |
| `settings_db_exporting` | Exporting database and photos... | Exportando banco de dados e fotos... | Datenbank und Fotos werden exportiert... | Exportando base de datos y fotos... | Exportation de la base et des photos... | Esportazione database e foto in corso... |
| `settings_db_importing` | Importing database and photos... | Importando banco de dados e fotos... | Datenbank und Fotos werden importiert... | Importando base de datos y fotos... | Importation de la base et des photos... | Importazione database e foto in corso... |
| `settings_db_export_success` | Database and photos exported successfully. | Banco de dados e fotos exportados com sucesso. | Datenbank und Fotos erfolgreich exportiert. | Base de datos y fotos exportadas con éxito. | Base de données et photos exportées avec succès. | Database e foto esportati con successo. |
| `settings_db_export_failed` | Failed to export database backup. | Falha ao exportar backup do banco de dados. | Exportieren der Datenbanksicherung fehlgeschlagen. | Error al exportar copia de seguridad. | Échec de l'exportation de la sauvegarde. | Esportazione backup database non riuscita. |
| `settings_db_import_title` | Import Database Backup | Importar Backup do Banco | Datenbanksicherung Importieren | Importar Copia de Seguridad | Importer une Sauvegarde | Importa Backup Database |
| `settings_db_import_confirm` | Are you sure you want to restore the database from this backup? Current records will be replaced and photos will be extracted. | Tem certeza de que deseja restaurar o banco a partir deste backup? Os registros atuais serão substituídos e as fotos extraídas. | Möchten Sie die Datenbank wirklich aus dieser Sicherung wiederherstellen? Vorhandene Daten werden ersetzt und Fotos entpackt. | ¿Está seguro de restaurar la base de datos desde esta copia? Los registros actuales se reemplazarán y se extraerán las fotos. | Êtes-vous sûr de vouloir restaurer la base depuis cette sauvegarde? Les enregistrements seront remplacés et les photos extraites. | Sei sicuro di voler ripristinare il database da questo backup? I record attuali verranno sostituiti e le foto estratte. |
| `settings_db_import_success` | Successfully imported backup: %1$d toys, %2$d makers, %3$d photos. | Backup importado com sucesso: %1$d brinquedos, %2$d fabricantes, %3$d fotos. | Sicherung erfolgreich importiert: %1$d Modelle, %2$d Hersteller, %3$d Fotos. | Copia importada con éxito: %1$d modelos, %2$d fabricantes, %3$d fotos. | Sauvegarde importée avec succès: %1$d jouets, %2$d fabricants, %3$d photos. | Backup importato con successo: %1$d modelli, %2$d produttori, %3$d foto. |
| `settings_db_import_failed` | Failed to import database backup: %1$s | Falha ao importar backup do banco: %1$s | Importieren der Sicherung fehlgeschlagen: %1$s | Error al importar copia de seguridad: %1$s | Échec de l'importation de la sauvegarde: %1$s | Importazione backup database non riuscita: %1$s |
| `settings_db_missing_backup_title` | Backup File Not Found | Arquivo de Backup Não Encontrado | Sicherungsdatei Nicht Gefunden | Archivo de Respaldo No Encontrado | Fichier de Sauvegarde Introuvable | File di Backup Non Trovato |
| `settings_db_missing_backup_msg` | The file 'toydb_backup.zip' was not found in your Downloads folder. Please place a backup file in Downloads and try again. | O arquivo 'toydb_backup.zip' não foi encontrado na pasta Downloads. Coloque o arquivo de backup em Downloads e tente novamente. | Die Datei 'toydb_backup.zip' wurde im Download-Ordner nicht gefunden. Bitte legen Sie die Datei im Ordner Downloads ab. | No se encontró 'toydb_backup.zip' en la carpeta Descargas. Coloque el archivo en Descargas e intente de nuevo. | Le fichier 'toydb_backup.zip' n'a pas été trouvé dans Téléchargements. Veuillez y placer le fichier et réessayer. | Il file 'toydb_backup.zip' non è stato trovato nella cartella Download. Inserisci il file nei Download e riprova. |

---

## 9. Verification & Testing Protocol

### 9.1 Unit Tests
Create unit tests under `composeApp/src/desktopTest/kotlin/com/gepetto/toydb/service/ImportExportZipTest.kt`:
1. `testZipPackagingAndExtraction()`:
   - Create mock JSON table strings and temporary mock photo files.
   - Package with `ZipHelper.createZip`.
   - Verify archive structure using `ZipHelper.extractZip`.
   - Verify file contents and UTF-8 characters.
2. `testDatabaseExportAndRestoreRoundtrip()`:
   - Populate in-memory / temporary test database with sample categories, makers, and toys.
   - Run `exportDatabaseZipBytes`.
   - Verify tables JSON content.
   - Clear database and run `importDatabaseZipBytes`.
   - Assert all records and attributes match baseline exactly.

### 9.2 Build Verification Commands
Execute the following verification commands:
```bash
# Verify Desktop JVM build and tests
./gradlew :composeApp:testDesktopUnitTest
./gradlew compileKotlinDesktop

# Verify Android build
./gradlew compileDebugKotlinAndroid
```

---

## 10. Living Document: Execution Checklist

- [ ] **Phase 1: Localized Strings Setup**
  - [ ] Add all 14 string keys to `composeApp/src/commonMain/composeResources/values/strings.xml`
  - [ ] Add Portuguese translations to `values-pt/strings.xml`
  - [ ] Add German translations to `values-de/strings.xml`
  - [ ] Add Spanish translations to `values-es/strings.xml`
  - [ ] Add French translations to `values-fr/strings.xml`
  - [ ] Add Italian translations to `values-it/strings.xml`
- [ ] **Phase 2: Platform File Helper (`FileExportHelper`)**
  - [ ] Ensure `GcAppInfo.application_Context = application` is initialized in `AppMainActivity.onCreate()`
  - [ ] Create `composeApp/src/commonMain/kotlin/com/gepetto/toydb/platform/FileExportHelper.kt`
  - [ ] Create `composeApp/src/desktopMain/kotlin/com/gepetto/toydb/platform/FileExportHelper.desktop.kt`
  - [ ] Create `composeApp/src/androidMain/kotlin/com/gepetto/toydb/platform/FileExportHelper.android.kt`
  - [ ] Create `composeApp/src/wasmJsMain/kotlin/com/gepetto/toydb/platform/FileExportHelper.wasmJs.kt`
- [ ] **Phase 3: Archive Helper (`ZipHelper`)**
  - [ ] Create `composeApp/src/commonMain/kotlin/com/gepetto/toydb/platform/ZipHelper.kt`
  - [ ] Create `composeApp/src/desktopMain/kotlin/com/gepetto/toydb/platform/ZipHelper.desktop.kt`
  - [ ] Create `composeApp/src/androidMain/kotlin/com/gepetto/toydb/platform/ZipHelper.android.kt`
  - [ ] Create `composeApp/src/wasmJsMain/kotlin/com/gepetto/toydb/platform/ZipHelper.wasmJs.kt`
- [ ] **Phase 4: ImportExportService Enhancement**
  - [ ] Implement `exportAllToys` and `importAllToys` in `ImportExportService.kt`
  - [ ] Implement referenced asset disk scanner in `ImportExportService.kt`
  - [ ] Implement `exportDatabaseZipBytes` in `ImportExportService.kt`
  - [ ] Implement `importDatabaseZipBytes` in `ImportExportService.kt`
- [ ] **Phase 5: User Interface Integration**
  - [ ] Update `ImportExportActions` composable in `SettingsScreen.kt` with Export and Import buttons
  - [ ] Add import confirmation modal dialog in `SettingsScreen.kt`
  - [ ] Add Android missing backup notification dialog in `SettingsScreen.kt`
  - [ ] Ensure dark mode dialog border compliance (`sysBackgroundColor`, `BorderStroke`)
  - [ ] Connect refresh callbacks (`onCategoriesChanged()`) after import completes
- [ ] **Phase 6: Verification & Test Suite**
  - [ ] Write unit tests in `composeApp/src/desktopTest/kotlin/com/gepetto/toydb/service/`
  - [ ] Run `./gradlew :composeApp:testDesktopUnitTest`
  - [ ] Run `./gradlew compileKotlinDesktop`
  - [ ] Run `./gradlew compileDebugKotlinAndroid`
  - [ ] Update `ToyCollection/.agents/TODO.txt` marking task complete

---

## 11. Living Document: Change Log

| Date (UTC) | Version | Author / Agent | Changes Made |
| :--- | :--- | :--- | :--- |
| 2026-10-07 | 1.0.0 | Antigravity Agent | Initial creation of the comprehensive execution plan. |

---

## 12. Living Document: Bug & Issue Tracking Log

| Issue ID | Date | Affected Component | Description & Root Cause | Resolution Status | Fix Description |
| :--- | :--- | :--- | :--- | :--- | :--- |
| *None* | 2026-10-07 | N/A | Initial plan creation. No implementation bugs encountered yet. | Open | N/A |
