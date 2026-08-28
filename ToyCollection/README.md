# Gepetto's Toy Collection Multiplatform (Viewer App)

A Kotlin Multiplatform (KMP) application designed to browse and view Gepetto's extensive collections of toys, including slot cars, trains, static models, kits, and miscellaneous other items.

---

## 📱 Supported Targets

* **Android**: Mobile application.
* **Desktop (JVM)**: Native macOS (DMG/PKG) and Windows (MSI/EXE) applications.
* **Web (Wasm/JS)**: Web-based deployment for browser environments.

---

## 🛠 Tech Stack & Architecture

1. **Compose Multiplatform**: Declarative Compose UI with Material 3 styling.
2. **Circum MVI**: State management powered by the custom `circum` Model-View-Intent library.
3. **Adaptive UI Shell (`gepetto-utils`)**:
   - Uses `GcTheme` to automatically handle system theme states (light/dark mode).
   - Uses `GcAdaptiveScaffold` / `GcNavBar` for dynamic shell rendering. In portrait orientation, it displays a horizontal bottom navigation bar. In landscape orientation, it automatically shifts to a vertical side navigation rail.
4. **Adaptive Navigation (`navigation3`)**:
   - Implements `GcNavDisplay` combined with `GcSceneStrategy` for dynamic screen pane management.
   - On compact screens (portrait phones), it behaves as standard single-pane stack navigation.
   - On medium/expanded screens (tablets or desktop windows), it adapts into two-pane (List-Detail) or three-pane (List-Detail-Extra) layouts.
5. **Ktor Client & kotlinx.serialization**:
   - Connects to a default web host (`https://gepetto.club/database/`) or a user-defined custom endpoint.
   - Downloads database JSON tables and caches them locally.
6. **Coil 3**:
   - Efficient cross-platform image loading, caching, and placeholder rendering.
   - Resolves toy images dynamically using naming conventions (e.g. `car1234.jpg` for slot car refNum 1234).
7. **Localization**:
   - Integrates localized resources (e.g. privacy policies, about pages) dynamically adapting to the system language (English, Portuguese, Spanish, Italian, German, French).

---

## 📂 Core Module Structure

* [**`:shared:common`**](file:///Users/luizvaldetaro/valdetaro/ToyCollection/ToyCollectionMultiplatform/shared/common): Shared common constants, settings models, platform utilities, and custom file systems.
* [**`:feature:toycollection`**](file:///Users/luizvaldetaro/valdetaro/ToyCollection/ToyCollectionMultiplatform/feature/toycollection): The core feature implementation containing:
  - **MVI Intent Processors**: `HomeIntentProcessor`, `CollectionIntentProcessor`, `ToyIntentProcessor`, `MakerIntentProcessor`, `SearchIntentProcessor`.
  - **Data Providers**: Services that orchestrate local fetching and download syncing.
  - **UI Views**: Declarative screens including `HomeView`, `CollectionView`, `MakerView`, `ToyView`, `SearchView`, `WebPageView` (inline web search viewer), and `AboutSheet`.
* [**`:composeApp`**](file:///Users/luizvaldetaro/valdetaro/ToyCollection/ToyCollectionMultiplatform/composeApp): Launcher project configuring application properties, dependencies, launcher entry points (`MainActivity` for Android, `Main` for desktop, and `main.kt` for Web wasmJs), and packaging build tasks.

---

## 🚀 Building & Running

Ensure you have Java JDK 17+ installed.

### Run Desktop (JVM)
```bash
./gradlew :composeApp:run
```

### Build Android APK (Debug)
```bash
./gradlew :composeApp:assembleDebug
```

### Run Web App (Wasm/JS Development Server)
```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```
