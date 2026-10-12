# Google Drive Backup Synchronization Plan: Toy Collection (Living Document)

> **Document Status**: Living Roadmap & Technical Specification  
> **Target Application**: Toy Collection Manager (`/Users/luizvaldetaro/valdetaro/ToyCollection`)  
> **Workspace**: `/Users/luizvaldetaro/valdetaro`  
> **Document Location**: `ToyCollection/.agents/GOOGLE_DRIVE_SYNC_PLAN.md`  
> **Last Updated**: 2026-10-12 (Rev 1 — Initial technical plan for Option B backup sync with dedicated tab)  
> **Current Phase**: Phase 0 (Planning & Setup)

---

## Table of Contents
1. [Executive Summary & Architecture Scope](#1-executive-summary--architecture-scope)
2. [Google Cloud Console Registration Guide](#2-google-cloud-console-registration-guide)
3. [Deep Link & Platform Authorization Specification](#3-deep-link--platform-authorization-specification)
   - 3.1 [Android Deep Link & Manifest Setup](#31-android-deep-link--manifest-setup)
   - 3.2 [Desktop Localhost Loopback Setup](#32-desktop-localhost-loopback-setup)
4. [OAuth 2.0 with PKCE & Token Lifecycle](#4-oauth-20-with-pkce--token-lifecycle)
5. [Google Drive API v3 Service Specification (KMP / Ktor)](#5-google-drive-api-v3-service-specification-kmp--ktor)
6. [UI & UX Specification: Dedicated Google Drive Settings Tab](#6-ui--ux-specification-dedicated-google-drive-settings-tab)
7. [Localization Requirements (6 Languages)](#7-localization-requirements-6-languages)
8. [Phase-by-Phase Execution Plan for Agents](#8-phase-by-phase-execution-plan-for-agents)
9. [Living Document Changelog](#9-living-document-changelog)
10. [Bug, Blocker & Issue Tracker](#10-bug-blocker--issue-tracker)

---

## 1. Executive Summary & Architecture Scope

### 1.1 Goal
Provide reliable, user-friendly cloud backup synchronization for the **Toy Collection** application using Google Drive.

### 1.2 Selected Design: Option B (Snapshot Archive Sync)
- **Backup Mechanism**:
  - The app creates standard `.zip` backup archives using the existing `BackupRestoreService`.
  - The archive packages `manifest.json`, `photos.json`, collection databases (`carmaker.json`, category lists), and photos into one archive.
  - The archive is uploaded directly to a dedicated folder in the user's Google Drive named `Toy Collection Backups`.
- **Restore Mechanism**:
  - The app queries Google Drive for available `.zip` archives in the `Toy Collection Backups` folder.
  - The user selects a backup archive to restore.
  - The app downloads the archive to a temporary file, validates the manifest, shows counts of toys, makers, and photos in a confirmation dialog, and restores the collection.

### 1.3 UI Placement
- A dedicated **Google Drive** tab in `SettingsScreen.kt` alongside `General`, `Backup & Restore`, `Server Sync (SFTP)`, `Dropbox`, and `Categories`.

### 1.4 Kotlin Multiplatform (KMP) Architecture
- Google Drive API v3 runs completely on standard HTTPS REST endpoints.
- All HTTP requests will use the existing `createToyHttpClient()` Ktor client in `commonMain`.
- No Google Play Services library or Java-only Google API Client library is used. This preserves full multiplatform support across Desktop (macOS M1/Intel, Windows) and Android.

---

## 2. Google Cloud Console Registration Guide

> [!IMPORTANT]
> **Single Registration Rule**: Only the developer registers **one project** in the Google Cloud Console. End users do not create developer accounts or register projects. End users only log in with their Google accounts and click "Allow".

Follow these exact steps to register the application:

1. **Open Google Cloud Console**:
   - Navigate to: `https://console.cloud.google.com/`.
   - Sign in with your Google developer account.

2. **Create New Project**:
   - Click the project drop-down menu at the top of the page.
   - Click **New Project**.
   - **Project Name**: Enter `Gepetto Toy Collection` (or `Toy Collection Manager`).
   - Click **Create**.
   - Ensure the new project is selected in the top bar.

3. **Enable Google Drive API**:
   - In the left navigation menu, go to **APIs & Services** > **Library**.
   - Search for `Google Drive API`.
   - Click **Google Drive API** and click **Enable**.

4. **Configure OAuth Consent Screen**:
   - In the left navigation menu, go to **APIs & Services** > **OAuth consent screen**.
   - **User Type**: Select **External**.
   - Click **Create**.
   - **App Information**:
     - **App name**: `Gepetto Toy Collection Manager`
     - **User support email**: Select your email address.
     - **Developer contact information**: Enter your email address.
   - Click **Save and Continue**.
   - **Scopes Step**:
     - Click **Add or Remove Scopes**.
     - Add the following non-sensitive / restricted-to-app scopes:
       - `https://www.googleapis.com/auth/drive.file` *(Allows the app to view and manage only files and folders that the app itself created in Google Drive. This avoids expensive verification audits for full personal Drive access).*
       - `https://www.googleapis.com/auth/userinfo.profile` *(Allows showing the user's name on the connected account card).*
       - `https://www.googleapis.com/auth/userinfo.email` *(Allows showing the user's email address).*
     - Click **Update** and then **Save and Continue**.
   - **Test Users Step**:
     - While the publishing status is "Testing", add your personal/testing Google email addresses.
     - Click **Save and Continue**.
     - Click **Back to Dashboard**.

5. **Create OAuth 2.0 Credentials**:
   - Go to **APIs & Services** > **Credentials**.
   - Click **Create Credentials** > **OAuth client ID**.

   - **Credential 1: Desktop Application**:
     - **Application type**: Select **Desktop app**.
     - **Name**: `Toy Collection Desktop`.
     - Click **Create**.
     - Copy the generated **Client ID** (for example: `123456789-abc.apps.googleusercontent.com`).
     *(Note: Google natively permits loopback redirects `http://127.0.0.1:<port>` for Desktop client IDs without needing manual redirect URI whitelisting).*

   - **Credential 2: Android / Mobile Application**:
     - Click **Create Credentials** > **OAuth client ID**.
     - **Application type**: Select **Android** (or **Web application** if using custom scheme loopback).
     - **Package name**: `com.gepetto.slotcarscollection` (from `build.gradle.kts` defaultConfig).
     - **SHA-1 certificate fingerprint**:
       - To get debug SHA-1 on macOS:
         ```bash
         keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
         ```
       - Copy the SHA1 fingerprint and paste it into the console.
       - Click **Create**.
     - Note the **Client ID**.

---

## 3. Deep Link & Platform Authorization Specification

### 3.1 Android Deep Link & Manifest Setup

To receive the OAuth authorization code back from the web browser on Android:

1. **Google Custom Scheme Format**:
   Google uses a reverse DNS scheme derived from the Client ID:
   - If Client ID is: `1234567890-abcdef.apps.googleusercontent.com`
   - The reverse scheme is: `com.googleusercontent.apps.1234567890-abcdef`
   - The path is: `/oauth2redirect`
   - Complete redirect URI: `com.googleusercontent.apps.1234567890-abcdef:/oauth2redirect`
   *(Alternative: A custom scheme such as `toydb://googledrive-callback` can be used with a Web/Custom Client ID).*

2. **Modify `AndroidManifest.xml`**:
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
       <!-- Google Drive OAuth callback deep link -->
       <intent-filter>
           <action android:name="android.intent.action.VIEW" />
           <category android:name="android.intent.category.DEFAULT" />
           <category android:name="android.intent.category.BROWSABLE" />
           <data android:scheme="com.googleusercontent.apps.${googleClientIdPrefix}" android:path="/oauth2redirect" />
       </intent-filter>
   </activity>
   ```

3. **Handle Incoming Intent in `AppMainActivity.kt`**:
   In `AppMainActivity.kt`:
   - Override `onNewIntent(intent: Intent)`:
     ```kotlin
     override fun onNewIntent(intent: android.content.Intent) {
         super.onNewIntent(intent)
         handleGoogleDriveCallback(intent)
     }
     ```
   - In `onCreate(savedInstanceState: Bundle?)`, call `handleGoogleDriveCallback(intent)`.
   - `handleGoogleDriveCallback(intent)` extracts `intent.data?.getQueryParameter("code")` and forwards it to `GoogleDriveAuthManager.onAuthCodeReceived(code)`.

### 3.2 Desktop Localhost Loopback Setup

On macOS and Windows:
1. No OS deep link is required.
2. The desktop app starts a lightweight temporary HTTP server listening on `http://127.0.0.1:52479/oauth/callback`.
3. The app opens the system browser with the Google authorization URL.
4. When the user approves access, Google redirects the browser to `http://127.0.0.1:52479/oauth/callback?code=...`.
5. The local server:
   - Extracts the `code` query parameter.
   - Sends an HTTP 200 response with a user-friendly page:
     ```html
     <!DOCTYPE html>
     <html>
       <head><title>Authorization Successful</title><meta charset="utf-8"></head>
       <body style="font-family: sans-serif; text-align: center; padding-top: 50px;">
         <h2>Toy Collection is connected to Google Drive!</h2>
         <p>You can close this tab and return to the application.</p>
       </body>
     </html>
     ```
   - Shuts down the local server.
   - Forwards the authorization code to complete the token exchange.

---

## 4. OAuth 2.0 with PKCE & Token Lifecycle

### 4.1 Proof Key for Code Exchange (PKCE)
Google OAuth 2.0 strongly recommends PKCE for native public desktop and mobile clients (RFC 7636).

1. **Code Verifier**:
   - Cryptographically random string between 43 and 128 characters from `[A-Z]`, `[a-z]`, `[0-9]`, `-`, `_`, `.`, `~`.
2. **Code Challenge**:
   - SHA-256 hash of `code_verifier`.
   - Base64URL-encoded (without padding `=`).
   - Hash is computed using `org.kotlincrypto.hash.sha2.SHA256` in `commonMain`.

### 4.2 Authorization Request
- Browser opens:
  ```
  https://accounts.google.com/o/oauth2/v2/auth?client_id=<CLIENT_ID>&redirect_uri=<ENCODED_REDIRECT_URI>&response_type=code&scope=https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fdrive.file%20https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fuserinfo.profile%20https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fuserinfo.email&code_challenge=<CHALLENGE>&code_challenge_method=S256&access_type=offline&prompt=consent
  ```
- `access_type=offline` and `prompt=consent` ensure Google always returns a `refresh_token`.

### 4.3 Token Exchange
- POST request to: `https://oauth2.googleapis.com/token`
- Content-Type: `application/x-www-form-urlencoded`
- Parameters:
  - `grant_type`: `authorization_code`
  - `code`: `<AUTH_CODE>`
  - `client_id`: `<CLIENT_ID>`
  - `code_verifier`: `<CODE_VERIFIER>`
  - `redirect_uri`: `<REDIRECT_URI>`
- Returns:
  ```json
  {
    "access_token": "ya29.a0Af...",
    "token_type": "Bearer",
    "expires_in": 3599,
    "refresh_token": "1//04...",
    "scope": "https://www.googleapis.com/auth/drive.file ..."
  }
  ```

### 4.4 Token Storage & Silent Auto-Refresh
- The `refresh_token`, `account_id`, `account_name`, and `account_email` are saved in `ToyRepository` under:
  - `gdrive_refresh_token`
  - `gdrive_account_id`
  - `gdrive_account_name`
  - `gdrive_account_email`
- The `access_token` and expiry timestamp are kept in memory.
- Before each Google Drive API call, if the access token is missing or expired, the app calls:
  - POST `https://oauth2.googleapis.com/token`
  - Parameters:
    - `grant_type`: `refresh_token`
    - `refresh_token`: `<STORED_REFRESH_TOKEN>`
    - `client_id`: `<CLIENT_ID>`
- This refresh happens automatically in the background. The user logs in only once.

---

## 5. Google Drive API v3 Service Specification (KMP / Ktor)

The service is located in `composeApp/src/commonMain/kotlin/com/gepetto/toydb/service/GoogleDriveService.kt`.

### 5.1 Data Models
```kotlin
data class GoogleDriveAccount(
    val id: String,
    val displayName: String,
    val email: String,
    val photoUrl: String?
)

data class GoogleDriveFileEntry(
    val id: String,
    val name: String,
    val mimeType: String,
    val modifiedTime: String,
    val sizeBytes: Long
)
```

### 5.2 Interface Contract
```kotlin
interface GoogleDriveService {
    val isConnected: Boolean
    suspend fun getAccountInfo(): Result<GoogleDriveAccount>
    suspend fun getOrCreateBackupFolder(): Result<String>
    suspend fun listBackups(): Result<List<GoogleDriveFileEntry>>
    suspend fun uploadBackup(
        backupFile: okio.Path,
        remoteFileName: String,
        onProgress: (bytesWritten: Long, totalBytes: Long) -> Unit
    ): Result<GoogleDriveFileEntry>
    suspend fun downloadBackup(
        fileId: String,
        targetFile: okio.Path,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit
    ): Result<Unit>
    suspend fun disconnect()
}
```

### 5.3 Key Endpoints
1. **Get User Info**:
   - `GET https://www.googleapis.com/oauth2/v3/userinfo`
   - Header: `Authorization: Bearer <access_token>`
   - Returns: `sub` (ID), `name`, `email`, `picture`.

2. **Find or Create Backups Folder**:
   - Query existing folder:
     - `GET https://www.googleapis.com/drive/v3/files?q=name='Toy Collection Backups' and mimeType='application/vnd.google-apps.folder' and trashed=false&fields=files(id,name)`
   - If not found, create folder:
     - `POST https://www.googleapis.com/drive/v3/files`
     - Header: `Authorization: Bearer <access_token>`, `Content-Type: application/json`
     - Body: `{"name": "Toy Collection Backups", "mimeType": "application/vnd.google-apps.folder"}`
     - Returns folder `id`.

3. **List Backups**:
   - `GET https://www.googleapis.com/drive/v3/files?q='<folderId>'+in+parents+and+trashed=false+and+name+contains+'.zip'&fields=files(id,name,mimeType,modifiedTime,size)&orderBy=modifiedTime+desc`
   - Header: `Authorization: Bearer <access_token>`
   - Returns list of `.zip` files with ID, name, modification timestamp, and size.

4. **Upload Backup File (Resumable Upload)**:
   - Step 1: Initiate session:
     - `POST https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable`
     - Header:
       - `Authorization: Bearer <access_token>`
       - `Content-Type: application/json; charset=UTF-8`
       - `X-Upload-Content-Type: application/zip`
       - `X-Upload-Content-Length: <fileSize>`
     - Body: `{"name": "<fileName>", "parents": ["<folderId>"]}`
     - Returns: HTTP 200 with `Location: <uploadSessionUrl>` header.
   - Step 2: Upload bytes:
     - `PUT <uploadSessionUrl>`
     - Stream file bytes and report progress.

5. **Download Backup File**:
   - `GET https://www.googleapis.com/drive/v3/files/<fileId>?alt=media`
   - Header: `Authorization: Bearer <access_token>`
   - Response: Binary stream. Target file is written using `systemFileSystem`.

---

## 6. UI & UX Specification: Dedicated Google Drive Settings Tab

### 6.1 `SettingsTab` Enum Update
In `ToyCollection/composeApp/src/commonMain/kotlin/com/gepetto/toydb/ui/SettingsScreen.kt`:
```kotlin
enum class SettingsTab(val id: String, val titleRes: StringResource) {
    GENERAL("general", Res.string.tab_general),
    BACKUP_RESTORE("backup_restore", Res.string.backup_title),
    SERVER_SYNC("server_sync", Res.string.info_tab_backup),
    DROPBOX("dropbox", Res.string.tab_dropbox),
    GOOGLE_DRIVE("google_drive", Res.string.tab_google_drive),
    CATEGORIES("categories", Res.string.categories)
}
```

### 6.2 Composable Layout: `GoogleDriveSettingsTab.kt`
Location: `ToyCollection/composeApp/src/commonMain/kotlin/com/gepetto/toydb/ui/GoogleDriveSettingsTab.kt`

Cards displayed in the tab:

1. **Google Drive Account Card**:
   - When Disconnected:
     - Icon and explanation text: "Connect your Google account to back up and restore your collections directly to Google Drive."
     - Button: **Connect to Google Drive** (Primary color).
   - When Connected:
     - Status indicator: Green checkmark with user display name and email.
     - Storage target info: "Target folder: `Google Drive / Toy Collection Backups`"
     - Button: **Disconnect** (Outlined error color).

2. **Upload Backup Card**:
   - Button: **Create and Upload Backup to Google Drive**.
   - Input: Optional custom backup label or default timestamp `toy_collection_backup_YYYYMMDD_HHMM.zip`.
   - Progress bar showing collection export and upload stages.
   - Success alert showing toy count, maker count, photo count, and uploaded file name.

3. **Cloud Backups List Card**:
   - Header with **Refresh** button.
   - List of available backup `.zip` files in Google Drive:
     - File name (e.g. `toy_collection_backup_20261012_1430.zip`)
     - Date and time
     - Formatted file size (e.g. `42.5 MB`)
     - Action button: **Restore**
   - Empty state: "No cloud backups found in Google Drive. Create your first backup above."

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
- `tab_google_drive`: "Google Drive"
- `gdrive_connected_as`: "Connected as %1$s (%2$s)"
- `gdrive_not_connected`: "Google Drive is not connected"
- `gdrive_connect_btn`: "Connect to Google Drive"
- `gdrive_disconnect_btn`: "Disconnect"
- `gdrive_create_backup_btn`: "Create and Upload Backup"
- `gdrive_uploading_progress`: "Uploading backup to Google Drive (%1$d%%)..."
- `gdrive_downloading_progress`: "Downloading backup from Google Drive (%1$d%%)..."
- `gdrive_available_backups`: "Available Cloud Backups"
- `gdrive_no_backups`: "No backups found in Google Drive."
- `gdrive_restore_confirm_title`: "Restore Backup from Google Drive"
- `gdrive_restore_confirm_desc`: "Are you sure you want to restore '%1$s'? Current collection data will be replaced."
- `gdrive_backup_success`: "Backup successfully uploaded to Google Drive."
- `gdrive_restore_success`: "Collection successfully restored from Google Drive."
- `gdrive_error`: "Google Drive error: %1$s"

---

## 8. Phase-by-Phase Execution Plan for Agents

### Phase 1: Authentication Core & Settings Storage
- [ ] Define `GoogleDriveConfig.kt` (client IDs, endpoints, redirect URIs, scopes).
- [ ] Implement/Reuse `PkceUtil.kt` in `commonMain` using `org.kotlincrypto.hash.sha2.SHA256`.
- [ ] Add Google Drive settings accessors in `ToyRepository.kt` (`gdrive_refresh_token`, `gdrive_account_id`, `gdrive_account_name`, `gdrive_account_email`).
- [ ] Implement `GoogleDriveAuthManager.kt` (generates Google auth URL, exchanges code for tokens, handles silent token refresh).
- [ ] Unit tests for Google auth URL composition and token exchange serialization.

### Phase 2: Platform Authorization Hooks (Android & Desktop)
- [ ] Desktop: Implement `DesktopGoogleDriveAuthHandler.kt` with a local HTTP loopback server on port `52479`.
- [ ] Android: Add deep-link intent filter in `AndroidManifest.xml` (`com.googleusercontent.apps.${googleClientIdPrefix}:/oauth2redirect`).
- [ ] Android: Implement `onNewIntent` handler in `AppMainActivity.kt` to catch the Google authorization code.
- [ ] Expect/actual platform binder `openGoogleDriveLogin(authUrl: String)`.

### Phase 3: Google Drive API Service (Ktor Client)
- [ ] Implement `GoogleDriveServiceImpl.kt` in `commonMain`.
- [ ] `getAccountInfo()` implementation (`/oauth2/v3/userinfo`).
- [ ] `getOrCreateBackupFolder()` implementation.
- [ ] `listBackups()` implementation.
- [ ] `uploadBackup()` resumable upload implementation.
- [ ] `downloadBackup()` streaming download to temp file.
- [ ] Unit tests for Drive API response models and mock HTTP responses.

### Phase 4: UI & Navigation Integration
- [ ] Add `GOOGLE_DRIVE` to `SettingsTab` in `SettingsScreen.kt`.
- [ ] Create `GoogleDriveSettingsTab.kt` with Account, Backup, and Restore cards.
- [ ] Add Restore confirmation dialog with `BackupRestoreService` manifest inspection.
- [ ] Add all string keys to `strings.xml` in `en`, `de`, `es`, `fr`, `it`, `pt`.
- [ ] Add previews for Light, Dark, and Landscape modes.

### Phase 5: Verification & Quality Assurance
- [ ] Test Desktop build (`./gradlew :composeApp:compileKotlinDesktop`).
- [ ] Test Android assemble (`./gradlew :composeApp:assembleDebug`).
- [ ] Verify clean restore flow with mock or real Google Drive backup archive.
- [ ] Check logs use `GcLog` exclusively.
- [ ] Confirm no Git commits or pushes were made without explicit user instruction.

---

## 9. Living Document Changelog

| Date | Revision | Author | Description of Changes |
| :--- | :--- | :--- | :--- |
| 2026-10-12 | Rev 1 | Antigravity | Initial creation of the living Google Drive Backup Synchronization Plan (Option B with dedicated tab, PKCE OAuth 2.0, Desktop loopback, Android deep links, and Ktor client). |

---

## 10. Bug, Blocker & Issue Tracker

| Issue ID | Phase | Severity | Status | Description | Root Cause | Workaround / Solution |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| *None yet* | - | - | - | No blockers encountered yet. | - | - |
