import java.security.MessageDigest

plugins {
    id("com.android.application")
    kotlin("plugin.compose")
}

android {
    namespace = "moe.tnxg.koma"
    compileSdk = 35
    defaultConfig {
        applicationId = "moe.tnxg.koma"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        release {
            // 本地构建与 adb 安装沿用调试签名；正式分发时替换为发布密钥。
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    buildFeatures { compose = true }
    packaging { jniLibs.useLegacyPackaging = true }
}

kotlin { jvmToolchain(21) }

dependencies {
    implementation(project(":core"))
    implementation(project(":ui:compose"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}

tasks.register("verifyOcrModel") {
    doLast {
        val model = rootProject.file("core/src/androidMain/assets/ocr/captcha.onnx")
        check(model.isFile && model.length() == 13_606_051L) { "Missing OCR model: run mise run setup" }
        val hash = MessageDigest.getInstance("SHA-256").digest(model.readBytes())
            .joinToString("") { "%02x".format(it) }
        check(hash == "b8f2ad9cbc1f2e3922a6cb9459e30824e7e2467f3fb4fd61420640e34ea0bf68") {
            "OCR model checksum mismatch: run mise run setup"
        }
    }
}
tasks.named("preBuild") { dependsOn("verifyOcrModel") }
