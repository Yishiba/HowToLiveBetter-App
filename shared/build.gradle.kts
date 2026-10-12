import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
}

kotlin {
    android {
        namespace = "app.lifeguide.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources { enable = true }
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    // 只用于跑测试（golden 对拍要在 JVM 上读仓库里的文件）和桌面快速预览
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.compose.components.resources)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.okio)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.okio.fakefilesystem)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(compose.desktop.currentOs)
        }
    }
}

// ---------- 内置正文快照 ----------
// 从 upstream/（锁定 commit 的子模块）复制 README 和 book/*.md，生成 manifest.json。
// 文件清单的取法和网页 index.html 的 init() 一致：README 里所有指向 book/*.md 的链接，去重后按路径排序。
abstract class SyncCorpusTask : DefaultTask() {
    @get:InputDirectory
    abstract val upstreamDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun sync() {
        val src = upstreamDir.get().asFile
        val out = outputDir.get().asFile.resolve("files/corpus")
        out.deleteRecursively()
        out.mkdirs()

        val readme = src.resolve("README.md").readText().replace("\r\n", "\n")
        val books = Regex("""\]\((book/[^)]+\.md)\)""").findAll(readme).map { it.groupValues[1] }.toSortedSet().toList()
        check(books.isNotEmpty()) { "README 里没找到 book/*.md 链接" }

        out.resolve("README.md").writeText(readme)
        var entries = 0
        for (path in books) {
            val text = src.resolve(path).readText().replace("\r\n", "\n")
            entries += Regex("""^### \d+\. """, RegexOption.MULTILINE).findAll(text).count()
            out.resolve(path).apply { parentFile.mkdirs() }.writeText(text)
        }

        fun git(vararg args: String): String {
            val p = ProcessBuilder("git", "-C", src.absolutePath, *args).redirectErrorStream(true).start()
            val text = p.inputStream.bufferedReader().readText().trim()
            check(p.waitFor() == 0) { "git ${args.joinToString(" ")} 失败：$text" }
            return text
        }
        val commit = git("rev-parse", "HEAD")
        val date = git("log", "-1", "--format=%cI")
        val files = books.joinToString(",") { "\"$it\"" }
        out.resolve("manifest.json").writeText(
            """{"commit":"$commit","commitDate":"$date","files":[$files],"entryCount":$entries}""" + "\n"
        )
        logger.lifecycle("syncCorpus: ${books.size} 个文件，$entries 条，upstream ${commit.take(7)}")
    }
}

val syncCorpus = tasks.register<SyncCorpusTask>("syncCorpus") {
    upstreamDir.set(rootProject.layout.projectDirectory.dir("upstream"))
    outputDir.set(layout.buildDirectory.dir("corpus-resources"))
}

// ---------- golden 对拍 ----------
// 用上游 index.html 自己的解析函数生成标准答案，jvmTest 拿 Kotlin 解析结果逐字段比对。
val goldenExport = tasks.register<Exec>("goldenExport") {
    val up = rootProject.layout.projectDirectory.dir("upstream")
    val script = rootProject.layout.projectDirectory.file("tools/golden/export.mjs")
    val out = layout.buildDirectory.file("golden/golden.json")
    inputs.file(script)
    inputs.file(up.file("index.html"))
    inputs.file(up.file("README.md"))
    inputs.dir(up.dir("book"))
    outputs.file(out)
    commandLine("node", script.asFile.absolutePath, up.asFile.absolutePath, out.get().asFile.absolutePath)
}

tasks.named<Test>("jvmTest") {
    dependsOn(goldenExport, syncCorpus)
    val golden = goldenExport.map { it.outputs.files.singleFile.absolutePath }
    val corpus = syncCorpus.flatMap { it.outputDir }.map { it.dir("files/corpus").asFile.absolutePath }
    jvmArgumentProviders += CommandLineArgumentProvider {
        listOf("-Dgolden=${golden.get()}", "-Dcorpus=${corpus.get()}")
    }
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
