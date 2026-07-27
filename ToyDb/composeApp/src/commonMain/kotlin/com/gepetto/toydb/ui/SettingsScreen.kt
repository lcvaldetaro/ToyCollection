package com.gepetto.toydb.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import club.gepetto.GcLog
import club.gepetto.composeutils.GcSpacing
import club.gepetto.composeutils.sysBackgroundColor
import club.gepetto.composeutils.sysTextColor
import com.gepetto.toydb.database.CategorySetting
import com.gepetto.toydb.database.ToyDatabase
import com.gepetto.toydb.database.ToyRepository
import com.gepetto.toydb.service.ImportExportService
import okio.FileSystem
import okio.Path.Companion.toPath
import com.gepetto.toydb.utils.selectDirectoryDialog
import com.gepetto.toydb.utils.isDesktopPlatform
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    db: ToyDatabase,
    currentTheme: Int = 0,
    onThemeChanged: (Int) -> Unit = {},
    onCategoriesChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    val repository = remember { ToyRepository(db) }
    var statusText by remember { mutableStateOf("Ready") }

    // Dynamic Category List state
    var categoriesList by remember { mutableStateOf(repository.getCategorySettings()) }

    // Category Dialog State
    var showCategoryDialog by remember { mutableStateOf(false) }
    var dialogIsEditMode by remember { mutableStateOf(false) }
    var dialogCategoryKey by remember { mutableStateOf("") }
    var dialogCategoryLabel by remember { mutableStateOf("") }
    var dialogCategoryPrefix by remember { mutableStateOf("") }
    var dialogErrorText by remember { mutableStateOf("") }

    // Delete Category confirmation state
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<CategorySetting?>(null) }

    // Export success dialog state
    var showExportDialog by remember { mutableStateOf(false) }
    var exportDirectoryPath by remember { mutableStateOf("") }
    val exportedFiles = remember { mutableStateListOf<String>() }

    // Import success dialog state
    var showImportDialog by remember { mutableStateOf(false) }
    val importCountsMap = remember { mutableStateMapOf<String, Int>() }

    // Paths state
    var customImagesPath by remember { mutableStateOf(repository.getImagesPathSetting()) }
    var customImportExportPath by remember { mutableStateOf(repository.getImportExportPathSetting()) }

    fun readJsonFile(fileName: String, dirPath: String? = null): String? {
        val paths = if (dirPath != null) {
            listOf("$dirPath/$fileName".toPath())
        } else {
            listOf(
                "/Users/luizvaldetaro/valdetaro/ToyDb/json/$fileName".toPath(),
                "ToyDb/json/$fileName".toPath(),
                "json/$fileName".toPath()
            )
        }
        for (path in paths) {
            try {
                if (FileSystem.SYSTEM.exists(path)) {
                    return FileSystem.SYSTEM.read(path) { readUtf8() }
                }
            } catch (e: Exception) {
                GcLog.e("SettingsScreen", "Error reading $path: ${e.message}", e)
            }
        }
        return null
    }

    fun writeJsonFile(fileName: String, content: String, dirPath: String? = null): String? {
        val paths = if (dirPath != null) {
            listOf("$dirPath/$fileName".toPath())
        } else {
            listOf(
                "/Users/luizvaldetaro/valdetaro/ToyDb/json/$fileName".toPath(),
                "ToyDb/json/$fileName".toPath(),
                "json/$fileName".toPath()
            )
        }
        for (path in paths) {
            try {
                // Ensure parent directory exists
                path.parent?.let { parent ->
                    if (!FileSystem.SYSTEM.exists(parent)) {
                        FileSystem.SYSTEM.createDirectories(parent)
                    }
                }
                FileSystem.SYSTEM.write(path) { writeUtf8(content) }
                return path.toString()
            } catch (e: Exception) {
                GcLog.e("SettingsScreen", "Error writing $path: ${e.message}", e)
            }
        }
        return null
    }

    // Category Create/Edit Dialog
    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            containerColor = sysBackgroundColor(),
            title = {
                Text(
                    text = if (dialogIsEditMode) "Edit Category" else "Add New Category",
                    color = sysTextColor()
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (dialogErrorText.isNotEmpty()) {
                        Text(dialogErrorText, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    OutlinedTextField(
                        value = dialogCategoryKey,
                        onValueChange = { dialogCategoryKey = it.trim().lowercase() },
                        label = { Text("Category ID (lowercase, unique)*") },
                        placeholder = { Text("e.g. lego") },
                        enabled = !dialogIsEditMode, // Key cannot be edited
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = sysTextColor(),
                            unfocusedTextColor = sysTextColor(),
                            disabledTextColor = sysTextColor().copy(alpha = 0.5f)
                        )
                    )
                    OutlinedTextField(
                        value = dialogCategoryLabel,
                        onValueChange = { dialogCategoryLabel = it },
                        label = { Text("Category Name (Label)*") },
                        placeholder = { Text("e.g. Lego Sets") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = sysTextColor(),
                            unfocusedTextColor = sysTextColor()
                        )
                    )
                    OutlinedTextField(
                        value = dialogCategoryPrefix,
                        onValueChange = { dialogCategoryPrefix = it.trim() },
                        label = { Text("Filename Prefix (e.g. leg)*") },
                        placeholder = { Text("e.g. leg") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = sysTextColor(),
                            unfocusedTextColor = sysTextColor()
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (dialogCategoryKey.trim().isEmpty() || dialogCategoryLabel.trim().isEmpty() || dialogCategoryPrefix.trim().isEmpty()) {
                            dialogErrorText = "All fields marked with * are required."
                            return@Button
                        }
                        
                        val newSetting = CategorySetting(
                            category = dialogCategoryKey.trim(),
                            label = dialogCategoryLabel.trim(),
                            imagePrefix = dialogCategoryPrefix.trim()
                        )
                        
                        if (dialogIsEditMode) {
                            repository.updateCategorySetting(newSetting)
                        } else {
                            if (categoriesList.any { it.category == newSetting.category }) {
                                dialogErrorText = "Category ID '${newSetting.category}' already exists."
                                return@Button
                            }
                            repository.addCategorySetting(newSetting)
                        }
                        
                        // Reload state & trigger callback
                        categoriesList = repository.getCategorySettings()
                        onCategoriesChanged()
                        showCategoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCategoryDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Category Confirmation Dialog
    if (showDeleteConfirmDialog && categoryToDelete != null) {
        val cat = categoryToDelete!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = sysBackgroundColor(),
            title = { Text("Delete Category", color = sysTextColor()) },
            text = {
                Text(
                    "Are you sure you want to delete category '${cat.label}'?\n\n" +
                    "WARNING: This will permanently delete ALL toys belonging to this category from the database!",
                    color = MaterialTheme.colorScheme.error
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteCategorySetting(cat.category)
                        categoriesList = repository.getCategorySettings()
                        onCategoriesChanged()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete Category")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Export Success Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            containerColor = sysBackgroundColor(),
            title = {
                Text(
                    text = "Export Successful",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = sysTextColor()
                )
            },
            text = {
                Column {
                    Text("The database records were exported to the following directory:", color = sysTextColor())
                    Spacer(modifier = Modifier.height(GcSpacing.Small))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = sysBackgroundColor()),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = exportDirectoryPath,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(GcSpacing.Small),
                            color = sysTextColor()
                        )
                    }
                    Spacer(modifier = Modifier.height(GcSpacing.Standard))
                    Text("Exported Files:", fontWeight = FontWeight.SemiBold, color = sysTextColor())
                    Spacer(modifier = Modifier.height(4.dp))
                    exportedFiles.forEach { file ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text("✓ ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(file, fontSize = 14.sp, color = sysTextColor())
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showExportDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Import Success Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = sysBackgroundColor(),
            title = {
                Text(
                    text = "Import Successful",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = sysTextColor()
                )
            },
            text = {
                Column {
                    Text("The database records were imported successfully from the following directory:", color = sysTextColor())
                    Spacer(modifier = Modifier.height(GcSpacing.Small))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = sysBackgroundColor()),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = customImportExportPath ?: "Default directory",
                            fontSize = 13.sp,
                            modifier = Modifier.padding(GcSpacing.Small),
                            color = sysTextColor()
                        )
                    }
                    Spacer(modifier = Modifier.height(GcSpacing.Standard))
                    Text("Imported Records:", fontWeight = FontWeight.SemiBold, color = sysTextColor())
                    Spacer(modifier = Modifier.height(4.dp))
                    importCountsMap.forEach { (label, count) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text("✓ ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text("$label: $count", fontSize = 14.sp, color = sysTextColor())
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showImportDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(sysBackgroundColor())
            .verticalScroll(rememberScrollState())
            .padding(GcSpacing.Standard)
    ) {
        Text("Database & System Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = sysTextColor())
        Spacer(modifier = Modifier.height(GcSpacing.Standard))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isWide = maxWidth > 850.dp
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GcSpacing.Standard)
                ) {
                    // Left Column: Configurations
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(GcSpacing.Standard)
                    ) {
                        StatusBanner(statusText)
                        ThemeSelector(currentTheme, onThemeChanged)
                        if (isDesktopPlatform()) {
                            ImagesDirectorySettings(
                                customImagesPath = customImagesPath,
                                onSelectPath = { selectedDir ->
                                    repository.setImagesPathSetting(selectedDir)
                                    com.gepetto.toydb.utils.ImageResolverConfig.imagesPath = selectedDir
                                    customImagesPath = selectedDir
                                },
                                onClearPath = {
                                    repository.setImagesPathSetting(null)
                                    com.gepetto.toydb.utils.ImageResolverConfig.imagesPath = null
                                    customImagesPath = null
                                }
                            )
                            ImportExportDirectorySettings(
                                customImportExportPath = customImportExportPath,
                                onSelectPath = { selectedDir ->
                                    repository.setImportExportPathSetting(selectedDir)
                                    customImportExportPath = selectedDir
                                },
                                onClearPath = {
                                    repository.setImportExportPathSetting(null)
                                    customImportExportPath = null
                                }
                            )
                            ImportExportActions(
                                customImportExportPath = customImportExportPath,
                                db = db,
                                onImportComplete = { counts ->
                                    importCountsMap.clear()
                                    importCountsMap.putAll(counts)
                                    showImportDialog = true
                                    statusText = "Import Complete!"
                                },
                                onExportComplete = { path, files ->
                                    exportDirectoryPath = path
                                    exportedFiles.clear()
                                    exportedFiles.addAll(files)
                                    showExportDialog = true
                                    statusText = "Export Complete!"
                                },
                                onSetStatus = { statusText = it },
                                readJsonFile = { name, dir -> readJsonFile(name, dir) },
                                writeJsonFile = { name, content, dir -> writeJsonFile(name, content, dir) }
                            )
                        }
                    }

                    // Right Column: Categories Manager
                    Column(
                        modifier = Modifier.weight(1.2f),
                        verticalArrangement = Arrangement.spacedBy(GcSpacing.Standard)
                    ) {
                        CategoriesManager(
                            categoriesList = categoriesList,
                            onAddCategory = {
                                dialogIsEditMode = false
                                dialogCategoryKey = ""
                                dialogCategoryLabel = ""
                                dialogCategoryPrefix = ""
                                dialogErrorText = ""
                                showCategoryDialog = true
                            },
                            onEditCategory = { cat ->
                                dialogIsEditMode = true
                                dialogCategoryKey = cat.category
                                dialogCategoryLabel = cat.label
                                dialogCategoryPrefix = cat.imagePrefix
                                dialogErrorText = ""
                                showCategoryDialog = true
                            },
                            onDeleteCategory = { cat ->
                                categoryToDelete = cat
                                showDeleteConfirmDialog = true
                            }
                        )
                    }
                }
            } else {
                // Mobile layout
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(GcSpacing.Standard)
                ) {
                    StatusBanner(statusText)
                    ThemeSelector(currentTheme, onThemeChanged)
                    if (isDesktopPlatform()) {
                        ImagesDirectorySettings(
                            customImagesPath = customImagesPath,
                            onSelectPath = { selectedDir ->
                                repository.setImagesPathSetting(selectedDir)
                                com.gepetto.toydb.utils.ImageResolverConfig.imagesPath = selectedDir
                                customImagesPath = selectedDir
                            },
                            onClearPath = {
                                repository.setImagesPathSetting(null)
                                com.gepetto.toydb.utils.ImageResolverConfig.imagesPath = null
                                customImagesPath = null
                            }
                        )
                        ImportExportDirectorySettings(
                            customImportExportPath = customImportExportPath,
                            onSelectPath = { selectedDir ->
                                repository.setImportExportPathSetting(selectedDir)
                                customImportExportPath = selectedDir
                            },
                            onClearPath = {
                                repository.setImportExportPathSetting(null)
                                customImportExportPath = null
                            }
                        )
                        ImportExportActions(
                            customImportExportPath = customImportExportPath,
                            db = db,
                            onImportComplete = { counts ->
                                importCountsMap.clear()
                                importCountsMap.putAll(counts)
                                showImportDialog = true
                                statusText = "Import Complete!"
                            },
                            onExportComplete = { path, files ->
                                exportDirectoryPath = path
                                exportedFiles.clear()
                                exportedFiles.addAll(files)
                                showExportDialog = true
                                statusText = "Export Complete!"
                            },
                            onSetStatus = { statusText = it },
                            readJsonFile = { name, dir -> readJsonFile(name, dir) },
                            writeJsonFile = { name, content, dir -> writeJsonFile(name, content, dir) }
                        )
                    }
                    CategoriesManager(
                        categoriesList = categoriesList,
                        onAddCategory = {
                            dialogIsEditMode = false
                            dialogCategoryKey = ""
                            dialogCategoryLabel = ""
                            dialogCategoryPrefix = ""
                            dialogErrorText = ""
                            showCategoryDialog = true
                        },
                        onEditCategory = { cat ->
                            dialogIsEditMode = true
                            dialogCategoryKey = cat.category
                            dialogCategoryLabel = cat.label
                            dialogCategoryPrefix = cat.imagePrefix
                            dialogErrorText = ""
                            showCategoryDialog = true
                        },
                        onDeleteCategory = { cat ->
                            categoryToDelete = cat
                            showDeleteConfirmDialog = true
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun StatusBanner(statusText: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = sysBackgroundColor()),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(GcSpacing.Standard)) {
            Text("Storage Type: SQLite Local Database", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Status: $statusText", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = sysTextColor())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelector(currentTheme: Int, onThemeChanged: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Theme Mode", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = sysTextColor())
        Spacer(modifier = Modifier.height(GcSpacing.Small))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().height(40.dp)) {
            val buttonColors = SegmentedButtonDefaults.colors(
                activeContainerColor = MaterialTheme.colorScheme.primary,
                activeContentColor = MaterialTheme.colorScheme.onPrimary,
                inactiveContainerColor = Color.Transparent,
                inactiveContentColor = sysTextColor(),
                activeBorderColor = MaterialTheme.colorScheme.primary,
                inactiveBorderColor = MaterialTheme.colorScheme.outline
            )
            SegmentedButton(
                selected = currentTheme == 0,
                onClick = { onThemeChanged(0) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                colors = buttonColors
            ) { Text("System Theme", fontSize = 12.sp) }
            SegmentedButton(
                selected = currentTheme == 1,
                onClick = { onThemeChanged(1) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                colors = buttonColors
            ) { Text("Light", fontSize = 12.sp) }
            SegmentedButton(
                selected = currentTheme == 2,
                onClick = { onThemeChanged(2) },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                colors = buttonColors
            ) { Text("Dark", fontSize = 12.sp) }
        }
    }
}

@Composable
fun ImagesDirectorySettings(
    customImagesPath: String?,
    onSelectPath: (String) -> Unit,
    onClearPath: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Images Directory", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = sysTextColor())
        Spacer(modifier = Modifier.height(GcSpacing.Small))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = sysBackgroundColor()),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(GcSpacing.Standard)) {
                Text(
                    text = if (customImagesPath.isNullOrEmpty()) {
                        "Using default locations (relative to working directory)"
                    } else {
                        "Custom Path: $customImagesPath"
                    },
                    fontSize = 14.sp,
                    color = sysTextColor()
                )
                Spacer(modifier = Modifier.height(GcSpacing.Small))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(GcSpacing.Small)
                ) {
                    Button(
                        onClick = {
                            val selectedDir = selectDirectoryDialog("Select Images Directory")
                            if (selectedDir != null) {
                                onSelectPath(selectedDir)
                            }
                        }
                    ) {
                        Text("Select Directory")
                    }
                    if (!customImagesPath.isNullOrEmpty()) {
                        OutlinedButton(onClick = onClearPath) {
                            Text("Clear Custom Path")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImportExportDirectorySettings(
    customImportExportPath: String?,
    onSelectPath: (String) -> Unit,
    onClearPath: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Import / Export Directory", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = sysTextColor())
        Spacer(modifier = Modifier.height(GcSpacing.Small))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = sysBackgroundColor()),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(GcSpacing.Standard)) {
                Text(
                    text = if (customImportExportPath.isNullOrEmpty()) {
                        "Using default locations (relative to working directory)"
                    } else {
                        "Custom Path: $customImportExportPath"
                    },
                    fontSize = 14.sp,
                    color = sysTextColor()
                )
                Spacer(modifier = Modifier.height(GcSpacing.Small))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(GcSpacing.Small)
                ) {
                    Button(
                        onClick = {
                            val selectedDir = selectDirectoryDialog("Select Import/Export Directory")
                            if (selectedDir != null) {
                                onSelectPath(selectedDir)
                            }
                        }
                    ) {
                        Text("Select Directory")
                    }
                    if (!customImportExportPath.isNullOrEmpty()) {
                        OutlinedButton(onClick = onClearPath) {
                            Text("Clear Custom Path")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImportExportActions(
    customImportExportPath: String?,
    db: ToyDatabase,
    onImportComplete: (counts: Map<String, Int>) -> Unit,
    onExportComplete: (path: String, files: List<String>) -> Unit,
    onSetStatus: (String) -> Unit,
    readJsonFile: (fileName: String, dirPath: String?) -> String?,
    writeJsonFile: (fileName: String, content: String, dirPath: String?) -> String?
) {
    val repository = remember(db) { ToyRepository(db) }
    val coroutineScope = rememberCoroutineScope()
    var isImporting by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Import / Export Actions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = sysTextColor())
        Spacer(modifier = Modifier.height(GcSpacing.Small))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GcSpacing.Standard)
        ) {
            Button(
                enabled = !isImporting && !isExporting,
                onClick = {
                    val selectedDir = customImportExportPath
                    if (selectedDir != null) {
                        coroutineScope.launch {
                            isImporting = true
                            onSetStatus("Importing JSON files from $selectedDir...")
                            try {
                                val importedCounts = mutableMapOf<String, Int>()

                                // Execute SQLite insertions on IO dispatcher
                                withContext(Dispatchers.IO) {
                                    // 1. Import Category Settings
                                    val catSettingsContent = readJsonFile("category_settings.json", selectedDir)
                                    val catSettingsCount = catSettingsContent?.let { ImportExportService.importCategorySettings(db, it) } ?: 0
                                    if (catSettingsCount > 0) {
                                        importedCounts["Category Settings"] = catSettingsCount
                                    }

                                    // 2. Import Makers
                                    var makerCount = 0
                                    val makersContent = readJsonFile("carmaker.json", selectedDir) ?: readJsonFile("makers.json", selectedDir)
                                    makersContent?.let { makerCount = ImportExportService.importMakers(db, it) }
                                    if (makerCount > 0) {
                                        importedCounts["Makers"] = makerCount
                                    }

                                    // 4. Import Toys for all active categories
                                    val activeCategories = repository.getCategorySettings()
                                    activeCategories.forEach { cat ->
                                        val content = readJsonFile("${cat.imagePrefix}list.json", selectedDir)
                                            ?: readJsonFile("${cat.category}s.json", selectedDir)
                                            ?: readJsonFile("${cat.category}list.json", selectedDir)
                                            ?: readJsonFile("${cat.category}.json", selectedDir)
                                        if (content != null) {
                                            val count = ImportExportService.importToys(db, cat.category, content)
                                            importedCounts[cat.label] = count
                                        }
                                    }
                                }

                                isImporting = false
                                onImportComplete(importedCounts)
                            } catch (e: Exception) {
                                isImporting = false
                                onSetStatus("Error during import: ${e.message}")
                                GcLog.e("SettingsScreen", "Import error", e)
                            }
                        }
                    } else {
                        onSetStatus("Import failed: Import/Export directory is not configured.")
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Importing...")
                } else {
                    Text("Import JSON Data")
                }
            }

            Button(
                enabled = !isImporting && !isExporting,
                onClick = {
                    val selectedDir = customImportExportPath
                    if (selectedDir != null) {
                        coroutineScope.launch {
                            isExporting = true
                            onSetStatus("Exporting JSON files to $selectedDir...")
                            try {
                                val filesToWrite = mutableListOf<Pair<String, String>>()
                                var successCount = 0
                                var lastWrittenPath: String? = null
                                val exported = mutableListOf<String>()

                                withContext(Dispatchers.IO) {
                                    // 1. Export Makers
                                    val makersJson = ImportExportService.exportMakers(db)
                                    filesToWrite.add("carmaker.json" to makersJson)

                                    // 2. Export Category Settings
                                    val categorySettingsJson = ImportExportService.exportCategorySettings(db)
                                    filesToWrite.add("category_settings.json" to categorySettingsJson)

                                    // 4. Export Toys
                                    val activeCategories = repository.getCategorySettings()
                                    activeCategories.forEach { cat ->
                                        val toysJson = ImportExportService.exportToys(db, cat.category)
                                        filesToWrite.add("${cat.imagePrefix}list.json" to toysJson)
                                    }

                                    for ((fileName, jsonContent) in filesToWrite) {
                                        val pathStr = writeJsonFile(fileName, jsonContent, selectedDir)
                                        if (pathStr != null) {
                                            successCount++
                                            exported.add(fileName)
                                            lastWrittenPath = pathStr
                                        }
                                    }
                                }

                                isExporting = false
                                if (successCount == filesToWrite.size && lastWrittenPath != null) {
                                    val dirPath = lastWrittenPath.substringBeforeLast("/")
                                    onExportComplete(dirPath, exported)
                                } else {
                                    onSetStatus("Export Completed with errors ($successCount/${filesToWrite.size} succeeded).")
                                }
                            } catch (e: Exception) {
                                isExporting = false
                                onSetStatus("Error during export: ${e.message}")
                                GcLog.e("SettingsScreen", "Export error", e)
                            }
                        }
                    } else {
                        onSetStatus("Export failed: Import/Export directory is not configured.")
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onSecondary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exporting...")
                } else {
                    Text("Export JSON Data")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoriesManager(
    categoriesList: List<CategorySetting>,
    onAddCategory: () -> Unit,
    onEditCategory: (CategorySetting) -> Unit,
    onDeleteCategory: (CategorySetting) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Toy Categories Manager", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = sysTextColor())
            IconButton(onClick = onAddCategory) {
                Icon(Icons.Default.Add, contentDescription = "Add Category", tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(GcSpacing.Small))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GcSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(GcSpacing.Small)
        ) {
            categoriesList.forEach { cat ->
                Card(
                    modifier = Modifier.width(320.dp),
                    colors = CardDefaults.cardColors(containerColor = sysBackgroundColor()),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(GcSpacing.Standard),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cat.label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = sysTextColor())
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Key: ${cat.category} | Prefix: ${cat.imagePrefix}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row {
                            IconButton(onClick = { onEditCategory(cat) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Category", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onDeleteCategory(cat) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Category", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
