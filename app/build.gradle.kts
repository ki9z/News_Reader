import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.serialization)
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    alias(libs.plugins.google.services) apply false
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { input -> this.load(input) }
    }
}

fun readSecret(key: String): String {
    val fromGradle = (project.findProperty(key) as? String)?.trim().orEmpty()
    if (fromGradle.isNotBlank()) return fromGradle
    return localProps.getProperty(key)?.trim().orEmpty()
}

val newsApiKey = readSecret("NEWS_API_KEY")
val newsBaseUrl = readSecret("NEWS_BASE_URL").ifBlank { "https://newsapi.org/" }
val newsApiDailyLimit = readSecret("NEWS_API_DAILY_LIMIT").toIntOrNull()?.coerceAtLeast(1) ?: 90
val backendAppToken = readSecret("BACKEND_APP_TOKEN")
val firebaseWebClientId = readSecret("FIREBASE_WEB_CLIENT_ID")
val firebaseAuthEnabled = firebaseWebClientId.isNotBlank()

android {
    namespace = "com"
    compileSdk = 35

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/src")
        }
        getByName("androidTest") {
            java.srcDirs("src/androidTest/src")
        }
        getByName("test") {
            java.srcDirs("src/test/java")
        }
    }

    defaultConfig {
        applicationId = "com.ptit.newsreaderandroid"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "NEWS_API_KEY", "\"$newsApiKey\"")
        buildConfigField("String", "NEWS_BASE_URL", "\"$newsBaseUrl\"")
        buildConfigField("int", "NEWS_API_DAILY_LIMIT", newsApiDailyLimit.toString())
        buildConfigField("String", "BACKEND_APP_TOKEN", "\"$backendAppToken\"")
        buildConfigField("String", "FIREBASE_WEB_CLIENT_ID", "\"$firebaseWebClientId\"")
        buildConfigField("boolean", "FIREBASE_AUTH_ENABLED", firebaseAuthEnabled.toString())

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }
    lint {
        baseline = file("lint-baseline.xml")
    }
    buildToolsVersion = "36.1.0"
}

dependencies {
    implementation("androidx.security:security-crypto-ktx:1.1.0-alpha06")
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)


    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Lifecycle / ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Retrofit & Gson
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.gson)
    implementation(libs.okhttp.logging.interceptor)
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("com.google.firebase:firebase-messaging")

    // Glide
    implementation(libs.glide)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Compose & admin (copied from requirement)
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("io.coil-kt:coil-compose:2.6.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

