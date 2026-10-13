import app.lifeguide.buildlogic.SyncCorpusTask

plugins {
    alias(libs.plugins.lifeguide.kmp.compose)
}

// App 模块：把各功能模块装进导航、组装 Koin、提供内置正文快照；iOS 的 Shared.framework 和桌面预览也从这里出。
kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.corpus)
            implementation(projects.core.data)
            implementation(projects.core.ui)
            implementation(projects.feature.checklist)
            implementation(projects.feature.find)
            implementation(projects.feature.entry)
            implementation(projects.feature.settings)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.navigation.compose)
            // initKoin 的参数和返回值是 Koin 类型，平台入口要看得到
            api(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.okio)
        }
        commonTest.dependencies {
            implementation(libs.koin.test)
            implementation(libs.kotlinx.serialization.json)
        }
        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
            implementation(compose.desktop.currentOs)
        }
    }
}

// 内置正文快照，见 build-logic 里的 SyncCorpusTask
val syncCorpus = tasks.register<SyncCorpusTask>("syncCorpus") {
    upstreamDir.set(rootProject.layout.projectDirectory.dir("upstream"))
    outputDir.set(layout.buildDirectory.dir("corpus-resources"))
}

compose.resources {
    publicResClass = false
    packageOfResClass = "app.lifeguide.resources"
    customDirectory(
        sourceSetName = "commonMain",
        directoryProvider = syncCorpus.flatMap { it.outputDir },
    )
}

compose.desktop {
    application {
        mainClass = "app.lifeguide.MainKt"
    }
}
