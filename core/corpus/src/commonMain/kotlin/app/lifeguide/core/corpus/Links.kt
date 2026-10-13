package app.lifeguide.core.corpus

/** 上游仓库与网页的地址。正文、网页锚点、联网更新都以这里为准。 */
object Links {
    const val OWNER = "eternity4719"
    const val REPO_NAME = "HowToLiveBetter"
    const val REPO = "https://github.com/$OWNER/$REPO_NAME"
    const val SITE = "https://$OWNER.github.io/$REPO_NAME/"
    const val ISSUES = "$REPO/issues"
    const val LICENSE = "https://creativecommons.org/licenses/by/4.0/deed.zh-hans"

    fun entryOnSite(sec: String, n: String) = "${SITE}#e-$sec-$n"

    /** 按 UTF-8 百分号编码每一段路径（文件名是中文） */
    fun encodePath(path: String): String = path.split('/').joinToString("/") { seg ->
        buildString {
            for (b in seg.encodeToByteArray()) {
                val c = b.toInt() and 0xFF
                val ch = c.toChar()
                if (ch.isLetterOrDigit() && c < 0x80 || ch in "-._~") append(ch)
                else append('%').append(HEX[c shr 4]).append(HEX[c and 0xF])
            }
        }
    }

    private const val HEX = "0123456789ABCDEF"
}
