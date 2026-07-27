plugins {
    // Declare the Kotlin Multiplatform, Android, Serialization and Compose compiler plugins.
    // They are configured but not applied to the root project itself.
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    id("org.jetbrains.kotlin.multiplatform") version "2.4.0" apply false
    id("org.jetbrains.compose") version "1.11.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0" apply false
}

// Subprojects resolutionStrategy removed to allow normal dependency resolution in Compose 1.11.0


