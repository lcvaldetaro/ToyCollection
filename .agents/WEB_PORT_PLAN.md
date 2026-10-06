# Web (wasmJs) Port Plan: Gepetto Toy Database Manager (Living Document)

> **Document Status**: Living roadmap and technical specification
> **Target Application**: Toy Database Manager / Toy Collection (`/Users/luizvaldetaro/valdetaro/ToyCollection`)
> **Workspace**: `/Users/luizvaldetaro/valdetaro`
> **Document Location**: `ToyCollection/.agents/WEB_PORT_PLAN.md`
> **Current Phase**: Phase 0 (in progress). Preflight and phase execution starting.
> **Last Updated**: 2026-10-06 (Rev 7: review against `HEAD` `f2195ef`; verified line numbers, symbols, dependencies across all source sets; start of execution)
> **Code baseline**: ToyCollection commit `1891463` (branch `main`). Source code is unchanged up to `HEAD` `f2195ef` (later commits change documents only). gepetto-utils 2.1.2 is unchanged since it was published. versionCode 232, versionName 3.0.32, Kotlin 2.4.20, Compose Multiplatform 1.12.1, gepetto-utils 2.1.2.

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
- **Gradle in a sandbox**: `./gradlew` connects to the Gradle daemon over loopback TCP (`127.0.0.1`). If your agent sandbox blocks this connection, run Gradle outside the sandbox (use the setting of your agent tool). The first wasm build and `kotlinWasmUpgradeYarnLock` also download Node.js and npm packages, so they need network access.
- Every new user-visible string goes in `composeResources/values*/strings.xml` in all 6 languages: en, pt, de, es, fr, it (rule 6).
- New composables need `@PreviewLightDark` and a landscape `@Preview`, wrapped in `GcTheme {}` (rule 4).
- Use `GcLog` for logs. Do not use `println` (rule 3). In new code, use `GcLog.e(throwable, "Tag: message")` or `GcLog.d("Tag: message")`. `GcLog` has no tag parameter: the form `GcLog.e(TAG, "message", e)` prints only the tag (ISSUE-27).
- Add dependencies AND versions (including npm package versions) to `gradle/libs.versions.toml` only. Do not write a version number in a build file (rule 8).
- Tests use `kotlin.test` (rule 5). If an existing test fails, assume your change caused it (rule 0 and 15).
- Write documents in Simplified Technical English (rule 16): short sentences, active voice.
- Desktop and Android MUST still compile and behave the same after every phase. The only approved exceptions are the Desktop/Android-visible changes listed in decisions D9 and D13.
- **Coordination with `TODO.txt`.** The open items "simplify the settings / break into tabs" and "change info tab Backup to Server Sync" move code in `SettingsScreen.kt` and `InfoScreen.kt`. If they are done before this plan, find the code by symbol name, not by line number.

**Gate command** (run from `ToyCollection/`; called "GATE" below):

```bash
./gradlew :composeApp:compileKotlinDesktop :composeApp:compileDebugKotlinAndroid :composeApp:compileKotlinWasmJs
```

**Assemble command** (rule 9: assemble all targets, but never use `assemble`, which builds release variants; called "ASSEMBLE" below):

```bash
./gradlew :composeApp:assembleDebug :composeApp:desktopJar :composeApp:wasmJsBrowserDevelopmentExecutableDistribution
```

---

## 1. Summary and decisions

Gepetto's Toy Database Manager is a Kotlin Multiplatform app (Compose Multiplatform). It targets Desktop (JVM 21) and Android. This plan adds a **Web (wasmJs)** target.

### Owner decisions (2026-10-06)

| ID | Decision |
| :--- | :--- |
| **D1** | **Scope of Web v1 = "browser-local editor".** Users can browse, search, add, edit and delete toys, makers and categories. The data lives in the browser (IndexedDB). Users can sync data in from the server over HTTP. **Out of scope on Web v1** (hidden in the UI): image upload, image file rename, JSON import/export, HTML export, SFTP (including the SFTP setup button on InfoScreen's Backup tab), data-directory setting. |
| **D2** | **Sync policy on Web.** The automatic startup sync runs only on first load (when the `toys` table is empty). After that, sync runs only when the user does a manual sync (Settings, Web Synchronization, Save). Desktop and Android keep the current behavior (startup sync always runs). |
| **D8** | **Info screen Backup tab on Web (Rev 4).** Keep the tab. Hide only the "SFTP Server Setup Guide" button (`sftp_setup_guide_btn`). Hiding the whole tab is not in scope now. |
| **D9** | **Category list reload after a manual sync (Rev 4).** Apply on **all targets**. This is an approved cross-platform bug fix: today the Settings category list on Desktop and Android stays stale after a manual web sync. |
| **D10** | **No image-name guessing on Web (Rev 4).** When a toy has a blank `picture`, Web shows the fallback, the same as Desktop and Android. Web does not request `"$prefix$refNum.jpg"`. |
| **D11** | **`webVersionCode` (Rev 4).** Add `webVersionCode = versionCode * 10 + 6` to the generated `CommonConfig`, the same as LapCounter, RaceDirector and ToyCollectionLegacy. |
| **D12** | **Workspace documentation (Rev 4).** In Phase 8, update the Toy Collection entry in `~/valdetaro/.agents/AGENTS.md` to list the Web target. |
| **D14** | **Host name (Rev 4).** Use only `https://gepetto.club` (no `www`). The app is at `https://gepetto.club/database/web/` and the data at `https://gepetto.club/database/`. Do not add `www` support, redirects or CORS headers. |

### Design decisions (made during the review)

| ID | Decision | Reason |
| :--- | :--- | :--- |
| **D3** | Database = **sql.js 1.14.2** (SQLite compiled to WebAssembly) on the main thread, with a snapshot of the database file saved in **IndexedDB**. | `ToyDatabase` is a synchronous API (`execute`, `query`). Worker-based drivers (SQLDelight web worker, `androidx.sqlite` web) are asynchronous and do not fit. `ToyRepository.kt` stays unchanged. |
| **D4** | Images load from the server only. Use `GcImage(imageFile = name, urlForImages = baseUrl + "/")`. | On wasm, `PlatformFile.exists()` is always false, so `GcImage` builds the URL itself. This also makes the full-screen image popup work. |
| **D5** | Use `club.gepetto.composeutils.createPlatformHttpClient()` from gepetto-utils. Do **not** add a second copy in the app. | The library already has it, with a wasm actual that uses the Ktor `Js` engine. The Desktop and Android actuals are `HttpClient(OkHttp)` with a preconfigured `OkHttpClient` (MODERN_TLS, COMPATIBLE_TLS, CLEARTEXT). No `expectSuccess`, so non-200 responses still return a status (verified, Rev 4). |
| **D6** | Date parsing and SHA-256 are pure common Kotlin (`JsonDateParser`, `org.kotlincrypto.hash:sha2`). | Web Crypto is asynchronous. `calculateHash` is synchronous. One common implementation removes platform actuals. |
| **D7** | Production hosting = `https://gepetto.club/database/web/`. | Same origin as the data in `https://gepetto.club/database/`. This avoids CORS. |
| **D13** | `HtmlSyncService` sends `Cache-Control: no-cache` on every request (all targets). | The browser HTTP cache can return old JSON, so a manual sync can report "already up to date" (ISSUE-20). OkHttp has no cache by default, so Desktop and Android behave the same. |

### What the user must know (limits of D1)

- Edits stay in one browser. There is no way to publish them (no SFTP, no export on Web v1).
- A manual sync replaces local data **only** when the server data is newer, its hash differs, or the local `toys` table is empty (`HtmlSyncService.syncIfNewer`). If the server data did not change, local edits stay.
- The app saves edits to the browser about 1 second after the last change. The save when the tab closes is best effort, so edits made in the last second before the tab closes can be lost.

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
- Manual sync (Settings, Web Synchronization, Save) against a local copy of `ToyCollection/json/` worked. The Dashboard showed 1,567 toys. The dialog said "Web synchronization completed".
- After a full page reload the Dashboard showed the same data (1,567 toys, totals from SQL `SUM` and `GROUP BY`). The IndexedDB snapshot was 614,400 bytes. No startup sync ran on the second load.
- Settings on Web shows App Title, Web Synchronization and Categories. The SFTP, import/export and data-directory sections are hidden.
- Toy images build the URL `baseUrl + filename` (the local test server had no images, so the requests returned 404 as expected).
- Note (Rev 6, replaces the Rev 4 note): the local `ToyCollection/json/` folder holds **1,649** toys (carlist 1,443, tralist 68, stalist 113, plalist 17, mislist 8). The Dashboard count of **1,567 is correct**. `ToyRepository.getDashboardStats` counts only toys with an empty `traded` field, and 82 slot cars have a `traded` value (1,649 − 82 = 1,567). The Explorer shows all toys, including traded toys. See Task 7.2 for the expected values (ISSUE-28).

**Not verified (do these in Phase 7):**
- Real images from `gepetto.club`, and the CORS headers of `gepetto.club` (not reachable from the review environment).
- `wasmJsBrowserDevelopmentRun` (the dev server with live reload). The prototype used `wasmJsBrowserDevelopmentExecutableDistribution` plus `python3 -m http.server`.
- Firefox and Safari. Mobile viewport.
- Two tabs open at the same time (see ISSUE-15).
- Desktop headless runs (`--headless-import-export`). Do not run them: they change the real Desktop database (Task 2.11, ISSUE-29). Only compilation was checked.
- Console output of `GcLog` on Web. The prototype did not plant a log tree, so `GcLog` wrote nothing (ISSUE-27).

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
sqlJs = "1.14.2"
kotlincryptoHash = "0.8.0"
copyWebpackPlugin = "13.0.0"

[libraries]
ktor-client-js = { module = "io.ktor:ktor-client-js", version.ref = "ktorClientCore" }
kotlincrypto-sha2 = { module = "org.kotlincrypto.hash:sha2", version.ref = "kotlincryptoHash" }
jetbrains-compose-ui-tooling-preview = { module = "org.jetbrains.compose.ui:ui-tooling-preview", version.ref = "jetbrainsCompose" }
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }
```

**`composeApp/build.gradle.kts`** — change:

```kotlin
// In the existing generateCommonConfig task (decision D11), add one line to the generated object,
// after "desktopVersionCode". Same formula as LapCounter, RaceDirector and ToyCollectionLegacy:
//                 const val webVersionCode = ${vCode * 10 + 6}L

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
        val commonMain = sourceSets.getByName("commonMain")
        commonMain.kotlin.srcDir(generateCommonConfig)
        commonMain.dependencies {
            // REMOVE:  implementation(libs.ktor.client.okhttp)
            // REMOVE:  implementation(libs.androidx.ui.tooling.preview)   (no wasm variant, see ISSUE-07)
            // KEEP:    implementation(compose.components.uiToolingPreview)
            implementation(libs.jetbrains.compose.ui.tooling.preview)
            implementation(libs.kotlincrypto.sha2)
            // all other existing entries stay
        }

        val commonTest = sourceSets.getByName("commonTest")   // Phase 6
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        val desktopMain = sourceSets.getByName("desktopMain")
        desktopMain.dependencies {
            implementation(libs.ktor.client.okhttp)          // ADD
            // all other existing entries stay
        }

        val androidMain = sourceSets.getByName("androidMain")
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)          // ADD
            // libs.androidx.ui.tooling.preview is already here: keep it.
            // all other existing entries stay
        }

        val wasmJsMain = sourceSets.getByName("wasmJsMain")
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
            implementation(npm("sql.js", libs.versions.sqlJs.get()))
            implementation(devNpm("copy-webpack-plugin", libs.versions.copyWebpackPlugin.get()))
        }
    }
}
```

Notes:
- The `ExperimentalWasmDsl` opt-in is optional (the build works without it). The legacy apps use it. Keep it for consistency.
- Gradle downloads the npm packages (`sql.js`, `copy-webpack-plugin`) during the build. This is expected.
- After the npm dependencies change, run `./gradlew kotlinWasmUpgradeYarnLock`. Without it the build fails with "Lock file was changed" (ISSUE-11). **Commit `kotlin-js-store/wasm/yarn.lock`** (the owner commits; the agent does not). Do NOT add `kotlin-js-store/` to `.gitignore`. `ToyCollectionLegacy` commits it too.
- `webVersionCode` (Rev 4, decision D11) is back: `CommonConfig.webVersionCode = versionCode * 10 + 6` (232 gives 2326). It is for consistency with the other apps. `InfoScreen` keeps `CommonConfig.versionCode` on all targets. Do not change any other code to read it.
- `ktor-client-js` is optional: the Ktor `Js` engine is also in `ktor-client-core` for wasmJs. Keep it; it is harmless and makes the engine explicit.

**`composeApp/webpack.config.d/sqljs.js`** (new file). webpack does not emit the sql.js `.wasm` file by itself (ISSUE-12):

```javascript
// Emit the sql.js WebAssembly binary next to composeApp.js (sql.js loads it by file name at run time).
const CopyWebpackPlugin = require('copy-webpack-plugin');

