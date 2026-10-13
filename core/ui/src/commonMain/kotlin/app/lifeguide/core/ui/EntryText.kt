package app.lifeguide.core.ui

import app.lifeguide.core.corpus.CorpusParser
import app.lifeguide.core.corpus.Entry
import app.lifeguide.core.corpus.Lens

/** 卡片和条目页上的说明文字。文案取自 [Labels]（和网页同一套说法）。 */
object EntryText {
    /**
     * 要花什么：只列真正有代价的几项（权重见 [CorpusParser.COST_W]），零成本时说「几乎没有」。
     * 例：钱=0 时间=少 毅力=些 →「要一点毅力」，不写「不花钱，顺手，要一点毅力」。
     */
    fun cost(e: Entry): String {
        if (e.cs == 0) return "几乎没有"
        val w = CorpusParser.COST_W
        return listOfNotNull(
            Labels.MONEY[e.money].takeIf { (w.getValue("money")[e.money] ?: 0) > 0 },
            Labels.TIME[e.time].takeIf { (w.getValue("time")[e.time] ?: 0) > 0 },
            Labels.WILL[e.will].takeIf { (w.getValue("will")[e.will] ?: 0) > 0 },
        ).joinToString("，")
    }

    fun costShort(e: Entry): String = if (e.cs == 0) "零成本" else cost(e)

    /** 换回什么，例如「寿命」 */
    fun gain(e: Entry): String = Lens.of(e.lens)?.gain ?: e.lens

    /** 章节、搜索结果里的小字：性价比极高 · A 级 · 零成本 */
    fun browseMeta(e: Entry): String = listOf("性价比${e.ratio}", "${e.grade} 级", costShort(e)).joinToString(" · ")

    /** 清单里的小字：换寿命 · 不要早死 */
    fun checklistMeta(e: Entry, sectionTitle: String?): String =
        listOfNotNull("换" + gain(e), sectionTitle?.let(::shortTitle)).joinToString(" · ")

    /** 「别把自己搭进去：法律与财产安全」→「别把自己搭进去」，小字里放得下 */
    fun shortTitle(t: String): String = t.substringBefore('：')
}
