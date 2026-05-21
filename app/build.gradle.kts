plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
}

android {
    namespace = "com.example.gestionturnosapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.gestionturnosapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.3-PREMIUM"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
        dataBinding = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.security.crypto)
    
    // Navigation Component
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // ViewModel and LiveData
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.activity.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    
    // Material Design (Views)
    implementation(libs.material)
    implementation(libs.androidx.swiperefreshlayout)
    
    // Coil for Image Loading
    implementation(libs.coil)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.crashlytics)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
