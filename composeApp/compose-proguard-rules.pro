# Suppress warnings from OkHttp's optional platform dependencies
-dontwarn okhttp3.internal.platform.**
-dontwarn okhttp3.internal.graal.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn org.bouncycastle.**
-dontwarn io.ktor.network.sockets.**
-dontwarn net.schmizz.sshj.**

# GraalVM / Native Image classes
-dontwarn org.graalvm.nativeimage.**
-dontwarn com.oracle.svm.core.annotate.**

# Kotlin compiler generated classes
-dontwarn kotlin.Deprecated$Container
