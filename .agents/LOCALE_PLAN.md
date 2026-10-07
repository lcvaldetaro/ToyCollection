# Toy Collection: Language Locale Selection Implementation Plan

**Target Document**: `ToyCollection/.agents/LOCALE_PLAN.md`  
**Document Version**: 1.0.0  
**Target Module**: `:composeApp` (`commonMain`, `desktopMain`, `androidMain`, `wasmJsMain`)  
**Document Nature**: **LIVE DOCUMENT** — Maintained and updated in `ToyCollection/.agents/LOCALE_PLAN.md` throughout execution.  
**Specification Standard**: ASD-STE100 Simplified Technical English & AGENTS.md Multiplatform Rules.

---

## 1. Document Control & Living History

> [!IMPORTANT]
> This document is a **living document**. Every executing agent must update this document.
> When you make a change, record it in **Section 1.1 (Change Log)**.
> When you find a bug or issue, record it in **Section 1.2 (Bug & Issue Tracker)**.
> When you finish a task, check off the item in **Section 7 (Completion Checklist)**.

### 1.1 Change Log (Living Record)

| Date | Agent / Author | Action / Change Summary | Affected Components | Status |
| :--- | :--- | :--- | :--- | :--- |
| 2026-10-07 | Antigravity | Initial plan creation based on Lap Counter & Race Director architecture. | Full Plan | Baseline |
| 2026-10-07 | Antigravity | Implemented Phases 1-9: strings.xml (6 languages), LocaleHelper (expect/actual), Language.kt, ToyRepository, GeneralSettingsTab, SettingsScreen, ToyDbNavigation, Main.kt (Desktop & Web), AppMainActivity, InfoScreen, build.gradle.kts, LanguageModelTest, and ToyRepositoryLanguageTest. Verified compiles on Desktop, Android, and WasmJS, and all desktopTests passed with 0 errors. | All components | Complete & Verified |
| 2026-10-07 | Antigravity | Phase 10: Audited all top navigation and screen icons/labels across all 6 languages. Added localized category short labels (nav_cat_*) and full display labels (cat_label_*) across en, pt, es, fr, it, de. Added CategoryLocalization.kt with rememberCategoryShortLabelResolver and rememberCategoryLabelResolver. Updated ToyDbNavigation.kt, DashboardScreen.kt, ExplorerScreen.kt, HomeDestination.kt, ToyForm.kt, MakerForm.kt, and SyncImage.kt to use localized strings and multilingual keyword resolution for icon vectors and drawables. Added CategoryLocalizationTest suite (4 tests passing). All Desktop, Android, and WasmJS targets compile cleanly. | strings.xml, CategoryLocalization.kt, ToyDbNavigation.kt, DashboardScreen.kt, ExplorerScreen.kt, HomeDestination.kt, ToyForm.kt, MakerForm.kt, SyncImage.kt, CategoryLocalizationTest.kt | Complete & Verified |
| 2026-10-07 | Antigravity | Phase 11: Completed comprehensive localization audit across the entire codebase. Verified 0 hardcoded user-facing strings in UI, 100% parity across all 6 string resource files (350 keys each), and 100% parity across all 48 user-facing markdown documentation files (8 topics x 6 languages). Added 5 localized keys (`password_show`, `password_hide`, `select_private_key_file`, `select_image_dialog_title`, `unknown_error`) across all 6 languages. Fixed `ToyForm.kt` hardcoded string check for multiline fields, localized `SftpSetupScreen.kt` with dynamic language resolution, and replaced hardcoded desktop dialog title in `ImageResolver.kt`. | strings.xml (6 files), SftpSetupScreen.kt, ToyForm.kt, ServerSyncSettingsTab.kt, BackupRestoreCard.kt, ImageResolver.kt, files/*_sftp_setup.md | Complete & Verified |
| 2026-10-07 | Antigravity | Phase 12: Updated General Settings documentation across all 6 languages (`*_general.md` and fallback `general.md`) to include explicit instructions and feature description for the Language selection dropdown on Settings / General. Verified builds and test suites. | files/*_general.md, files/general.md | Complete & Verified |

### 1.2 Bug & Issue Tracker (Living Record)

| Issue ID | Platform / Component | Symptom / Description | Root Cause | Workaround / Fix | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **ISSUE-01** | Android / Gradle Build | Android builds may miss localized string files (`values-pt`, `values-es`, etc.). | `prepareAndroidResources` task in `build.gradle.kts` uses `include("values/**")` which omits hyphenated folders. | Changed include glob to `include("values*/**")` in `build.gradle.kts`. | Resolved |
| **ISSUE-02** | Common / InfoScreen | Markdown documentation language was cached and did not react to user locale changes. | `InfoScreen.kt` calculated `currentLang` inside a static `remember { ... }` block without listening to language selection. | Passed `selectedLanguage` into `InfoScreen` and keyed `currentLang = remember(effectiveLang)` to reload markdown dynamically. | Resolved |
| **ISSUE-03** | Common / UI Navigation | Category navigation buttons on top bar and category cards remained in English when switched to Portuguese (or other languages). | Short labels were hardcoded in `ToyDbNavigation.kt` (`"slot" -> "Slots"`, `"train" -> "Trains"`, etc.) and category cards displayed raw SQLite seeded labels. | Created `CategoryLocalization.kt` to map built-in category IDs to localized resources across all 6 languages with fallback for custom categories. Enhanced `getIconByName` and `getCollectionDrawableResource` with multilingual keywords. | Resolved |
| **ISSUE-04** | Common / ToyForm | Comment and work fields collapsed into single-line fields when UI was switched away from English. | `singleLine = label != "General Comments" && !label.contains("Work")` was an English-specific hardcoded string comparison. | Added explicit `singleLine: Boolean = true` parameter to `FormField` and set `singleLine = false` explicitly on comments/work fields. | Resolved |
| **ISSUE-05** | Common / SftpSetupScreen | SFTP setup instructions loaded only English markdown (`sftp_setup.md`). | `SftpSetupScreen.kt` loaded only `"files/sftp_setup.md"` without considering the active language locale. | Created 6 localized markdown files (`*_sftp_setup.md`), added `selectedLanguage` parameter, and resolved localized markdown with fallback. | Resolved |
| **ISSUE-06** | Desktop / ImageResolver | Desktop image picker dialog title was hardcoded in English. | `ImageResolver.kt` hardcoded `"Select Image"` in file dialog constructor. | Replaced with `stringResource(Res.string.select_image_dialog_title)` across all 6 languages. | Resolved |

