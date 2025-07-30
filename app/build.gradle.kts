plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
    id("kotlin-parcelize")
}

android {
    namespace = "com.informatlux.test"
    compileSdk = 35

    aaptOptions {
        noCompress ; "tflite"
    }

    defaultConfig {
        applicationId = "com.informatlux.test"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "HF_API_KEY", "\"${properties["HF_API_KEY"]}\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // Core AndroidX and Material
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.activity)                // androidx.activity:activity-ktx
    implementation(libs.androidx.material3.android)       // Material3 components if used
    implementation("com.google.android.material:material:1.14.0-alpha03") // stable Material Components
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("com.google.android.gms:play-services-maps:19.2.0")
    implementation("com.google.ai.edge.aicore:aicore:0.0.1-exp02")
    implementation("com.google.guava:guava:31.1-jre")

    // Lifecycle libraries
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.2")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // ML Kit (exclude litert libraries to avoid duplicate TensorFlow Lite API classes)
    implementation("com.google.mlkit:genai-image-description:1.0.0-beta1") {
        exclude(group = "com.google.ai.edge.litert", module = "litert-api")
        exclude(group = "com.google.ai.edge.litert", module = "litert-support-api")
    }
    implementation("com.google.mlkit:image-labeling:17.0.9") {
        exclude(group = "com.google.ai.edge.litert", module = "litert-api")
        exclude(group = "com.google.ai.edge.litert", module = "litert-support-api")
    }
    implementation("com.google.mlkit:object-detection:17.0.2") {
        exclude(group = "com.google.ai.edge.litert", module = "litert-api")
        exclude(group = "com.google.ai.edge.litert", module = "litert-support-api")
    }
    implementation("com.google.mlkit:smart-reply:17.0.4") {
        exclude(group = "com.google.ai.edge.litert", module = "litert-api")
        exclude(group = "com.google.ai.edge.litert", module = "litert-support-api")
    }

    // TensorFlow Lite core and task libraries, keep these only once and consistent versions
    implementation("org.tensorflow:tensorflow-lite:2.17.0") {
        exclude(group = "com.google.ai.edge.litert", module = "litert-api")
        exclude(group = "com.google.ai.edge.litert", module = "litert-support-api")
    }
    implementation("org.tensorflow:tensorflow-lite-support:0.5.0"){
        exclude(group = "com.google.ai.edge.litert", module = "litert-api")
        exclude(group = "com.google.ai.edge.litert", module = "litert-support-api")
    }
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.4"){
        exclude(group = "com.google.ai.edge.litert", module = "litert-api")
        exclude(group = "com.google.ai.edge.litert", module = "litert-support-api")
    }
    implementation("org.tensorflow:tensorflow-lite-gpu-delegate-plugin:0.4.4"){
        exclude(group = "com.google.ai.edge.litert", module = "litert-api")
        exclude(group = "com.google.ai.edge.litert", module = "litert-support-api")
    }

    // Google Play Services
    implementation("com.google.android.gms:play-services-auth:21.4.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Firebase BOM and components
    implementation(platform("com.google.firebase:firebase-bom:34.0.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore:26.0.0")
    implementation("com.google.firebase:firebase-storage:22.0.0")
    implementation("com.google.firebase:firebase-analytics")

    // Identity and Credentials
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")

    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Networking – OkHttp, Gson, Moshi, Retrofit
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("com.squareup.moshi:moshi:1.15.2")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.retrofit2:converter-scalars:3.0.0")

    // OSMDroid
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
