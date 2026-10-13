package app.lifeguide.core.corpus

// 上游的规则都写在 JS 里。JS 的 \s 和 trim() 认的空白和 Kotlin/Java 不一样
// （Java 正则的 \s 只认 ASCII；Kotlin 的 trim() 不认 ﻿、却认 \u001C–\u001F），
// 照搬会在含全角空格、不换行空格的行上静默偏离。这里统一按 ECMAScript 的 WhiteSpace + LineTerminator。

internal const val JS_WS = "\\t\\n\\u000B\\f\\r \\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF"

internal fun Char.isJsWhitespace(): Boolean = when (this) {
    '\t', '\n', '\u000B', '\u000C', '\r', ' ', ' ', ' ',
    ' ', ' ', ' ', ' ', '　', '﻿' -> true
    else -> this in ' '..' '
}

internal fun String.jsTrim(): String = trim { it.isJsWhitespace() }
internal fun String.jsTrimEnd(): String = trimEnd { it.isJsWhitespace() }

private val JS_WS_RUN = Regex("[$JS_WS]+")

/** `s.split(/\s+/)`：和 JS 一样保留首尾的空串 */
internal fun String.jsSplitWs(): List<String> = split(JS_WS_RUN)