config.plugins = config.plugins || [];
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

### 4.2 Changes in `commonMain` (Desktop and Android stay unchanged, except D9 and D13)

Line numbers are for commit `1891463` (still valid at `HEAD` `328449f`; Rev 6 checked each one and found exact matches). Use symbol names if lines moved.

| File (`composeApp/src/commonMain/kotlin/com/gepetto/toydb/...`) | Change |
| :--- | :--- |
| **`utils/PlatformSupport.kt`** (new) | `expect val systemFileSystem: FileSystem`, `expect fun userHomeDirectory(): String?`, `expect fun isWebPlatform(): Boolean`. See Appendix A.1. |
| **`utils/JsonDateParser.kt`** (new) | Pure Kotlin parser for `"October 4, 2026"` (US English, UTC). Returns 0 when invalid. See Appendix A.2. |
| **`service/HtmlSyncService.kt`** | (a) `private val client = createPlatformHttpClient { defaultRequest { header(HttpHeaders.CacheControl, "no-cache") } }` (decision D13; imports `club.gepetto.composeutils.createPlatformHttpClient`, `io.ktor.client.plugins.defaultRequest`, `io.ktor.client.request.header`, `io.ktor.http.HttpHeaders`); remove `import io.ktor.client.engine.okhttp.*`. (b) `parseJsonDate` calls `JsonDateParser.parse`. (c) `calculateHash` uses `SHA256().digest(...)` and `toUByte().toString(16).padStart(2, '0')`. (d) In `syncIfNewer`, replace the final `catch (e: Exception)` with the `Throwable` version in Appendix A.3 (ISSUE-09). |
| **`ui/SyncImage.kt`** | (a) `htmlHttpClient = createPlatformHttpClient()`; remove okhttp import. (b) `System.getProperty("user.home")` becomes `userHomeDirectory()`. (c) `FileSystem.SYSTEM` becomes `systemFileSystem`. (d) At the top of `SyncImage`, add `if (isWebPlatform()) { WebSyncImage(...); return }`. Add the private `WebSyncImage` composable (Appendix A.4). It never guesses a file name (decision D10). |
| **`ui/SettingsScreen.kt`** | (a) `FileSystem.SYSTEM` becomes `systemFileSystem` (≈ lines 200, 201, 227, 228, 231). (b) `Dispatchers.IO` becomes `club.gepetto.utils.ioDispatcher` (≈ 143 and 1597). (c) Line ≈ 340: `categoryExistsText.format(newSetting.category)` becomes `categoryExistsText.replace("%1\$s", newSetting.category)`. (d) Hide `SftpSettingsCard`, `SftpSyncActions` and `ImportExportActions` with one `if (!isWebPlatform()) { ... }` block in each layout (2 blocks in total). Each block starts at the `Spacer` just after `BaseUrlSettingsCard` (≈ 1072 wide layout, ≈ 1281 narrow layout) and ends after the closing parenthesis of the `ImportExportActions(...)` call (≈ 1187 wide, ≈ 1395 narrow). The block contains 2 `Spacer` lines (≈ 1072 and 1176, ≈ 1281 and 1385) and the 3 calls. In the narrow layout, keep the `Spacer` before `CategoriesManager` (≈ 1396) outside the block. Do not touch the `@Preview` call at ≈ 2061. `DataDirectorySettings` is already inside `if (isDesktopPlatform())`. (e) Decision D9, **all targets**: in both sync completion blocks (≈ 1058 and 1267, inside `if (syncSuccess)`), add `categoriesList = repository.getCategorySettings()` next to `onCategoriesChanged()`. (f) Add the `WebLocalDataNotice` composable (Task 5.6). |
| **`ui/ToyDbNavigation.kt`** | (a) `FileSystem.SYSTEM` becomes `systemFileSystem` (in `copyMakerImages`, ≈ 65-94). (b) Add parameter `runStartupSync: Boolean = true` (after `onAppTitleChanged`, before `modifier`). The Desktop call (`Main.kt` line 92) and the Android call (`AppMainActivity.kt` line 75) use named or leading arguments, so they need no change. (c) In the `LaunchedEffect(repository)` block (≈ 150) change `launch(club.gepetto.utils.ioDispatcher) {` to `if (runStartupSync) launch(club.gepetto.utils.ioDispatcher) {`. |
| **`ui/ToyForm.kt`** | `FileSystem.SYSTEM` becomes `systemFileSystem` (≈ 112-163). `System.currentTimeMillis()` becomes `gcCurrentTimeMillis()` (≈ 136; import `club.gepetto.composeutils.gcCurrentTimeMillis`). Hide the rename icon (≈ 259) and the image upload button (≈ 237) with `if (!isWebPlatform())`. |
| **`ui/ToyDetailScreen.kt`** | (a) `FileSystem.SYSTEM` becomes `systemFileSystem` (≈ 90-100). (b) `System.currentTimeMillis()` becomes `gcCurrentTimeMillis()` (≈ 101). (c) Hide the "add secondary image" button (≈ 302) on Web. (d) In `allImagePaths` (≈ 69-78): when `isWebPlatform()`, add `toy.picture.trim()` only when it is not empty (decision D10: no `"$prefix${toy.refNum}.jpg"` guess), then add the trimmed, non-empty secondary image file names. This gives the `files` array for full-screen carousel navigation on Web (ISSUE-17). |
| **`ui/MakerDetailScreen.kt`** | (a) `FileSystem.SYSTEM` becomes `systemFileSystem` (≈ 126-136). (b) `System.currentTimeMillis()` becomes `gcCurrentTimeMillis()` (≈ 137). (c) Hide the "add image" `IconButton` (≈ 332) on Web. (d) In `allImagePaths` (≈ 108-114): when `isWebPlatform()`, assign `makerImages.toTypedArray()`. Without this, line ≈ 344 `if (allImagePaths.isNotEmpty())` skips the maker images gallery completely on Web (ISSUE-17). |
| **`ui/MakerForm.kt`** | Hide the rename icon (≈ 90) with `if (!isWebPlatform())`. |
| **`ui/ImageRenameDialog.kt`** | `FileSystem.SYSTEM` becomes `systemFileSystem` (≈ 55, 191). |
| **`ui/InfoScreen.kt`** | (a) ≈ 57: `java.util.Locale.getDefault().language.lowercase()` becomes `androidx.compose.ui.text.intl.Locale.current.language.lowercase()`. (b) ≈ 262–272, in `BackupTabContent`: wrap both `Button(onClick = onNavigateToSftpSetup)` (text `sftp_setup_guide_btn`, "SFTP Server Setup Guide") and the following `Spacer(modifier = Modifier.height(20.dp))` in `if (!isWebPlatform()) { ... }` so no empty 36dp gap stays before the divider on Web. Keep the Backup tab and its text on Web (decision D8, ISSUE-19). |
| **`service/ImportExportService.kt`** | `FileSystem.SYSTEM` becomes `systemFileSystem` (≈ 146, 160, 230, 241, 530, 673). Keep `import okio.FileSystem` (it is used as a type). |

