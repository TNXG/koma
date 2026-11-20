plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("com.android.kotlin.multiplatform.library")
}

// 版本号唯一来源是 gradle.properties 的 koma.version；这里生成 commonMain 常量供关于页读取。
val komaVersion: String = providers.gradleProperty("koma.version").orNull
    ?: error("gradle.properties 缺少 koma.version")

val generateAppVersion = tasks.register("generateAppVersion") {
    val version = komaVersion
    val outputDir = layout.buildDirectory.dir("generated/appVersion/kotlin")
    inputs.property("version", version)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("moe/tnxg/koma/version/AppVersion.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |// 由 :ui:compose 的 generateAppVersion 生成，勿手改。
            |// 来源：gradle.properties 的 koma.version。
            |package moe.tnxg.koma.version
            |
            |const val APP_VERSION: String = "$version"
            |""".trimMargin() + "\n",
        )
    }
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
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.13.0")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")
        }
        commonMain { kotlin.srcDir(generateAppVersion) }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
        }
        getByName("androidHostTest").dependencies { implementation(kotlin("test-junit")) }
        androidMain.dependencies { implementation("androidx.activity:activity-compose:1.13.0") }
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
