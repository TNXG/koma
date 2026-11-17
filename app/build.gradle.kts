import java.security.MessageDigest

plugins {
    id("com.android.application")
    kotlin("plugin.compose")
}

// 版本号同时用于 APK 文件名，保持单一来源。
val komaVersionName = "0.0.2"

// 发布签名口令来源：环境变量优先，其次未跟踪的 gradle.properties。
val releaseSecret: (String) -> String? = { name ->
    System.getenv(name) ?: project.providers.gradleProperty(name).orNull
}

android {
    namespace = "moe.tnxg.koma"
    compileSdk = 37
    defaultConfig {
        applicationId = "moe.tnxg.koma"
        minSdk = 29
        targetSdk = 37
        versionCode = 3
        versionName = komaVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // onnxruntime 的原生库按 ABI 切成独立 APK，另出一个含全部 ABI 的 universal 包。
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
            isUniversalApk = true
        }
    }
    signingConfigs {
        create("release") {
            // 口令只从环境变量或未跟踪的 gradle.properties 读取，绝不写入版本库。
            storeFile = file(
                System.getenv("KOMA_KEYSTORE") ?: providers.gradleProperty("koma.keystore").orNull
                    ?: "${System.getProperty("user.home")}/android.p12",
            )
            storePassword = releaseSecret("KOMA_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KOMA_KEY_ALIAS") ?: providers.gradleProperty("koma.keyAlias").orNull ?: "app_sign"
            keyPassword = releaseSecret("KOMA_KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
    buildFeatures { compose = true }
    packaging { jniLibs.useLegacyPackaging = true }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val abi = output.filters.firstOrNull {
                it.filterType == com.android.build.api.variant.FilterConfiguration.FilterType.ABI
            }?.identifier ?: "universal"
            output.outputFileName.set("koma-$komaVersionName-$abi.apk")
        }
    }
}

kotlin { jvmToolchain(21) }

dependencies {
    implementation(project(":core"))
    implementation(project(":ui:compose"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
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

// 口令不进版本库，所以发布包必须在构建时显式具备，缺失即失败而不是产出无签名包。
val verifyReleaseSigning = tasks.register("verifyReleaseSigning") {
    doLast {
        val missing = listOf("KOMA_KEYSTORE_PASSWORD", "KOMA_KEY_PASSWORD")
            .filter { releaseSecret(it).isNullOrBlank() }
        check(missing.isEmpty()) {
            "缺少发布签名口令：${missing.joinToString()}；请设置环境变量，或在 ~/.gradle/gradle.properties 中配置。"
        }
    }
}
listOf("assembleRelease", "bundleRelease").forEach { name ->
    tasks.matching { it.name == name }.configureEach { dependsOn(verifyReleaseSigning) }
}
