package app.lifeguide.core.data

import app.lifeguide.core.corpus.Corpus
import app.lifeguide.core.corpus.CorpusParser
import app.lifeguide.core.corpus.isCore
import app.lifeguide.core.corpus.key
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChecklistTest {
    private val dir = File(System.getProperty("upstream") ?: error("要经 Gradle 的 jvmTest 跑"))
    private val readme = dir.resolve("README.md").readText()
    private val corpus = Corpus(CorpusParser.parseFiles(readme) { dir.resolve(it).readText() })
    private val core = corpus.entries.filter { it.isCore }
    private val tagged = core.filter { it.who.isNotEmpty() }

    @Test
    fun coreWithoutProfileSkipsTaggedEntries() {
        val c = ChecklistRepository.build(corpus, emptySet(), emptySet(), emptySet())
        assertEquals(core.size - tagged.size, c.core.size)
        assertTrue(c.core.none { it.entry.who.isNotEmpty() })
    }

    @Test
    fun profileBringsMatchingTaggedEntriesBack() {
        val all = tagged.flatMap { it.who.flatten() }.toSet()
        val c = ChecklistRepository.build(corpus, all, emptySet(), emptySet())
        assertEquals(core.size, c.core.size)
    }

    @Test
    fun doneAndAddedAreTrackedByKey() {
        val nonCore = corpus.entries.first { !it.isCore }
        val first = core.first { it.who.isEmpty() }
        val c = ChecklistRepository.build(corpus, emptySet(), setOf(first.key, nonCore.key), setOf(nonCore.key, first.key))
        assertEquals(listOf(nonCore.key), c.added.map { it.entry.key }, "核心条目加进清单不重复出现")
        assertTrue(c.added.single().done)
        assertTrue(c.core.first { it.entry.key == first.key }.done)
        assertEquals(2, c.doneCount)
    }
}
