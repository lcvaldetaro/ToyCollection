# Toy Collection: Backup & Restore Implementation Plan

**Target Document**: `ToyCollection/.agents/IMPORT_EXPORT_PLAN.md` (file name kept so existing links in `TODO.txt` stay valid)
**Document Version**: 2.1.0
**Target Module**: `:composeApp`
**Target Platforms**: Desktop (macOS, Windows), Android. Web: compile-only stubs, feature hidden.
**Document Status**: **LIVING DOCUMENT** — Update this plan during implementation. Log every change and bug.

---

## 0. How to Use This Plan

1. Read Section 2 (rules) and Section 3 (decisions). Do not reopen the decisions in Section 3.
2. Do the phases in Section 10 in order. Each phase must compile before you start the next one.
3. Line numbers in this plan were correct on 2026-10-07. If they moved, find the code by the names given.
4. If the code does not match this plan, stop, record it in Section 12, and ask the user.

---

## 1. Objective

Give the user a simple way to **back up** the whole collection to one file and to **restore** it later, on the same device or on a different device.

- The backup is one `.zip` file. It contains the collection data (as JSON) and every photo that the data refers to.
- Restore replaces the toys, makers and categories on the device with the ones in the backup, and copies the photos into the data directory.
- The behavior follows the Race Director app (`RaceDirector/composeApp/src/*/kotlin/com/gepettoracedirector/platform/FileExportHelper*.kt`).

In the same change:
- Rename the existing **"HTML Generation"** card to **"Create Website Pages"** (user-visible text only, Section 7.6).
- Rename the Info screen tab **"Backup"** (the SFTP guide) to **"Server Sync"**, so the app does not show two different things called "Backup" (Section 7.8).

---

## 2. Rules That Apply

Obey `~/valdetaro/.agents/AGENTS.md` and `ToyCollection/.agents/AGENTS.md`. The most important rules for this work:

> [!IMPORTANT]
> **R1 — User-facing words.** The user does not want technical words in the UI. In every user-visible text use **"Back Up"**, **"Backup"**, **"Restore"**, **"collection"**, **"photos"**, **"website pages"**. Do **not** use "import", "export", "database", "JSON", "zip" or "HTML" in user-visible text. (The Android file name `toy_collection_backup.zip` is the only exception, because the user must find that file.)

> [!IMPORTANT]
> **R2 — `commonMain` stays pure KMP.** No `java.*`, `javax.*` or `android.*` imports in `commonMain`. Use `okio` (already a `commonMain` dependency) for paths and streams. Put zip, dialogs and MediaStore code behind `expect`/`actual`.

> [!IMPORTANT]
> **R3 — Never hold the whole backup in memory.** The desktop data directory has about **1.5 GB in 2,900 photos**. Stream every photo, one at a time, from disk into the zip and from the zip to disk. Only the JSON entries (a few MB) may be held in memory. No API in this plan may take or return the archive as a `ByteArray`.

> [!IMPORTANT]
> **R4 — Logging.** Use `club.gepetto.GcLog` only. No `println`, `System.out`, `android.util.Log`, `e.printStackTrace()`.

> [!IMPORTANT]
> **R5 — Strings.** Every user-visible text is a string resource in all 6 files: `composeResources/values{,-de,-es,-fr,-it,-pt}/strings.xml`. Escape apostrophes as `\'` and `&` as `&amp;` (see existing entries). Native dialog titles also come from string resources (`getString(...)`).

> [!IMPORTANT]
> **R6 — UI.** Use `sysBackgroundColor()` / `sysTextColor()`. Every new dialog must have a border in dark mode: `Modifier.border(1.dp, MaterialTheme.colorScheme.outline, AlertDialogDefaults.shape)` when `club.gepetto.composeutils.isDark()` is true. Every new composable with UI has `@PreviewLightDark` and `@Preview(name = "Landscape", widthDp = 800, heightDp = 480)` previews wrapped in `GcTheme {}`. Long work runs on `club.gepetto.utils.ioDispatcher`.

> [!IMPORTANT]
> **R7 — Architecture.** Keep today's pattern: UI state lives in the composable (`remember`), logic lives in the service object. Do **not** add a ViewModel.

> [!IMPORTANT]
> **R8 — Git.** Never `git commit`, `git push` or `git add` unless the user orders it.

> [!IMPORTANT]
> **R9 — Build.** Never run `./gradlew assemble`. Use the commands in Section 9.

> [!IMPORTANT]
> **R10 — Tests.** If an existing test fails, first assume the new code is wrong. Temporary test files go in a temp directory, never in the module root.

> [!IMPORTANT]
> **R11 — Living document.** Check off Section 10 items as you finish them. Add every code change to Section 11 and every problem to Section 12.

---

## 3. Decisions (Approved by the User on 2026-10-07)

| # | Topic | Decision |
|---|---|---|
| D1 | UI name | **"Backup & Restore"**. Buttons: **"Back Up Collection"** and **"Restore Collection"**. |
| D2 | HTML card | Rename user-visible text from "HTML Generation" to **"Create Website Pages"**. |
| D3 | Android restore source | Same as Race Director: read only `Downloads/toy_collection_backup.zip`. If it is not found, show the "Backup File Not Found" dialog. No file picker. |
| D4 | Android 9 and below | Add `WRITE_EXTERNAL_STORAGE` / `READ_EXTERNAL_STORAGE` with `maxSdkVersion="28"` and ask for the permission at run time. |
| D5 | Settings that are not in the backup | All `sftp_*` keys, `images_path`, `data_path`, `import_export_path`, and all `html_sync_imported_*` keys. |
| D6 | Architecture | Keep today's pattern (Rule R7). |
| D7 | Startup web sync | Accepted: the web sync at app start can still replace a restored collection when the server has newer data. No change. |
| D8 | Info tab name | The Info screen tab "Backup" (SFTP guide) becomes **"Server Sync"**. Text only. |

### Known limitation (accepted with D3)
On Android 10 and later, the app can only find a file in Downloads that **this installation** created. After the app is reinstalled, or when the file was copied from a computer, restore reports "not found". This is the same as Race Director. Record it in `HOW_IT_WORKS.md` (Phase 7).

---

## 4. Current Code Facts (Checked 2026-10-07)

The executing agent depends on these facts. Check them before Phase 2.

