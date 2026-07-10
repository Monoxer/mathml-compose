import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("com.android.library") version "8.11.2"
    id("org.jetbrains.kotlin.multiplatform") version "2.3.20"
    id("org.jetbrains.compose") version "1.9.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20"
}

group = "com.monoxer.mathmlcompose"
version = "0.1.0"

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "MathMLCompose"
            isStatic = true
        }
    }

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.uiToolingPreview)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            // Needed so createFontFamilyResolver() can initialize Skia's FontCollection when
            // tests construct a TextMeasurer directly (outside of a Compose UI test harness).
            implementation(compose.desktop.currentOs)
        }
    }
}

android {
    namespace = "com.monoxer.mathmlcompose"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
