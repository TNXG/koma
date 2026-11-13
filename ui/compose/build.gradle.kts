plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    androidLibrary {
        namespace = "moe.tnxg.koma.ui"
        compileSdk = 37
        minSdk = 29
        androidResources.enable = true
        withHostTestBuilder {}.configure {}
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
    }
    jvmToolchain(21)
    sourceSets {
        commonMain.dependencies {
            api(project(":core"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation("top.yukonga.miuix.kmp:miuix-blur:0.9.4")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        }
        getByName("androidHostTest").dependencies { implementation(kotlin("test-junit")) }
        androidMain.dependencies { implementation("androidx.activity:activity-compose:1.10.1") }
    }
}

compose.resources { packageOfResClass = "moe.tnxg.koma.resources" }

composeCompiler {
    // gradle ... -PcomposeMetrics -> build/compose-reports shows skippable/stable analysis
    if (providers.gradleProperty("composeMetrics").isPresent) {
        reportsDestination = layout.buildDirectory.dir("compose-reports")
        metricsDestination = layout.buildDirectory.dir("compose-metrics")
    }
}
