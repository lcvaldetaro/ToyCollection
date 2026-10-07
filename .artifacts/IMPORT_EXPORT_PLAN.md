# Toy Collection: Backup & Restore Implementation Plan

**Target Document**: `ToyCollection/.agents/IMPORT_EXPORT_PLAN.md` (file name kept so existing links in `../.agents/TODO.txt` stay valid)
**Document Version**: 2.5.0
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
- Rename the toy form tab **"Restore"** (restoration work on a toy) to **"Restoration"**, so it is not confused with "Restore Collection" (Section 7.10, D16).
- Make the web sync replace the collection inside a database transaction, so a failed sync does not leave an empty collection (Section 6.9, D17).

---

## 2. Rules That Apply

Obey `~/valdetaro/.agents/AGENTS.md` and `ToyCollection/.agents/AGENTS.md`. The most important rules for this work:

> [!IMPORTANT]
> **R1 — User-facing words.** The user does not want technical words in the UI. In every user-visible text use **"Back Up"**, **"Backup"**, **"Restore"**, **"collection"**, **"photos"**, **"website pages"**. Do **not** use "import", "export", "database", "JSON", "zip" or "HTML" in user-visible text. (The Android file name `toy_collection_backup.zip` is the only exception, because the user must find that file.) Do not put the app name "Toy Database Manager" in new texts, because it contains "Database"; say "the app" instead.

> [!IMPORTANT]
> **R2 — `commonMain` stays pure KMP.** No `java.*`, `javax.*` or `android.*` imports in `commonMain`. Use `okio` (already a `commonMain` dependency) for paths and streams. Put zip, dialogs and MediaStore code behind `expect`/`actual`.

> [!IMPORTANT]
> **R3 — Never hold the whole backup in memory.** The desktop data directory has about **1.5 GB in 2,900 photos**. Stream every photo, one at a time, from disk into the zip and from the zip to disk. Only the JSON entries (a few MB) may be held in memory. No API in this plan may take or return the archive as a `ByteArray`.

> [!IMPORTANT]
> **R4 — Logging.** Use `club.gepetto.GcLog` only. No `println`, `System.out`, `android.util.Log`, `e.printStackTrace()`. Note that `GcLog.d(message, *args)` automatically gets the tag from the stack trace (`getStackTag()`). Do not pass `(tag, message)` as two separate strings; if no `%s` placeholder is in the first string, the second string is dropped. Use a single string with prefix `GcLog.w("Tag: message")` or `GcLog.e(t, "Tag: message")`.

> [!IMPORTANT]
> **R5 — Strings.** Every user-visible text is a string resource in all 6 files: `composeResources/values{,-de,-es,-fr,-it,-pt}/strings.xml`. Escape apostrophes as `\'` and `&` as `&amp;` (see existing entries). Native dialog titles also come from string resources (`getString(...)`).

> [!IMPORTANT]
> **R6 — UI.** Use `sysBackgroundColor()` / `sysTextColor()`. Every new dialog must have a border in dark mode: `Modifier.border(1.dp, MaterialTheme.colorScheme.outline, AlertDialogDefaults.shape)` when `club.gepetto.composeutils.isDark()` is true. Every new composable with UI has `@PreviewLightDark` and `@Preview(name = "Landscape", widthDp = 800, heightDp = 480)` previews wrapped in `GcTheme {}`. Long work runs on `club.gepetto.utils.ioDispatcher`.

> [!IMPORTANT]
> **R7 — Architecture.** Keep today's pattern: UI state lives in the composable (`remember`), logic lives in the service object. Do **not** add a ViewModel.

> [!IMPORTANT]
> **R8 — Git.** Never `git commit`, `git push` or `git add` unless the user orders it.

> [!IMPORTANT]
> **R9 — Build.** Never run the plain `./gradlew assemble` task (it also builds release versions). `:composeApp:assembleDebug` is allowed. Use the commands in Section 9.

> [!IMPORTANT]
> **R10 — Tests.** If an existing test fails, first assume the new code is wrong. Temporary test files go in a temp directory, never in the module root.

> [!IMPORTANT]
> **R11 — Living document.** Check off Section 10 items as you finish them. Add every code change to Section 11 and every problem to Section 12.

> [!IMPORTANT]
> **R12 — Android API 24–25.** minSdk is 24. In `androidMain` do **not** use `java.nio.file.*` (`Files`, `Paths`, `StandardCopyOption`) or `java.nio.file.attribute.FileTime` (all need API 26). Use `java.io.File` (`renameTo`, `delete`, `setLastModified`) and okio.

---

## 3. Decisions (Approved by the User on 2026-10-07)

| # | Topic | Decision |
|---|---|---|
| D1 | UI name | **"Backup & Restore"**. Buttons: **"Back Up Collection"** and **"Restore Collection"**. |
| D2 | HTML card | Rename user-visible text from "HTML Generation" to **"Create Website Pages"**. |
| D3 | Android restore source | Same as Race Director: read only the backup file in `Downloads` (`toy_collection_backup.zip`, or the name Android gave it, for example `toy_collection_backup (1).zip`, see Section 6.4). If it is not found, show the "Backup File Not Found" dialog. No file picker. |
| D4 | Android 9 and below | Add `WRITE_EXTERNAL_STORAGE` / `READ_EXTERNAL_STORAGE` with `maxSdkVersion="28"` and ask for the permission at run time. |
| D5 | Settings that are not in the backup | All `sftp_*` keys, `images_path`, `data_path`, `import_export_path`, and all `html_sync_imported_*` keys. (Restore **writes new** `html_sync_imported_*` values from the backup itself, see D7. They are never copied from `app_settings.json`.) |
| D6 | Architecture | Keep today's pattern (Rule R7). |
| D7 | Startup web sync | *(Changed 2026-10-07, v2.2.0, clarified v2.3.0.)* The web sync at app start may replace a restored collection **only when the server has data newer than the backup**. To do this, restore sets the sync markers (`html_sync_imported_date_*` / `html_sync_imported_hash_*`) from the backup's own JSON files (Section 6.7, step 4.6). Without this, empty markers (fresh install, `HtmlSyncService.kt:76`) would make the sync replace the restored collection at the next start. A restored collection with 1 or more toys is protected. A restored collection with 0 toys is not: `HtmlSyncService.kt:182-185` forces a sync when the `toys` table is empty. Also see known limitation 7. |
| D8 | Info tab & SFTP text | The Info screen tab "Backup" (SFTP guide) becomes **"Server Sync"**, heading becomes **"Server Synchronization"**, and `sftp_setup.md` bullet becomes **"Automatic Sync"** (ISSUE-01 approved by user). *(v2.4.0)* `sftp_setup.md` line 3 says "to synchronize" instead of "to synchronize and back up". Text only. |
| D9 | Restore vs. web sync at the same time | Add `CollectionWriteLock` (a shared `Mutex`). Restore and `HtmlSyncService.syncIfNewer` both hold it, so they never run at the same time (Section 6.8). This is a small change to `HtmlSyncService.kt`. |
| D10 | Android free space | Before Android copies the backup to `cacheDir`, check the free space. If it is too small, show `restore_no_space` (Section 6.4). *(v2.4.0)* On Android 9 and below (no copy), also check the free space for the photos (`filesDir.usableSpace < file size + 50 MB` → `restore_no_space`). |
| D11 | Desktop backup without a data directory | **Back Up** also requires a data directory on desktop. If there is none, **or the folder does not exist** *(v2.4.0)*, show `backup_no_data_dir` (same as Restore). No backup without photos. |
| D12 | Photo modified times | Store the exact modified time (milliseconds) of each photo in `photos.json` inside the backup, and set it on restore. Do not depend on the zip entry time (2-second, local time zone; `FileTime` needs API 26). Reason: `DesktopSftpService` compares modified times in seconds; a time shift makes the next SFTP sync send all photos again. |
| D13 | Back Up vs. web sync | *(Added 2026-10-07, v2.4.0.)* Back Up holds `CollectionWriteLock` while it reads the database (`prepareBackup`, Section 6.7). Photos are streamed after the lock is released. Reason: the startup web sync deletes all data and imports it again; a backup made at that time could miss toys, with no warning. |
| D14 | Category file names | *(Added v2.4.0.)* `manifest.json` has `categoryFiles` (category → entry name), and each category gets a unique file name (Section 5.6). Restore uses this map first. Reason: two categories can have the same image prefix (`SettingsScreen.kt:342` checks only the category id), and both would write `data/{prefix}list.json`. |
| D15 | Photos not found at backup | *(Added v2.4.0.)* When photos are not found, the "Backup Complete" dialog also shows `backup_missing_photos(count)`. Reason: on Android the data folder has only the photos that the app downloaded, so a backup can leave out many photos. |
| D16 | Toy form tab "Restore" | *(Added v2.4.0.)* Change the value of `tab_restoration` (restoration work on a toy) to "Restoration" (en), "Restaurierung" (de), "Restauración" (es), "Restauration" (fr). pt and it stay "Restauro". Text only (Section 7.10). |
| D17 | Web sync transaction | *(Added v2.4.0.)* `HtmlSyncService.syncIfNewer` runs its delete-and-import and the marker writes inside `db.transaction { }` (Section 6.9). A failed sync then leaves the collection unchanged. |

### Known limitations (accepted)
1. **Android 10 and later (D3):** the app can only find a file in Downloads that **this installation** created. After the app is reinstalled, or when the file was copied from a computer, restore reports "not found". This is the same as Race Director.
2. **Android 10 and later, name change:** if a `toy_collection_backup.zip` that this installation did not create is already in Downloads, Android saves the new backup as `toy_collection_backup (1).zip`. The "Backup Complete" dialog shows the real name, and restore finds it (Section 6.4).
3. **Android 10 and later, overwrite:** when this installation already made the backup file, a new backup writes over the same file (mode `"wt"`). If that write fails, the old backup is damaged. Same as Race Director.
4. **Main photo flag:** `importToys` sets `has_picture = 'n'` when `picture` is blank (`ImportExportService.kt:234-258`). A toy with `has_picture = 'y'` and a blank `picture` loses its main photo after restore. Current data has **0** such toys (checked 2026-10-07 in all `*list.json` files and `default_toydb.db`).
5. **Money values:** the JSON files store `value` and `amount_paid` with 2 decimals. A value of 0 or less is written as empty and comes back as 0 (`formatDouble`, `ImportExportService.kt:488-496`). The web sync and SFTP sync do the same.
6. **Toys without a category:** toys whose `toy_type` is not in `category_settings` are not in the backup, because toys are exported one category at a time. Back Up logs their count with `GcLog.w`.
7. **Categories only on the server (D7):** the web sync checks a marker for each category in the **server's** `category_settings.json` (`HtmlSyncService.kt:121-163`). If the server has a category that the backup does not have, that category has no marker, and the startup sync replaces the restored collection.

Record all seven in `../.agents/HOW_IT_WORKS.md` (Phase 7).

---

## 4. Current Code Facts (Checked 2026-10-07)

The executing agent depends on these facts. Check them before Phase 2.

