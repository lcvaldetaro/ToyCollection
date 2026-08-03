plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.application)
}

base {
    val vName = libs.versions.versionName.get()
    val vCode = libs.versions.versionCode.get()
    archivesName.set("GepettoToyDatabaseManager-v$vName-($vCode)")
}

val desktopMajor = libs.versions.versionName.get().split(".").getOrElse(0) { "1" }
val desktopMinor = libs.versions.versionName.get().split(".").getOrElse(1) { "0" }
val desktopBuildNum = libs.versions.versionCode.get()
val desktopPackageVersion = "${desktopMajor}.${desktopMinor}.${desktopBuildNum}"

val generateCommonConfig = tasks.register("generateCommonConfig") {
    val vName = libs.versions.versionName.get()
    val vCode = libs.versions.versionCode.get().toLong()
    val outputDir = layout.buildDirectory.dir("generated/commonConfig/kotlin").get().asFile
    val outputFile = File(outputDir, "com/gepetto/toydb/CommonConfig.kt")
    
    inputs.property("versionName", vName)
    inputs.property("versionCode", vCode)
    outputs.dir(outputDir)

    doLast {
        outputFile.parentFile.mkdirs()
        outputFile.writeText("""
            package com.gepetto.toydb

            object CommonConfig {
                const val versionName = "$vName"
                const val versionCode = ${vCode}L
                const val versionCodeString = "$vCode"
            }
        """.trimIndent())
    }
}

val prepareAndroidResources = tasks.register<Copy>("prepareAndroidResources") {
    from("src/commonMain/composeResources") {
        include("values/**")
        include("drawable/**")
    }
    into(layout.buildDirectory.dir("generated/android/res"))
}

tasks.configureEach {
    if (name == "preBuild") {
        dependsOn(prepareAndroidResources)
    }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
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

    sourceSets {
        val commonMain = sourceSets.getByName("commonMain")
        commonMain.kotlin.srcDir(generateCommonConfig)
        commonMain.dependencies {
            implementation(libs.circum)
            implementation(libs.gepetto.utils)
            implementation(libs.gepetto.gclog)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.ui.tooling.preview)

            implementation(libs.kotlinx.serialization.json)
            implementation(libs.coil3.coil.compose)
            implementation(libs.okio)

            // Navigation3
            implementation(libs.androidx.navigation3.runtime)
            implementation(libs.androidx.navigation3.ui)
            implementation(libs.androidx.lifecycle.viewmodel.navigation3)

            // Adaptive layout
            implementation(libs.adaptive)
            implementation(libs.adaptive.layout)
            implementation(libs.adaptive.navigation)
        }

        val desktopMain = sourceSets.getByName("desktopMain")
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            // SQLite JDBC driver for desktop SQLite support
            implementation("org.xerial:sqlite-jdbc:3.45.1.0")
            implementation(libs.sshj)
            implementation(libs.slf4j.simple)
        }

        val androidMain = sourceSets.getByName("androidMain")
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.appcompat)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.sshj)
            implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
            implementation(libs.androidx.ui.tooling)
            implementation(libs.androidx.ui.tooling.preview)
        }
    }
}

android {
    namespace = "com.gepetto.toydb"
    compileSdk = libs.versions.compileSdk.get().toInt()

    signingConfigs {
        create("release") {
            storeFile = file(project.findProperty("gepetto.lapcounter.store_file") as String? ?: "release.keystore")
            storePassword = project.findProperty("gepetto.store_psw") as String?
            keyAlias = project.findProperty("gepetto.key_alias") as String?
            keyPassword = project.findProperty("gepetto.key_psw") as String?
        }
    }

    defaultConfig {
        applicationId = "com.gepetto.toydb"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()
    }
    sourceSets {
        getByName("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
            res.srcDirs(
                prepareAndroidResources,
                "src/androidMain/res"
            )
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            isMinifyEnabled = false
            isDebuggable = true
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

compose.resources {
    publicResClass = true
}

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
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi
            )
            packageName = "Gepetto Toy Database Manager"
            packageVersion = desktopPackageVersion
            modules("java.sql")

            macOS {
                packageName = "Gepetto Toy Database Manager"
                iconFile.set(project.file("src/desktopMain/resources/icons/icon.icns"))
                bundleID = "com.gepetto.toydb.manager"
            }
            windows {
                packageName = "Gepetto Toy Database Manager"
                iconFile.set(project.file("src/desktopMain/resources/icons/icon.ico"))
                shortcut = true
                menu = true
                upgradeUuid = "a2f4c3d8-5b4e-4f3a-9c7d-8e9f0a1b2c3d"
            }
        }
    }
}

tasks.matching { it.name == "packageDmg" }.configureEach {
    doFirst {
        val resourcesDir = project.file("build/compose/tmp/resources")
        println("[VolumeIconHook] packageDmg doFirst started. Deleting resources directory.")
        if (resourcesDir.exists()) {
            resourcesDir.deleteRecursively()
        }
        val sourceIcon = project.file("packaging/macos/Gepetto Toy Database Manager-volume.icns")
        if (sourceIcon.exists()) {
            Thread {
                val startTime = System.currentTimeMillis()
                val timeout = 300000L
                var copied = false
                val targetIcon = File(resourcesDir, "Gepetto Toy Database Manager-volume.icns")
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
        val targetIcon = File(resourcesDir, "Gepetto Toy Database Manager-volume.icns")
        println("[VolumeIconHook] Target icon exists at the end: ${targetIcon.exists()}")
        if (resourcesDir.exists()) {
            println("[VolumeIconHook] Files at the end: ${resourcesDir.list()?.joinToString()}")
        }
        val dmgDir = File(layout.buildDirectory.get().asFile, "compose/binaries/main/dmg")
        val generatedFile = File(dmgDir, "Gepetto Toy Database Manager-${desktopPackageVersion}.dmg")
        val targetFile = File(dmgDir, "toydatabasemanager.dmg")
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
        val generatedFile = File(msiDir, "Gepetto Toy Database Manager-${desktopPackageVersion}.msi")
        val targetFile = File(msiDir, "toydatabasemanager.msi")
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
