package app.lifeguide.corpus

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import app.lifeguide.Links

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
    private const val REPO_BLOB = "${Links.REPO}/blob/main/"

    fun unescape(s: String) = s.replace(ESC, "$1")

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
        Regex("^(https?:|mailto:)").containsMatchIn(p) -> p
        else -> REPO_BLOB + app.lifeguide.data.CorpusUpdater.encodePath(p.replace(Regex("""^(\.\.?/)+"""), ""))
    }

    /**
     * @param urlsLast 来源栏用：裸链接攒到末尾各占一行，正文读起来不被长链接打断
     */
    fun render(
        text: String,
        terms: List<String> = emptyList(),
        link: SpanStyle,
        mark: SpanStyle,
        urlsLast: Boolean = false,
    ): AnnotatedString {
        val held = mutableListOf<String>()
        var justHeld = false
        val base = buildAnnotatedString {
            fun pushText(s: String) {
                if (s.isEmpty()) return
                if (justHeld && SEPARATOR_ONLY.matches(s)) return
                justHeld = false
                append(unescape(s))
            }
            fun url(u: String) = withLink(LinkAnnotation.Url(u, TextLinkStyles(link))) {
                append(u.removePrefix("https://").removePrefix("http://").removeSuffix("/"))
            }
            var last = 0
            for (m in LINK_RE.findAll(text)) {
                pushText(text.substring(last, m.range.first))
                last = m.range.last + 1
                val g = m.groups
                when {
                    g[4] != null -> { withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(unescape(g[4]!!.value)) }; justHeld = false }
                    g[3] != null -> {
                        val (u, tail) = trimUrl(g[3]!!.value)
                        if (urlsLast) { held += u; justHeld = true } else url(u)
                        pushText(tail)
                    }
                    else -> {
                        withLink(LinkAnnotation.Url(hrefOf(g[2]!!.value), TextLinkStyles(link))) { append(g[1]!!.value) }
                        justHeld = false
                    }
                }
            }
            pushText(text.substring(last))
            for (u in held) { append('\n'); url(u) }
        }
        return highlight(base, terms, mark)
    }

    fun highlight(s: AnnotatedString, terms: List<String>, mark: SpanStyle): AnnotatedString {
        if (terms.isEmpty()) return s
        val lower = s.text.lowercase()
        if (lower.length != s.text.length) return s
        return AnnotatedString.Builder(s).apply {
            for (t in terms) {
                var i = lower.indexOf(t)
                while (i >= 0) { addStyle(mark, i, i + t.length); i = lower.indexOf(t, i + t.length) }
            }
        }.toAnnotatedString()
    }
}
