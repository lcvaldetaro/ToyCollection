import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import club.gepetto.GcLog
import club.gepetto.composeutils.GcTheme
import club.gepetto.composeutils.image.gCsetImagesBaseUrl
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.gepetto.toydb.database.ToyRepository
import com.gepetto.toydb.database.WasmToyDatabase
import com.gepetto.toydb.service.WebSftpService
import com.gepetto.toydb.ui.ToyDbNavigation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

private const val TAG_MAIN = "WebMain"

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // ISSUE-27 (Rev 6): GcLog writes nothing until a tree is planted. On wasm, DebugTree prints to the browser console.
    GcLog.plant(GcLog.DebugTree())
    SingletonImageLoader.setSafe { ImageLoader.Builder(PlatformContext.INSTANCE).build() }
    MainScope().launch {
        try {
            val database = WasmToyDatabase.open()
            val repository = ToyRepository(database)
            val savedLanguage = repository.getLanguageSetting()
            com.gepetto.toydb.platform.LocaleHelper.setAppLocale(savedLanguage)
            val baseUrl = repository.getBaseUrlSetting()
            if (!baseUrl.isNullOrBlank()) {
                gCsetImagesBaseUrl(baseUrl.trimEnd('/') + "/")
            }

            // Initial tab title (ISSUE-24). onAppTitleChanged runs only when the user edits the title.
            kotlinx.browser.document.title = repository.getAppTitleSetting()

            // Startup sync runs only on first load (empty toys table). Later syncs are manual.
            val needsInitialSync = database.toysCount() == 0

            ComposeViewport(viewportContainerId = "compose-App") {
                val sftpService = remember { WebSftpService() }
                GcTheme {
                    ToyDbNavigation(
                        db = database,
                        sftpService = sftpService,
                        onAppTitleChanged = { title ->
                            kotlinx.browser.document.title = title
                        },
                        runStartupSync = needsInitialSync
                    )
                }
            }
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            // ISSUE-25: log the start-up error. A visible message is a Phase 9 backlog item.
            // ISSUE-27: GcLog has no tag parameter, so put the tag in the message and pass the throwable first.
            GcLog.e(e, "$TAG_MAIN: web start-up failed: ${e.message}")
        }
    }
}
