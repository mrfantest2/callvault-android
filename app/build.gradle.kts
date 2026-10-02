plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "win.fantest.callvault"
    compileSdk = 36
    defaultConfig {
        applicationId = "win.fantest.callvault"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.3.0-cp03"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}