| Fact | Where |
|---|---|
| `ToyDatabase` has only `execute`, `query`, `close`. No transaction support. | `commonMain/.../database/Database.kt:17` |
| Platform databases: JDBC (`DesktopDatabase.kt`), Android `SQLiteDatabase` (`AndroidDatabase.kt`), sql.js (`WasmDatabase.wasmJs.kt`). | `*/database/` |
| JSON envelope classes and import/export functions for each table already exist: `exportCategorySettings`, `exportMakers`, `exportToys(db, toyType)`, `exportAppSettings`, `importCategorySettings`, `importMakers`, `importToys(db, toyType, json)`, `importAppSettings`. | `commonMain/.../service/ImportExportService.kt` |
| `importToys` and `processBitmaps` read size and timestamp **from the photo file on disk** in the folder `getImagesPath(db)` returns (`app_settings.images_path`). If the file is not there, they use the values in the JSON. | `ImportExportService.kt:122-305` |
| The existing JSON file names are `category_settings.json`, `carmaker.json` (makers), `{imagePrefix}list.json` (one per category). The web sync and the headless CLI use the same names. | `HtmlSyncService.kt`, `desktopMain/kotlin/Main.kt:144-180` |
| The web sync already does a "clean restore": `DELETE FROM toys/makers/category_settings`, then imports categories, makers, toys. | `HtmlSyncService.kt` (~line 195) |
| `SyncImage` downloads a photo from the server again when the DB timestamp is **newer** than the file's modified time. | `ui/SyncImage.kt:118` |
| Real `app_settings` keys: `app_title`, `base_url`, `theme`, `data_path`, `images_path`, `import_export_path`, `sftp_host`, `sftp_port`, `sftp_username`, `sftp_auth_type`, `sftp_password`, `sftp_key_path`, `sftp_key_passphrase`, `sftp_remote_dir`, `sftp_approved_fingerprints`, `html_sync_imported_date_*`, `html_sync_imported_hash_*`. | user's desktop DB |
| Data directory: `ToyRepository.getDataPathSetting()` (falls back to `images_path` / `import_export_path` and writes `images_path`). Android sets it to `filesDir/data` at start. Desktop can have no data directory. | `ToyRepository.kt:528`, `AppMainActivity.kt` |
| `ImportExportActions` (the HTML card) is called **twice** in `SettingsScreen` — wide layout (~line 1196) and narrow layout (~line 1414). Both calls are inside `if (!isWebPlatform())`. | `ui/SettingsScreen.kt` |
| `ImportExportActions` has two hard-coded English status texts. | `SettingsScreen.kt:1624, 1631` |
| After a full data change, lists refresh only when `syncTrigger` changes. Settings only has `onCategoriesChanged`, which reloads categories. | `ui/ToyDbNavigation.kt:139, 159, 419-429, 498` |
| Desktop already has a LOAD file dialog: `selectFileDialog(title, allowedExtensions)`. Use it for restore. | `desktopMain/.../utils/ImageResolver.kt` |
| `GcAppInfo.application_Context` is **not** set in this app today. | `androidMain/.../AppMainActivity.kt` |
| `AndroidManifest.xml` has only `INTERNET`. minSdk 24, targetSdk 37. | `androidMain/AndroidManifest.xml`, `gradle/libs.versions.toml` |
| No ViewModels exist in the app. | — |
| Gradle tasks that exist: `:composeApp:desktopTest`, `:composeApp:compileKotlinDesktop`, `:composeApp:compileDebugKotlinAndroid`, `:composeApp:compileKotlinWasmJs`. (`testDesktopUnitTest` does **not** exist.) | `./gradlew :composeApp:tasks --all` |

---

## 5. Backup File Format

### 5.1 Layout

```
toy_collection_backup.zip
├── manifest.json                 # format id, version, counts (Section 5.2)
├── data/
│   ├── category_settings.json    # ImportExportService.exportCategorySettings(db)
│   ├── carmaker.json             # ImportExportService.exportMakers(db)
│   ├── app_settings.json         # exportAppSettings(db), filtered (Section 5.3)
│   ├── carlist.json              # exportToys(db, "slot")   -> "{imagePrefix}list.json"
│   ├── tralist.json              # one file for each row in category_settings
│   └── ...
└── images/
    ├── car1444.jpg               # name EXACTLY as written in the DB (picture / bitmaps token)
    └── ...
```

- Use the existing JSON envelopes and file names. Do **not** add a combined `toys.json` (it would copy every toy twice and make it unclear which copy wins).
- The `data/` JSON files are the same files the web server uses, so a backup can also be used to feed the server.

### 5.2 `manifest.json`

```kotlin
@Serializable
data class BackupManifest(
    val format: String = BACKUP_FORMAT_ID,      // "gepetto-toy-collection-backup"
    val formatVersion: Int = BACKUP_FORMAT_VERSION, // 1
    val createdAt: String = getCurrentDateString(),
    val appVersionCode: String = CommonConfig.versionCodeString,
    val categories: Int,
    val makers: Int,
    val toys: Int,
    val photos: Int
)
```

On restore, the file is valid only if `manifest.json` exists, `format == BACKUP_FORMAT_ID`, `formatVersion <= BACKUP_FORMAT_VERSION`, and `data/category_settings.json` and `data/carmaker.json` exist and parse. Otherwise show `restore_invalid_file`.

### 5.3 Settings Filter

Add to `BackupRestoreService`:

```kotlin
internal fun isPortableSettingKey(key: String): Boolean =
    !key.startsWith("sftp_") &&
    !key.startsWith("html_sync_imported_") &&
    key !in setOf("images_path", "data_path", "import_export_path")
```

- **Backup**: write only portable keys to `data/app_settings.json`.
- **Restore**: apply only portable keys (protects against a backup made by an older or edited file).
- Restore does **not** delete `app_settings` rows. It only updates the portable keys that are in the backup.

### 5.4 Which Photos Go Into the Backup

`imagesDir` = `ToyRepository(db).getDataPathSetting()`. If it is null or blank, back up the data with no photos and log a warning (Android always has it; desktop may not).

Build one map `lowercaseName -> actual file name` from `systemFileSystem.listOrNull(imagesDir)` once, so a name that differs only in letter case is still found.

Collect names in a `LinkedHashSet<String>` (no duplicates):
1. **Makers**: every space-separated token in `makers.bitmaps`.
2. **Toys, main photo**: `toys.picture` when not blank. When `picture` is blank and `has_picture == "y"`, use the file name part of `resolveImageUri(prefix, refNum)` (prefix from `category_settings.image_prefix`).
3. **Toys, other photos**: every space-separated token in `toys.bitmaps`.

For each name: if `imagesDir/name` is a regular file, add entry `images/<name>`; else, if the case-insensitive map has it, add `images/<name as written in the DB>` with the actual file as source. If the file is missing, skip it and count it in `missingPhotos` (log with `GcLog.w`). The entry name is the name **as written in the DB**, so restore finds it by exact name.

### 5.5 Archive Rules

