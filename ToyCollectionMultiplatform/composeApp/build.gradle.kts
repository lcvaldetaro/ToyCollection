import java.io.File
import java.nio.file.Paths

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    alias(libs.plugins.android.application)
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

base {
    val vName = libs.versions.versionName.get()
    val vCode = libs.versions.versionCode.get()
    archivesName.set("GepettoToysCollection-v$vName-($vCode)")
}

val generateCommonConfig = tasks.register("generateCommonConfig") {
    val vName = libs.versions.versionName.get()
    val vCode = libs.versions.versionCode.get().toLong()
    val outputDir = layout.buildDirectory.dir("generated/commonConfig/kotlin").get().asFile
    val outputFile = File(outputDir, "com/gepetto/toys/CommonConfig.kt")
    
    inputs.property("versionName", vName)
    inputs.property("versionCode", vCode)
    outputs.dir(outputDir)

    doLast {
        outputFile.parentFile.mkdirs()
        outputFile.writeText("""
            package com.gepetto.toys

            object CommonConfig {
                const val versionName = "$vName"
                const val desktopVersionCode = ${vCode*10+4}L
                const val webVersionCode = ${vCode*10+6}L
            }
        """.trimIndent())
    }
}

android {
    namespace = "com.gepetto.toys"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.gepetto.slotcarscollection"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()
    }
    
    signingConfigs {
        create("release") {
            val storeFilePath = project.findProperty("gepetto.store_file") as? String
            storeFile = storeFilePath?.let { file(it) }
            storePassword = project.findProperty("gepetto.store_psw") as? String
            keyAlias = project.findProperty("gepetto.key_alias") as? String
            keyPassword = project.findProperty("gepetto.key_psw") as? String
        }
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
                "-opt-in=androidx.compose.animation.ExperimentalSharedTransitionApi",
                "-opt-in=androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi",
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
            )
        }
    }
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
                "-opt-in=androidx.compose.animation.ExperimentalSharedTransitionApi",
                "-opt-in=androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi",
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
            )
        }
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir(generateCommonConfig)
            dependencies {
                implementation(project(":shared:common"))
                implementation(libs.circum)
                implementation(libs.gepetto.utils)
                implementation(libs.gepetto.gclog)
                implementation(project(":feature:toycollection"))

                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)

                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.coil3.coil.compose)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.appcompat)
                implementation(libs.kotlinx.coroutines.android)
            }
        }
        
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}

val desktopMajor = libs.versions.versionName.get().split(".").getOrElse(0) { "1" }
val desktopMinor = libs.versions.versionName.get().split(".").getOrElse(1) { "0" }
val desktopBuildNum = libs.versions.versionCode.get()
val desktopPackageVersion = "$desktopMajor.$desktopMinor.$desktopBuildNum"

val resolvedJavaHome: String? = run {
    var found: String? = null
    var dir: File? = projectDir
    while (dir != null) {
        val localJdk = File(dir, ".jdk/Contents/Home")
        if (File(localJdk, "bin/jpackage").exists()) {
            found = localJdk.absolutePath
            break
        }
        val siblingDirs = dir.listFiles()?.filter { it.isDirectory }
        if (siblingDirs != null) {
            for (sib in siblingDirs) {
                val sibJdk = File(sib, ".jdk/Contents/Home")
                if (File(sibJdk, "bin/jpackage").exists()) {
                    found = sibJdk.absolutePath
                    break
                }
            }
        }
        if (found != null) break
        dir = dir.parentFile
    }
    if (found == null) {
        val env = System.getenv("JAVA_HOME")
        if (!env.isNullOrEmpty() && File(File(env), "bin/jpackage").exists()) {
            found = env
        }
    }
    if (found == null) {
        val sys = System.getProperty("java.home")
        if (!sys.isNullOrEmpty() && File(File(sys), "bin/jpackage").exists()) {
            found = sys
        }
    }
    if (found == null) {
        try {
            val process = ProcessBuilder("/usr/libexec/java_home").start()
            val path = process.inputStream.bufferedReader().readText().trim()
            if (path.isNotEmpty() && File(File(path), "bin/jpackage").exists()) {
                found = path
            }
        } catch (e: Exception) {}
    }
    found
}

