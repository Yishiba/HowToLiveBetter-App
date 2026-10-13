package app.lifeguide.core.corpus

/** 术语表里的一个词。一格里写了几个词的（「d、g」）拆成几条，意思相同。 */
data class GlossaryTerm(val term: String, val meaning: String, val latin: Boolean)

/**
 * README「读懂数字（术语表）」那张表，正文里出现这些词时可以点开看解释。
 * 解析和匹配移植自上游 index.html 的 parseGlossary / termify：长词在前保证最长匹配，拉丁术语要求前后不是字母数字。
 *
 * 和网页有一处故意不同：单个字母的术语（d、g、r）只有后面紧跟「=」「≈」「约」这类比较符号时才算。
 * 网页会把「100 g 纯酒精」的克、「利率为 r」的利率也标成效应量和相关系数，解释是错的；
 * 错的解释比没有解释更糟。GoldenTest 按这条规则从网页结果里扣掉相应的匹配再比，差异写在测试里。
 */
class Glossary(
    /** README 里的顺序（作者按「统计 → 医学 → 金钱法律」排的），术语表页面照这个顺序列 */
    val terms: List<GlossaryTerm>,
) {
    private val byTerm = terms.associateBy { it.term }

    private val regex: Regex? = run {
        // 稳定排序：等长的保持原顺序，同网页 Array.prototype.sort
        val rows = terms.sortedByDescending { it.term.length }
        val latin = rows.filter { it.latin }
        val cjk = rows.filterNot { it.latin }
        fun alt(list: List<GlossaryTerm>) = list.joinToString("|") { Regex.escape(it.term) }
        val parts = buildList {
            if (latin.isNotEmpty()) add("(?<![A-Za-z0-9])(?:${alt(latin)})(?![A-Za-z0-9])")
            if (cjk.isNotEmpty()) add("(?:${alt(cjk)})")
        }
        if (parts.isEmpty()) null else Regex(parts.joinToString("|"))
    }

    /** 一段文字里出现的术语和位置，按出现顺序 */
    fun find(s: String): List<Pair<IntRange, GlossaryTerm>> {
        val re = regex ?: return emptyList()
        return re.findAll(s).mapNotNull { m ->
            val g = byTerm[m.value] ?: return@mapNotNull null
            if (g.term.length == 1 && !isComparison(s, m.range.last + 1)) return@mapNotNull null
            m.range to g
        }.toList()
    }

    companion object {
        val EMPTY = Glossary(emptyList())

        private val SECTION = Regex("""^## 读懂数字[^\n]*\n([\s\S]*?)(?=^## )""", RegexOption.MULTILINE)
        private val ROW = Regex("""^\|[$JS_WS]*(.+?)[$JS_WS]*\|[$JS_WS]*(.+?)[$JS_WS]*\|$""")
        private val DASHES = Regex("^-+$")
        private val LATIN = Regex("^[A-Za-z0-9%. ]+$")
        private val COMPARISON = Regex("^[$JS_WS]*[=≈＝<>≤≥约]")

        /** 单字母术语的判据：后面（隔着空白）是比较符号，「g = 0.53」「r 约 0.3」 */
        internal fun isComparison(s: String, from: Int) = COMPARISON.containsMatchIn(s.substring(from))

        fun parse(readme: String): Glossary {
            val body = SECTION.find(readme)?.groupValues?.get(1) ?: return EMPTY
            val rows = mutableListOf<GlossaryTerm>()
            for (line in body.split(Regex("\r?\n"))) {
                val c = ROW.find(line) ?: continue
                val first = c.groupValues[1]
                if (first == "术语" || DASHES.matches(first)) continue
                for (t in first.split('、')) {
                    val term = t.jsTrim()
                    if (term.isNotEmpty()) rows += GlossaryTerm(term, c.groupValues[2], LATIN.matches(term))
                }
            }
            return Glossary(rows)
        }
    }
}