| Fact | Where |
|---|---|
| `ToyDatabase` has only `execute`, `query`, `close`. No transaction support. | `commonMain/.../database/Database.kt:17` |
| Platform databases: JDBC (`DesktopDatabase.kt`), Android `SQLiteDatabase` (`AndroidDatabase.kt`), sql.js (`WasmDatabase.wasmJs.kt`). | `*/database/` |
| JSON envelope classes and import/export functions for each table already exist: `exportCategorySettings`, `exportMakers`, `exportToys(db, toyType)`, `exportAppSettings`, `importCategorySettings`, `importMakers`, `importToys(db, toyType, json)`, `importAppSettings`. | `commonMain/.../service/ImportExportService.kt` |
| `importToys` and `processBitmaps` read size and timestamp **from the photo file on disk** in the folder `getImagesPath(db)` returns. `getImagesPath` reads **only** `app_settings.images_path` (not `data_path`). If the file is not there, they use the values in the JSON; if those are empty, the photo name is dropped. | `ImportExportService.kt:122-305` |
| The existing JSON file names are `category_settings.json`, `carmaker.json` (makers), `{imagePrefix}list.json` (one per category). The web sync and the headless CLI use the same names. Every JSON envelope has a `date` field made by `getCurrentDateString()` (day precision, for example `October 7, 2026`). | `HtmlSyncService.kt`, `desktopMain/kotlin/Main.kt:144-191`, `ImportExportServiceDesktop.kt:7` |
| The web sync already does a "clean restore": `DELETE FROM toys/makers/category_settings`, then imports categories, makers, toys, then writes the sync markers. No transaction and no lock. | `HtmlSyncService.kt:196-225` |
| The web sync treats the server data as newer when the stored marker is **empty**, when the server `date` is later, or when the dates are equal and the hash differs. Markers: `html_sync_imported_date_category_settings.json`, `html_sync_imported_hash_category_settings.json`, `html_sync_imported_date_makers`, `html_sync_imported_hash_makers`, `html_sync_imported_date_{category}`, `html_sync_imported_hash_{category}`. Hash = `HtmlSyncService.calculateHash(fileText)`. A fresh install deletes all markers. Note: `HtmlSyncService.kt:182-185` also forces sync if the local `toys` table has 0 records (`!hasToys`). | `HtmlSyncService.kt:67-76, 104-113, 150-159, 182-185, 213-224`; `Main.kt:71`; `AppMainActivity.kt:52` |
| The startup web sync runs on `ioDispatcher` at app start; "Save" in the Base URL card runs it again. A restore started at the same time would share the one JDBC connection with it. | `ui/ToyDbNavigation.kt:149-167`, `ui/SettingsScreen.kt:1056-1090` |
| The UI shows a toy's **main photo by the name `{imagePrefix}{refNum}.{ext}`** (`resolveImageUri(prefix, refNum)`), not by `toys.picture`. The app names new main photos the same way (`ToyForm.kt:107`). In all current data `picture` equals that name. | `ui/SyncImage.kt:88`, `ui/ToyForm.kt:107` |
| `resolveImageUri` (desktop) also searches fallback folders (`~/valdetaro/ToyCollection/ToyDb/images`, `images`, `../images`, ...) and returns an absolute path. **Do not use it for the backup.** | `desktopMain/.../utils/ImageResolver.kt:9-40` |
| `SyncImage` downloads a photo from the server again when the DB timestamp is **newer** than the file's modified time. | `ui/SyncImage.kt:119` |
| SFTP sync compares local and remote modified times in **seconds** (and sizes). A changed modified time makes it plan "Newer Timestamp" transfers. | `desktopMain/.../service/DesktopSftpService.kt:157-170, 208-224` |
| Coil caches images by path. After a photo file is replaced with the same name, the app must clear the cache (`ToyForm` does it for one photo). | `ui/ToyForm.kt:127-135` |
| Real `app_settings` keys: `app_title`, `base_url`, `theme`, `data_path`, `images_path`, `import_export_path`, `sftp_host`, `sftp_port`, `sftp_username`, `sftp_auth_type`, `sftp_password`, `sftp_key_path`, `sftp_key_passphrase`, `sftp_remote_dir`, `sftp_approved_fingerprints`, `html_sync_imported_date_*`, `html_sync_imported_hash_*`. | user's desktop DB |
| A new database (`checkUpgrade` on an empty file) gets 5 default categories (`slot`, `train`, `static`, `kit`, `misc`) and the settings `theme = 0` and `base_url`. Tests must allow for this. | `database/Database.kt:120-209, 237-243` |
| Data directory: `ToyRepository.getDataPathSetting()` returns `data_path`; if it is missing it falls back to `images_path` / `import_export_path` and then calls `setDataPathSetting`, which writes **all three** keys. Android sets it to `filesDir/data` at start. Desktop asks for it at first start (`showSetupPrompt`), but the user can clear it later. | `ToyRepository.kt:528-549`, `AppMainActivity.kt:59-70`, `ToyDbNavigation.kt:142-146` |
| `ImportExportActions` (the HTML card) is called **twice** in `SettingsScreen` — wide layout (~line 1196) and narrow layout (~line 1414). Both calls are inside `if (!isWebPlatform())`. It is a plain `Column` (title + button), not a `Card`. | `ui/SettingsScreen.kt:1603-1661` |
| `ImportExportActions` has two hard-coded English status texts. | `SettingsScreen.kt:1624, 1631` |
| After a full data change, lists refresh only when `syncTrigger` changes. Settings only has `onCategoriesChanged`, which reloads categories. The Settings entry is `entry<Destination.Settings>` at line 489. | `ui/ToyDbNavigation.kt:139, 159, 419-429, 489-507` |
| Desktop already has a LOAD file dialog: `selectFileDialog(title, allowedExtensions)`. Use it for restore. | `desktopMain/.../utils/ImageResolver.kt` |
| `GcAppInfo.application_Context` is **not** set in this app today. | `androidMain/.../AppMainActivity.kt` |
| `AndroidManifest.xml` has only `INTERNET`. minSdk 24, targetSdk 37. | `androidMain/AndroidManifest.xml`, `../gradle/libs.versions.toml` |
| No ViewModels exist in the app. | — |
| Gradle tasks confirmed: `:composeApp:desktopTest`, `:composeApp:compileKotlinDesktop`, `:composeApp:compileDebugKotlinAndroid`, `:composeApp:compileKotlinWasmJs`, `:composeApp:assembleDebug`. (`testDesktopUnitTest` does **not** exist.) Gradle talks to its daemon through loopback sockets. If your agent runs shell commands in a sandbox that blocks them, run Gradle outside the sandbox. (Re-checked 2026-10-07: the task list ran with no special settings.) | `./gradlew :composeApp:tasks --all` |
| The SFTP guide shown in the Info tab is `composeResources/files/sftp_setup.md` (English only). There are **no** `{lang}_sftp_setup.md` files; `InfoScreen` tries them and falls back to `sftp_setup.md`. `SftpSetupScreen` reads `sftp_setup.md` directly. | `ui/InfoScreen.kt:73-88`, `ui/SftpSetupScreen.kt:32` |
| `../SCHEMA.md` does not list `app_settings` keys. | `../SCHEMA.md` |
| The Settings category dialog rejects a duplicate category id only. Two categories can have the same `image_prefix` (D14). | `ui/SettingsScreen.kt:342` |
| `exportToys` writes `value` and `amount_paid` with `formatDouble`: 2 decimals, and an empty string for 0 or less (known limitation 5). | `ImportExportService.kt:369-371, 488-496` |
| On Android the data folder (`filesDir/data`) has only the photos that `SyncImage` downloaded when a toy was shown (D15). | `ui/SyncImage.kt:157-184`, `AppMainActivity.kt:59-70` |
| The web sync checks the markers only for the categories in the server's `category_settings.json`. A category with no marker counts as newer (known limitation 7). | `HtmlSyncService.kt:121-163` |
| `HtmlSyncService.saveMetadataSetting` catches and logs every exception. | `HtmlSyncService.kt:238-244` |
| `tab_restoration` (toy form tab for restoration work) is "Restore" in en, de, es, fr and "Restauro" in pt, it (D16). | `ui/ToyForm.kt:49`, `strings.xml` ×6 |
| `java.util.zip.ZipFile` throws `ZipException` for a file that is not a zip. kotlinx `Json` does not write default values unless `encodeDefaults = true`, and it fills a missing field with its default value when it reads. | — |
| `sftp_setup.md` line 3 says "to synchronize and back up your Toy Database" (D8). | `composeResources/files/sftp_setup.md` |

---

## 5. Backup File Format

### 5.1 Layout

```
toy_collection_backup.zip
├── manifest.json                 # format id, version, counts, categoryFiles (Section 5.2)
├── photos.json                   # exact size and modified time of every photo (Section 5.5, D12)
├── data/
│   ├── category_settings.json    # ImportExportService.exportCategorySettings(db)
│   ├── carmaker.json             # ImportExportService.exportMakers(db)
│   ├── app_settings.json         # exportAppSettings(db), filtered (Section 5.3)
│   ├── carlist.json              # exportToys(db, "slot"); name from Section 5.6, listed in manifest.categoryFiles
│   ├── tralist.json              # one file for each row in category_settings
│   └── ...
└── images/
    ├── car1444.jpg               # name EXACTLY as written in the DB (picture / bitmaps token)
    └── ...
```

- Write all text entries (`manifest.json`, `photos.json`, `data/*`) **before** the photos.
- Use the existing JSON envelopes and file names (category files: Section 5.6). Do **not** add a combined `toys.json` (it would copy every toy twice and make it unclear which copy wins).
- The `data/` JSON files are the same files the web server uses, so a backup can also be used to feed the server.

### 5.2 `manifest.json` and `photos.json`

Put these declarations in `BackupRestoreService.kt` (package `com.gepetto.toydb.service`):

```kotlin
const val BACKUP_FORMAT_ID = "gepetto-toy-collection-backup"
const val BACKUP_FORMAT_VERSION = 1

/**
 * Used to write and read manifest.json and photos.json, and to read the data/ files.
 * encodeDefaults = true: every field is written. Same ignoreUnknownKeys/coerceInputValues as ImportExportService.
 */
internal val backupJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
    prettyPrint = true
}

@Serializable
data class BackupManifest(
    val format: String,              // NO default value: a manifest without "format" must fail to parse
    val formatVersion: Int,          // NO default value
    val createdAt: String,           // getCurrentDateString(), set when the backup is made
    val appVersionCode: String,      // CommonConfig.versionCodeString, set when the backup is made
    val categories: Int,
    val makers: Int,
    val toys: Int,
    val photos: Int,
    val categoryFiles: Map<String, String> = emptyMap()   // category -> entry name, e.g. "slot" -> "data/carlist.json" (D14, Section 5.6)
)

@Serializable
data class BackupPhotoInfo(val name: String, val size: Long, val modified: Long)   // modified = epoch milliseconds

@Serializable
data class BackupPhotoIndex(val photos: List<BackupPhotoInfo>)
```

> [!IMPORTANT]
> Do not give `format`, `formatVersion`, `createdAt` or `appVersionCode` a default value. kotlinx `Json` fills a missing field with its default value when it reads, so with a default the format check would pass for any JSON object. Always write and read these files with `backupJson`.

Size and modified time come from okio `systemFileSystem.metadataOrNull(path)` (`size`, `lastModifiedAtMillis`), so this stays in `commonMain`.

On restore, the file is valid only if all of these are true. Otherwise show `restore_invalid_file`:
- The file is a zip that `ZipFile` can open.
- `manifest.json` exists and parses with `backupJson`, `format == BACKUP_FORMAT_ID`, `1 <= formatVersion <= BACKUP_FORMAT_VERSION`.
- `photos.json` exists and parses.
- `data/category_settings.json` and `data/carmaker.json` exist and parse.
- Every other `data/*.json` that is present parses (Section 6.7).
- Every value in `manifest.categoryFiles` is a `data/*.json` entry that is in the archive.

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
- Restore does **not** delete `app_settings` rows, except the `html_sync_imported_*` rows that step 4.6 of Section 6.7 replaces. It only updates the portable keys that are in the backup.

### 5.4 Which Photos Go Into the Backup

