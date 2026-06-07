plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.gptalertlauncher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.gptalertlauncher"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.1"
    }

    signingConfigs {
        getByName("debug") {
            val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
            val keystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            val keyAlias = System.getenv("ANDROID_KEY_ALIAS")
            val keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            if (!keystorePath.isNullOrBlank() && !keystorePassword.isNullOrBlank() && !keyAlias.isNullOrBlank() && !keyPassword.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
