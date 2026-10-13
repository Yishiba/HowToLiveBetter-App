package app.lifeguide.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * 所有 KMP 模块的公共部分：目标平台（Android、iOS 真机与模拟器、JVM）、SDK 级别、JVM 字节码版本、测试依赖。
 * JVM 目标只用于跑单元测试和桌面端快速预览，不发布。
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")

        extensions.configure<KotlinMultiplatformExtension> {
            extensions.configure<KotlinMultiplatformAndroidLibraryExtension> {
                namespace = lifeguideNamespace
                compileSdk = libs.int("android-compileSdk")
                minSdk = libs.int("android-minSdk")
            }
            targets.withType(KotlinMultiplatformAndroidLibraryTarget::class.java).configureEach {
                compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
            }
            iosArm64()
            iosSimulatorArm64()
            jvm { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

            sourceSets.getByName("commonTest").dependencies {
                implementation(libs.lib("kotlin-test"))
                implementation(libs.lib("kotlinx-coroutines-test"))
            }
        }
    }
}
