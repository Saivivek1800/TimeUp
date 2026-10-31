import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Release signing. The key and its passwords live OUTSIDE the project (default: ~/reclaim-keys/keystore.properties,
// or the file named by the RECLAIM_SIGNING environment variable), so they can never be committed by accident.
// Without that file the release build is simply left unsigned.
val signingFile = file(System.getenv("RECLAIM_SIGNING") ?: "${System.getProperty("user.home")}/reclaim-keys/keystore.properties")
val signingProps = Properties().apply { if (signingFile.exists()) signingFile.inputStream().use { load(it) } }

android {
    namespace = "com.vivek.reclaim"
    // Supported: Android 7.0 (API 24) up to Android 17 (API 37).
    //  - minSdk 24: the oldest phone it installs on (Android 7.0). This is the lowest the current libraries allow:
    //    Navigation needs API 24, and Hilt's navigation helper needs 23. Going lower means older, riskier libraries.
    //  - targetSdk 36: Android 16, which Google Play requires for new apps and updates since 31 Aug 2026.
    //  - compileSdk 37: the toolkit used to build; the latest androidx libraries need a recent one.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.vivek.reclaim"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "0.1.1-beta"
    }

    signingConfigs {
        if (signingFile.exists()) {
            create("release") {
                storeFile = file(signingProps.getProperty("storeFile"))
                storePassword = signingProps.getProperty("storePassword")
                keyAlias = signingProps.getProperty("keyAlias")
                keyPassword = signingProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Shrink and obfuscate the code and drop unused resources: a smaller, harder-to-copy app.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // Lets Android 7.x use java.time (dates, times, time zones), which Android itself only has from 8.0.
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.browser)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.kotlinx.coroutines.android)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
