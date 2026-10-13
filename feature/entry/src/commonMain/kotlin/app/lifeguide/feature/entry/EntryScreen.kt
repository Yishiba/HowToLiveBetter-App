package app.lifeguide.feature.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lifeguide.core.corpus.Entry
import app.lifeguide.core.corpus.GlossaryTerm
import app.lifeguide.core.corpus.Links
import app.lifeguide.core.corpus.SplitRules
import app.lifeguide.core.ui.AppCard
import app.lifeguide.core.ui.EntryText
import app.lifeguide.core.ui.Icons
import app.lifeguide.core.ui.Labels
import app.lifeguide.core.ui.RichText
import app.lifeguide.core.ui.TermSheet
import app.lifeguide.core.ui.XrefSheet
import app.lifeguide.core.ui.rememberInlineText
import app.lifeguide.core.ui.rememberXrefNavigator
import app.lifeguide.core.ui.theme.LocalExtraColors
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EntryScreen(
    id: String,
    onOpenEntry: (String) -> Unit,
    onOpenSection: (String) -> Unit,
    onOpenGlossary: () -> Unit,
    onBack: () -> Unit,
    vm: EntryViewModel = koinViewModel(key = id) { parametersOf(id) },
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val s = ui as? EntryUi.Ready
    val uri = LocalUriHandler.current
    val xrefNav = rememberXrefNavigator(onOpenEntry, onOpenSection)
    var term by remember { mutableStateOf<GlossaryTerm?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Back, "返回") } },
                actions = {
                    if (s != null) {
                        IconButton(onClick = { uri.openUri(Links.entryOnSite(s.entry.sec, s.entry.n)) }) { Icon(Icons.OpenInNew, "在网页中打开") }
                        IconButton(onClick = { vm.share(s.entry) }) { Icon(Icons.Share, "分享") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = { if (s != null) PrimaryBar(s, onPrimary = { vm.primaryAction(s) }, onRemove = { vm.removeFromList(s.entry) }) },
    ) { pad ->
        if (ui == EntryUi.Missing) {
            Text("这一条在当前版本的正文里找不到了，可能被合并或删掉了。", Modifier.padding(pad).padding(24.dp))
            return@Scaffold
        }
        if (s == null) return@Scaffold
        val e = s.entry
        // 正文各栏都认交叉引用和术语，同网页 renderCard
        @Composable fun inline(t: String, urlsLast: Boolean = false) =
            rememberInlineText(t, s.terms, urlsLast, s.xref, xrefNav::open, s.xref.corpus.glossary) { term = it }
        @Composable fun Para(t: String) = RichText(inline(t), style = MaterialTheme.typography.bodyMedium)
        SelectionContainer(Modifier.padding(pad).fillMaxSize()) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                s.section?.let { sec ->
                    Text(
                        "${EntryText.shortTitle(sec.title)} · 第 ${e.n} 条",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onOpenSection(sec.n) },
                    )
                }
                RichText(inline(e.title), style = MaterialTheme.typography.headlineSmall)
                if (e.who.isNotEmpty()) {
                    Text("适用：" + Labels.who(e.who), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }

                if (e.human.isNotEmpty()) {
                    RichText(
                        inline(e.human),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                    )
                }
                Overview(e)

                Fold("成本", initiallyOpen = false) { Para(e.cost) }
                Fold("收益", initiallyOpen = true) { SplitRules.splitGain(e.gain).forEach { Para(it) } }
                if (e.note.isNotEmpty()) {
                    Fold(
                        if (e.dispute) "备注 · 有争议" else "备注",
                        initiallyOpen = e.dispute,
                        color = if (e.dispute) LocalExtraColors.current.danger else null,
                    ) { SplitRules.splitGain(e.note).forEach { Para(it) } }
                }
                val sources = SplitRules.splitSrc(e.src)
                Fold("来源", hint = "${sources.size} 条文献", initiallyOpen = false) {
                    sources.forEachIndexed { i, p ->
                        Row {
                            if (sources.size > 1) Text("${i + 1}. ", style = MaterialTheme.typography.bodySmall)
                            RichText(inline(p, urlsLast = true), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Text(
                    "数字的口径以条目内标注为准，不同口径之间不做比较。内容可能过时或有错，办事、看病、打官司以当地部门、医生和律师的说法为准。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
    s?.let { XrefSheet(xrefNav, it.xref.corpus) }
    term?.let { t -> TermSheet(t, onDismiss = { term = null }, onOpenGlossary = { term = null; onOpenGlossary() }) }
}

/** 三格速览：要花什么、换回什么、证据多硬。点证据那格展开等级含义 */
@Composable
private fun Overview(e: Entry) {
    var showGrade by rememberSaveable(e.id) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 三格等高：按最高的那格撑开，文字多时换行而不是截断
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Cell("要花", EntryText.cost(e), Modifier.weight(1f).fillMaxHeight())
            Cell("换回", EntryText.gain(e), Modifier.weight(1f).fillMaxHeight())
            Cell("证据", "${e.grade} 级 ⓘ", Modifier.weight(1f).fillMaxHeight().clickable { showGrade = !showGrade })
        }
        if (showGrade) {
            Text(
                "${e.grade} 级：${Labels.GRADE[e.grade].orEmpty()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Cell(label: String, value: String, modifier: Modifier = Modifier) {
    AppCard(modifier, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
        }
    }
}

/** 可折叠的字段 */
@Composable
private fun Fold(
    label: String,
    initiallyOpen: Boolean,
    hint: String? = null,
    color: Color? = null,
    content: @Composable () -> Unit,
) {
    var open by rememberSaveable(label) { mutableStateOf(initiallyOpen) }
    Column {
        HorizontalDivider(thickness = 0.5.dp, color = LocalExtraColors.current.hairline)
        Row(
            Modifier.fillMaxWidth().clickable { open = !open }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = color ?: MaterialTheme.colorScheme.onSurface)
            hint?.let { Text("  $it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                Icon(if (open) Icons.ExpandLess else Icons.ExpandMore, if (open) "收起" else "展开", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (open) Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) { content() }
    }
}

/** 底部唯一的主按钮，文案见 docs/ux.md「条目」 */
@Composable
private fun PrimaryBar(s: EntryUi.Ready, onPrimary: () -> Unit, onRemove: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp)) {
            val modifier = Modifier.fillMaxWidth().height(48.dp)
            when {
                s.done -> FilledTonalButton(onPrimary, modifier) {
                    Icon(Icons.Check, null, Modifier.size(18.dp)); Text("  已做到，点一下取消")
                }
                s.inList -> Button(onPrimary, modifier) { Text("标记为做到了") }
                else -> Button(onPrimary, modifier) {
                    Icon(Icons.Add, null, Modifier.size(18.dp)); Text("  加入清单")
                }
            }
            // 自己加进来的条目可以移出；自动在清单里的核心条目一直在
            if (s.addedByUser) {
                TextButton(onRemove, Modifier.align(Alignment.CenterHorizontally)) { Text("移出清单") }
            }
        }
    }
}
