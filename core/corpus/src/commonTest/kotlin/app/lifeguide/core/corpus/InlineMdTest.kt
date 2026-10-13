package app.lifeguide.core.corpus

import kotlin.test.Test
import kotlin.test.assertEquals

class InlineMdTest {
    @Test
    fun boldLinkAndEscape() {
        assertEquals(
            listOf(
                Inline.Text("先 "), Inline.Bold("重点*"), Inline.Text(" 见 "),
                Inline.Link("第一节", "${Links.REPO}/blob/main/book/01-%E4%B8%8D.md"), Inline.Text(" 共 3*2"),
            ),
            InlineMd.parse("""先 **重点\*** 见 [第一节](../book/01-不.md) 共 3\*2"""),
        )
    }

    @Test
    fun anchorLinkGoesToSite() {
        assertEquals(listOf(Inline.Link("见上", Links.SITE + "#e-1-2")), InlineMd.parse("[见上](#e-1-2)"))
    }

    @Test
    fun bareUrlDropsTrailingChinesePunctuation() {
        assertEquals(
            listOf(Inline.Text("（见 "), Inline.Url("https://a.org/x"), Inline.Text("）。")),
            InlineMd.parse("（见 https://a.org/x）。"),
        )
    }

    @Test
    fun urlsLastMovesUrlsToEndAndDropsBareSeparators() {
        assertEquals(
            listOf(
                Inline.Text("某研究 "), Inline.Text("\n"), Inline.Url("https://a.org"), Inline.Text("\n"), Inline.Url("https://b.org"),
            ),
            InlineMd.parse("某研究 https://a.org 、 https://b.org", urlsLast = true),
        )
    }
}
