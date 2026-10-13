plugins {
    alias(libs.plugins.lifeguide.kmp.library)
    alias(libs.plugins.kotlinSerialization)
}

// 纯 Kotlin 的领域层：正文模型、解析、拆块、筛选、行内 Markdown 切分。不依赖 Compose，也不碰 IO。
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
    }
}

// ---------- golden 对拍 ----------
// 用上游 index.html 自己的解析函数生成标准答案，jvmTest 拿 Kotlin 解析结果逐字段比对。
val upstream = rootProject.layout.projectDirectory.dir("upstream")

val goldenExport = tasks.register<Exec>("goldenExport") {
    val script = rootProject.layout.projectDirectory.file("tools/golden/export.mjs")
    val out = layout.buildDirectory.file("golden/golden.json")
    inputs.file(script)
    inputs.file(upstream.file("index.html"))
    inputs.file(upstream.file("README.md"))
    inputs.dir(upstream.dir("book"))
    outputs.file(out)
    commandLine("node", script.asFile.absolutePath, upstream.asFile.absolutePath, out.get().asFile.absolutePath)
}

tasks.named<Test>("jvmTest") {
    dependsOn(goldenExport)
    inputs.file(upstream.file("README.md"))
    inputs.dir(upstream.dir("book"))
    val golden = goldenExport.map { it.outputs.files.singleFile.absolutePath }
    val up = upstream.asFile.absolutePath
    jvmArgumentProviders += CommandLineArgumentProvider { listOf("-Dgolden=${golden.get()}", "-Dupstream=$up") }
}
