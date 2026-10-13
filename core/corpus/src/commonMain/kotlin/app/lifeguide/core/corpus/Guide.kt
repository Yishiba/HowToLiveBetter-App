package app.lifeguide.core.corpus

import kotlinx.serialization.Serializable

/**
 * 原书最该先做的一批：性价比极高（不花钱、顺手、不用毅力，并且收益大）且证据 A 级。
 * App 的清单默认就是这批，见 docs/ux.md「依据」。
 */
val Entry.isCore: Boolean get() = ratio == "极高" && grade == "A"

/**
 * 清单、更新提示用的稳定键：标题去掉 Markdown 记号。
 * 不用「第几节第几条」：上游插入新条目会让条号整体后移，标题比条号稳定。
 */
val Entry.key: String get() = InlineMd.plain(title).trim()

/** 「换回什么」，即原书的口径。清单按它分段。 */
enum class Lens(val value: String, val tab: String, val gain: String) {
    Life("死亡率", "活得久", "寿命"),
    Money("金钱", "少亏钱", "钱"),
    Freedom("自由", "不惹事", "人身自由"),
    Time("时间", "省时间", "时间精力"),
    ;

    companion object {
        fun of(value: String): Lens? = entries.firstOrNull { it.value == value }
    }
}

/** 查找页的章节目录：34 节按生活场景分 5 组。上游新增的节先归入 [OTHER]。 */
object Scenes {
    const val OTHER = "其他"

    val ALL: List<Pair<String, List<String>>> = listOf(
        "身心健康" to listOf("1", "2", "16", "22", "24", "28", "29", "33", "34"),
        "钱与时间" to listOf("3", "4", "5", "6", "7", "15"),
        "安全与法律" to listOf("8", "9", "11", "13", "14", "21"),
        "工作与成长" to listOf("12", "19", "23", "26", "31", "32"),
        "家庭" to listOf("10", "17", "18", "20", "25", "27", "30"),
    )

    /** 按场景分好组的章节，组内按节号排；没有章节的组不出现 */
    fun group(sections: List<Section>): List<Pair<String, List<Section>>> {
        val byN = sections.associateBy { it.n }
        val placed = ALL.flatMap { it.second }.toSet()
        val groups = ALL.map { (name, ns) -> name to ns.mapNotNull { byN[it] } }
        val other = sections.filter { it.n !in placed }
        return (groups + (OTHER to other)).filter { it.second.isNotEmpty() }
    }
}

/** 查找页「遇到事了」：急事一点直达对应章节 */
object Situations {
    val ALL: List<Pair<String, String>> = listOf(
        "急救" to "13",
        "被骗了" to "8",
        "看病" to "24",
        "没钱了" to "7",
        "工作纠纷" to "19",
        "租房买房" to "15",
        "家里老人" to "17",
        "怀孕" to "27",
        "新生儿" to "20",
        "亲人去世" to "25",
    )
}

/** 两份正文之间新增和改动的条目（按 [key] 对齐） */
@Serializable
data class CorpusChanges(val added: List<String>, val changed: List<String>) {
    val isEmpty: Boolean get() = added.isEmpty() && changed.isEmpty()

    companion object {
        fun between(old: Corpus, new: Corpus): CorpusChanges {
            val before = old.entries.associateBy { it.key }
            val added = mutableListOf<String>()
            val changed = mutableListOf<String>()
            for (e in new.entries) {
                val o = before[e.key]
                when {
                    o == null -> added += e.key
                    o.content != e.content -> changed += e.key
                }
            }
            return CorpusChanges(added, changed)
        }

        // 只比正文字段，节号和条号变了不算改动
        private val Entry.content
            get() = listOf(cost, human, gain, grade, src, note, money, time, will, level, lens, who.toString())
    }
}
