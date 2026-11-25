import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
}
val bitlyAccessToken = providers.gradleProperty("BITLY_ACCESS_TOKEN")
    .orElse(providers.environmentVariable("BITLY_ACCESS_TOKEN"))
    .orElse(localProperties.getProperty("BITLY_ACCESS_TOKEN").orEmpty())
    .get()
val escapedBitlyAccessToken = bitlyAccessToken
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

android {
    namespace = "com.example.urlshortener"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.urlshortener"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "BITLY_ACCESS_TOKEN", "\"$escapedBitlyAccessToken\"")
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(
            org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        )
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
