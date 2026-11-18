pluginManagement {
    // pluginManagement 是先行编译的独立作用域，属性必须在此处读取
    val kotlinOhosVersion = providers.gradleProperty("kotlinOhosVersion").orNull
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // 鸿蒙定制 Kotlin/KGP 仓库（仅提供官方没有的 fork 版本，官方版本仍由前面的仓库解析）
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")
    }
    if (kotlinOhosVersion != null) {
        resolutionStrategy {
            eachPlugin {
                if (requested.id.id.startsWith("org.jetbrains.kotlin.")) {
                    useVersion(kotlinOhosVersion)
                }
            }
        }
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // kotlinx-* 鸿蒙变体（ohosArm64/ohosX64）只发布在这里
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")
    }
}
rootProject.name = "Koma"
include(":app", ":core", ":ui:compose")