compose.desktop {
    application {
        mainClass = "MainKt"
        javaHome = resolvedJavaHome ?: System.getProperty("java.home")

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Pkg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe
            )
            packageName = "Gepetto Toy Collection"
            packageVersion = desktopPackageVersion
            modules("jdk.crypto.ec")
            macOS {
                packageName = "Gepetto Toy Collection"
                iconFile.set(project.file("src/desktopMain/resources/icons/icon.icns"))
            }
            windows {
                packageName = "Gepetto Toy Collection"
                iconFile.set(project.file("src/desktopMain/resources/icons/icon.ico"))
                shortcut = true
                menu = true
                upgradeUuid = "1b6e4b95-df63-479c-9c71-f925b306b9a8"
            }
        }
    }
}

tasks.register<Exec>("packageMacPkg") {
    dependsOn("createDistributable")
    
    val buildDir = layout.buildDirectory.get().asFile
    val javaHome = System.getProperty("java.home")
    val jpackagePath = Paths.get(javaHome, "bin", "jpackage").toString()
    val appImageDir = File(buildDir, "compose/binaries/main/app/Gepetto Toy Collection.app").absolutePath
    val destDir = File(buildDir, "compose/binaries/main/pkg").absolutePath

    executable(jpackagePath)
    args(
        "--type", "pkg",
        "--app-image", appImageDir,
        "--dest", destDir,
        "--name", "Gepetto Toy Collection",
        "--app-version", desktopPackageVersion
    )
    
    doLast {
        println("PKG package created successfully under: $destDir")
    }
}

tasks.matching { it.name == "packageDmg" }.configureEach {
    doFirst {
        val resourcesDir = project.file("build/compose/tmp/resources")
        println("[VolumeIconHook] packageDmg doFirst started. Deleting resources directory.")
        if (resourcesDir.exists()) {
            resourcesDir.deleteRecursively()
        }
        val sourceIcon = project.file("packaging/macos/Gepetto Toy Collection-volume.icns")
        if (sourceIcon.exists()) {
            Thread {
                val startTime = System.currentTimeMillis()
                val timeout = 300000L
                var copied = false
                val targetIcon = File(resourcesDir, "Gepetto Toy Collection-volume.icns")
                val triggerFile = File(resourcesDir, "Info.plist")
                while (System.currentTimeMillis() - startTime < timeout) {
                    if (triggerFile.exists()) {
                        Thread.sleep(50)
                        try {
                            sourceIcon.copyTo(targetIcon, overwrite = true)
                            println("[VolumeIconHook] Successfully copied icon to ${targetIcon.absolutePath}")
                            copied = true
                            break
                        } catch (e: Exception) {
                            Thread.sleep(50)
                        }
                    }
                    Thread.sleep(20)
                }
                if (!copied) {
                    println("[VolumeIconHook] Failed to copy icon: timeout or trigger not found")
                }
            }.start()
        } else {
            println("[VolumeIconHook] Source icon not found at ${sourceIcon.absolutePath}")
        }
    }
    doLast {
        val resourcesDir = project.file("build/compose/tmp/resources")
        val targetIcon = File(resourcesDir, "Gepetto Toy Collection-volume.icns")
        println("[VolumeIconHook] Target icon exists at the end: ${targetIcon.exists()}")
        if (resourcesDir.exists()) {
            println("[VolumeIconHook] Files at the end: ${resourcesDir.list()?.joinToString()}")
        }
        val dmgDir = File(layout.buildDirectory.get().asFile, "compose/binaries/main/dmg")
        val generatedFile = File(dmgDir, "Gepetto Toy Collection-$desktopPackageVersion.dmg")
        val targetFile = File(dmgDir, "gepettotoycollection.dmg")
        if (generatedFile.exists()) {
            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (generatedFile.renameTo(targetFile)) {
                println("Renamed DMG to ${targetFile.name}")
            } else {
                println("Failed to rename DMG")
            }
        } else {
            println("Generated DMG file not found at ${generatedFile.absolutePath}")
        }
    }
}

tasks.matching { it.name == "packageMsi" }.configureEach {
    doLast {
        val msiDir = File(layout.buildDirectory.get().asFile, "compose/binaries/main/msi")
        val generatedFile = File(msiDir, "Gepetto Toy Collection-$desktopPackageVersion.msi")
        val targetFile = File(msiDir, "gepettotoycollection.msi")
        if (generatedFile.exists()) {
            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (generatedFile.renameTo(targetFile)) {
                println("Renamed MSI to ${targetFile.name}")
            } else {
                println("Failed to rename MSI")
            }
        } else {
            println("Generated MSI file not found at ${generatedFile.absolutePath}")
        }
    }
}

tasks.withType<org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask>().configureEach {
    if (name.contains("Msi", ignoreCase = true)) {
        freeArgs.add("--resource-dir")
        freeArgs.add(project.file("wix").absolutePath)
    }
}