- Keep each photo's modified time: `ZipEntry.time = file.lastModified()` on backup; `File.setLastModified(entry.time)` on restore. This keeps `SyncImage` from downloading photos again (Section 4).
- JSON entries: `Deflater.DEFAULT_COMPRESSION`. Photo entries: `Deflater.BEST_SPEED` (JPEG/PNG do not compress more).
- **Unsafe names**: on restore, accept only entries `images/<name>` where `<name>` is not empty, has no `/`, `\`, or `..`, and is not `.` or `..`. Skip and log every other entry outside `manifest.json` and `data/`.
- Restore writes photos into `imagesDir`. A photo with the same name is replaced. Photos that are not in the backup stay.

---

## 6. Code Design

### 6.1 New and Changed Files

| File | Change |
|---|---|
| `commonMain/.../database/Database.kt` | Add `fun <T> transaction(block: () -> T): T` to `ToyDatabase`. |
| `desktopMain/.../database/DesktopDatabase.kt` | Implement: `autoCommit = false`, run block, `commit()`; on throw `rollback()` and rethrow; `finally autoCommit = true`. |
| `androidMain/.../database/AndroidDatabase.kt` | Implement: `beginTransaction()`, block, `setTransactionSuccessful()`, `finally endTransaction()`. |
| `wasmJsMain/.../database/WasmDatabase.wasmJs.kt` | Implement with `execute("BEGIN")` / `execute("COMMIT")` / `execute("ROLLBACK")`. |
| `commonMain/.../platform/BackupFileHelper.kt` | **New** `expect object` (6.2). |
| `desktopMain/.../platform/BackupFileHelper.desktop.kt` | **New** actual (6.3). |
| `androidMain/.../platform/BackupFileHelper.android.kt` | **New** actual (6.4). |
| `wasmJsMain/.../platform/BackupFileHelper.wasmJs.kt` | **New** actual: every call returns `Failed("not supported on web")`. Never return success. |
| `commonMain/.../platform/BackupArchive.kt` | **New** `expect object` (6.5). |
| `desktopMain/.../platform/BackupArchive.desktop.kt` and `androidMain/.../platform/BackupArchive.android.kt` | **New**, same JVM code in both (no shared `jvmMain` source set exists; do not add one). |
| `wasmJsMain/.../platform/BackupArchive.wasmJs.kt` | **New**: every function throws `UnsupportedOperationException`. |
| `commonMain/.../platform/StoragePermission.kt` + 3 actuals | **New** `@Composable expect fun rememberStoragePermissionRequest(): suspend () -> Boolean` (6.6). |
| `commonMain/.../service/BackupRestoreService.kt` | **New** service object (6.7). |
| `commonMain/.../service/ImportExportService.kt` | Add an optional key filter to `importAppSettings` and `exportAppSettings`: `keyFilter: (String) -> Boolean = { true }`. No other change. |
| `commonMain/.../ui/BackupRestoreCard.kt` | **New** composable with its own state and dialogs (Section 7). |
| `commonMain/.../ui/SettingsScreen.kt` | Show `BackupRestoreCard` in both layouts; new parameter `onCollectionRestored`; rename HTML card text (7.6). |
| `commonMain/.../ui/ToyDbNavigation.kt` | Pass `onCollectionRestored` (7.5). |
| `androidMain/AndroidManifest.xml` | Add permissions (D4). |
| `androidMain/.../AppMainActivity.kt` | Add `GcAppInfo.application_Context = application` at the start of `onCreate`, before `setContent`. |
| strings ×6 | Section 8. |
| Docs | Section 10, Phase 7. |

Package for new platform files: `com.gepetto.toydb.platform` (path `.../kotlin/com/gepetto/toydb/platform/`).

### 6.2 `BackupFileHelper` (commonMain)

```kotlin
package com.gepetto.toydb.platform

import okio.Path
import okio.Sink

const val ANDROID_BACKUP_FILE_NAME = "toy_collection_backup.zip"

sealed interface BackupSaveResult {
    data class Saved(val location: String) : BackupSaveResult   // shown to the user
    data object Cancelled : BackupSaveResult
    data class Failed(val message: String) : BackupSaveResult
}

sealed interface BackupOpenResult {
    /** [isTemporary] = true: call [BackupFileHelper.release] when done. */
    data class Opened(val path: Path, val isTemporary: Boolean) : BackupOpenResult
    data object Cancelled : BackupOpenResult
    data object NotFound : BackupOpenResult      // Android only
    data class Failed(val message: String) : BackupOpenResult
}

expect object BackupFileHelper {
    /** Gets a destination, then calls [write] with a sink to it. [write] runs on the calling thread. */
    suspend fun saveBackup(suggestedName: String, dialogTitle: String, write: (Sink) -> Unit): BackupSaveResult
    /** Gets a backup file that can be read with random access. */
    suspend fun openBackup(dialogTitle: String): BackupOpenResult
    fun release(path: Path)
}
```

Call both functions from `ioDispatcher`.

### 6.3 Desktop Actual

- `saveBackup`:
  1. Show `java.awt.FileDialog(null as Frame?, dialogTitle, FileDialog.SAVE)` with `file = suggestedName`. Run it with `SwingUtilities.invokeAndWait` unless already on the EDT (same pattern as `selectFileDialog`). Call `dispose()` after.
  2. No file chosen → `Cancelled`.
  3. Add `.zip` if the chosen name does not end with `.zip` (ignore case).
  4. Write to `<target>.partial` with `File.sink()` from okio (`okio.sink`), call `write`, close it, then move it over the target (`Files.move(..., REPLACE_EXISTING, ATOMIC_MOVE)`; if `ATOMIC_MOVE` is not supported, retry without it). On any error delete the `.partial` file and return `Failed(e.message)`.
  5. Return `Saved(target.absolutePath)`.
- `openBackup`: `selectFileDialog(dialogTitle, listOf("zip", "ZIP"))`. Null → `Cancelled`. Return `Opened(path.toPath(), isTemporary = false)`. (Note: the `.zip` filter does nothing on Windows; that is acceptable.)
- `release`: no-op for non-temporary paths.
- Default suggested name (built in the UI): `toy_collection_backup.zip`.

### 6.4 Android Actual

Context: `club.gepetto.utils.GcAppInfo.application_Context as? Context`; if null return `Failed("no context")`.
Always use `ANDROID_BACKUP_FILE_NAME`; ignore `suggestedName` and `dialogTitle`.

- `saveBackup`:
  - **API 29+**: same MediaStore logic as Race Director's `FileExportHelper.android.kt` (find existing entry in `MediaStore.Downloads` with `DISPLAY_NAME = ?` and `RELATIVE_PATH LIKE 'Download%'`; else insert with `IS_PENDING = 1`). Open `resolver.openOutputStream(uri, "wt")`, wrap with okio `.sink()`, call `write`, close, then set `IS_PENDING = 0` for a new entry. On error after insert of a new entry, delete that entry.
  - **API 24–28**: `File(Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS), ANDROID_BACKUP_FILE_NAME)`, write with `.partial` + rename as on desktop.
  - Return `Saved("Downloads/$ANDROID_BACKUP_FILE_NAME")`.
- `openBackup`:
  - **API 29+**: query as above, sorted by `DATE_MODIFIED DESC`. No row → `NotFound`. Copy `openInputStream(uri)` to `File(context.cacheDir, "restore_backup.zip")` with streaming copy (`source().buffer().readAll(sink)`), return `Opened(path, isTemporary = true)`.
  - **API 24–28**: public Downloads file; missing → `NotFound`; else `Opened(path, isTemporary = false)`.
- `release`: delete the file if it is inside `context.cacheDir`.

### 6.5 `BackupArchive` (commonMain contract, JVM actuals)

```kotlin
data class BackupPhoto(val entryName: String, val source: Path)   // entryName = "images/<name>"