`imagesDir` = `ToyRepository(db).getDataPathSetting()`. It must not be null or blank, and it must be an existing directory (`systemFileSystem.metadataOrNull(imagesDir)?.isDirectory == true`) (D11): the UI checks it first (Section 7.3); the service throws `IllegalStateException("no data directory")` if it is not.

Build one map `lowercaseName -> actual file name` from `systemFileSystem.listOrNull(imagesDir)` once, so a name that differs only in letter case is still found.

Collect names in a `LinkedHashMap<String, String>` with key `name.lowercase()` and value = the name. If the key is already there, keep the first name and ignore the new one. (Without this, on macOS a DB name `car12.jpg` and a disk name `car12.JPG` would both go into the backup, because macOS finds both.)
1. **Makers**: every space-separated token in `makers.bitmaps`.
2. **Toys, main photo**:
   - `toys.picture` when not blank.
   - Also the file the UI shows (Section 4): look up `{imagePrefix}{refNum}.{jpg,jpeg,png,gif,webp}` (in this order) in the case-insensitive map; add the first one found, with its **actual file name on disk** (the value in the map). `imagePrefix` comes from `category_settings.image_prefix`. Usually this is the same name as `picture`, and the map removes the copy.
   - Do **not** call `resolveImageUri()` (it searches other folders, Section 4).
3. **Toys, other photos**: every space-separated token in `toys.bitmaps`.

Read toys one category at a time (`SELECT ... FROM toys WHERE toy_type = ?` for each row of `category_settings`), the same as `exportToys`. Count toys whose `toy_type` is not in `category_settings` and log the count with `GcLog.w("BackupRestoreService: $count toys have unknown category")` (known limitation 6).

For each name: if `imagesDir/name` is a regular file, add entry `images/<name>`; else, if the case-insensitive map has it, add `images/<name>` with the actual file as source. If the file is missing, skip it and count it in `missingPhotos` (log with `GcLog.w("BackupRestoreService: missing photo $name")`). The entry name is the name that was collected (for steps 1, 2a and 3 this is the name **as written in the DB**), so restore finds it by exact name. Add one `BackupPhotoInfo(name = <entry name without "images/">, size, modified)` for each photo that goes in. Size and modified time come from the actual source file.

### 5.5 Archive Rules

- **Modified times (D12):** the exact time is in `photos.json`. On restore, call `File.setLastModified(photoIndex[name])`; use `entry.time` only when the name is not in the index and `entry.time > 0`. Also set `ZipEntry.time = modified` on backup (for other zip tools only). Do not use `ZipEntry.setLastModifiedTime(FileTime)` (API 26, R12). Keeping the exact time stops `SyncImage` from downloading photos again and stops SFTP sync from sending all photos again (Section 4).
- JSON entries: `Deflater.DEFAULT_COMPRESSION`. Photo entries: `Deflater.BEST_SPEED` (JPEG/PNG do not compress more).
- **Unsafe names**: on restore, accept only entries `images/<name>` where `<name>` is not empty, has no `/`, `\`, `:` (Windows drive and stream syntax, for example `C:x.jpg`) or `..`, has no control character (code < 32 or 127), and is not `.` or `..`. Skip and log every other entry outside `manifest.json`, `photos.json` and `data/`.
- Restore writes photos into `imagesDir`. A photo with the same name is replaced. Photos that are not in the backup stay.

### 5.6 Category File Names (D14)

Two categories can have the same `image_prefix` (Section 4). Each category must have its own file in the backup.

- **Backup**: for each row of `category_settings` (in `ORDER BY category` order), use the first name that no other category uses yet, from this list: `{imagePrefix}list.json`, `{category}list.json`, `{category}_2list.json`, `{category}_3list.json`, ... Put `"data/<name>"` in `manifest.categoryFiles[category]`. Usually the result is `{imagePrefix}list.json`, the same name the web server uses.
- **Restore** (Section 6.7 step 4.4): use `manifest.categoryFiles[category]` first. Only if the category is not in the map, use the fallback order `{imagePrefix}list.json`, `{category}s.json`, `{category}list.json`, `{category}.json`.

---

## 6. Code Design

### 6.1 New and Changed Files

| File | Change |
|---|---|
| `commonMain/.../database/Database.kt` | Add `fun <T> transaction(block: () -> T): T` to `ToyDatabase`. |
| `desktopMain/.../database/DesktopDatabase.kt` | Implement: save previous `autoCommit`, `autoCommit = false`, run block, `commit()`; on throw catch error, run `rollback()` in safe `try-catch`, and rethrow; `finally autoCommit = previousAutoCommit`. |
| `androidMain/.../database/AndroidDatabase.kt` | Implement: `beginTransaction()`, block, `setTransactionSuccessful()`, `finally endTransaction()`. |
| `wasmJsMain/.../database/WasmDatabase.wasmJs.kt` | Implement with `execute("BEGIN")`, run block, `execute("COMMIT")`; on throw run `execute("ROLLBACK")` in safe `try-catch`, and rethrow. |
| `commonMain/.../platform/BackupFileHelper.kt` | **New** `expect object` (6.2). Add `@file:OptIn(kotlin.experimental.ExperimentalMultiplatform::class)` for Kotlin 2.x compatibility. |
| `desktopMain/.../platform/BackupFileHelper.desktop.kt` | **New** actual (6.3). |
| `androidMain/.../platform/BackupFileHelper.android.kt` | **New** actual (6.4). |
| `wasmJsMain/.../platform/BackupFileHelper.wasmJs.kt` | **New** actual: every call returns `Failed("not supported on web")`. Never return success. |
| `commonMain/.../platform/BackupArchive.kt` | **New** `expect object` (6.5). Add `@file:OptIn(kotlin.experimental.ExperimentalMultiplatform::class)` for Kotlin 2.x compatibility. |
| `desktopMain/.../platform/BackupArchive.desktop.kt` and `androidMain/.../platform/BackupArchive.android.kt` | **New**, same JVM code in both (no shared `jvmMain` source set exists; do not add one). Use only `java.io` + `java.util.zip` + okio so the same code is valid on Android API 24 (R12). |
| `wasmJsMain/.../platform/BackupArchive.wasmJs.kt` | **New**: every function throws `UnsupportedOperationException`. |
| `commonMain/.../platform/StoragePermission.kt` + 3 actuals | **New** `@Composable expect fun rememberStoragePermissionRequest(): suspend () -> Boolean` (6.6). |
| `commonMain/.../service/BackupRestoreService.kt` | **New** service object, constants and `@Serializable` classes (5.2, 6.7). |
| `commonMain/.../service/CollectionWriteLock.kt` | **New** shared lock (6.8, D9). |
| `commonMain/.../service/HtmlSyncService.kt` | Hold `CollectionWriteLock` in `syncIfNewer` (6.8, D9) and run the clean import inside `db.transaction { }` (6.9, D17). No other change. |
| `commonMain/.../service/ImportExportService.kt` | Add an optional key filter to `importAppSettings` and `exportAppSettings`: `keyFilter: (String) -> Boolean = { true }`. No other change. |
| `commonMain/.../ui/BackupRestoreCard.kt` | **New** composable with its own state and dialogs (Section 7). Clears the Coil image cache after a restore (7.5). |
| `commonMain/.../ui/SettingsScreen.kt` | Show `BackupRestoreCard` in both layouts; new parameter `onCollectionRestored`; rename HTML card text (7.6). |
| `commonMain/.../ui/ToyDbNavigation.kt` | Pass `onCollectionRestored` (7.5). |
| `androidMain/AndroidManifest.xml` | Add permissions (D4). |
| `androidMain/.../AppMainActivity.kt` | Add `GcAppInfo.application_Context = application` at the start of `onCreate`, before `setContent`. |
| strings ×6 | Section 8 (includes `tab_restoration`, D16; no code change in `ToyForm.kt`). |
| Docs | Section 10, Phase 7. |

Package for new platform files: `com.gepetto.toydb.platform` (path `.../kotlin/com/gepetto/toydb/platform/`).

### 6.2 `BackupFileHelper` (commonMain)

```kotlin
@file:OptIn(kotlin.experimental.ExperimentalMultiplatform::class)

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
    data object NoSpace : BackupOpenResult       // Android only (D10)
    data class Failed(val message: String) : BackupOpenResult
}

expect object BackupFileHelper {
    /**
     * Gets a destination, then calls [write] with a sink to it.
     * [write] is called only after a destination is chosen (so the UI shows its progress dialog only then, Section 7.3).
     * [write] runs in the caller's coroutine. It can suspend (it waits for CollectionWriteLock, D13).
     */
    suspend fun saveBackup(suggestedName: String, dialogTitle: String, write: suspend (Sink) -> Unit): BackupSaveResult
    /** Gets a backup file that can be read with random access. */
    suspend fun openBackup(dialogTitle: String): BackupOpenResult
    fun release(path: Path)
}
```

Call both functions from `ioDispatcher`. If `write` throws `CancellationException`, clean up the same way as for an error (delete the `.partial` file or the new MediaStore entry), then rethrow it. Do not return `Failed` for it.

### 6.3 Desktop Actual

- `saveBackup`:
  1. Show `java.awt.FileDialog(null as Frame?, dialogTitle, FileDialog.SAVE)` with `file = suggestedName`. Run it with `SwingUtilities.invokeAndWait` unless already on the EDT (same pattern as `selectFileDialog`). Call `dispose()` after.
  2. No file chosen → `Cancelled`.
  3. Add `.zip` if the chosen name does not end with `.zip` (ignore case).
  4. Write to `<target>.partial` with `File.sink()` from okio (`okio.sink`). Wrap in `try { write(sink) } finally { runCatching { sink.close() } }`. (Closing `sink` in `finally` is mandatory: on Windows, an unclosed stream locks the file and prevents deletion or move). Then move it over the target (`Files.move(..., REPLACE_EXISTING, ATOMIC_MOVE)`; if `ATOMIC_MOVE` is not supported, retry without it). On any error delete the `.partial` file and return `Failed(e.message)`.
  5. Return `Saved(target.absolutePath)`.
- `openBackup`: `selectFileDialog(dialogTitle, listOf("zip", "ZIP"))`. Null → `Cancelled`. Return `Opened(path.toPath(), isTemporary = false)`. (Note: the `.zip` filter does nothing on Windows; that is acceptable.)
- `release`: no-op for non-temporary paths.
- Default suggested name (built in the UI): `toy_collection_backup.zip`.

### 6.4 Android Actual

Context: `club.gepetto.utils.GcAppInfo.application_Context as? Context`; if null return `Failed("no context")`.
Use `ANDROID_BACKUP_FILE_NAME` as the base name; ignore `suggestedName` and `dialogTitle`. Obey R12 (no `java.nio.file`).

Shared query for API 29+ (`MediaStore.Downloads.EXTERNAL_CONTENT_URI`), projection `_ID, DISPLAY_NAME, SIZE`:
- selection `"${DISPLAY_NAME} LIKE ? AND ${RELATIVE_PATH} LIKE ?"`, args `["toy_collection_backup%.zip", "Download%"]`, sort `"${DATE_MODIFIED} DESC"`. This finds `toy_collection_backup.zip` and the names Android makes when the name is taken (`toy_collection_backup (1).zip`). (`_` is a one-character wildcard in `LIKE`; that is acceptable.) Without the READ permission, the query returns only files this installation created.

- `saveBackup`:
  - **API 29+**:
    1. Run the shared query. If there is a row whose `DISPLAY_NAME` is exactly `ANDROID_BACKUP_FILE_NAME`, use it; else use the first row; else insert a new entry (`DISPLAY_NAME = ANDROID_BACKUP_FILE_NAME`, `MIME_TYPE = "application/zip"`, `RELATIVE_PATH = Environment.DIRECTORY_DOWNLOADS`, `IS_PENDING = 1`).
    2. Open `resolver.openOutputStream(uri, "wt")`, wrap with okio `.sink()`, call `write`, close. For a new entry set `IS_PENDING = 0`. On error after the insert of a new entry, delete that entry.
    3. Read back `DISPLAY_NAME` of `uri` (Android can change the name to `toy_collection_backup (1).zip`). Return `Saved("Downloads/<that name>")`.
  - **API 24–28**: `target = File(Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS), ANDROID_BACKUP_FILE_NAME)`. Write to `File(target.path + ".partial")` with okio `.sink()`, close, then `target.delete()` and `partial.renameTo(target)`; if `renameTo` returns false, throw `IOException`. On any error delete the `.partial` file and return `Failed(message)`. Return `Saved("Downloads/$ANDROID_BACKUP_FILE_NAME")`.
- `openBackup`:
  - **API 29+**: run the shared query and take the first row. No row → `NotFound`.
    - **Free space (D10):** `size` = the row's `SIZE` (if 0 or null, use `openFileDescriptor(uri, "r").statSize`). If `context.cacheDir.usableSpace < 2 * size + 50 MB`, return `NoSpace`. (The temp copy and the photos go to internal storage; 2 × is the copy plus the extracted photos.)
    - Copy `openInputStream(uri)` to `File(context.cacheDir, "restore_backup.zip")` with a streaming copy (`source().buffer().readAll(sink)`). Return `Opened(path, isTemporary = true)`. On error delete the temp file and return `Failed(message)`.
  - **API 24–28**: public Downloads file; missing → `NotFound`. No copy is made, but the photos still go to internal storage: if `context.filesDir.usableSpace < file.length() + 50 MB`, return `NoSpace` (D10). Else `Opened(path, isTemporary = false)`.
- `release`: delete the file if it is inside `context.cacheDir`.

### 6.5 `BackupArchive` (commonMain contract, JVM actuals)

```kotlin
@file:OptIn(kotlin.experimental.ExperimentalMultiplatform::class)

