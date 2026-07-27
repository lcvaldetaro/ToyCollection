# Gepetto's Toy Collection Workspace

This workspace houses two Kotlin Multiplatform (KMP) applications developed to manage and view Gepetto's extensive toy collections (comprising Slot Cars, Model Trains, Static Models, Model Kits, and Miscellaneous items).

---

## 📂 Project Index

### 1. 🔍 [ToyCollectionMultiplatform (Viewer App)](file:///Users/luizvaldetaro/valdetaro/ToyCollection/ToyCollectionMultiplatform)
A multi-device viewer designed for browsing, searching, and inspecting toys and manufacturers.
* **Targets**: Android, Desktop (macOS, Windows), and Web (Wasm/JS).
* **Key Features**:
  * Syncs data dynamically over HTTP from web endpoints (`https://gepetto.club/database/`).
  * Features adaptive layouts that morph based on display width and orientation (e.g., bottom bar navigation on portrait mobile screens vs. a side navigation rail on landscape desktop windows).
  * Uses Material 3 with adaptive multi-pane layouts (single pane on mobile, List-Detail-Extra pane layout on tablet/desktop).
  * Built using KMP, Compose Multiplatform, and the **Circum MVI** architecture library.

---

### 2. 🗄 [ToyDb (Database Manager / CRUD Editor)](file:///Users/luizvaldetaro/valdetaro/ToyCollection/ToyDb)
A local database coordinator designed to execute CRUD operations, imports, exports, and integrity validations.
* **Targets**: Desktop (macOS, Windows) and Android.
* **Key Features**:
  * Interfaces directly with a local SQLite database (`toydb.db`) using JDBC (on Desktop) or `SQLiteOpenHelper` (on Android) with custom migration versioning.
  * Supports importing and exporting database tables from/to JSON files matching the legacy database formats.
  * Auto-updates and manages image files using Okio.
  * Includes integrity validation scripts (`verify_db.py`, `verify_export.py`) to prevent data degradation.

---

## 🎨 Design Guidelines & Naming Conventions

* **Primary Naming Conventions**:
  * Toys are matched with their main images dynamically using `category_settings` lookup.
  * Naming rule: `{image_prefix}{refNum}.*` (e.g., Slot Car `1234` is named `car1234.jpg`, Train `56` is named `tra56.png`).
* **Theme Styling**:
  * Both applications share the `GcTheme` wrapper from the `gepetto-utils` library for seamless system-wide light/dark mode adaptation.
