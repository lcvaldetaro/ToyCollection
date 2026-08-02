package com.gepetto.toydb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gepetto.toydb.database.ToyRepository
import com.gepetto.toydb.database.createDatabase
import com.gepetto.toydb.service.AndroidSftpService
import com.gepetto.toydb.ui.ToyDbNavigation
import com.gepetto.toydb.utils.ImageResolverConfig
import kotlinx.coroutines.runBlocking
import toydb.composeapp.generated.resources.Res
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import java.io.File

class AppMainActivity : ComponentActivity() {
    companion object {
        init {
            Security.removeProvider("BC")
            Security.insertProviderAt(BouncyCastleProvider(), 1)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val dbFile = getDatabasePath("toydb.db")
        val isInitialInstall = !dbFile.exists() || dbFile.length() < 50000L

        if (isInitialInstall) {
            try {
                dbFile.parentFile?.mkdirs()
                runBlocking {
                    val bytes = Res.readBytes("files/default_toydb.db")
                    dbFile.writeBytes(bytes)
                }
                android.util.Log.d("AppMainActivity", "Database successfully initialized from packaged resources.")
            } catch (e: Exception) {
                android.util.Log.e("AppMainActivity", "Failed to initialize database from resources: ${e.message}", e)
            }
        }

        val database = createDatabase(this, dbFile.absolutePath)

        if (isInitialInstall) {
            try {
                database.execute("DELETE FROM toys")
                android.util.Log.d("AppMainActivity", "Initial install detected. Cleared toys table, keeping pre-populated makers and settings.")
            } catch (e: Exception) {
                android.util.Log.e("AppMainActivity", "Failed to clear toys table on initial install: ${e.message}", e)
            }
        }

        // Ensure data directory exists
        val dataDir = File(filesDir, "data")
        if (!dataDir.exists()) {
            dataDir.mkdirs()
        }

        // Initialize settings with fixed paths
        val repository = ToyRepository(database)
        if (repository.getDataPathSetting().isNullOrEmpty()) {
            repository.setDataPathSetting(dataDir.absolutePath)
        }
        ImageResolverConfig.imagesPath = repository.getDataPathSetting()

        val sftpService = AndroidSftpService()

        setContent {
            ToyDbNavigation(
                db = database,
                sftpService = sftpService
            )
        }
    }
}
