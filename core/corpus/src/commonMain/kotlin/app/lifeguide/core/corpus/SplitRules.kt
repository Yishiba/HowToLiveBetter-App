package app.lifeguide.core.corpus

/**
 * 移植自上游 index.html 的 `// <split-rules>` 段（与 tools/lib/split-items.mjs 同一套规则）：
 * 收益、备注超过 300 字的，在括号外的句号处、且下一句开始引用新依据时拆块；来源按括号外的「；」拆条。
 * 只改版式不动字：拆出来的块按顺序拼回去和原文逐字相同。
 */
object SplitRules {
    const val GAIN_MIN_LEN = 300
    const val GAIN_MIN_CHUNK = 25

    // 与上游逐条对应；JS 里没有 u 标志，[一-龥] 按 UTF-16 码元比较，Kotlin 正则同义
    private val GAIN_TOPIC_START = listOf(
        Regex("""^[^，。「」《》：；（）的]{1,12}看(《|[^，。「」的]{0,10}法[：「（第])"""),
        Regex("""^《[^》]{2,40}》"""),
        Regex("""^(?!该|本|此|这|同|上述|但|而|所以|因此|并且|也)[一-龥]{1,12}法(第[一二三四五六七八九十百零]+条|「)"""),
        Regex("""^(另一|另有一|还有一|再一|又一)(项|篇)"""),
        Regex("""^(国内|国外|国内外)也有"""),
        Regex("""^案例[一二三四五六七八九十]"""),
        Regex("""^(同一天|同一试验|同一指南|同一研究)"""),
        Regex("""^(美国|英国|欧洲|世界卫生组织|WHO|中国)[^，。]{0,20}(指南|共识|建议|学会)"""),
        Regex("""^(先说|再说|最后说|先看|再看|最后看)"""),
        Regex("""^一(项|篇)[^，。]{0,20}(试验|研究|综述|分析|调查)"""),
        Regex("""^(芬兰|北欧|英国|美国|日本|韩国|瑞典|丹麦|挪威|德国|法国|澳大利亚|加拿大|荷兰|台湾|香港|上海|北京)[^，。]{0,25}(研究|数据库|试验|队列|调查)"""),
    )

    fun splitGain(text: String): List<String> {
        if (text.isEmpty() || text.length < GAIN_MIN_LEN) return listOf(text)
        val sentences = mutableListOf<String>()
        val buf = StringBuilder()
        var depth = 0
        for (c in text) {
            if (c in "「『《（") depth++
            else if (c in "」』》）") depth = maxOf(0, depth - 1)
            buf.append(c)
            if (c == '。' && depth == 0) { sentences += buf.toString(); buf.clear() }
        }
        if (buf.isNotEmpty()) sentences += buf.toString()

        val chunks = mutableListOf<String>()
        for (s in sentences) {
            if (chunks.isNotEmpty() && GAIN_TOPIC_START.any { it.containsMatchIn(s) }) chunks += s
            else if (chunks.isNotEmpty()) chunks[chunks.lastIndex] += s
            else chunks += s
        }
        val out = mutableListOf<String>()
        for (c in chunks) {
            if (out.isNotEmpty() && c.length < GAIN_MIN_CHUNK) out[out.lastIndex] += c
            else out += c
        }
        if (out.size > 1 && out[0].length < GAIN_MIN_CHUNK) {
            val merged = out[0] + out[1]
            out.removeAt(0); out[0] = merged
        }
        return out
    }

    fun splitSrc(text: String): List<String> {
        val parts = mutableListOf<String>()
        val buf = StringBuilder()
        var depth = 0
        for (i in text.indices) {
            val c = text[i]
            if (c in "「『（") depth++
            if (c in "」』）") depth = maxOf(0, depth - 1)
            val semi = c == '；' || (c == ';' &&
                text.getOrNull(i - 1)?.isJsWhitespace() == true &&
                text.getOrNull(i + 1)?.isJsWhitespace() == true)
            if (semi && depth == 0) { parts += buf.toString(); buf.clear(); continue }
            buf.append(c)
        }
        parts += buf.toString()
        return parts.map { it.jsTrim() }.filter { it.isNotEmpty() }
    }
}
