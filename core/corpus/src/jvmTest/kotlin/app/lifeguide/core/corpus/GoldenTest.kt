package app.lifeguide.core.corpus

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * 拿上游网页自己的 JS（tools/golden/export.mjs 跑出来的 golden.json）对拍 Kotlin 解析结果。
 * 两边读的是同一份 upstream/（:shared:syncCorpus 打进 App 的也是它，只多做了换行符归一，解析器本身也做）。
 */
class GoldenTest {
    private val golden: JsonObject = Json.parseToJsonElement(File(prop("golden")).readText()).jsonObject
    private val upstream = File(prop("upstream"))
    private val readme = upstream.resolve("README.md").readText()
    private val sections = CorpusParser.parseFiles(readme) { upstream.resolve(it).readText() }
    private val entries = sections.flatMap { it.entries }

    private fun prop(k: String) = System.getProperty(k) ?: error("缺系统属性 $k，要经 Gradle 的 jvmTest 跑")

    @Test
    fun totals() {
        assertEquals(golden.strings("files"), CorpusParser.bookFiles(readme), "文件清单")
        assertEquals(golden.getValue("sectionCount").jsonPrimitive.int, sections.size, "节数")
        assertEquals(golden.getValue("entryCount").jsonPrimitive.int, entries.size, "条目数（对 golden）")
        // 再用一条和两边解析器都不相干的正则数一遍条目标题（syncCorpus 写进 manifest 的也是这个数）
        val counted = CorpusParser.bookFiles(readme).sumOf { f ->
            Regex("""^### \d+\. """, RegexOption.MULTILINE).findAll(upstream.resolve(f).readText()).count()
        }
        assertEquals(counted, entries.size, "条目数（对独立计数）")
        assertTrue(entries.size > 600, "条目数只有 ${entries.size}，正文可能没读全")
        val empty = entries.filter { e -> listOf(e.cost, e.human, e.gain, e.grade, e.src, e.note).any { it.isEmpty() } }
        assertTrue(empty.isEmpty(), "有字段为空的条目：${empty.map { it.id }}")
    }

    @Test
    fun everyFieldMatches() {
        val gs = golden.getValue("sections").jsonArray
        val problems = mutableListOf<String>()
        assertEquals(gs.size, sections.size)
        gs.zip(sections).forEach { (g, s) ->
            val go = g.jsonObject
            if (go.str("n") != s.n || go.str("title") != s.title) problems += "节 ${s.n}：节号或标题不同"
            if (go.strings("intro") != s.intro) problems += "节 ${s.n}：导读不同"
            val ge = go.getValue("entries").jsonArray
            if (ge.size != s.entries.size) problems += "节 ${s.n}：条数 ${ge.size} vs ${s.entries.size}"
            ge.zip(s.entries).forEach { (je, e) -> problems += diff(je.jsonObject, e) }
        }
        if (problems.isNotEmpty()) fail("${problems.size} 处不一致：\n" + problems.take(40).joinToString("\n"))
    }

    @Test
    fun filtersMatch() {
        val problems = mutableListOf<String>()
        val fs = golden.getValue("filters").jsonArray
        assertTrue(fs.size >= 15)
        for (f in fs.map { it.jsonObject }) {
            val state = FilterState(
                q = f["q"]?.jsonPrimitive?.content ?: "",
                sec = f.set("sec"), who = f.set("who"), ratio = f.set("ratio"), lens = f.set("lens"),
                grade = f.set("grade"), money = f.set("money"), time = f.set("time"), will = f.set("will"),
                dispute = f["dispute"]?.jsonPrimitive?.boolean ?: false,
                todo = f["todo"]?.jsonPrimitive?.boolean ?: false,
            )
            val want = f.strings("ids")
            val got = Filter.apply(entries, state).map { it.id }
            if (want != got) problems += "${f.str("name")}：期望 ${want.size} 条，得到 ${got.size} 条"
        }
        if (problems.isNotEmpty()) fail(problems.joinToString("\n"))
    }

