# ToyDb Database Schema Documentation

This document defines the database schema for **ToyDb**, derived from the JSON files located in [ToyDb/json](file:///Users/luizvaldetaro/valdetaro/ToyDb/json).

The database consists of **7 tables**:
1. **Makers** (`makers.json`): Directory of manufacturers.
2. **Category Settings**: Table mapping toy categories to their file naming conventions and settings.
3. **Slots** (`slots.json`): Slot car collection.
4. **Trains** (`trains.json`): Model trains collection.
5. **Static** (`static.json`): Static models collection.
6. **Kits** (`kits.json`): Model kits collection.
7. **Misc** (`misc.json`): Others collection.

---

## 1. Table Schema: Makers (`makers`)
Source: [makers.json](file:///Users/luizvaldetaro/valdetaro/ToyDb/json/makers.json) (202 records)

This table stores metadata about toy manufacturers.

### Field Mapping

| Field | JSON Type | SQLite/DB Type | Population % | Recommended Constraint / Mappings |
| :--- | :--- | :--- | :--- | :--- |
| `name` | `string` | `TEXT` | 100.0% | Primary Key (Surrogate ID recommended, see note) |
| `country` | `string` | `TEXT` | 99.5% | Nullable |
| `bitmaps` | `string` | `TEXT` | 81.7% | List of space-separated image filenames |
| `bitmapsSize` | `string` | `TEXT` | 81.7% | List of space-separated file sizes (bytes) |
| `bitmapsTimeStamp` | `string` | `TEXT` | 81.7% | List of space-separated timestamps (UNIX epoch ms) |
| `comments` | `string` | `TEXT` | 32.7% | Nullable, max length observed: 10,118 chars |

> [!IMPORTANT]
> **Duplicate Manufacturer Names**: The raw `makers.json` dataset contains 4 duplicate manufacturer names (`MDC`, `Minicraft`, `Paraiso`, and `RMT`). If using `name` as a Primary Key, these duplicates must be resolved or merged during the import. Alternatively, a surrogate autoincrementing integer `id` should be used as the primary key.

---

## 2. Toy Collection Schema (Common Structure)
Sources: `slots.json` (1437 records), `trains.json` (68 records), `static.json` (113 records), `kits.json` (17 records), `misc.json` (8 records)

All five toy collection tables share the **exact same schema structure** of 34 fields. Below is the master definition for these fields.

### Field Mapping

| Field | JSON Type | SQLite Type | Populated % (Slots) | Description / Recommended Mappings |
| :--- | :--- | :--- | :--- | :--- |
| `refNum` | `string` | `INTEGER` | 100.0% | **Primary Key** (Unique within each table) |
| `description` | `string` | `TEXT` | 100.0% | Name or brief description of the toy |
| `makerCombo` | `string` | `TEXT` | 100.0% | Combined manufacturer name (redundant with body/chassis) |
| `scale` | `string` | `TEXT` | 100.0% | Scale (e.g., `'1/32'`, `'1/18'`, `'1/44'`) |
| `factoryCar` | `string` | `TEXT` | 100.0% | Boolean flag: `'y'` = true, `'n'` = false |
| `bodyMaker` | `string` | `TEXT` | 100.0% | Manufacturer of the body / shell |
| `acquired` | `string` | `TEXT` | 95.1% | Purchase details, location, or donor |
| `chassisType` | `string` | `TEXT` | 94.1% | Configuration of the chassis (e.g., `'in line P'`) |
| `chassisMaker` | `string` | `TEXT` | 93.9% | Manufacturer of the chassis |
| `condition` | `string` | `TEXT` | 92.2% | Condition code (e.g., `'C7'`, `'C9/C10'`) |
| `picture` | `string` | `TEXT` | 92.1% | Primary picture filename |
| `pictureSize` | `string` | `INTEGER` | 92.1% | Size of the primary picture in bytes |
| `pictureTimeStamp`| `string` | `INTEGER` | 92.1% | UNIX timestamp of primary picture modification |
| `color` | `string` | `TEXT` | 91.4% | Color(s) of the toy |
| `bitmaps` | `string` | `TEXT` | 91.4% | Space-separated list of secondary image filenames |
| `bitmapsSize` | `string` | `TEXT` | 91.4% | Space-separated list of secondary image sizes |
| `bitmapsTimeStamp`| `string` | `TEXT` | 91.4% | Space-separated list of secondary image timestamps |
| `motorMaker` | `string` | `TEXT` | 91.0% | Manufacturer of the motor |
| `hasPicture` | `string` | `TEXT` | 88.4% | Boolean flag: `'y'` = true, `'n'` = false |
| `motorDetails` | `string` | `TEXT` | 85.0% | Details about the motor (e.g., `'S-Can'`) |
| `catalogNumber` | `string` | `TEXT` | 69.5% | Manufacturer catalog/model number |
| `comments` | `string` | `TEXT` | 42.0% | Collector comments |
| `majorWork` | `string` | `TEXT` | 11.0% | Restoration or major modification work description |
| `minorWork` | `string` | `TEXT` | 10.4% | Minor tuning or repair work description |
| `repro` | `string` | `TEXT` | 9.2% | Reproduction parts flag: `'y'` = yes, `'r'` = repro, `''` = no |
| `value` | `string` | `REAL` | 97.8% | Estimated collector value (Decimal/Float) |
| `amountPaid` | `string` | `REAL` | 99.2% | Cost of acquisition (Decimal/Float) |
| `amountSold` | `string` | `TEXT` | 4.3% | Price sold. **Note:** contains text like `'Traded'` / `'traded'` |
| `traded` | `string` | `TEXT` | 5.7% | Details about trading transactions |
| `buy` | `string` | `TEXT` | 3.8% | Buy intent or details |
| `maintenance` | `string` | `TEXT` | 2.4% | Scheduled or past maintenance logs |
| `toMake` | `string` | `TEXT` | 1.7% | Plans to construct or details |
| `detail` | `string` | `TEXT` | 0.1% | Fine detail decorations or decals |
| `boxed` | `string` | `TEXT` | 21.3% | Boolean flag: `'y'` = boxed, `'n'` = unboxed |

---

## 3. General Settings & Category Image Prefix Rules

To handle dynamic image file lookup, a **Category Settings** schema is introduced to store rules that match toys in each category to their corresponding main image files.

### The Prefix Rule
For the main picture of a toy:
- If the toy category has a defined prefix, and the toy's `hasPicture` is `'y'` (or `picture` field is populated), the software will look for the image file matching:
  `"{prefix}{refNum}.*"` (e.g., `"car1234.jpg"`, `"tra56.png"`, etc.)
- **Note**: This naming rule **only** applies to the primary/main picture of a toy. Secondary images listed in the space-separated `bitmaps` field are not automatically named.

| Toy Category | Internal Key (`toy_type`) | Image Filename Prefix | File Example |
| :--- | :--- | :--- | :--- |
| **Slots** (Slot Cars) | `slot` / `slots` | `"car"` | `car1234.*` |
| **Trains** (Model Trains) | `train` / `trains` | `"tra"` | `tra56.*` |
| **Static** (Static Models) | `static` | `"sta"` | `sta789.*` |
| **Kits** (Model Kits) | `kit` / `kits` | `"pla"` | `pla12.*` |
| **Misc** (Others) | `misc` | `"mis"` | `mis34.*` |

### Settings Table Schema (`category_settings`)
This configuration metadata should be stored in a permanent settings table inside the database to avoid hardcoding naming rules in the KMP app codebase.

| Field | DB Type | Constraint | Description |
| :--- | :--- | :--- | :--- |
| `category` | `TEXT` | `PRIMARY KEY` | Category key (e.g., `'slot'`, `'train'`) |
| `image_prefix` | `TEXT` | `NOT NULL` | Prefix used for main image files |
| `label` | `TEXT` | `NOT NULL` | Display name of the category |

---

## 4. Relational Database Design Insights

When migrating this JSON dataset to a relational database (such as SQLite in a Kotlin Multiplatform app), there are three key architectural improvements that should be considered:

### A. Primary Key Overlaps
* **Observation**: `refNum` is a clean incrementing integer unique *within* each individual JSON file, but overlaps across tables (e.g., `refNum = 1` exists in all tables).
* **Direct Approach**: Keep 5 separate tables, using `refNum` as the Primary Key for each.
* **Unified Approach**: If you want to merge them into a single table (e.g. `toys`), define a composite Primary Key of `(toy_type, refNum)`, or create a surrogate `id INTEGER PRIMARY KEY AUTOINCREMENT`.

### B. Maker/Manufacturer Relationships
* **Observation**: `makerCombo` represents a combination of the body and chassis makers, e.g., `"MRRC/A2M"` where `bodyMaker = "A2M"` and `chassisMaker = "MRRC"`.
* **Normalization**: Instead of storing duplicate strings, `bodyMaker` and `chassisMaker` can be Foreign Keys referencing `makers.name` or `makers.id`.

### C. Multi-Image Relationships (Bitmaps)
* **Observation**: The `bitmaps`, `bitmapsSize`, and `bitmapsTimeStamp` columns store space-separated values representing multiple secondary images. For example:
  * `bitmaps`: `"corgitoys.jpg lotus.jpg"`
  * `bitmapsSize`: `"508154 2563"`
  * `bitmapsTimeStamp`: `"1781632881000 1781632886000"`
* **Normalization**: Create a separate `toy_images` table to store these as distinct rows, establishing a one-to-many relationship.

---

## 5. SQL DDL Definitions (SQLite Dialect)

Below are the two recommended ways to translate this database schema into SQL.

### Option 1: Clean, Normalized Schema (Recommended)
This approach normalizes the multi-image bitmaps, handles duplicate makers with an autoincrementing ID, and links body/chassis makers cleanly using foreign keys.

```sql
-- 1. Category Settings Table (Stores image prefix rules)
CREATE TABLE IF NOT EXISTS category_settings (
    category TEXT PRIMARY KEY,
    image_prefix TEXT NOT NULL,
    label TEXT NOT NULL
);

-- Prepopulate Category Settings Table
INSERT OR REPLACE INTO category_settings (category, image_prefix, label) VALUES
('slot', 'car', 'Slot Cars'),
('train', 'tra', 'Model Trains'),
('static', 'sta', 'Static Models'),
('kit', 'pla', 'Model Kits'),
('misc', 'mis', 'Others');

-- 2. Manufacturers Table
CREATE TABLE IF NOT EXISTS makers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    country TEXT,
    comments TEXT
);

-- 3. Master Toy Table
CREATE TABLE IF NOT EXISTS toys (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    toy_type TEXT NOT NULL REFERENCES category_settings(category),
    ref_num INTEGER NOT NULL,
    description TEXT NOT NULL,
    scale TEXT NOT NULL,
    catalog_number TEXT,
    color TEXT,
    condition TEXT,
    comments TEXT,
    
    -- Relationships
    body_maker_id INTEGER REFERENCES makers(id),
    chassis_maker_id INTEGER REFERENCES makers(id),
    chassis_type TEXT,
    motor_maker_id INTEGER REFERENCES makers(id),
    motor_details TEXT,
    
    -- Status & Restoration Details
    factory_car INTEGER NOT NULL DEFAULT 0, -- Boolean (0 or 1)
    boxed INTEGER NOT NULL DEFAULT 0,       -- Boolean
    repro TEXT,                             -- 'y', 'r', or NULL
    major_work TEXT,
    minor_work TEXT,
    maintenance TEXT,
    to_make TEXT,
    detail TEXT,
    
    -- Financial Details
    value REAL,
    amount_paid REAL,
    amount_sold TEXT, -- Stored as text to accommodate values like 'Traded'
    traded TEXT,
    buy TEXT,
    acquired TEXT,
    
    -- Primary Picture
    picture TEXT,
    picture_size INTEGER,
    picture_timestamp INTEGER,
    has_picture INTEGER NOT NULL DEFAULT 0, -- Boolean
    
    UNIQUE(toy_type, ref_num)
);

-- 4. Secondary Images Table (Resolves space-separated bitmaps list)
CREATE TABLE IF NOT EXISTS toy_images (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    toy_id INTEGER NOT NULL REFERENCES toys(id) ON DELETE CASCADE,
    filename TEXT NOT NULL,
    size INTEGER,
    timestamp INTEGER
);

-- 5. Maker Images Table
CREATE TABLE IF NOT EXISTS maker_images (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    maker_id INTEGER NOT NULL REFERENCES makers(id) ON DELETE CASCADE,
    filename TEXT NOT NULL,
    size INTEGER,
    timestamp INTEGER
);
```

### Option 2: Direct Mapping Schema
This approach matches the exact structure of the JSON documents, allowing for easy import/export logic without conversion overhead.

```sql
-- Category Settings Table
CREATE TABLE IF NOT EXISTS category_settings (
    category TEXT PRIMARY KEY,
    image_prefix TEXT NOT NULL,
    label TEXT NOT NULL
);

-- Prepopulate Category Settings Table
INSERT OR REPLACE INTO category_settings (category, image_prefix, label) VALUES
('slots', 'car', 'Slot Cars'),
('trains', 'tra', 'Model Trains'),
('static', 'sta', 'Static Models'),
('kits', 'pla', 'Model Kits'),
('misc', 'mis', 'Others');

-- Manufacturers Table
CREATE TABLE IF NOT EXISTS makers (
    name TEXT PRIMARY KEY,
    country TEXT,
    bitmaps TEXT,
    bitmapsSize TEXT,
    bitmapsTimeStamp TEXT,
    comments TEXT
);

-- Define column list for creating identical toy tables
-- Create the 5 individual tables: slots, trains, static, kits, misc
CREATE TABLE IF NOT EXISTS slots (
    refNum INTEGER PRIMARY KEY,
    description TEXT,
    makerCombo TEXT,
    scale TEXT,
    factoryCar TEXT,
    bodyMaker TEXT,
    acquired TEXT,
    chassisType TEXT,
    chassisMaker TEXT,
    condition TEXT,
    picture TEXT,
    pictureSize INTEGER,
    pictureTimeStamp INTEGER,
    color TEXT,
    bitmaps TEXT,
    bitmapsSize TEXT,
    bitmapsTimeStamp TEXT,
    motorMaker TEXT,
    hasPicture TEXT,
    motorDetails TEXT,
    catalogNumber TEXT,
    comments TEXT,
    majorWork TEXT,
    minorWork TEXT,
    repro TEXT,
    value REAL,
    amountPaid REAL,
    amountSold TEXT,
    traded TEXT,
    buy TEXT,
    maintenance TEXT,
    toMake TEXT,
    detail TEXT,
    boxed TEXT
);

-- (Create similar tables for trains, static, kits, and misc with the same structure)
```
