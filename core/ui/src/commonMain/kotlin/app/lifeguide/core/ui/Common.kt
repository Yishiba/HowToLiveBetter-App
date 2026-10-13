package app.lifeguide.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.lifeguide.core.corpus.Entry
import app.lifeguide.core.ui.theme.LocalExtraColors

@Composable
fun linkStyle() = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.None)

@Composable
fun markStyle() = SpanStyle(background = LocalExtraColors.current.mark)

/** 白底、细边、12dp 圆角的卡片，全 App 只有这一种 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val border = BorderStroke(0.5.dp, LocalExtraColors.current.hairline)
    val color = MaterialTheme.colorScheme.surfaceContainerLowest
    val inner: @Composable () -> Unit = { Box(Modifier.padding(contentPadding)) { content() } }
    if (onClick != null) Surface(onClick, modifier, shape = shape, color = color, border = border, content = inner)
    else Surface(modifier, shape = shape, color = color, border = border, content = inner)
}

/** 清单里的勾：空心圆 → 实心绿圆带勾 */
@Composable
fun CheckMark(checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val c = MaterialTheme.colorScheme
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .then(if (checked) Modifier.background(c.primary) else Modifier.border(1.5.dp, c.outline, CircleShape))
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .semantics { contentDescription = if (checked) "已做到，点一下取消" else "标记为做到了" },
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(Icons.Check, null, Modifier.size(size * 0.7f), tint = c.onPrimary)
    }
}

/** 一排可选的小药丸：分段、快捷开关都用它 */
@Composable
fun Pill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) c.onPrimaryContainer else c.onSurfaceVariant,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) c.primaryContainer else c.surfaceContainerLowest)
            .border(0.5.dp, if (selected) c.primaryContainer else LocalExtraColors.current.hairline, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

/**
 * 条目卡片：标题（两行）、说人话（两行）、一行灰色小字。全 App 的条目列表都用它。
 * @param meta 底部小字，例如「换寿命 · 不要早死」；有争议时自动在末尾加红色「有争议」
 * @param leading 清单里放 [CheckMark]
 * @param isNew 最近一次更新新增或改动的条目，标题前加小圆点
 */
@Composable
fun EntryCard(
    entry: Entry,
    meta: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    terms: List<String> = emptyList(),
    done: Boolean = false,
    isNew: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    val c = MaterialTheme.colorScheme
    val danger = LocalExtraColors.current.danger
    AppCard(modifier.fillMaxWidth(), onClick = onClick) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            leading?.let { Box(Modifier.padding(top = 1.dp)) { it() } }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isNew) Box(Modifier.padding(end = 6.dp).size(7.dp).clip(CircleShape).background(c.primary))
                    Text(
                        rememberInlineText(entry.title, terms),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (done) c.onSurfaceVariant else c.onSurface,
                        textDecoration = if (done) TextDecoration.LineThrough else null,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!done && entry.human.isNotEmpty()) {
                    Text(
                        rememberInlineText(entry.human, terms),
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    buildAnnotatedString {
                        append(meta)
                        if (entry.dispute) withStyle(SpanStyle(color = danger)) { append(" · 有争议") }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = c.onSurfaceVariant.copy(alpha = 0.85f),
                )
            }
        }
    }
}

/** 列表里的小标题，例如场景名、清单分组 */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.Bottom) {
        Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        trailing?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
