package app.lifeguide.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** 带界面的 KMP 模块：在 [KmpLibraryConventionPlugin] 之上加 Compose 编译器、Compose Multiplatform 与常用 UI 依赖 */
class KmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(KmpLibraryConventionPlugin::class.java)
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        pluginManager.apply("org.jetbrains.compose")

        extensions.configure<KotlinMultiplatformExtension> {
            // compose 资源（composeResources/）在 Android 上要走 Android 资源打包
            extensions.configure<KotlinMultiplatformAndroidLibraryExtension> {
                androidResources { enable = true }
            }
            sourceSets.getByName("commonMain").dependencies {
                implementation(libs.lib("compose-runtime"))
                implementation(libs.lib("compose-foundation"))
                implementation(libs.lib("compose-ui"))
                implementation(libs.lib("compose-material3"))
                implementation(libs.lib("compose-components-resources"))
                implementation(libs.lib("androidx-lifecycle-runtime-compose"))
            }
        }
    }
}
