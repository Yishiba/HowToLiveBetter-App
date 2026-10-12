package app.lifeguide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lifeguide.corpus.Entry
import app.lifeguide.ui.theme.LocalTones

@Composable
fun linkStyle() = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.None)

@Composable
fun markStyle() = SpanStyle(background = LocalTones.current.mark)

@Composable
fun Badge(text: String, fg: Color, bg: Color) {
    Text(
        text,
        color = fg,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.background(bg, RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** 一条的徽章：性价比、证据等级、口径、三项成本、人群、争议、待核实 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryBadges(e: Entry, modifier: Modifier = Modifier, withCosts: Boolean = true) {
    val t = LocalTones.current
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        when (e.ratio) {
            "极高" -> Badge("性价比极高", t.brand, t.brandSoft)
            "高" -> Badge("性价比高", t.green, t.greenSoft)
            else -> Badge("性价比一般", t.gray, t.graySoft)
        }
        when (e.grade) {
            "A" -> Badge("A 级证据", t.green, t.greenSoft)
            "B" -> Badge("B 级证据", t.yellow, t.yellowSoft)
            "C" -> Badge("C 级证据", t.gray, t.graySoft)
        }
        Labels.LENS[e.lens]?.let { Badge(it, t.gray, t.graySoft) }
        if (withCosts) {
            listOfNotNull(Labels.MONEY[e.money], Labels.TIME[e.time], Labels.WILL[e.will]).forEach { Badge(it, t.gray, t.graySoft) }
        }
        if (e.who.isNotEmpty()) Badge("适用：" + Labels.who(e.who), t.brand, t.brandSoft)
        if (e.dispute) Badge("有争议", t.red, t.redSoft)
        if (e.todo) Badge("有待核实", t.yellow, t.yellowSoft)
    }
}
