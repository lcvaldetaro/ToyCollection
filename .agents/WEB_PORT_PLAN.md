# Web (wasmJs) Port Plan: Gepetto Toy Database Manager (Living Document)

> **Document Status**: Living roadmap and technical specification
> **Target Application**: Toy Database Manager / Toy Collection (`/Users/luizvaldetaro/valdetaro/ToyCollection`)
> **Workspace**: `/Users/luizvaldetaro/valdetaro`
> **Document Location**: `ToyCollection/.agents/WEB_PORT_PLAN.md`
> **Current Phase**: Phase 0 (not started). No project source file has been changed yet.
> **Last Updated**: 2026-10-06 (Rev 2: reviewed against code, owner decisions added, design verified by prototype)
> **Code baseline**: ToyCollection commit `24d5c2b` (branch `main`), versionCode 232, Kotlin 2.4.20, Compose Multiplatform 1.12.1, gepetto-utils 2.1.2.

---

## Table of Contents
0. [How to use this document](#0-how-to-use-this-document)
1. [Summary and decisions](#1-summary-and-decisions)
2. [Prototype evidence](#2-prototype-evidence)
3. [Companion apps (reference)](#3-companion-apps-reference)
4. [Technical specification](#4-technical-specification)
5. [Phase checklist](#5-phase-checklist)
6. [Changelog](#6-changelog)
7. [Issue tracker](#7-issue-tracker)
8. [Appendix A: verified code](#8-appendix-a-verified-code)

---

## 0. How to use this document

An agent that continues this work MUST do these steps:

1. Read `/Users/luizvaldetaro/valdetaro/.agents/AGENTS.md` and `ToyCollection/.agents/AGENTS.md`. Read `ToyCollection/.agents/HOW_IT_WORKS.md` for the architecture.
2. Read sections 1, 4 and 7 of this document. Do the tasks in section 5 in order.
3. Tick each checkbox when the task is done AND its gate command passes. Add a row to section 6 (Changelog) for each work session.
4. Log each bug, blocker or surprise in section 7 (Issue tracker). Include the symptom, the root cause and the fix.
5. If the code does not match this document, stop. Log the difference in section 7. Ask the owner before you continue.

Execution rules (from the AGENTS.md files):

- **Never commit, push or stage.** The owner inspects all changes (workspace rule 1).
- **Stay in scope.** Suggest extra work in section 5, Phase 9 (backlog). Do not do it without approval.
- Use `./gradlew` for all Gradle commands. Never use `assemble` (rule 9 and 14).
- Every new user-visible string goes in `composeResources/values*/strings.xml` in all 6 languages: en, pt, de, es, fr, it (rule 6).
- New composables need `@PreviewLightDark` and a landscape `@Preview`, wrapped in `GcTheme {}` (rule 4).
- Use `GcLog` for logs. Do not use `println` (rule 3).
- Add dependencies to `gradle/libs.versions.toml` only (rule 8).
- Tests use `kotlin.test` (rule 5). If an existing test fails, assume your change caused it (rule 0 and 15).
- Write documents in Simplified Technical English (rule 16): short sentences, active voice.
- Desktop and Android MUST still compile and behave the same after every phase.

**Gate command** (run from `ToyCollection/`; called "GATE" below):

```bash
./gradlew :composeApp:compileKotlinDesktop :composeApp:compileDebugKotlinAndroid :composeApp:compileKotlinWasmJs
```

---

## 1. Summary and decisions

Gepetto's Toy Database Manager is a Kotlin Multiplatform app (Compose Multiplatform). It targets Desktop (JVM 21) and Android. This plan adds a **Web (wasmJs)** target.

### Owner decisions (2026-10-06)

| ID | Decision |
| :--- | :--- |
| **D1** | **Scope of Web v1 = "browser-local editor".** Users can browse, search, add, edit and delete toys, makers and categories. The data lives in the browser (IndexedDB). Users can sync data in from the server over HTTP. **Out of scope on Web v1** (hidden in the UI): image upload, image file rename, JSON import/export, HTML export, SFTP, data-directory setting. |
| **D2** | **Sync policy on Web.** The automatic startup sync runs only on first load (when the `toys` table is empty). After that, sync runs only when the user does a manual sync (Settings, Web Synchronization, Save). Desktop and Android keep the current behavior (startup sync always runs). |

### Design decisions (made during the review)

| ID | Decision | Reason |
| :--- | :--- | :--- |
| **D3** | Database = **sql.js 1.14.2** (SQLite compiled to WebAssembly) on the main thread, with a snapshot of the database file saved in **IndexedDB**. | `ToyDatabase` is a synchronous API (`execute`, `query`). Worker-based drivers (SQLDelight web worker, `androidx.sqlite` web) are asynchronous and do not fit. `ToyRepository.kt` stays unchanged. |
| **D4** | Images load from the server only. Use `GcImage(imageFile = name, urlForImages = baseUrl + "/")`. | On wasm, `PlatformFile.exists()` is always false, so `GcImage` builds the URL itself. This also makes the full-screen image popup work. |
| **D5** | Use `club.gepetto.composeutils.createPlatformHttpClient()` from gepetto-utils. Do **not** add a second copy in the app. | The library already has it, with a wasm actual that uses the Ktor `Js` engine. |
| **D6** | Date parsing and SHA-256 are pure common Kotlin (`JsonDateParser`, `org.kotlincrypto.hash:sha2`). | Web Crypto is asynchronous. `calculateHash` is synchronous. One common implementation removes platform actuals. |
| **D7** | Production hosting = `https://gepetto.club/database/web/`. | Same origin as the data in `https://gepetto.club/database/`. This avoids CORS. |

### What the user must know (limits of D1)

- Edits stay in one browser. There is no way to publish them (no SFTP, no export on Web v1).
- A manual sync replaces local data **only** when the server data is newer or its hash differs. If the server data did not change, local edits stay.

---

## 2. Prototype evidence

On 2026-10-06 a throw-away copy of ToyCollection (outside the repo) was changed as described in section 4, then built and run. The prototype was **not** kept. Appendix A holds the verified code.

**Verified (compile):**
- `compileKotlinWasmJs`, `compileKotlinDesktop` and `compileDebugKotlinAndroid` all pass.
- `wasmJsBrowserDevelopmentExecutableDistribution` and `wasmJsBrowserDistribution` both pass.
- Production output sizes: app `.wasm` 7.1 MB, `skiko.wasm` 8.6 MB, `composeApp.js` 0.6 MB, `sql-wasm-browser.wasm` 0.66 MB.

**Verified (run in a browser, dev build served by a static server):**
- sql.js starts, the packaged `default_toydb.db` loads, migrations run (`checkUpgrade`), and the UI shows (Home, Dashboard, Settings, Explorer).
- First-load sync: it ran (empty `toys`). It failed with a CORS error because the page was on `localhost` (see ISSUE-10). With the error normalization in Task 5.4 the app stayed alive.
- Manual sync (Settings, Web Synchronization, Save) against a local copy of `ToyCollection/json/` imported 1,567 toys. The dialog said "Web synchronization completed".
- After a full page reload the Dashboard showed the same data (1,567 toys, totals from SQL `SUM` and `GROUP BY`). The IndexedDB snapshot was 614,400 bytes. No startup sync ran on the second load.
- Settings on Web shows App Title, Web Synchronization and Categories. The SFTP, import/export and data-directory sections are hidden.
- Toy images build the URL `baseUrl + filename` (the local test server had no images, so the requests returned 404 as expected).

**Not verified (do these in Phase 7):**
- Real images from `gepetto.club`, and the CORS headers of `gepetto.club` (not reachable from the review environment).
- `wasmJsBrowserDevelopmentRun` (the dev server with live reload). The prototype used `wasmJsBrowserDevelopmentExecutableDistribution` plus `python3 -m http.server`.
- Firefox and Safari. Mobile viewport.
- Two tabs open at the same time (see ISSUE-15).
- Hiding the three image-upload buttons (Task 2.9), the web notice string (Task 5.6), unit tests (Phase 6).
- Desktop headless runs (`--headless-import-export`). Only compilation was checked.

---

## 3. Companion apps (reference)

All paths are under `/Users/luizvaldetaro/valdetaro`.

| App | Web approach | Useful files |
| :--- | :--- | :--- |
| **ToyCollectionLegacy** | Static JSON catalog viewer. No database. | `ToyCollectionLegacy/composeApp/src/wasmJsMain/kotlin/main.kt`, `.../resources/toycollection.html`, `ToyCollectionLegacy/composeApp/webpack.config.d/devServer.js`, `ToyCollectionLegacy/kotlin-js-store/wasm/yarn.lock` (committed) |
| **LapCounter** | Client viewer. Settings in `localStorage`. | `LapCounter/composeApp/src/wasmJsMain/kotlin/main.kt`, `LapCounter/composeApp/src/wasmJsMain/resources/gepettolapcounter.html`, `LapCounter/common/src/wasmJsMain/kotlin/com/valdetaro/common/FileWasm.kt` |
| **RaceDirector** | Client only. `canHostServer() = false`. | `RaceDirector/composeApp/src/wasmJsMain/kotlin/com/gepettoracedirector/platform/PlatformHostHelper.wasmJs.kt` |

Library support (gepetto-utils 2.1.2 ships `wasm-js` artifacts for `gepetto-utils`, `circum` and `gclog`): `GcTheme`, `GcImage`, `gCsetImagesBaseUrl`, `ioDispatcher` (= `Dispatchers.Default` on wasm), `gcCurrentTimeMillis()`, `createPlatformHttpClient()`, Navigation 3 wrappers. `PlatformFile` on wasm is a stub (`exists()` is always `false`).

---

## 4. Technical specification

### 4.1 Gradle and version catalog

**`gradle/libs.versions.toml`** — add:

```toml
[versions]
ktorClientJs = "3.6.0"
sqlJs = "1.14.2"
kotlincryptoHash = "0.8.0"

[libraries]
ktor-client-js = { module = "io.ktor:ktor-client-js", version.ref = "ktorClientJs" }
kotlincrypto-sha2 = { module = "org.kotlincrypto.hash:sha2", version.ref = "kotlincryptoHash" }
jetbrains-compose-ui-tooling-preview = { module = "org.jetbrains.compose.ui:ui-tooling-preview", version.ref = "jetbrainsCompose" }
```

**`composeApp/build.gradle.kts`** — change (the existing `generateCommonConfig` task stays unchanged):

```kotlin
kotlin {
    // androidTarget and jvm("desktop") stay as they are.
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            // REMOVE:  implementation(libs.ktor.client.okhttp)
            // REMOVE:  implementation(libs.androidx.ui.tooling.preview)   (no wasm variant, see ISSUE-07)
            // KEEP:    implementation(compose.components.uiToolingPreview)
            implementation(libs.jetbrains.compose.ui.tooling.preview)
            implementation(libs.kotlincrypto.sha2)
            // all other entries stay
        }
        desktopMain.dependencies {
            implementation(libs.ktor.client.okhttp)          // ADD
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)          // ADD
            // libs.androidx.ui.tooling.preview is already here: keep it.
        }
        val wasmJsMain = sourceSets.getByName("wasmJsMain")
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
            implementation(npm("sql.js", libs.versions.sqlJs.get()))
            implementation(devNpm("copy-webpack-plugin", "13.0.0"))
        }
    }
}
```

Notes:
- The `ExperimentalWasmDsl` opt-in is optional (the build works without it). The legacy apps use it. Keep it for consistency.
- Gradle downloads the npm packages (`sql.js`, `copy-webpack-plugin`) during the build. This is expected.
- After the npm dependencies change, run `./gradlew kotlinWasmUpgradeYarnLock`. Without it the build fails with "Lock file was changed" (ISSUE-11). **Commit `kotlin-js-store/wasm/yarn.lock`** (the owner commits; the agent does not). Do NOT add `kotlin-js-store/` to `.gitignore`. `ToyCollectionLegacy` commits it too.
- `webVersionCode` was in Rev 1. It is **removed**: no code reads it (`InfoScreen` uses `CommonConfig.versionCode`).

**`composeApp/webpack.config.d/sqljs.js`** (new file). webpack does not emit the sql.js `.wasm` file by itself (ISSUE-12):

```javascript
// Emit the sql.js WebAssembly binary next to composeApp.js (sql.js loads it by file name at run time).
const CopyWebpackPlugin = require('copy-webpack-plugin');

config.plugins.push(
    new CopyWebpackPlugin({
        patterns: [
            { from: require.resolve('sql.js/dist/sql-wasm-browser.wasm'), to: '.' }
        ]
    })
);
```

- webpack picks the `sql-wasm-browser.js` build of sql.js. No `resolve.fallback` for `fs`, `path` or `crypto` is needed.
- webpack prints the warning "Critical dependency: the request of a dependency is an expression". It comes from sql.js. Ignore it.

### 4.2 Changes in `commonMain` (all must keep Desktop and Android unchanged)

Line numbers are for commit `24d5c2b`. Use the symbol names if the lines moved.

| File (`composeApp/src/commonMain/kotlin/com/gepetto/toydb/...`) | Change |
| :--- | :--- |
| **`utils/PlatformSupport.kt`** (new) | `expect val systemFileSystem: FileSystem`, `expect fun userHomeDirectory(): String?`, `expect fun isWebPlatform(): Boolean`. See Appendix A. |
| **`utils/JsonDateParser.kt`** (new) | Pure Kotlin parser for `"October 4, 2026"` (US English, UTC). Returns 0 when invalid. See Appendix A. |
| **`service/HtmlSyncService.kt`** | (a) `private val client = createPlatformHttpClient()` (import `club.gepetto.composeutils.createPlatformHttpClient`); remove `import io.ktor.client.engine.okhttp.*`. (b) `parseJsonDate` calls `JsonDateParser.parse`. (c) `calculateHash` uses `SHA256().digest(...)` and `toUByte().toString(16).padStart(2, '0')`. (d) In `syncIfNewer`, replace the final `catch (e: Exception)` with the `Throwable` version in Appendix A (ISSUE-09). |
| **`ui/SyncImage.kt`** | (a) `htmlHttpClient = createPlatformHttpClient()`; remove the okhttp import. (b) `System.getProperty("user.home")` becomes `userHomeDirectory()`. (c) `FileSystem.SYSTEM` becomes `systemFileSystem`. (d) At the top of `SyncImage`, add `if (isWebPlatform()) { WebSyncImage(...); return }`. Add the private `WebSyncImage` composable (Appendix A). |
| **`ui/SettingsScreen.kt`** | (a) `FileSystem.SYSTEM` becomes `systemFileSystem` (≈ lines 200, 201, 227, 228, 231). (b) `Dispatchers.IO` becomes `club.gepetto.utils.ioDispatcher` (≈ 143 and 1597). (c) Line ≈ 340: `categoryExistsText.format(newSetting.category)` becomes `categoryExistsText.replace("%1\$s", newSetting.category)`. (d) Wrap each call of `SftpSettingsCard`, `SftpSyncActions` and `ImportExportActions` in `if (!isWebPlatform()) { ... }`. There are two layouts, so 6 call sites (≈ 1073, 1116, 1177 and 1282, 1325, 1386). Do not touch the `@Preview` call at ≈ 2061. `DataDirectorySettings` is already inside `if (isDesktopPlatform())`. (e) Add the web notice (Task 5.6). |
| **`ui/ToyDbNavigation.kt`** | (a) `FileSystem.SYSTEM` becomes `systemFileSystem` (in `copyMakerImages`, ≈ 65-94). (b) Add parameter `runStartupSync: Boolean = true` (after `onAppTitleChanged`, before `modifier`). (c) In the `LaunchedEffect(repository)` block (≈ 152) change `launch(club.gepetto.utils.ioDispatcher) {` to `if (runStartupSync) launch(club.gepetto.utils.ioDispatcher) {`. |
| **`ui/ToyForm.kt`** | `FileSystem.SYSTEM` becomes `systemFileSystem` (≈ 112-163). `System.currentTimeMillis()` becomes `gcCurrentTimeMillis()` (≈ 136; import `club.gepetto.composeutils.gcCurrentTimeMillis`). Hide the rename icon (≈ 259) and the image upload button (≈ 237) with `if (!isWebPlatform())`. |
| **`ui/ToyDetailScreen.kt`** | `FileSystem.SYSTEM` becomes `systemFileSystem` (≈ 90-100). `System.currentTimeMillis()` becomes `gcCurrentTimeMillis()` (≈ 101). Hide the "add secondary image" button (≈ 303) on Web. |
| **`ui/MakerDetailScreen.kt`** | `FileSystem.SYSTEM` becomes `systemFileSystem` (≈ 126-136). `System.currentTimeMillis()` becomes `gcCurrentTimeMillis()` (≈ 137). Hide the "add image" button (≈ 333) on Web. |
| **`ui/MakerForm.kt`** | Hide the rename icon (≈ 90) with `if (!isWebPlatform())`. |
| **`ui/ImageRenameDialog.kt`** | `FileSystem.SYSTEM` becomes `systemFileSystem` (≈ 55, 191). |
| **`ui/InfoScreen.kt`** | ≈ 57: `java.util.Locale.getDefault().language.lowercase()` becomes `androidx.compose.ui.text.intl.Locale.current.language.lowercase()`. |
| **`service/ImportExportService.kt`** | `FileSystem.SYSTEM` becomes `systemFileSystem` (≈ 146, 160, 230, 241, 530, 673). Keep `import okio.FileSystem` (it is used as a type). |

Each edited file that now uses `systemFileSystem`, `isWebPlatform` or `userHomeDirectory` needs `import com.gepetto.toydb.utils.<name>`.

All `@Preview` / `@PreviewLightDark` imports (`androidx.compose.ui.tooling.preview.*`) stay unchanged. The new `libs.jetbrains.compose.ui.tooling.preview` dependency supplies them on wasm.

### 4.3 Actuals

| Source set | File | Content |
| :--- | :--- | :--- |
| `desktopMain`, `androidMain` | `utils/PlatformSupport.kt` | `systemFileSystem = FileSystem.SYSTEM`; `userHomeDirectory() = System.getProperty("user.home")`; `isWebPlatform() = false`. |
| `wasmJsMain` | `utils/PlatformSupport.wasmJs.kt` | Empty `NoFileSystem` (all lookups return "not found", all writes throw `IOException`); `userHomeDirectory() = null`; `isWebPlatform() = true`. |
| `wasmJsMain` | `utils/ImageResolver.wasmJs.kt` | `resolveImageUri` and `resolveBitmapUri` return `null`; the two dialogs return `null`; `isDesktopPlatform() = false`; `rememberImagePicker` returns `{}`; `formatTimestamp` uses a JS `Date`. |
| `wasmJsMain` | `utils/KeepScreenOn.wasmJs.kt` | No-op. |
| `wasmJsMain` | `ui/PlatformScrollbar.wasmJs.kt` | No-op for `PlatformScrollbar` and `PlatformGridScrollbar`. |
| `wasmJsMain` | `service/ImportExportServiceWasm.kt` | `getCurrentDateString()` uses `toLocaleDateString('en-US', ...)` to get `"October 4, 2026"`. |
| `wasmJsMain` | `service/WebSftpService.kt` | Class `WebSftpService : SftpService` with `isSupported = false`. Every method returns `Result.failure(UnsupportedOperationException)`. (`SftpService.isSupported` is not read by the UI today. The UI hides the SFTP sections with `isWebPlatform()`.) |
| `wasmJsMain` | `database/SqlJs.wasmJs.kt`, `database/IndexedDb.wasmJs.kt`, `database/WasmDatabase.wasmJs.kt` | The web database. See 4.4. |
| `wasmJsMain` | `Main.kt` | Entry point. See 4.5. |
| `wasmJsMain` | `resources/index.html` | HTML shell. See 4.5. |

### 4.4 Web database (`WasmToyDatabase`)

Rules for the implementation (code in Appendix A):

1. **Kotlin/Wasm interop types.** `external` declarations may use only `JsAny`-based types, `String`, numbers and `Unit`. Use `JsArray<JsAny?>`, `JsString`, `JsNumber`, `Promise<...>`, `org.khronos.webgl.Uint8Array`. Do not use `Array<Any?>` or `Any?`. Opt in with `@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)`.
2. **Asynchronous open.** `createDatabase()` is synchronous, but sql.js start-up, `Res.readBytes` and the IndexedDB read are asynchronous. `WasmToyDatabase.open()` is a `suspend` function. `main()` calls it in a coroutine and calls `ComposeViewport` afterwards. The wasm `createDatabase` actual only throws.
3. **First run.** If IndexedDB has no snapshot, load `files/default_toydb.db` from Compose resources. Then run `checkUpgrade(database)`. Then `DELETE FROM toys` and `DELETE FROM app_settings WHERE key LIKE 'html_sync_imported_%'` (the same as the desktop `Main.kt` does on first install). Then save the snapshot at once.
4. **Existing snapshot.** Open it. Run `checkUpgrade`. If the snapshot cannot be opened, log the error and start from the default database.
5. **Saving.** Each `execute()` marks the database "dirty" and (re)starts a 1-second timer (debounce). When the timer ends, export the database (`db.export()`) and store it in IndexedDB (database `toydb_web`, store `kv`, key `database_snapshot`). Also save when the page becomes hidden (`visibilitychange`) and on `pagehide`. Do NOT save on every statement: one sync is about 1,900 statements.
6. **Cursor.** `exec()` returns all rows. Convert them at once to Kotlin lists (`null`, `Double`, `String`). `getString` of a whole `Double` returns the integer text (for example `"5"`). Known difference from JDBC: a REAL column that holds `5.0` reads as `"5"`.
7. **Parameters.** `null`, `Int`, `Long`, `Double`, `Boolean` (1 or 0) and `String` map to JS values. Anything else uses `toString()`.
8. **No explicit transactions** exist in `ToyRepository` or `HtmlSyncService` today. Do not add any in v1.
9. **Blocking.** Sync runs on the main thread on wasm (`ioDispatcher` is `Dispatchers.Default`). The UI can freeze for about one second during an import. This is accepted for v1.

### 4.5 Entry point, HTML shell and sync behavior

**`Main.kt`** (Appendix A): set the Coil loader, open the database in a coroutine, compute `needsInitialSync = database.toysCount() == 0` (decision D2), then call `ComposeViewport(viewportContainerId = "compose-App")` with `ToyDbNavigation(database, WebSftpService(), runStartupSync = needsInitialSync)`.

**Real signature** (Rev 1 was wrong): `ToyDbNavigation(db: ToyDatabase, sftpService: SftpService, onAppTitleChanged: ((String) -> Unit)? = null, runStartupSync: Boolean = true, modifier: Modifier = Modifier)`. It creates its own `ToyRepository`.

**`resources/index.html`**: one file only (not two). It uses `index.html` so the dev server needs no `devServer.js`. The shell has `#compose-App` and loads `composeApp.js`. Appendix A has the text.

**Sync on Web (D2):**
- First load: `toys` is empty, so `runStartupSync = true`. If it fails (offline, CORS), the app stays alive and the collection is empty. The next load tries again (the table is still empty).
- Later loads: `runStartupSync = false`. No automatic sync.
- Manual sync: Settings, Web Synchronization, Save. This calls `HtmlSyncService.syncIfNewer`. It replaces all local data only when the server data is newer or its hash differs (see section 1).
- **Error handling (ISSUE-09).** Ktor's JS engine throws `JsError`, which is a `Throwable` but not an `Exception`. The existing `catch (e: Exception)` blocks do not catch it. Without a fix, an uncaught error in the first-load sync **blanks the whole app**. `HtmlSyncService.syncIfNewer` must convert any non-`Exception` `Throwable` into an `Exception` (code in Appendix A). The callers (`ToyDbNavigation`, `SettingsScreen`) then work without change.

### 4.6 What the user sees on Web v1

| Area | Web v1 |
| :--- | :--- |
| Home, Dashboard, Explorer, Makers, Info | Same as Desktop. |
| Add, edit, delete toys, makers, categories | Works. Saved in the browser. |
| Toy and maker images | Shown from the server. |
| Image upload, image file rename | Hidden. |
| Settings: App title, theme, categories, Web Synchronization | Shown. |
| Settings: data directory, SFTP, JSON import/export, HTML export | Hidden. |
| Back button of the browser | Does not navigate (`BackHandler` is a stub in the library). Known limit. |

### 4.7 Dev server, CORS and hosting

- **Dev test method (verified):** build with `./gradlew :composeApp:wasmJsBrowserDevelopmentExecutableDistribution`. Copy `ToyCollection/json/*.json` into `composeApp/build/dist/wasmJs/developmentExecutable/database/`. Serve that folder, for example `python3 -m http.server 8081`. Open `http://localhost:8081/index.html`. In Settings set the Base URL to `http://localhost:8081/database/` and press Save.
- **CORS.** From `localhost` the browser blocks `https://gepetto.club/database/...` (no `Access-Control-Allow-Origin` header). This is why Task 7 uses a local copy of the data. Optional: add a webpack dev-server proxy in `composeApp/webpack.config.d/devServer.js` (not tested): `config.devServer.proxy = [{ context: ['/database'], target: 'https://gepetto.club', changeOrigin: true }];` and use Base URL `http://localhost:8080/database/`.
- **Production:** host the content of `composeApp/build/dist/wasmJs/productionExecutable/` at `https://gepetto.club/database/web/`. The server MUST send `.wasm` files as `application/wasm`. Enable gzip or brotli (the total is about 17 MB uncompressed). The page and the data are on the same origin, so CORS is not needed.
- **Base URL scheme.** The Base URL must be `https`. A page served over `https` cannot read `http` data (mixed content). The code default is `https://gepetto.club/database/` (`ToyRepository.DEFAULT_BASE_URL`). `TODO.txt` still mentions `http://valdetaro.com/database`: the code is the authority.
- **Browser support.** Kotlin/Wasm needs a recent browser with WebAssembly GC (Chrome and Edge 119+, Firefox 120+, Safari 18.2+).

---

## 5. Phase checklist

Run the gate that each phase names. All gates MUST pass. From Phase 4 on the gate is GATE (all three targets).

### Phase 0: Preflight
- [ ] **0.1** `git status` in `ToyCollection/` is clean. Record the commit hash in the changelog.
- [ ] **0.2** Run `./gradlew :composeApp:compileKotlinDesktop :composeApp:compileDebugKotlinAndroid`. Both pass (baseline).
- [ ] **0.3** Confirm Node and a modern Chrome are available (Gradle installs its own Node for the build). Do not build the companion apps (not needed).

### Phase 1: Gradle and catalog (section 4.1)
- [ ] **1.1** Edit `gradle/libs.versions.toml` (3 versions, 3 libraries).
- [ ] **1.2** Edit `composeApp/build.gradle.kts` (wasmJs target, move okhttp, preview dependency, new dependencies).
- [ ] **1.3** Create `composeApp/webpack.config.d/sqljs.js`.
- [ ] **1.4** Run `./gradlew kotlinWasmUpgradeYarnLock`. Check that `kotlin-js-store/wasm/yarn.lock` exists. Tell the owner to commit it.
- [ ] **1.5** No gate in this phase. `commonMain` still imports OkHttp until Phase 2, so compile errors are expected. Go to Phase 2.

### Phase 2: Decouple `commonMain` (section 4.2)
- [ ] **2.1** Create `utils/PlatformSupport.kt` (expects) and the desktop and Android actuals.
- [ ] **2.2** Create `utils/JsonDateParser.kt`.
- [ ] **2.3** Edit `HtmlSyncService.kt`.
- [ ] **2.4** Edit `SyncImage.kt` (including `WebSyncImage`).
- [ ] **2.5** Replace `FileSystem.SYSTEM` in: `SettingsScreen`, `ToyForm`, `ToyDetailScreen`, `MakerDetailScreen`, `ToyDbNavigation`, `ImageRenameDialog`, `ImportExportService`. Check with `grep -rn "FileSystem.SYSTEM" composeApp/src/commonMain`: no result.
- [ ] **2.6** Replace `System.currentTimeMillis()` (3 files), `Dispatchers.IO` (2 places), `.format(...)` (SettingsScreen), `java.util.Locale` (InfoScreen). Check with `grep -rnE "System\.|java\.|Dispatchers\.IO" composeApp/src/commonMain`: no result.
- [ ] **2.7** Add the `runStartupSync` parameter to `ToyDbNavigation`.
- [ ] **2.8** Hide the Settings sections (`SftpSettingsCard`, `SftpSyncActions`, `ImportExportActions`) on Web.
- [ ] **2.9** Hide the rename icons (`ToyForm`, `MakerForm`) and the three image-upload buttons on Web.
- [ ] **2.10** Run `./gradlew :composeApp:compileKotlinDesktop :composeApp:compileDebugKotlinAndroid`. Pass.
- [ ] **2.11** (Optional, ask the owner first: the command writes files to the data directory) Run the Desktop headless check `./gradlew :composeApp:run --args="--headless-import-export"` and compare the result with a run before the change.

### Phase 3: Web platform actuals (section 4.3)
- [ ] **3.1** Create `wasmJsMain/.../utils/PlatformSupport.wasmJs.kt`.
- [ ] **3.2** Create `ImageResolver.wasmJs.kt`, `KeepScreenOn.wasmJs.kt`, `PlatformScrollbar.wasmJs.kt`, `ImportExportServiceWasm.kt`, `WebSftpService.kt`.
- [ ] **3.3** No wasm gate yet: the `createDatabase` actual comes in Phase 4, so `compileKotlinWasmJs` reports a missing actual. Run `./gradlew :composeApp:compileKotlinDesktop :composeApp:compileDebugKotlinAndroid` only.

### Phase 4: Web database (section 4.4)
- [ ] **4.1** Create `SqlJs.wasmJs.kt` (external declarations).
- [ ] **4.2** Create `IndexedDb.wasmJs.kt`.
- [ ] **4.3** Create `WasmDatabase.wasmJs.kt` (`WasmToyDatabase`, `WasmSqlCursor`, `createDatabase` actual).
- [ ] **4.4** Run GATE. All three compile.

### Phase 5: Web shell and behavior (section 4.5)
- [ ] **5.1** Create the real `wasmJsMain/kotlin/Main.kt`.
- [ ] **5.2** Create `wasmJsMain/resources/index.html`.
- [ ] **5.3** Run GATE, then `./gradlew :composeApp:wasmJsBrowserDevelopmentExecutableDistribution`. Check that `composeApp/build/dist/wasmJs/developmentExecutable/` contains `index.html`, `composeApp.js` and `sql-wasm-browser.wasm`.
- [ ] **5.4** Make sure `HtmlSyncService.syncIfNewer` has the `Throwable` normalization (Task 2.3). Test it: with the page on `localhost` and the default Base URL, the first-load sync fails with a CORS error, and the app still shows the Home screen.
- [ ] **5.5** Check `SettingsScreen` on Web (Task 2.8): the three sections are hidden.
- [ ] **5.6** Add the web notice: new string key `web_local_data_notice` in all 6 `strings.xml` files. English text: "Web version: your changes are saved only in this browser. A manual sync replaces your local data when the server has newer data." Show it above `BaseUrlSettingsCard` (both layouts) only when `isWebPlatform()`. Add a `@PreviewLightDark` preview if you add a new composable.

### Phase 6: Tests (rule 5)
- [ ] **6.1** Add a `commonTest` source set with `kotlin("test")` (the project has no tests today; `src/desktopTest` is empty). Keep test data files in a `testfiles` subdirectory (rule 15).
- [ ] **6.2** `JsonDateParserTest`: `"October 4, 2026"` returns `1791072000000`; `"Oct 4, 2026"` returns the same; `"not a date"` returns `0`; `""` returns `0`; `"February 29, 2024"` returns the epoch ms of that day (verify the value with an independent tool).
- [ ] **6.3** `HtmlSyncServiceHashTest`: `HtmlSyncService.calculateHash("abc")` equals `ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad`; the empty string gives `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`.
- [ ] **6.4** Run `./gradlew :composeApp:desktopTest`. (A wasm test run needs a headless browser: optional.)

### Phase 7: Browser verification (manual; use the method in 4.7)
- [ ] **7.1** First load: clear site data. Open the page. Home shows; the Dashboard shows 0 toys; the console shows a handled sync error (CORS) and no uncaught error.
- [ ] **7.2** Set the Base URL to the local data URL, press Save. The dialog says "Web synchronization completed". The Dashboard shows the toy counts.
- [ ] **7.3** Reload. The Dashboard shows the same counts. Check that no sync ran (network tab).
- [ ] **7.4** Add a toy, edit a toy, rename a maker (with the confirmation dialog), add and delete a category. Wait 2 seconds. Reload. All changes are still there.
- [ ] **7.5** Press Save again with the same data. The dialog says there are no updates. Local edits are still there.
- [ ] **7.6** Settings shows no SFTP, no import/export and no data-directory section. The web notice shows. The image upload and rename buttons are hidden in the toy and maker screens.
- [ ] **7.7** Explorer: search and filter work. Open a toy: the full-screen image popup opens (with a real image server, see 7.8).
- [ ] **7.8** With the real server (`https://gepetto.club/database/`, same origin, or a CORS-enabled server): images load. Record the result.
- [ ] **7.9** Check a narrow (mobile) viewport and light and dark themes.
- [ ] **7.10** Open the app in Firefox and Safari. Record any problem in section 7.
- [ ] **7.11** Measure: time to first screen, and the time of a full sync, on a normal laptop. Record both.

### Phase 8: Production build and hosting notes
- [ ] **8.1** Run `./gradlew :composeApp:wasmJsBrowserDistribution`. Check `composeApp/build/dist/wasmJs/productionExecutable/` (index.html, composeApp.js, two `.wasm` files, `sql-wasm-browser.wasm`, `composeResources/`).
- [ ] **8.2** Serve that folder from a sub-path (for example `/database/web/`) and test that it loads.
- [ ] **8.3** Write the deployment steps in `README.md` (MIME type for `.wasm`, compression, folder `https://gepetto.club/database/web/`). Do not upload anything: the owner does it.
- [ ] **8.4** Update `HOW_IT_WORKS.md` (targets, source tree, Web limits) and `.agents/TODO.txt` (tick the web line).

### Phase 9: Backlog (needs the owner's approval before work)
- Download of the JSON files from Web (export) and upload (import).
- Image upload (store image bytes in IndexedDB).
- Button "Reset browser data" (delete the IndexedDB snapshot).
- Message on the empty collection when the first-load sync fails.
- Guard against two tabs that write the same snapshot.
- Loading indicator and browser-support notice in `index.html`.
- Wrap the import in one transaction (also helps Desktop and Android).
- Run the sync in a Web Worker so the UI does not freeze.

---

## 6. Changelog

| Date | Rev | Author / Agent | Changes |
| :--- | :---: | :--- | :--- |
| 2026-10-06 | 1 | Antigravity | First version of the plan. |
| 2026-10-06 | 2 | Claude (review) | Reviewed the plan against the code and the library. Prototype in a throw-away copy (compile on 3 targets, browser run). **Added:** owner decisions D1 and D2; design decisions D3-D7; the full list of `commonMain` breaks (15 files, 139 errors with Rev 1 Phase 1 only); webpack and npm steps for sql.js; Kotlin/Wasm interop rules; first-run and sync behavior; verified code (Appendix A); tests; browser verification list; hosting notes; execution rules from AGENTS.md. **Corrected:** `ToyDbNavigation` signature; the `Main.kt` example; the claim that `ImageResolver.kt` calls `java.io` (it holds only `expect` declarations); the claim that `ToyRepository` uses transactions (it does not); the image-URL strategy (full URLs break `GcImage`); the `generateCommonConfig` snippet (it dropped `inputs.property` and `outputs.dir`); a wrong LapCounter link; the "previously" wording for `InfoScreen`. **Removed:** a duplicate `createPlatformHttpClient` (the library has one); `webVersionCode`; the second HTML file; building the companion apps (Task 0.2); the `fs`/`path`/`crypto` webpack fallback (not needed). **Renamed:** `DesktopSftpService.kt` on wasm is now `WebSftpService.kt`. |

---

## 7. Issue tracker

| ID | Date | Component | Symptom | Root cause | Status / resolution |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **ISSUE-01** | 2026-10-06 | `build.gradle.kts` | `libs.ktor.client.okhttp` in `commonMain` fails on wasm. | OkHttp is JVM-only. | **Open.** Fix: Task 1.2. Verified by prototype. |
| **ISSUE-02** | 2026-10-06 | `database` | No `ToyDatabase` for wasm. | Desktop uses JDBC, Android uses the framework SQLite. | **Open.** Fix: Phase 4 (D3). Verified by prototype. |
| **ISSUE-03** | 2026-10-06 | `commonMain` | `okio.FileSystem.SYSTEM`, `System.getProperty`, `System.currentTimeMillis`, `Dispatchers.IO`, `String.format` do not exist on wasm. | `commonMain` compiled only because both targets are JVM. 15 files fail on wasm. | **Open.** Fix: Phase 2 (table in 4.2). Rev 1 named only 3 of these files. |
| **ISSUE-04** | 2026-10-06 | `HtmlSyncService.kt` | Uses `SimpleDateFormat`, `TimeZone`, `MessageDigest`, `"%02x".format`. | Java classes in `commonMain`. | **Open.** Fix: Task 2.2 and 2.3 (D6). Verified by prototype. |
| **ISSUE-05** | 2026-10-06 | `SftpService` | SFTP needs raw TCP sockets. | Browser sandbox. | **Open.** Fix: `WebSftpService` stub and hidden UI (D1). `isSupported` is not read by the UI, so the UI hides the sections itself. |
| **ISSUE-06** | 2026-10-06 | `ImportExportService`, UI | Okio has no system file system on wasm. | No disk in the browser. | **Open.** Fix: `systemFileSystem` stub (Task 2.5, 3.1). Import/export is out of scope on Web (D1). |
| **ISSUE-07** | 2026-10-06 | `build.gradle.kts` | "KMP Dependencies Resolution Failure": `androidx.compose.ui:ui-tooling-preview` has no wasm variant. | `libs.androidx.ui.tooling.preview` is in `commonMain`. | **Open.** Fix: Task 1.2 (use `org.jetbrains.compose.ui:ui-tooling-preview`, as LapCounter does). Verified: all preview errors disappear. |
| **ISSUE-08** | 2026-10-06 | `SettingsScreen.kt` | `Unresolved reference 'IO'`. | `Dispatchers.IO` does not exist on wasm. AGENTS.md mentions `Dispatchers.IO`; on wasm use `club.gepetto.utils.ioDispatcher`. | **Open.** Fix: Task 2.6. |
| **ISSUE-09** | 2026-10-06 | `HtmlSyncService`, `ToyDbNavigation` | A failed first-load sync (CORS or offline) **blanks the whole app**. Console: `Uncaught Error: Fail to fetch`. | Ktor JS throws `JsError`, a `Throwable` that is not an `Exception`. The `catch (e: Exception)` blocks miss it. The error then cancels the composition. | **Open.** Fix: Task 2.3 and 5.4 (normalize in `syncIfNewer`). Verified by prototype: the app stays alive. |
| **ISSUE-10** | 2026-10-06 | Dev setup | From `localhost`, requests to `https://gepetto.club/database/...` fail: "No 'Access-Control-Allow-Origin' header". | Cross-origin request without CORS headers. | **Open (dev only).** Fix: use local data (4.7). Production uses the same origin (D7). Check the real server in Task 7.8. |
| **ISSUE-11** | 2026-10-06 | Gradle | After adding npm dependencies: `Lock file was changed. Run the kotlinWasmUpgradeYarnLock task`. | Kotlin/Wasm keeps `kotlin-js-store/wasm/yarn.lock`. | **Open.** Fix: Task 1.4. Commit the lock file. |
| **ISSUE-12** | 2026-10-06 | webpack | The sql.js `.wasm` file is not in the output. | sql.js loads it by URL at run time; webpack does not see it. | **Open.** Fix: Task 1.3 (`CopyWebpackPlugin`). Verified by prototype. `require('copy-webpack-plugin')` needs the `devNpm` dependency. |
| **ISSUE-13** | 2026-10-06 | `SyncImage`, `GcImage` | Rev 1 would return full URLs from `resolveImageUri`. `GcImage` then builds `base + "https://..."`. | `PlatformFile.exists()` is always `false` on wasm, so `GcImage` prepends the base URL by itself. | **Open.** Fix: `WebSyncImage` (Task 2.4, D4). Verified for URL building. |
| **ISSUE-14** | 2026-10-06 | `ToyDbNavigation` | The startup sync would replace local edits on every load. | `syncIfNewer` imports again whenever the server data is newer. | **Open.** Fix: `runStartupSync` (Task 2.7, D2). |
| **ISSUE-15** | 2026-10-06 | `WasmToyDatabase` | Two tabs can overwrite each other's snapshot. | Last writer wins in IndexedDB. | **Open (accepted risk).** Backlog item. |
| **ISSUE-16** | 2026-10-06 | `WasmSqlCursor` | A REAL value `5.0` reads as `"5"` with `getString`. | JS numbers do not keep the integer or real type. | **Open (accepted).** Check in Phase 7 that prices show correctly. |

---

## 8. Appendix A: verified code

These files were compiled on 2026-10-06 (Kotlin 2.4.20, Compose Multiplatform 1.12.1) and the web build was run in a browser. Keep the behavior. You may change names and style to fit the project.

### A.1 `commonMain/.../utils/PlatformSupport.kt`

```kotlin
package com.gepetto.toydb.utils

import okio.FileSystem

/** File system used for local images and exports. On the web target it is an empty stub. */
expect val systemFileSystem: FileSystem

/** User home directory, or null when the platform has none (web). */
expect fun userHomeDirectory(): String?

/** True only on the browser (wasmJs) target. */
expect fun isWebPlatform(): Boolean
```

`desktopMain` and `androidMain` (`utils/PlatformSupport.kt`):

```kotlin
package com.gepetto.toydb.utils

import okio.FileSystem

actual val systemFileSystem: FileSystem = FileSystem.SYSTEM

actual fun userHomeDirectory(): String? = System.getProperty("user.home")

actual fun isWebPlatform(): Boolean = false
```

### A.2 `commonMain/.../utils/JsonDateParser.kt`

```kotlin
package com.gepetto.toydb.utils

/** Parses dates like "October 4, 2026" (US English, UTC). Returns epoch milliseconds, or 0 when invalid. */
object JsonDateParser {
    private val months = listOf("january","february","march","april","may","june","july","august","september","october","november","december")

    fun parse(text: String): Long {
        val match = Regex("""^\s*([A-Za-z]+)\s+(\d{1,2}),\s*(\d{4})\s*$""").find(text) ?: return 0L
        val name = match.groupValues[1].lowercase()
        val month = months.indexOfFirst { it == name || (name.length >= 3 && it.startsWith(name)) } + 1
        val day = match.groupValues[2].toInt()
        val year = match.groupValues[3].toInt()
        if (month == 0 || day !in 1..31) return 0L
        return daysFromCivil(year, month, day) * 86_400_000L
    }

    private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
        val y = if (month <= 2) year - 1 else year
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val mp = (month + 9) % 12
        val doy = (153 * mp + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097L + doe - 719468L
    }
}
```

(The algorithm matched a reference for all 47,847 dates from 1970 to 2100. `"October 4, 2026"` returns `1791072000000`.)

### A.3 `commonMain/.../service/HtmlSyncService.kt` (changed parts)

```kotlin
import club.gepetto.composeutils.createPlatformHttpClient
import com.gepetto.toydb.utils.JsonDateParser
import kotlinx.coroutines.CancellationException
import org.kotlincrypto.hash.sha2.SHA256
// remove: import io.ktor.client.engine.okhttp.*

    private val client = createPlatformHttpClient()

    fun parseJsonDate(dateStr: String): Long = JsonDateParser.parse(dateStr)

    fun calculateHash(content: String): String {
        val hashBytes = SHA256().digest(content.encodeToByteArray())
        return hashBytes.joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
    }

// at the end of syncIfNewer (replaces "} catch (e: Exception) { ... throw e }"):
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                GcLog.e(TAG, "Error performing HTML startup sync: ${e.message}", e)
                // Ktor's JS engine throws JsError, which is a Throwable but not an Exception.
                throw if (e is Exception) e else Exception(e.message, e)
            }
```

### A.4 `commonMain/.../ui/SyncImage.kt` (web branch)

At the top of `SyncImage(...)`, before the first `val`:

```kotlin
    if (isWebPlatform()) {
        WebSyncImage(toy, repository, modifier, prefix, filename, isMainImage, size, cornerSize, paddingSize, contentScale, fullImageOnClick, files, fallbackBitmap, onClick)
        return
    }
```

At the end of the file:

```kotlin
@Composable
private fun WebSyncImage(
    toy: Toy?,
    repository: ToyRepository?,
    modifier: Modifier,
    prefix: String,
    filename: String?,
    isMainImage: Boolean,
    size: Dp,
    cornerSize: Dp,
    paddingSize: Dp,
    contentScale: ContentScale,
    fullImageOnClick: Boolean,
    files: Array<String>?,
    fallbackBitmap: PlatformBitmap?,
    onClick: () -> Unit
) {
    val baseUrl = remember { repository?.getBaseUrlSetting() }
    val name = if (isMainImage) {
        toy?.picture?.trim().takeUnless { it.isNullOrEmpty() } ?: toy?.let { "$prefix${it.refNum}.jpg" }
    } else {
        filename?.trim()
    }
    val urlForImages = if (baseUrl.isNullOrBlank()) null else baseUrl.trimEnd('/') + "/"
    GcImage(
        modifier = modifier,
        imageFile = if (urlForImages == null) null else name,
        urlForImages = urlForImages,
        imageBitmap = if (urlForImages == null) fallbackBitmap else null,
        contentDescription = toy?.description ?: filename ?: "",
        fullImageOnClick = fullImageOnClick,
        size = size,
        cornerSize = cornerSize,
        paddingSize = paddingSize,
        files = files,
        contentScale = contentScale,
        onClick = onClick
    )
}
```

(Rev 2 note: an earlier draft used `imageFromUrl`. Use `imageFile` + `urlForImages`: then the full-screen popup also works.)

### A.5 `wasmJsMain/.../utils/PlatformSupport.wasmJs.kt`

```kotlin
package com.gepetto.toydb.utils

import okio.FileHandle
import okio.FileMetadata
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Sink
import okio.Source

/** The browser has no disk. Every lookup reports "not found" and every write fails. */
private object NoFileSystem : FileSystem() {
    private fun unsupported(): Nothing = throw IOException("File system not available on web")
    override fun canonicalize(path: Path): Path = path
    override fun metadataOrNull(path: Path): FileMetadata? = null
    override fun list(dir: Path): List<Path> = unsupported()
    override fun listOrNull(dir: Path): List<Path>? = null
    override fun openReadOnly(file: Path): FileHandle = unsupported()
    override fun openReadWrite(file: Path, mustCreate: Boolean, mustExist: Boolean): FileHandle = unsupported()
    override fun source(file: Path): Source = unsupported()
    override fun sink(file: Path, mustCreate: Boolean): Sink = unsupported()
    override fun appendingSink(file: Path, mustExist: Boolean): Sink = unsupported()
    override fun createDirectory(dir: Path, mustCreate: Boolean) = unsupported()
    override fun atomicMove(source: Path, target: Path) = unsupported()
    override fun delete(path: Path, mustExist: Boolean) = unsupported()
    override fun createSymlink(source: Path, target: Path) = unsupported()
}

actual val systemFileSystem: FileSystem = NoFileSystem

actual fun userHomeDirectory(): String? = null

actual fun isWebPlatform(): Boolean = true
```

### A.6 Other small wasm actuals

`utils/ImageResolver.wasmJs.kt`:

```kotlin
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.gepetto.toydb.utils

import androidx.compose.runtime.Composable

actual fun resolveImageUri(prefix: String, refNum: Int): String? = null

actual fun resolveBitmapUri(filename: String): String? = null

actual fun selectDirectoryDialog(title: String): String? = null

actual fun selectFileDialog(title: String, allowedExtensions: List<String>): String? = null

actual fun isDesktopPlatform(): Boolean = false

@Composable
actual fun rememberImagePicker(onImagePicked: (String) -> Unit): () -> Unit = {}

@JsFun("""(ms) => {
    const d = new Date(ms);
    const p = (n) => String(n).padStart(2, '0');
    return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes()) + ':' + p(d.getSeconds());
}""")
private external fun jsFormatTimestamp(millis: Double): String

actual fun formatTimestamp(timestamp: Long): String = jsFormatTimestamp(timestamp.toDouble())
```

`utils/KeepScreenOn.wasmJs.kt`:

```kotlin
package com.gepetto.toydb.utils

import androidx.compose.runtime.Composable

@Composable
actual fun KeepScreenOn(enabled: Boolean) {
    // No-op on web
}
```

`ui/PlatformScrollbar.wasmJs.kt`:

```kotlin
package com.gepetto.toydb.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun PlatformScrollbar(state: LazyListState, modifier: Modifier) {
    // No-op on web
}

@Composable
actual fun PlatformGridScrollbar(state: LazyGridState, modifier: Modifier) {
    // No-op on web
}
```

`service/ImportExportServiceWasm.kt`:

```kotlin
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.gepetto.toydb.service

@JsFun("() => new Date().toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' })")
private external fun jsCurrentDate(): String

actual fun getCurrentDateString(): String = jsCurrentDate()
```

`service/WebSftpService.kt`:

```kotlin
package com.gepetto.toydb.service

import com.gepetto.toydb.database.ToyDatabase

/** SFTP needs raw TCP sockets, which browsers do not allow. */
class WebSftpService : SftpService {
    override val isSupported: Boolean = false

    private fun <T> unsupported(): Result<T> =
        Result.failure(UnsupportedOperationException("SFTP is not available on web"))

    override suspend fun testConnection(
        config: SftpConfig,
        onHostKeyUnverified: suspend (String, Int, String) -> Boolean
    ): Result<Unit> = unsupported()

    override suspend fun calculateUploadPlan(
        config: SftpConfig, db: ToyDatabase,
        onHostKeyUnverified: suspend (String, Int, String) -> Boolean
    ): Result<List<SyncAction>> = unsupported()

    override suspend fun calculateDownloadPlan(
        config: SftpConfig, db: ToyDatabase,
        onHostKeyUnverified: suspend (String, Int, String) -> Boolean
    ): Result<List<SyncAction>> = unsupported()

    override suspend fun uploadData(
        config: SftpConfig, db: ToyDatabase,
        onHostKeyUnverified: suspend (String, Int, String) -> Boolean,
        selectedFiles: Set<String>?, onProgress: (String, Float) -> Unit
    ): Result<Unit> = unsupported()

    override suspend fun downloadData(
        config: SftpConfig, db: ToyDatabase,
        onHostKeyUnverified: suspend (String, Int, String) -> Boolean,
        selectedFiles: Set<String>?, onProgress: (String, Float) -> Unit
    ): Result<Unit> = unsupported()
}
```

### A.7 `wasmJsMain/.../database/SqlJs.wasmJs.kt`

```kotlin
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.gepetto.toydb.database

import kotlin.js.JsAny
import kotlin.js.JsArray
import kotlin.js.JsString
import kotlin.js.Promise
import org.khronos.webgl.Uint8Array

external interface SqlJsQueryResult : JsAny {
    val columns: JsArray<JsString>
    val values: JsArray<JsArray<JsAny?>>
}

external interface SqlJsDatabase : JsAny {
    fun run(sql: String, params: JsArray<JsAny?>)
    fun exec(sql: String, params: JsArray<JsAny?>): JsArray<SqlJsQueryResult>
    fun export(): Uint8Array
    fun close()
}

external interface SqlJsStatic : JsAny

@JsModule("sql.js")
external fun initSqlJs(config: JsAny): Promise<SqlJsStatic>

@JsFun("() => ({ locateFile: (file) => file })")
external fun sqlJsConfig(): JsAny

@JsFun("(sql, data) => new sql.Database(data)")
external fun newSqlJsDatabase(sql: SqlJsStatic, data: Uint8Array?): SqlJsDatabase

/** 0 = null, 1 = number, 2 = string, 3 = other */
@JsFun("(v) => v === null || v === undefined ? 0 : (typeof v === 'number' ? 1 : (typeof v === 'string' ? 2 : 3))")
external fun cellKind(value: JsAny?): Int

@JsFun("(b64) => { const bin = atob(b64); const u8 = new Uint8Array(bin.length); for (let i = 0; i < bin.length; i++) u8[i] = bin.charCodeAt(i); return u8; }")
external fun base64ToUint8Array(base64: String): Uint8Array
```

(`locateFile: (file) => file` makes sql.js load `sql-wasm-browser.wasm` relative to the page. It works when the `.wasm` file is next to `index.html`, also under a sub-path.)

### A.8 `wasmJsMain/.../database/IndexedDb.wasmJs.kt`

```kotlin
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.gepetto.toydb.database

import kotlin.js.JsAny
import kotlin.js.Promise
import org.khronos.webgl.Uint8Array

@JsFun("""(key) => new Promise((resolve, reject) => {
    const open = indexedDB.open('toydb_web', 1);
    open.onupgradeneeded = () => open.result.createObjectStore('kv');
    open.onerror = () => reject(open.error);
    open.onsuccess = () => {
        const req = open.result.transaction('kv', 'readonly').objectStore('kv').get(key);
        req.onsuccess = () => resolve(req.result === undefined ? null : req.result);
        req.onerror = () => reject(req.error);
    };
})""")
external fun idbGet(key: String): Promise<Uint8Array?>

@JsFun("""(key, value) => new Promise((resolve, reject) => {
    const open = indexedDB.open('toydb_web', 1);
    open.onupgradeneeded = () => open.result.createObjectStore('kv');
    open.onerror = () => reject(open.error);
    open.onsuccess = () => {
        const tx = open.result.transaction('kv', 'readwrite');
        tx.objectStore('kv').put(value, key);
        tx.oncomplete = () => resolve(null);
        tx.onerror = () => reject(tx.error);
    };
})""")
external fun idbPut(key: String, value: Uint8Array): Promise<JsAny?>

@JsFun("(callback) => { document.addEventListener('visibilitychange', () => { if (document.visibilityState === 'hidden') callback(); }); window.addEventListener('pagehide', () => callback()); }")
external fun onPageHidden(callback: () -> Unit)
```

### A.9 `wasmJsMain/.../database/WasmDatabase.wasmJs.kt`

```kotlin
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class, kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.gepetto.toydb.database

import club.gepetto.GcLog
import kotlin.io.encoding.Base64
import kotlin.js.JsAny
import kotlin.js.JsArray
import kotlin.js.toJsNumber
import kotlin.js.toJsString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import toydb.composeapp.generated.resources.Res

private const val SNAPSHOT_KEY = "database_snapshot"
private const val SAVE_DELAY_MS = 1000L
private const val TAG_WASM = "WasmToyDatabase"

class WasmSqlCursor(private val columns: List<String>, private val rows: List<List<Any?>>) : SqlCursor {
    private var index = -1
    override fun next(): Boolean = ++index < rows.size
    private fun cell(name: String): Any? {
        val col = columns.indexOf(name)
        return if (col < 0) null else rows[index][col]
    }
    override fun getString(columnName: String): String? = when (val v = cell(columnName)) {
        null -> null
        is Double -> if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
        else -> v.toString()
    }
    override fun getInt(columnName: String): Int? = when (val v = cell(columnName)) {
        null -> null
        is Double -> v.toInt()
        else -> v.toString().toIntOrNull()
    }
    override fun getDouble(columnName: String): Double? = when (val v = cell(columnName)) {
        null -> null
        is Double -> v
        else -> v.toString().toDoubleOrNull()
    }
    override fun close() {}
}

class WasmToyDatabase private constructor(private val db: SqlJsDatabase) : ToyDatabase {
    private val scope: CoroutineScope = MainScope()
    private var saveJob: Job? = null
    private var dirty = false

    private fun List<Any?>.toJsParams(): JsArray<JsAny?> {
        val out = JsArray<JsAny?>()
        forEachIndexed { i, v ->
            out[i] = when (v) {
                null -> null
                is Int -> v.toJsNumber()
                is Long -> v.toDouble().toJsNumber()
                is Double -> v.toJsNumber()
                is Boolean -> (if (v) 1 else 0).toJsNumber()
                else -> v.toString().toJsString()
            }
        }
        return out
    }

    override fun execute(sql: String, bindArgs: List<Any?>) {
        db.run(sql, bindArgs.toJsParams())
        markDirty()
    }

    override fun query(sql: String, bindArgs: List<String>): SqlCursor {
        val results = db.exec(sql, bindArgs.toJsParams())
        if (results.length == 0) return WasmSqlCursor(emptyList(), emptyList())
        val first = results[0]!!
        val columns = List(first.columns.length) { first.columns[it].toString() }
        val rows = List(first.values.length) { r ->
            val row = first.values[r]!!
            List(row.length) { c ->
                val v = row[c]
                when (cellKind(v)) {
                    0 -> null
                    1 -> v!!.unsafeCast<kotlin.js.JsNumber>().toDouble()
                    else -> v.toString()
                }
            }
        }
        return WasmSqlCursor(columns, rows)
    }

    private fun markDirty() {
        dirty = true
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(SAVE_DELAY_MS)
            flush()
        }
    }

    suspend fun flush() {
        if (!dirty) return
        dirty = false
        try {
            idbPut(SNAPSHOT_KEY, db.export()).await<JsAny?>()
        } catch (e: Throwable) {
            dirty = true
            GcLog.e(TAG_WASM, "Failed to persist database snapshot: ${e.message}")
        }
    }

    override fun close() {
        db.close()
    }

    fun toysCount(): Int {
        val c = query("SELECT COUNT(*) as total FROM toys")
        val n = if (c.next()) c.getInt("total") ?: 0 else 0
        c.close()
        return n
    }

    companion object {
        suspend fun open(): WasmToyDatabase {
            val sql = initSqlJs(sqlJsConfig()).await<SqlJsStatic>()
            var raw: SqlJsDatabase? = null
            val snapshot = idbGet(SNAPSHOT_KEY).await<org.khronos.webgl.Uint8Array?>()
            if (snapshot != null) {
                try { raw = newSqlJsDatabase(sql, snapshot) } catch (e: Throwable) {
                    GcLog.e(TAG_WASM, "Stored snapshot is unreadable, starting from default database: ${e.message}")
                }
            }
            val isInitialInstall = raw == null
            if (raw == null) {
                val bytes = Res.readBytes("files/default_toydb.db")
                raw = newSqlJsDatabase(sql, base64ToUint8Array(Base64.encode(bytes)))
            }
            val database = WasmToyDatabase(raw)
            checkUpgrade(database)
            if (isInitialInstall) {
                database.execute("DELETE FROM toys")
                database.execute("DELETE FROM app_settings WHERE key LIKE 'html_sync_imported_%'")
                database.flush()
            }
            onPageHidden { database.scope.launch { database.flush() } }
            return database
        }
    }
}

actual fun createDatabase(platformContext: Any?, dbName: String): ToyDatabase =
    error("On web, call WasmToyDatabase.open() from main() instead of createDatabase().")
```

(The prototype stored the opened database in a holder instead of throwing. The `error(...)` version above is simpler: `Main.kt` never calls `createDatabase`.)

### A.10 `wasmJsMain/kotlin/Main.kt`

```kotlin
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import club.gepetto.composeutils.GcTheme
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.gepetto.toydb.database.WasmToyDatabase
import com.gepetto.toydb.service.WebSftpService
import com.gepetto.toydb.ui.ToyDbNavigation
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    SingletonImageLoader.setSafe { ImageLoader.Builder(PlatformContext.INSTANCE).build() }
    MainScope().launch {
        val database = WasmToyDatabase.open()
        // Startup sync runs only on first load (empty toys table). Later syncs are manual.
        val needsInitialSync = database.toysCount() == 0
        ComposeViewport(viewportContainerId = "compose-App") {
            val sftpService = remember { WebSftpService() }
            GcTheme {
                ToyDbNavigation(database, sftpService, runStartupSync = needsInitialSync)
            }
        }
    }
}
```

### A.11 `wasmJsMain/resources/index.html`

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Gepetto Toy Database Manager</title>
    <style>
        html, body { width: 100%; height: 100%; margin: 0; padding: 0; overflow: hidden; background-color: #121212; }
        #compose-App { width: 100%; height: 100%; }
    </style>
    <script type="application/javascript" src="composeApp.js"></script>
</head>
<body>
    <div id="compose-App"></div>
</body>
</html>
```
