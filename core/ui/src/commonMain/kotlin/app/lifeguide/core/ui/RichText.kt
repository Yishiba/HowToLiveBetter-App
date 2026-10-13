package app.lifeguide.core.ui

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/** 术语在 AnnotatedString 里的标记，值是术语本身 */
const val TERM_TAG = "glossary-term"

/**
 * 能画术语虚线下划线的 Text。术语不改字色：它和引用（绿色实线下划线）、外链（绿色）都要分得开，
 * 而 Compose 的下划线只能和字同色，所以虚线自己画。
 */
@Composable
fun RichText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
) {
    val ranges = remember(text) { text.getStringAnnotations(TERM_TAG, 0, text.length) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text,
        modifier = if (ranges.isEmpty()) modifier else modifier.drawBehind {
            val l = layout ?: return@drawBehind
            val dash = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx()))
            for (r in ranges) {
                if (r.end > l.layoutInput.text.length) continue
                val first = l.getLineForOffset(r.start)
                val last = l.getLineForOffset(r.end - 1)
                // 术语跨行时每行各画一段
                for (line in first..last) {
                    val x0 = if (line == first) l.getBoundingBox(r.start).left else l.getLineLeft(line)
                    val x1 = if (line == last) l.getBoundingBox(r.end - 1).right else l.getLineRight(line)
                    val y = l.getLineBaseline(line) + 3.dp.toPx()
                    drawLine(lineColor, Offset(x0, y), Offset(x1, y), strokeWidth = 1.dp.toPx(), pathEffect = dash)
                }
            }
        },
        style = style,
        color = color,
        onTextLayout = { layout = it },
    )
}
