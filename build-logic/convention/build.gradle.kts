plugins {
    `kotlin-dsl`
}

group = "app.lifeguide.buildlogic"

// compileOnly：插件本体由根工程 build.gradle.kts 以 apply false 放上 classpath，这里只要能编译
dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    compileOnly(libs.compose.multiplatform.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = libs.plugins.lifeguide.kmp.library.get().pluginId
            implementationClass = "app.lifeguide.buildlogic.KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = libs.plugins.lifeguide.kmp.compose.get().pluginId
            implementationClass = "app.lifeguide.buildlogic.KmpComposeConventionPlugin"
        }
        register("kmpFeature") {
            id = libs.plugins.lifeguide.kmp.feature.get().pluginId
            implementationClass = "app.lifeguide.buildlogic.KmpFeatureConventionPlugin"
        }
    }
}
