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
# MediaPipe Graph static init uses Google Flogger; forEnclosingClass() walks the
# stack and fails under R8 with: IllegalStateException: no caller found on the stack.
-keep class com.google.common.flogger.** { *; }
-keep class * extends com.google.common.flogger.backend.Platform$LogCallerFinder { *; }
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

# ── MediaPipe / AutoValue annotation-processor stubs ─────────────────────────
# com.google.auto.value ships its annotation processor code inside the runtime
# JAR; these javax.lang.model / javax.annotation.processing classes only exist
# at compile time and are never called at runtime, so it is safe to suppress.
-dontwarn javax.annotation.processing.AbstractProcessor
-dontwarn javax.annotation.processing.SupportedAnnotationTypes
-dontwarn javax.lang.model.SourceVersion
-dontwarn javax.lang.model.element.Element
-dontwarn javax.lang.model.element.ElementKind
-dontwarn javax.lang.model.element.Modifier
-dontwarn javax.lang.model.type.TypeMirror
-dontwarn javax.lang.model.type.TypeVisitor
-dontwarn javax.lang.model.util.SimpleTypeVisitor8