---

## 2. Executive Summary & Architectural Overview

### 2.1 Objective
Implement the ability to select the application language locale in **Toy Collection** (Gepetto Toy Database Manager), matching the behavior and user interface of **Lap Counter** and **Race Director**.

### 2.2 System Parity Analysis
The reference applications (**Lap Counter** and **Race Director**) share a consistent multiplatform architecture for localization:

1. **Reciprocal Language Names**:
   A data model (`Language.kt`) defines all supported languages (`en`, `pt`, `fr`, `es`, `it`, `de`). The model provides the localized name of each language translated into the active display language (for example: in Portuguese, English appears as "Inglês").
2. **Multiplatform Locale Switching**:
   An expect/actual object (`LocaleHelper`) manages runtime locale settings across platforms:
   - **Desktop (JVM)**: Controls `java.util.Locale.setDefault()`.
   - **Android (ART)**: Controls `Locale.setDefault()`, `LocaleList.setDefault()`, and context resource configurations.
   - **Web (WasmJS)**: Reads browser language via `window.navigator.language` and maintains state.
3. **Reactive Recomposition**:
   The root Compose container is keyed to the active language (`key(languageChoiceState.value)` in Race Director). When the user selects a language, the entire UI tree recomposes immediately with the new locale without an application restart.
4. **Settings Persistence**:
   The selected language code (`""` for System Default, or `"en"`, `"pt"`, `"fr"`, `"es"`, `"it"`, `"de"`) persists in settings storage. In Toy Collection, the database table `app_settings` holds persistent application preferences.
5. **Localized Documentation Sync**:
   Markdown files (`about`, `general`, `backup_restore`, `server_sync`, `categories`, `privacypolicy`, `terms`) load based on the active language prefix (for example, `pt_general.md`).

