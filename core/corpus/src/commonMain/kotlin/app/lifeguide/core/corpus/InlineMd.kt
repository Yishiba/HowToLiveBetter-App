package app.lifeguide.core.corpus

/** 正文字段里一段行内 Markdown 拆出来的片段，由界面层渲染 */
sealed interface Inline {
    /** 普通文字，转义已还原 */
    data class Text(val text: String) : Inline
    data class Bold(val text: String) : Inline
    /** [文字](地址)，[href] 已换成可直接打开的绝对地址 */
    data class Link(val label: String, val href: String) : Inline
    /** 裸链接或 <链接> */
    data class Url(val url: String) : Inline
    /** 交叉引用，见 [Xref] */
    data class Xref(val text: String, val targets: List<XrefTarget>) : Inline
}

/**
 * 正文字段里的行内 Markdown：[文字](地址)、裸链接或 <链接>、**加粗**、\* 转义。
 * 规则移植自上游 index.html 的 LINK_RE / trimUrl / hrefOf / renderText（术语表和交叉引用这一版不做）。
 */
object InlineMd {
    private val LINK_RE = Regex(
        """\[([^\]\n]+)\]\(([^)$JS_WS]+)\)""" +
            """|<?(https?://[A-Za-z0-9\-._~:/?#\[\]@!${'$'}&'()*+,=%]+)>?""" +
            """|\*\*((?:\\\*|[^*\n])+)\*\*""",
    )
    private val ESC = Regex("""\\([*_])""")
    private val SEPARATOR_ONLY = Regex("^[$JS_WS、,，]+$")
    private val ABSOLUTE = Regex("^(https?:|mailto:)")
    private val RELATIVE_PREFIX = Regex("""^(\.\.?/)+""")
    private const val REPO_BLOB = "${Links.REPO}/blob/main/"

    fun unescape(s: String) = s.replace(ESC, "$1")

    /** 去掉 Markdown 记号的纯文字，分享、复制时用 */
    fun plain(s: String) = unescape(s.replace("**", ""))

    /** 链接末尾的中文标点、不成对的右括号不算链接 */
    fun trimUrl(url: String): Pair<String, String> {
        var u = url
        var tail = ""
        while (u.isNotEmpty()) {
            val ch = u.last()
            val unbalanced = ch == ')' && u.count { it == '(' } < u.count { it == ')' }
            if (ch !in "）。,，" && !unbalanced) break
            tail = ch + tail
            u = u.dropLast(1)
        }
        return u to tail
    }

    fun hrefOf(p: String): String = when {
        p.startsWith("#") -> Links.SITE + p
        ABSOLUTE.containsMatchIn(p) -> p
        else -> REPO_BLOB + Links.encodePath(p.replace(RELATIVE_PREFIX, ""))
    }

    /**
     * @param urlsLast 来源栏用：裸链接攒到末尾各占一行，正文读起来不被长链接打断；
     *   被攒走的链接两侧只剩分隔符（顿号、逗号、空白）时，分隔符一并去掉
     * @param xref 给了就把普通文字里的「第 X 条」「第 X 节」切成 [Inline.Xref]；链接文字和加粗里的不认，同网页
     */
    fun parse(text: String, urlsLast: Boolean = false, xref: XrefContext? = null): List<Inline> {
        val out = mutableListOf<Inline>()
        val held = mutableListOf<String>()
        var justHeld = false
        fun pushText(s: String) {
            if (s.isEmpty()) return
            if (justHeld && SEPARATOR_ONLY.matches(s)) return
            justHeld = false
            val plain = unescape(s)
            if (xref == null) out += Inline.Text(plain) else out += Xref.split(plain, xref)
        }
        var last = 0
        for (m in LINK_RE.findAll(text)) {
            pushText(text.substring(last, m.range.first))
            last = m.range.last + 1
            val g = m.groups
            when {
                g[4] != null -> { out += Inline.Bold(unescape(g[4]!!.value)); justHeld = false }
                g[3] != null -> {
                    val (u, tail) = trimUrl(g[3]!!.value)
                    if (urlsLast) { held += u; justHeld = true } else out += Inline.Url(u)
                    pushText(tail)
                }
                else -> { out += Inline.Link(g[1]!!.value, hrefOf(g[2]!!.value)); justHeld = false }
            }
        }
        pushText(text.substring(last))
        for (u in held) { out += Inline.Text("\n"); out += Inline.Url(u) }
        return out
    }
}
