package app.lifeguide.ui

/**
 * 筛选与徽章的文案，逐字取自上游 index.html（侧栏 .chips[data-dim] 的按钮、LABEL、LENS_LABEL、WHO_LABEL），
 * 让用 App 和用网页的人说的是同一套话。
 */
object Labels {
    data class Opt(val value: String, val label: String)
    data class Group(val dim: String, val title: String, val hint: String?, val options: List<Opt>)

    val GROUPS = listOf(
        Group("who", "我的情况", "不相干的条目会藏起来", listOf(
            Opt("怀孕", "怀孕或备孕"), Opt("婴幼儿", "有 3 岁以下孩子"), Opt("孩子", "有 3 到 18 岁孩子"),
            Opt("老人", "家有老人"), Opt("35岁", "35 岁以上"), Opt("60岁", "60 岁以上"),
            Opt("女性", "女性"), Opt("无", "以上都不是"),
        )),
        Group("ratio", "性价比", "作者判断，同口径内可比", listOf(Opt("极高", "极高"), Opt("高", "高"), Opt("一般", "一般"))),
        Group("lens", "换回什么", "口径，不跨口径比较", listOf(
            Opt("死亡率", "寿命"), Opt("金钱", "钱"), Opt("时间", "时间精力"), Opt("自由", "人身自由"),
        )),
        Group("grade", "证据等级", "荟萃/RCT · 有研究 · 共识", listOf(Opt("A", "A 级"), Opt("B", "B 级"), Opt("C", "C 级"))),
        Group("money", "花钱", null, listOf(Opt("0", "不花钱"), Opt("少", "少"), Opt("多", "多"))),
        Group("time", "花时间", null, listOf(Opt("少", "顺手"), Opt("中", "几小时"), Opt("多", "每天占用"))),
        Group("will", "要毅力", null, listOf(Opt("否", "不用"), Opt("些", "一点"), Opt("是", "很多"))),
    )

    val LENS = mapOf("死亡率" to "换寿命", "金钱" to "换钱", "时间" to "换时间精力", "自由" to "换人身自由")
    val WHO = mapOf(
        "怀孕" to "怀孕或备孕", "婴幼儿" to "家有 3 岁以下孩子", "孩子" to "家有 3 到 18 岁孩子", "老人" to "家有老人",
        "35岁" to "35 岁以上", "60岁" to "60 岁以上", "女性" to "女性",
    )
    val MONEY = mapOf("0" to "不花钱", "少" to "花少量钱", "多" to "花不少钱")
    val TIME = mapOf("少" to "顺手", "中" to "花几小时", "多" to "每天占时间")
    val WILL = mapOf("否" to "不用毅力", "些" to "要一点毅力", "是" to "要很多毅力")

    // 证据分级的含义，取自上游 README「证据分级」
    val GRADE = mapOf(
        "A" to "有可量化证据：来自荟萃分析、大型队列或随机对照试验，能给出具体数字",
        "B" to "有研究支撑但难以量化，或证据来自小样本、单一研究",
        "C" to "作者经验或普遍共识，没有直接文献",
    )

    /** 「人群=」显示成一句话：逗号是或、加号是且 */
    fun who(groups: List<List<String>>): String =
        groups.joinToString(" 或 ") { g -> g.joinToString("且") { WHO[it] ?: it } }
}
