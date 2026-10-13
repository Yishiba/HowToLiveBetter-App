package app.lifeguide.core.corpus

/**
 * 逐行移植自上游 index.html 的 parseReadme()。上游改了解析规则，这里要跟着改，
 * golden 测试（tools/golden/export.mjs 用上游原函数生成）会把偏差逐字段报出来。
 */
object CorpusParser {
    // 性价比三档：和 index.html 的 COST_W、e.ratio 两行一致
    val COST_W: Map<String, Map<String, Int>> = mapOf(
        "money" to mapOf("0" to 0, "少" to 1, "多" to 2),
        "time" to mapOf("少" to 0, "中" to 1, "多" to 2),
        "will" to mapOf("否" to 0, "些" to 1, "是" to 2),
    )

    private val SEC = Regex("""^#{1,2} (\d+)\. (.+)$""")
    private val ANY_HEAD = Regex("""^#{1,2} """)
    private val ENTRY = Regex("""^### (\d+)\. (.+)$""")
    private val TAG = Regex("""^<!--[$JS_WS]*成本标签:[$JS_WS]*(.*?)[$JS_WS]*-->""")
    private val COST = Regex("""^- 成本：(.*)$""")
    private val HUMAN = Regex("""^- 说人话：(.*)$""")
    private val GAIN = Regex("""^- 收益：(.*)$""")
    private val GRADE = Regex("""^- 证据等级：[$JS_WS]*([ABC])""")
    private val SRC = Regex("""^- 来源：(.*)$""")
    private val NOTE = Regex("""^- 备注：(.*)$""")
    private val TODO = Regex("待核实|TODO")
    private val LINE_SPLIT = Regex("\r?\n")
    private val ESC = Regex("""\\([*_])""")

    /** 解析拼接好的全书（各 book 文件按路径排序后以空行连接，同 init()） */
    fun parse(md: String): List<Section> {
        class SecB(val n: String, val title: String) {
            val intro = mutableListOf<String>()
            val entries = mutableListOf<Entry>()
        }

        val sections = mutableListOf<SecB>()
        var sec: SecB? = null
        var entry: Entry? = null
        fun flush() {
            val e = entry
            if (e != null && sec != null) sec!!.entries += e
            entry = null
        }

        for (raw in md.split(LINE_SPLIT)) {
            val line = raw.jsTrimEnd()
            val secM = SEC.find(line)
            if (secM != null) {
                flush()
                sec = SecB(secM.groupValues[1], secM.groupValues[2].jsTrim()).also { sections += it }
                continue
            }
            if (ANY_HEAD.containsMatchIn(line)) { flush(); sec = null; continue }
            val s = sec ?: continue
            val entryM = ENTRY.find(line)
            if (entryM != null) {
                flush()
                entry = Entry(sec = s.n, n = entryM.groupValues[1], title = entryM.groupValues[2].jsTrim())
                continue
            }
            val tag = TAG.find(line)
            val cur = entry
            if (tag != null && cur != null) {
                var e: Entry = cur
                for (kv in tag.groupValues[1].jsSplitWs()) {
                    val parts = kv.split('=')
                    val k = parts[0]
                    val v = parts.getOrNull(1) ?: continue
                    e = when (k) {
                        "钱" -> e.copy(money = v)
                        "时间" -> e.copy(time = v)
                        "毅力" -> e.copy(will = v)
                        "收益" -> e.copy(level = v)
                        "口径" -> e.copy(lens = v)
                        "人群" -> e.copy(who = v.split(',').map { it.split('+') })
                        else -> e
                    }
                }
                entry = e
                continue
            }
            if (cur != null) {
                fun g(re: Regex) = re.find(line)?.groupValues?.get(1)
                entry = g(COST)?.let { cur.copy(cost = it) }
                    ?: g(HUMAN)?.let { cur.copy(human = it) }
                    ?: g(GAIN)?.let { cur.copy(gain = it) }
                    ?: g(GRADE)?.let { cur.copy(grade = it) }
                    ?: g(SRC)?.let { cur.copy(src = it) }
                    ?: g(NOTE)?.let { cur.copy(note = it) }
                    ?: cur
                continue
            }
            if (line.isNotEmpty()) s.intro += line
        }
        flush()

        return sections.map { s -> Section(s.n, s.title, s.intro.toList(), s.entries.map(::derive)) }
    }

    private fun derive(e: Entry): Entry {
        val cs = (COST_W.getValue("money")[e.money] ?: 0) +
            (COST_W.getValue("time")[e.time] ?: 0) +
            (COST_W.getValue("will")[e.will] ?: 0)
        return e.copy(
            dispute = e.note.startsWith("争议"),
            todo = TODO.containsMatchIn(e.src + e.gain + e.note + e.cost),
            cs = cs,
            ratio = ratioOf(e.level, cs),
            hay = listOf(e.title, e.human, e.cost, e.gain, e.note, e.src, e.grade).joinToString("\n")
                .replace("**", "").replace(ESC, "$1").lowercase(),
        )
    }

    fun ratioOf(level: String, cs: Int): String = when (level) {
        "大" -> if (cs == 0) "极高" else if (cs <= 2) "高" else "一般"
        "中" -> if (cs == 0) "高" else "一般"
        else -> "一般"
    }

    /** README 里指向 book 目录下 md 文件的链接，去重后按路径排序（同 init()） */
    fun bookFiles(readme: String): List<String> =
        Regex("""\]\((book/[^)]+\.md)\)""").findAll(readme).map { it.groupValues[1] }.distinct().sorted().toList()

    fun parseFiles(readme: String, read: (String) -> String): List<Section> =
        parse(bookFiles(readme).joinToString("\n\n") { read(it).replace("\r\n", "\n") })
}