    /** 交叉引用：每条每个字段里认出的引用、指向的目标，和网页逐处一致 */
    @Test
    fun xrefsMatch() {
        val corpus = Corpus(sections)
        val problems = mutableListOf<String>()
        var total = 0
        val gs = golden.getValue("sections").jsonArray
        gs.zip(sections).forEach { (g, s) ->
            val go = g.jsonObject
            total += checkXrefs("节 ${s.n} 导读", go.getValue("introXrefs").jsonArray, xrefsOf(s.intro, corpus, s.n), problems)
            go.getValue("entries").jsonArray.zip(s.entries).forEach { (je, e) ->
                val texts = listOf(e.title, e.cost, e.human) + SplitRules.splitGain(e.gain) + SplitRules.splitGain(e.note) + SplitRules.splitSrc(e.src)
                total += checkXrefs(e.id, je.jsonObject.getValue("xrefs").jsonArray, xrefsOf(texts, corpus, e.sec), problems)
            }
        }
        assertEquals(golden.getValue("xrefCount").jsonPrimitive.int, total, "引用总数")
        assertTrue(total > 500, "只认出 $total 处引用，规则可能坏了")
        if (problems.isNotEmpty()) fail("${problems.size} 处引用不一致：\n" + problems.take(30).joinToString("\n"))
    }

    /** 术语表：解析出的术语、顺序（长词在前）和网页一致 */
    @Test
    fun glossaryParsesLikeWeb() {
        val want = golden.getValue("glossary").jsonArray.map { it.jsonObject.let { o -> Triple(o.str("term"), o.str("meaning"), o.getValue("latin").jsonPrimitive.boolean) } }
        val got = glossary.terms.sortedByDescending { it.term.length }.map { Triple(it.term, it.meaning, it.latin) }
        assertEquals(want, got)
        assertTrue(got.size >= 30, "只解析出 ${got.size} 个术语")
    }

    /**
     * 正文里认出的术语和网页一致，只扣掉 App 故意加严的那一处：单个字母的术语后面要紧跟比较符号（见 [Glossary]）。
     * 网页的每处匹配带着后面几个字，这里用同一条规则算出期望值。
     */
    @Test
    fun glossaryMatchesLikeWebExceptSingleLetters() {
        val corpus = Corpus(sections, glossary)
        val problems = mutableListOf<String>()
        var total = 0
        var dropped = 0
        golden.getValue("sections").jsonArray.zip(sections).forEach { (g, s) ->
            g.jsonObject.getValue("entries").jsonArray.zip(s.entries).forEach { (je, e) ->
                val js = je.jsonObject.getValue("terms").jsonArray.map { it.jsonArray.let { a -> a[0].jsonPrimitive.content to a[1].jsonPrimitive.content } }
                val want = js.filter { (t, after) -> t.length > 1 || Glossary.isComparison(after, 0) }.map { it.first }
                dropped += js.size - want.size
                val texts = listOf(e.title, e.cost, e.human) + SplitRules.splitGain(e.gain) + SplitRules.splitGain(e.note) + SplitRules.splitSrc(e.src)
                val got = texts.flatMap { t -> glossaryTermsIn(t, corpus, e.sec) }
                total += got.size
                if (want != got) problems += "${e.id}：\n  js=$want\n  kt=$got"
            }
        }
        if (problems.isNotEmpty()) fail("${problems.size} 条术语不一致：\n" + problems.take(30).joinToString("\n"))
        assertTrue(total > 500, "只认出 $total 处术语")
        // 扣掉的应当正好是克数、利率这类单字母误认，几十处，不该是几百处
        assertTrue(dropped in 1..100, "单字母规则扣掉了 $dropped 处")
    }

    private val glossary by lazy { Glossary.parse(readme) }

    /** 和界面一样：普通文字、粗体、引用文字里认术语，链接里不认 */
    private fun glossaryTermsIn(text: String, corpus: Corpus, sec: String): List<String> =
        InlineMd.parse(text, xref = XrefContext(corpus, sec)).flatMap { p ->
            when (p) {
                is Inline.Text -> corpus.glossary.find(p.text)
                is Inline.Bold -> corpus.glossary.find(p.text)
                is Inline.Xref -> corpus.glossary.find(p.text)
                is Inline.Link, is Inline.Url -> emptyList()
            }
        }.map { it.second.term }

