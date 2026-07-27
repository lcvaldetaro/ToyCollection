package com.gepetto.toys

import android.app.Application
import android.content.Context
import club.gepetto.composeutils.image.gCnewImageLoader
import club.gepetto.utils.GcAppInfo
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.PlatformContext
import com.gepetto.common.GcFile
import com.gepetto.common.Common
import com.gepetto.toycollection.toyCollectionModule
import com.gepetto.common.WEBSITE_BASE_URL
import club.gepetto.composeutils.image.gCsetImagesBaseUrl
import org.koin.core.context.startKoin
import club.gepetto.utils.gcGetAppBuild
import club.gepetto.utils.gcGetAppFolder
import club.gepetto.utils.gcGetAppVersion
import club.gepetto.utils.ioCoroutine
import com.gepetto.common.models.Settings
import club.gepetto.GcLog
import java.io.File

class App : Application(), SingletonImageLoader.Factory {
    companion object {
        lateinit var appContext : Context
        lateinit var directoryFile : File
        lateinit var packageFolder: String

        var versionString = ""
        var versionBuild  = 0L
        var appName = ""
        var releaseVersion = true
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        Common.imageLoader = gCnewImageLoader(context)
        return Common.imageLoader
    }

    override fun onCreate() {
        super.onCreate()

        GcLog.plant(GcLog.DebugTree())

        // Start Koin
        val androidModule = org.koin.dsl.module {
            factory { AppIntentProcessor() }
        }

        startKoin {
            modules(toyCollectionModule, androidModule)
        }

        GcAppInfo.initialize(this)
        gCsetImagesBaseUrl(WEBSITE_BASE_URL)

        appContext = this
        directoryFile = appContext.dataDir

        packageFolder = gcGetAppFolder(this)
        versionString = GcAppInfo.versionName!!
        versionBuild  = GcAppInfo.versionCode!!
        releaseVersion = GcAppInfo.releaseVersion

        appName = this.getString(R.string.app_name)

        ioCoroutine {
            var settings = Settings.restore()
            if (settings != null) {
                Common.caching = settings.caching
                Common.forceLightMode = settings.forceLightMode
                Common.forceDarkMode = settings.forceDarkMode
                Common.customBaseUrl = settings.baseUrl
            }
            else {
                settings = Settings()
                settings.save()
            }
            gCsetImagesBaseUrl(Common.getActiveBaseUrl())
        }

        Common.directoryFile = GcFile(directoryFile)
        Common.packageFolder = packageFolder
        Common.versionString = versionString
        Common.versionBuild = versionBuild
        Common.releaseVersion = releaseVersion
        Common.appName = appName
    }
}