expect object BackupArchive {
    /** Streams [textEntries] (UTF-8) then [photos] into [sink]. Calls [onPhoto] after each photo. Closes [sink]. */
    fun write(sink: Sink, textEntries: Map<String, String>, photos: List<BackupPhoto>, onPhoto: (done: Int, total: Int) -> Unit)
    /** Returns the text of "manifest.json" and every "data/*.json" entry. Does not read photos. */
    fun readTextEntries(archive: Path): Map<String, String>
    /** Number of "images/" entries that pass the name check (Section 5.5). */
    fun countPhotos(archive: Path): Int
    /** Streams every valid "images/" entry into [targetDir]; keeps modified time. Returns the count written. */
    fun extractPhotos(archive: Path, targetDir: Path, onPhoto: (done: Int, total: Int) -> Unit): Int
}
```

JVM actual:
- `write`: `ZipOutputStream(sink.buffer().outputStream())`. For each photo, open `FileInputStream`, copy with an 8 KB buffer. Do not read a whole photo into memory.
- Read functions: `java.util.zip.ZipFile(archive.toFile())` (random access; the file is on disk). Read JSON with `zip.getInputStream(entry).readBytes().decodeToString()`.
- `extractPhotos`: create `targetDir` if missing; write each photo to `<name>.partial` then rename over `<name>` (so a stopped restore never leaves a half photo with the real name); then `setLastModified(entry.time)` when `entry.time > 0`.

### 6.6 Storage Permission (D4)

- Manifest:
  ```xml
  <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />
  <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="28" />
  ```
- Android actual of `rememberStoragePermissionRequest()`: returns a suspend function. On API 29+ it returns `true`. On API 24–28 it returns `true` if both permissions are granted (`ContextCompat.checkSelfPermission`); else it launches `ActivityResultContracts.RequestMultiplePermissions` (from `rememberLauncherForActivityResult`) and waits for the result with a `CompletableDeferred<Boolean>`.
- Desktop and web actuals: return `{ true }`.
- If the result is `false`, show `backup_permission_denied` and stop.

### 6.7 `BackupRestoreService` (commonMain)

```kotlin
object BackupRestoreService {
    data class BackupSummary(val categories: Int, val makers: Int, val toys: Int, val photos: Int, val missingPhotos: Int = 0)
    class InvalidBackupException(message: String) : Exception(message)
    class ValidatedBackup internal constructor(val archive: Path, val manifest: BackupManifest, internal val texts: Map<String, String>)

    data class BackupContent(val textEntries: Map<String, String>, val photos: List<BackupPhoto>, val summary: BackupSummary)

    /** Builds all JSON texts (incl. manifest.json) and the photo list. Writes nothing. Easy to unit test. */
    fun collectBackupContent(db: ToyDatabase): BackupContent

    fun writeBackup(db: ToyDatabase, sink: Sink, onPhoto: (Int, Int) -> Unit): BackupSummary

    /** Reads and checks the archive. Throws InvalidBackupException. Changes nothing. */
    fun readBackup(archive: Path): ValidatedBackup

