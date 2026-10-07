<!-- !web -->
Publish your collection and synchronize data across devices using a private server.

### 1. Web Page Creation
- **What it is for**: Converts your collection from the app database into web pages (`.html`) in your data storage folder.
- **How it works**: Tap **Create Pages** to build the catalog files. Once created, this app uploads these pages to your server using SFTP.

### 2. Web Server URL
<!-- /!web -->
<!-- web -->
Synchronize catalog updates and display toy photos using your private web server.

### Web Server URL
<!-- /web -->
- **What it means**: The internet address (such as `http://myserver.com/database`) where your collection files and photos are hosted.
- **How this app uses it**:
  - **Loading Photos**: When the app runs on a device that does not have photo files stored locally, the app uses this URL to download and display toy photos.
  - **Checking Updates**: When you tap **Save & Check Web Updates**, the app checks this URL for newer catalog files and updates your collection without needing server passwords.
<!-- !web -->

### 3. SFTP Information & Credentials
- **What it means**: SFTP (Secure File Transfer Protocol) is the private connection this app uses to transfer files to and from your server storage disk.
- **How this app uses it**:
  - **Upload to Cloud**: The app connects through SFTP to upload your database, photos, and web pages to your server.
  - **Download from Cloud**: The app connects through SFTP to download database updates and photos from your server into this device.
  - **Test Connection**: The app verifies your server settings before transferring files.
- **Settings**:
  - **Server Address & Port**: The network address and port (usually 22) of your server.
<!-- desktop -->
  - **Username & Password / Key**: Your credentials to authenticate this app on the server (password or private key file).
<!-- /desktop -->
<!-- android -->
  - **Username & Password**: Your credentials to authenticate this app on the server.
<!-- /android -->
  - **Remote Folder**: The folder path on the server where the app stores and retrieves collection files.

### 4. Cloud Synchronization Actions
- **Test SFTP Connection**: Confirms your device can connect to your server before transferring files.
- **Upload to Cloud**: Scans local files and sends newly added or changed toys, makers, photos, and web pages to your server.
- **Download from Cloud**: Scans the server and downloads updates to keep this device in sync.
<!-- /!web -->
