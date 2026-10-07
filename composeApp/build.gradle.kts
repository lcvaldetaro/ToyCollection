plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.application)
}

base {
    val vName = libs.versions.versionName.get()
    val vCode = libs.versions.versionCode.get()
    archivesName.set("GepettoToyDatabaseManager-v$vName-($vCode)")
}

val macArch = (project.findProperty("macArch") as String?)?.lowercase() ?: run {
    val isAppleSilicon = try {
        val process = ProcessBuilder("sysctl", "-n", "hw.optional.arm64").start()
        val output = process.inputStream.bufferedReader().readText().trim()
        output == "1"
    } catch (e: Exception) {
        System.getProperty("os.arch").lowercase().contains("aarch64") || System.getProperty("os.arch").lowercase().contains("arm64")
    }
    if (isAppleSilicon) "m1" else "intel"
}
val isArm64 = macArch.contains("aarch64") || macArch.contains("arm64") || macArch == "m1" || macArch == "arm"
val suffix = if (isArm64) "m1" else "intel"


val desktopMajor = libs.versions.versionName.get().split(".").getOrElse(0) { "1" }
val desktopMinor = libs.versions.versionName.get().split(".").getOrElse(1) { "0" }
val desktopBuildNum = libs.versions.versionCode.get()
val desktopPackageVersion = "${desktopMajor}.${desktopMinor}.${desktopBuildNum}"

val generateCommonConfig = tasks.register("generateCommonConfig") {
    val vName = libs.versions.versionName.get()
    val vCode = libs.versions.versionCode.get().toLong()
    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    val desktopCode = if (isWindows) vCode * 10 + 5 else vCode * 10 + 4
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
                const val desktopVersionCode = ${desktopCode}L
                const val webVersionCode = ${vCode * 10 + 6}L
                const val versionCodeString = "$vCode"
            }
        """.trimIndent())
    }
}

val prepareAndroidResources = tasks.register<Copy>("prepareAndroidResources") {
    from("src/commonMain/composeResources") {
        include("values*/**")
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
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
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
            implementation(libs.jetbrains.compose.ui.tooling.preview)
            implementation(libs.kotlincrypto.sha2)

            implementation(libs.kotlinx.serialization.json)
            implementation(libs.coil3.coil.compose)
            implementation(libs.okio)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            // Navigation3
            implementation(libs.androidx.navigation3.runtime)
            implementation(libs.androidx.navigation3.ui)
            implementation(libs.androidx.lifecycle.viewmodel.navigation3)

            // Adaptive layout
            implementation(libs.adaptive)
            implementation(libs.adaptive.layout)
            implementation(libs.adaptive.navigation)
        }

        val commonTest = sourceSets.getByName("commonTest")
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        val desktopMain = sourceSets.getByName("desktopMain")
        desktopMain.dependencies {
            val osName = System.getProperty("os.name").lowercase()
            if (osName.contains("mac")) {
                if (suffix == "m1") {
                    implementation(compose.desktop.macos_arm64)
                } else {
                    implementation(compose.desktop.macos_x64)
                }
            } else {
                implementation(compose.desktop.currentOs)
            }
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.swing)
            // SQLite JDBC driver for desktop SQLite support
            implementation("org.xerial:sqlite-jdbc:3.45.1.0")
            implementation(libs.sshj)
            implementation(libs.slf4j.simple)
        }

        val androidMain = sourceSets.getByName("androidMain")
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.appcompat)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.sshj)
            implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
            implementation(libs.androidx.ui.tooling)
            implementation(libs.androidx.ui.tooling.preview)
        }

        val wasmJsMain = sourceSets.getByName("wasmJsMain")
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
            implementation(npm("sql.js", libs.versions.sqlJs.get()))
            implementation(devNpm("copy-webpack-plugin", libs.versions.copyWebpackPlugin.get()))
        }
    }
}

android {
    namespace = "com.gepetto.toydb"
    compileSdk = libs.versions.compileSdk.get().toInt()

    signingConfigs {
        create("release") {
            val storeFilePath = project.findProperty("gepetto.store_file") as? String
            storeFile = storeFilePath?.let { file(it) }
            storePassword = project.findProperty("gepetto.store_psw") as? String
            keyAlias = project.findProperty("gepetto.key_alias") as? String
            keyPassword = project.findProperty("gepetto.key_psw") as? String
        }
    }

    /*signingConfigs {
        create("release") {
            val customStoreFile = project.findProperty("gepetto.lapcounter.store_file") as String?
            val targetFile = file(customStoreFile ?: "release.keystore")
            if (targetFile.exists()) {
                storeFile = targetFile
                storePassword = project.findProperty("gepetto.store_psw") as String?
                keyAlias = project.findProperty("gepetto.key_alias") as String?
                keyPassword = project.findProperty("gepetto.key_psw") as String?
            }
        }
    }*/

    defaultConfig {
        // TODO change to "com.gepetto.slotcarscollection"
        applicationId = "com.gepetto.slotcarscollection"
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
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile?.exists() == true) {
                signingConfig = releaseSigning
            }
        }
        getByName("debug") {
            isMinifyEnabled = false
            isDebuggable = true
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile?.exists() == true) {
                signingConfig = releaseSigning
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    bundle {
        language {
            enableSplit = false
        }
    }
}

compose.resources {
    publicResClass = true
}

fun getJdkMajorVersion(home: String?): Int {
    if (home.isNullOrEmpty()) return -1
    val releaseFiles = listOf(File(home, "release"), File(home, "../release"))
    for (rf in releaseFiles) {
        if (rf.exists()) {
            try {
                rf.useLines { lines ->
                    for (line in lines) {
                        if (line.startsWith("JAVA_VERSION=")) {
                            val verStr = line.substringAfter("=").trim('"', '\'')
                            val major = verStr.split(".", "-").firstOrNull()?.toIntOrNull() ?: -1
                            if (major > 0) return major
                        }
                    }
                }
            } catch (e: Exception) {}
        }
    }
    val isWin = org.gradle.internal.os.OperatingSystem.current().isWindows
    val javaBin = if (isWin) File(home, "bin/java.exe") else File(home, "bin/java")
    if (javaBin.exists()) {
        try {
            val p = ProcessBuilder(javaBin.absolutePath, "-version").redirectErrorStream(true).start()
            val out = p.inputStream.bufferedReader().readText()
            p.waitFor()
            val match = Regex("""(?:version\s+"(\d+))""").find(out)
            if (match != null) {
                return match.groupValues[1].toIntOrNull() ?: -1
            }
        } catch (e: Exception) {}
    }
    return -1
}

