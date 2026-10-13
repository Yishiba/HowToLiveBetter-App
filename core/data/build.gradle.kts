plugins {
    alias(libs.plugins.lifeguide.kmp.library)
    alias(libs.plugins.kotlinSerialization)
}

// 数据层：选用哪份正文、联网更新、本机偏好。不依赖 Compose。
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.corpus)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.okio)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.koin.core)
            implementation(libs.kermit)
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.okio.fakefilesystem)
        }
        androidMain.dependencies { implementation(libs.ktor.client.okhttp) }
        iosMain.dependencies { implementation(libs.ktor.client.darwin) }
        jvmMain.dependencies { implementation(libs.ktor.client.cio) }
    }
}

// 测试拿 upstream/ 的真实正文当「远端」和「内置」
tasks.named<Test>("jvmTest") {
    val upstream = rootProject.layout.projectDirectory.dir("upstream")
    inputs.file(upstream.file("README.md"))
    inputs.dir(upstream.dir("book"))
    val up = upstream.asFile.absolutePath
    jvmArgumentProviders += CommandLineArgumentProvider { listOf("-Dupstream=$up") }
}
