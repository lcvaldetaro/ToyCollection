# Gepetto's Toy Collection Workspace

This workspace houses a Kotlin Multiplatform (KMP) application developed to manage and view Gepetto's extensive toy collections (comprising Slot Cars, Model Trains, Static Models, Model Kits, and Miscellaneous items).

---

## 📂 Project Index

### 1. 🗄 ToyDb (Database Manager / CRUD Editor)
A database coordinator designed to execute CRUD operations, imports, exports, and integrity validations.
* **Targets**: Desktop (macOS, Windows), Android, Web.
* **Key Features**:
  * Interfaces directly with a local SQLite database (`toydb.db`) using JDBC (Desktop), Android SQLite (Android), and sql.js WebAssembly with IndexedDB snapshot persistence (Web).
  * Supports importing and exporting database tables from/to JSON files matching the legacy database formats.
  * Auto-updates and manages image files using Okio and Coil.
  * Remote HTTP synchronization for automatic updates from server backups.
  * Includes integrity validation scripts (`verify_db.py`, `verify_export.py`) to prevent data degradation.

---

## 🌐 Web Version Deployment & Hosting

### Building the Web Distribution
To build the optimized production WebAssembly application:
```bash
./gradlew :composeApp:wasmJsBrowserDistribution
```
The output files will be in `composeApp/build/dist/wasmJs/productionExecutable/`.

### Hosting Requirements
Host the distribution files under `https://gepetto.club/database/web/`:
* **MIME Types**: The web server MUST serve `.wasm` files with `Content-Type: application/wasm`.
* **Compression**: Enable gzip or brotli compression on the web server.
* **Trailing Slash Redirect**: All asset paths in the web distribution are relative. The web server MUST redirect `https://gepetto.club/database/web` to `https://gepetto.club/database/web/`.
* **Caching**: Serve `index.html` and `composeApp.js` with `Cache-Control: no-cache` so users receive immediate updates upon new releases.
* **Host Name**: Use `https://gepetto.club` (canonical domain without `www`).

---

## 🎨 Design Guidelines & Naming Conventions

* **Primary Naming Conventions**:
  * Toys are matched with their main images dynamically using `category_settings` lookup.
  * Naming rule: `{image_prefix}{refNum}.*` (e.g., Slot Car `1234` is named `car1234.jpg`, Train `56` is named `tra56.png`).
* **Theme Styling**:
  * Both applications share the `GcTheme` wrapper from the `gepetto-utils` library for seamless system-wide light/dark mode adaptation.
