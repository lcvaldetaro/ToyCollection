# Gepetto's Toy Collection Workspace

This workspace houses a Kotlin Multiplatform (KMP) application developed to manage and view Gepetto's extensive toy collections (comprising Slot Cars, Model Trains, Static Models, Model Kits, and Miscellaneous items).

---

## 📂 Project Index

### 1. 🗄 ToyDb (Database Manager / CRUD Editor)
A local database coordinator designed to execute CRUD operations, imports, exports, and integrity validations.
* **Targets**: Desktop (macOS, Windows).
* **Key Features**:
  * Interfaces directly with a local SQLite database (`toydb.db`) using JDBC with custom migration versioning.
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