---

## 3. Inviolable Directives for Executing Agents

> [!IMPORTANT]
> ### Directive 1: Multiplatform Purity in `commonMain`
> Do not import platform-specific classes (`java.util.Locale`, `android.content.Context`) into `commonMain`. Keep all platform logic behind expect/actual declarations.

> [!IMPORTANT]
> ### Directive 2: Compose Theme & Styling Compliance
> Use `sysForegroundColor()`, `sysBackgroundColor()`, and `sysTextColor()` from `club.gepetto.composeutils`. Ensure all new UI components render cleanly in both Light and Dark modes. Every Composable must include `@PreviewLightDark` and `@Preview(name = "Landscape", widthDp = 800, heightDp = 480)` wrapped in `GcTheme {}`.

> [!IMPORTANT]
> ### Directive 3: Technical Writing Standard (ASD-STE100)
> Write all user-facing documentation, comments, and strings in accordance with ASD-STE100 Simplified Technical English. Use simple verbs, direct instructions, and active voice.

> [!IMPORTANT]
> ### Directive 4: Resource Completeness Across All Locales
> Any new string resource must exist in all six resource folders: `values`, `values-pt`, `values-es`, `values-fr`, `values-it`, `values-de`. Never leave a language file with missing keys.

> [!IMPORTANT]
> ### Directive 5: Living Document Maintenance
> Before and after executing each phase:
> 1. Record changes in Section 1.1.
> 2. Record any encountered bugs or errors in Section 1.2.
> 3. Mark completed steps with `[x]` in Section 7.

---

## 4. Architectural Design & Component Specifications

### 4.1 Architecture Diagram

```
+-----------------------------------------------------------------------------------+
|                                 ToyDbNavigation                                   |
|   - Holds languageChoice: State<String>                                           |
|   - Wraps content tree in: key(languageChoice) { ... }                            |
+-----------------------------------------------------------------------------------+
                                         |
            +----------------------------+----------------------------+
            |                                                         |
            v                                                         v
+--------------------------+                              +-----------------------+
|    GeneralSettingsTab    |                              |      InfoScreen       |
|  - LanguageSelector      |                              |  - Dynamic currentLang|
|    (ExposedDropdown)     |                              |  - Reads localized md |
+--------------------------+                              +-----------------------+
            |
            v
+-----------------------------------------------------------------------------------+
|                        ToyRepository (app_settings table)                         |
|   - getLanguageSetting(): String                                                  |
|   - setLanguageSetting(language: String): Unit                                    |
+-----------------------------------------------------------------------------------+
            |
            v
+-----------------------------------------------------------------------------------+
|                     LocaleHelper (Multiplatform Expect/Actual)                    |
|   + commonMain:   expect object LocaleHelper                                      |
|   + desktopMain:  actual object LocaleHelper (Locale.setDefault)                  |
|   + androidMain:  actual object LocaleHelper (LocaleList, resources config)       |
|   + wasmJsMain:   actual object LocaleHelper (window.navigator fallback)          |
+-----------------------------------------------------------------------------------+
```

---

## 5. Detailed Step-by-Step Implementation Instructions

### Phase 1: String Resources & Localization Assets
Add `settings_language` and `settings_language_system` across all six `strings.xml` files located in `ToyCollection/composeApp/src/commonMain/composeResources/`.

#### 1.1 Default English (`values/strings.xml`)
```xml
<string name="settings_language">Language</string>
<string name="settings_language_system">System Default</string>
```

#### 1.2 Portuguese (`values-pt/strings.xml`)
```xml
<string name="settings_language">Idioma</string>
<string name="settings_language_system">Padrão do Sistema</string>
```

#### 1.3 Spanish (`values-es/strings.xml`)
```xml
<string name="settings_language">Idioma</string>
<string name="settings_language_system">Predeterminado del Sistema</string>
```

#### 1.4 French (`values-fr/strings.xml`)
```xml
<string name="settings_language">Langue</string>
<string name="settings_language_system">Par défaut du Système</string>
```

#### 1.5 Italian (`values-it/strings.xml`)
```xml
<string name="settings_language">Lingua</string>
<string name="settings_language_system">Predefinito di Sistema</string>
```

