import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import com.gepetto.common.GcFile
import com.gepetto.common.Common
import com.gepetto.common.models.Settings
import com.gepetto.toycollection.toyCollectionModule
import com.gepetto.toycollection.ui.ToyCollectionNavigation
import org.koin.compose.KoinContext
import org.koin.core.context.startKoin
import java.io.File
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.PlatformContext

import club.gepetto.composeutils.image.gCsetImagesBaseUrl
import com.gepetto.common.WEBSITE_BASE_URL
import org.jetbrains.compose.resources.painterResource
import toycollectionmultiplatform.shared.common.generated.resources.Res
import toycollectionmultiplatform.shared.common.generated.resources.icon


fun main() {
    // Override disabled algorithms to re-enable TLS_RSA_* ciphers if they are disabled by the JVM configuration
    try {
        val disabledAlgorithms = java.security.Security.getProperty("jdk.tls.disabledAlgorithms")
        if (disabledAlgorithms != null && disabledAlgorithms.contains("TLS_RSA_*")) {
            val newDisabledAlgorithms = disabledAlgorithms
                .replace(", TLS_RSA_*", "")
                .replace("TLS_RSA_*, ", "")
                .replace("TLS_RSA_*", "")
            java.security.Security.setProperty("jdk.tls.disabledAlgorithms", newDisabledAlgorithms)
            println("Security Override: Removed TLS_RSA_* from disabled algorithms list.")
        }
    } catch (e: Exception) {
        System.err.println("Failed to override jdk.tls.disabledAlgorithms: ${e.message}")
    }

    // 1. Initialize Koin DI
    startKoin {
        modules(toyCollectionModule)
    }



    val cacheDir = club.gepetto.utils.getAppDataDir("GepettoToyCollection")

    Common.directoryFile = GcFile(cacheDir)
    Common.packageFolder = cacheDir.absolutePath + File.separator

    Common.appName = "Gepetto Toy Collection"
    Common.versionString = com.gepetto.toys.CommonConfig.versionName
    Common.versionBuild = com.gepetto.toys.CommonConfig.desktopVersionCode
    Common.releaseVersion = false

    val settings = Settings.restore()
    if (settings != null) {
        Common.caching = settings.caching
        Common.forceLightMode = settings.forceLightMode
        Common.forceDarkMode = settings.forceDarkMode
        Common.customBaseUrl = settings.baseUrl
    } else {
        Settings().save()
    }

    gCsetImagesBaseUrl(Common.getActiveBaseUrl())



    // 3. Initialize Coil 3 Image Loader
    SingletonImageLoader.setSafe {
        ImageLoader.Builder(PlatformContext.INSTANCE)
            .build()
    }
    Common.imageLoader = SingletonImageLoader.get(PlatformContext.INSTANCE)

    // 4. Launch Compose Desktop Window
    application {
        val windowState = rememberWindowState(
            width = 1200.dp,
            height = 800.dp
        )
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = Common.appName,
            icon = painterResource(Res.drawable.icon)
        ) {
            KoinContext {
                club.gepetto.composeutils.GcTheme {
                    ToyCollectionNavigation()
                }
            }
        }
    }
}
