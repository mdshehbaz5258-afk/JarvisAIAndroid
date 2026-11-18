plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.jarvisai.assistant"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jarvisai.assistant"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("JARVIS_KEYSTORE_FILE") ?: "release-key.jks")
            storePassword = System.getenv("JARVIS_KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("JARVIS_KEY_ALIAS") ?: ""
            keyPassword = System.getenv("JARVIS_KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation("com.alphacephei:vosk-android:0.3.75")
}
