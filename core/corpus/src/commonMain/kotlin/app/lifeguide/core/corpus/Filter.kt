package app.lifeguide.core.corpus

import kotlinx.serialization.Serializable

/**
 * 筛选条件，语义同上游 index.html 的 state：同一组内多选是「或」，不同组之间是「且」。
 * [sec] 网页上一次只选一节，这里也只放 0 或 1 个。
 */
@Serializable
data class FilterState(
    val q: String = "",
    val sec: Set<String> = emptySet(),
    val who: Set<String> = emptySet(),
    val ratio: Set<String> = emptySet(),
    val lens: Set<String> = emptySet(),
    val grade: Set<String> = emptySet(),
    val money: Set<String> = emptySet(),
    val time: Set<String> = emptySet(),
    val will: Set<String> = emptySet(),
    val dispute: Boolean = false,
    val todo: Boolean = false,
) {
    /** 「筛选」按钮上的角标：不算关键词和章节 */
    val activeCount: Int
        get() = who.size + ratio.size + lens.size + grade.size + money.size + time.size + will.size +
            (if (dispute) 1 else 0) + (if (todo) 1 else 0)

    val terms: List<String> get() = q.lowercase().jsSplitWs().filter { it.isNotEmpty() }
}

object Filter {
    /** fitsMe()：没勾「我的情况」或条目没标人群的始终显示；勾了 60岁 也算满足 35岁 */
    fun fitsMe(e: Entry, who: Set<String>): Boolean {
        if (who.isEmpty() || e.who.isEmpty()) return true
        fun has(v: String) = v in who || (v == "35岁" && "60岁" in who)
        return e.who.any { g -> g.all(::has) }
    }

    /** apply() 里除章节外的条件；章节单独判断，是为了算每节的命中数（网页侧栏也这么算） */
    fun matchesExceptSec(e: Entry, s: FilterState, terms: List<String> = s.terms): Boolean {
        if (s.ratio.isNotEmpty() && e.ratio !in s.ratio) return false
        if (s.lens.isNotEmpty() && e.lens !in s.lens) return false
        if (s.grade.isNotEmpty() && e.grade !in s.grade) return false
        if (s.money.isNotEmpty() && e.money !in s.money) return false
        if (s.time.isNotEmpty() && e.time !in s.time) return false
        if (s.will.isNotEmpty() && e.will !in s.will) return false
        if (!fitsMe(e, s.who)) return false
        if (s.dispute && !e.dispute) return false
        if (s.todo && !e.todo) return false
        if (terms.isNotEmpty() && !terms.all { it in e.hay }) return false
        return true
    }

    fun apply(entries: List<Entry>, s: FilterState): List<Entry> {
        val terms = s.terms
        return entries.filter { matchesExceptSec(it, s, terms) && (s.sec.isEmpty() || it.sec in s.sec) }
    }
}