package com.gepetto.toydb.platform

data class BackupPhoto(val entryName: String, val source: Path, val modified: Long)   // entryName = "images/<name>", modified = epoch ms

expect object BackupArchive {
    /** Streams [textEntries] (UTF-8) then [photos] into [sink]. Sets ZipEntry.time = photo.modified. Calls [onPhoto] after each photo. Closes [sink]. */
    fun write(sink: Sink, textEntries: Map<String, String>, photos: List<BackupPhoto>, onPhoto: (done: Int, total: Int) -> Unit)
    /** Returns the text of "manifest.json", "photos.json" and every "data/<name>.json" entry (direct children of data/ only). Does not read photos. */
    fun readTextEntries(archive: Path): Map<String, String>
    /**
     * Streams every valid "images/" entry (name check, Section 5.5) into [targetDir]. Sets the modified time to [modifiedTimes][name]
     * (name without "images/"); falls back to entry.time when the name is missing and entry.time > 0.
     * [onPhoto] total = number of valid "images/" entries. Returns the count written.
     */
    fun extractPhotos(archive: Path, targetDir: Path, modifiedTimes: Map<String, Long>, onPhoto: (done: Int, total: Int) -> Unit): Int
}
```

(v2.4.0: `countPhotos` was removed. Nothing used it; `extractPhotos` counts the valid entries itself.)

JVM actual (same code in desktop and Android; R12):
- `write`: `ZipOutputStream(sink.buffer().outputStream())`. For each photo, open `FileInputStream`, copy with an 8 KB buffer. Always close `FileInputStream` in `finally` (`use {}`). Do not read a whole photo into memory. Use `ZipEntry.time`, never `setLastModifiedTime(FileTime)`.
- Read functions: `java.util.zip.ZipFile(File(archive.toString()))` (random access; the file is on disk). Read JSON with `zip.getInputStream(entry).readBytes().decodeToString()`. Close the `ZipFile` in `finally` (`use {}`). No `ZipFile` stays open after a call returns.
- `extractPhotos`:
  1. Create `targetDir` if missing.
  2. Write each photo to `File(targetDir.toFile(), "$name.partial")` using `FileOutputStream.use {}`. (Closing the output stream in `finally` before rename/delete is mandatory to prevent Windows file locking). Then `File(targetDir.toFile(), name).delete()` and `partial.renameTo(File(targetDir.toFile(), name))`, so a stopped restore never leaves a half photo with the real name. If the delete or the rename fails, wait 100 ms (`Thread.sleep`) and try again, up to 3 times (on Windows, antivirus or a preview can lock a file for a short time). If it still fails, throw `IOException`.
  3. On any error for a photo, ensure streams are closed, then delete its `<name>.partial` file before the exception leaves `extractPhotos`, so no `.partial` file stays in the data folder.
  4. Then `val ok = targetFile.setLastModified(time); if (!ok) GcLog.w("BackupArchive: Could not set mtime for $name")`. Do not throw if `setLastModified` returns false. Do not use `java.nio.file.Files.move`.

### 6.6 Storage Permission (D4)

- Manifest:
  ```xml
  <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />
  <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="28" />
  ```
- Android actual of `rememberStoragePermissionRequest()`: returns a suspend function. On API 29+ it returns `true`. On API 24–28 it returns `true` if both permissions are granted (`ContextCompat.checkSelfPermission`); else it launches `ActivityResultContracts.RequestMultiplePermissions` (from `rememberLauncherForActivityResult`) and waits for the result with a `CompletableDeferred<Boolean>`.
  > [!IMPORTANT]
  > The permission request function MUST be invoked on `Dispatchers.Main` (the UI thread) prior to switching to `ioDispatcher`, because Compose activity result launchers cannot be triggered from background dispatchers.
- Desktop and web actuals: return `{ true }`.
- If the result is `false`, show `backup_permission_denied` and stop.

### 6.7 `BackupRestoreService` (commonMain)

```kotlin
object BackupRestoreService {
    data class BackupSummary(val categories: Int, val makers: Int, val toys: Int, val photos: Int, val missingPhotos: Int = 0)
    class InvalidBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)
    class ValidatedBackup internal constructor(
        val archive: Path,
        val manifest: BackupManifest,
        internal val texts: Map<String, String>,
        internal val photoTimes: Map<String, Long>      // from photos.json
    )

    data class BackupContent(val textEntries: Map<String, String>, val photos: List<BackupPhoto>, val summary: BackupSummary)

    /** Builds all JSON texts (manifest.json, photos.json, data/*) and the photo list. Writes nothing. Does not take the lock. Easy to unit test. */
    fun collectBackupContent(db: ToyDatabase): BackupContent

    /** D13: CollectionWriteLock.mutex.withLock { withContext(ioDispatcher) { collectBackupContent(db) } }. Waits while a web sync or a restore runs. */
    suspend fun prepareBackup(db: ToyDatabase): BackupContent

    /** Streams [content] into [sink] (BackupArchive.write). Does not read the database. Returns content.summary. */
    fun writeBackup(content: BackupContent, sink: Sink, onPhoto: (Int, Int) -> Unit): BackupSummary

    /** Reads and checks the archive. Throws only InvalidBackupException. Changes nothing. Call it on ioDispatcher. */
    fun readBackup(archive: Path): ValidatedBackup

    /** Throws on error. On error the DB is unchanged (transaction rollback); photos may be partly copied. Holds CollectionWriteLock. */
    suspend fun restoreBackup(db: ToyDatabase, backup: ValidatedBackup, imagesDir: Path, onPhoto: (Int, Int) -> Unit): BackupSummary
}
```

`readBackup` must parse `manifest.json`, `photos.json` and every `data/*.json` with `backupJson` (Section 5.2) so a broken file fails **before** anything changes. The manifest counts are used in the confirm dialog. Serializer for each file:

| Entry | Serializer |
|---|---|
| `manifest.json` | `BackupManifest` |
| `photos.json` | `BackupPhotoIndex` |
| `data/category_settings.json` | `JsonCategorySettingsFile` |
| `data/carmaker.json` | `JsonMakersFile` |
| `data/app_settings.json` | `JsonAppSettingsFile` |
| every other `data/<name>.json` (direct child of `data/` only) | `JsonToysFile` |

Error mapping: put the whole body of `readBackup` in `try { ... }`. Rethrow an `InvalidBackupException` as it is. Change every other `Exception` (for example `ZipException` for a file that is not a zip, `IOException`, `SerializationException`, `IllegalArgumentException`) into `InvalidBackupException(e.message ?: e::class.simpleName ?: "invalid backup", e)`. Then the UI shows `restore_invalid_file` for all of them (Section 7.4).

`restoreBackup` order (**the order is required**, see Section 4 `SyncImage` and `importToys` facts):
1. Run everything below inside `CollectionWriteLock.mutex.withLock { ... }` (6.8) and `withContext(ioDispatcher)`.
2. `val dataPath = ToyRepository(db).getDataPathSetting()`. If `dataPath` is null or blank, or `imagesDir != dataPath.toPath()`, throw `IllegalStateException("data directory changed")`. Compare okio `Path` values, **not** strings: okio removes a trailing separator, so a stored `/x/data/` and `imagesDir` `/x/data` must be equal. Then call `ToyRepository(db).setDataPathSetting(dataPath)`, so `images_path` (which `importToys` reads) has the same value as `data_path`.
3. `BackupArchive.extractPhotos(backup.archive, imagesDir, backup.photoTimes, onPhoto)`.
4. `db.transaction { ... }`:
   1. `DELETE FROM toys`, `DELETE FROM makers`, `DELETE FROM category_settings` (in this order).
   2. `importCategorySettings(db, data/category_settings.json)`.
   3. `importMakers(db, data/carmaker.json)`.
   4. For each category in the restored `category_settings`: find the category JSON in backup texts. First use `backup.manifest.categoryFiles[category]` (D14, Section 5.6). If the category is not in the map, check in order: `"data/${imagePrefix}list.json"`, `"data/${category}s.json"`, `"data/${category}list.json"`, `"data/${category}.json"` (same fallback order as `Main.kt:163-166` and `HtmlSyncService.kt:122-127`). If found, `importToys(db, category, it)`.
   5. If `data/app_settings.json` exists: `importAppSettings(db, it, keyFilter = ::isPortableSettingKey)`.
   6. **Sync markers (D7):** `DELETE FROM app_settings WHERE key LIKE 'html_sync_imported_%'`, then write (with `INSERT OR REPLACE`):
      - `html_sync_imported_date_category_settings.json` = the `date` field of `data/category_settings.json`; `html_sync_imported_hash_category_settings.json` = `HtmlSyncService.calculateHash(<that text>)`.
      - `html_sync_imported_date_makers` / `html_sync_imported_hash_makers` from `data/carmaker.json`.
      - For each category imported in step 4.4: `html_sync_imported_date_{category}` / `html_sync_imported_hash_{category}` from the matched category JSON text.
      - Read `date` with `Json.parseToJsonElement(text).jsonObject["date"]?.jsonPrimitive?.content ?: ""` (the same way `HtmlSyncService` does). Result: the startup sync replaces the restored collection only when the server `date` is later than the backup date (or the same day with a different hash). Exceptions: a restored collection with 0 toys (`HtmlSyncService.kt:182-185` forces a sync when the `toys` table is empty), and a server category that is not in the backup (known limitation 7).
5. Count rows after the commit and return the summary.

Do not run steps 2–5 on the main thread.

### 6.8 `CollectionWriteLock` (D9)

New file `commonMain/.../service/CollectionWriteLock.kt`:

```kotlin
package com.gepetto.toydb.service

import kotlinx.coroutines.sync.Mutex

/**
 * Only one full collection replacement (restore or web sync) runs at a time, and Back Up does not read the
 * collection while one runs. Not re-entrant: never call one from inside another.
 */
