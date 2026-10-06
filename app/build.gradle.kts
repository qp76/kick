
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "1.9.20-1.0.14"
    id("dagger.hilt.android.plugin")
    id("kotlin-parcelize")
}

// Smaller/faster APK: build with  -PENABLE_R8=true  (or env ENABLE_R8=true). Off by default for safety.
val enableR8: Boolean = (System.getenv("ENABLE_R8") ?: (project.findProperty("ENABLE_R8") as String?) ?: "false").toBoolean()

android {
    namespace = "com.kicklite"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kicklite"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        // Official KICK API configuration. Keep secrets out of source control.
        buildConfigField("String", "KICK_API_BASE_URL", "\"${System.getenv("KICK_API_BASE_URL") ?: "https://api.kick.com/"}\"")
        buildConfigField("String", "KICK_API_TOKEN", "\"${System.getenv("KICK_API_TOKEN") ?: ""}\"")
        buildConfigField("String", "KICK_OAUTH_GATEWAY_URL", "\"${System.getenv("KICK_OAUTH_GATEWAY_URL") ?: ""}\"")
        buildConfigField("String", "KICK_CLIENT_ID", "\"${System.getenv("KICK_CLIENT_ID") ?: ""}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            // Filled in by Codemagic (see codemagic.yaml). Falls back to debug signing locally.
            val ksPath = System.getenv("KEYSTORE_PATH")
            if (ksPath != null && file(ksPath).exists()) {
                storeFile = file(ksPath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = if (System.getenv("KEYSTORE_PATH") != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            isMinifyEnabled = enableR8
            isShrinkResources = enableR8
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        viewBinding = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    
    // ExoPlayer
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.2.0")
    implementation("androidx.media3:media3-datasource:1.2.0")
    implementation("androidx.media3:media3-datasource-okhttp:1.2.0")
    
    // Image loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("io.coil-kt:coil-gif:2.5.0")
    implementation("io.coil-kt:coil-svg:2.5.0")
    
    // SwipeRefresh for pull-to-refresh
    implementation("androidx.compose.material:material:1.5.4")
    
    // Picture in Picture
    implementation("androidx.window:window:1.2.0")
    
    // MediaSessionCompat + MediaStyle notifications
    implementation("androidx.media:media:1.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
