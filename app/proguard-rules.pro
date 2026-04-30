# ──────────────────────────────────────────────────────────────────────────────
# DriveSwipe – Release ProGuard / R8 rules
# ──────────────────────────────────────────────────────────────────────────────

# Keep line-number information for crash stack-traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── App models / service contract ─────────────────────────────────────────────
# Preserve all data classes and enums used across service <-> UI boundaries
# so that name-based serialisation (Intent extras, broadcasts) keeps working.
-keep class com.example.driveswipe.** { *; }

# ── Kotlin ────────────────────────────────────────────────────────────────────
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$Companion { *; }
-keepclassmembers class ** {
    @kotlin.jvm.JvmStatic *;
    @kotlin.jvm.JvmField *;
}

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ── AndroidX / Jetpack ────────────────────────────────────────────────────────
# Lifecycle & ViewModel
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel { <init>(...); }

# DataStore
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# Navigation Compose
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# Compose runtime internals (required by R8 for reflection-based slot table)
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# ── CameraX ───────────────────────────────────────────────────────────────────
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ── MediaPipe Tasks Vision ────────────────────────────────────────────────────
# MediaPipe uses JNI + protobuf reflection; keep everything under its namespace.
-keep class com.google.mediapipe.** { *; }
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.mediapipe.**
-dontwarn com.google.protobuf.**

# MediaPipe native libraries loaded by name
-keepclasseswithmembernames class * {
    native <methods>;
}

# ── Serialisation safety ──────────────────────────────────────────────────────
# Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Enum valueOf / values() used reflectively (e.g. DriveAction.valueOf in service)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── Prevent Play Protect / store analyser warnings ────────────────────────────
# Do NOT suppress legitimate warnings — instead ensure sensitive-API call sites
# are wrapped in clearly named methods so automated scanners see the context.
# The rules below prevent R8 from inlining those wrappers away.
-keepclassmembers class com.example.driveswipe.GestureService {
    private void attachOverlay();
    private void detachOverlay();
}
-keepclassmembers class com.example.driveswipe.MainActivity {
    public void openOverlaySettings();
}

# ── Miscellaneous ─────────────────────────────────────────────────────────────
-dontwarn sun.misc.**
-dontwarn java.lang.invoke.**
-dontwarn org.conscrypt.**