object CollectionWriteLock {
    val mutex = Mutex()
}
```

- `BackupRestoreService.restoreBackup`: holds the lock for the whole restore (6.7 step 1).
- `BackupRestoreService.prepareBackup` (D13): holds the lock only while it reads the database and builds the JSON texts and the photo list. The photo streaming (`writeBackup`) runs after the lock is released.
- `HtmlSyncService.syncIfNewer`: wrap the **whole** body inside `withContext(ioDispatcher) { ... }` (today lines 49–234, the `try`/`catch`) in `CollectionWriteLock.mutex.withLock { ... }`. The lock must cover the marker comparison **and** the clean import; if it covered only the import, a sync that waited for a restore would still delete the restored data with its old decision. `withLock` is `inline`, so the existing `return@withContext` statements still work.
- Effect: a restore started during the startup sync waits until the sync ends (the progress dialog shows). A sync started during a restore waits, then sees the new markers (6.7 step 4.6) and skips. A Back Up started during the startup sync waits until the sync ends, so it never saves a half-replaced collection (D13).

### 6.9 Web Sync Transaction (D17)

In `HtmlSyncService.syncIfNewer`, step 4 (today lines 196–225) deletes all data, imports again, then writes the markers, with no transaction. If an import fails (for example, a broken list file on the server), the collection stays empty or half full.

- Wrap the lines from `db.execute("DELETE FROM toys")` to the last `saveMetadataSetting(...)` (today lines 197–225) in `db.transaction { ... }`. This is inside the `CollectionWriteLock` wrap from 6.8.
- Keep `GcLog.i(TAG, "HTML Startup Sync completed successfully.")` and `return@withContext true` **after** the transaction block. `ToyDatabase.transaction` is an interface function, so it is not `inline`, and `return@withContext` is not allowed inside its block.
- Do not change the network part, the marker comparison, or the `catch` block. An import error now rolls back, and the existing `catch` logs and rethrows it as today, so the Settings "Save" button shows its error dialog, and the collection is unchanged.
- `saveMetadataSetting` catches its own exceptions (Section 4). Keep that: a failed marker write does not roll back the import (same result as today).
- All three platforms run this code, web included. The web `transaction` (`BEGIN`/`COMMIT`/`ROLLBACK`, 6.1) must work for the first web start, when the sync fills the empty collection.

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

The second box only marks the place of the existing website pages controls. `ImportExportActions` (renamed `WebsitePagesActions`, 7.6) is a plain `Column` (title + button), not a `Card`. Do not change its layout.

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
private enum class BackupPhase { Idle, Saving, Saved, SaveFailed, Opening, Confirm, Restoring, Restored, RestoreFailed, NotFound, Invalid, NoDataDir, NoSpace, PermissionDenied }
```
Plus: `isWorking: Boolean` (true from the button click to the end of the flow; it disables both buttons, also while a native file dialog is open and no progress dialog shows), `progressDone` and `progressTotal` (`mutableIntStateOf`), `resultMessage: String`, `missingPhotos: Int`, `pendingBackup: ValidatedBackup?`, `pendingOpen: BackupOpenResult.Opened?`.

**Progress values:** the `onPhoto` callback runs once for each photo (about 2,900 times) on the IO thread. Write `progressDone` and `progressTotal` directly in the callback (Compose snapshot state can be written from any thread). Do not launch a main-thread coroutine for each photo. The same applies to setting `phase` from the IO thread.

**Status banner (`onSetStatus`):** Back Up success → `backup_done_title`; Back Up failure → `backup_failed(message)`; Restore success → `restore_done_title`; Restore failure → `restore_failed(message)`. Do not change the banner for Cancelled.

### 7.3 Back Up Flow

0. Desktop only (D11): if `dataPath` is null or blank, **or** `systemFileSystem.metadataOrNull(dataPath.toPath())?.isDirectory != true` → `NoDataDir` dialog (`backup_no_data_dir`). Do not continue.
1. Request storage permission on the UI thread (`Dispatchers.Main`, 6.6). Denied → `PermissionDenied`.
2. `isWorking = true`. Do **not** show a progress dialog yet: on desktop the native Save dialog opens first, and a progress dialog behind it would flash when the user cancels. On `ioDispatcher`:
   ```kotlin
   val result = BackupFileHelper.saveBackup("toy_collection_backup.zip", getString(Res.string.backup_save_dialog_title)) { sink ->
       phase = BackupPhase.Saving                                  // destination chosen: show the progress dialog now
       val content = BackupRestoreService.prepareBackup(db)        // waits while the startup web sync runs (D13)
       summary = BackupRestoreService.writeBackup(content, sink) { d, t -> progressDone = d; progressTotal = t }
   }
   ```
   The `Saving` progress dialog is not dismissible. It shows `backup_saving`, and `backup_progress` when `progressTotal > 0`.
3. `Saved` → dialog `backup_done_title` / `backup_done_msg(toys, photos, location)`. Use the `location` from `Saved` (on Android it can be `toy_collection_backup (1).zip`, Section 6.4). If `summary.missingPhotos > 0`, show `backup_missing_photos(missingPhotos)` as a second paragraph in the same dialog (D15). `Cancelled` → back to `Idle` with no dialog. `Failed` or exception → `backup_failed`. Rethrow `CancellationException`.
4. Set the status banner text through `onSetStatus` (7.2). In a `finally`, set `isWorking = false`.

### 7.4 Restore Flow

1. Desktop only: if `dataPath` is null or blank → `NoDataDir` dialog (`backup_no_data_dir`). (A missing folder is allowed here: `extractPhotos` creates it.)
2. Request storage permission on the UI thread (`Dispatchers.Main`, 6.6). Denied → `PermissionDenied`.
3. `isWorking = true`. On Android only, set phase `Opening` (progress dialog, not dismissible, text `restore_running`) now, because `openBackup` copies the file to `cacheDir` and that can take a long time. On desktop, keep no dialog while the native Open dialog shows. Then `BackupFileHelper.openBackup(getString(restore_open_dialog_title))` on `ioDispatcher`:
   - `Cancelled` → `Idle`. `NotFound` → `NotFound` dialog (`restore_missing_title` / `restore_missing_msg`). `NoSpace` → `NoSpace` dialog (`restore_no_space`, D10). `Failed` → `RestoreFailed`.
4. `Opened` → phase `Opening` (all platforms). `BackupRestoreService.readBackup(path)` on `ioDispatcher`. `InvalidBackupException` → `Invalid` dialog (`restore_invalid_file`). (`readBackup` throws no other exception, 6.7.)
5. `Confirm` dialog: title `restore_confirm_title`, text `restore_confirm_msg(toys, makers, photos)` from the manifest. Buttons: `OutlinedButton` `cancel` (existing key), `Button` with `containerColor = MaterialTheme.colorScheme.error` and text `restore_confirm_btn`.
6. Phase `Restoring` (progress dialog, not dismissible; show `restore_running`, and `backup_progress` while photos are copied). In a coroutine (`rememberCoroutineScope().launch`), call the `suspend` function `restoreBackup(db, backup, dataPath.toPath(), ...)`. It moves to `ioDispatcher` and holds `CollectionWriteLock` itself (6.7, 6.8). If the startup web sync runs, the restore waits and the progress dialog stays on the screen. Do not add a time-out.
7. Success → on the main thread: clear the Coil caches (7.5), call `onCollectionRestored()`, then show the `Restored` dialog (`restore_done_title` / `restore_done_msg`). Error → `RestoreFailed` with `restore_failed(message)`.
8. In a `finally`, if the opened file was temporary, call `BackupFileHelper.release(path)`, and set `isWorking = false`. Also release it (and set `isWorking = false`) when the user cancels the confirm dialog. Set the status banner text through `onSetStatus` (7.2).

### 7.5 Refresh After Restore

- **Coil caches:** a restored photo can have the same file name as an old photo, so Coil can show the old image from its cache (`ToyForm.kt:127-135`). In `BackupRestoreCard`, read the context in the composable body (`val platformContext = coil3.compose.LocalPlatformContext.current`). After a successful restore, and before `onCollectionRestored()`, call:
  ```kotlin
  val loader = coil3.SingletonImageLoader.get(platformContext)
  loader.memoryCache?.clear()
  loader.diskCache?.clear()
  ```
  Do this on all platforms where the card shows (desktop and Android).
