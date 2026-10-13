package app.lifeguide.feature.find

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lifeguide.core.corpus.FilterState
import app.lifeguide.core.ui.Labels

private fun FilterState.dim(d: String): Set<String> = when (d) {
    "who" -> who; "ratio" -> ratio; "lens" -> lens; "grade" -> grade
    "money" -> money; "time" -> time; "will" -> will; else -> emptySet()
}

/**
 * 高级筛选：和网页同样的 10 组条件。默认收起，只在搜索结果和章节页里通过「筛选」按钮打开。
 * @param shown 当前条件下的条数，显示在「看 n 条」按钮上
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun FilterSheet(
    f: FilterState,
    shown: Int?,
    onUpdate: ((FilterState) -> FilterState) -> Unit,
    onDismiss: () -> Unit,
) {
    fun toggle(dim: String, v: String) = onUpdate { s ->
        fun Set<String>.flip() = if (v in this) this - v else this + v
        when (dim) {
            "who" -> s.copy(who = s.who.flip())
            "ratio" -> s.copy(ratio = s.ratio.flip())
            "lens" -> s.copy(lens = s.lens.flip())
            "grade" -> s.copy(grade = s.grade.flip())
            "money" -> s.copy(money = s.money.flip())
            "time" -> s.copy(time = s.time.flip())
            "will" -> s.copy(will = s.will.flip())
            else -> s
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding()) {
            for (g in Labels.GROUPS) {
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)) {
                    Text(g.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    g.hint?.let { Text("  $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val sel = f.dim(g.dim)
                    for (o in g.options) {
                        FilterChip(selected = o.value in sel, onClick = { toggle(g.dim, o.value) }, label = { Text(o.label) })
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            ToggleRow("只看标了争议的", f.dispute) { v -> onUpdate { it.copy(dispute = v) } }
            ToggleRow("只看有待核实的", f.todo) { v -> onUpdate { it.copy(todo = v) } }
            Text(
                "同一组多选是「或」，不同组之间是「且」。「我的情况」勾的是你符合哪几样，没标人群的条目始终显示。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 10.dp),
            )
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onUpdate { FilterState(q = it.q, sec = it.sec) } }, modifier = Modifier.weight(1f)) { Text("清空筛选") }
                Button(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(shown?.let { "看 $it 条" } ?: "完成") }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
