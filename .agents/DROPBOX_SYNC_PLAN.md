# Dropbox Backup Synchronization Plan: Toy Collection (Living Document)

> **Document Status**: Living Roadmap & Technical Specification  
> **Target Application**: Toy Collection Manager (`/Users/luizvaldetaro/valdetaro/ToyCollection`)  
> **Workspace**: `/Users/luizvaldetaro/valdetaro`  
> **Document Location**: `ToyCollection/.agents/DROPBOX_SYNC_PLAN.md`  
> **Last Updated**: 2026-10-12 (Rev 1 — Initial technical plan for Option B backup sync with dedicated tab)  
> **Current Phase**: Phase 0 (Planning & Setup)

---

## Table of Contents
1. [Executive Summary & Architecture Scope](#1-executive-summary--architecture-scope)
2. [Dropbox Developer Console Registration Guide](#2-dropbox-developer-console-registration-guide)
3. [Deep Link & Platform Authorization Specification](#3-deep-link--platform-authorization-specification)
   - 3.1 [Android Deep Link & Manifest Setup](#31-android-deep-link--manifest-setup)
   - 3.2 [Desktop Localhost Loopback Setup](#32-desktop-localhost-loopback-setup)
4. [OAuth 2.0 with PKCE & Token Lifecycle](#4-oauth-20-with-pkce--token-lifecycle)
5. [Dropbox API v2 Service Specification (KMP / Ktor)](#5-dropbox-api-v2-service-specification-kmp--ktor)
6. [UI & UX Specification: Dedicated Dropbox Settings Tab](#6-ui--ux-specification-dedicated-dropbox-settings-tab)
7. [Localization Requirements (6 Languages)](#7-localization-requirements-6-languages)
8. [Phase-by-Phase Execution Plan for Agents](#8-phase-by-phase-execution-plan-for-agents)
9. [Living Document Changelog](#9-living-document-changelog)
10. [Bug, Blocker & Issue Tracker](#10-bug-blocker--issue-tracker)

---

## 1. Executive Summary & Architecture Scope

### 1.1 Goal
Provide reliable, user-friendly cloud backup synchronization for the **Toy Collection** application using Dropbox.

### 1.2 Selected Design: Option B (Snapshot Archive Sync)
- **Backup Mechanism**:
  - The app creates standard `.zip` backup archives using the existing `BackupRestoreService`.
  - The archive packages `manifest.json`, `photos.json`, collection databases (`carmaker.json`, category lists), and photos into one archive.
  - The archive is uploaded directly to the user's Dropbox App folder under `/Backups/`.
- **Restore Mechanism**:
  - The app queries Dropbox for available `.zip` archives.
  - The user selects a backup archive to restore.
  - The app downloads the archive to a temporary file, validates the manifest, shows counts of toys, makers, and photos in a confirmation dialog, and restores the collection.

### 1.3 UI Placement
- A dedicated **Dropbox** tab in `SettingsScreen.kt` alongside `General`, `Backup & Restore`, `Server Sync (SFTP)`, and `Categories`.

### 1.4 Kotlin Multiplatform (KMP) Architecture
- Dropbox API v2 runs completely on standard HTTPS REST endpoints.
- All HTTP requests will use the existing `createToyHttpClient()` Ktor client in `commonMain`.
- No Java-only Dropbox SDK is used. This preserves full multiplatform support across Desktop (macOS M1/Intel, Windows) and Android.

---

## 2. Dropbox Developer Console Registration Guide

> [!IMPORTANT]
> **Single Registration Rule**: Only the developer registers **one app** in the Dropbox App Console. End users do not register apps. End users only log in and authorize the app.

Follow these exact steps to register the application:

1. **Log in to Dropbox**:
   - Open a browser and navigate to: `https://www.dropbox.com/developers/apps`.
   - Sign in with your developer Dropbox account.

2. **Create New App**:
   - Click the blue **Create app** button.
   - **Step 1: Choose an API**: Select **Scoped access**.
   - **Step 2: Choose the type of access**: Select **App folder**.
     *(Name of the folder will be dedicated to this app inside `/Apps/`. This isolates app data from all user personal files).*
   - **Step 3: Name your app**: Enter a unique name (for example, `Gepetto Toy Collection Manager`).
     *(Note: Dropbox does not permit the word "Dropbox" in the app name).*
   - Click **Create app**.

3. **Configure Permissions (Scopes)**:
   - On your app dashboard, click the **Permissions** tab.
   - Under **Files and folders**, select:
     - `files.metadata.read`
     - `files.content.read`
     - `files.content.write`
   - Under **Account information**, select:
     - `account_info.read`
   - Scroll to the bottom and click **Submit**.

4. **Configure OAuth 2 Settings & Redirect URIs**:
   - Click the **Settings** tab.
   - Locate the **App key** (`client_id`). Copy this value.
     *(Note: Do not distribute the App Secret. With PKCE, native public clients do not use the App Secret).*
   - Scroll down to the **OAuth 2** section.
   - Under **Redirect URIs**, add the following two URIs:
     1. **Desktop Loopback URI**: `http://localhost:52478/oauth/callback`
     2. **Android Custom Scheme URI**: `db-<APP_KEY>://1/connect` (replace `<APP_KEY>` with your real App Key).
        *(Alternative Android scheme if needed: `toydb://dropbox-callback`)*
   - Click **Add** for each redirect URI.
   - Verify that **Allow implicit grant** is set to **Disallow** (PKCE authorization code flow is used).

---

## 3. Deep Link & Platform Authorization Specification

### 3.1 Android Deep Link & Manifest Setup

To receive the authorization code back from the web browser on Android:

1. **Modify `AndroidManifest.xml`**:
   Add an `<intent-filter>` to `AppMainActivity` inside `ToyCollection/composeApp/src/androidMain/AndroidManifest.xml`:

   ```xml
   <activity
       android:name="com.gepetto.toydb.AppMainActivity"
       android:exported="true"
       android:launchMode="singleTask"
       android:configChanges="orientation|screenSize|screenLayout|keyboardHidden"
       android:windowSoftInputMode="adjustResize">
       <intent-filter>
           <action android:name="android.intent.action.MAIN" />
           <category android:name="android.intent.category.LAUNCHER" />
       </intent-filter>
       <!-- Dropbox OAuth callback deep link -->
       <intent-filter>
           <action android:name="android.intent.action.VIEW" />
           <category android:name="android.intent.category.DEFAULT" />
           <category android:name="android.intent.category.BROWSABLE" />
           <data android:scheme="db-${dropboxAppKey}" android:host="1" android:path="/connect" />
       </intent-filter>
   </activity>
   ```

2. **Handle Incoming Intent in `AppMainActivity.kt`**:
   In `AppMainActivity.kt`:
   - Override `onNewIntent(intent: Intent)`:
     ```kotlin
     override fun onNewIntent(intent: android.content.Intent) {
         super.onNewIntent(intent)
         handleDropboxCallback(intent)
     }
     ```
   - In `onCreate(savedInstanceState: Bundle?)`, call `handleDropboxCallback(intent)`.
   - `handleDropboxCallback(intent)` extracts `intent.data?.getQueryParameter("code")` and notifies `DropboxAuthManager.onAuthCodeReceived(code)`.

### 3.2 Desktop Localhost Loopback Setup

On macOS and Windows:
1. No OS deep link is required.
2. The desktop app starts a lightweight temporary HTTP server listening on `http://127.0.0.1:52478/oauth/callback`.
3. The app opens the system browser with the authorization URL.
4. When the user approves access, Dropbox redirects the browser to `http://localhost:52478/oauth/callback?code=...`.
5. The local server:
   - Extracts the `code` query parameter.
   - Sends an HTTP 200 response with a user-friendly page:
     ```html
     <!DOCTYPE html>
     <html>
       <head><title>Authorization Successful</title><meta charset="utf-8"></head>
       <body style="font-family: sans-serif; text-align: center; padding-top: 50px;">
         <h2>Toy Collection is connected to Dropbox!</h2>
         <p>You can close this tab and return to the application.</p>
       </body>
     </html>
     ```
   - Shuts down the local server.
   - Forwards the authorization code to complete the token exchange.

---

## 4. OAuth 2.0 with PKCE & Token Lifecycle

### 4.1 Proof Key for Code Exchange (PKCE)
PKCE removes the need for a client secret in public desktop/mobile apps (RFC 7636).

1. **Code Verifier**:
   - High-entropy cryptographic random string (43 to 128 characters from `[A-Z]`, `[a-z]`, `[0-9]`, `-`, `_`, `.`, `~`).
2. **Code Challenge**:
   - SHA-256 hash of `code_verifier`.
   - Base64URL-encoded (without padding `=`).
   - Hash is computed using `org.kotlincrypto.hash.sha2.SHA256` (already in `commonMain`).

### 4.2 Authorization Request
- Browser opens:
  ```
  https://www.dropbox.com/oauth2/authorize?client_id=<APP_KEY>&response_type=code&code_challenge=<CHALLENGE>&code_challenge_method=S256&token_access_type=offline&redirect_uri=<ENCODED_REDIRECT_URI>
  ```
- `token_access_type=offline` ensures Dropbox returns a permanent `refresh_token`.

### 4.3 Token Exchange
- POST request to: `https://api.dropboxapi.com/oauth2/token`
- Content-Type: `application/x-www-form-urlencoded`
- Parameters:
  - `grant_type`: `authorization_code`
  - `code`: `<AUTH_CODE>`
  - `client_id`: `<APP_KEY>`
  - `code_verifier`: `<CODE_VERIFIER>`
  - `redirect_uri`: `<REDIRECT_URI>`
- Returns:
  ```json
  {
    "access_token": "sl.u.AF...",
    "token_type": "bearer",
    "expires_in": 14400,
    "refresh_token": "dbrfr:...",
    "account_id": "dbid:..."
  }
  ```

### 4.4 Token Storage & Silent Auto-Refresh
- The `refresh_token`, `account_id`, and `account_name` are saved in `ToyRepository` under:
  - `dropbox_refresh_token`
  - `dropbox_account_id`
  - `dropbox_account_name`
  - `dropbox_account_email`
- The `access_token` and expiry timestamp are kept in memory.
- Before each Dropbox API call, if the access token is missing or expired, the app calls:
  - POST `https://api.dropboxapi.com/oauth2/token`
  - Parameters:
    - `grant_type`: `refresh_token`
    - `refresh_token`: `<STORED_REFRESH_TOKEN>`
    - `client_id`: `<APP_KEY>`
- This refresh happens automatically in the background. The user logs in only once.

---

## 5. Dropbox API v2 Service Specification (KMP / Ktor)

The service is located in `composeApp/src/commonMain/kotlin/com/gepetto/toydb/service/DropboxService.kt`.

### 5.1 Data Models
```kotlin
data class DropboxAccount(
    val accountId: String,
    val displayName: String,
    val email: String
)

data class DropboxFileEntry(
    val name: String,
    val pathDisplay: String,
    val serverModified: String,
    val sizeBytes: Long
)
```

### 5.2 Interface Contract
```kotlin
interface DropboxService {
    val isConnected: Boolean
    suspend fun getAccountInfo(): Result<DropboxAccount>
    suspend fun listBackups(): Result<List<DropboxFileEntry>>
    suspend fun uploadBackup(
        backupFile: okio.Path,
        remoteFileName: String,
        onProgress: (bytesWritten: Long, totalBytes: Long) -> Unit
    ): Result<DropboxFileEntry>
    suspend fun downloadBackup(
        remoteFilePath: String,
        targetFile: okio.Path,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit
    ): Result<Unit>
    suspend fun disconnect()
}
```

### 5.3 Key Endpoints
1. **Get Account Info**:
   - `POST https://api.dropboxapi.com/2/users/get_current_account`
   - Header: `Authorization: Bearer <access_token>`
2. **List Folder**:
   - `POST https://api.dropboxapi.com/2/files/list_folder`
   - Header: `Authorization: Bearer <access_token>`, `Content-Type: application/json`
   - Body: `{"path": "/Backups", "recursive": false}`
   - If folder does not exist, treat as empty list.
3. **Upload File**:
   - `POST https://content.dropboxapi.com/2/files/upload`
   - Header:
     - `Authorization: Bearer <access_token>`
     - `Dropbox-API-Arg: {"path": "/Backups/<fileName>", "mode": "add", "autorename": true, "mute": false}`
     - `Content-Type: application/octet-stream`
   - Body: Binary stream.
4. **Download File**:
   - `POST https://content.dropboxapi.com/2/files/download`
   - Header:
     - `Authorization: Bearer <access_token>`
     - `Dropbox-API-Arg: {"path": "<remotePath>"}`
   - Response: Binary stream. Target file is written using `systemFileSystem`.

---

## 6. UI & UX Specification: Dedicated Dropbox Settings Tab

### 6.1 `SettingsTab` Enum Update
In `ToyCollection/composeApp/src/commonMain/kotlin/com/gepetto/toydb/ui/SettingsScreen.kt`:
```kotlin
enum class SettingsTab(val id: String, val titleRes: StringResource) {
    GENERAL("general", Res.string.tab_general),
    BACKUP_RESTORE("backup_restore", Res.string.backup_title),
    SERVER_SYNC("server_sync", Res.string.info_tab_backup),
    DROPBOX("dropbox", Res.string.tab_dropbox),
    CATEGORIES("categories", Res.string.categories)
}
```

### 6.2 Composable Layout: `DropboxSettingsTab.kt`
Location: `ToyCollection/composeApp/src/commonMain/kotlin/com/gepetto/toydb/ui/DropboxSettingsTab.kt`

Cards displayed in the tab:

1. **Dropbox Account Card**:
   - When Disconnected:
     - Icon and explanation text: "Connect your Dropbox account to back up and restore your collections directly to the cloud."
     - Button: **Connect to Dropbox** (Primary color).
   - When Connected:
     - Status indicator: Green checkmark with user display name and email.
     - Storage target info: "Target folder: `/Apps/Gepetto Toy Collection/Backups/`"
     - Button: **Disconnect** (Outlined error color).

2. **Upload Backup Card**:
   - Button: **Create and Upload Backup to Dropbox**.
   - Input: Optional custom backup label or default timestamp `toy_collection_backup_YYYYMMDD_HHMM.zip`.
   - Progress bar showing collection export and network upload stages.
   - Success alert showing toy count, maker count, photo count, and uploaded file name.

3. **Cloud Backups List Card**:
   - Header with **Refresh** button.
   - List of available backup `.zip` files in Dropbox:
     - File name (e.g. `toy_collection_backup_20261012_1430.zip`)
     - Date and time
     - Formatted file size (e.g. `42.5 MB`)
     - Action button: **Restore**
   - Empty state: "No cloud backups found in Dropbox. Create your first backup above."

4. **Restore Confirmation Dialog**:
   - When the user clicks **Restore**:
     - Downloads the `.zip` archive to a temporary file.
     - Validates the manifest with `BackupRestoreService.openBackup(...)`.
     - Displays summary:
       - Created Date
       - Toys count
       - Makers count
       - Photos count
     - Displays warning: "Restoring this backup will replace current collection data."
     - Buttons: **Cancel** and **Confirm Restore**.
     - On confirmation: runs `BackupRestoreService.restore(...)` and triggers app UI refresh.

### 6.3 Compose UI Rules
- Use `sysBackgroundColor()` and `sysTextColor()`.
- Dialogs must have borders in dark mode per Rule 15 of `ToyCollection/.agents/AGENTS.md`.
- Include `@PreviewLightDark` previews wrapped with `GcTheme`.

---

## 7. Localization Requirements (6 Languages)

Add string resources across all 6 language files:
- `values/strings.xml` (English)
- `values-de/strings.xml` (German)
- `values-es/strings.xml` (Spanish)
- `values-fr/strings.xml` (French)
- `values-it/strings.xml` (Italian)
- `values-pt/strings.xml` (Portuguese)

Keys to add:
- `tab_dropbox`: "Dropbox"
- `dropbox_connected_as`: "Connected as %1$s (%2$s)"
- `dropbox_not_connected`: "Dropbox is not connected"
- `dropbox_connect_btn`: "Connect to Dropbox"
- `dropbox_disconnect_btn`: "Disconnect"
- `dropbox_create_backup_btn`: "Create and Upload Backup"
- `dropbox_uploading_progress`: "Uploading backup to Dropbox (%1$d%%)..."
- `dropbox_downloading_progress`: "Downloading backup from Dropbox (%1$d%%)..."
- `dropbox_available_backups`: "Available Cloud Backups"
- `dropbox_no_backups`: "No backups found in Dropbox."
- `dropbox_restore_confirm_title`: "Restore Backup from Dropbox"
- `dropbox_restore_confirm_desc`: "Are you sure you want to restore '%1$s'? Current collection data will be replaced."
- `dropbox_backup_success`: "Backup successfully uploaded to Dropbox."
- `dropbox_restore_success`: "Collection successfully restored from Dropbox."
- `dropbox_error`: "Dropbox error: %1$s"

---

## 8. Phase-by-Phase Execution Plan for Agents

### Phase 1: Authentication Core & Settings Storage
- [ ] Define `DropboxConfig.kt` (app key, endpoints, redirect URIs).
- [ ] Implement `PkceUtil.kt` in `commonMain` using `org.kotlincrypto.hash.sha2.SHA256`.
- [ ] Add Dropbox settings accessors in `ToyRepository.kt` (`dropbox_refresh_token`, `dropbox_account_id`, `dropbox_account_name`, `dropbox_account_email`).
- [ ] Implement `DropboxAuthManager.kt` (generates auth URL, exchanges code for tokens, handles silent token refresh).
- [ ] Unit tests for PKCE generation and URL composition.

### Phase 2: Platform Authorization Hooks (Android & Desktop)
- [ ] Desktop: Implement `DesktopDropboxAuthHandler.kt` with a local HTTP loopback server on port `52478`.
- [ ] Android: Add deep-link intent filter in `AndroidManifest.xml` (`db-<APP_KEY>://1/connect`).
- [ ] Android: Implement `onNewIntent` handler in `AppMainActivity.kt` to catch the OAuth authorization code.
- [ ] Expect/actual platform binder `openDropboxLogin(authUrl: String)`.

### Phase 3: Dropbox API Service (Ktor Client)
- [ ] Implement `DropboxServiceImpl.kt` in `commonMain`.
- [ ] `getAccountInfo()` implementation.
- [ ] `listBackups()` implementation.
- [ ] `uploadBackup()` streaming upload integration.
- [ ] `downloadBackup()` streaming download to temp file.
- [ ] Unit tests for JSON response parsing and mock HTTP responses.

### Phase 4: UI & Navigation Integration
- [ ] Add `DROPBOX` to `SettingsTab` in `SettingsScreen.kt`.
- [ ] Create `DropboxSettingsTab.kt` with Account, Backup, and Restore cards.
- [ ] Add Restore confirmation dialog with `BackupRestoreService` manifest inspection.
- [ ] Add all string keys to `strings.xml` in `en`, `de`, `es`, `fr`, `it`, `pt`.
- [ ] Add previews for Light, Dark, and Landscape modes.

### Phase 5: Verification & Quality Assurance
- [ ] Test Desktop build (`./gradlew :composeApp:compileKotlinDesktop`).
- [ ] Test Android assemble (`./gradlew :composeApp:assembleDebug`).
- [ ] Verify clean restore flow with mock or real Dropbox backup archive.
- [ ] Check logs use `GcLog` exclusively.
- [ ] Confirm no Git commits or pushes were made without explicit user instruction.

---

## 9. Living Document Changelog

| Date | Revision | Author | Description of Changes |
| :--- | :--- | :--- | :--- |
| 2026-10-12 | Rev 1 | Antigravity | Initial creation of the living Dropbox Backup Synchronization Plan (Option B with dedicated tab, PKCE OAuth 2.0, Desktop loopback, Android deep links, and Ktor client). |

---

## 10. Bug, Blocker & Issue Tracker

| Issue ID | Phase | Severity | Status | Description | Root Cause | Workaround / Solution |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| *None yet* | - | - | - | No blockers encountered yet. | - | - |