Each edited file that now uses `systemFileSystem`, `isWebPlatform` or `userHomeDirectory` needs `import com.gepetto.toydb.utils.<name>`.

All `@Preview` / `@PreviewLightDark` imports (`androidx.compose.ui.tooling.preview.*`) stay unchanged. The new `libs.jetbrains.compose.ui.tooling.preview` dependency supplies them on wasm (Rev 4: `PreviewLightDark` is present in `ui-tooling-preview-wasm-js:1.12.1`).

Do **not** change `ui/DashboardScreen.kt`. Its `String.format("%.2f", …)` calls (≈ 115, 119, 202, 203) use a private common helper `String.Companion.format` (≈ 240). They are already KMP-safe.

### 4.3 Actuals

| Source set | File | Content |
| :--- | :--- | :--- |
| `desktopMain`, `androidMain` | `utils/PlatformSupport.kt` | `systemFileSystem = FileSystem.SYSTEM`; `userHomeDirectory() = System.getProperty("user.home")`; `isWebPlatform() = false`. |
| `wasmJsMain` | `utils/PlatformSupport.wasmJs.kt` | Empty `NoFileSystem` (all lookups return "not found", all writes throw `IOException`); `userHomeDirectory() = null`; `isWebPlatform() = true`. |
| `wasmJsMain` | `utils/ImageResolver.wasmJs.kt` | `resolveImageUri` and `resolveBitmapUri` return `null`; the two dialogs return `null`; `isDesktopPlatform() = false`; `rememberImagePicker` returns `{}`; `formatTimestamp` uses a JS `Date`. |
| `wasmJsMain` | `utils/KeepScreenOn.wasmJs.kt` | No-op. |
| `wasmJsMain` | `ui/PlatformScrollbar.wasmJs.kt` | No-op for `PlatformScrollbar` and `PlatformGridScrollbar`. |
| `wasmJsMain` | `service/ImportExportServiceWasm.kt` | `getCurrentDateString()` uses deterministic JS `Date` formatting. |
| `wasmJsMain` | `service/WebSftpService.kt` | Class `WebSftpService : SftpService` with `isSupported = false`. Every method returns `Result.failure(UnsupportedOperationException)`. |
| `wasmJsMain` | `database/SqlJs.wasmJs.kt`, `database/IndexedDb.wasmJs.kt`, `database/WasmDatabase.wasmJs.kt` | The web database, including `SqlJsException`. See 4.4. |
| `wasmJsMain` | `Main.kt` | Entry point. See 4.5. |
| `wasmJsMain` | `resources/index.html` | HTML shell. See 4.5. |

### 4.4 Web database (`WasmToyDatabase`)

Rules for the implementation (code in Appendix A):

1. **Kotlin/Wasm interop types.** `external` declarations may use only `JsAny`-based types, `String`, numbers and `Unit`. Use `JsArray<JsAny?>`, `JsString`, `JsNumber`, `Promise<...>`, `org.khronos.webgl.Uint8Array`. Do not use `Array<Any?>` or `Any?`. Opt in with `@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)`.
2. **Asynchronous open.** `createDatabase()` is synchronous, but sql.js start-up, `Res.readBytes` and the IndexedDB read are asynchronous. `WasmToyDatabase.open()` is a `suspend` function. `main()` calls it in a coroutine and calls `ComposeViewport` afterwards. The wasm `createDatabase` actual only throws.
3. **First run.** If IndexedDB has no snapshot, load `files/default_toydb.db` from Compose resources. Then run `checkUpgrade(database)`. Then `DELETE FROM toys` and `DELETE FROM app_settings WHERE key LIKE 'html_sync_imported_%'` (the same as the desktop `Main.kt` does on first install). Then save the snapshot at once.
4. **Existing snapshot.** Open it. Run `checkUpgrade`. If the snapshot cannot be opened, log the error and start from the default database. If IndexedDB itself fails (blocked, quota, private mode), log the error with `GcLog.e` and continue without a snapshot (Rev 4). The session then works in memory only.
5. **Saving.** Each `execute()` marks the database "dirty" and (re)starts a 1-second timer (debounce). When the timer ends, export the database (`db.export()`) and store it in IndexedDB (database `toydb_web`, store `kv`, key `database_snapshot`). Also save when the page becomes hidden (`visibilitychange`) and on `pagehide`. Do NOT save on every statement: one sync is about 2,000 `execute()` calls (1,649 toy inserts plus makers, categories and metadata). Each IndexedDB read or write closes its connection when it is done (Appendix A.8).
6. **Cursor case-insensitivity (ISSUE-18, preventive).** The JDBC cursor (Desktop) matches column names without case. `WasmSqlCursor.cell` does the same: `columns.indexOfFirst { it.equals(name, ignoreCase = true) }`. The repository reads names like `COUNT(*)`, `SUM(value)` and `MAX(ref_num)`. sql.js returns them as written, so they match today. The case-insensitive match keeps them correct if a query changes the spelling.
7. **Parameters.** When `bindArgs` is empty, pass `null` to `sql.js` (an empty array `[]` is truthy in JS and triggers `stmt.bind([])`). `null`, `Int`, `Long`, `Double`, `Boolean` (1 or 0) and `String` map to JS values. Anything else uses `toString()`.
8. **No explicit transactions** exist in `ToyRepository` or `HtmlSyncService` today. Do not add any in v1. Note: sql.js `export()` closes and opens the database again. This ends an open transaction. A later change that adds transactions (Phase 9) must make sure that `flush()` never runs while a transaction is open.
9. **Blocking.** Sync runs on the main thread on wasm (`ioDispatcher` is `Dispatchers.Default`). The UI can freeze for about one second during an import. This is accepted for v1.
10. **Error type (ISSUE-26).** On Kotlin/Wasm, an error that sql.js throws arrives as `kotlin.js.JsException`. `JsException` extends `Throwable`, not `Exception`. `ToyRepository` (23 `catch` blocks), `checkUpgrade` and `HtmlSyncService.saveMetadataSetting` catch only `Exception`. On Desktop, JDBC throws `SQLException` (an `Exception`), so the app logs the error and continues. `WasmToyDatabase.execute` and `query` MUST catch `Throwable` from sql.js and throw `SqlJsException` (a subclass of `Exception`) instead (Appendix A.9). Without this, an SQL error escapes the shared `catch` blocks. In a coroutine it can stop the UI, the same as ISSUE-09.

### 4.5 Entry point, HTML shell and sync behavior

**`Main.kt`** (Appendix A.10): plant the log tree with `GcLog.plant(GcLog.DebugTree())` (the same as Android `AppMainActivity`; ISSUE-27), set the Coil loader, open the database in a coroutine, initialize `gCsetImagesBaseUrl` from stored settings, set the initial tab title `kotlinx.browser.document.title = repository.getAppTitleSetting()` (Rev 4: `onAppTitleChanged` runs only when the user edits the title in Settings; Desktop sets its window title from the same setting at start), compute `needsInitialSync = database.toysCount() == 0` (decision D2), then call `ComposeViewport(viewportContainerId = "compose-App")` with `ToyDbNavigation(database, WebSftpService(), onAppTitleChanged = { title -> kotlinx.browser.document.title = title }, runStartupSync = needsInitialSync)`. Wrap the start sequence in `try/catch (e: Throwable)` and log with `GcLog.e(e, "WebMain: ...")` (Rev 4, Rev 6). `kotlinx.browser` resolves because `kotlinx-browser` is an API dependency of Compose `ui-wasm-js` (checked in Rev 4).

