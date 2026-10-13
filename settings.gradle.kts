rootProject.name = "LifeGuide"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
    }
}

// App 入口
include(":androidApp")
include(":shared")
// 功能：一个功能一个模块，互不依赖
include(":feature:browse")
include(":feature:entry")
include(":feature:settings")
// 基础层：corpus（纯领域逻辑）← data（数据来源）、ui（设计系统）
include(":core:corpus")
include(":core:data")
include(":core:ui")
