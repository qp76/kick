import javax.imageio.ImageIO
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "1.9.20-1.0.14"
    id("dagger.hilt.android.plugin")
    id("kotlin-parcelize")
}

android {
    namespace = "com.kickstra"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kickstra"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
    lint {
        baseline = file("lint-baseline.xml")
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

// Optimize launcher icon at build time: resize to <=512px and convert to WebP if cwebp is available.
val optimizeLauncherIcon by tasks.registering {
    val resDir = layout.projectDirectory.dir("src/main/res/drawable-nodpi").asFile
    val pngFile = resDir.resolve("kickstra_logo.png")
    val webpFile = resDir.resolve("kickstra_logo.webp")

    // Don't validate the PNG at configuration time; it may be removed after WebP conversion.
    // We'll handle existence checks at execution time.
    outputs.file(webpFile)
    // Consider up-to-date if WebP already exists and PNG removed,
    // or PNG exists and is already <=512px
    outputs.upToDateWhen {
    if (!pngFile.exists() && webpFile.exists()) return@upToDateWhen true
    if (!pngFile.exists()) return@upToDateWhen true // PNG yoksa, dönüşüm zaten yapılmıştır
        try {
            System.setProperty("java.awt.headless", "true")
            val img = ImageIO.read(pngFile)
            val maxDim = maxOf(img.width, img.height)
            maxDim <= 512
        } catch (e: Exception) {
            false
        }
    }

    doLast {
        if (!pngFile.exists()) return@doLast

    System.setProperty("java.awt.headless", "true")
    val img = ImageIO.read(pngFile)
        if (img == null) {
            logger.warn("Cannot read launcher icon PNG: ${'$'}pngFile")
            return@doLast
        }
        val maxDim = maxOf(img.width, img.height)
        val targetMax = 512
        if (maxDim > targetMax) {
            val scale = targetMax.toDouble() / maxDim.toDouble()
            val newW = (img.width * scale).toInt().coerceAtLeast(1)
            val newH = (img.height * scale).toInt().coerceAtLeast(1)
            val out = BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB)
            val g2 = out.createGraphics() as java.awt.Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.drawImage(img, 0, 0, newW, newH, null)
            g2.dispose()
            ImageIO.write(out, "png", pngFile)
            logger.lifecycle("Resized launcher icon to ${'$'}newW x ${'$'}newH")
        } else {
            logger.lifecycle("Launcher icon already <= ${'$'}targetMax px; skipping resize")
        }

        // Try to convert to WebP if 'cwebp' exists
        try {
            val check = ByteArrayOutputStream()
            exec {
                commandLine("bash", "-lc", "command -v cwebp >/dev/null 2>&1 && echo yes || echo no")
                standardOutput = check
                isIgnoreExitValue = true
            }
            val hasCwebp = check.toString().trim().endsWith("yes")
            if (hasCwebp) {
                exec {
                    // Use high-quality with alpha preserved; tweak q if needed
                    commandLine(
                        "cwebp", "-q", "85", "-m", "6", "-mt", "-alpha_q", "100",
                        pngFile.absolutePath, "-o", webpFile.absolutePath
                    )
                }
                if (webpFile.exists()) {
                    // Remove the PNG so the resource name resolves to the WebP
                    if (pngFile.delete()) {
                        logger.lifecycle("Converted launcher icon to WebP and removed PNG: ${'$'}webpFile")
                    }
                }
            } else {
                logger.lifecycle("'cwebp' not found; keeping optimized PNG. To enable WebP, install libwebp tools.")
            }
        } catch (e: Exception) {
            logger.warn("WebP conversion step failed: ${'$'}e")
        }
    }
}

// Ensure optimization runs before building resources
tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn(optimizeLauncherIcon)
}
