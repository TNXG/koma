import java.security.MessageDigest

plugins {
    id("com.android.application")
    kotlin("plugin.compose")
}

// RemoteViews 用 Android 字体资源，沿用 Compose 的同一份 Inter 源文件。
abstract class WidgetFontResources : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val fonts: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val destination = outputDirectory.dir("font").get().asFile.apply { mkdirs() }
        for (font in fonts.files) font.copyTo(destination.resolve(font.name.lowercase()), overwrite = true)
    }
}

val generateWidgetFonts = tasks.register<WidgetFontResources>("generateWidgetFonts") {
    val source = rootProject.layout.projectDirectory.dir("ui/compose/src/commonMain/composeResources/font")
    fonts.from(source.file("Inter400.ttf"), source.file("Inter500.ttf"))
    outputDirectory.set(layout.buildDirectory.dir("generated/widgetFonts"))
}

// 版本号同时用于 APK 文件名，唯一来源是 gradle.properties 的 koma.version。
val komaVersionName: String = providers.gradleProperty("koma.version").orNull
    ?: error("gradle.properties 缺少 koma.version")

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
            // CI / 配好口令的本地环境用发布签名；本地缺口令时回退 debug key，
            // 保证非 CI 环境不配密钥也能出可安装的 release 包（CI 由 workflow 显式注入口令）。
            signingConfig = if (
                releaseSecret("KOMA_KEYSTORE_PASSWORD").isNullOrBlank() ||
                releaseSecret("KOMA_KEY_PASSWORD").isNullOrBlank()
            ) {
                check(System.getenv("CI") != null) {
                    "CI 环境缺少发布签名口令：请设置 KOMA_KEYSTORE_PASSWORD / KOMA_KEY_PASSWORD。"
                }
                signingConfigs.getByName("debug")
            } else {
                signingConfigs.getByName("release")
            }
        }
    }
    buildFeatures { compose = true }
    packaging { jniLibs.useLegacyPackaging = true }
}

androidComponents {
    onVariants { variant ->
        variant.sources.res?.addGeneratedSourceDirectory(generateWidgetFonts, WidgetFontResources::outputDirectory)
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
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")
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