#### 1.6 German (`values-de/strings.xml`)
```xml
<string name="settings_language">Sprache</string>
<string name="settings_language_system">Systemstandard</string>
```

---

### Phase 2: Multiplatform Locale Helper (`LocaleHelper`)

Create the multiplatform abstraction to manage OS-level locale changes.

#### 2.1 Common Specification
**Target File**: `../composeApp/src/commonMain/kotlin/com/gepetto/toydb/platform/LocaleHelper.kt`
```kotlin
package com.gepetto.toydb.platform

expect object LocaleHelper {
    fun getSystemLanguageCode(): String
    fun setAppLocale(languageCode: String)
}
```

#### 2.2 Desktop Implementation
**Target File**: `../composeApp/src/desktopMain/kotlin/com/gepetto/toydb/platform/LocaleHelper.desktop.kt`
```kotlin
package com.gepetto.toydb.platform

import java.util.Locale

actual object LocaleHelper {
    private val systemDefaultLocale: Locale = Locale.getDefault()

    actual fun getSystemLanguageCode(): String {
        return systemDefaultLocale.language
    }

    actual fun setAppLocale(languageCode: String) {
        val targetLocale = if (languageCode.isNotEmpty()) {
            Locale.forLanguageTag(languageCode)
        } else {
            systemDefaultLocale
        }
        Locale.setDefault(targetLocale)
    }
}
```

#### 2.3 Android Implementation
**Target File**: `../composeApp/src/androidMain/kotlin/com/gepetto/toydb/platform/LocaleHelper.android.kt`
```kotlin
package com.gepetto.toydb.platform

import android.content.Context
import android.os.Build
import android.os.LocaleList
import club.gepetto.utils.GcAppInfo
import java.util.Locale

actual object LocaleHelper {
    private val systemDefaultLocale: Locale = Locale.getDefault()
    private val systemDefaultLocaleList: LocaleList? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        LocaleList.getDefault()
    } else null

    actual fun getSystemLanguageCode(): String {
        return systemDefaultLocale.language
    }

    actual fun setAppLocale(languageCode: String) {
        val targetLocale = if (languageCode.isNotEmpty()) {
            Locale.forLanguageTag(languageCode)
        } else {
            systemDefaultLocale
        }
        Locale.setDefault(targetLocale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val list = if (languageCode.isNotEmpty()) {
                LocaleList(targetLocale)
            } else {
                systemDefaultLocaleList ?: LocaleList(targetLocale)
            }
            LocaleList.setDefault(list)
        }
        val context = GcAppInfo.application_Context as? Context
        if (context != null) {
            try {
                val res = context.resources
                val config = res.configuration
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val list = if (languageCode.isNotEmpty()) {
                        LocaleList(targetLocale)
                    } else {
                        systemDefaultLocaleList ?: LocaleList(targetLocale)
                    }
                    config.setLocales(list)
                } else {
                    @Suppress("DEPRECATION")
                    config.locale = targetLocale
                }
                @Suppress("DEPRECATION")
                res.updateConfiguration(config, res.displayMetrics)
            } catch (_: Throwable) {}
        }
    }
}
```

#### 2.4 Web Implementation
**Target File**: `../composeApp/src/wasmJsMain/kotlin/com/gepetto/toydb/platform/LocaleHelper.wasmJs.kt`
```kotlin
package com.gepetto.toydb.platform

import kotlinx.browser.window

actual object LocaleHelper {
    actual fun getSystemLanguageCode(): String {
        return try {
            val navLang = window.navigator.language
            navLang.split('-')[0].split('_')[0].lowercase()
        } catch (_: Throwable) {
            "en"
        }
    }

    actual fun setAppLocale(languageCode: String) {
        // Browser sandboxed runtime
    }
}
```

---

### Phase 3: Language Data Model & Repository Persistence

