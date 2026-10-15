plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

// 版本号由发版流程传入：./gradlew assembleRelease -PversionName=1.2.3。
// versionCode 由它推出（主*10000 + 次*100 + 修订），保证每次发版单调递增，不用手工维护两个数。
val appVersionName: String = providers.gradleProperty("versionName").getOrElse("0.1.0")
val appVersionCode: Int = run {
    val m = Regex("""^(\d+)\.(\d+)\.(\d+)$""").matchEntire(appVersionName)
        ?: error("versionName 要写成 主.次.修订，比如 1.2.3，现在是「$appVersionName」")
    val (major, minor, patch) = m.destructured
    require(minor.toInt() < 100 && patch.toInt() < 100) { "次版本号和修订号要小于 100" }
    major.toInt() * 10000 + minor.toInt() * 100 + patch.toInt()
}

// 发版签名：密钥不进仓库，四项都从环境变量读。CI 里来自 GitHub Secrets，见 docs/release.md。
// 没配齐时 release 包不签名（本机照样能 assembleRelease 看混淆结果），发版流程会因验签失败而停下。
val releaseSigning = listOf("LIFEGUIDE_KEYSTORE", "LIFEGUIDE_KEYSTORE_PASSWORD", "LIFEGUIDE_KEY_ALIAS", "LIFEGUIDE_KEY_PASSWORD")
    .associateWith { providers.environmentVariable(it).orNull }
    .takeIf { env -> env.values.all { !it.isNullOrEmpty() } }

android {
    namespace = "app.lifeguide.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        // 上架前确认最终包名
        applicationId = "app.lifeguide"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName
    }
    signingConfigs {
        if (releaseSigning != null) {
            create("release") {
                storeFile = file(releaseSigning.getValue("LIFEGUIDE_KEYSTORE")!!)
                storePassword = releaseSigning.getValue("LIFEGUIDE_KEYSTORE_PASSWORD")
                keyAlias = releaseSigning.getValue("LIFEGUIDE_KEY_ALIAS")
                keyPassword = releaseSigning.getValue("LIFEGUIDE_KEY_PASSWORD")
            }
        }
    }
    buildFeatures { compose = true }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(projects.shared)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.ui)
}