fun isMatchingJdk(home: String?, targetArm64: Boolean): Boolean {
    if (home.isNullOrEmpty()) return false
    val isWin = org.gradle.internal.os.OperatingSystem.current().isWindows
    val javaBin = if (isWin) File(home, "bin/java.exe") else File(home, "bin/java")
    val jpackageBin = if (isWin) File(home, "bin/jpackage.exe") else File(home, "bin/jpackage")
    if (!javaBin.exists() || !jpackageBin.exists()) return false

    val major = getJdkMajorVersion(home)
    if (major in 1..20) return false

    if (!org.gradle.internal.os.OperatingSystem.current().isMacOsX) return true
    return try {
        val p = ProcessBuilder("file", javaBin.absolutePath).start()
        val out = p.inputStream.bufferedReader().readText().lowercase()
        p.waitFor()
        if (targetArm64) (out.contains("arm64") || out.contains("aarch64")) else out.contains("x86_64")
    } catch (e: Exception) {
        false
    }
}

val resolvedJavaHome: String = run {
    if (!org.gradle.internal.os.OperatingSystem.current().isMacOsX) {
        val env = System.getenv("JAVA_HOME")
        if (isMatchingJdk(env, false)) {
            return@run env!!
        }
        val sys = System.getProperty("java.home")
        if (isMatchingJdk(sys, false)) {
            return@run sys
        }
        val jdksDir = File(System.getProperty("user.home"), ".gradle/jdks")
        if (jdksDir.exists() && jdksDir.isDirectory) {
            val jdk21 = jdksDir.walkTopDown()
                .filter { it.name == "Home" || it.name.startsWith("jdk") }
                .find { isMatchingJdk(it.absolutePath, false) }
            if (jdk21 != null) return@run jdk21.absolutePath
        }
        return@run env ?: sys
    }

    if (!isArm64) {
        val prop = project.findProperty("intelJavaHome") as? String
        if (isMatchingJdk(prop, false)) return@run prop!!
    } else {
        val prop = project.findProperty("armJavaHome") as? String
        if (isMatchingJdk(prop, true)) return@run prop!!
    }

    var dir: File? = projectDir
    while (dir != null) {
        val localJdk = File(dir, ".jdk/Contents/Home")
        if (isMatchingJdk(localJdk.absolutePath, isArm64)) {
            return@run localJdk.absolutePath
        }
        val siblingDirs = dir.listFiles()?.filter { it.isDirectory }
        if (siblingDirs != null) {
            for (sib in siblingDirs) {
                val sibJdk = File(sib, ".jdk/Contents/Home")
                if (isMatchingJdk(sibJdk.absolutePath, isArm64)) {
                    return@run sibJdk.absolutePath
                }
            }
        }
        dir = dir.parentFile
    }

    val jdksDir = File(System.getProperty("user.home"), ".gradle/jdks")
    if (jdksDir.exists() && jdksDir.isDirectory) {
        val jdk21Home = jdksDir.walkTopDown()
            .filter { (it.name == "Home" || it.name == "Contents") && (it.absolutePath.contains("21") || it.absolutePath.contains("-21-")) }
            .map { if (it.name == "Contents") File(it, "Home") else it }
            .find { isMatchingJdk(it.absolutePath, isArm64) }
        if (jdk21Home != null) return@run jdk21Home.absolutePath

        val anyMatching = jdksDir.walkTopDown()
            .filter { it.name == "Home" }
            .find { isMatchingJdk(it.absolutePath, isArm64) }
        if (anyMatching != null) return@run anyMatching.absolutePath
    }

    val envJava = System.getenv("JAVA_HOME")
    if (isMatchingJdk(envJava, isArm64)) return@run envJava!!

    val sysJava = System.getProperty("java.home")
    if (isMatchingJdk(sysJava, isArm64)) return@run sysJava!!

    if (org.gradle.internal.os.OperatingSystem.current().isMacOsX) {
        val archArg = if (isArm64) "arm64" else "x86_64"
        try {
            val p = ProcessBuilder("/usr/libexec/java_home", "-a", archArg, "-v", "21").start()
            val out = p.inputStream.bufferedReader().use { it.readText().trim() }
            p.waitFor()
            if (isMatchingJdk(out, isArm64)) return@run out
        } catch (e: Exception) {}
        try {
            val p = ProcessBuilder("/usr/libexec/java_home", "-a", archArg).start()
            val out = p.inputStream.bufferedReader().use { it.readText().trim() }
            p.waitFor()
            if (isMatchingJdk(out, isArm64)) return@run out
        } catch (e: Exception) {}

        val jvmDir = File("/Library/Java/JavaVirtualMachines")
        if (jvmDir.exists() && jvmDir.isDirectory) {
            val jdk21 = jvmDir.listFiles()
                ?.filter { it.name.contains("21") }
                ?.map { File(it, "Contents/Home") }
                ?.find { isMatchingJdk(it.absolutePath, isArm64) }
            if (jdk21 != null) return@run jdk21.absolutePath

            val anyJvm = jvmDir.listFiles()
                ?.map { File(it, "Contents/Home") }
                ?.find { isMatchingJdk(it.absolutePath, isArm64) }
            if (anyJvm != null) return@run anyJvm.absolutePath
        }

        val brewCandidate = if (isArm64) "/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home" else "/usr/local/opt/openjdk/libexec/openjdk.jdk/Contents/Home"
        if (isMatchingJdk(brewCandidate, isArm64)) return@run brewCandidate
    }

    val fallback = System.getenv("JAVA_HOME") ?: System.getProperty("java.home")
    if (isMatchingJdk(fallback, isArm64)) return@run fallback

    if (org.gradle.internal.os.OperatingSystem.current().isMacOsX) {
        val targetName = if (isArm64) "ARM64" else "Intel x86_64"
        throw GradleException("Could not locate a valid JDK 21+ with jpackage for target architecture: $targetName")
    }
    fallback
}
println("[ArchitectureConfig] Target: macArch=$macArch (isArm64=$isArm64) -> javaHome: $resolvedJavaHome")

