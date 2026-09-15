import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val airshipProps = Properties().apply {
    val local = rootProject.file("../config/airship.local.properties")
    val example = rootProject.file("../config/airship.local.properties.example")
    val file = if (local.exists()) local else example
    file.inputStream().use { load(it) }
}

fun prop(key: String, default: String = ""): String =
    (airshipProps.getProperty(key) ?: default).trim()

android {
    namespace = "com.airship.ctvlab"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.airship.ctvlab"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "AIRSHIP_APP_KEY", "\"${prop("airship.appKey")}\"")
        buildConfigField("String", "AIRSHIP_APP_SECRET", "\"${prop("airship.appSecret")}\"")
        buildConfigField("String", "AIRSHIP_SITE", "\"${prop("airship.site", "eu")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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
        compose = true
        buildConfig = true
    }
}

dependencies {
    val airship = "20.11.0"
    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")

    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.tv:tv-material:1.1.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.media3:media3-exoplayer:1.8.0")
    implementation("androidx.media3:media3-ui:1.8.0")

    implementation("com.urbanairship.android:urbanairship-automation-compose:$airship")
    implementation("com.urbanairship.android:urbanairship-message-center-compose:$airship")
    implementation("com.urbanairship.android:urbanairship-preference-center-compose:$airship")
    implementation("com.urbanairship.android:urbanairship-feature-flag:$airship")
    debugImplementation("com.urbanairship.android:urbanairship-debug:$airship")
}
