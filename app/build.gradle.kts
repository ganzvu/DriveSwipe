import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

// Resolve signing values: keystore.properties takes priority, then environment variables.
fun signingValue(propKey: String, envKey: String): String? =
    (keystoreProperties[propKey] as? String)?.takeIf { it.isNotBlank() }
        ?: System.getenv(envKey)?.takeIf { it.isNotBlank() }

val releaseStoreFile    = signingValue("storeFile",    "KEYSTORE_FILE")
val releaseStorePass    = signingValue("storePassword", "KEYSTORE_STORE_PASSWORD")
val releaseKeyAlias     = signingValue("keyAlias",      "KEYSTORE_KEY_ALIAS")
val releaseKeyPassword  = signingValue("keyPassword",   "KEYSTORE_KEY_PASSWORD")
val hasSigningConfig    = releaseStoreFile != null &&
                          releaseStorePass != null &&
                          releaseKeyAlias  != null &&
                          releaseKeyPassword != null

android {
    namespace = "com.example.driveswipe"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.driveswipe"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            if (hasSigningConfig) {
                storeFile = rootProject.file(requireNotNull(releaseStoreFile))
                storePassword = requireNotNull(releaseStorePass)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                // No signing credentials found — the resulting APK will be unsigned
                // and cannot be installed on a device.  Fail fast so the problem is
                // obvious rather than silently publishing an invalid APK.
                throw GradleException(
                    "Release build requires signing credentials. " +
                    "Provide keystore.properties or set the KEYSTORE_FILE / " +
                    "KEYSTORE_STORE_PASSWORD / KEYSTORE_KEY_ALIAS / KEYSTORE_KEY_PASSWORD " +
                    "environment variables."
                )
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    androidResources {
        noCompress += "task"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-service:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    
    val camerax_version = "1.3.0-rc01"
    implementation("androidx.camera:camera-core:${camerax_version}")
    implementation("androidx.camera:camera-camera2:${camerax_version}")
    implementation("androidx.camera:camera-lifecycle:${camerax_version}")
    implementation("androidx.camera:camera-view:${camerax_version}")

    implementation("com.google.mediapipe:tasks-vision:0.10.14")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

android.applicationVariants.all {
    if (buildType.name == "release") {
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "DriveSwipe-v${versionName}.apk"
        }
    }
}