**Real signature**: `ToyDbNavigation(db: ToyDatabase, sftpService: SftpService, onAppTitleChanged: ((String) -> Unit)? = null, runStartupSync: Boolean = true, modifier: Modifier = Modifier)`. It creates its own `ToyRepository`.

**`resources/index.html`**: one file only. It uses `index.html` so the dev server needs no `devServer.js`. The shell has `#compose-App` and loads `composeApp.js` at the **end of `<body>`** (Rev 4, same as `LapCounter/.../gepettolapcounter.html`). Appendix A.11 has the text.

**Sync on Web (D2):**
- First load: `toys` is empty, so `runStartupSync = true`. If it fails (offline, CORS), the app stays alive and the collection is empty. The next load tries again (the table is still empty).
- Later loads: `runStartupSync = false`. No automatic sync.
- Manual sync: Settings, Web Synchronization, Save. This calls `HtmlSyncService.syncIfNewer`. It replaces all local data only when the server data is newer, its hash differs, or the local `toys` table is empty (see section 1). Requests send `Cache-Control: no-cache` (D13), so the browser cache cannot hide new server data.
- **Error handling (ISSUE-09).** Ktor's JS engine throws `JsError`, which is a `Throwable` but not an `Exception`. The existing `catch (e: Exception)` blocks do not catch it. Without a fix, an uncaught error in the first-load sync **blanks the whole app**. `HtmlSyncService.syncIfNewer` must convert any non-`Exception` `Throwable` into an `Exception` (code in Appendix A.3). The callers (`ToyDbNavigation`, `SettingsScreen`) then work without change. Errors from sql.js have the same problem everywhere else. `WasmToyDatabase` converts them (section 4.4, rule 10).

**Logging on Web (ISSUE-27).** `GcLog` writes nothing until a log tree is planted (`if (FOREST.isEmpty()) return` in the library). Today only Android plants a tree. On wasm, `GcLog.DebugTree` writes with `println`, which goes to the browser console. Use `GcLog.e(e, "Tag: message")` in new code. The existing shared code calls `GcLog.e(TAG, "message", e)`. `GcLog` has no tag parameter, so this form prints only the tag (for example `[ERROR/GcLogWeb] HtmlSyncService`). Do not change the existing calls in this plan (out of scope).

### 4.6 What the user sees on Web v1

| Area | Web v1 |
| :--- | :--- |
| Home, Dashboard, Explorer, Makers | Same as Desktop. |
| Info Screen | Same as Desktop, except the "SFTP Server Setup Guide" button on the Backup tab is hidden (D8). The Backup tab text stays. |
| Add, edit, delete toys, makers, categories | Works. Saved in the browser. |
| Toy and maker images | Shown from the server. Toys with a blank `picture` show the fallback image, as on Desktop (D10). If the server has no file for a name that is not blank (HTTP 404), Web shows an empty frame. Desktop shows the fallback image after its download fails. Accepted for v1 (ISSUE-30, Phase 9). |
| Save of edits | About 1 second after the last change. The save on tab close is best effort: edits from the last second before the tab closes can be lost. |
| Image upload, image file rename | Hidden. |
| Settings: App title, theme, categories, Web Synchronization | Shown. |
| Settings: data directory, SFTP, JSON import/export, HTML export | Hidden. |
| Back button of the browser | Does not navigate (`BackHandler` is a stub in the library). Known limit. |

### 4.7 Dev server, CORS and hosting

- **Dev test method (verified):** build with `./gradlew :composeApp:wasmJsBrowserDevelopmentExecutableDistribution`. Copy `ToyCollection/json/*.json` into `composeApp/build/dist/wasmJs/developmentExecutable/database/`. Serve that folder, for example `python3 -m http.server 8081`. Open `http://localhost:8081/index.html`. In Settings set the Base URL to `http://localhost:8081/database/` and press Save.
- **CORS.** From `localhost` the browser blocks `https://gepetto.club/database/...` (no `Access-Control-Allow-Origin` header). This is why Task 7 uses a local copy of the data. Optional: add a webpack dev-server proxy in `composeApp/webpack.config.d/devServer.js` (not tested): `config.devServer.proxy = [{ context: ['/database'], target: 'https://gepetto.club', changeOrigin: true }];` and use Base URL `http://localhost:8080/database/`.
- **Production:** host the content of `composeApp/build/dist/wasmJs/productionExecutable/` at `https://gepetto.club/database/web/`. The server MUST send `.wasm` files as `application/wasm`. Enable gzip or brotli (the total is about 17 MB uncompressed). The page and the data are on the same origin, so CORS is not needed.
- **Trailing slash (ISSUE-31).** All file references are relative: `composeApp.js` in `index.html`, the `.wasm` files, `composeResources/`, and the sql.js `locateFile` result. The page works only at `https://gepetto.club/database/web/` (with the slash). At `https://gepetto.club/database/web` the browser asks for the files in `/database/` and the page does not load. The server MUST redirect `/database/web` to `/database/web/`. Most servers do this by default for a real folder. Test it in Task 8.2.
- **Cache after a deploy.** `index.html` and `composeApp.js` keep the same names in each build. Serve them with `Cache-Control: no-cache`, so that browsers get the new version after a deploy.
- **Host name (D14).** Use only `https://gepetto.club`. The owner does not use `www`. The page and the data are on the same origin, so no redirect and no CORS headers are needed.
- **Base URL scheme.** The Base URL must be `https`. A page served over `https` cannot read `http` data (mixed content). The code default is `https://gepetto.club/database/` (`ToyRepository.DEFAULT_BASE_URL`). `TODO.txt` still mentions `http://valdetaro.com/database`: the code is the authority.
- **Browser support.** Kotlin/Wasm needs a recent browser with WebAssembly GC (Chrome and Edge 119+, Firefox 120+, Safari 18.2+).

---

## 5. Phase checklist

Run the gate that each phase names. All gates MUST pass. From Phase 4 on the gate is GATE (all three targets).

### Phase 0: Preflight
- [x] **0.1** `git status` in `ToyCollection/` is clean. Record the commit hash in the changelog.
- [x] **0.2** Run `./gradlew :composeApp:compileKotlinDesktop :composeApp:compileDebugKotlinAndroid`. Both pass (baseline).
- [x] **0.3** Confirm Node and a modern Chrome are available (Gradle installs its own Node for the build). Do not build the companion apps (not needed).

### Phase 1: Gradle and catalog (section 4.1)
- [x] **1.1** Edit `gradle/libs.versions.toml` (3 versions, 4 libraries, including `copyWebpackPlugin` and `kotlin-test`).
- [x] **1.2** Edit `composeApp/build.gradle.kts` (wasmJs target, move okhttp, preview dependency, new dependencies, `commonTest` dependency, `webVersionCode` in `generateCommonConfig` (D11)).
- [x] **1.3** Create `composeApp/webpack.config.d/sqljs.js`.
- [x] **1.4** Run `./gradlew kotlinWasmUpgradeYarnLock`. Check that `kotlin-js-store/wasm/yarn.lock` exists. Tell the owner to commit it.
- [x] **1.5** No gate in this phase. `commonMain` still imports OkHttp until Phase 2, so compile errors are expected. Go to Phase 2.

### Phase 2: Decouple `commonMain` (section 4.2)
- [x] **2.1** Create `utils/PlatformSupport.kt` (expects) and the desktop and Android actuals.
- [x] **2.2** Create `utils/JsonDateParser.kt`.
- [x] **2.3** Edit `HtmlSyncService.kt`.
- [x] **2.4** Edit `SyncImage.kt` (including `WebSyncImage`).
- [x] **2.5** Replace `FileSystem.SYSTEM` in: `SettingsScreen`, `ToyForm`, `ToyDetailScreen`, `MakerDetailScreen`, `ToyDbNavigation`, `ImageRenameDialog`, `ImportExportService`. Check with `grep -rn "FileSystem.SYSTEM" composeApp/src/commonMain`: no result.
- [x] **2.6** Replace `System.currentTimeMillis()` (3 files), `Dispatchers.IO` (2 places), `.format(...)` (SettingsScreen), `java.util.Locale` (InfoScreen). Check with `grep -rnE "(^|[^A-Za-z])System\.|java\.|javax\.|Dispatchers\.IO|engine\.okhttp|\.format\(" composeApp/src/commonMain`. Expected result: only the `DashboardScreen.kt` lines that call its private common `String.format` helper (about lines 115, 119, 202, 203). Do not change them (section 4.2 note). The Rev 3 pattern `System\.` also matched `FileSystem.` and `PlatformFileSystem.` (ISSUE-22).
- [x] **2.7** Add the `runStartupSync` parameter to `ToyDbNavigation`.
- [x] **2.8** Hide the Settings sections (`SftpSettingsCard`, `SftpSyncActions`, `ImportExportActions`) on Web. In each layout, use one `if (!isWebPlatform()) { ... }` block. It starts at the `Spacer` just after `BaseUrlSettingsCard` and ends after the `ImportExportActions(...)` call (section 4.2, `SettingsScreen` item (d)). The 2 `Spacer` lines come before `SftpSettingsCard` and before `ImportExportActions`, so they go inside the block. No empty gap stays. In `InfoScreen` (`BackupTabContent`) hide the "SFTP Server Setup Guide" button and its following `Spacer(20.dp)` inside `if (!isWebPlatform()) { ... }` so no empty 36dp gap stays; the Backup tab and its text stay (D8).
- [x] **2.8b** In `SettingsScreen`, after a successful manual web sync, reload the categories list next to the `onCategoriesChanged()` call (D9). Do this on all targets (no `isWebPlatform()` check).
- [x] **2.9** Hide the rename icons (`ToyForm`, `MakerForm`) and the three image-upload buttons on Web.
- [x] **2.9b** Update `allImagePaths` in `MakerDetailScreen.kt` and `ToyDetailScreen.kt` so image filenames are preserved directly on Web (ISSUE-17). Do not guess a filename for a toy with a blank `picture` (D10): skip blank names.
- [x] **2.10** Run `./gradlew :composeApp:compileKotlinDesktop :composeApp:compileDebugKotlinAndroid`. Pass.
- [x] **2.11** Do NOT run the Desktop headless mode (`--headless-import-export`). It is not a safe check (ISSUE-29). It opens the real Desktop database (`getAppDataDir("ToyDatabaseManager")/toydb.db`, on macOS `~/Library/Application Support/ToyDatabaseManager/toydb.db`). It deletes all toys and makers. It imports the JSON files from the `data_path` setting, or from `~/valdetaro/ToyCollection/ToyDb/json` (this folder does not exist). Then it writes over the JSON files. The Phase 6 tests (6.2b, 6.3) show that the changed date and hash code gives the same results on Desktop. Tick this box when you have read this note.

