package app.lifeguide.core.corpus

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 清单、场景目录、「遇到事了」这些产品规则在真实正文上成立 */
class GuideTest {
    private val upstream = File(System.getProperty("upstream") ?: error("要经 Gradle 的 jvmTest 跑"))
    private val readme = upstream.resolve("README.md").readText()
    private val corpus = Corpus(CorpusParser.parseFiles(readme) { upstream.resolve(it).readText() })

    @Test
    fun coreIsZeroCostBigGainGradeA() {
        val core = corpus.entries.filter { it.isCore }
        assertTrue(core.size in 50..200, "核心条目 ${core.size} 条，规则或正文可能变了")
        assertTrue(core.all { it.cs == 0 && it.level == "大" && it.grade == "A" })
    }

    /** 清单按标题记勾；标题重复的话，勾一条会把另一条也勾上 */
    @Test
    fun keysAreUnique() {
        val dup = corpus.entries.groupBy { it.key }.filterValues { it.size > 1 }
        assertTrue(dup.isEmpty(), "标题重复：${dup.mapValues { (_, v) -> v.map { it.id } }}")
    }

    /** 上游新增了节会进「其他」，App 照常能用，但要来更新映射表 */
    @Test
    fun everySectionHasAScene() {
        val groups = Scenes.group(corpus.sections)
        assertFalse(groups.any { it.first == Scenes.OTHER }, "有节没归入场景：${groups.firstOrNull { it.first == Scenes.OTHER }?.second?.map { it.n }}")
        assertEquals(corpus.sections.size, groups.sumOf { it.second.size })
    }

    @Test
    fun situationsPointToExistingSections() {
        val missing = Situations.ALL.filter { corpus.sectionByN[it.second] == null }
        assertTrue(missing.isEmpty(), "指向不存在的节：$missing")
    }

    @Test
    fun everyLensIsKnown() {
        val unknown = corpus.entries.filter { Lens.of(it.lens) == null }.map { it.id }
        assertTrue(unknown.isEmpty(), "口径不认识：$unknown")
    }

    @Test
    fun changesFindAddedAndEditedButNotRenumbered() {
        val s0 = corpus.sections[0]
        val e0 = s0.entries[0]
        val e1 = s0.entries[1]
        // 删掉第一条（于是后面的条号都要变）、改第二条的说人话
        val older = Corpus(listOf(s0.copy(entries = listOf(e1.copy(n = "1", human = e1.human + "（旧）")) + s0.entries.drop(2))) + corpus.sections.drop(1))
        val c = CorpusChanges.between(older, corpus)
        assertEquals(listOf(e0.key), c.added)
        assertEquals(listOf(e1.key), c.changed)
        assertTrue(CorpusChanges.between(corpus, corpus).isEmpty)
    }
}
