import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":demo"))
    implementation("androidx.activity:activity-compose:1.12.2")
    implementation(compose.components.uiToolingPreview)
    debugImplementation(compose.uiTooling)
}

android {
    namespace = "com.monoxer.mathmlcompose.demo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.monoxer.mathmlcompose.demo"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
