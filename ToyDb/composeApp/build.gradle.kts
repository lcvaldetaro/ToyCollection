plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    alias(libs.plugins.kotlin.serialization)
}

val desktopMajor = libs.versions.versionName.get().split(".").getOrElse(0) { "1" }
val desktopMinor = libs.versions.versionName.get().split(".").getOrElse(1) { "0" }
val desktopBuildNum = libs.versions.versionCode.get()
val desktopPackageVersion = "${desktopMajor}.${desktopMinor}.${desktopBuildNum}"

kotlin {
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
        }
    }
}

compose.resources {
    publicResClass = true
}

compose.desktop {
    application {
        mainClass = "MainKt"
        javaHome = System.getenv("JAVA_HOME") ?: System.getProperty("java.home")

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
