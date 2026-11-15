plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    androidLibrary {
        namespace = "moe.tnxg.koma.core"
        compileSdk = 37
        minSdk = 29
        androidResources.enable = true
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder { sourceSetTreeName = "test" }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
    }
    jvmToolchain(21)
    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.13.0")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
        }
        androidMain.dependencies {
            implementation("com.squareup.okhttp3:okhttp:5.5.1")
            implementation("org.jsoup:jsoup:1.24.1")
            implementation("androidx.activity:activity-ktx:1.13.0")
            implementation("androidx.core:core-ktx:1.19.1")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
            implementation("com.microsoft.onnxruntime:onnxruntime-android:1.32.0")
        }
        getByName("androidHostTest").dependencies {
            implementation(kotlin("test-junit"))
            implementation("com.squareup.okhttp3:mockwebserver:5.5.1")
        }
        getByName("androidDeviceTest").dependencies {
            implementation("androidx.test.ext:junit:1.3.0")
            implementation("androidx.test:core:1.7.0")
            implementation("androidx.test:runner:1.7.0")
            implementation("com.squareup.okhttp3:mockwebserver:5.5.1")
        }
    }
}