### Phase 3: Web platform actuals (section 4.3)
- [x] **3.1** Create `wasmJsMain/.../utils/PlatformSupport.wasmJs.kt`.
- [x] **3.2** Create `ImageResolver.wasmJs.kt`, `KeepScreenOn.wasmJs.kt`, `PlatformScrollbar.wasmJs.kt`, `ImportExportServiceWasm.kt`, `WebSftpService.kt`.
- [x] **3.3** No wasm gate yet: the `createDatabase` actual comes in Phase 4, so `compileKotlinWasmJs` reports a missing actual. Run `./gradlew :composeApp:compileKotlinDesktop :composeApp:compileDebugKotlinAndroid` only.

### Phase 4: Web database (section 4.4)
- [x] **4.1** Create `SqlJs.wasmJs.kt` (external declarations with nullable params for parameterless queries).
- [x] **4.2** Create `IndexedDb.wasmJs.kt`.
- [x] **4.3** Create `WasmDatabase.wasmJs.kt` (`WasmToyDatabase`, `SqlJsException` and the `sqlCall` wrapper in `execute` and `query` (section 4.4, rule 10), case-insensitive `WasmSqlCursor`, `createDatabase` actual).
- [x] **4.4** Run GATE. All three compile.

### Phase 5: Web shell and behavior (section 4.5)
- [x] **5.1** Create the real `wasmJsMain/kotlin/Main.kt` (Appendix A.10): `GcLog.plant(GcLog.DebugTree())` as the first line (ISSUE-27), initial tab title from `repository.getAppTitleSetting()`, tab title sync, base URL initialization, `try/catch (e: Throwable)` with `GcLog.e(e, ...)`. In `WasmToyDatabase.open()` a failed IndexedDB read logs an error and continues without a snapshot (Appendix A.9).
- [x] **5.2** Create `wasmJsMain/resources/index.html` (Appendix A.11). The `composeApp.js` script is at the end of `<body>`.
- [x] **5.3** Run GATE, then `./gradlew :composeApp:wasmJsBrowserDevelopmentExecutableDistribution`. Check that `composeApp/build/dist/wasmJs/developmentExecutable/` contains `index.html`, `composeApp.js` and `sql-wasm-browser.wasm`.
- [x] **5.4** Make sure `HtmlSyncService.syncIfNewer` has the `Throwable` normalization (Task 2.3). Test it: with the page on `localhost` and the default Base URL, the first-load sync fails with a CORS error, and the app still shows the Home screen.
- [x] **5.5** Check `SettingsScreen` on Web (Task 2.8): the three sections are hidden and no empty gap stays. Check Task 2.8b: the categories list reloads after a manual web sync (all targets, D9).
- [x] **5.6** Add the web notice string `web_local_data_notice` in all 6 `strings.xml` files:
  - **values/strings.xml**: `Web version: your changes are saved only in this browser. A manual sync replaces your local data when the server has newer data.`
  - **values-pt/strings.xml** (Brazilian Portuguese, the same as the other `values-pt` strings: "Salvar", "Arquivo", "você"): `Versão Web: suas alterações são salvas somente neste navegador. Uma sincronização manual substitui seus dados locais quando o servidor tiver dados mais recentes.`
  - **values-de/strings.xml**: `Web-Version: Ihre Änderungen werden nur in diesem Browser gespeichert. Eine manuelle Synchronisierung ersetzt Ihre lokalen Daten, wenn der Server neuere Daten hat.`
  - **values-es/strings.xml**: `Versión web: sus cambios se guardan solo en este navegador. Una sincronización manual reemplaza sus datos locales cuando el servidor tiene datos más recientes.`
  - **values-fr/strings.xml**: `Version Web : vos modifications sont enregistrées uniquement dans ce navigateur. Une synchronisation manuelle remplace vos données locales lorsque le serveur a des données plus récentes.`
  - **values-it/strings.xml**: `Versione Web: le modifiche vengono salvate solo in questo browser. Una sincronizzazione manuale sostituisce i dati locali quando il server dispone di dati più recenti.`
  Create a private composable `WebLocalDataNotice()` in `SettingsScreen.kt`. It shows the text with the theme colors (`sysForegroundColor` / `sysTextColor`, the same as the other Settings cards). Add previews for it (`@PreviewLightDark` and a landscape preview, inside `GcTheme {}`, rule for previews in `AGENTS.md`). Show it above `BaseUrlSettingsCard` (both layouts) only when `isWebPlatform()`.
- [x] **5.7** Run ASSEMBLE (section 0). All three targets build.

### Phase 6: Tests (rule 5)
- [x] **6.1** Add a `commonTest` source set with `implementation(libs.kotlin.test)` (catalog entry from Task 1.1; no direct version in Gradle files). The project has no tests today; `src/desktopTest` is empty. Keep test data files in a `testfiles` subdirectory (rule 15).
- [x] **6.2** `JsonDateParserTest`: `"October 4, 2026"` returns `1791072000000`; `"Oct 4, 2026"` returns the same; `"not a date"` returns `0`; `""` returns `0`; `"February 29, 2024"` returns `1709164800000`.
- [x] **6.2b** `JsonDateParserParityTest` in `desktopTest`: for every `date` value in `ToyCollection/json/*.json` (or a copy in `testfiles`), compare `JsonDateParser.parse` with the old code `SimpleDateFormat("MMMM d, yyyy", Locale.US)` in UTC. The results MUST be equal. Known difference: `SimpleDateFormat` accepts trailing text after the year, `JsonDateParser` returns `0`. If a data value has trailing text, record it in the test and in section 7.
- [x] **6.3** `HtmlSyncServiceHashTest`: `HtmlSyncService.calculateHash("abc")` equals `ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad`; the empty string gives `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`.
- [x] **6.4** Run `./gradlew :composeApp:desktopTest`.

### Phase 7: Browser verification (manual; use the method in 4.7)
- [ ] **7.1** First load: clear site data. Open the page. Home shows; the Dashboard shows 0 toys. The console shows the browser CORS message, then `[ERROR/GcLogWeb] HtmlSyncService` and `[ERROR/GcLogWeb] ToyDbNavigation` (the existing calls print only the tag, ISSUE-27). The console shows no "Uncaught" error.
- [ ] **7.2** Set the Base URL to the local data URL, press Save. The dialog says "Web synchronization completed". The Dashboard counts only toys with an empty `traded` field (section 2 note, ISSUE-28). For the current `ToyCollection/json/` folder, the Dashboard shows: slot 1,361, train 68, static 113, kit 17, misc 8 (total 1,567). The Explorer shows all toys, including traded toys (slot 1,443). If you serve different data, count the toys with an empty `traded` field.
- [ ] **7.2b** Check D13 (ISSUE-20): in the network tab the JSON requests send `Cache-Control: no-cache`. Change one value in a served JSON file and its date, press Save. The change shows in the app.
- [ ] **7.3** Reload. The Dashboard shows the same counts. Check that no sync ran (network tab).
- [ ] **7.4** Add a toy, edit a toy, rename a maker (with the confirmation dialog), add and delete a category. Wait 2 seconds. Reload. All changes are still there.
- [ ] **7.5** Press Save again with the same data. The dialog says there are no updates. Local edits are still there.
- [ ] **7.6** Settings shows no SFTP, no import/export and no data-directory section, and no empty gaps. The web notice shows. The image upload and rename buttons are hidden in the toy and maker screens. Info: the Backup tab shows, the "SFTP Server Setup Guide" button is hidden (D8). The browser tab title is the app title from Settings at start.
- [ ] **7.7** Explorer: search and filter work. Open a toy: the full-screen image popup opens and pages through images. Open a maker: the maker images gallery row is displayed and rendered. Open a toy with a blank `picture` (for example a slot car with `hasPicture = 'n'`): the fallback image shows and the network tab has no image request for it (D10). A name that is not blank but has no file on the server shows an empty frame (accepted, ISSUE-30).
- [ ] **7.8** With the real server (`https://gepetto.club/database/`, same origin, or a CORS-enabled server): images load. Record the result.
- [ ] **7.9** Check a narrow (mobile) viewport and light and dark themes.
- [ ] **7.10** Open the app in Firefox and Safari. Record any problem in section 7.
- [ ] **7.11** Measure: time to first screen, and the time of a full sync, on a normal laptop. Record both.