- `SettingsScreen`: new parameter `onCollectionRestored: () -> Unit = {}`. Inside the `BackupRestoreCard` callback, first reload local state: `categoriesList = repository.getCategorySettings()`, `appTitle = repository.getAppTitleSetting()`, `htmlBaseUrl = repository.getBaseUrlSetting()`, `dataPath = repository.getDataPathSetting()`; then call `onCollectionRestored()`.
- `ToyDbNavigation` (`entry<Destination.Settings>`, line 489): pass
  ```kotlin
  onCollectionRestored = {
      categoriesSettings = repository.getCategorySettings()
      themeMode = repository.getThemeSetting()
      appTitle = repository.getAppTitleSetting()
      com.gepetto.toydb.utils.ImageResolverConfig.imagesPath = repository.getDataPathSetting()
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

All new dialogs: `AlertDialog` with `containerColor = sysBackgroundColor()`, text color `sysTextColor()`, dark-mode border (Rule R6). Progress dialogs: `onDismissRequest = {}`, no buttons, `CircularProgressIndicator` plus `LinearProgressIndicator(progress = { done / total })` and `backup_progress` text when `total > 0` (progress values: 7.2). Result dialogs: one `Button` with the existing `ok` key. Put the dialogs in their own composable (for example `BackupRestoreDialogs(phase, ...)`) so they can have previews (7.9).

### 7.8 Info Tab "Server Sync" & Text Alignment (D8, ISSUE-01)

- `ui/InfoScreen.kt`: the tab label comes from `InfoTopic.BACKUP.titleRes = Res.string.info_tab_backup`. Change the **value** of `info_tab_backup` to "Server Sync" in all 6 files (Section 8.2).
- Do not rename the enum constant `InfoTopic.BACKUP`, its id `"backup"`, or the composable `BackupTabContent`.
- **ISSUE-01 Approved:** Change heading `backup_sync_title` to "Server Synchronization" and `backup_sync_description` to refer to server synchronization in all 6 files (Section 8.2). Change `composeResources/files/sftp_setup.md` line 11 bullet from "**Automatic Backup**" to "**Automatic Sync**".
- *(v2.4.0, D8)* Change `sftp_setup.md` line 3 from "to synchronize and back up your Toy Database across multiple devices" to "to synchronize your Toy Database across multiple devices".

### 7.9 Previews

`BackupRestoreCard` previews: Light/Dark and Landscape, in `GcTheme {}`. To preview without a DB, split the visual part into `BackupRestoreCardContent(isWorking: Boolean, onBackUp: () -> Unit, onRestore: () -> Unit)` and preview that. Also preview the dialogs composable (7.7) at least for `Confirm`, `Saving` (with progress) and `Saved` with a missing-photos line.

### 7.10 Toy Form Tab "Restoration" (D16)

- The toy form (`ui/ToyForm.kt:49`) shows a tab for restoration work on a toy, with the label `tab_restoration`. Today the value is "Restore" in en, de, es and fr. Next to "Restore Collection" this is the same problem D8 fixed for "Backup".
- Change the **value** (not the key) of `tab_restoration` in 4 files (Section 8.2): en "Restoration", de "Restaurierung", es "Restauración", fr "Restauration". pt and it stay "Restauro". No code change.

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
| `backup_missing_photos` | %1$d photos were not found on this device. They are not in the backup. | %1$d fotos não foram encontradas neste dispositivo. Elas não estão no backup. | %1$d Fotos wurden auf diesem Gerät nicht gefunden. Sie sind nicht in der Sicherung. | No se encontraron %1$d fotos en este dispositivo. No están en la copia de seguridad. | %1$d photos sont introuvables sur cet appareil. Elles ne sont pas dans la sauvegarde. | %1$d foto non sono state trovate su questo dispositivo. Non sono nel backup. |
| `restore_confirm_title` | Restore Your Collection? | Restaurar sua coleção? | Sammlung wiederherstellen? | ¿Restaurar su colección? | Restaurer votre collection ? | Ripristinare la collezione? |
| `restore_confirm_msg` | This replaces all toys, makers and categories on this device with the ones in the backup (%1$d toys, %2$d makers, %3$d photos). Photos with the same name are replaced. You cannot undo this. | Isto substitui todos os brinquedos, fabricantes e categorias deste dispositivo pelos do backup (%1$d brinquedos, %2$d fabricantes, %3$d fotos). Fotos com o mesmo nome são substituídas. Não é possível desfazer. | Alle Spielzeuge, Hersteller und Kategorien auf diesem Gerät werden durch die aus der Sicherung ersetzt (%1$d Spielzeuge, %2$d Hersteller, %3$d Fotos). Fotos mit gleichem Namen werden ersetzt. Dies kann nicht rückgängig gemacht werden. | Esto reemplaza todos los juguetes, fabricantes y categorías de este dispositivo por los de la copia (%1$d juguetes, %2$d fabricantes, %3$d fotos). Las fotos con el mismo nombre se reemplazan. No se puede deshacer. | Tous les jouets, fabricants et catégories de cet appareil seront remplacés par ceux de la sauvegarde (%1$d jouets, %2$d fabricants, %3$d photos). Les photos portant le même nom seront remplacées. Cette action est irréversible. | Tutti i giocattoli, i produttori e le categorie su questo dispositivo verranno sostituiti con quelli del backup (%1$d giocattoli, %2$d produttori, %3$d foto). Le foto con lo stesso nome verranno sostituite. L\'operazione non può essere annullata. |
| `restore_confirm_btn` | Restore | Restaurar | Wiederherstellen | Restaurar | Restaurer | Ripristina |
| `restore_running` | Restoring your collection... | Restaurando sua coleção... | Sammlung wird wiederhergestellt... | Restaurando su colección... | Restauration de votre collection... | Ripristino della collezione... |
| `restore_done_title` | Restore Complete | Restauração Concluída | Wiederherstellung abgeschlossen | Restauración completada | Restauration terminée | Ripristino completato |
| `restore_done_msg` | Your collection was restored: %1$d toys, %2$d makers, %3$d photos. | Sua coleção foi restaurada: %1$d brinquedos, %2$d fabricantes, %3$d fotos. | Ihre Sammlung wurde wiederhergestellt: %1$d Spielzeuge, %2$d Hersteller, %3$d Fotos. | Su colección se restauró: %1$d juguetes, %2$d fabricantes, %3$d fotos. | Votre collection a été restaurée : %1$d jouets, %2$d fabricants, %3$d photos. | La collezione è stata ripristinata: %1$d giocattoli, %2$d produttori, %3$d foto. |
| `restore_failed` | The collection could not be restored: %1$s. Your toys, makers and categories were not changed. | Não foi possível restaurar a coleção: %1$s. Seus brinquedos, fabricantes e categorias não foram alterados. | Die Sammlung konnte nicht wiederhergestellt werden: %1$s. Ihre Spielzeuge, Hersteller und Kategorien wurden nicht geändert. | No se pudo restaurar la colección: %1$s. Sus juguetes, fabricantes y categorías no se modificaron. | La collection n\'a pas pu être restaurée : %1$s. Vos jouets, fabricants et catégories n\'ont pas été modifiés. | Impossibile ripristinare la collezione: %1$s. Giocattoli, produttori e categorie non sono stati modificati. |
| `restore_invalid_file` | This file is not a backup of a toy collection. | Este arquivo não é um backup de uma coleção de brinquedos. | Diese Datei ist keine Sicherung einer Spielzeugsammlung. | Este archivo no es una copia de seguridad de una colección de juguetes. | Ce fichier n\'est pas une sauvegarde d\'une collection de jouets. | Questo file non è un backup di una collezione di giocattoli. |
| `restore_missing_title` | Backup File Not Found | Arquivo de Backup Não Encontrado | Sicherungsdatei nicht gefunden | Archivo de copia no encontrado | Fichier de sauvegarde introuvable | File di backup non trovato |
| `restore_missing_msg` | The file \'toy_collection_backup.zip\' is not in the Downloads folder. Put the backup file in Downloads, then try again. | O arquivo \'toy_collection_backup.zip\' não está na pasta Downloads. Coloque o arquivo de backup em Downloads e tente novamente. | Die Datei \'toy_collection_backup.zip\' ist nicht im Ordner „Downloads“. Legen Sie die Sicherungsdatei in „Downloads“ ab und versuchen Sie es erneut. | El archivo \'toy_collection_backup.zip\' no está en la carpeta Descargas. Coloque el archivo de copia en Descargas e inténtelo de nuevo. | Le fichier \'toy_collection_backup.zip\' ne se trouve pas dans le dossier Téléchargements. Placez le fichier de sauvegarde dans Téléchargements, puis réessayez. | Il file \'toy_collection_backup.zip\' non è nella cartella Download. Inserisci il file di backup in Download e riprova. |
| `backup_no_data_dir` | Select a data directory first. Your photos are kept there. | Selecione primeiro um diretório de dados. Suas fotos ficam nele. | Wählen Sie zuerst ein Datenverzeichnis. Dort werden Ihre Fotos gespeichert. | Seleccione primero un directorio de datos. Allí se guardan sus fotos. | Sélectionnez d\'abord un dossier de données. Vos photos y sont conservées. | Seleziona prima una cartella dati. Le foto sono conservate lì. |
| `restore_no_space` | There is not enough free space on this device to restore the backup. Delete some files, then try again. | Não há espaço livre suficiente neste dispositivo para restaurar o backup. Apague alguns arquivos e tente novamente. | Auf diesem Gerät ist nicht genug freier Speicher, um die Sicherung wiederherzustellen. Löschen Sie einige Dateien und versuchen Sie es erneut. | No hay suficiente espacio libre en este dispositivo para restaurar la copia de seguridad. Elimine algunos archivos e inténtelo de nuevo. | Il n\'y a pas assez d\'espace libre sur cet appareil pour restaurer la sauvegarde. Supprimez quelques fichiers, puis réessayez. | Spazio libero insufficiente su questo dispositivo per ripristinare il backup. Elimina alcuni file e riprova. |
| `backup_permission_denied` | The app needs permission to use the Downloads folder. | O app precisa de permissão para usar a pasta Downloads. | Die App benötigt die Berechtigung, den Ordner „Downloads“ zu verwenden. | La aplicación necesita permiso para usar la carpeta Descargas. | L\'application a besoin d\'une autorisation pour utiliser le dossier Téléchargements. | L\'app ha bisogno dell\'autorizzazione per usare la cartella Download. |
| `backup_save_dialog_title` | Save Backup | Salvar Backup | Sicherung speichern | Guardar copia de seguridad | Enregistrer la sauvegarde | Salva backup |
| `restore_open_dialog_title` | Choose a Backup File | Escolha um Arquivo de Backup | Sicherungsdatei auswählen | Elija un archivo de copia | Choisir un fichier de sauvegarde | Scegli un file di backup |
| `website_status_creating` | Creating website pages in %1$s... | Criando páginas do site em %1$s... | Webseiten werden in %1$s erstellt... | Creando páginas web en %1$s... | Création des pages du site dans %1$s... | Creazione delle pagine del sito in %1$s... |
| `website_status_done` | Created %1$d website pages in %2$s. | %1$d páginas do site criadas em %2$s. | %1$d Webseiten in %2$s erstellt. | Se crearon %1$d páginas web en %2$s. | %1$d pages du site créées dans %2$s. | %1$d pagine del sito create in %2$s. |

Reuse existing keys `ok` and `cancel`. Do not add keys for "Import"/"Export".

### 8.2 Changed Values of Existing Keys (D2, D8, D16)

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
| `backup_sync_title` | Server Synchronization | Sincronização com Servidor | Server-Synchronisierung | Sincronización con Servidor | Synchronisation avec le Serveur | Sincronizzazione con il Server |
| `backup_sync_description` | If you want to synchronize your collection data safely with your private server, or synchronize your data across several devices (such as your phone, tablet, and computer), you will need to set up a private SFTP server. | Se você deseja sincronizar os dados da sua coleção com segurança com seu servidor privado ou sincronizar seus dados em vários dispositivos (como telefone, tablet e computador), precisará configurar um servidor SFTP privado. | Wenn Sie Ihre Sammlungsdaten sicher mit Ihrem privaten Server oder über mehrere Geräte (wie Telefon, Tablet und Computer) synchronisieren möchten, müssen Sie einen privaten SFTP-Server einrichten. | Si desea sincronizar los datos de su colección de forma segura con su servidor privado, o sincronizar sus datos en varios dispositivos (como su teléfono, tableta y computadora), deberá configurar un servidor SFTP privado. | Si vous souhaitez synchroniser les données de votre collection en toute sécurité avec votre serveur privé, ou synchroniser vos données sur plusieurs appareils (tels que votre téléphone, votre tablette et votre ordinateur), vous devrez configurer un serveur SFTP privé. | Se desideri sincronizzare i dati della tua collezione in modo sicuro con il tuo server privato o sincronizzare i tuoi dati su più dispositivi (come telefono, tablet e computer), dovrai configurare un server SFTP privato. |
| `tab_restoration` | Restoration | Restauro *(no change)* | Restaurierung | Restauración | Restauration | Restauro *(no change)* |
| `error_html_no_dir` | Select a data directory first. The website pages are created there. | Selecione primeiro um diretório de dados. As páginas do site são criadas nele. | Wählen Sie zuerst ein Datenverzeichnis. Dort werden die Webseiten erstellt. | Seleccione primero un directorio de datos. Allí se crean las páginas web. | Sélectionnez d\'abord un dossier de données. Les pages du site y sont créées. | Seleziona prima una cartella dati. Le pagine del sito vengono create lì. |

---

## 9. Verification

### 9.1 Unit Tests

File: `../composeApp/src/desktopTest/kotlin/com/gepetto/toydb/service/BackupRestoreServiceTest.kt` (`kotlin.test`, class PascalCase, methods `testXxx`).
Use `DesktopToyDatabase` on a DB file inside `java.nio.file.Files.createTempDirectory(...)`, and a photos folder in the same temp directory. Call `ToyRepository(db).setDataPathSetting(photosDir.toString())` in the setup.

General rules for all tests:
- A new DB already has 5 default categories and the settings `theme` and `base_url` (Section 4). Delete the default categories (`DELETE FROM category_settings`) before you seed the test data, or include them in the expected values.
- `restoreBackup` and `prepareBackup` are `suspend`: call them in `runBlocking { ... }`. To make a backup file: `val content = runBlocking { prepareBackup(db) }`, then `writeBackup(content, zipFile.sink(), { _, _ -> })`.
- `BackupArchive` opens and closes its own `ZipFile` in each call, so no `ZipFile` stays open. In `finally`: close the DB and any stream the test opened itself, then delete the temp directory. (On Windows, an open file cannot be deleted.)
- Seed `value` and `amount_paid` with 2 decimals at most (for example `12.50`). The JSON files round to 2 decimals (known limitation 5).

1. `testBackupAndRestoreRoundTrip` — seed 2 categories, 3 makers (one with `bitmaps`), 5 toys (with `picture` and `bitmaps`), 4 photo files. Give each photo a modified time with milliseconds that are not 0 (for example `1_700_000_123_457`). Back up to a temp zip, clear the DB and photo folder, restore. Every column must match the values `importToys`/`importMakers` produce (they recompute `maker_combo` and read photo metadata), photo bytes must match, and each photo `lastModified()` must be **exactly** equal to the original value (from `photos.json`, D12).
2. `testSecretsAreNotBackedUp` — seed `sftp_password`, `sftp_host`, `images_path`, `data_path`, `import_export_path`, `html_sync_imported_hash_slot`, `app_title`. In `data/app_settings.json`: none of the `sftp_*`, path or `html_sync_imported_*` keys are present; `app_title`, `theme` and `base_url` are present.
3. `testRestoreDoesNotOverwriteLocalOnlySettings` — a backup zip whose `app_settings.json` contains `images_path`, `data_path` and `sftp_password` with other values. After restore: `sftp_password` keeps the local value, and `images_path` and `data_path` both equal the local `imagesDir` (6.7 step 2).
4. `testInvalidBackupChangesNothing` — a zip without `manifest.json`, a zip with a broken `carmaker.json`, and a text file named `backup.zip` that is not a zip: each time `readBackup` throws `InvalidBackupException` (no other exception type), and the DB and photo folder are unchanged.
5. `testPhotosJsonRequired` — a zip with a valid `manifest.json` and `data/*` but no `photos.json`: `readBackup` throws `InvalidBackupException`.
6. `testUnsafeEntryNamesAreSkipped` — entries `images/../evil.jpg`, `images/a/b.jpg`, `../x.jpg`, `images/C:evil.jpg` and `images/a\u0001.jpg` are not written anywhere.
7. `testFailedRestoreRollsBack` — wrap the test DB in a `ToyDatabase` that delegates every call, **including `transaction`**, to the real DB, but throws on the first `INSERT OR REPLACE INTO toys`. Run `restoreBackup` and check that toys, makers, categories and the `html_sync_imported_*` rows are unchanged.
8. `testMissingPhotoIsSkipped` — a toy refers to a missing file: backup succeeds and `missingPhotos == 1`.
9. `testRestoreSetsSyncMarkers` — before restore, write old values in `html_sync_imported_date_slot`, `html_sync_imported_hash_slot` and `html_sync_imported_date_obsolete`. After restore: for `category_settings.json`, `makers` and each restored category, `html_sync_imported_date_*` equals the `date` field of the backup JSON file and `html_sync_imported_hash_*` equals `HtmlSyncService.calculateHash(<file text>)`. `html_sync_imported_date_obsolete` does not exist.
10. *(Optional)* `testRestoreWaitsForLock` — hold `CollectionWriteLock.mutex` in the test, start `restoreBackup` in another coroutine, check that the DB is not changed yet, release the lock, then check that the restore completes.
11. `testManifestWithoutFormatIsRejected` — (a) the `manifest.json` text from `collectBackupContent` contains `"format"` and `"formatVersion"` (written by `backupJson`, `encodeDefaults = true`); (b) a zip whose `manifest.json` is `{"categories":0,"makers":0,"toys":0,"photos":0}` (no `format`): `readBackup` throws `InvalidBackupException` (Section 5.2).
12. `testCategoriesWithSamePrefix` — seed two categories with the same `image_prefix` (`slot`/`car` and `racing`/`car`), with different toys in each. Back up: `manifest.categoryFiles` has 2 different entry names, and both entries are in the zip. Restore: each category has exactly its own toys (D14, Section 5.6).
13. `testCaseOnlyDuplicatePhotoStoredOnce` — a toy with `picture = "car12.jpg"` and, on disk, only `car12.JPG`. Back up: the zip has exactly one `images/` entry whose name is `car12.jpg` when compared without letter case, and `missingPhotos == 0` (Section 5.4).
14. `testRestorePathCheckIgnoresTrailingSlash` — store `data_path` as `photosDir.toString() + "/"`, call `restoreBackup(db, backup, photosDir.toString().toPath(), ...)`: no exception (6.7 step 2).
15. `testBackupWithoutDataFolderFails` — `data_path` points to a folder that does not exist: `collectBackupContent` throws `IllegalStateException` (D11, Section 5.4).
16. *(Optional)* `testBackupWaitsForLock` — hold `CollectionWriteLock.mutex`, start `prepareBackup` in another coroutine, check that it has not completed, release the lock, then check that it completes (D13).

Run existing tests too (`JsonDateParserParityTest`, `JsonDateParserTest`, `HtmlSyncServiceHashTest`).

### 9.2 Build Commands

> [!NOTE]
> Gradle talks to its daemon through loopback sockets. If your agent runs shell commands in a sandbox that blocks loopback sockets, run these commands outside the sandbox (use the option your agent tool has for this).

```bash
./gradlew :composeApp:desktopTest
./gradlew :composeApp:compileKotlinDesktop
./gradlew :composeApp:compileDebugKotlinAndroid
./gradlew :composeApp:compileKotlinWasmJs
./gradlew :composeApp:assembleDebug   # also checks the merged AndroidManifest (permissions, 6.6)
```

### 9.3 Manual Checks

- **Desktop**: back up with the real data directory (~1.5 GB). Check memory stays normal and the progress moves. Restore on a copy of the app data, check toys, makers, categories, photos, app title and theme refresh without restart. Cancel both dialogs: nothing happens, and no progress dialog shows behind the native Save or Open dialog (7.3, 7.4). No data directory: **Back Up** and **Restore** both show `backup_no_data_dir`. Data directory set but its folder deleted: **Back Up** shows `backup_no_data_dir` (D11).
- **Desktop, backup during startup sync**: start the app with a server `base_url` whose data is newer, and click **Back Up Collection** at once. The backup waits until the sync ends, and the backup has all toys (D13).
- **Desktop, web sync failure**: serve a copy of the server files with `python3 -m http.server` from a temp folder. Replace one `*list.json` file with `{"date": "December 31, 2099"}` (valid JSON, so the date check passes and marks it newer, but it has no `cars` field, so `importToys` fails after the delete). Do not use broken JSON: `HtmlSyncService` parses each file for its `date` before the delete, so broken JSON fails too early to test the rollback. Set that URL as Base URL and click Save. The error dialog shows, and the collection is unchanged (D17).
- **Desktop, photos refresh**: replace one photo file with a different image of the same name in a backup, restore, and open that toy: the new image shows (no old image from the cache, 7.5).
- **Desktop, SFTP after restore**: restore, then open the SFTP sync plan. Photos must **not** all show as "Newer Timestamp" (D12).
- **Desktop or Android, restore during startup sync**: start the app with a server `base_url` and start a restore while the startup sync runs. The progress dialog stays until the sync ends; then the restore completes and the restored data is still there after the next start (D7, D9).
- **Android emulator, API 34+**: back up → file in Downloads; restore; delete the file → "Backup File Not Found".
- **Android emulator, API 34+, name change**: copy a `toy_collection_backup.zip` into Downloads with `adb push` (so this installation did not create it), then back up. The "Backup Complete" dialog shows `toy_collection_backup (1).zip`, and restore finds it.
- **Android emulator, low space**: fill the emulator storage until the free space is less than 2 × the backup size, then restore → `restore_no_space`.
- **Android emulator, API 28**: permission prompt appears; deny → `backup_permission_denied`; allow → back up and restore work. Low space on API 28 → `restore_no_space` (D10).
- **Android, missing photos**: on a new install, open only a few toys (so only their photos are downloaded), then back up. The "Backup Complete" dialog shows the `backup_missing_photos` line (D15).
- **Toy form**: the restoration tab shows "Restoration" (en), "Restaurierung" (de), "Restauración" (es), "Restauration" (fr) (D16).
- **Web**: the card is not shown; the app still builds and runs. On the first start (empty browser storage), the startup web sync still fills the collection (D17, web `transaction`).
- **Dark and light mode**: all new dialogs readable, border in dark mode.
- **All 6 languages**: no missing strings (switch the device language at least once for de and pt).

---

## 10. Execution Checklist

- [x] **Phase 1 — Strings**
  - [x] Add Section 8.1 keys to all 6 `strings.xml` files (includes `restore_no_space` and the reworded `restore_invalid_file` / `backup_permission_denied`, R1).
  - [x] Change Section 8.2 values in all 6 files (includes `info_tab_backup`, `backup_sync_title`, `backup_sync_description`, ISSUE-01).
  - [x] *(v2.4.0)* Add `backup_missing_photos` (D15) to all 6 files; change `tab_restoration` in en, de, es, fr (D16, 7.10).
- [x] **Phase 2 — Database transaction**
  - [x] Add `transaction` to `ToyDatabase` and implement it for Desktop, Android, Wasm (safe rollback in `try-catch`; preserve prior `autoCommit` on Desktop).
- [x] **Phase 3 — Platform layer**
  - [x] `GcAppInfo.application_Context = application` in `AppMainActivity.onCreate`.
  - [x] Manifest permissions (6.6).
  - [x] `BackupFileHelper` common + desktop + android + wasm (Android: shared MediaStore query, "(1)" name, `NoSpace` check, 6.4; `@file:OptIn(kotlin.experimental.ExperimentalMultiplatform::class)` for Kotlin 2.x).
  - [x] *(v2.4.0)* `saveBackup` takes `write: suspend (Sink) -> Unit` and calls it only after a destination is chosen; clean up and rethrow on `CancellationException` (6.2). Android API 24–28 free-space check for the photos (6.4, D10).
  - [x] `BackupArchive` common + desktop + android + wasm (`photos.json` times, `File(archive.toString())`, non-fatal `setLastModified`, 6.5; `@file:OptIn(kotlin.experimental.ExperimentalMultiplatform::class)` for Kotlin 2.x; stream closure in `finally` before delete/move on Windows).
  - [x] *(v2.4.0)* `BackupArchive`: no `countPhotos`; name check rejects `:` and control characters (5.5); `extractPhotos` deletes its `.partial` file on error and retries delete/rename 3 times (6.5).
  - [x] `rememberStoragePermissionRequest` common + desktop + android + wasm (invoked on `Dispatchers.Main`).
  - [x] Android actuals obey R12: no `java.nio.file.*` and no `FileTime`.
  - [x] Build all 3 targets.
- [x] **Phase 4 — Service**
  - [x] `keyFilter` parameter in `importAppSettings` / `exportAppSettings`.
  - [x] `BackupManifest`, `BackupPhotoIndex`, `isPortableSettingKey`, `BackupRestoreService` (6.7, 5.x), including `photos.json`, category JSON fallback resolution (4.4), and sync markers (6.7 step 4.6).
  - [x] *(v2.4.0)* `backupJson`; no default values for `format`, `formatVersion`, `createdAt`, `appVersionCode` (5.2).
  - [x] *(v2.4.0)* `manifest.categoryFiles` with unique category file names, used first on restore (5.6, D14).
  - [x] *(v2.4.0)* Photo list: data folder must exist, case-insensitive dedupe, actual disk name for rule 2b, log toys without a category (5.4).
  - [x] *(v2.4.0)* `readBackup`: serializer table, direct children of `data/` only, every error → `InvalidBackupException` (6.7).
  - [x] *(v2.4.0)* `prepareBackup` holds `CollectionWriteLock` (D13); `writeBackup(content, sink, onPhoto)` (6.7).
  - [x] *(v2.4.0)* Restore step 2 compares okio `Path` values, not strings (6.7).
  - [x] `CollectionWriteLock` and the `withLock` wrap in `HtmlSyncService.syncIfNewer` (6.8).
  - [x] *(v2.4.0)* `db.transaction { }` around the clean import in `HtmlSyncService.syncIfNewer`; `return@withContext true` stays outside it (6.9, D17).
- [x] **Phase 5 — UI**
  - [x] `BackupRestoreCard.kt` with dialogs and previews (`NoDataDir` for Back Up and Restore, `NoSpace`).
  - [x] *(v2.4.0)* `isWorking`; `Saving` dialog only after the Save dialog returns a file; `Opening` dialog only on Android before `openBackup`; progress values written directly from the callback; status banner texts; `backup_missing_photos` line; Back Up checks that the data folder exists (7.2–7.4).
  - [x] Clear the Coil caches after a restore (7.5).
  - [x] Add the card to both layouts in `SettingsScreen`.
  - [x] `onCollectionRestored` in `SettingsScreen` (refreshing `categoriesList`, `appTitle`, `htmlBaseUrl`, `dataPath`) and `ToyDbNavigation` (also refreshing `ImageResolverConfig.imagesPath` and `syncTrigger++`).
  - [x] Website pages changes (7.6): new status keys, rename `ImportExportActions` → `WebsitePagesActions`. Do not change its layout (7.1).
  - [x] Info tab "Server Sync" (7.8): check the tab label and aligned headers in all 6 languages.
  - [x] *(v2.4.0)* Toy form tab "Restoration" (7.10): check the label in en, de, es, fr.
- [x] **Phase 6 — Tests and builds**
  - [x] `BackupRestoreServiceTest` (9.1), tests 1–16 (10 and 16 optional).
  - [x] Section 9.2 commands all pass (see the sandbox note in 9.2).
  - [x] Section 9.3 manual checks (desktop at minimum; Android on emulator when available).
- [x] **Phase 7 — Documentation** (use ASD-STE100 Simplified Technical English)
  - [x] `../.agents/HOW_IT_WORKS.md`:
    - §1: add a "Backup & Restore" capability; rename "Static Website Publisher" text to "Create Website Pages".
    - §2: add the `platform/` folder and new files to the source tree; note the manifest storage permissions.
    - §3: add `transaction(block)` to the `ToyDatabase` description.
    - §5: add a subsection "Backup & Restore": file layout from Section 5 (with `photos.json` and `manifest.categoryFiles`, 5.6), excluded settings, restore order, `CollectionWriteLock` (6.8, also held by Back Up while it reads, D13), the sync markers that restore writes (D7), and the 7 known limitations from Section 3.
    - §6 ("Automated and Manual Database Synchronization"): add one line: the web sync holds `CollectionWriteLock` (D9) and runs its delete-and-import in one database transaction, so a failed sync leaves the collection unchanged (D17).
    - §8: add the Android backup location `Downloads/toy_collection_backup.zip`.
    - §7: the navigation diagram labels are already correct (`SettingsScreen` "Backup/Restore", `InfoScreen` "App info & user guide"); do not change them. The Info tab list does not exist yet: **add** it (About, Server Sync, Privacy Policy, Terms of Use).
    - §9: flow 3 is out of date (it says "Database Operations" / "Export HTML Web Pages"); change it to **Settings → Create Website Pages → Create Pages**. Add flow "Backing Up and Restoring Your Collection".
    - §11: add `:composeApp:desktopTest`, `:composeApp:compileKotlinWasmJs` and `:composeApp:assembleDebug`.
  - [x] `../README.md`: add a "Backup & Restore" key feature; change "importing and exporting ... JSON" wording to plain words.
  - [x] User-visible About files `composeResources/files/about.md` and `{en,de,es,fr,it,pt}_about.md`: replace the "Portability" bullet with:
    - `**Backup & Restore**: Save a copy of your whole collection, including photos, in one file. Restore it on this device or on another device.`
    - `**Website Pages**: Create website pages that show your collection.`
    Translate both bullets into each language file. Note: `about.md` and `en_about.md` are not the same (`en_about.md` has an extra "Cloud & Network Synchronization" bullet). Edit each file on its own; do not copy one over the other.
  - [x] `../SCHEMA.md`: no change needed. It does not list `app_settings` keys (checked 2026-10-07).
  - [x] `../.agents/TODO.txt`: mark `[x]` on the backup item and on "change info tab "Backup" to "Server Sync"".
  - [x] User-visible guide `composeResources/files/sftp_setup.md` (English only): if the text calls this tab "Backup", change it to "Server Sync". Change line 11 bullet from "**Automatic Backup**" to "**Automatic Sync**" (ISSUE-01 approved). Change line 3 "to synchronize and back up your Toy Database" to "to synchronize your Toy Database" (D8, 7.8).
  - [x] This plan: Sections 10, 11, 12.

---

## 11. Change Log

| Date (UTC) | Version | Author / Agent | Changes Made |
| :--- | :--- | :--- | :--- |
| 2026-10-07 | 1.0.0 | Antigravity Agent | Initial plan. |
| 2026-10-07 | 2.0.0 | Claude (Opus 5.5) review | Reviewed against current code. Streaming design (no in-memory archive, ~1.5 GB photos); settings filter (no SFTP secrets or local paths in backups); DB transaction API; required restore order and photo timestamps (SyncImage); reuse existing JSON names (`carmaker.json`, `{prefix}list.json`), removed `toys.json`; `manifest.json`; unsafe entry names; Android permissions for API ≤ 28; full refresh after restore (`syncTrigger`); corrected Gradle tasks; user-friendly naming "Backup & Restore" (D1) and "Create Website Pages" (D2); complete strings in 6 languages; documentation phase. |
| 2026-10-07 | 2.1.0 | Claude (Opus 5.5) review | Added D8: Info tab "Backup" (SFTP guide) renamed to "Server Sync" (Section 7.8, string, checklist, docs, ISSUE-01). |
| 2026-10-07 | 2.2.0 | Claude (Opus 5.5) review | Reviewed again (code not changed since v2.1.0). Correctness: exact photo times in new `photos.json` (D12); R12, no `java.nio.file`/`FileTime` on Android API 24–25; Android "(1)" file name handled (shared MediaStore query, real name in the dialog); test 2 assertion corrected; main photo lookup without `resolveImageUri`; `images_path` set to the data directory before import; Coil caches cleared after restore. Decisions: D7 changed (restore writes sync markers from the backup JSON), D9 `CollectionWriteLock` with `HtmlSyncService`, D10 Android free-space check (`restore_no_space`), D11 desktop Back Up needs a data directory. Accuracy: Section 4 facts (settings keys, default categories, line numbers, `sftp_setup.md` English only, `../SCHEMA.md`), R1 (no app name in new strings), stricter validation, new tests, `assembleDebug`, more manual checks, 4 known limitations. |
| 2026-10-07 | 2.3.0 | Antigravity Agent review | Code audit & accuracy update: Documented HtmlSyncService `!hasToys` forced sync edge case; updated SettingsScreen line references (1056-1090) post-SSL commit 15dcd7a; clarified UI thread requirement for rememberStoragePermissionRequest; specified `File(archive.toString())` and non-fatal setLastModified handling; added 4-pattern fallback category JSON resolution on restore; added dataPath refresh in SettingsScreen on restore; noted BypassSandbox requirement for Gradle daemon; resolved ISSUE-01 (user approved updating backup_sync_title to "Server Synchronization", backup_sync_description to refer to server synchronization, and sftp_setup.md bullet to "**Automatic Sync**"). |
| 2026-10-07 | 2.4.0 | Claude (Opus 5.5) review | Reviewed against the code at `6886efc`: no code changed after the plan (newest code commit `8d3ca5c`, 2026-10-06); all Section 4 facts still true; RaceDirector `FileExportHelper` and gepetto-utils 2.1.2 unchanged; Gradle task names re-checked. Approved by the user: **New decisions** D13 (Back Up holds `CollectionWriteLock` while it reads: `prepareBackup` + `writeBackup(content, …)`), D14 (`manifest.categoryFiles`, unique category file names, Section 5.6), D15 (`backup_missing_photos` in the "Backup Complete" dialog), D16 (toy form tab `tab_restoration` → "Restoration", Section 7.10), D17 (web sync clean import in `db.transaction`, Section 6.9). **Correctness**: manifest fields without default values + `backupJson` with `encodeDefaults` (a missing `format` now fails); `readBackup` maps every error to `InvalidBackupException` and has a serializer table; restore path check compares okio `Path` values. **Robustness**: unsafe names also reject `:` and control characters; Back Up needs an existing data folder (D11); `.partial` cleanup and rename retries in `extractPhotos`; Android API 24–28 free-space check (D10); case-insensitive photo dedupe and actual disk name for rule 2b; `countPhotos` removed; `saveBackup` `write` is `suspend` and runs only after a destination is chosen. **UI**: `isWorking`, no progress dialog behind native dialogs, progress written directly from the callback, status banner texts, dialogs composable with previews. **Docs/accuracy**: known limitations 5–7 (money rounding, toys without a category, server-only categories); line numbers (`Main.kt:144-191`, `ToyForm.kt:107`, `127-135`); new Section 4 facts; agent-neutral Gradle sandbox note; §7.1 website pages layout note; HOW_IT_WORKS §6 line and §7 Info tab list; `sftp_setup.md` line 3 (D8); D7 text without math notation. **Tests**: 4 and 6 extended; new tests 11–16. |
| 2026-10-07 | 2.5.0 | Antigravity Agent review | Code audit & accuracy review against current codebase across Desktop, Android, and Wasm: (1) Fixed GcLog usage rule R4 and calls (GcLog automatically computes stack tag; passing two strings drops message without %s, use single string prefix GcLog.w("Tag: message")); (2) Added Kotlin 2.x @file:OptIn(kotlin.experimental.ExperimentalMultiplatform::class) to BackupFileHelper and BackupArchive expect/actual objects; (3) Added safe rollback try-catch in Wasm and Desktop transaction implementations, restoring previous autoCommit; (4) Added ImageResolverConfig.imagesPath refresh to onCollectionRestored in ToyDbNavigation; (5) Mandated stream closure in finally before partial file delete/rename for Windows NTFS file locking safety; (6) Verified all 27 new strings and 13 changed strings in 6 languages. |
| 2026-10-07 | 2.6.0 | Antigravity Agent | Completed execution of Phases 1 through 7: Implemented strings across 6 languages, database transaction abstraction with rollback, platform actuals for Desktop/Android/Wasm, BackupRestoreService streaming pipeline, CollectionWriteLock, BackupRestoreCard UI with dialogs, 19 unit tests passing, all builds successful (:composeApp:desktopTest, :composeApp:compileKotlinDesktop, :composeApp:compileDebugKotlinAndroid, :composeApp:compileKotlinWasmJs, :composeApp:assembleDebug), and completed all documentation updates in ASD-STE100 Simplified Technical English. |

---

## 12. Bug & Issue Log

| Issue ID | Date | Affected Component | Description & Root Cause | Resolution Status | Fix Description |
| :--- | :--- | :--- | :--- | :--- | :--- |
| ISSUE-01 | 2026-10-07 | `InfoScreen` Server Sync tab; `sftp_setup.md` | The tab content heading `backup_sync_title` said "Backup & Synchronization", and `backup_sync_description` started with "If you want to back up your collection data safely to the cloud...". After D8 the tab name and its heading did not match. Also, `composeResources/files/sftp_setup.md` line 11 said "**Automatic Backup**: Your collection data is stored safely on your private server…". | Resolved (approved by user) | Updated `backup_sync_title` to "Server Synchronization", `backup_sync_description` to refer to server synchronization in all 6 language files, and `sftp_setup.md` bullet to "**Automatic Sync**". |
