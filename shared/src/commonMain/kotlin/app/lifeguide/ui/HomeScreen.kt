package app.lifeguide.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lifeguide.corpus.Entry
import app.lifeguide.corpus.InlineMd
import app.lifeguide.corpus.Section

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(vm: MainViewModel, snackbar: SnackbarHostState, onOpen: (String) -> Unit, onAbout: () -> Unit) {
    val list by vm.list.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val corpus by vm.corpus.collectAsStateWithLifecycle()
    var showFilter by rememberSaveable { mutableStateOf(false) }
    var showSections by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val focus = LocalFocusManager.current

    // 筛选或关键词变了才回到顶部；从详情页返回时不动滚动位置
    var appliedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val key = list?.let { "${filter.copy(q = "")}|${it.terms}" }
    LaunchedEffect(key) {
        if (key != null && appliedKey != null && key != appliedKey) listState.scrollToItem(0)
        if (key != null) appliedKey = key
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("高性价比人生指南", fontWeight = FontWeight.SemiBold) },
                actions = { IconButton(onClick = onAbout) { Icon(Icons.Info, "关于与设置") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text("搜索条目，多个词用空格隔开") },
                leadingIcon = { Icon(Icons.Search, null) },
                trailingIcon = if (query.isNotEmpty()) {
                    { IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Close, "清空搜索") } }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { focus.clearFocus() }),
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val secTitle = filter.sec.firstOrNull()?.let { n -> corpus?.corpus?.sectionByN?.get(n)?.let { "$n. ${it.title}" } }
                FilterChip(
                    selected = secTitle != null,
                    onClick = { showSections = true },
                    label = { Text(secTitle ?: "全部章节", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    trailingIcon = { Icon(Icons.ExpandMore, null, Modifier.size(18.dp)) },
                    modifier = Modifier.widthIn(max = 220.dp),
                )
                val n = filter.activeCount
                FilterChip(
                    selected = n > 0,
                    onClick = { showFilter = true },
                    label = { Text(if (n > 0) "筛选 · $n" else "筛选") },
                    leadingIcon = { Icon(Icons.Tune, null, Modifier.size(18.dp)) },
                )
                Text(
                    list?.let { "${it.shown}/${it.total}" }.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            val ui = list
            if (ui == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }
            val idle = filter.activeCount == 0 && filter.sec.isEmpty() && ui.terms.isEmpty()
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
                if (idle) item(key = "intro") { BookIntro() }
                if (ui.shown == 0) item(key = "empty") { EmptyState(onClear = vm::clearAll) }
                for (g in ui.groups) {
                    stickyHeader(key = "h-${g.section.n}") { SectionHeader(g.section, g.entries.size) }
                    if (ui.terms.isEmpty() && g.section.intro.isNotEmpty()) {
                        item(key = "i-${g.section.n}") { SectionIntro(g.section) }
                    }
                    items(g.entries, key = { it.id }) { e -> EntryCard(e, ui.terms) { onOpen(e.id) } }
                }
            }
        }
    }

    if (showFilter) FilterSheet(vm, onDismiss = { showFilter = false })
    if (showSections) SectionSheet(vm, onDismiss = { showSections = false })
}

@Composable
private fun BookIntro() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("每一条都回答两个问题：花掉什么，换回什么。", style = MaterialTheme.typography.titleSmall)
        Text(
            "不用全做：这是一份按性价比排好的备选单，不是任务清单。挑走一两条就算数，剩下的需要时再回来查。" +
                "想挑省力的，在「筛选」里把花钱选「不花钱」、要毅力选「不用」。",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "内容可能过时，也可能有错。不少政策各地执行不一样。真去办事、看病、打官司时，以当地部门、医生和律师的说法为准。",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionHeader(s: Section, count: Int) {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${s.n}. ${s.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text("$count 条", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionIntro(s: Section) {
    var open by rememberSaveable(s.n) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            if (open) "收起本节导读" else "本节导读",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { open = !open }.padding(vertical = 4.dp),
        )
        if (open) {
            val link = linkStyle(); val mark = markStyle()
            for (p in s.intro) {
                Text(
                    InlineMd.render(p, link = link, mark = mark),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 3.dp),
                )
            }
        }
    }
}

@Composable
fun EntryCard(e: Entry, terms: List<String>, onClick: () -> Unit) {
    val link = linkStyle(); val mark = markStyle()
    val title = remember(e.id, terms) { InlineMd.render("${e.n}. ${e.title}", terms, link, mark) }
    val human = remember(e.id, terms) { InlineMd.render(e.human, terms, link, mark) }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, lineHeight = 23.sp), fontWeight = FontWeight.SemiBold)
            if (e.human.isNotEmpty()) {
                Text(human, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
            EntryBadges(e, withCosts = false)
        }
    }
}

@Composable
private fun EmptyState(onClear: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("没有匹配的条目。去掉一个筛选条件，或换个更短的关键词。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = onClear) { Text("清空筛选") }
    }
}
