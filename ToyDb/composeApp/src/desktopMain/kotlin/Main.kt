import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import club.gepetto.GcLog
import club.gepetto.composeutils.GcTheme
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.PlatformContext
import com.gepetto.toydb.database.createDatabase
import com.gepetto.toydb.database.ToyRepository
import com.gepetto.toydb.service.ImportExportService
import com.gepetto.toydb.service.DesktopSftpService
import com.gepetto.toydb.ui.ToyDbNavigation
import java.io.File
import org.jetbrains.compose.resources.painterResource
import toydb.composeapp.generated.resources.Res
import toydb.composeapp.generated.resources.icon
import okio.FileSystem
import okio.Path.Companion.toPath

fun main(args: Array<String>) {
    // Initialize Coil 3 Image Loader for Desktop
    SingletonImageLoader.setSafe {
        ImageLoader.Builder(PlatformContext.INSTANCE)
            .build()
    }

    // Open/Create local SQLite database
    val appDataDir = club.gepetto.utils.getAppDataDir("ToyDatabaseManager")
    val dbFile = File(appDataDir, "toydb.db")
    val isInitialInstall = !dbFile.exists() || dbFile.length() < 50000L
    
    // Auto-populate from packaged default database if empty or non-existent
    if (isInitialInstall) {
        try {
            dbFile.parentFile?.mkdirs()
            kotlinx.coroutines.runBlocking {
                val bytes = Res.readBytes("files/default_toydb.db")
                dbFile.writeBytes(bytes)
            }
            println("Database successfully initialized from packaged resources.")
        } catch (e: Exception) {
            System.err.println("Failed to initialize database from resources: ${e.message}")
        }
    }
    val database = createDatabase(null, dbFile.absolutePath)
    com.gepetto.toydb.database.checkUpgrade(database)

    if (isInitialInstall) {
        try {
            database.execute("DELETE FROM toys")
            GcLog.d("ToyDbMain", "Initial install detected. Cleared toys table, keeping pre-populated makers and settings.")
        } catch (e: Exception) {
            GcLog.e("ToyDbMain", "Failed to clear toys table on initial install: ${e.message}", e)
        }
    }

    if (args.contains("--headless-import-export")) {
        runHeadlessImportExport(database)
        database.close()
        return
    }

    if (args.contains("--headless-export-html")) {
        runHeadlessExportHtml(database)
        database.close()
        return
    }

    application {
        val windowState = rememberWindowState(
            width = 1200.dp,
            height = 800.dp
        )
        val sftpService = remember { DesktopSftpService() }
        val repository = remember { ToyRepository(database) }
        var appTitle by remember { mutableStateOf(repository.getAppTitleSetting()) }

        Window(
            onCloseRequest = {
                database.close()
                exitApplication()
            },
            state = windowState,
            title = appTitle,
            icon = painterResource(Res.drawable.icon)
        ) {
            GcTheme {
                ToyDbNavigation(database, sftpService, onAppTitleChanged = { appTitle = it })
            }
        }
    }
}

fun runHeadlessImportExport(db: com.gepetto.toydb.database.ToyDatabase) {
    println("--- Headless Import & Export Mode ---")
    val repository = ToyRepository(db)
    val dataPath = repository.getDataPathSetting()
    val homeDir = System.getProperty("user.home")
    val defaultJsonPath = if (homeDir != null) "$homeDir/valdetaro/ToyCollection/ToyDb/json" else "json"
    val jsonDir = dataPath ?: defaultJsonPath
    
    fun readJson(fileName: String): String? {
        val path = "$jsonDir/$fileName".toPath()
        return if (FileSystem.SYSTEM.exists(path)) {
            FileSystem.SYSTEM.read(path) { readUtf8() }
        } else null
    }

    fun writeJson(fileName: String, content: String) {
        val path = "$jsonDir/$fileName".toPath()
        FileSystem.SYSTEM.write(path) { writeUtf8(content) }
        println("Wrote $fileName")
    }

    // 1. Import
    println("Importing settings and data...")
    
    // Clear toys & makers first for a clean import
    db.execute("DELETE FROM toys")
    db.execute("DELETE FROM makers")
    
    // Category settings
    val catSettingsContent = readJson("category_settings.json")
    if (catSettingsContent != null) {
        val count = ImportExportService.importCategorySettings(db, catSettingsContent)
        println("Imported $count category settings.")
    } else {
        println("category_settings.json not found, using default categories.")
    }


    // Makers
    val makersContent = readJson("carmaker.json") ?: readJson("makers.json")
    if (makersContent != null) {
        val count = ImportExportService.importMakers(db, makersContent)
        println("Imported $count makers.")
    }

    // Toys for all active categories
    val categories = repository.getCategorySettings()
    categories.forEach { cat ->
        val content = readJson("${cat.imagePrefix}list.json")
            ?: readJson("${cat.category}s.json")
            ?: readJson("${cat.category}list.json")
            ?: readJson("${cat.category}.json")
        if (content != null) {
            val count = ImportExportService.importToys(db, cat.category, content)
            println("Imported $count toys for category ${cat.category} (${cat.label}).")
        } else {
            println("No JSON file found for category ${cat.category} (${cat.label}).")
        }
    }

    // 2. Export
    println("Exporting settings and data back to JSON...")
    
    // Export makers
    val makersJson = ImportExportService.exportMakers(db)
    writeJson("carmaker.json", makersJson)

    // Export category settings
    val categorySettingsJson = ImportExportService.exportCategorySettings(db)
    writeJson("category_settings.json", categorySettingsJson)


    // Export toys
    categories.forEach { cat ->
        val toysJson = ImportExportService.exportToys(db, cat.category)
        writeJson("${cat.imagePrefix}list.json", toysJson)
    }
    
    println("Headless Import & Export completed successfully.")
}

fun runHeadlessExportHtml(db: com.gepetto.toydb.database.ToyDatabase) {
    println("--- Headless HTML Export Mode ---")
    val repository = ToyRepository(db)
    val homeDir = System.getProperty("user.home")
    val defaultExportDir = if (homeDir != null) "$homeDir/slots/slots" else "slots"
    val exportDir = repository.getDataPathSetting() ?: defaultExportDir
    println("Exporting HTML pages to: $exportDir")
    val count = ImportExportService.exportHtml(db, exportDir)
    println("Headless HTML Export completed successfully. Generated $count files.")
}
