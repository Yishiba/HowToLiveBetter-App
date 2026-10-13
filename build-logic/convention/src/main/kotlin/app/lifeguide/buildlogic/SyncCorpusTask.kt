package app.lifeguide.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * 内置正文快照：从 upstream/（锁定 commit 的子模块）复制 README 和 book 目录下各节 md，生成 manifest.json，
 * 输出目录作为 compose 资源打进安装包。
 * 文件清单的取法和网页 index.html 的 init() 一致：README 里所有指向 book 目录下 md 的链接，去重后按路径排序。
 */
@DisableCachingByDefault(because = "要读子模块的 git 信息，复制本身很快")
abstract class SyncCorpusTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
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
        check(books.isNotEmpty()) { "README 里没找到 book 目录下的 md 链接" }

        out.resolve("README.md").writeText(readme)
        var entries = 0
        for (path in books) {
            val text = src.resolve(path).readText().replace("\r\n", "\n")
            entries += Regex("""^### \d+\. """, RegexOption.MULTILINE).findAll(text).count()
            out.resolve(path).apply { parentFile.mkdirs() }.writeText(text)
        }

        val commit = git(src, "rev-parse", "HEAD")
        val date = git(src, "log", "-1", "--format=%cI")
        val files = books.joinToString(",") { "\"$it\"" }
        out.resolve("manifest.json").writeText(
            """{"commit":"$commit","commitDate":"$date","files":[$files],"entryCount":$entries}""" + "\n",
        )
        logger.lifecycle("syncCorpus: ${books.size} 个文件，$entries 条，upstream ${commit.take(7)}")
    }

    private fun git(dir: File, vararg args: String): String {
        val p = ProcessBuilder("git", "-C", dir.absolutePath, *args).redirectErrorStream(true).start()
        val text = p.inputStream.bufferedReader().readText().trim()
        check(p.waitFor() == 0) { "git ${args.joinToString(" ")} 失败：$text" }
        return text
    }
}
