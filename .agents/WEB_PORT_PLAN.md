# Web (wasmJs) Port Plan: Gepetto Toy Database Manager (Living Document)

> **Document Status**: Living Roadmap & Technical Specification  
> **Target Application**: Toy Database Manager / Toy Collection (`/Users/luizvaldetaro/valdetaro/ToyCollection`)  
> **Workspace**: `/Users/luizvaldetaro/valdetaro`  
> **Document Location**: `.agents/WEB_PORT_PLAN.md`  
> **Current Phase**: Phase 0 (Planning & Architectural Alignment)  
> **Last Updated**: 2026-10-06 (Rev 1 — Initial Comprehensive Living Roadmap)  

---

## Table of Contents
1. [Executive Summary & Background](#1-executive-summary--background)
2. [Comparative Architecture: How the Companion Apps Do Web](#2-comparative-architecture-how-the-companion-apps-do-web)
   - 2.1 [Toy Collection Legacy (`/ToyCollectionLegacy`)](#21-toy-collection-legacy-toycollectionlegacy)
   - 2.2 [Lap Counter (`/LapCounter`)](#22-lap-counter-lapcounter)
   - 2.3 [Race Director (`/RaceDirector`)](#23-race-director-racedirector)
3. [Toy Collection Architectural Challenges & Web Strategy](#3-toy-collection-architectural-challenges--web-strategy)
   - 3.1 [Challenge 1: Relational SQLite Persistence on the Web](#31-challenge-1-relational-sqlite-persistence-on-the-web)
   - 3.2 [Challenge 2: Decoupling Platform Networking (Ktor OkHttp vs JS)](#32-challenge-2-decoupling-platform-networking-ktor-okhttp-vs-js)
   - 3.3 [Challenge 3: Filesystem Independence & Image Resolution](#33-challenge-3-filesystem-independence--image-resolution)
   - 3.4 [Challenge 4: Sandboxed Export & SFTP Service Demarcation](#34-challenge-4-sandboxed-export--sftp-service-demarcation)
   - 3.5 [Challenge 5: Platform Crypto and Date Formatting](#35-challenge-5-platform-crypto-and-date-formatting)
4. [Detailed Technical Specification by Module](#4-detailed-technical-specification-by-module)
   - 4.1 [Toolchain, Gradle & Version Catalog (`libs.versions.toml`)](#41-toolchain-gradle--version-catalog-libsversionstoml)
   - 4.2 [Decoupling `commonMain` Platform Invocations](#42-decoupling-commonmain-platform-invocations)
   - 4.3 [Web Database Engine (`WasmDatabase.kt` / `sql.js` + IndexedDB)](#43-web-database-engine-wasmdatabasekt--sqljs--indexeddb)
   - 4.4 [Platform Expect/Actuals for `wasmJsMain`](#44-platform-expectactuals-for-wasmjsmain)
   - 4.5 [Web Shell, Host HTML, and Entry Point (`Main.kt`)](#45-web-shell-host-html-and-entry-point-mainkt)
5. [Phase-by-Phase Actionable Execution Checklist](#5-phase-by-phase-actionable-execution-checklist)
   - [Phase 0: Environment & Prerequisite Audit](#phase-0-environment--prerequisite-audit)
   - [Phase 1: Version Catalog & Gradle Target Configuration](#phase-1-version-catalog--gradle-target-configuration)
   - [Phase 2: Platform Decoupling in commonMain](#phase-2-platform-decoupling-in-commonmain)
   - [Phase 3: Web Database Implementation (Wasm SQLite + IndexedDB)](#phase-3-web-database-implementation-wasm-sqlite--indexeddb)
   - [Phase 4: Implement wasmJsMain Platform Actuals](#phase-4-implement-wasmjsmain-platform-actuals)
   - [Phase 5: Web UI Shell, HTML Host & Main Entry Point](#phase-5-web-ui-shell-html-host--main-entry-point)
   - [Phase 6: Multiplatform Compilation, Verification & Browser Testing](#phase-6-multiplatform-compilation-verification--browser-testing)
   - [Phase 7: Packaging, Hosting & Deployment](#phase-7-packaging-hosting--deployment)
6. [Living Document Changelog](#6-living-document-changelog)
7. [Bug, Blocker & Issue Tracker](#7-bug-blocker--issue-tracker)

---

## 1. Executive Summary & Background

**Gepetto's Toy Database Manager (Toy Collection)** is a Kotlin Multiplatform (KMP) application built with Compose Multiplatform. It maintains, updates, catalogs, exports, and synchronizes collector toy collections across five standard categories (Slot Cars, Model Trains, Static Models, Model Kits, and Miscellaneous) and user-defined custom categories.

Currently, the application targets:
- **Desktop (JVM 21)**: macOS (DMG for Intel and Apple Silicon) and Windows (MSI).
- **Android**: Phones and tablets (minSdk 24, compileSdk 37).

The companion workspace applications—**Toy Collection Legacy**, **Lap Counter**, and **Race Director**—all support WebAssembly (`wasmJs`) deployment. This document establishes the exact roadmap for equipping **Toy Collection** with a modern, fully functional Web version running in standard web browsers.

---

## 2. Comparative Architecture: How the Companion Apps Do Web

Before designing the architecture for Toy Collection, the three existing web implementations in `~/valdetaro` were analyzed:

### 2.1 Toy Collection Legacy (`/ToyCollectionLegacy`)
* **Role**: Public collection viewer and catalog browser.
* **Gradle Target**:
  ```kotlin
  wasmJs {
      browser {
          commonWebpackConfig {
              outputFileName = "composeApp.js"
          }
      }
      binaries.executable()
  }
  ```
* **HTML Shell**: [`toycollection.html`](file:///Users/luizvaldetaro/valdetaro/ToyCollectionLegacy/composeApp/src/wasmJsMain/resources/toycollection.html) mounting a `#compose-App` full-screen container with a `#000000` background.
* **Entry Point**: [`composeApp/src/wasmJsMain/kotlin/main.kt`](file:///Users/luizvaldetaro/valdetaro/ToyCollectionLegacy/composeApp/src/wasmJsMain/kotlin/main.kt):
  - Initializes Koin dependency injection (`toyCollectionModule`).
  - Sets image base URL via `gCsetImagesBaseUrl(Common.getActiveBaseUrl())` (`http://valdetaro.com/database/`).
  - Initializes Coil 3 image loader: `SingletonImageLoader.setSafe { ImageLoader.Builder(PlatformContext.INSTANCE).build() }`.
  - Injects Compose into the DOM via `ComposeViewport(viewportContainerId = "compose-App")`.
* **Data Flow**: Does not use SQLite. Loads static JSON catalog files (`carlist.json`, `carmaker.json`, etc.) from the web server using `Network.kt` and Ktor's `createPlatformHttpClient` (`HttpClient(Js)` on `wasmJs`, `HttpClient(OkHttp)` on JVM/Android).
* **Filesystem**: Disk filesystem calls ([`GcFile.wasmJs.kt`](file:///Users/luizvaldetaro/valdetaro/ToyCollectionLegacy/shared/common/src/wasmJsMain/kotlin/com/gepetto/common/GcFile.wasmJs.kt)) are stubbed as no-ops.

### 2.2 Lap Counter (`/LapCounter`)
* **Role**: Camera lap detection server (Desktop/Android) and client broadcast viewer (Web).
* **Gradle Target**: `wasmJs` with `commonWebpackConfig { outputFileName = "composeApp.js" }`.
* **HTML Shell**: [`gepettolapcounter.html`](file:///Users/luizvaldetaro/LapCounter/composeApp/src/wasmJsMain/resources/gepettolapcounter.html) with `#compose-App` container, dark background (`#121212`), and browser compatibility banner.
* **Entry Point**: [`composeApp/src/wasmJsMain/kotlin/main.kt`](file:///Users/luizvaldetaro/valdetaro/LapCounter/composeApp/src/wasmJsMain/kotlin/main.kt):
  - Injects version codes (`CommonConfig.webVersionCode = vCode * 10 + 6`).
  - Mounts `ComposeViewport(viewportContainerId = "compose-App") { LapCounterNavigation(...) }`.
* **Persistence**: [`FileWasm.kt`](file:///Users/luizvaldetaro/valdetaro/LapCounter/common/src/wasmJsMain/kotlin/com/valdetaro/common/FileWasm.kt) maps `File.writeText()` and `File.readText()` directly to `window.localStorage.setItem()` and `window.localStorage.getItem()`.

### 2.3 Race Director (`/RaceDirector`)
* **Role**: Slot Car Race Management System. Runs server embedded on Desktop/Android; client runs everywhere including Web.
* **Client-Only Architecture on Web**:
  - In [`PlatformHostHelper.wasmJs.kt`](file:///Users/luizvaldetaro/valdetaro/RaceDirector/composeApp/src/wasmJsMain/kotlin/com/gepettoracedirector/platform/PlatformHostHelper.wasmJs.kt):
    `canHostServer(): Boolean = false` and `isServerDefaultEnabled(): Boolean = false`.
  - The Web target does not run the embedded Ktor server or local SQLite instance; it connects over HTTP/WebSockets to a remote server.
* **Storage & Platform Abstractions**:
  - [`SettingsStorage.wasmJs.kt`](file:///Users/luizvaldetaro/valdetaro/RaceDirector/composeApp/src/wasmJsMain/kotlin/com/gepettoracedirector/platform/SettingsStorage.wasmJs.kt) bridges key-value persistence to `window.localStorage`.
  - [`ScrollbarHelper.wasmJs.kt`](file:///Users/luizvaldetaro/valdetaro/RaceDirector/composeApp/src/wasmJsMain/kotlin/com/gepettoracedirector/platform/ScrollbarHelper.wasmJs.kt) leaves platform scrollbars as no-ops to let Compose canvas handle scrolling natively.

---

## 3. Toy Collection Architectural Challenges & Web Strategy

While the companion viewer apps load read-only JSON or connect to a remote server, **Toy Collection** is an interactive **relational database management application**. The following technical challenges must be resolved for Web:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Toy Collection CommonMain                       │
│  UI (Compose M3 + Nav3) │ ToyRepository │ HtmlSyncService │ Models     │
└──────────────┬─────────────────────────┬──────────────────────┬────────┘
               │                         │                      │
       ┌───────▼───────┐         ┌───────▼───────┐      ┌───────▼───────┐
       │  desktopMain  │         │  androidMain  │      │  wasmJsMain   │
       │  JVM 21       │         │  Android ART  │      │  Kotlin/Wasm  │
       │  JDBC SQLite  │         │  Android SQLite│     │  sql.js Wasm  │
       │  SSHJ SFTP    │         │  SSHJ SFTP    │      │  IndexedDB    │
       │  OkHttp Ktor  │         │  OkHttp Ktor  │      │  Ktor JS      │
       └───────────────┘         └───────────────┘      └───────────────┘
```

### 3.1 Challenge 1: Relational SQLite Persistence on the Web
* **The Problem**: `ToyRepository` contains ~727 lines of SQL statements (`SELECT`, `JOIN`, `GROUP BY`, `SUM`, `COUNT`, `INSERT`, `UPDATE`, `DELETE`, `PRAGMA`, transactions, and cascading foreign updates). Web browsers do not offer a native C/JDBC SQLite driver.
* **The Solution**: **SQLite WebAssembly (`sql.js`) + Browser `IndexedDB`**.
  - `sql.js` compiles official SQLite to WebAssembly.
  - On application startup, load `default_toydb.db` from Compose resources or retrieve the user's updated database binary from browser `IndexedDB`.
  - Implement `ToyDatabase` and `SqlCursor` over the `sql.js` database instance.
  - Whenever an `execute()` mutation (insert, update, delete) occurs, asynchronously persist the exported SQLite binary snapshot to `IndexedDB`.
  - **Advantage**: Zero modifications required to `ToyRepository.kt`. 100% feature parity for queries, filters, sorting, category schema changes, and offline editing in the browser.

### 3.2 Challenge 2: Decoupling Platform Networking (Ktor OkHttp vs JS)
* **The Problem**: Currently, `commonMain.dependencies` in `composeApp/build.gradle.kts` directly declares:
  ```kotlin
  implementation(libs.ktor.client.okhttp)
  ```
  `OkHttp` is JVM-only and causes compilation failures on `wasmJs`. Furthermore, `HtmlSyncService.kt` and `SyncImage.kt` directly instantiate `HttpClient(OkHttp)`.
* **The Solution**:
  - Keep `ktor-client-core`, `content-negotiation`, and `serialization-kotlinx-json` in `commonMain`.
  - Move `ktor-client-okhttp` into `desktopMain` and `androidMain`.
  - Add `ktor-client-js` to `wasmJsMain`.
  - Introduce multiplatform factory `createPlatformHttpClient()` in commonMain with actuals for each target.

### 3.3 Challenge 3: Filesystem Independence & Image Resolution
* **The Problem**: `ImageResolver.kt`, `SyncImage.kt`, and `ImportExportService.kt` make calls to `java.io.File`, `System.getProperty("user.home")`, and `okio.FileSystem.SYSTEM`. Browsers operate inside an isolated sandbox without local disk directory access.
* **The Solution**:
  - In `wasmJsMain`, `resolveImageUri()` and `resolveBitmapUri()` resolve images directly to remote HTTP/HTTPS URLs (`${baseUrl}/${filename}`).
  - Coil 3 (`coil3.coil.compose`) handles asynchronous downloading, caching, and canvas rendering directly in memory and browser HTTP cache.
  - Guard `resolveImagesDir()` and local disk searches so they are only invoked on Desktop and Android.

### 3.4 Challenge 4: Sandboxed Export & SFTP Service Demarcation
* **The Problem**:
  - Desktop/Android write exported JSON files and HTML pages to local folders.
  - `DesktopSftpService` and `AndroidSftpService` use `net.schmizz.sshj` to open raw TCP/SSH sockets. Web browsers cannot open raw TCP sockets.
* **The Solution**:
  - **Export Operations**: On Web, exporting JSON or HTML triggers browser file downloads using Blob URLs (`<a download="carlist.json" href="blob:...">`).
  - **SFTP Service**: On Web, provide `WebSftpService` which explains via UI dialog that direct SFTP requires the Desktop or Android app, while guiding web users to use **Web HTTP Synchronization** (`HtmlSyncService`), which runs natively over standard HTTP/HTTPS.

### 3.5 Challenge 5: Platform Crypto and Date Formatting
* **The Problem**: `HtmlSyncService.kt` currently uses:
  - `java.text.SimpleDateFormat` and `java.util.TimeZone`
  - `java.security.MessageDigest.getInstance("SHA-256")`
  - `InfoScreen.kt` previously called `java.util.Locale.getDefault().language`
* **The Solution**:
  - Replace `java.util.Locale` in `InfoScreen.kt` with `androidx.compose.ui.text.intl.Locale.current.language`.
  - Introduce platform helper functions for date parsing and SHA-256 calculation (`PlatformCrypto` / `PlatformDate`).

---

## 4. Detailed Technical Specification by Module

### 4.1 Toolchain, Gradle & Version Catalog (`libs.versions.toml`)

#### 1. Add `ktor-client-js` to `gradle/libs.versions.toml`:
```toml
[versions]
ktorClientJs = "3.6.0"

[libraries]
ktor-client-js = { module = "io.ktor:ktor-client-js", version.ref = "ktorClientJs" }
```

#### 2. Configure `composeApp/build.gradle.kts`:
```kotlin
kotlin {
    androidTarget { ... }
    jvm("desktop") { ... }
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
            // Remove ktor-client-okhttp from here!
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            ...
        }
        desktopMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            ...
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            ...
        }
        val wasmJsMain = sourceSets.getByName("wasmJsMain")
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
    }
}
```

#### 3. Update `generateCommonConfig` Task:
```kotlin
val generateCommonConfig = tasks.register("generateCommonConfig") {
    val vName = libs.versions.versionName.get()
    val vCode = libs.versions.versionCode.get().toLong()
    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    val desktopCode = if (isWindows) vCode * 10 + 5 else vCode * 10 + 4
    val webCode = vCode * 10 + 6
    val outputDir = layout.buildDirectory.dir("generated/commonConfig/kotlin").get().asFile
    val outputFile = File(outputDir, "com/gepetto/toydb/CommonConfig.kt")
    
    doLast {
        outputFile.parentFile.mkdirs()
        outputFile.writeText("""
            package com.gepetto.toydb

            object CommonConfig {
                const val versionName = "$vName"
                const val versionCode = ${vCode}L
                const val desktopVersionCode = ${desktopCode}L
                const val webVersionCode = ${webCode}L
                const val versionCodeString = "$vCode"
            }
        """.trimIndent())
    }
}
```

---

### 4.2 Decoupling `commonMain` Platform Invocations

#### 1. Platform HTTP Client Factory (`com.gepetto.toydb.utils.PlatformHttpClient.kt`):
```kotlin
package com.gepetto.toydb.utils

import io.ktor.client.*

expect fun createPlatformHttpClient(block: HttpClientConfig<*>.() -> Unit = {}): HttpClient
```
* **`desktopMain` & `androidMain`**:
  ```kotlin
  actual fun createPlatformHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient(OkHttp, block)
  ```
* **`wasmJsMain`**:
  ```kotlin
  actual fun createPlatformHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient(Js, block)
  ```

#### 2. Platform Date & Crypto (`com.gepetto.toydb.utils.PlatformUtils.kt`):
```kotlin
package com.gepetto.toydb.utils

expect fun parseIsoOrCustomDate(dateStr: String): Long
expect fun calculateSha256(content: String): String
```
* On JVM/Android: implement using `SimpleDateFormat` and `MessageDigest`.
* On Wasm: implement using JS Date and Web Crypto API / pure Kotlin algorithm.

---

### 4.3 Web Database Engine (`WasmDatabase.kt` / `sql.js` + IndexedDB)

Create `composeApp/src/wasmJsMain/kotlin/com/gepetto/toydb/database/WasmDatabase.kt`:

1. **JS Interop Bindings for `sql.js`**:
   Declare external interfaces for SQL.js database:
   ```kotlin
   external interface SqlJsDatabase {
       fun run(sql: String, params: Array<Any?> = definedExternally)
       fun exec(sql: String, params: Array<Any?> = definedExternally): Array<SqlJsQueryResult>
       fun prepare(sql: String, params: Array<Any?> = definedExternally): SqlJsStatement
       fun export(): org.khronos.webgl.Uint8Array
       fun close()
   }
   ```
2. **`ToyDatabase` & `SqlCursor` Implementation**:
   - `WasmToyDatabase` implements `ToyDatabase`.
   - `WasmSqlCursor` wraps the result set and advances row by row.
   - Synchronizes mutations to IndexedDB store `"toydb_data"` under key `"database_snapshot"`.
3. **`createDatabase()` actual**:
   ```kotlin
   actual fun createDatabase(platformContext: Any?, dbName: String): ToyDatabase {
       return WasmToyDatabase.getInstance(dbName)
   }
   ```

---

### 4.4 Platform Expect/Actuals for `wasmJsMain`

Implement all required actuals under `composeApp/src/wasmJsMain/kotlin/`:

| File | Functions Implemented | Implementation Strategy |
| :--- | :--- | :--- |
| **`ui/PlatformScrollbar.wasmJs.kt`** | `PlatformScrollbar`, `PlatformGridScrollbar` | Lightweight overlay or let Compose canvas manage scrolling |
| **`utils/ImageResolver.wasmJs.kt`** | `resolveImageUri`, `resolveBitmapUri` | Resolve directly to remote URL `${baseUrl}/${filename}` for Coil |
| | `selectDirectoryDialog`, `selectFileDialog` | Return null or web notification (sandboxed) |
| | `rememberImagePicker` | File upload trigger using `<input type="file">` |
| | `isDesktopPlatform` | Returns `false` |
| | `formatTimestamp` | Uses JS `Date.toLocaleDateString()` |
| **`utils/KeepScreenOn.wasmJs.kt`** | `KeepScreenOn` | Uses HTML5 `navigator.wakeLock` or no-op |
| **`service/DesktopSftpService.kt`** | `DesktopSftpService` / `SftpService` | Web fallback stub indicating SFTP is desktop/mobile only |
| **`service/ImportExportService.kt`** | `getCurrentDateString()` | Formats date using JS Date |

---

### 4.5 Web Shell, Host HTML, and Entry Point (`Main.kt`)

#### 1. Host HTML Document: `composeApp/src/wasmJsMain/resources/toydatabase.html` (and `index.html`):
```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Gepetto Toy Database Manager</title>
    <style>
        html, body {
            width: 100%;
            height: 100%;
            margin: 0;
            padding: 0;
            overflow: hidden;
            background-color: #121212;
            color: #ffffff;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
        }
        #compose-App {
            width: 100%;
            height: 100%;
        }
    </style>
    <script type="application/javascript" src="composeApp.js"></script>
</head>
<body>
    <div id="compose-App"></div>
</body>
</html>
```

#### 2. Entry Point: `composeApp/src/wasmJsMain/kotlin/Main.kt`:
```kotlin
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import club.gepetto.composeutils.GcTheme
import club.gepetto.composeutils.image.gCsetImagesBaseUrl
import com.gepetto.toydb.CommonConfig
import com.gepetto.toydb.database.createDatabase
import com.gepetto.toydb.database.ToyRepository
import com.gepetto.toydb.ui.ToyDbNavigation

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val db = createDatabase(null, "toydb.db")
    val repository = ToyRepository(db)

    val baseUrl = repository.getBaseUrlSetting() ?: "https://gepetto.club/database/"
    gCsetImagesBaseUrl(baseUrl)

    ComposeViewport(viewportContainerId = "compose-App") {
        GcTheme {
            ToyDbNavigation(
                database = db,
                repository = repository
            )
        }
    }
}
```

---

## 5. Phase-by-Phase Actionable Execution Checklist

### Phase 0: Environment & Prerequisite Audit
- [ ] **Task 0.1**: Verify Node.js and browser environment for Wasm execution.
- [ ] **Task 0.2**: Verify that companion apps build `wasmJs` successfully as reference baseline:
  ```bash
  cd ~/valdetaro/ToyCollectionLegacy && ./gradlew compileKotlinWasmJs
  cd ~/valdetaro/LapCounter && ./gradlew compileKotlinWasmJs
  cd ~/valdetaro/RaceDirector && ./gradlew compileKotlinWasmJs
  ```
- [ ] **Task 0.3**: Confirm active Git status on `ToyCollection` and verify baseline builds pass:
  ```bash
  cd ~/valdetaro/ToyCollection && ./gradlew compileKotlinDesktop && ./gradlew compileDebugKotlinAndroid
  ```

### Phase 1: Version Catalog & Gradle Target Configuration
- [ ] **Task 1.1**: Update `ToyCollection/gradle/libs.versions.toml`: add `ktor-client-js` to `[libraries]`.
- [ ] **Task 1.2**: Update `ToyCollection/composeApp/build.gradle.kts`: add `wasmJs` target block with browser webpack config.
- [ ] **Task 1.3**: Move `implementation(libs.ktor.client.okhttp)` from `commonMain` to `desktopMain` and `androidMain`.
- [ ] **Task 1.4**: Add `implementation(libs.ktor.client.js)` to `wasmJsMain` in `build.gradle.kts`.
- [ ] **Task 1.5**: Update `generateCommonConfig` task in `build.gradle.kts` to output `webVersionCode = ${vCode * 10 + 6}L`.

### Phase 2: Platform Decoupling in commonMain
- [ ] **Task 2.1**: Create `com.gepetto.toydb.utils.PlatformHttpClient.kt` (`expect fun createPlatformHttpClient`).
- [ ] **Task 2.2**: Provide actuals in `desktopMain` and `androidMain` using `OkHttp`.
- [ ] **Task 2.3**: Refactor `HtmlSyncService.kt` to use `createPlatformHttpClient()` instead of hardcoded `HttpClient(OkHttp)`.
- [ ] **Task 2.4**: Refactor `SyncImage.kt` to use `createPlatformHttpClient()`.
- [ ] **Task 2.5**: Replace `java.util.Locale` in `InfoScreen.kt` with `androidx.compose.ui.text.intl.Locale.current.language`.
- [ ] **Task 2.6**: Create `PlatformUtils.kt` in `commonMain` with expect declarations for date parsing and SHA-256 hashing; provide desktop and android actuals.

### Phase 3: Web Database Implementation (Wasm SQLite + IndexedDB)
- [ ] **Task 3.1**: Create `composeApp/src/wasmJsMain/kotlin/com/gepetto/toydb/database/WasmDatabase.kt`.
- [ ] **Task 3.2**: Implement `SqlJsDatabase` external interfaces or SQLite Wasm bridge.
- [ ] **Task 3.3**: Implement `WasmToyDatabase` implementing `ToyDatabase` and `WasmSqlCursor` implementing `SqlCursor`.
- [ ] **Task 3.4**: Implement `actual fun createDatabase(...)` returning `WasmToyDatabase`.
- [ ] **Task 3.5**: Implement IndexedDB asynchronous snapshot save on database mutation.

### Phase 4: Implement wasmJsMain Platform Actuals
- [ ] **Task 4.1**: Implement `PlatformScrollbar.wasmJs.kt` (canvas-friendly scrollbar overlay).
- [ ] **Task 4.2**: Implement `ImageResolver.wasmJs.kt` (`resolveImageUri`, `resolveBitmapUri`, `isDesktopPlatform`, `formatTimestamp`, `rememberImagePicker`).
- [ ] **Task 4.3**: Implement `KeepScreenOn.wasmJs.kt` (wakeLock or no-op).
- [ ] **Task 4.4**: Implement `PlatformHttpClient.wasmJs.kt` returning `HttpClient(Js)`.
- [ ] **Task 4.5**: Implement `PlatformUtils.wasmJs.kt` (JS Date and SHA-256 implementation).
- [ ] **Task 4.6**: Implement `ImportExportService.wasmJs.kt` (`getCurrentDateString()` and browser download trigger).
- [ ] **Task 4.7**: Implement `DesktopSftpService.wasmJs.kt` (browser notice stub).

### Phase 5: Web UI Shell, HTML Host & Main Entry Point
- [ ] **Task 5.1**: Create `composeApp/src/wasmJsMain/resources/index.html` and `toydatabase.html` with `#compose-App`.
- [ ] **Task 5.2**: Create `composeApp/src/wasmJsMain/kotlin/Main.kt` using `ComposeViewport`.
- [ ] **Task 5.3**: Wire `ToyDbNavigation` with `WasmToyDatabase` and initial database hydration.

### Phase 6: Multiplatform Compilation, Verification & Browser Testing
- [ ] **Task 6.1**: Run `./gradlew compileKotlinWasmJs` and resolve any missing klib or JS interop errors.
- [ ] **Task 6.2**: Run `./gradlew wasmJsBrowserDevelopmentRun` and verify application loads at `http://localhost:8080`.
- [ ] **Task 6.3**: Verify Navigation 3 flows on Web (Home, Dashboard, Explorer, Makers, Settings, Info with tabs).
- [ ] **Task 6.4**: Verify Web HTTP Sync (`HtmlSyncService`) imports collection data from `https://gepetto.club/database/`.
- [ ] **Task 6.5**: Verify Coil 3 renders toy images on canvas without local disk dependencies.
- [ ] **Task 6.6**: Run regression tests: `./gradlew compileKotlinDesktop` and `./gradlew compileDebugKotlinAndroid` to ensure zero breakages.

### Phase 7: Packaging, Hosting & Deployment
- [ ] **Task 7.1**: Run `./gradlew wasmJsBrowserDistribution`.
- [ ] **Task 7.2**: Verify production static bundle in `composeApp/build/dist/wasmJs/productionExecutable/`.
- [ ] **Task 7.3**: Document deployment instructions for hosting on `https://gepetto.club/database/web/`.

---

## 6. Living Document Changelog

| Date | Rev | Author / Agent | Changes Made |
| :--- | :---: | :--- | :--- |
| **2026-10-06** | 1 | Antigravity | Initial creation of the Web (wasmJs) Port Plan living document. Full comparative analysis of `ToyCollectionLegacy`, `LapCounter`, and `RaceDirector`, technical solutions for relational SQLite on Wasm (`sql.js` + IndexedDB), Ktor OkHttp decoupling, sandboxed image resolution, and phase-by-phase actionable execution checklist. |

---

## 7. Bug, Blocker & Issue Tracker

| ID | Date Found | Component | Description & Symptoms | Root Cause | Status / Resolution |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **ISSUE-01** | 2026-10-06 | `composeApp` dependencies | `libs.ktor.client.okhttp` is in `commonMain` and fails on Wasm. | OkHttp is a JVM-only Java library. | **Open** - Must move `ktor.client.okhttp` to `desktopMain` and `androidMain`, and add `ktor.client.js` to `wasmJsMain`. |
| **ISSUE-02** | 2026-10-06 | `database` | `ToyDatabase` has no implementation for `wasmJs`. | Desktop uses JDBC sqlite, Android uses Android framework sqlite. | **Open** - Planned `WasmToyDatabase` using `sql.js` (WebAssembly SQLite) with IndexedDB snapshot persistence. |
| **ISSUE-03** | 2026-10-06 | `SyncImage.kt` | Calls `System.getProperty("user.home")` and `FileSystem.SYSTEM`. | Desktop/Android filesystem assumptions in commonMain. | **Open** - Abstract disk directory lookups; on Web, load directly via remote URL in Coil 3. |
| **ISSUE-04** | 2026-10-06 | `HtmlSyncService.kt` | Uses `SimpleDateFormat`, `TimeZone.getTimeZone`, and `MessageDigest`. | Java standard library classes in commonMain. | **Open** - Abstract date parsing and SHA-256 calculation to expect/actual `PlatformUtils`. |
| **ISSUE-05** | 2026-10-06 | `SftpService` | SSH/SFTP cannot connect over raw TCP sockets in browser sandbox. | Web browser security sandbox prohibits raw socket connections. | **Open** - Provide `WebSftpService` stub guiding web users to Web HTTP Sync (`HtmlSyncService`). |
| **ISSUE-06** | 2026-10-06 | `ImportExportService` | Uses `okio.FileSystem.SYSTEM` which is not available in Okio for wasmJs. | Okio does not provide system filesystem on browser wasm. | **Open** - Web export operations will trigger browser file downloads (Blob URL `<a download="...">`). |
