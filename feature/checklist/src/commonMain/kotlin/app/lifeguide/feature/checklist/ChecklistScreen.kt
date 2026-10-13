package app.lifeguide.feature.checklist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lifeguide.core.corpus.Entry
import app.lifeguide.core.corpus.key
import app.lifeguide.core.data.ChecklistItem
import app.lifeguide.core.data.RecentChanges
import app.lifeguide.core.ui.AppCard
import app.lifeguide.core.ui.CheckMark
import app.lifeguide.core.ui.EntryCard
import app.lifeguide.core.ui.EntryText
import app.lifeguide.core.ui.Icons
import app.lifeguide.core.ui.Pill
import app.lifeguide.core.ui.SectionLabel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun ChecklistScreen(
    onOpenEntry: (String) -> Unit,
    onOpenSettings: () -> Unit,
    vm: ChecklistViewModel = koinViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var showChanges by rememberSaveable { mutableStateOf(false) }
    val s = ui ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    @Composable
    fun Item(item: ChecklistItem) = EntryCard(
        entry = item.entry,
        meta = EntryText.checklistMeta(item.entry, s.sectionTitles[item.entry.sec]),
        onClick = { onOpenEntry(item.entry.id) },
        done = item.done,
        isNew = item.entry.key in s.newKeys,
        leading = { CheckMark(item.done, onToggle = { vm.setDone(item.entry, !item.done) }) },
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "header") {
            Column(Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("先做这些", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Settings, "关于与设置", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                Text(
                    "不花钱、顺手、不用毅力，收益大，证据 A 级",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item(key = "progress") { Progress(s.done, s.total, Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) }
        s.changes?.let { c ->
            item(key = "changes") {
                ChangesBanner(
                    text = buildList {
                        if (c.changes.added.isNotEmpty()) add("新增 ${c.changes.added.size} 条")
                        if (c.changes.changed.isNotEmpty()) add("改动 ${c.changes.changed.size} 条")
                    }.joinToString("，"),
                    onOpen = { showChanges = true },
                    onDismiss = vm::dismissChanges,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                )
            }
        }
        item(key = "lenses") {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Pill("全部", s.selected == null, onClick = { vm.select(null) }) }
                items(s.lenses, key = { it.first.name }) { (lens, n) ->
                    Pill("${lens.tab} $n", s.selected == lens, onClick = { vm.select(lens) })
                }
            }
        }
        item(key = "gap") { Box(Modifier.height(8.dp)) }
        items(s.core, key = { "c-" + it.entry.key }) { Item(it) }
        if (s.added.isNotEmpty()) {
            item(key = "added-label") { SectionLabel("我加的", Modifier.padding(horizontal = 20.dp), trailing = "${s.added.size} 条") }
            items(s.added, key = { "a-" + it.entry.key }) { Item(it) }
        }
    }

    if (showChanges) {
        s.changes?.let { c ->
            val entries = vm.entriesFor(c.changes.added + c.changes.changed)
            ChangesSheet(c, entries, onOpenEntry = { showChanges = false; onOpenEntry(it) }, onDismiss = { showChanges = false })
        }
    }
}

@Composable
private fun Progress(done: Int, total: Int, modifier: Modifier = Modifier) {
    AppCard(modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("已经做到", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text("$done", style = MaterialTheme.typography.titleLarge)
                Text(" / $total", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else done.toFloat() / total },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                drawStopIndicator = {},
                gapSize = 0.dp,
            )
        }
    }
}

@Composable
private fun ChangesBanner(text: String, onOpen: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onOpen)
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("正文更新：$text", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        TextButton(onClick = onDismiss) { Text("知道了") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangesSheet(
    changes: RecentChanges,
    titles: List<Entry>,
    onOpenEntry: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("${changes.commitDate.take(10)} 的更新", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp)) }
            items(titles, key = { it.key }) { e ->
                val isAdded = e.key in changes.changes.added
                EntryCard(e, meta = (if (isAdded) "新增" else "改动") + " · " + EntryText.browseMeta(e), onClick = { onOpenEntry(e.id) })
            }
        }
    }
}
