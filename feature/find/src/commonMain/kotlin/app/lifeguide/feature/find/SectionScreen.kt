package app.lifeguide.feature.find

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lifeguide.core.corpus.key
import app.lifeguide.core.ui.AppCard
import app.lifeguide.core.ui.EntryCard
import app.lifeguide.core.ui.EntryText
import app.lifeguide.core.ui.Icons
import app.lifeguide.core.ui.Pill
import app.lifeguide.core.ui.rememberInlineText
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SectionScreen(
    n: String,
    onOpenEntry: (String) -> Unit,
    onBack: () -> Unit,
    vm: SectionViewModel = koinViewModel(key = "section-$n") { parametersOf(n) },
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    var showFilter by rememberSaveable { mutableStateOf(false) }
    var introOpen by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Back, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        val s = ui ?: return@Scaffold
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
            item(key = "title") {
                Column(Modifier.padding(horizontal = 4.dp)) {
                    Text(s.section.title, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "${s.section.entries.size} 条 · 按性价比从高到低",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            if (s.section.intro.isNotEmpty()) {
                item(key = "intro") {
                    AppCard(Modifier.fillMaxWidth().padding(top = 14.dp), onClick = { introOpen = !introOpen }) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row {
                                Text("本节导读", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                Icon(if (introOpen) Icons.ExpandLess else Icons.ExpandMore, if (introOpen) "收起" else "展开", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            val paras = if (introOpen) s.section.intro else s.section.intro.take(1)
                            for (p in paras) {
                                Text(
                                    rememberInlineText(p),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = if (introOpen) Int.MAX_VALUE else 2,
                                )
                            }
                        }
                    }
                }
            }
            item(key = "toggles") {
                Row(Modifier.padding(top = 14.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("只看零成本", filter.zeroCost, onClick = { vm.setZeroCost(!filter.zeroCost) })
                    Pill("只看 A 级", filter.gradeAOnly, onClick = { vm.setGradeA(!filter.gradeAOnly) })
                    // 角标只数快捷开关以外的条件
                    val extra = filter.activeCount - (if (filter.zeroCost) 3 else 0) - (if (filter.gradeAOnly) 1 else 0)
                    Pill(if (extra > 0) "筛选 · $extra" else "筛选", extra > 0, onClick = { showFilter = true })
                }
            }
            if (s.entries.isEmpty()) {
                item(key = "empty") {
                    Text(
                        "这一节没有符合条件的条目。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp).clickable { vm.update { it.copy(money = emptySet(), time = emptySet(), will = emptySet(), grade = emptySet()) } },
                    )
                }
            }
            items(s.entries, key = { it.id }) { e ->
                EntryCard(
                    e,
                    meta = EntryText.browseMeta(e),
                    onClick = { onOpenEntry(e.id) },
                    isNew = e.key in s.newKeys,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
    }

    if (showFilter) FilterSheet(filter, shown = ui?.entries?.size, onUpdate = vm::update, onDismiss = { showFilter = false })
}
