package app.lifeguide.corpus

import app.lifeguide.filter.Filter
import app.lifeguide.filter.FilterState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
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
 * 正文用的是 :shared:syncCorpus 打进 App 的那一份，不是另读一遍 upstream/。
 */
class GoldenTest {
    private val golden: JsonObject = Json.parseToJsonElement(File(prop("golden")).readText()).jsonObject
    private val corpusDir = File(prop("corpus"))
    private val readme = corpusDir.resolve("README.md").readText()
    private val sections = CorpusParser.parseFiles(readme) { corpusDir.resolve(it).readText() }
    private val entries = sections.flatMap { it.entries }

    private fun prop(k: String) = System.getProperty(k) ?: error("缺系统属性 $k，要经 Gradle 的 jvmTest 跑")

    @Test
    fun totals() {
        val manifest = Json.parseToJsonElement(corpusDir.resolve("manifest.json").readText()).jsonObject
        assertEquals(golden.strings("files"), CorpusParser.bookFiles(readme), "文件清单")
        assertEquals(golden.getValue("sectionCount").jsonPrimitive.int, sections.size, "节数")
        assertEquals(golden.getValue("entryCount").jsonPrimitive.int, entries.size, "条目数（对 golden）")
        // manifest 的条数是 syncCorpus 用一条独立的正则数出来的，和两边解析器都不相干
        assertEquals(manifest.getValue("entryCount").jsonPrimitive.int, entries.size, "条目数（对 manifest）")
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
    @Suppress("unused") private fun JsonElement.dummy() = Unit
}