### Phase 8: Production build and hosting notes
- [x] **8.1** Run `./gradlew :composeApp:wasmJsBrowserDistribution`. Check `composeApp/build/dist/wasmJs/productionExecutable/` (index.html, composeApp.js, two `.wasm` files, `sql-wasm-browser.wasm`, `composeResources/`).
- [ ] **8.2** Serve that folder from a sub-path (for example `/database/web/`) and test that it loads. Also open the URL without the trailing slash (`/database/web`). The server must redirect it to `/database/web/` (ISSUE-31). Record the result of each URL.
- [x] **8.3** Write the deployment steps in `README.md` (MIME type for `.wasm`, compression, folder `https://gepetto.club/database/web/`, redirect from `/database/web` to `/database/web/`, `Cache-Control: no-cache` for `index.html` and `composeApp.js`, host name `gepetto.club` only (D14)). Also change the README line `* **Targets**: Desktop (macOS, Windows).` to `* **Targets**: Desktop (macOS, Windows), Android, Web.`. Do not upload anything: the owner does it.
- [x] **8.4** Update these documents:
  - `HOW_IT_WORKS.md` (targets, source tree, Web limits).
  - `.agents/TODO.txt` (tick the web line).
  - `.docs/published_versions.md`: do NOT change it. The file has no dates. In LapCounter and RaceDirector, the Web section holds only the plain versionCode (for example `283`). After the deploy, the owner replaces "Not available yet" with the versionCode (`232`). Tell the owner.
  - `~/valdetaro/.agents/AGENTS.md` (workspace file, D12): add Web to the Toy Collection targets line (about line 29).
- [x] **8.5** Run ASSEMBLE (section 0). All three targets build.

### Phase 9: Backlog (needs the owner's approval before work)
- Download of the JSON files from Web (export) and upload (import).
- Image upload (store image bytes in IndexedDB).
- Button "Reset browser data" (delete the IndexedDB snapshot).
- Message on the empty collection when the first-load sync fails.
- Visible start-up error message when `Main.kt` catches an error (today it only logs, ISSUE-25).
- Hide the whole Info Backup tab on Web (D8 follow-up).
- Guard against two tabs that write the same snapshot.
- Loading indicator and browser-support notice in `index.html`.
- Wrap the import in one transaction (also helps Desktop and Android). On Web, `flush()` must not run while the transaction is open, because sql.js `export()` ends it (section 4.4, rule 8).
- Run the sync in a Web Worker so the UI does not freeze.
- Show the fallback image on Web when the server has no file for an image name (ISSUE-30).

---

## 6. Changelog

| Date | Rev | Author / Agent | Changes |
| :--- | :---: | :--- | :--- |
| 2026-10-06 | 1 | Antigravity | First version of the plan. |
| 2026-10-06 | 2 | Claude (review) | Reviewed against code and library. Added owner decisions D1 and D2; design decisions D3-D7; list of `commonMain` breaks; webpack and npm steps for sql.js; Kotlin/Wasm interop rules; verified code (Appendix A); tests; browser verification list; hosting notes. Corrected `ToyDbNavigation` signature; image URL strategy; removed duplicate `createPlatformHttpClient`. |
| 2026-10-06 | 3 | Antigravity (review) | Reviewed plan against commit `1891463` and full codebase. **Fixed:** (1) `MakerDetailScreen` and `ToyDetailScreen` image galleries on Web (`allImagePaths` direct filename assignment, ISSUE-17). (2) `WasmSqlCursor` case-sensitive column lookup bug on SQL aggregate functions (`COUNT(*)`, `SUM(value)`, `MAX(ref_num)`, ISSUE-18). (3) Hide SFTP setup guide button in `InfoScreen` on Web (ISSUE-19). (4) Safe `null` parameter passing in `SqlJsDatabase.exec`/`run` for parameterless statements. (5) Full multilingual translations for `web_local_data_notice` in all 6 languages (Task 5.6). (6) Browser tab title synchronization (`document.title`) and `gCsetImagesBaseUrl` in `Main.kt`. (7) Sandbox bypass guidance for `./gradlew` daemon loopback sockets. |
| 2026-10-06 | 4 | Claude (review) | Reviewed against `HEAD b54abfe` (source same as `1891463`). **Owner decisions:** D8 (Info: hide only the SFTP button), D9 (reload categories after manual sync, all targets), D10 (no image-name guess for blank `picture`), D11 (`webVersionCode = vCode * 10 + 6`), D12 (update workspace `AGENTS.md`). **Design decision:** D13 (`Cache-Control: no-cache` on sync requests, ISSUE-20). **Added:** ASSEMBLE command (rule 9) and Tasks 5.7, 8.5; catalog entries `copyWebpackPlugin` and `kotlin-test` (no versions in Gradle files); Task 2.8b; Task 6.2b date parity test; Task 7.2b; initial tab title and start-up `try/catch` in `Main.kt` (ISSUE-24, ISSUE-25); IndexedDB read failure fallback (A.9); `www` and apex origin note (ISSUE-21), closed by D14 (`gepetto.club` only); document updates in 8.3 and 8.4. **Corrected:** 4.2 line references; Info button text; Task 2.6 grep (ISSUE-22); `WebSyncImage` blank-name handling (ISSUE-23); spacers in hidden Settings sections; `index.html` script position; exact Feb 29 2024 value in 6.2; data counts note (1,649 toys). |
| 2026-10-06 | 5 | Antigravity (review) | Reviewed against `HEAD 161b028`. **Fixed:** (1) In `InfoScreen.kt` (`BackupTabContent`), wrap trailing `Spacer(20.dp)` with the SFTP button inside `if (!isWebPlatform()) { ... }` so no empty 36dp gap stays before the divider. (2) Defensive `config.plugins = config.plugins || []` initialization in `webpack.config.d/sqljs.js`. (3) Task 2.11 note on desktop mock data path context. |
| 2026-10-06 | 6 | Claude (review) | Reviewed against `HEAD 328449f`. Source and gepetto-utils 2.1.2 are unchanged since the baseline. All 4.2 line numbers match. **Added:** `SqlJsException` and the `sqlCall` wrapper in `WasmToyDatabase` (4.4 rule 10, A.9, ISSUE-26); `GcLog.plant(GcLog.DebugTree())` in `Main.kt` and the `GcLog.e(e, "...")` form in new code (4.5, A.9, A.10, ISSUE-27); IndexedDB connections close after each call (A.8); `export()` transaction note (4.4 rule 8, Phase 9); best-effort save on tab close (1, 4.6); trailing-slash redirect and cache headers (4.7, 8.2, 8.3, ISSUE-31); missing-image difference (4.6, 7.7, Phase 9, ISSUE-30). **Corrected:** Dashboard counts exclude traded toys, so 1,567 is correct (section 2, Task 7.2, ISSUE-28); Task 2.11 replaced: headless mode changes the real Desktop database and the Rev 5 path note was wrong (ISSUE-29); Task 2.8 and 4.2 (d): one `if` block per layout, the spacers come before the hidden cards; Task 5.6 Portuguese text is now Brazilian Portuguese; Task 8.4: do not change `published_versions.md`; ISSUE-18 is preventive (only JDBC ignores case); Task 7.1 expected console output; tool-neutral sandbox note. |
| 2026-10-06 | 7 | Antigravity (review & exec) | Reviewed against `HEAD f2195ef`. Verified all line numbers, method signatures, symbols, and dependencies across all source sets. Began Phase 0 preflight and implementation. |

---

## 7. Issue tracker

