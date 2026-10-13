package app.lifeguide.core.data

import app.lifeguide.core.corpus.CorpusParser
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 选哪份正文：内置、下载的、以及「装了新版 App 后内置的反而更新」 */
class CorpusRepositoryTest {
    private val dir = File(System.getProperty("upstream") ?: error("要经 Gradle 的 jvmTest 跑"))
    private val readme = dir.resolve("README.md").readText().replace("\r\n", "\n")
    private val books = CorpusParser.bookFiles(readme)
    private val files = books.associateWith { dir.resolve(it).readText().replace("\r\n", "\n") } + ("README.md" to readme)

    private val bundledSha = "c".repeat(40)
    private val bundledDate = "2026-10-01T00:00:00Z"
    private var manifestReads = 0
    private val bundled = BundledCorpusSource { path ->
        if (path == "manifest.json") {
            manifestReads++
            """{"commit":"$bundledSha","commitDate":"$bundledDate","files":[],"entryCount":0}"""
        } else files.getValue(path)
    }

    private fun downloaded(store: CorpusStore, sha: String, date: String) =
        store.install(CorpusManifest(sha, date, books, 0), files)

    @Test
    fun usesBundledWhenNothingDownloaded() = runTest {
        val repo = CorpusRepository(CorpusStore(FakeFileSystem(), "/d".toPath()), bundled)
        val c = repo.ensureLoaded()
        assertEquals(CorpusSource.Bundled, c.source)
        assertEquals(bundledSha, c.manifest.commit)
    }

    @Test
    fun usesNewerDownload() = runTest {
        val store = CorpusStore(FakeFileSystem(), "/d".toPath())
        downloaded(store, "d".repeat(40), "2026-10-12T00:00:00Z")
        val c = CorpusRepository(store, bundled).ensureLoaded()
        assertEquals(CorpusSource.Downloaded, c.source)
        assertEquals("d".repeat(40), c.manifest.commit)
    }

    @Test
    fun olderDownloadIsDiscarded() = runTest {
        val store = CorpusStore(FakeFileSystem(), "/d".toPath())
        downloaded(store, "e".repeat(40), "2026-09-01T00:00:00Z")
        val c = CorpusRepository(store, bundled).ensureLoaded()
        assertEquals(CorpusSource.Bundled, c.source)
        assertNull(store.activeManifest(), "旧的下载快照应当被清掉")
    }

    @Test
    fun concurrentEnsureLoadedLoadsOnce() = runTest {
        val repo = CorpusRepository(CorpusStore(FakeFileSystem(), "/d".toPath()), bundled)
        val results = List(5) { async { repo.ensureLoaded() } }.awaitAll()
        assertEquals(1, manifestReads)
        assertEquals(1, results.map { it.manifest.commit }.distinct().size)
    }
}
