plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.kotlin.multiplatform.library")
}

// OHOS spike: 仅在 -PenableOhos 时注册鸿蒙 target（正常 Android 构建不受影响）
// ohosArm64() 是 fork KGP 才有的 API，拆到独立脚本避免官方工具链下编译失败
val ohosEnabled = providers.gradleProperty("enableOhos").orNull == "true"
if (ohosEnabled) {
    apply(from = "ohos-target.gradle")
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
    // OHOS spike: fork 版本同时发布 androidJVM 与 ohos 变体；serialization 官方 1.13.0 尚无 ohos 产物，fork 停在 1.11.0
    val coroutinesVersion = if (ohosEnabled) "1.11.0-0.1.0-07" else "1.11.0"
    val serializationVersion = if (ohosEnabled) "1.11.0-0.1.0-07" else "1.13.0"
    val datetimeVersion = if (ohosEnabled) "0.8.0-0.1.0-07" else "0.8.0"
    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:$datetimeVersion")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
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