| ID | Date | Component | Symptom | Root cause | Status / resolution |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **ISSUE-01** | 2026-10-06 | `build.gradle.kts` | `libs.ktor.client.okhttp` in `commonMain` fails on wasm. | OkHttp is JVM-only. | **Open.** Fix: Task 1.2. Verified by prototype. |
| **ISSUE-02** | 2026-10-06 | `database` | No `ToyDatabase` for wasm. | Desktop uses JDBC, Android uses the framework SQLite. | **Open.** Fix: Phase 4 (D3). Verified by prototype. |
| **ISSUE-03** | 2026-10-06 | `commonMain` | `okio.FileSystem.SYSTEM`, `System.getProperty`, `System.currentTimeMillis`, `Dispatchers.IO`, `String.format` do not exist on wasm. | `commonMain` compiled only because both targets are JVM. 10 existing files fail on wasm (Rev 4 count): `HtmlSyncService`, `ImportExportService`, `SyncImage`, `SettingsScreen`, `ToyDbNavigation`, `ToyForm`, `ToyDetailScreen`, `MakerDetailScreen`, `ImageRenameDialog`, `InfoScreen`. | **Open.** Fix: Phase 2 (table in 4.2). |
| **ISSUE-04** | 2026-10-06 | `HtmlSyncService.kt` | Uses `SimpleDateFormat`, `TimeZone`, `MessageDigest`, `"%02x".format`. | Java classes in `commonMain`. | **Open.** Fix: Task 2.2 and 2.3 (D6). Verified by prototype. |
| **ISSUE-05** | 2026-10-06 | `SftpService` | SFTP needs raw TCP sockets. | Browser sandbox. | **Open.** Fix: `WebSftpService` stub and hidden UI (D1). |
| **ISSUE-06** | 2026-10-06 | `ImportExportService`, UI | Okio has no system file system on wasm. | No disk in the browser. | **Open.** Fix: `systemFileSystem` stub (Task 2.5, 3.1). Import/export is out of scope on Web (D1). |
| **ISSUE-07** | 2026-10-06 | `build.gradle.kts` | "KMP Dependencies Resolution Failure": `androidx.compose.ui:ui-tooling-preview` has no wasm variant. | `libs.androidx.ui.tooling.preview` is in `commonMain`. | **Open.** Fix: Task 1.2 (use `org.jetbrains.compose.ui:ui-tooling-preview`). |
| **ISSUE-08** | 2026-10-06 | `SettingsScreen.kt` | `Unresolved reference 'IO'`. | `Dispatchers.IO` does not exist on wasm. Use `club.gepetto.utils.ioDispatcher`. | **Open.** Fix: Task 2.6. |
| **ISSUE-09** | 2026-10-06 | `HtmlSyncService`, `ToyDbNavigation` | A failed first-load sync (CORS or offline) **blanks the whole app**. Console: `Uncaught Error: Fail to fetch`. | Ktor JS throws `JsError`, a `Throwable` that is not an `Exception`. The `catch (e: Exception)` blocks miss it. | **Open.** Fix: Task 2.3 and 5.4 (normalize in `syncIfNewer`). Verified by prototype: the app stays alive. |
| **ISSUE-10** | 2026-10-06 | Dev setup | From `localhost`, requests to `https://gepetto.club/database/...` fail: "No 'Access-Control-Allow-Origin' header". | Cross-origin request without CORS headers. | **Open (dev only).** Fix: use local data (4.7). Production uses the same origin (D7). |
| **ISSUE-11** | 2026-10-06 | Gradle | After adding npm dependencies: `Lock file was changed. Run the kotlinWasmUpgradeYarnLock task`. | Kotlin/Wasm keeps `kotlin-js-store/wasm/yarn.lock`. | **Open.** Fix: Task 1.4. Commit the lock file. |
| **ISSUE-12** | 2026-10-06 | webpack | The sql.js `.wasm` file is not in the output. | sql.js loads it by URL at run time; webpack does not see it. | **Open.** Fix: Task 1.3 (`CopyWebpackPlugin`). Verified by prototype. |
| **ISSUE-13** | 2026-10-06 | `SyncImage`, `GcImage` | Rev 1 would return full URLs from `resolveImageUri`. `GcImage` then builds `base + "https://..."`. | `PlatformFile.exists()` is always `false` on wasm, so `GcImage` prepends the base URL by itself. | **Open.** Fix: `WebSyncImage` (Task 2.4, D4). Verified for URL building. |
| **ISSUE-14** | 2026-10-06 | `ToyDbNavigation` | The startup sync would replace local edits on every load. | `syncIfNewer` imports again whenever the server data is newer. | **Open.** Fix: `runStartupSync` (Task 2.7, D2). |
| **ISSUE-15** | 2026-10-06 | `WasmToyDatabase` | Two tabs can overwrite each other's snapshot. | Last writer wins in IndexedDB. | **Open (accepted risk).** Backlog item. |
| **ISSUE-16** | 2026-10-06 | `WasmSqlCursor` | A REAL value `5.0` reads as `"5"` with `getString`. | JS numbers do not keep integer or real type. | **Open (accepted).** Check in Phase 7 that prices show correctly. |
| **ISSUE-17** | 2026-10-06 | `MakerDetailScreen`, `ToyDetailScreen` | Maker images row is hidden on Web; toy secondary images cannot be paged in full-screen popup. | `allImagePaths` resolved via `resolveBitmapUri()` which returns `null` on Web. `MakerDetailScreen` line ≈ 344 checks `if (allImagePaths.isNotEmpty())`. | **Open.** Fix: Task 2.9b (assign filenames directly to `allImagePaths` on Web; skip blank names, no guess, D10). |
| **ISSUE-18** | 2026-10-06 | `WasmSqlCursor` | Preventive (Rev 6). No failure today: sql.js returns aggregate column names (`COUNT(*)`, `SUM(value)`, `MAX(ref_num)`) as written, so a case-sensitive lookup finds them. A query with a different spelling would return `null` (0 counts). | `columns.indexOf(name)` is case-sensitive. The JDBC cursor (Desktop) finds columns without case. | **Open.** Fix: Task 4.3 (case-insensitive column matching in `WasmSqlCursor.cell`, for parity with JDBC). |
| **ISSUE-19** | 2026-10-06 | `InfoScreen.kt` | The Info Backup tab shows the "SFTP Server Setup Guide" button on Web. It opens a feature that Web does not support. | Commit `1891463` added the tabbed Info screen with this button. | **Open.** Fix: Task 2.8 (hide only the button with `if (!isWebPlatform())`; the tab stays, D8). |
| **ISSUE-20** | 2026-10-06 | `HtmlSyncService` | A manual sync can read old JSON files from the browser HTTP cache and report "no updates". | The browser caches `GET` responses. Desktop and Android (OkHttp without cache) do not. | **Open.** Fix: D13 (`Cache-Control: no-cache` request header, Task 2.3, check in Task 7.2b). |
| **ISSUE-21** | 2026-10-06 | Hosting | A page on `https://www.gepetto.club` could not read data from `https://gepetto.club/database/`. | `www` and `gepetto.club` are different origins (CORS). | **Closed (D14).** The owner uses only `https://gepetto.club`. No `www` support is needed. |
| **ISSUE-22** | 2026-10-06 | Plan, Task 2.6 | The Rev 3 check `grep "System\."` never gives "no result". | The pattern also matches `FileSystem.` and `PlatformFileSystem.`. It also missed `javax.`, `engine.okhttp` and `.format(`. | **Fixed in plan (Rev 4).** New pattern and expected result in Task 2.6. |
| **ISSUE-23** | 2026-10-06 | Plan, `WebSyncImage` | The Rev 3 code would request a guessed `"$prefix$refNum.jpg"` for toys with a blank `picture` (116 slot cars, all `hasPicture = 'n'`), which gives 404 requests. | The guess does not match Desktop, which shows the fallback image. | **Fixed in plan (Rev 4).** D10: no guess; Appendix A.4 and Task 2.9b. Check in Task 7.7. |
| **ISSUE-24** | 2026-10-06 | Plan, `Main.kt` | The browser tab title is not the app title at start. | `onAppTitleChanged` runs only when the user edits the title in Settings. | **Fixed in plan (Rev 4).** `Main.kt` sets `document.title` at start (Appendix A.10). |
| **ISSUE-25** | 2026-10-06 | `Main.kt`, `WasmToyDatabase` | An error in the start sequence (for example IndexedDB blocked in private mode) gives a blank page. | Uncaught `Throwable` before `ComposeViewport`. | **Mitigated in plan (Rev 4).** IndexedDB read failure continues without a snapshot (A.9); start sequence in `try/catch` with `GcLog.e` (A.10). A visible message is in the Phase 9 backlog. The log line shows in the console only with the ISSUE-27 fix. |
| **ISSUE-26** | 2026-10-06 | `WasmToyDatabase` | An SQL error on Web escapes the `catch (e: Exception)` blocks in `ToyRepository`, `checkUpgrade` and `HtmlSyncService.saveMetadataSetting`. In a coroutine it can stop the UI. On Desktop the same error is logged and the app continues. | Kotlin/Wasm gives JS errors as `kotlin.js.JsException`, which extends `Throwable`, not `Exception` (checked in the Kotlin 2.4.20 wasm stdlib). | **Open.** Fix: Task 4.3 (`SqlJsException` and `sqlCall`, section 4.4 rule 10, Appendix A.9). |
| **ISSUE-27** | 2026-10-06 | `Main.kt`, `GcLog` | On Web, no `GcLog` output shows in the browser console. Task 7.1 cannot pass. | `GcLog` returns at once when no tree is planted (`if (FOREST.isEmpty()) return`). Only Android plants a tree. Also, `GcLog` has no tag parameter: `GcLog.e(TAG, "message", e)` uses `TAG` as the format string and prints only the tag. | **Open.** Fix: Task 5.1 (`GcLog.plant(GcLog.DebugTree())` in `Main.kt`); new code uses `GcLog.e(e, "Tag: message")` (A.9, A.10). The existing tag-first calls stay (out of scope; separate task). |
| **ISSUE-28** | 2026-10-06 | Plan, Task 7.2 | The Rev 4 plan expected the Dashboard to show the toy count of the JSON files (1,649). It shows 1,567. | `getDashboardStats` counts only toys with an empty `traded` field. 82 slot cars are traded. | **Fixed in plan (Rev 6).** Section 2 note and Task 7.2 give the expected values per category. |
| **ISSUE-29** | 2026-10-06 | Plan, Task 2.11 | Rev 5 said Desktop `Main.kt` reads `File("ToyDb/json")`. It does not. The headless command deletes all toys and makers in the real Desktop database and writes over the JSON files. | `runHeadlessImportExport` uses the real database in `getAppDataDir("ToyDatabaseManager")` and the `data_path` setting (fallback `~/valdetaro/ToyCollection/ToyDb/json`, which does not exist). | **Fixed in plan (Rev 6).** Task 2.11 now says: do not run it. Phase 6 tests cover Desktop parity. |
| **ISSUE-30** | 2026-10-06 | `WebSyncImage`, `GcImage` | On Web, an image name that is not blank but has no file on the server shows an empty frame. Desktop shows the fallback image. | Desktop `SyncImage` shows `fallbackBitmap` after a failed download. On Web, `GcImage` loads the URL with Coil and has no fallback for a `PlatformBitmap`. | **Open (accepted for v1).** Check in Task 7.7. Phase 9 backlog item. |
| **ISSUE-31** | 2026-10-06 | Hosting | At `https://gepetto.club/database/web` (no trailing slash) the page does not load. | All file references are relative, so the browser asks for them in `/database/`. | **Open (hosting).** The server redirects to `/database/web/` (section 4.7). Check in Task 8.2. |

