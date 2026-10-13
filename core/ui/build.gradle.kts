plugins {
    alias(libs.plugins.lifeguide.kmp.compose)
}

// 设计系统：配色、图标、徽章、正文渲染、界面文案。各功能模块共用，不含业务状态。
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.corpus)
        }
    }
}
