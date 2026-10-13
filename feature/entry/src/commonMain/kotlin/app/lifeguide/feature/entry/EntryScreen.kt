package app.lifeguide.feature.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lifeguide.core.corpus.Links
import app.lifeguide.core.corpus.SplitRules
import app.lifeguide.core.ui.EntryBadges
import app.lifeguide.core.ui.Icons
import app.lifeguide.core.ui.Labels
import app.lifeguide.core.ui.rememberInlineText
import app.lifeguide.core.ui.theme.LocalTones
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EntryScreen(
    id: String,
    onBack: () -> Unit,
    vm: EntryViewModel = koinViewModel(key = id) { parametersOf(id) },
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val ready = ui as? EntryUi.Ready
    val e = ready?.entry
    val section = ready?.section
    val terms = ready?.terms.orEmpty()
    val uri = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(e?.let { "第 ${it.sec} 节 · 第 ${it.n} 条" } ?: "条目") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Back, "返回") } },
                actions = {
                    if (e != null) {
                        IconButton(onClick = { vm.share(e) }) { Icon(Icons.Share, "分享") }
                        IconButton(onClick = { uri.openUri(Links.entryOnSite(e.sec, e.n)) }) { Icon(Icons.OpenInNew, "在网页中打开") }
                    }
                },
            )
        },
    ) { pad ->
        if (ui == EntryUi.Missing) {
            Text("这一条在当前版本的正文里找不到了，可能被合并或删掉了。", Modifier.padding(pad).padding(24.dp))
            return@Scaffold
        }
        if (e == null) return@Scaffold
        SelectionContainer(Modifier.padding(pad).fillMaxSize()) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                section?.let { Text("${it.n}. ${it.title}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
                Text(rememberInlineText(e.title, terms), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                EntryBadges(e)

                if (e.human.isNotEmpty()) {
                    Text(
                        rememberInlineText(e.human, terms),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth()
                            .background(LocalTones.current.brandSoft, RoundedCornerShape(10.dp))
                            .padding(14.dp),
                    )
                }
                Field("成本") { Para(e.cost, terms) }
                Field("收益") { SplitRules.splitGain(e.gain).forEach { Para(it, terms) } }
                Field("证据等级") {
                    Text("${e.grade} 级：${Labels.GRADE[e.grade].orEmpty()}", style = MaterialTheme.typography.bodyMedium)
                }
                Field("来源") {
                    val parts = SplitRules.splitSrc(e.src)
                    parts.forEachIndexed { i, p ->
                        Row {
                            if (parts.size > 1) Text("${i + 1}. ", style = MaterialTheme.typography.bodySmall)
                            Text(rememberInlineText(p, terms, urlsLast = true), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Field(if (e.dispute) "备注（有争议）" else "备注", if (e.dispute) LocalTones.current.red else null) {
                    SplitRules.splitGain(e.note).forEach { Para(it, terms) }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    "数字的口径以条目内标注为准，不同口径之间不做比较。内容可能过时或有错，办事、看病、打官司以当地部门、医生和律师的说法为准。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Field(label: String, color: Color? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = color ?: MaterialTheme.colorScheme.onSurface)
        content()
    }
}

@Composable
private fun Para(text: String, terms: List<String>) {
    Text(rememberInlineText(text, terms), style = MaterialTheme.typography.bodyMedium)
}
