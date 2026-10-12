# Microsoft OneDrive Backup Synchronization Plan: Toy Collection (Living Document)

> **Document Status**: Living Roadmap & Technical Specification  
> **Target Application**: Toy Collection Manager (`/Users/luizvaldetaro/valdetaro/ToyCollection`)  
> **Workspace**: `/Users/luizvaldetaro/valdetaro`  
> **Document Location**: `ToyCollection/.agents/ONEDRIVE_SYNC_PLAN.md`  
> **Last Updated**: 2026-10-12 (Rev 1 — Initial technical plan for Option B backup sync with dedicated tab)  
> **Current Phase**: Phase 0 (Planning & Setup)

---

## Table of Contents
1. [Executive Summary & Architecture Scope](#1-executive-summary--architecture-scope)
2. [Microsoft Entra Admin Center Registration Guide](#2-microsoft-entra-admin-center-registration-guide)
3. [Deep Link & Platform Authorization Specification](#3-deep-link--platform-authorization-specification)
   - 3.1 [Android Deep Link & Manifest Setup](#31-android-deep-link--manifest-setup)
   - 3.2 [Desktop Localhost Loopback Setup](#32-desktop-localhost-loopback-setup)
4. [OAuth 2.0 with PKCE & Token Lifecycle](#4-oauth-20-with-pkce--token-lifecycle)
5. [Microsoft Graph API Service Specification (KMP / Ktor)](#5-microsoft-graph-api-service-specification-kmp--ktor)
6. [UI & UX Specification: Dedicated OneDrive Settings Tab](#6-ui--ux-specification-dedicated-onedrive-settings-tab)
7. [Localization Requirements (6 Languages)](#7-localization-requirements-6-languages)
8. [Phase-by-Phase Execution Plan for Agents](#8-phase-by-phase-execution-plan-for-agents)
9. [Living Document Changelog](#9-living-document-changelog)
10. [Bug, Blocker & Issue Tracker](#10-bug-blocker--issue-tracker)

---

## 1. Executive Summary & Architecture Scope

### 1.1 Goal
Provide reliable, user-friendly cloud backup synchronization for the **Toy Collection** application using Microsoft OneDrive.

### 1.2 Selected Design: Option B (Snapshot Archive Sync)
- **Backup Mechanism**:
  - The app creates standard `.zip` backup archives using the existing `BackupRestoreService`.
  - The archive packages `manifest.json`, `photos.json`, collection databases (`carmaker.json`, category lists), and photos into one archive.
  - The archive is uploaded directly to the OneDrive special application folder (`special/approot`), which maps to `/Apps/Toy Collection Manager/Backups/`.
- **Restore Mechanism**:
  - The app queries Microsoft Graph API for available `.zip` archives in the application folder.
  - The user selects a backup archive to restore.
  - The app downloads the archive to a temporary file, validates the manifest, shows counts of toys, makers, and photos in a confirmation dialog, and restores the collection.

### 1.3 UI Placement
- A dedicated **OneDrive** tab in `SettingsScreen.kt` alongside `General`, `Backup & Restore`, `Server Sync (SFTP)`, `Dropbox`, `Google Drive`, and `Categories`.

### 1.4 Kotlin Multiplatform (KMP) Architecture
- Microsoft Graph API v1.0 runs completely on standard HTTPS REST endpoints.
- All HTTP requests will use the existing `createToyHttpClient()` Ktor client in `commonMain`.
- No Microsoft Authentication Library (MSAL) Java/Android-only SDK is used. This preserves full multiplatform support across Desktop (macOS M1/Intel, Windows) and Android.

---

## 2. Microsoft Entra Admin Center Registration Guide

> [!IMPORTANT]
> **Single Registration Rule**: Only the developer registers **one app** in the Microsoft Entra admin center (formerly Azure Active Directory). End users do not register apps. End users only log in with their personal Microsoft accounts and click "Accept".

Follow these exact steps to register the application:

1. **Open Microsoft Entra Admin Center**:
   - Navigate to: `https://entra.microsoft.com/` (or `https://portal.azure.com/#blade/Microsoft_AAD_RegisteredApps/ApplicationsListBlade`).
   - Sign in with your developer Microsoft account.

2. **Register a New Application**:
   - In the left sidebar, navigate to **Identity** > **Applications** > **App registrations**.
   - Click **New registration** at the top.
   - **Name**: Enter `Gepetto Toy Collection Manager`.
   - **Supported account types**:
     - Select **Personal Microsoft accounts only** (or **Accounts in any organizational directory and personal Microsoft accounts**).
     *(This ensures any user with an `@outlook.com`, `@hotmail.com`, or `@live.com` account can sign in).*
   - **Redirect URI (optional)**:
     - Platform dropdown: Select **Public client/native (mobile & desktop)**.
     - URI: Enter `http://localhost:52480/oauth/callback`.
   - Click **Register**.

3. **Record Application (Client) ID**:
   - On the application **Overview** page, copy the **Application (client) ID** (a GUID, for example: `a1b2c3d4-e5f6-7890-abcd-ef1234567890`).
   *(Note: With PKCE on public native clients, no Client Secret is required or stored).*

4. **Configure Redirect URIs**:
   - In the left menu, select **Authentication**.
   - Under **Platform configurations**, click **Add a platform** (or look at the existing **Mobile and desktop applications** section).
   - Ensure the following redirect URIs are present:
     1. **Desktop Loopback URI**: `http://localhost:52480/oauth/callback`
     2. **Android Custom Scheme URI**: `toydb://onedrive-callback` (or `msal<CLIENT_ID>://auth`)
   - Under **Advanced settings**:
     - Ensure **Allow public client flows** is set to **Yes** (Enable the following mobile and desktop flows).
   - Click **Save**.

5. **Configure API Permissions (Scopes)**:
   - In the left menu, select **API permissions**.
   - Click **Add a permission** > **Microsoft Graph** > **Delegated permissions**.
   - Search for and select:
     - `Files.ReadWrite.AppFolder` *(Gives read and write access to the app's own dedicated folder in OneDrive without granting access to the user's personal documents).*
     - `User.Read` *(Enables retrieving display name and email address for the account card).*
     - `offline_access` *(Enables receiving a refresh token for perpetual background renewal).*
   - Click **Add permissions**.

---

## 3. Deep Link & Platform Authorization Specification

### 3.1 Android Deep Link & Manifest Setup

To receive the OAuth authorization code back from the web browser on Android:

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
       <!-- OneDrive OAuth callback deep link -->
       <intent-filter>
           <action android:name="android.intent.action.VIEW" />
           <category android:name="android.intent.category.DEFAULT" />
           <category android:name="android.intent.category.BROWSABLE" />
           <data android:scheme="toydb" android:host="onedrive-callback" />
       </intent-filter>
   </activity>
   ```

2. **Handle Incoming Intent in `AppMainActivity.kt`**:
   In `AppMainActivity.kt`:
   - Override `onNewIntent(intent: Intent)`:
     ```kotlin
     override fun onNewIntent(intent: android.content.Intent) {
         super.onNewIntent(intent)
         handleOneDriveCallback(intent)
     }
     ```
   - In `onCreate(savedInstanceState: Bundle?)`, call `handleOneDriveCallback(intent)`.
   - `handleOneDriveCallback(intent)` extracts `intent.data?.getQueryParameter("code")` and forwards it to `OneDriveAuthManager.onAuthCodeReceived(code)`.

### 3.2 Desktop Localhost Loopback Setup

On macOS and Windows:
1. No OS deep link is required.
2. The desktop app starts a lightweight temporary HTTP server listening on `http://127.0.0.1:52480/oauth/callback`.
3. The app opens the system browser with the Microsoft authorization URL.
4. When the user approves access, Microsoft redirects the browser to `http://localhost:52480/oauth/callback?code=...`.
5. The local server:
   - Extracts the `code` query parameter.
   - Sends an HTTP 200 response with a user-friendly page:
     ```html
     <!DOCTYPE html>
     <html>
       <head><title>Authorization Successful</title><meta charset="utf-8"></head>
       <body style="font-family: sans-serif; text-align: center; padding-top: 50px;">
         <h2>Toy Collection is connected to Microsoft OneDrive!</h2>
         <p>You can close this tab and return to the application.</p>
       </body>
     </html>
     ```
   - Shuts down the local server.
   - Forwards the authorization code to complete the token exchange.

---

## 4. OAuth 2.0 with PKCE & Token Lifecycle

### 4.1 Proof Key for Code Exchange (PKCE)
Microsoft Identity platform requires PKCE for native public desktop and mobile clients (RFC 7636).

1. **Code Verifier**:
   - Cryptographically random string between 43 and 128 characters from `[A-Z]`, `[a-z]`, `[0-9]`, `-`, `_`, `.`, `~`.
2. **Code Challenge**:
   - SHA-256 hash of `code_verifier`.
   - Base64URL-encoded (without padding `=`).
   - Hash is computed using `org.kotlincrypto.hash.sha2.SHA256` in `commonMain`.

### 4.2 Authorization Request
- Browser opens:
  ```
  https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize?client_id=<CLIENT_ID>&response_type=code&redirect_uri=<ENCODED_REDIRECT_URI>&scope=Files.ReadWrite.AppFolder%20User.Read%20offline_access&code_challenge=<CHALLENGE>&code_challenge_method=S256&prompt=select_account
  ```
- `scope=offline_access` ensures Microsoft returns a `refresh_token`.

### 4.3 Token Exchange
- POST request to: `https://login.microsoftonline.com/consumers/oauth2/v2.0/token`
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
    "token_type": "Bearer",
    "scope": "Files.ReadWrite.AppFolder User.Read",
    "expires_in": 3600,
    "access_token": "EwB...",
    "refresh_token": "M.R3..."
  }
  ```

### 4.4 Token Storage & Silent Auto-Refresh
- The `refresh_token`, `account_id`, `account_name`, and `account_email` are saved in `ToyRepository` under:
  - `onedrive_refresh_token`
  - `onedrive_account_id`
  - `onedrive_account_name`
  - `onedrive_account_email`
- The `access_token` and expiry timestamp are kept in memory.
- Before each Microsoft Graph API call, if the access token is missing or expired, the app calls:
  - POST `https://login.microsoftonline.com/consumers/oauth2/v2.0/token`
  - Parameters:
    - `grant_type`: `refresh_token`
    - `refresh_token`: `<STORED_REFRESH_TOKEN>`
    - `client_id`: `<CLIENT_ID>`
    - `scope`: `Files.ReadWrite.AppFolder User.Read offline_access`
- This refresh happens automatically in the background. The user logs in only once.

---

## 5. Microsoft Graph API Service Specification (KMP / Ktor)

The service is located in `composeApp/src/commonMain/kotlin/com/gepetto/toydb/service/OneDriveService.kt`.

### 5.1 Data Models
```kotlin
data class OneDriveAccount(
    val id: String,
    val displayName: String,
    val email: String
)

data class OneDriveFileEntry(
    val id: String,
    val name: String,
    val lastModified: String,
    val sizeBytes: Long
)
```

### 5.2 Interface Contract
```kotlin
interface OneDriveService {
    val isConnected: Boolean
    suspend fun getAccountInfo(): Result<OneDriveAccount>
    suspend fun listBackups(): Result<List<OneDriveFileEntry>>
    suspend fun uploadBackup(
        backupFile: okio.Path,
        remoteFileName: String,
        onProgress: (bytesWritten: Long, totalBytes: Long) -> Unit
    ): Result<OneDriveFileEntry>
    suspend fun downloadBackup(
        fileId: String,
        targetFile: okio.Path,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit
    ): Result<Unit>
    suspend fun disconnect()
}
```

### 5.3 Key Endpoints
1. **Get Profile Info**:
   - `GET https://graph.microsoft.com/v1.0/me`
   - Header: `Authorization: Bearer <access_token>`
   - Returns: `displayName`, `userPrincipalName` / `mail`, `id`.

2. **List Backups in App Folder**:
   - `GET https://graph.microsoft.com/v1.0/me/drive/special/approot:/Backups:/children`
   - Header: `Authorization: Bearer <access_token>`
   - Returns list of children items with `id`, `name`, `size`, `lastModifiedDateTime`.
   - If `/Backups` does not exist yet (HTTP 404), return an empty list.

3. **Upload Backup File (Upload Session)**:
   - For backup files (> 4MB), Microsoft Graph requires an upload session:
     - Step 1: Create session:
       - `POST https://graph.microsoft.com/v1.0/me/drive/special/approot:/Backups/<fileName>:/createUploadSession`
       - Header: `Authorization: Bearer <access_token>`, `Content-Type: application/json`
       - Body: `{"item": {"@microsoft.graph.conflictBehavior": "rename"}}`
       - Returns: JSON with `uploadUrl`.
     - Step 2: Upload bytes:
       - `PUT <uploadUrl>`
       - Header: `Content-Length: <fileSize>`, `Content-Range: bytes 0-<fileSize - 1>/<fileSize>`
       - Stream file bytes and report progress.

4. **Download Backup File**:
   - `GET https://graph.microsoft.com/v1.0/me/drive/items/<fileId>/content`
   - Header: `Authorization: Bearer <access_token>`
   - Ktor automatically follows the 302 redirect to the temporary binary download location and writes bytes directly to target file using `systemFileSystem`.

---

## 6. UI & UX Specification: Dedicated OneDrive Settings Tab

### 6.1 `SettingsTab` Enum Update
In `ToyCollection/composeApp/src/commonMain/kotlin/com/gepetto/toydb/ui/SettingsScreen.kt`:
```kotlin
enum class SettingsTab(val id: String, val titleRes: StringResource) {
    GENERAL("general", Res.string.tab_general),
    BACKUP_RESTORE("backup_restore", Res.string.backup_title),
    SERVER_SYNC("server_sync", Res.string.info_tab_backup),
    DROPBOX("dropbox", Res.string.tab_dropbox),
    GOOGLE_DRIVE("google_drive", Res.string.tab_google_drive),
    ONEDRIVE("onedrive", Res.string.tab_onedrive),
    CATEGORIES("categories", Res.string.categories)
}
```

### 6.2 Composable Layout: `OneDriveSettingsTab.kt`
Location: `ToyCollection/composeApp/src/commonMain/kotlin/com/gepetto/toydb/ui/OneDriveSettingsTab.kt`

Cards displayed in the tab:

1. **OneDrive Account Card**:
   - When Disconnected:
     - Icon and explanation text: "Connect your Microsoft account to back up and restore your collections directly to OneDrive."
     - Button: **Connect to OneDrive** (Primary color).
   - When Connected:
     - Status indicator: Green checkmark with user display name and email.
     - Storage target info: "Target folder: `OneDrive / Apps / Toy Collection Manager / Backups`"
     - Button: **Disconnect** (Outlined error color).

2. **Upload Backup Card**:
   - Button: **Create and Upload Backup to OneDrive**.
   - Input: Optional custom backup label or default timestamp `toy_collection_backup_YYYYMMDD_HHMM.zip`.
   - Progress bar showing collection export and upload stages.
   - Success alert showing toy count, maker count, photo count, and uploaded file name.

3. **Cloud Backups List Card**:
   - Header with **Refresh** button.
   - List of available backup `.zip` files in OneDrive:
     - File name (e.g. `toy_collection_backup_20261012_1430.zip`)
     - Date and time
     - Formatted file size (e.g. `42.5 MB`)
     - Action button: **Restore**
   - Empty state: "No cloud backups found in OneDrive. Create your first backup above."

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
- `tab_onedrive`: "OneDrive"
- `onedrive_connected_as`: "Connected as %1$s (%2$s)"
- `onedrive_not_connected`: "OneDrive is not connected"
- `onedrive_connect_btn`: "Connect to OneDrive"
- `onedrive_disconnect_btn`: "Disconnect"
- `onedrive_create_backup_btn`: "Create and Upload Backup"
- `onedrive_uploading_progress`: "Uploading backup to OneDrive (%1$d%%)..."
- `onedrive_downloading_progress`: "Downloading backup from OneDrive (%1$d%%)..."
- `onedrive_available_backups`: "Available Cloud Backups"
- `onedrive_no_backups`: "No backups found in OneDrive."
- `onedrive_restore_confirm_title`: "Restore Backup from OneDrive"
- `onedrive_restore_confirm_desc`: "Are you sure you want to restore '%1$s'? Current collection data will be replaced."
- `onedrive_backup_success`: "Backup successfully uploaded to OneDrive."
- `onedrive_restore_success`: "Collection successfully restored from OneDrive."
- `onedrive_error`: "OneDrive error: %1$s"

---

## 8. Phase-by-Phase Execution Plan for Agents

### Phase 1: Authentication Core & Settings Storage
- [ ] Define `OneDriveConfig.kt` (client ID, endpoints, redirect URIs, scopes).
- [ ] Implement/Reuse `PkceUtil.kt` in `commonMain` using `org.kotlincrypto.hash.sha2.SHA256`.
- [ ] Add OneDrive settings accessors in `ToyRepository.kt` (`onedrive_refresh_token`, `onedrive_account_id`, `onedrive_account_name`, `onedrive_account_email`).
- [ ] Implement `OneDriveAuthManager.kt` (generates Microsoft auth URL, exchanges code for tokens, handles silent token refresh).
- [ ] Unit tests for Microsoft auth URL composition and token exchange serialization.

### Phase 2: Platform Authorization Hooks (Android & Desktop)
- [ ] Desktop: Implement `DesktopOneDriveAuthHandler.kt` with a local HTTP loopback server on port `52480`.
- [ ] Android: Add deep-link intent filter in `AndroidManifest.xml` (`toydb://onedrive-callback`).
- [ ] Android: Implement `onNewIntent` handler in `AppMainActivity.kt` to catch the Microsoft authorization code.
- [ ] Expect/actual platform binder `openOneDriveLogin(authUrl: String)`.

### Phase 3: Microsoft Graph API Service (Ktor Client)
- [ ] Implement `OneDriveServiceImpl.kt` in `commonMain`.
- [ ] `getAccountInfo()` implementation (`/v1.0/me`).
- [ ] `listBackups()` implementation (`/me/drive/special/approot:/Backups:/children`).
- [ ] `uploadBackup()` upload session implementation.
- [ ] `downloadBackup()` streaming download to temp file.
- [ ] Unit tests for Microsoft Graph response models and mock HTTP responses.

### Phase 4: UI & Navigation Integration
- [ ] Add `ONEDRIVE` to `SettingsTab` in `SettingsScreen.kt`.
- [ ] Create `OneDriveSettingsTab.kt` with Account, Backup, and Restore cards.
- [ ] Add Restore confirmation dialog with `BackupRestoreService` manifest inspection.
- [ ] Add all string keys to `strings.xml` in `en`, `de`, `es`, `fr`, `it`, `pt`.
- [ ] Add previews for Light, Dark, and Landscape modes.

### Phase 5: Verification & Quality Assurance
- [ ] Test Desktop build (`./gradlew :composeApp:compileKotlinDesktop`).
- [ ] Test Android assemble (`./gradlew :composeApp:assembleDebug`).
- [ ] Verify clean restore flow with mock or real OneDrive backup archive.
- [ ] Check logs use `GcLog` exclusively.
- [ ] Confirm no Git commits or pushes were made without explicit user instruction.

---

## 9. Living Document Changelog

| Date | Revision | Author | Description of Changes |
| :--- | :--- | :--- | :--- |
| 2026-10-12 | Rev 1 | Antigravity | Initial creation of the living Microsoft OneDrive Backup Synchronization Plan (Option B with dedicated tab, PKCE OAuth 2.0, Desktop loopback, Android deep links, and Ktor client). |

---

## 10. Bug, Blocker & Issue Tracker

| Issue ID | Phase | Severity | Status | Description | Root Cause | Workaround / Solution |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| *None yet* | - | - | - | No blockers encountered yet. | - | - |
