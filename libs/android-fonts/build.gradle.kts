plugins {
    id("com.android.library")
}

android {
    namespace = "com.tom_roush.android_fonts"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
