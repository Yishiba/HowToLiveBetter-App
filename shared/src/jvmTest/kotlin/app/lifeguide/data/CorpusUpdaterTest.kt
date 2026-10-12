package app.lifeguide.data

import app.lifeguide.corpus.Corpus
import app.lifeguide.corpus.CorpusParser
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import java.io.File
import java.net.URLDecoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 更新器的几条分支：拿真实正文当「远端」，网络用 MockEngine 模拟 */
class CorpusUpdaterTest {
    private val dir = File(System.getProperty("corpus") ?: error("要经 Gradle 的 jvmTest 跑"))
    private val readme = dir.resolve("README.md").readText()
    private val files = CorpusParser.bookFiles(readme).associateWith { dir.resolve(it).readText() } + ("README.md" to readme)
    private val corpus = Corpus(CorpusParser.parseFiles(readme) { files.getValue(it) })

    private val oldSha = "a".repeat(40)
    private val newSha = "b".repeat(40)
    private val current = LoadedCorpus(corpus, CorpusManifest(oldSha, "2026-10-01T00:00:00+08:00", emptyList(), corpus.entries.size), CorpusSource.Bundled)

    private fun commitJson(sha: String, date: String) = """{"sha":"$sha","commit":{"committer":{"date":"$date"}}}"""

    /** 两个镜像：m1 和 m2，路径里带上镜像名，方便按镜像决定返回什么 */
    private val mirrors: List<(String, String) -> String> = listOf(
        { sha, p -> "https://m1.test/$sha/${CorpusUpdater.encodePath(p)}" },
        { sha, p -> "https://m2.test/$sha/${CorpusUpdater.encodePath(p)}" },
    )

    private fun updater(fs: FakeFileSystem, remoteDate: String = "2026-10-12T10:04:49Z", serve: (host: String, path: String) -> String?) =
        CorpusUpdater(
            client = HttpClient(MockEngine { req ->
                val url = req.url
                if (url.host == "api.test") respond(commitJson(newSha, remoteDate), headers = headersOf("Content-Type", "application/json"))
                else {
                    val path = URLDecoder.decode(url.encodedPath.substringAfter("/$newSha/"), "UTF-8")
                    serve(url.host, path)?.let { respond(it) } ?: respondError(HttpStatusCode.InternalServerError)
                }
            }),
            store = CorpusStore(fs, "/data".toPath()),
            current = { current },
            mirrors = mirrors,
            latestCommitUrl = "https://api.test/commits/main",
        )

    @Test
    fun installsNewerCorpusAndFallsBackToSecondMirror() = runTest {
        val fs = FakeFileSystem()
        // 第一个镜像全挂，必须换到第二个
        val r = updater(fs) { host, path -> if (host == "m1.test") null else files[path] }.check()
        assertIs<UpdateResult.Updated>(r)
        val store = CorpusStore(fs, "/data".toPath())
        val m = store.activeManifest()!!
        assertEquals(newSha, m.commit)
        assertEquals(corpus.entries.size, m.entryCount)
        assertEquals(files.getValue("book/01-不要早死.md"), store.read(newSha, "book/01-不要早死.md"))
    }

    @Test
    fun truncatedDownloadKeepsCurrentVersion() = runTest {
        val fs = FakeFileSystem()
        // 每个文件只给前 2% 的内容：解析出来条目远不到九成
        val r = updater(fs) { _, path -> files[path]?.let { if (path.startsWith("book/")) it.take(it.length / 50) else it } }.check()
        assertIs<UpdateResult.Failed>(r)
        assertTrue("校验" in r.reason, r.reason)
        assertNull(CorpusStore(fs, "/data".toPath()).activeManifest())
    }

    @Test
    fun allMirrorsDownFails() = runTest {
        val fs = FakeFileSystem()
        val r = updater(fs) { _, _ -> null }.check()
        assertIs<UpdateResult.Failed>(r)
        assertNull(CorpusStore(fs, "/data".toPath()).activeManifest())
    }

    @Test
    fun olderRemoteIsUpToDate() = runTest {
        val fs = FakeFileSystem()
        val r = updater(fs, remoteDate = "2026-09-01T00:00:00Z") { _, path -> files[path] }.check()
        assertEquals(UpdateResult.UpToDate, r)
    }

    @Test
    fun encodesChinesePath() {
        assertEquals("book/01-%E4%B8%8D.md", CorpusUpdater.encodePath("book/01-不.md"))
    }
}
