plugins {
    id("com.android.application")
    //id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.techsyllabi.voiceactivatedhomeautomation"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.techsyllabi.voiceactivatedhomeautomation"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.10.1") // For ActivityResultLauncher

    // Add testing dependencies
    testImplementation("junit:junit:4.13.2") // For unit tests
    androidTestImplementation("androidx.test.ext:junit:1.2.1") // For instrumented tests
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1") // For UI tests (optional)
}