    /** 对照组：把某一节的「本节」解析错，必须对不上 */
    @Test
    fun xrefCheckCatchesWrongSection() {
        val corpus = Corpus(sections)
        val s = sections.first { sec -> sec.entries.any { xrefsOf(listOf(it.note), corpus, sec.n).any { x -> "节" !in x.first() } } }
        val e = s.entries.first { xrefsOf(listOf(it.note), corpus, s.n).any { x -> "节" !in x.first() } }
        val other = sections.first { it.n != s.n }.n
        assertTrue(xrefsOf(listOf(e.note), corpus, s.n) != xrefsOf(listOf(e.note), corpus, other), "换了当前节，结果应当不同")
    }

    private fun xrefsOf(texts: List<String>, corpus: Corpus, sec: String): List<List<String>> =
        texts.flatMap { t -> InlineMd.parse(t, xref = XrefContext(corpus, sec)) }
            .filterIsInstance<Inline.Xref>()
            .map { x ->
                listOf(x.text) + x.targets.map {
                    when (it) {
                        is XrefTarget.ToEntry -> it.id
                        is XrefTarget.ToSection -> "s" + it.n
                    }
                }
            }

    private fun checkXrefs(where: String, want: JsonArray, got: List<List<String>>, problems: MutableList<String>): Int {
        val w = want.map { a -> a.jsonArray.map { it.jsonPrimitive.content } }
        if (w != got) problems += "$where：\n  js=$w\n  kt=$got"
        return got.size
    }

    /** 对照组：比对函数本身要抓得住改动，否则上面的「全部一致」不能信 */
    @Test
    fun diffCatchesChanges() {
        val g0 = golden.getValue("sections").jsonArray[0].jsonObject.getValue("entries").jsonArray[0].jsonObject
        val e0 = sections[0].entries[0]
        assertTrue(diff(g0, e0).isEmpty(), "基准就不一致：${diff(g0, e0)}")
        val mutants = listOf(
            e0.copy(title = e0.title + "x"), e0.copy(human = ""), e0.copy(hay = e0.hay.drop(1)),
            e0.copy(who = listOf(listOf("女性"))), e0.copy(ratio = "一般"), e0.copy(cs = e0.cs + 1),
            e0.copy(dispute = !e0.dispute), e0.copy(todo = !e0.todo), e0.copy(gain = e0.gain + "。"),
            e0.copy(src = e0.src.replaceFirst(" ; ", "；")),
        )
        mutants.forEachIndexed { i, m -> assertTrue(diff(g0, m).isNotEmpty(), "变异 #$i 没被抓到") }
    }

    private fun diff(g: JsonObject, e: Entry): List<String> {
        val out = mutableListOf<String>()
        fun chk(k: String, want: Any?, got: Any?) { if (want != got) out += "${e.id} $k：\n  js=${short(want)}\n  kt=${short(got)}" }
        listOf(
            "sec" to e.sec, "n" to e.n, "title" to e.title, "cost" to e.cost, "human" to e.human,
            "gain" to e.gain, "grade" to e.grade, "src" to e.src, "note" to e.note, "money" to e.money,
            "time" to e.time, "will" to e.will, "level" to e.level, "lens" to e.lens, "ratio" to e.ratio, "hay" to e.hay,
        ).forEach { (k, v) -> chk(k, g.str(k), v) }
        chk("who", g.getValue("who").jsonArray.map { it.jsonArray.map { x -> x.jsonPrimitive.content } }, e.who)
        chk("dispute", g.getValue("dispute").jsonPrimitive.boolean, e.dispute)
        chk("todo", g.getValue("todo").jsonPrimitive.boolean, e.todo)
        chk("cs", g.getValue("cs").jsonPrimitive.int, e.cs)
        chk("gainChunks", g.strings("gainChunks"), SplitRules.splitGain(e.gain))
        chk("noteChunks", g.strings("noteChunks"), SplitRules.splitGain(e.note))
        chk("srcParts", g.strings("srcParts"), SplitRules.splitSrc(e.src))
        return out
    }

    private fun short(v: Any?) = v.toString().let { if (it.length > 160) it.take(160) + "…（${it.length} 字）" else it }
    private fun JsonObject.str(k: String) = getValue(k).jsonPrimitive.content
    private fun JsonObject.strings(k: String) = getValue(k).jsonArray.map { it.jsonPrimitive.content }
    private fun JsonObject.set(k: String) = (this[k] as? JsonArray)?.map { it.jsonPrimitive.content }?.toSet() ?: emptySet()
}
