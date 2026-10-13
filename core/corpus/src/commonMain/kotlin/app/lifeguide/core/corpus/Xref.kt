package app.lifeguide.core.corpus

/** 交叉引用指向哪里 */
sealed interface XrefTarget {
    /** 一条，[id] 是「节号-条号」，和 [Entry.id] 同一个键 */
    data class ToEntry(val id: String) : XrefTarget
    /** 整节 */
    data class ToSection(val n: String) : XrefTarget
}

/** 解析引用要知道全书有哪些条目，以及正在渲染的是哪一节（「本节第 X 条」和省掉节号的「第 X 条」按它定位） */
class XrefContext(val corpus: Corpus, val currentSection: String)

/**
 * 正文里的「见第 8 节第 16 条」「本节第 3 条」「第 12、13 条」「第 8 到 10 条」「见第 9 节」。
 * 移植自上游 index.html 的 NUMS / XREF_RE / xrefKeys / xrefInto。
 * 只有能解析到真实条目或节的才算引用：来源和备注里的法条条款号（「第 285 条」）解析不到，自然留作普通文字。
 */
object Xref {
    private const val WS = "[$JS_WS]*"
    // 条号可以写成「第 13 条」「第 12、13 条」，也可以写成「第 8 到 10 条」这种范围
    // 写成 ${WS}到 而不是 $WS到：Kotlin 标识符能含汉字，后者会被当成一个叫「WS到」的变量
    private const val NUMS = """[\d、]+(?:${WS}到$WS\d+)?"""
    // 整节指路（最后一段）必须排在最后：前面几段先吃掉「第 9 节第 3 条」，剩下的才轮到它
    private val RE = Regex(
        """第$WS(\d+)${WS}节第$WS($NUMS)${WS}条""" +
            """|本节第$WS($NUMS)${WS}条""" +
            """|第$WS($NUMS)${WS}条""" +
            """|第$WS([\d、]+)${WS}节""",
    )
    private val RANGE = Regex("""^$WS(\d+)${WS}到$WS(\d+)$WS$""")

    /** 「8 到 10」摊成 8、9、10；上限 40 是防着把法条条款号那种大范围摊成几百个键 */
    internal fun entryKeys(sec: String, spec: String, corpus: Corpus): List<String> {
        val ns = mutableListOf<String>()
        for (part in spec.split('、')) {
            val r = RANGE.find(part)
            if (r != null) {
                val a = r.groupValues[1].toLongOrNull()
                val b = r.groupValues[2].toLongOrNull()
                if (a != null && b != null && b >= a && b - a <= 40) for (i in a..b) ns += i.toString()
                continue
            }
            val n = part.jsTrim()
            if (n.isNotEmpty()) ns += n
        }
        return ns.map { "$sec-$it" }.filter { corpus.entry(it) != null }
    }

    /** 把一段普通文字切成文字和引用 */
    fun split(s: String, ctx: XrefContext): List<Inline> {
        val out = mutableListOf<Inline>()
        var last = 0
        for (m in RE.findAll(s)) {
            val g = m.groups
            val targets: List<XrefTarget> = if (g[5] != null) {
                g[5]!!.value.split('、').map { it.jsTrim() }.filter { ctx.corpus.sectionByN[it] != null }.map { XrefTarget.ToSection(it) }
            } else {
                val sec = g[1]?.value ?: ctx.currentSection
                val spec = g[2]?.value ?: g[3]?.value ?: g[4]?.value ?: ""
                entryKeys(sec, spec, ctx.corpus).map { XrefTarget.ToEntry(it) }
            }
            if (targets.isEmpty()) continue
            if (m.range.first > last) out += Inline.Text(s.substring(last, m.range.first))
            out += Inline.Xref(m.value, targets)
            last = m.range.last + 1
        }
        if (last < s.length) out += Inline.Text(s.substring(last))
        return out
    }
}