---

## 8. Appendix A: verified code

These files are verified for Kotlin 2.4.20 and Compose Multiplatform 1.12.1. Keep the behavior. You may change names and style to fit the project.

Rev 6 changed A.8 (connections close), A.9 (`SqlJsException`, `sqlCall`, log calls) and A.10 (`GcLog.plant`, log call). The Rev 6 changes were checked against the library and stdlib sources, but they were not compiled. If one of them does not compile, log it in section 7 and fix it.

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

### A.3 `commonMain/.../service/HtmlSyncService.kt` (changed parts)

```kotlin
import club.gepetto.composeutils.createPlatformHttpClient
import com.gepetto.toydb.utils.JsonDateParser
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import org.kotlincrypto.hash.sha2.SHA256
// remove: import io.ktor.client.engine.okhttp.*

    // D13: the browser must not answer the sync requests from its HTTP cache (ISSUE-20).
    private val client = createPlatformHttpClient {
        defaultRequest { header(HttpHeaders.CacheControl, "no-cache") }
    }

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
    // D10: no guessed file name. A blank name shows the fallback image, as on Desktop.
    val name = if (isMainImage) {
        toy?.picture?.trim()?.takeIf { it.isNotEmpty() }
    } else {
        filename?.trim()?.takeIf { it.isNotEmpty() }
    }
    val urlForImages = if (baseUrl.isNullOrBlank() || name == null) null else baseUrl.trimEnd('/') + "/"
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

@JsFun("""() => {
    const d = new Date();
    const months = ["January","February","March","April","May","June","July","August","September","October","November","December"];
    return months[d.getMonth()] + ' ' + d.getDate() + ', ' + d.getFullYear();
}""")
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
    fun run(sql: String, params: JsArray<JsAny?>?)
    fun exec(sql: String, params: JsArray<JsAny?>?): JsArray<SqlJsQueryResult>
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

### A.8 `wasmJsMain/.../database/IndexedDb.wasmJs.kt`

```kotlin
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.gepetto.toydb.database

import kotlin.js.JsAny
import kotlin.js.Promise
import org.khronos.webgl.Uint8Array

// Rev 6: each call closes its connection when the transaction ends.
@JsFun("""(key) => new Promise((resolve, reject) => {
    const open = indexedDB.open('toydb_web', 1);
    open.onupgradeneeded = () => open.result.createObjectStore('kv');
    open.onerror = () => reject(open.error);
    open.onsuccess = () => {
        const db = open.result;
        const req = db.transaction('kv', 'readonly').objectStore('kv').get(key);
        req.onsuccess = () => { db.close(); resolve(req.result === undefined ? null : req.result); };
        req.onerror = () => { db.close(); reject(req.error); };
    };
})""")
external fun idbGet(key: String): Promise<Uint8Array?>

@JsFun("""(key, value) => new Promise((resolve, reject) => {
    const open = indexedDB.open('toydb_web', 1);
    open.onupgradeneeded = () => open.result.createObjectStore('kv');
    open.onerror = () => reject(open.error);
    open.onsuccess = () => {
        const db = open.result;
        const tx = db.transaction('kv', 'readwrite');
        tx.objectStore('kv').put(value, key);
        tx.oncomplete = () => { db.close(); resolve(null); };
        tx.onerror = () => { db.close(); reject(tx.error); };
        tx.onabort = () => { db.close(); reject(tx.error); };
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

/**
 * ISSUE-26 (Rev 6): sql.js errors reach Kotlin as JsException, which is a Throwable but not an Exception.
 * The shared code (ToyRepository, checkUpgrade) catches Exception only, the same as JDBC's SQLException on Desktop.
 */
class SqlJsException(message: String?, cause: Throwable?) : Exception(message, cause)

private inline fun <T> sqlCall(sql: String, block: () -> T): T = try {
    block()
} catch (e: Throwable) {
    throw if (e is Exception) e else SqlJsException("${e.message} [${sql.take(80)}]", e)
}

class WasmSqlCursor(private val columns: List<String>, private val rows: List<List<Any?>>) : SqlCursor {
    private var index = -1
    override fun next(): Boolean = ++index < rows.size

    private fun cell(name: String): Any? {
        val col = columns.indexOfFirst { it.equals(name, ignoreCase = true) }
        return if (col < 0) {
            if (columns.size == 1 && rows[index].isNotEmpty()) rows[index][0] else null
        } else rows[index][col]
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

    private fun List<Any?>.toJsParams(): JsArray<JsAny?>? {
        if (isEmpty()) return null
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
        sqlCall(sql) { db.run(sql, bindArgs.toJsParams()) }
        markDirty()
    }

    override fun query(sql: String, bindArgs: List<String>): SqlCursor {
        val results = sqlCall(sql) { db.exec(sql, bindArgs.toJsParams()) }
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
            GcLog.e(e, "$TAG_WASM: failed to persist database snapshot: ${e.message}")
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
            // ISSUE-25: IndexedDB can be blocked (for example private mode). Continue without a snapshot.
            val snapshot = try {
                idbGet(SNAPSHOT_KEY).await<org.khronos.webgl.Uint8Array?>()
            } catch (e: Throwable) {
                GcLog.e(e, "$TAG_WASM: cannot read the stored snapshot, starting from default database: ${e.message}")
                null
            }
            if (snapshot != null) {
                try { raw = newSqlJsDatabase(sql, snapshot) } catch (e: Throwable) {
                    GcLog.e(e, "$TAG_WASM: stored snapshot is unreadable, starting from default database: ${e.message}")
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

### A.10 `wasmJsMain/kotlin/Main.kt`

```kotlin
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import club.gepetto.GcLog
import club.gepetto.composeutils.GcTheme
import club.gepetto.composeutils.image.gCsetImagesBaseUrl
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.gepetto.toydb.database.ToyRepository
import com.gepetto.toydb.database.WasmToyDatabase
import com.gepetto.toydb.service.WebSftpService
import com.gepetto.toydb.ui.ToyDbNavigation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

private const val TAG_MAIN = "WebMain"

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // ISSUE-27 (Rev 6): GcLog writes nothing until a tree is planted. On wasm, DebugTree prints to the browser console.
    GcLog.plant(GcLog.DebugTree())
    SingletonImageLoader.setSafe { ImageLoader.Builder(PlatformContext.INSTANCE).build() }
    MainScope().launch {
        try {
            val database = WasmToyDatabase.open()
            val repository = ToyRepository(database)
            val baseUrl = repository.getBaseUrlSetting()
            if (!baseUrl.isNullOrBlank()) {
                gCsetImagesBaseUrl(baseUrl.trimEnd('/') + "/")
            }

            // Initial tab title (ISSUE-24). onAppTitleChanged runs only when the user edits the title.
            kotlinx.browser.document.title = repository.getAppTitleSetting()

            // Startup sync runs only on first load (empty toys table). Later syncs are manual.
            val needsInitialSync = database.toysCount() == 0

            ComposeViewport(viewportContainerId = "compose-App") {
                val sftpService = remember { WebSftpService() }
                GcTheme {
                    ToyDbNavigation(
                        db = database,
                        sftpService = sftpService,
                        onAppTitleChanged = { title ->
                            kotlinx.browser.document.title = title
                        },
                        runStartupSync = needsInitialSync
                    )
                }
            }
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            // ISSUE-25: log the start-up error. A visible message is a Phase 9 backlog item.
            // ISSUE-27: GcLog has no tag parameter, so put the tag in the message and pass the throwable first.
            GcLog.e(e, "$TAG_MAIN: web start-up failed: ${e.message}")
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
</head>
<body>
    <div id="compose-App"></div>
    <script type="application/javascript" src="composeApp.js"></script>
</body>
</html>
```
