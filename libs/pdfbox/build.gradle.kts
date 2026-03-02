plugins {
    id("com.android.library")
}

android {
    namespace = "com.tom_roush.pdfbox"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":libs:android-fonts"))
    api("com.tom-roush:pdfbox-android:2.0.27.0")
    api("com.github.jai-imageio:jai-imageio-jpeg2000:1.4.0")
}
