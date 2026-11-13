plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    androidLibrary {
        namespace = "moe.tnxg.koma.core"
        compileSdk = 35
        minSdk = 26
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
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        }
        androidMain.dependencies {
            implementation("com.squareup.okhttp3:okhttp:4.12.0")
            implementation("org.jsoup:jsoup:1.19.1")
            implementation("androidx.activity:activity-ktx:1.10.1")
            implementation("androidx.core:core-ktx:1.16.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
            implementation("com.microsoft.onnxruntime:onnxruntime-android:1.21.0")
        }
        getByName("androidHostTest").dependencies {
            implementation(kotlin("test-junit"))
            implementation("com.squareup.okhttp3:mockwebserver:4.12.0")
        }
        getByName("androidDeviceTest").dependencies {
            implementation("androidx.test.ext:junit:1.2.1")
            implementation("androidx.test:core:1.6.1")
            implementation("androidx.test:runner:1.6.2")
            implementation("com.squareup.okhttp3:mockwebserver:4.12.0")
        }
    }
}
