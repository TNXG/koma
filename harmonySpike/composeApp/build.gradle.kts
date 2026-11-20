import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.net.URI

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val repoRoot = rootProject.projectDir.parentFile

val prepareOcrModel by tasks.registering(Exec::class) {
    workingDir(repoRoot)
    commandLine("bash", File(repoRoot, "tools/fetch-ocr-model.sh").absolutePath)
}

val miuixBase = layout.buildDirectory.dir("miuix-0.9.4")
val miuixSrc = miuixBase.map { it.dir("src") }

val downloadMiuixSources by tasks.registering {
    val blurJar = miuixBase.map { it.file("miuix-blur-0.9.4-sources.jar") }
    val shaderJar = miuixBase.map { it.file("miuix-shader-0.9.4-sources.jar") }
    outputs.files(blurJar, shaderJar)
    doLast {
        val dir = miuixBase.get().asFile.apply { mkdirs() }
        for ((artifact, jarName) in listOf(
            "miuix-blur" to "miuix-blur-0.9.4-sources.jar",
            "miuix-shader" to "miuix-shader-0.9.4-sources.jar",
        )) {
            val target = File(dir, jarName)
            if (target.exists() && target.length() > 0) continue
            URI("https://repo1.maven.org/maven2/top/yukonga/miuix/kmp/$artifact/0.9.4/$jarName").toURL()
                .openStream().use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
        }
    }
}

val extractMiuixSources by tasks.registering(Sync::class) {
    dependsOn(downloadMiuixSources)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(zipTree(miuixBase.map { it.file("miuix-blur-0.9.4-sources.jar") }))
    from(zipTree(miuixBase.map { it.file("miuix-shader-0.9.4-sources.jar") }))
    into(miuixSrc)
    exclude("META-INF/**")
    exclude("skikoMain/top/yukonga/miuix/kmp/blur/internal/RenderEffectCompat.skiko.kt")
    exclude("skikoMain/top/yukonga/miuix/kmp/shader/RuntimeShader.skiko.kt")
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    ohosArm64 {
        compilations.getByName("main").cinterops.create("assetStore") {
            defFile(project.file("src/nativeInterop/cinterop/assetStore.def"))
            val sdk = System.getenv("DEVECO_SDK_HOME")
                ?: File(repoRoot, ".tools/ohos/command-line-tools/sdk").absolutePath
            includeDirs("$sdk/default/openharmony/native/sysroot/usr/include")
        }
        binaries.sharedLib {
            baseName = "kn"
            export(libs.compose.multiplatform.export)
            linkerOpts("-lz")
            linkerOpts("-lasset_ndk.z")
            linkerOpts(
                "-lnative_drawing",
                "-limage_source",
                "-lpixelmap",
                "-lpixelmap_ndk.z",
                "-lnative_window",
                "-lace_napi.z",
                "-lhilog_ndk.z",
                "-lhitrace_ndk.z",
                "-luv",
                "-lunwind",
                "-licu",
            )
        }
        compilations.all {
            compilerOptions.options.freeCompilerArgs.add("-Xcontext-parameters")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.atomicFu)
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.1-1.1.0-02")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1-1.1.0-02")
        }

        val ohosMain = sourceSets.create("ohosMain").apply {
            dependsOn(commonMain.get())
        }
        ohosMain.dependencies {
            api(libs.compose.multiplatform.export)
            implementation("io.ktor:ktor-client-core:3.3.3-1.1.0")
            implementation("io.ktor:ktor-client-curl:3.3.3-1.1.0")
        }
        val ohosArm64Main by getting {
            dependsOn(ohosMain)
        }
    }

    sourceSets.all {
        languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        languageSettings.optIn("kotlin.experimental.ExperimentalNativeApi")
        languageSettings.optIn("kotlin.time.ExperimentalTime")
        languageSettings.optIn("androidx.compose.ui.ExperimentalComposeUiApi")
    }

    sourceSets["commonMain"].apply {
        kotlin.srcDir("${repoRoot.absolutePath}/core/src/commonMain/kotlin")
        kotlin.srcDir("${repoRoot.absolutePath}/ui/compose/src/commonMain/kotlin")
        kotlin.srcDir(miuixSrc.get().dir("commonMain"))
        resources.srcDir("${repoRoot.absolutePath}/ui/compose/src/commonMain/composeResources")
    }
    sourceSets["ohosArm64Main"].apply {
        kotlin.srcDir(miuixSrc.get().dir("skikoMain"))
    }
    sourceSets["ohosMain"].kotlin.srcDir("src/ohosMain/kotlin")
}

compose.resources {
    packageOfResClass = "moe.tnxg.koma.resources"
}

android {
    namespace = "com.example.cmp_hello"
    compileSdk = 36

    defaultConfig {
        applicationId = "moe.tnxg.koma"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

afterEvaluate {
    tasks.matching { it.name == "compileKotlinOhosArm64" }.configureEach {
        dependsOn(extractMiuixSources)
    }
}

// OHOS 构建产物发布到壳工程（harmonyApp）
arrayOf("debug" to "Debug").forEach { (type, cap) ->
    tasks.register<Copy>("publish${cap}BinariesToHarmonyApp") {
        group = "harmony"
        dependsOn("link${cap}SharedOhosArm64")
        dependsOn(prepareOcrModel)
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        into(rootProject.file("harmonyApp"))
        from("build/bin/ohosArm64/${type}Shared/libkn_api.h") {
            into("entry/src/main/cpp/include/arm64-v8a/")
        }
        from("build/bin/ohosArm64/${type}Shared/libkn.so") {
            into("entry/libs/arm64-v8a/")
        }
        from("${repoRoot.absolutePath}/ui/compose/src/commonMain/composeResources") {
            into("entry/src/main/resources/rawfile/composeResources/moe.tnxg.koma.resources/")
        }
        from("${repoRoot.absolutePath}/core/src/androidMain/assets/ocr") {
            into("entry/src/main/resources/rawfile/ocr/")
        }
    }
}
