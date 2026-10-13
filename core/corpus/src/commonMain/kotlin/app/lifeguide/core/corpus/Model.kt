package app.lifeguide.core.corpus

/** 一节。[n] 是节号字符串（和网页一样按字符串比较），[intro] 是第一条之前的每一段导读。 */
data class Section(
    val n: String,
    val title: String,
    val intro: List<String>,
    val entries: List<Entry>,
)

/**
 * 一条。字段名与取值和网页 index.html 的 parseReadme() 一一对应，golden 测试逐字段比对。
 * [who] 是「人群=」：外层是「或」，内层是「且」，空表示人人适用。
 */
data class Entry(
    val sec: String,
    val n: String,
    val title: String,
    val cost: String = "",
    val human: String = "",
    val gain: String = "",
    val grade: String = "",
    val src: String = "",
    val note: String = "",
    val money: String = "",
    val time: String = "",
    val will: String = "",
    val level: String = "",
    val lens: String = "",
    val who: List<List<String>> = emptyList(),
    val dispute: Boolean = false,
    val todo: Boolean = false,
    val cs: Int = 0,
    val ratio: String = "",
    val hay: String = "",
) {
    /** 和网页锚点 #e-{sec}-{n} 同一个键 */
    val id: String get() = "$sec-$n"
}

class Corpus(val sections: List<Section>) {
    val entries: List<Entry> = sections.flatMap { it.entries }
    val sectionByN: Map<String, Section> = sections.associateBy { it.n }
    private val byId = entries.associateBy { it.id }
    fun entry(id: String): Entry? = byId[id]
}