    /** Throws on error. On error the DB is unchanged (transaction rollback); photos may be partly copied. */
    fun restoreBackup(db: ToyDatabase, backup: ValidatedBackup, imagesDir: Path, onPhoto: (Int, Int) -> Unit): BackupSummary
}
```

`readBackup` must parse every `data/*.json` with the existing serializers (`JsonCategorySettingsFile`, `JsonMakersFile`, `JsonToysFile`, `JsonAppSettingsFile`) so a broken file fails **before** anything changes. The manifest counts are used in the confirm dialog.

`restoreBackup` order (**the order is required**, see Section 4 `SyncImage` fact):
1. Check `imagesDir == ToyRepository(db).getDataPathSetting()`; else throw.
2. `BackupArchive.extractPhotos(backup.archive, imagesDir, onPhoto)`.
3. `db.transaction { ... }`:
   1. `DELETE FROM toys`, `DELETE FROM makers`, `DELETE FROM category_settings` (in this order).
   2. `importCategorySettings(db, data/category_settings.json)`.
   3. `importMakers(db, data/carmaker.json)`.
   4. For each category in the restored `category_settings`: if `data/{imagePrefix}list.json` exists, `importToys(db, category, it)`.
   5. If `data/app_settings.json` exists: `importAppSettings(db, it, keyFilter = ::isPortableSettingKey)`.
4. Count rows after the commit and return the summary.

Do not run steps 2–3 on the main thread.

---

## 7. User Interface

### 7.1 Placement

New card `BackupRestoreCard` in `ui/BackupRestoreCard.kt`. In `SettingsScreen`, put it in **both** layouts, inside the existing `if (!isWebPlatform())` block, directly **above** the `ImportExportActions(...)` call, with `Spacer(Modifier.height(GcSpacing.Standard))` between them. The feature is not shown on web.

```
┌────────────────────────────────────────────────────────┐
│ Backup & Restore                                        │
│ Save a copy of your whole collection, including makers  │
│ and photos, in one file. Use it to recover your         │
│ collection or to move it to another device.             │
│ [ Back Up Collection ]   [ Restore Collection ]         │
└────────────────────────────────────────────────────────┘
┌────────────────────────────────────────────────────────┐
│ Create Website Pages                                   │
│ [ Create Pages ]                                        │
└────────────────────────────────────────────────────────┘
```

Card style: same as `StatusBanner` (`Card`, `sysBackgroundColor()`, `BorderStroke(1.dp, outline.copy(alpha = 0.2f))`). "Back Up Collection" = `Button`; "Restore Collection" = `OutlinedButton`. Buttons share the row (`Modifier.weight(1f)`) and are disabled while work runs.

Signature:
```kotlin
@Composable
fun BackupRestoreCard(
    db: ToyDatabase,
    dataPath: String?,
    onCollectionRestored: () -> Unit,
    onSetStatus: (String) -> Unit,
    modifier: Modifier = Modifier
)
```

### 7.2 State (local `remember`, Rule R7)

```kotlin
private enum class BackupPhase { Idle, Saving, Saved, SaveFailed, Opening, Confirm, Restoring, Restored, RestoreFailed, NotFound, Invalid, NoDataDir, PermissionDenied }
```
Plus: `progressDone`, `progressTotal`, `resultMessage: String`, `pendingBackup: ValidatedBackup?`, `pendingOpen: BackupOpenResult.Opened?`.

### 7.3 Back Up Flow

1. Request storage permission (6.6). Denied → `PermissionDenied`.
2. Phase `Saving` (progress dialog, not dismissible). On `ioDispatcher`: `BackupFileHelper.saveBackup("toy_collection_backup.zip", getString(backup_save_dialog_title)) { sink -> summary = BackupRestoreService.writeBackup(db, sink) { d, t -> progress } }`. Update progress state on the main thread.
3. `Saved` → dialog `backup_done_title` / `backup_done_msg(toys, photos, location)`. `Cancelled` → back to `Idle` with no dialog. `Failed` or exception → `backup_failed`.
4. Set the status banner text through `onSetStatus`.

### 7.4 Restore Flow

1. Desktop only: if `dataPath` is null or blank → `NoDataDir` dialog (`backup_no_data_dir`).
2. Request storage permission. Denied → `PermissionDenied`.
3. `BackupFileHelper.openBackup(getString(restore_open_dialog_title))` on `ioDispatcher`:
   - `Cancelled` → `Idle`. `NotFound` → `NotFound` dialog (`restore_missing_title` / `restore_missing_msg`). `Failed` → `RestoreFailed`.
4. `BackupRestoreService.readBackup(path)`. `InvalidBackupException` → `Invalid` dialog (`restore_invalid_file`).
5. `Confirm` dialog: title `restore_confirm_title`, text `restore_confirm_msg(toys, makers, photos)` from the manifest. Buttons: `OutlinedButton` `cancel` (existing key), `Button` with `containerColor = MaterialTheme.colorScheme.error` and text `restore_confirm_btn`.
6. Phase `Restoring` (progress dialog, not dismissible; show `backup_progress` while photos are copied). Run `restoreBackup(db, backup, dataPath.toPath(), ...)` on `ioDispatcher`.
7. Success → call `onCollectionRestored()` on the main thread, then `Restored` dialog (`restore_done_title` / `restore_done_msg`). Error → `RestoreFailed` with `restore_failed(message)`.
8. In a `finally`, if the opened file was temporary, call `BackupFileHelper.release(path)`. Also release it when the user cancels the confirm dialog.

### 7.5 Refresh After Restore

- `SettingsScreen`: new parameter `onCollectionRestored: () -> Unit = {}`. Inside the `BackupRestoreCard` callback, first reload local state: `categoriesList = repository.getCategorySettings()`, `appTitle = repository.getAppTitleSetting()`, `htmlBaseUrl = repository.getBaseUrlSetting()`; then call `onCollectionRestored()`.
- `ToyDbNavigation` (`entry<Destination.Settings>`, ~line 490): pass
  ```kotlin
  onCollectionRestored = {
      categoriesSettings = repository.getCategorySettings()
      themeMode = repository.getThemeSetting()
      appTitle = repository.getAppTitleSetting()
      onAppTitleChanged?.invoke(appTitle)
      syncTrigger++
  }
  ```
  Check the names `categoriesSettings`, `themeMode`, `appTitle`, `syncTrigger`, `onAppTitleChanged` before you use them.

### 7.6 "Create Website Pages" (D2)

- Change the **values** (not the keys) of these existing strings in all 6 files (Section 8.2): `import_export_actions_title`, `export_html_btn`, `status_exporting_html`, `html_export_complete`, `html_export_success_title`, `html_export_success_desc`, `html_export_success_count`, `error_html`, `error_html_no_dir`.
- Replace the hard-coded status texts at `SettingsScreen.kt:1624` and `:1631` with the new keys `website_status_creating` and `website_status_done`.
- Rename the composable `ImportExportActions` to `WebsitePagesActions` (definition and both call sites). No other behavior change.

### 7.7 Dialogs

All new dialogs: `AlertDialog` with `containerColor = sysBackgroundColor()`, text color `sysTextColor()`, dark-mode border (Rule R6). Progress dialogs: `onDismissRequest = {}`, no buttons, `CircularProgressIndicator` plus `LinearProgressIndicator(progress = { done / total })` and `backup_progress` text when `total > 0`. Result dialogs: one `Button` with the existing `ok` key.

### 7.8 Info Tab "Server Sync" (D8)

- `ui/InfoScreen.kt`: the tab label comes from `InfoTopic.BACKUP.titleRes = Res.string.info_tab_backup`. Change only the **value** of `info_tab_backup` in all 6 files (Section 8.2).
- Do not rename the enum constant `InfoTopic.BACKUP`, its id `"backup"`, the composable `BackupTabContent`, or the file `{lang}_sftp_setup.md`. Other code can open the tab by the id `"backup"`.
- Do not change the tab content heading `backup_sync_title` ("Backup &amp; Synchronization") or `backup_sync_description` unless the user approves it (see Section 12, ISSUE-01).

### 7.9 Previews

`BackupRestoreCard` previews: Light/Dark and Landscape, in `GcTheme {}`. To preview without a DB, split the visual part into `BackupRestoreCardContent(isWorking: Boolean, onBackUp: () -> Unit, onRestore: () -> Unit)` and preview that.

---

## 8. Strings

### 8.1 New Keys (add to all 6 files)

| Key | en | pt | de | es | fr | it |
|---|---|---|---|---|---|---|
| `backup_title` | Backup &amp; Restore | Backup e Restauração | Sichern &amp; Wiederherstellen | Copia de seguridad y restauración | Sauvegarde et restauration | Backup e ripristino |
| `backup_desc` | Save a copy of your whole collection, including makers and photos, in one file. Use it to recover your collection or to move it to another device. | Salve uma cópia de toda a sua coleção, incluindo fabricantes e fotos, em um único arquivo. Use-a para recuperar sua coleção ou para levá-la para outro dispositivo. | Speichern Sie eine Kopie Ihrer gesamten Sammlung mit Herstellern und Fotos in einer Datei. Damit können Sie Ihre Sammlung wiederherstellen oder auf ein anderes Gerät übertragen. | Guarde una copia de toda su colección, con fabricantes y fotos, en un solo archivo. Úsela para recuperar su colección o para llevarla a otro dispositivo. | Enregistrez une copie de toute votre collection, avec les fabricants et les photos, dans un seul fichier. Utilisez-la pour récupérer votre collection ou la transférer sur un autre appareil. | Salva una copia dell\'intera collezione, con produttori e foto, in un unico file. Usala per recuperare la collezione o per spostarla su un altro dispositivo. |
| `backup_btn` | Back Up Collection | Fazer Backup da Coleção | Sammlung sichern | Hacer copia de la colección | Sauvegarder la collection | Esegui backup della collezione |
| `restore_btn` | Restore Collection | Restaurar Coleção | Sammlung wiederherstellen | Restaurar colección | Restaurer la collection | Ripristina collezione |
| `backup_saving` | Saving a copy of your collection... | Salvando uma cópia da sua coleção... | Kopie Ihrer Sammlung wird gespeichert... | Guardando una copia de su colección... | Enregistrement d\'une copie de votre collection... | Salvataggio di una copia della collezione... |
| `backup_progress` | %1$d of %2$d photos | %1$d de %2$d fotos | %1$d von %2$d Fotos | %1$d de %2$d fotos | %1$d sur %2$d photos | %1$d di %2$d foto |
| `backup_done_title` | Backup Complete | Backup Concluído | Sicherung abgeschlossen | Copia de seguridad completada | Sauvegarde terminée | Backup completato |
| `backup_done_msg` | A copy of your collection (%1$d toys, %2$d photos) was saved to:\n%3$s | Uma cópia da sua coleção (%1$d brinquedos, %2$d fotos) foi salva em:\n%3$s | Eine Kopie Ihrer Sammlung (%1$d Spielzeuge, %2$d Fotos) wurde gespeichert unter:\n%3$s | Se guardó una copia de su colección (%1$d juguetes, %2$d fotos) en:\n%3$s | Une copie de votre collection (%1$d jouets, %2$d photos) a été enregistrée dans :\n%3$s | Una copia della collezione (%1$d giocattoli, %2$d foto) è stata salvata in:\n%3$s |
| `backup_failed` | The backup could not be saved: %1$s | Não foi possível salvar o backup: %1$s | Die Sicherung konnte nicht gespeichert werden: %1$s | No se pudo guardar la copia de seguridad: %1$s | La sauvegarde n\'a pas pu être enregistrée : %1$s | Impossibile salvare il backup: %1$s |
| `restore_confirm_title` | Restore Your Collection? | Restaurar sua coleção? | Sammlung wiederherstellen? | ¿Restaurar su colección? | Restaurer votre collection ? | Ripristinare la collezione? |
| `restore_confirm_msg` | This replaces all toys, makers and categories on this device with the ones in the backup (%1$d toys, %2$d makers, %3$d photos). Photos with the same name are replaced. You cannot undo this. | Isto substitui todos os brinquedos, fabricantes e categorias deste dispositivo pelos do backup (%1$d brinquedos, %2$d fabricantes, %3$d fotos). Fotos com o mesmo nome são substituídas. Não é possível desfazer. | Alle Spielzeuge, Hersteller und Kategorien auf diesem Gerät werden durch die aus der Sicherung ersetzt (%1$d Spielzeuge, %2$d Hersteller, %3$d Fotos). Fotos mit gleichem Namen werden ersetzt. Dies kann nicht rückgängig gemacht werden. | Esto reemplaza todos los juguetes, fabricantes y categorías de este dispositivo por los de la copia (%1$d juguetes, %2$d fabricantes, %3$d fotos). Las fotos con el mismo nombre se reemplazan. No se puede deshacer. | Tous les jouets, fabricants et catégories de cet appareil seront remplacés par ceux de la sauvegarde (%1$d jouets, %2$d fabricants, %3$d photos). Les photos portant le même nom seront remplacées. Cette action est irréversible. | Tutti i giocattoli, i produttori e le categorie su questo dispositivo verranno sostituiti con quelli del backup (%1$d giocattoli, %2$d produttori, %3$d foto). Le foto con lo stesso nome verranno sostituite. L\'operazione non può essere annullata. |
| `restore_confirm_btn` | Restore | Restaurar | Wiederherstellen | Restaurar | Restaurer | Ripristina |
| `restore_running` | Restoring your collection... | Restaurando sua coleção... | Sammlung wird wiederhergestellt... | Restaurando su colección... | Restauration de votre collection... | Ripristino della collezione... |
| `restore_done_title` | Restore Complete | Restauração Concluída | Wiederherstellung abgeschlossen | Restauración completada | Restauration terminée | Ripristino completato |
| `restore_done_msg` | Your collection was restored: %1$d toys, %2$d makers, %3$d photos. | Sua coleção foi restaurada: %1$d brinquedos, %2$d fabricantes, %3$d fotos. | Ihre Sammlung wurde wiederhergestellt: %1$d Spielzeuge, %2$d Hersteller, %3$d Fotos. | Su colección se restauró: %1$d juguetes, %2$d fabricantes, %3$d fotos. | Votre collection a été restaurée : %1$d jouets, %2$d fabricants, %3$d photos. | La collezione è stata ripristinata: %1$d giocattoli, %2$d produttori, %3$d foto. |
| `restore_failed` | The collection could not be restored: %1$s. Your toys, makers and categories were not changed. | Não foi possível restaurar a coleção: %1$s. Seus brinquedos, fabricantes e categorias não foram alterados. | Die Sammlung konnte nicht wiederhergestellt werden: %1$s. Ihre Spielzeuge, Hersteller und Kategorien wurden nicht geändert. | No se pudo restaurar la colección: %1$s. Sus juguetes, fabricantes y categorías no se modificaron. | La collection n\'a pas pu être restaurée : %1$s. Vos jouets, fabricants et catégories n\'ont pas été modifiés. | Impossibile ripristinare la collezione: %1$s. Giocattoli, produttori e categorie non sono stati modificati. |
| `restore_invalid_file` | This file is not a Toy Database Manager backup. | Este arquivo não é um backup do Toy Database Manager. | Diese Datei ist keine Sicherung von Toy Database Manager. | Este archivo no es una copia de seguridad de Toy Database Manager. | Ce fichier n\'est pas une sauvegarde de Toy Database Manager. | Questo file non è un backup di Toy Database Manager. |
| `restore_missing_title` | Backup File Not Found | Arquivo de Backup Não Encontrado | Sicherungsdatei nicht gefunden | Archivo de copia no encontrado | Fichier de sauvegarde introuvable | File di backup non trovato |
| `restore_missing_msg` | The file \'toy_collection_backup.zip\' is not in the Downloads folder. Put the backup file in Downloads, then try again. | O arquivo \'toy_collection_backup.zip\' não está na pasta Downloads. Coloque o arquivo de backup em Downloads e tente novamente. | Die Datei \'toy_collection_backup.zip\' ist nicht im Ordner „Downloads“. Legen Sie die Sicherungsdatei in „Downloads“ ab und versuchen Sie es erneut. | El archivo \'toy_collection_backup.zip\' no está en la carpeta Descargas. Coloque el archivo de copia en Descargas e inténtelo de nuevo. | Le fichier \'toy_collection_backup.zip\' ne se trouve pas dans le dossier Téléchargements. Placez le fichier de sauvegarde dans Téléchargements, puis réessayez. | Il file \'toy_collection_backup.zip\' non è nella cartella Download. Inserisci il file di backup in Download e riprova. |
| `backup_no_data_dir` | Select a data directory first. Your photos are kept there. | Selecione primeiro um diretório de dados. Suas fotos ficam nele. | Wählen Sie zuerst ein Datenverzeichnis. Dort werden Ihre Fotos gespeichert. | Seleccione primero un directorio de datos. Allí se guardan sus fotos. | Sélectionnez d\'abord un dossier de données. Vos photos y sont conservées. | Seleziona prima una cartella dati. Le foto sono conservate lì. |
| `backup_permission_denied` | Toy Database Manager needs permission to use the Downloads folder. | O Toy Database Manager precisa de permissão para usar a pasta Downloads. | Toy Database Manager benötigt die Berechtigung, den Ordner „Downloads“ zu verwenden. | Toy Database Manager necesita permiso para usar la carpeta Descargas. | Toy Database Manager a besoin d\'une autorisation pour utiliser le dossier Téléchargements. | Toy Database Manager ha bisogno dell\'autorizzazione per usare la cartella Download. |
| `backup_save_dialog_title` | Save Backup | Salvar Backup | Sicherung speichern | Guardar copia de seguridad | Enregistrer la sauvegarde | Salva backup |
| `restore_open_dialog_title` | Choose a Backup File | Escolha um Arquivo de Backup | Sicherungsdatei auswählen | Elija un archivo de copia | Choisir un fichier de sauvegarde | Scegli un file di backup |
| `website_status_creating` | Creating website pages in %1$s... | Criando páginas do site em %1$s... | Webseiten werden in %1$s erstellt... | Creando páginas web en %1$s... | Création des pages du site dans %1$s... | Creazione delle pagine del sito in %1$s... |
| `website_status_done` | Created %1$d website pages in %2$s. | %1$d páginas do site criadas em %2$s. | %1$d Webseiten in %2$s erstellt. | Se crearon %1$d páginas web en %2$s. | %1$d pages du site créées dans %2$s. | %1$d pagine del sito create in %2$s. |

Reuse existing keys `ok` and `cancel`. Do not add keys for "Import"/"Export".

### 8.2 Changed Values of Existing Keys (D2)

| Key | en | pt | de | es | fr | it |
|---|---|---|---|---|---|---|
| `import_export_actions_title` | Create Website Pages | Criar Páginas do Site | Webseiten erstellen | Crear páginas web | Créer les pages du site | Crea pagine del sito |
| `export_html_btn` | Create Pages | Criar Páginas | Seiten erstellen | Crear páginas | Créer les pages | Crea pagine |
| `status_exporting_html` | Creating website pages... | Criando páginas do site... | Webseiten werden erstellt... | Creando páginas web... | Création des pages du site... | Creazione delle pagine del sito... |
| `html_export_complete` | Website pages created! | Páginas do site criadas! | Webseiten erstellt! | ¡Páginas web creadas! | Pages du site créées ! | Pagine del sito create! |
| `html_export_success_title` | Website Pages Created | Páginas do Site Criadas | Webseiten erstellt | Páginas web creadas | Pages du site créées | Pagine del sito create |
| `html_export_success_desc` | The website pages were created in this folder: | As páginas do site foram criadas nesta pasta: | Die Webseiten wurden in diesem Ordner erstellt: | Las páginas web se crearon en esta carpeta: | Les pages du site ont été créées dans ce dossier : | Le pagine del sito sono state create in questa cartella: |
| `html_export_success_count` | Created %1$d pages. | %1$d páginas criadas. | %1$d Seiten erstellt. | Se crearon %1$d páginas. | %1$d pages créées. | %1$d pagine create. |
| `error_html` | The website pages could not be created: %1$s | Não foi possível criar as páginas do site: %1$s | Die Webseiten konnten nicht erstellt werden: %1$s | No se pudieron crear las páginas web: %1$s | Les pages du site n\'ont pas pu être créées : %1$s | Impossibile creare le pagine del sito: %1$s |
| `info_tab_backup` | Server Sync | Sincronização com Servidor | Server-Synchronisierung | Sincronización con servidor | Synchronisation serveur | Sincronizzazione server |
| `error_html_no_dir` | Select a data directory first. The website pages are created there. | Selecione primeiro um diretório de dados. As páginas do site são criadas nele. | Wählen Sie zuerst ein Datenverzeichnis. Dort werden die Webseiten erstellt. | Seleccione primero un directorio de datos. Allí se crean las páginas web. | Sélectionnez d\'abord un dossier de données. Les pages du site y sont créées. | Seleziona prima una cartella dati. Le pagine del sito vengono create lì. |

---

## 9. Verification

### 9.1 Unit Tests

File: `composeApp/src/desktopTest/kotlin/com/gepetto/toydb/service/BackupRestoreServiceTest.kt` (`kotlin.test`, class PascalCase, methods `testXxx`).
Use `DesktopToyDatabase` on a DB file inside `java.nio.file.Files.createTempDirectory(...)`, and a photos folder in the same temp directory. Delete the temp directory in `finally`.

1. `testBackupAndRestoreRoundTrip` — seed 2 categories, 3 makers (one with `bitmaps`), 5 toys (with `picture` and `bitmaps`), 4 photo files. Back up to a temp zip, clear the DB and photo folder, restore. Every column must match the values `importToys`/`importMakers` produce (they recompute `maker_combo` and read photo metadata), photo bytes must match, and photo modified times must match.
2. `testSecretsAreNotBackedUp` — seed `sftp_password`, `sftp_host`, `images_path`, `html_sync_imported_hash_slot`, `app_title`. Only `app_title` is in `data/app_settings.json`.
3. `testRestoreDoesNotOverwriteLocalOnlySettings` — a backup zip whose `app_settings.json` contains `images_path` and `sftp_password` must not change those keys.
4. `testInvalidBackupChangesNothing` — zip without `manifest.json`, and a zip with a broken `carmaker.json`: `readBackup` throws `InvalidBackupException`, DB and photo folder are unchanged.
5. `testUnsafeEntryNamesAreSkipped` — entries `images/../evil.jpg`, `images/a/b.jpg`, `../x.jpg` are not written anywhere.
6. `testFailedRestoreRollsBack` — wrap the test DB in a `ToyDatabase` that delegates every call but throws on the first `INSERT OR REPLACE INTO toys`. Run `restoreBackup` and check that toys, makers and categories are unchanged.
7. `testMissingPhotoIsSkipped` — a toy refers to a missing file: backup succeeds and `missingPhotos == 1`.

Run existing tests too (`JsonDateParserParityTest`, `JsonDateParserTest`, `HtmlSyncServiceHashTest`).

### 9.2 Build Commands

```bash
./gradlew :composeApp:desktopTest
./gradlew :composeApp:compileKotlinDesktop
./gradlew :composeApp:compileDebugKotlinAndroid
./gradlew :composeApp:compileKotlinWasmJs
```

### 9.3 Manual Checks

- **Desktop**: back up with the real data directory (~1.5 GB). Check memory stays normal and the progress moves. Restore on a copy of the app data, check toys, makers, categories, photos, app title and theme refresh without restart. Cancel both dialogs: nothing happens. No data directory: `backup_no_data_dir`.
- **Android emulator, API 34+**: back up → file in Downloads; restore; delete the file → "Backup File Not Found".
- **Android emulator, API 28**: permission prompt appears; deny → `backup_permission_denied`; allow → back up and restore work.
- **Web**: the card is not shown; the app still builds and runs.
- **Dark and light mode**: all new dialogs readable, border in dark mode.
- **All 6 languages**: no missing strings (switch the device language at least once for de and pt).

---

## 10. Execution Checklist

- [ ] **Phase 1 — Strings**
  - [ ] Add Section 8.1 keys to all 6 `strings.xml` files.
  - [ ] Change Section 8.2 values in all 6 files.
- [ ] **Phase 2 — Database transaction**
  - [ ] Add `transaction` to `ToyDatabase` and implement it for Desktop, Android, Wasm.
- [ ] **Phase 3 — Platform layer**
  - [ ] `GcAppInfo.application_Context = application` in `AppMainActivity.onCreate`.
  - [ ] Manifest permissions (6.6).
  - [ ] `BackupFileHelper` common + desktop + android + wasm.
  - [ ] `BackupArchive` common + desktop + android + wasm.
  - [ ] `rememberStoragePermissionRequest` common + desktop + android + wasm.
  - [ ] Build all 3 targets.
- [ ] **Phase 4 — Service**
  - [ ] `keyFilter` parameter in `importAppSettings` / `exportAppSettings`.
  - [ ] `BackupManifest`, `isPortableSettingKey`, `BackupRestoreService` (6.7, 5.x).
- [ ] **Phase 5 — UI**
  - [ ] `BackupRestoreCard.kt` with dialogs and previews.
  - [ ] Add the card to both layouts in `SettingsScreen`.
  - [ ] `onCollectionRestored` in `SettingsScreen` and `ToyDbNavigation`.
  - [ ] Website pages changes (7.6): new status keys, rename `ImportExportActions` → `WebsitePagesActions`.
  - [ ] Info tab "Server Sync" (7.8): check the tab label in all 6 languages.
- [ ] **Phase 6 — Tests and builds**
  - [ ] `BackupRestoreServiceTest` (9.1).
  - [ ] Section 9.2 commands all pass.
  - [ ] Section 9.3 manual checks (desktop at minimum; Android on emulator when available).
- [ ] **Phase 7 — Documentation** (use ASD-STE100 Simplified Technical English)
  - [ ] `.agents/HOW_IT_WORKS.md`:
    - §1: add a "Backup & Restore" capability; rename "Static Website Publisher" text to "Create Website Pages".
    - §2: add the `platform/` folder and new files to the source tree; note the manifest storage permissions.
    - §3: add `transaction(block)` to the `ToyDatabase` description.
    - §5: add a subsection "Backup & Restore" (file layout from Section 5, excluded settings, restore order, Android limitation from Section 3).
    - §8: add the Android backup location `Downloads/toy_collection_backup.zip`.
    - §7: the navigation diagram labels `SettingsScreen` as "Backup/Restore" and `InfoScreen` as "App info & user guide"; make sure the Info tabs are listed as About, Server Sync, Privacy Policy, Terms of Use.
    - §9: flow 3 is out of date (it says "Database Operations" / "Export HTML Web Pages"); change it to **Settings → Create Website Pages → Create Pages**. Add flow "Backing Up and Restoring Your Collection".
    - §11: add `:composeApp:desktopTest` and `:composeApp:compileKotlinWasmJs`.
  - [ ] `README.md`: add a "Backup & Restore" key feature; change "importing and exporting ... JSON" wording to plain words.
  - [ ] User-visible About files `composeResources/files/about.md` and `{en,de,es,fr,it,pt}_about.md`: replace the "Portability" bullet with:
    - `**Backup & Restore**: Save a copy of your whole collection, including photos, in one file. Restore it on this device or on another device.`
    - `**Website Pages**: Create website pages that show your collection.`
    Translate both bullets into each language file.
  - [ ] `SCHEMA.md`: if it lists `app_settings` keys, mark which keys are not in a backup (D5).
  - [ ] `.agents/TODO.txt`: mark `[x]` on the backup item and on "change info tab "Backup" to "Server Sync"".
  - [ ] User-visible guide `composeResources/files/{en,de,es,fr,it,pt}_sftp_setup.md` (and `sftp_setup.md` if it exists): if the text calls this tab "Backup", change it to "Server Sync" in each language.
  - [ ] This plan: Sections 10, 11, 12.

---

## 11. Change Log

| Date (UTC) | Version | Author / Agent | Changes Made |
| :--- | :--- | :--- | :--- |
| 2026-10-07 | 1.0.0 | Antigravity Agent | Initial plan. |
| 2026-10-07 | 2.0.0 | Claude (Opus 5.5) review | Reviewed against current code. Streaming design (no in-memory archive, ~1.5 GB photos); settings filter (no SFTP secrets or local paths in backups); DB transaction API; required restore order and photo timestamps (SyncImage); reuse existing JSON names (`carmaker.json`, `{prefix}list.json`), removed `toys.json`; `manifest.json`; unsafe entry names; Android permissions for API ≤ 28; full refresh after restore (`syncTrigger`); corrected Gradle tasks; user-friendly naming "Backup & Restore" (D1) and "Create Website Pages" (D2); complete strings in 6 languages; documentation phase. |
| 2026-10-07 | 2.1.0 | Claude (Opus 5.5) review | Added D8: Info tab "Backup" (SFTP guide) renamed to "Server Sync" (Section 7.8, string, checklist, docs, ISSUE-01). |

---

## 12. Bug & Issue Log

| Issue ID | Date | Affected Component | Description & Root Cause | Resolution Status | Fix Description |
| :--- | :--- | :--- | :--- | :--- | :--- |
| ISSUE-01 | 2026-10-07 | `InfoScreen` Server Sync tab | The tab content heading `backup_sync_title` still says "Backup & Synchronization", and `backup_sync_description` starts with "If you want to back up your collection data safely to the cloud...". After D8 the tab name and its heading do not match. | Open — waiting for user decision | Not in scope until the user approves. |