compose.desktop {
    application {
        mainClass = "MainKt"
        javaHome = resolvedJavaHome

        buildTypes.release.proguard {
            configurationFiles.from(project.file("compose-proguard-rules.pro"))
        }

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
        if (generatedFile.exists()) {
            val outputsDmgDir = File(layout.buildDirectory.get().asFile, "outputs/dmg")
            outputsDmgDir.mkdirs()
            val targetFile = File(outputsDmgDir, "toydatabasemanager-$suffix.dmg")
            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (generatedFile.renameTo(targetFile)) {
                println("Moved and renamed DMG to ${targetFile.absolutePath}")
            } else {
                println("Failed to move/rename DMG")
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
        if (generatedFile.exists()) {
            val outputsMsiDir = File(layout.buildDirectory.get().asFile, "outputs/msi")
            outputsMsiDir.mkdirs()
            val targetFile = File(outputsMsiDir, "toydatabasemanager.msi")
            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (generatedFile.renameTo(targetFile)) {
                println("Moved and renamed MSI to ${targetFile.absolutePath}")
            } else {
                println("Failed to move/rename MSI")
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

tasks.configureEach {
    if (name.contains("package") || name.contains("createDistributable") || name.contains("createRuntimeImage")) {
        inputs.property("macArch", macArch)
    }
}