#### 3.1 Language Data Model
**Target File**: `../composeApp/src/commonMain/kotlin/com/gepetto/toydb/ui/Language.kt`
```kotlin
package com.gepetto.toydb.ui

import com.gepetto.toydb.platform.LocaleHelper

data class Language(
    val thisLanguage: String = "en",
    val english: String = "English",
    val portuguese: String = "Portuguese",
    val french: String = "French",
    val spanish: String = "Spanish",
    val italian: String = "Italian",
    val german: String = "German"
)

val languages = listOf(
    Language("en"),
    Language("pt", "Inglês", "Português", "Francês", "Espanhol", "Italiano", "Alemão"),
    Language("fr", "Anglais", "Portugais", "Français", "Espagnol", "Italien", "Allemand"),
    Language("es", "Inglés", "Portugués", "Francés", "Español", "Italiano", "Alemán"),
    Language("it", "Inglese", "Portoghese", "Francese", "Spagnolo", "Italiano", "Tedesco"),
    Language("de", "Englisch", "Portugiesisch", "Französisch", "Spanisch", "Italienisch", "Deutsch")
)

fun getSystemLanguage(): String {
    return LocaleHelper.getSystemLanguageCode()
}
```

#### 3.2 Database Persistence in ToyRepository
**Target File**: `../composeApp/src/commonMain/kotlin/com/gepetto/toydb/database/ToyRepository.kt`
Add methods to query and persist the language configuration in the `app_settings` SQLite table:
```kotlin
fun getLanguageSetting(): String = getAppSetting("language") ?: ""

fun setLanguageSetting(language: String) = setAppSetting("language", language)
```

---

### Phase 4: UI Composable Components

#### 4.1 LanguageSelector Composable
**Target File**: `../composeApp/src/commonMain/kotlin/com/gepetto/toydb/ui/GeneralSettingsTab.kt`

