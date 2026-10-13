package app.lifeguide.feature.browse

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lifeguide.core.corpus.FilterState
import app.lifeguide.core.ui.Labels

private fun FilterState.dim(d: String): Set<String> = when (d) {
    "who" -> who; "ratio" -> ratio; "lens" -> lens; "grade" -> grade
    "money" -> money; "time" -> time; "will" -> will; else -> emptySet()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun FilterSheet(vm: BrowseViewModel, onDismiss: () -> Unit) {
    val f by vm.filter.collectAsStateWithLifecycle()
    val list by vm.list.collectAsStateWithLifecycle()
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
                        FilterChip(selected = o.value in sel, onClick = { vm.toggle(g.dim, o.value) }, label = { Text(o.label) })
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            ToggleRow("只看标了争议的", f.dispute) { v -> vm.update { it.copy(dispute = v) } }
            ToggleRow("只看有待核实的", f.todo) { v -> vm.update { it.copy(todo = v) } }
            Text(
                "同一组多选是「或」，不同组之间是「且」。「我的情况」勾的是你符合哪几样，没标人群的条目始终显示。每节内按性价比从高到低排。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 10.dp),
            )
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { vm.update { FilterState(sec = it.sec) } }, modifier = Modifier.weight(1f)) { Text("清空筛选") }
                Button(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(list?.let { "看 ${it.shown} 条" } ?: "完成")
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SectionSheet(vm: BrowseViewModel, onDismiss: () -> Unit) {
    val f by vm.filter.collectAsStateWithLifecycle()
    val list by vm.list.collectAsStateWithLifecycle()
    val corpus by vm.corpus.collectAsStateWithLifecycle()
    val sections = corpus?.corpus?.sections ?: return
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.navigationBarsPadding()) {
            item {
                SectionRow("全部章节", list?.matchedAllSections, f.sec.isEmpty()) { vm.update { it.copy(sec = emptySet()) }; onDismiss() }
            }
            items(sections, key = { it.n }) { s ->
                SectionRow("${s.n}. ${s.title}", list?.perSection?.get(s.n), s.n in f.sec) {
                    vm.update { it.copy(sec = setOf(s.n)) }; onDismiss()
                }
            }
        }
    }
}

@Composable
private fun SectionRow(title: String, count: Int?, selected: Boolean, onClick: () -> Unit) {
    val color = when {
        selected -> MaterialTheme.colorScheme.primary
        count == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = color, fontWeight = if (selected) FontWeight.SemiBold else null, modifier = Modifier.weight(1f))
        count?.let { Text("$it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
