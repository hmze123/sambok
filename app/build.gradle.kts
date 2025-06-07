plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("kotlin-parcelize")
}

android {
    namespace = "com.spidroid.starry"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.spidroid.starry"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        }
    }
}

dependencies {
    // Core Library Desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // AndroidX and Material Components
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.preference:preference:1.2.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Lifecycle Components
    val lifecycle_version = "2.9.1"
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle_version")

    // Navigation Component
    val nav_version = "2.9.0"
    implementation("androidx.navigation:navigation-fragment-ktx:$nav_version")
    implementation("androidx.navigation:navigation-ui-ktx:$nav_version")

    // Media3 (ExoPlayer)
    val media3_version = "1.7.1"
    implementation("androidx.media3:media3-exoplayer:$media3_version")
    implementation("androidx.media3:media3-ui:$media3_version")
    implementation("androidx.media3:media3-session:$media3_version")
    implementation("androidx.media3:media3-exoplayer-dash:$media3_version")
    implementation("androidx.media3:media3-exoplayer-hls:$media3_version")
    implementation("androidx.media3:media3-common:$media3_version")

    // UI Libraries
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    // Link Preview
    implementation("org.jsoup:jsoup:1.20.1")

    // Firebase - استخدام الـ BOM لإدارة الإصدارات
    implementation(platform("com.google.firebase:firebase-bom:33.15.0"))
    implementation("com.google.firebase:firebase-analytics-ktx") // لا حاجة لتحديد الإصدار
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    // App Check dependencies
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-appcheck-debug")
    implementation("com.firebaseui:firebase-ui-storage:9.0.0")
    implementation("com.google.mlkit:translate:17.0.2")
    // Kotlin stdlib and Coroutines
    // الإصدار يتم تحديده الآن من خلال plugin في الملف الرئيسي
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Google Play Services
    implementation("com.google.android.gms:play-services-base:18.7.0")

    // Retrofit & Gson
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // CameraX
    val camerax_version = "1.4.2"
    implementation("androidx.camera:camera-core:$camerax_version")
    implementation("androidx.camera:camera-camera2:$camerax_version")
    implementation("androidx.camera:camera-lifecycle:$camerax_version")
    implementation("androidx.camera:camera-video:$camerax_version")
    implementation("androidx.camera:camera-view:$camerax_version")
    implementation("androidx.camera:camera-extensions:$camerax_version")

    // PrettyTime for relative time formatting
    implementation("org.ocpsoft.prettytime:prettytime:5.0.9.Final")

}