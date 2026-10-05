# ==============================================================================
# ProGuard / R8 Rules for Toy Database Manager (ToyDb) - Android Release
# ==============================================================================

# Preserve essential attributes for reflection, annotations, serialization, and stack traces
-keepattributes *Annotation*,InnerClasses,Signature,EnclosingMethod,SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ------------------------------------------------------------------------------
# ToyDb Application Classes, Models, and Services
# ------------------------------------------------------------------------------
-keep class com.gepetto.toydb.** { *; }
-keep interface com.gepetto.toydb.** { *; }
-keepclassmembers class com.gepetto.toydb.** { *; }

# ------------------------------------------------------------------------------
# Kotlinx Serialization
# ------------------------------------------------------------------------------
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    public static final ** Companion;
    public static final ** $serializer;
    public synthetic <init>(...);
    <fields>;
    <methods>;
}
-keepclassmembers class * implements kotlinx.serialization.KSerializer {
    public static final ** INSTANCE;
    public static final ** Companion;
    <fields>;
    <methods>;
}
-keepnames class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# ------------------------------------------------------------------------------
# SSHJ (SSH / SFTP Client) & Cryptographic Providers
# ------------------------------------------------------------------------------
-keep class net.schmizz.sshj.** { *; }
-keep interface net.schmizz.sshj.** { *; }
-keepclassmembers class net.schmizz.sshj.** { *; }
-dontwarn net.schmizz.sshj.**

-keep class org.bouncycastle.** { *; }
-keep interface org.bouncycastle.** { *; }
-keepclassmembers class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

-dontwarn com.jcraft.jzlib.**

# ------------------------------------------------------------------------------
# Ktor Client, OkHttp Engine & Okio
# ------------------------------------------------------------------------------
-keep class io.ktor.** { *; }
-keep interface io.ktor.** { *; }
-dontwarn io.ktor.**

-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# ------------------------------------------------------------------------------
# Coil 3 Image Loading
# ------------------------------------------------------------------------------
-keep class coil3.** { *; }
-keep interface coil3.** { *; }
-dontwarn coil3.**

# ------------------------------------------------------------------------------
# Compose Multiplatform, Navigation 3 & Resources
# ------------------------------------------------------------------------------
-keep class androidx.compose.** { *; }
-keep class org.jetbrains.compose.** { *; }
-keep class toydb.composeapp.generated.resources.** { *; }
-keep class androidx.navigation3.** { *; }
-keep class org.jetbrains.androidx.navigation3.** { *; }
-keep class org.jetbrains.androidx.lifecycle.** { *; }
-dontwarn org.jetbrains.compose.**
-dontwarn androidx.compose.**

# ------------------------------------------------------------------------------
# Gepetto Libraries
# ------------------------------------------------------------------------------
-keep class club.gepetto.** { *; }
-keep interface club.gepetto.** { *; }
-dontwarn club.gepetto.**

# ------------------------------------------------------------------------------
# Kotlin Coroutines
# ------------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ------------------------------------------------------------------------------
# Desktop/JVM-only APIs referenced by shared KMP code
# ------------------------------------------------------------------------------
-dontwarn java.awt.**
-dontwarn java.lang.management.**
-dontwarn javax.naming.**
-dontwarn javax.imageio.**
-dontwarn javax.swing.**
-dontwarn java.beans.**
-dontwarn org.slf4j.**
-dontwarn ch.qos.logback.**
-dontwarn kotlin.Deprecated$Container
