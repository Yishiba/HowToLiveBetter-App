package app.lifeguide.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * 功能模块（feature 目录下）：一个功能一个模块，自带界面、ViewModel、导航入口和 Koin 模块。
 * 只能依赖 core 模块，功能模块之间互不依赖，跳转由 :shared 的 NavHost 串起来。
 */
class KmpFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(KmpComposeConventionPlugin::class.java)
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.getByName("commonMain").dependencies {
                implementation(project(":core:corpus"))
                implementation(project(":core:data"))
                implementation(project(":core:ui"))
                implementation(libs.lib("androidx-lifecycle-viewmodel-compose"))
                implementation(libs.lib("androidx-navigation-compose"))
                implementation(libs.lib("kotlinx-serialization-json"))
                implementation(libs.lib("koin-core"))
                implementation(libs.lib("koin-compose"))
                implementation(libs.lib("koin-compose-viewmodel"))
            }
        }
    }
}
