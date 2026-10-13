package app.lifeguide.feature.find

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lifeguide.core.corpus.Section
import app.lifeguide.core.corpus.key
import app.lifeguide.core.ui.AppCard
import app.lifeguide.core.ui.EntryCard
import app.lifeguide.core.ui.EntryText
import app.lifeguide.core.ui.Icons
import app.lifeguide.core.ui.Pill
import app.lifeguide.core.ui.SectionLabel
import app.lifeguide.core.ui.theme.LocalExtraColors
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun FindScreen(
    onOpenSection: (String) -> Unit,
    onOpenEntry: (String) -> Unit,
    vm: FindViewModel = koinViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    // 输入框的字只放在界面本地：经 StateFlow 绕一圈再回填，快速输入或拼音输入法组字时会丢字
    var text by rememberSaveable { mutableStateOf(vm.query.value) }
    // 只有外部清空（「清空」按钮）时才回填
    LaunchedEffect(query) { if (query.isEmpty() && text.isNotEmpty()) text = "" }
    val filter by vm.filter.collectAsStateWithLifecycle()
    var showFilter by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val listState = rememberLazyListState()
    // 换了关键词或条件就回到顶部
    LaunchedEffect(ui?.terms, filter) { listState.scrollToItem(0) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it; vm.setQuery(it) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("搜 ${ui?.total ?: ""} 条，比如：租房押金") },
                leadingIcon = { Icon(Icons.Search, null) },
                trailingIcon = if (text.isNotEmpty()) {
                    { IconButton(onClick = { text = ""; vm.setQuery("") }) { Icon(Icons.Close, "清空搜索") } }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    unfocusedBorderColor = LocalExtraColors.current.hairline,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            )
            val n = filter.activeCount
            IconButton(onClick = { showFilter = true }) {
                Icon(Icons.Tune, if (n > 0) "筛选，已选 $n 项" else "筛选", tint = if (n > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        val s = ui ?: run {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }

        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
            val results = s.results
            if (results != null) {
                item(key = "count") {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                        Text(
                            "找到 ${results.size} 条" + if (filter.activeCount > 0) "（已筛选 ${filter.activeCount} 项）" else "",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = vm::clearAll) { Text("清空") }
                    }
                }
                if (results.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            "没有匹配的条目。换个更短的词，或去掉一个筛选条件。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 32.dp),
                        )
                    }
                }
                items(results, key = { it.id }) { e ->
                    EntryCard(
                        e,
                        meta = EntryText.browseMeta(e) + " · " + EntryText.shortTitle(s.sectionTitles[e.sec].orEmpty()),
                        onClick = { onOpenEntry(e.id) },
                        terms = s.terms,
                        isNew = e.key in s.newKeys,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            } else {
                item(key = "situations-label") { SectionLabel("遇到事了") }
                // 一行横滑，目录不被挤到下面去
                item(key = "situations") {
                    LazyRow(
                        Modifier.fillParentMaxWidth().layout { m, c ->
                            // 横滑区域贴满屏幕两边，第一个入口仍和标题对齐
                            val extra = 16.dp.roundToPx() * 2
                            val p = m.measure(c.copy(maxWidth = c.maxWidth + extra))
                            layout(c.maxWidth, p.height) { p.place(-extra / 2, 0) }
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(s.situations, key = { it.first }) { (label, n) -> Pill(label, selected = false, onClick = { onOpenSection(n) }) }
                    }
                }
                for ((scene, sections) in s.scenes) {
                    item(key = "scene-$scene") { SectionLabel(scene, trailing = "${sections.sumOf { it.entries.size }} 条") }
                    item(key = "scene-list-$scene") { SceneCard(sections, onOpenSection) }
                }
            }
        }
    }

    if (showFilter) {
        FilterSheet(filter, shown = ui?.results?.size ?: ui?.total, onUpdate = vm::update, onDismiss = { showFilter = false })
    }
}

/** 一个场景下的各节，放在一张卡片里，每节一行 */
@Composable
private fun SceneCard(sections: List<Section>, onOpenSection: (String) -> Unit) {
    AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        Column {
            sections.forEachIndexed { i, sec ->
                if (i > 0) HorizontalDivider(Modifier.padding(start = 16.dp), thickness = 0.5.dp, color = LocalExtraColors.current.hairline)
                Row(
                    Modifier.fillMaxWidth().clickable { onOpenSection(sec.n) }.padding(start = 16.dp, end = 10.dp, top = 13.dp, bottom = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(sec.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1)
                    Text("${sec.entries.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(Icons.ChevronRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
