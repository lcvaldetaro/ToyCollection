import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.gepetto.common.Common
import com.gepetto.common.GcFile
import com.gepetto.toycollection.toyCollectionModule
import com.gepetto.toycollection.ui.ToyCollectionNavigation
import org.koin.compose.KoinContext
import org.koin.core.context.startKoin
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.PlatformContext
import com.gepetto.common.models.Settings
import club.gepetto.composeutils.image.gCsetImagesBaseUrl

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // 1. Initialize Koin DI
    startKoin {
        modules(toyCollectionModule)
    }

    // 2. Initialize directory paths and application metadata
    Common.directoryFile = GcFile("cache")
    Common.packageFolder = "cache/"

    Common.appName = WEB_APPNAME
    Common.versionString = com.gepetto.toys.CommonConfig.versionName
    Common.versionBuild = com.gepetto.toys.CommonConfig.webVersionCode
    Common.releaseVersion = false

    val settings = Settings.restore()
    if (settings != null) {
        Common.caching = settings.caching
        Common.forceLightMode = settings.forceLightMode
        Common.forceDarkMode = settings.forceDarkMode
        Common.customBaseUrl = settings.baseUrl
    }

    gCsetImagesBaseUrl(Common.getActiveBaseUrl())

    // 3. Initialize Coil 3 Image Loader
    SingletonImageLoader.setSafe { ImageLoader.Builder(PlatformContext.INSTANCE).build() }
    Common.imageLoader = SingletonImageLoader.get(PlatformContext.INSTANCE)

    // 4. Launch Compose Web
    ComposeViewport(viewportContainerId = "compose-App") {
        KoinContext {
            club.gepetto.composeutils.GcTheme(darkTheme = false) {
                ToyCollectionNavigation()
            }
        }
    }
}
