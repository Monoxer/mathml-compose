import app.cash.licensee.UnusedAction.IGNORE
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("app.cash.licensee") version "1.14.1"
    id("com.android.library") version "8.11.2"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("org.jetbrains.kotlin.multiplatform") version "2.3.20"
    id("org.jetbrains.compose") version "1.9.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
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

dependencies {
    ktlintRuleset("io.nlopez.compose.rules:ktlint:0.5.3")
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/detekt.yml"))
    source.setFrom(files("src/"))
    parallel = true
    autoCorrect = true
}

ktlint {
    version.set("1.8.0")
    android.set(false)
    ignoreFailures.set(false)
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
}

licensee {
    // The shared allowlist is evaluated independently for each KMP target.
    unusedAction(IGNORE)
    allow("0BSD")
    allow("Apache-2.0")
    allow("BSD-2-Clause")
    allow("BSD-3-Clause")
    allow("CC0-1.0")
    allow("ISC")
    allow("MIT")
    allow("MIT-0")
    allow("Unlicense")
}

// Configure sources after the plugin so generated files are never linted.
afterEvaluate {
    tasks.withType<org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask>().configureEach {
        setSource(fileTree(projectDir) { exclude("**/build/**", "**/generated/**") })
    }
}
