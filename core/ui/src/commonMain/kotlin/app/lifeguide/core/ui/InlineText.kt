package app.lifeguide.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import app.lifeguide.core.corpus.Inline
import app.lifeguide.core.corpus.InlineMd
import app.lifeguide.core.corpus.XrefContext

/** 把正文字段渲染成带链接、加粗、关键词高亮的 [AnnotatedString]。切分规则在 [InlineMd]，这里只管样式。 */
object InlineText {
    /**
     * @param xref 给了就把「见第 X 节第 Y 条」渲染成可点的引用，点了调 [onXref]
     */
    fun render(
        text: String,
        terms: List<String> = emptyList(),
        link: SpanStyle,
        mark: SpanStyle,
        urlsLast: Boolean = false,
        xref: XrefContext? = null,
        onXref: (Inline.Xref) -> Unit = {},
    ): AnnotatedString {
        val base = buildAnnotatedString {
            for (part in InlineMd.parse(text, urlsLast, xref)) {
                when (part) {
                    is Inline.Text -> append(part.text)
                    is Inline.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(part.text) }
                    is Inline.Link -> withLink(LinkAnnotation.Url(part.href, TextLinkStyles(link))) { append(part.label) }
                    is Inline.Url -> withLink(LinkAnnotation.Url(part.url, TextLinkStyles(link))) {
                        append(part.url.removePrefix("https://").removePrefix("http://").removeSuffix("/"))
                    }
                    // 引用和外链区分开：引用带下划线，点了在 App 里跳转，外链不带、点了开浏览器
                    is Inline.Xref -> withLink(
                        LinkAnnotation.Clickable(
                            tag = part.text,
                            styles = TextLinkStyles(link.copy(textDecoration = TextDecoration.Underline)),
                            linkInteractionListener = { onXref(part) },
                        ),
                    ) { append(part.text) }
                }
            }
        }
        return highlight(base, terms, mark)
    }

    /** 关键词（已小写）不分大小写地标出来；小写后长度变了的文字（极少数字符）不标，免得错位 */
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

/**
 * 组合函数里用：按当前主题取链接和高亮样式，文字或关键词变了才重新渲染
 * @param xref 给了就把引用渲染成可点的；[onXref] 用最新的那个，不因回调换了实例就重算整段
 */
@Composable
fun rememberInlineText(
    text: String,
    terms: List<String> = emptyList(),
    urlsLast: Boolean = false,
    xref: XrefContext? = null,
    onXref: (Inline.Xref) -> Unit = {},
): AnnotatedString {
    val link = linkStyle()
    val mark = markStyle()
    val callback = rememberUpdatedState(onXref)
    return remember(text, terms, link, mark, urlsLast, xref) {
        InlineText.render(text, terms, link, mark, urlsLast, xref) { callback.value(it) }
    }
}