Add the `LanguageSelector` Composable directly beneath or alongside `ThemeSelector`:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelector(
    currentLanguageCode: String,
    onLanguageChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val displayLanguage = languages.find {
        it.thisLanguage == (if (currentLanguageCode.isEmpty()) getSystemLanguage() else currentLanguageCode)
    } ?: languages[0]
    val languageOptions = listOf("") + languages.map { it.thisLanguage }

    @Composable
    fun getLanguageName(code: String): String {
        if (code.isEmpty()) return stringResource(Res.string.settings_language_system)
        return when (code) {
            "en" -> displayLanguage.english
            "pt" -> displayLanguage.portuguese
            "fr" -> displayLanguage.french
            "es" -> displayLanguage.spanish
            "it" -> displayLanguage.italian
            "de" -> displayLanguage.german
            else -> code
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.settings_language),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = sysTextColor()
        )
        Spacer(modifier = Modifier.height(GcSpacing.Small))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = getLanguageName(currentLanguageCode),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(Res.string.settings_language)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = sysTextColor(),
                    unfocusedTextColor = sysTextColor(),
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = sysTextColor().copy(alpha = 0.6f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                containerColor = sysBackgroundColor(),
                onDismissRequest = { expanded = false }
            ) {
                languageOptions.forEach { code ->
                    DropdownMenuItem(
                        text = { Text(getLanguageName(code), color = sysTextColor()) },
                        onClick = {
                            onLanguageChanged(code)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
```

#### 4.2 Update `GeneralSettingsTab` Parameters
Update `GeneralSettingsTab` to take:
```kotlin
currentLanguage: String,
onLanguageChanged: (String) -> Unit
```
Place `LanguageSelector(currentLanguage, onLanguageChanged)` in the layout.

#### 4.3 Update `SettingsScreen` Parameters
Update `SettingsScreen` to pass `currentLanguage` and `onLanguageChanged` through to `GeneralSettingsTab`.

---

### Phase 5: Navigation & Reactive Recomposition Wiring

**Target File**: `../composeApp/src/commonMain/kotlin/com/gepetto/toydb/ui/ToyDbNavigation.kt`

1. Initialize `languageChoice` state:
```kotlin
var languageChoice by remember { mutableStateOf(repository.getLanguageSetting()) }
```

2. Wrap the app's composable content in `key(languageChoice)`:
```kotlin
key(languageChoice) {
    val isDark = when (themeMode) {
        1 -> false
        2 -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    GcTheme(darkTheme = isDark) {
        // App Scaffold & Navigation
    }
}
```

3. Handle language update events in `SettingsScreen` invocation:
```kotlin
currentLanguage = languageChoice,
onLanguageChanged = { newLanguage ->
    repository.setLanguageSetting(newLanguage)
    com.gepetto.toydb.platform.LocaleHelper.setAppLocale(newLanguage)
    languageChoice = newLanguage
}
```

---

### Phase 6: Application Startup Hydration Across Platforms

Hydrate the saved language preference when each platform application boots.

#### 6.1 Desktop Startup (`Main.kt`)
**Target File**: `../composeApp/src/desktopMain/kotlin/Main.kt`
In `main()`:
```kotlin
val database = createDatabase(null, dbFile.absolutePath)
val repository = ToyRepository(database)
val savedLanguage = repository.getLanguageSetting()
com.gepetto.toydb.platform.LocaleHelper.setAppLocale(savedLanguage)
```

#### 6.2 Android Startup (`AppMainActivity.kt`)
**Target File**: `../composeApp/src/androidMain/kotlin/com/gepetto/toydb/AppMainActivity.kt`
In `onCreate()`:
```kotlin
val database = createDatabase(this, dbFile.absolutePath)
val repository = ToyRepository(database)
val savedLanguage = repository.getLanguageSetting()
com.gepetto.toydb.platform.LocaleHelper.setAppLocale(savedLanguage)
```

#### 6.3 Web Startup (`Main.kt`)
**Target File**: `../composeApp/src/wasmJsMain/kotlin/Main.kt`
In `main()` coroutine:
```kotlin
val database = WasmToyDatabase.open()
val repository = ToyRepository(database)
val savedLanguage = repository.getLanguageSetting()
com.gepetto.toydb.platform.LocaleHelper.setAppLocale(savedLanguage)
```

---

### Phase 7: Dynamic Markdown Documentation Localization

**Target File**: `../composeApp/src/commonMain/kotlin/com/gepetto/toydb/ui/InfoScreen.kt`

Update `InfoScreen` to determine `currentLang` dynamically:
```kotlin
@Composable
fun InfoScreen(
    onNavigateToSftpSetup: () -> Unit = {},
    initialTopicId: String? = null,
    selectedLanguage: String = "",
    modifier: Modifier = Modifier
) {
    ...
    val effectiveLang = if (selectedLanguage.isNotEmpty()) {
        selectedLanguage
    } else {
        try {
            com.gepetto.toydb.platform.LocaleHelper.getSystemLanguageCode()
        } catch (_: Throwable) {
            "en"
        }
    }
    val currentLang = remember(effectiveLang) {
        when (effectiveLang.lowercase().take(2)) {
            "pt", "es", "it", "de", "fr" -> effectiveLang.lowercase().take(2)
            else -> "en"
        }
    }
```
In `ToyDbNavigation.kt`, pass `selectedLanguage = languageChoice` when instantiating `InfoScreen`.

---

### Phase 8: Android Resources Build Task Correction

**Target File**: `../composeApp/build.gradle.kts`

Fix the `prepareAndroidResources` copy task so that localized subdirectories are included in the generated Android resources:
```kotlin
val prepareAndroidResources = tasks.register<Copy>("prepareAndroidResources") {
    from("src/commonMain/composeResources") {
        include("values*/**")
        include("drawable/**")
    }
    into(layout.buildDirectory.dir("generated/android/res"))
}
```

---

### Phase 9: Unit Testing & Parity Verification

#### 9.1 Language Model Unit Test
**Target File**: `../composeApp/src/commonTest/kotlin/com/gepetto/toydb/ui/LanguageModelTest.kt`
Verify:
1. All 6 languages exist in `languages`.
2. Each entry provides distinct translated names for all 6 languages.
3. Fallback logic resolves unknown codes correctly.

#### 9.2 Repository Setting Test
**Target File**: `composeApp/src/desktopTest/kotlin/com/gepetto/toydb/database/ToyRepositoryLanguageTest.kt`
Verify:
1. `getLanguageSetting()` defaults to `""`.
2. `setLanguageSetting("pt")` persists and returns `"pt"`.
3. `setLanguageSetting("")` clears or resets to system default.

---

### Phase 10: Top Navigation & Screen Icons Audit Across All Languages

#### 10.1 Audit Findings & Parity Analysis
1. **Top Navigation Bar (Landscape & Desktop)**:
   - Fixed navigation buttons (`nav_home`, `nav_dashboard`, `nav_makers`, `nav_settings`, `nav_info`) were localized, but French, Italian, and German had generic English fallbacks ("Home" / "Dash"). Updated French to "Accueil" / "Tableau", Italian to "Pannello", and German to "Start" / "Übersicht".
   - Category buttons in landscape/desktop top bar used hardcoded English strings: `"Slots"`, `"Trains"`, `"Static"`, `"Kits"`, `"Misc"`.
2. **Category Cards & Headers (Dashboard & Explorer)**:
   - Displayed English database seed strings (`Slot Cars`, `Model Trains`, `Static Models`, `Model Kits`, `Others`).
3. **Hardcoded Content Descriptions**:
   - `HomeDestination.kt`: `"Gepetto"` logo and `"Close"` button.
   - `ToyForm.kt` & `MakerForm.kt`: `"Edit Filenames"`, `"Current image"`, `"New image"`.
   - `SyncImage.kt`: `"Downloading image..."`.

#### 10.2 Implemented Changes
- Added 15 new string resources to all 6 language `strings.xml` files (`values`, `values-pt`, `values-es`, `values-fr`, `values-it`, `values-de`):
  - `nav_cat_slots`, `nav_cat_trains`, `nav_cat_static`, `nav_cat_kits`, `nav_cat_misc`
  - `cat_label_slots`, `cat_label_trains`, `cat_label_static`, `cat_label_kits`, `cat_label_misc`
  - `gepetto_logo_desc`, `edit_filenames`, `current_image_desc`, `new_image_desc`, `downloading_image_desc`
- Created `CategoryLocalization.kt` with pure mapping functions and composable `rememberCategoryShortLabelResolver()` / `rememberCategoryLabelResolver()`.
- Enhanced `getIconByName` in `ToyDbNavigation.kt` and `getCollectionDrawableResource` in `DashboardScreen.kt` with multilingual keyword synonyms (Portuguese, Spanish, French, Italian, German).
- Localized all content descriptions in `HomeDestination.kt`, `ToyForm.kt`, `MakerForm.kt`, `SyncImage.kt`.
- Created and passed `CategoryLocalizationTest.kt` (4 unit tests).

### Phase 11: Comprehensive Codebase Localization Audit (Verification & Hardening)

#### 11.1 Audit Scope & Methodology
1. **Verification of No Hardcoded Strings**:
   - Automated AST / regex scan across all UI Composable functions in `composeApp/src/commonMain/kotlin/com/gepetto/toydb/ui/` and platform entry points for hardcoded `Text("")`, `contentDescription = ""`, dialog titles, button labels, placeholders, and error messages.
   - Fixed hardcoded string check in `ToyForm.kt` (`label != "General Comments" && !label.contains("Work")`) by adding an explicit parameter `singleLine: Boolean = true` to `FormField`.
   - Localized password visibility toggle descriptions (`password_show`, `password_hide`) and file picker title (`select_private_key_file`) in `ServerSyncSettingsTab.kt`.
   - Localized `"Unknown error"` fallback in `BackupRestoreCard.kt` (`unknown_error`).
   - Localized file dialog title in desktop `ImageResolver.kt` (`select_image_dialog_title`).
2. **String Resource Parity Across All 6 Languages**:
   - Automated XML element parsing across `values`, `values-pt`, `values-es`, `values-fr`, `values-it`, and `values-de`.
   - Verified that every single string resource key exists in all 6 files with non-empty translated content.
   - Result: Exactly 350 keys across all 6 files (0 missing, 0 extra, 0 empty).
3. **Markdown Documentation Parity Across All 6 Languages**:
   - Verified that all 8 user documentation topics (`about`, `general`, `backup_restore`, `server_sync`, `categories`, `privacypolicy`, `terms`, `sftp_setup`) exist in all 6 languages (`en`, `pt`, `es`, `fr`, `it`, `de`).
   - Created 6 localized variants of `sftp_setup.md` (`en_sftp_setup.md`, `pt_sftp_setup.md`, `es_sftp_setup.md`, `fr_sftp_setup.md`, `it_sftp_setup.md`, `de_sftp_setup.md`).
   - Updated `SftpSetupScreen.kt` to dynamically resolve localized markdown with fallbacks.
   - Result: Exactly 48 markdown files (8 topics x 6 languages), all populated and non-empty.

---

## 6. Build & Verification Protocol

Always execute gradle commands via `./gradlew` from `ToyCollection/`. Follow AGENTS.md rules:

```bash
# 1. Compile Desktop target
./gradlew :composeApp:compileKotlinDesktop

# 2. Compile Android target
./gradlew :composeApp:compileDebugKotlinAndroid

# 3. Run Common & Desktop Unit Test Suites
./gradlew :composeApp:desktopTest
```

### Manual Verification Checklist
1. **General Settings Tab**:
   - Open Settings -> General tab.
   - Verify the "Language" dropdown appears below the "Theme" selector.
   - Verify options: System Default, English, Português, Français, Español, Italiano, Deutsch.
2. **Dynamic UI Language Switch**:
   - Select "Português".
   - Verify UI labels (Settings tabs, screen headers, button labels) switch immediately to Portuguese.
   - Verify language dropdown now displays names in Portuguese ("Inglês", "Português", "Francês", etc.).
3. **Info Screen Documentation Switch**:
   - Navigate to Help / Info screen.
   - Verify tabs (About, General, Backup & Restore, Categories) display text from Portuguese markdown files (`pt_general.md`, etc.).
4. **Persistence Verification**:
   - Close and restart the desktop app.
   - Verify Portuguese remains active on launch.
   - Select "System Default".
   - Verify app returns to OS system language.

---

## 7. Completion Checklist (LIVE Progress Tracker in `LOCALE_PLAN2.md`)

- [x] **Document Established**: `ToyCollection/.agents/LOCALE_PLAN.md` created as authoritative living document.
- [x] **Phase 1**: String resources added in all 6 XML files (`values`, `values-pt`, `values-es`, `values-fr`, `values-it`, `values-de`).
- [x] **Phase 2**: Multiplatform `LocaleHelper` expect/actual implemented in `commonMain`, `desktopMain`, `androidMain`, `wasmJsMain`.
- [x] **Phase 3**: `Language.kt` data model created and `ToyRepository` persistence methods added.
- [x] **Phase 4**: `LanguageSelector` Composable added to `GeneralSettingsTab.kt` with Light/Dark and Landscape previews; `SettingsScreen.kt` updated.
- [x] **Phase 5**: `ToyDbNavigation.kt` wired with `languageChoice` state, `key(languageChoice)` recomposition wrapper, and repository callbacks.
- [x] **Phase 6**: Platform entry points (`Main.kt` Desktop, `AppMainActivity.kt` Android, `Main.kt` WasmJs) hydrated on startup.
- [x] **Phase 7**: `InfoScreen.kt` updated to dynamically resolve markdown documentation matching `languageChoice`.
- [x] **Phase 8**: `../composeApp/build.gradle.kts` updated (`include("values*/**")`) for Android resource packaging.
- [x] **Phase 9**: Unit tests implemented in `commonTest` and `desktopTest`.
- [x] **Phase 10**: Audited top screen icons and labels across all 6 languages; added localized category resources, CategoryLocalization helper, multilingual icon and drawable resolution, and unit tests.
- [x] **Phase 11**: Full codebase localization audit completed: 0 hardcoded user-facing strings across all UI, 100% resource key parity across all 6 languages (350 keys each), and 100% markdown documentation parity across all 6 languages (48 files across 8 topics).
- [x] **Phase 12**: Updated General Settings documentation across all 6 languages to include the Language option on Settings / General.
- [x] **Verification**: `./gradlew :composeApp:compileKotlinDesktop :composeApp:compileDebugKotlinAndroid :composeApp:compileKotlinWasmJs :composeApp:desktopTest` executed cleanly with 0 errors.
- [x] **TODO Checklist**: `ToyCollection/.agents/TODO.txt` item `[ ]-ability to change language locale` updated.
