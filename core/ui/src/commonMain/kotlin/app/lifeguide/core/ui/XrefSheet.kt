package app.lifeguide.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lifeguide.core.corpus.Corpus
import app.lifeguide.core.corpus.Inline
import app.lifeguide.core.corpus.XrefTarget

/**
 * 点了正文里的引用之后去哪：只指向一条或一节就直接打开（返回键回来），指向好几条时先列出来让人挑。
 * 网页要先弹预览，是因为跳过去会丢掉阅读位置；App 的返回键会回到原处，不需要这一步。
 */
@Stable
class XrefNavigator internal constructor(
    private val openEntry: () -> (String) -> Unit,
    private val openSection: () -> (String) -> Unit,
) {
    /** 指向好几处、等用户挑的那处引用；null 表示没在挑 */
    var choosing by mutableStateOf<Inline.Xref?>(null)
        internal set

    fun open(x: Inline.Xref) {
        val targets = x.targets.distinct()
        when (val single = targets.singleOrNull()) {
            is XrefTarget.ToEntry -> openEntry()(single.id)
            is XrefTarget.ToSection -> openSection()(single.n)
            null -> if (targets.isNotEmpty()) choosing = x.copy(targets = targets)
        }
    }

    internal fun go(t: XrefTarget) {
        choosing = null
        when (t) {
            is XrefTarget.ToEntry -> openEntry()(t.id)
            is XrefTarget.ToSection -> openSection()(t.n)
        }
    }
}

@Composable
fun rememberXrefNavigator(onOpenEntry: (String) -> Unit, onOpenSection: (String) -> Unit): XrefNavigator {
    val entry by rememberUpdatedState(onOpenEntry)
    val section by rememberUpdatedState(onOpenSection)
    return remember { XrefNavigator({ entry }, { section }) }
}

/** 引用指向好几条时的选择面板，放在页面里，[XrefNavigator.choosing] 不为空时出现 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XrefSheet(nav: XrefNavigator, corpus: Corpus) {
    val x = nav.choosing ?: return
    ModalBottomSheet(onDismissRequest = { nav.choosing = null }) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 标题是点的那处原文，比如「第 16 节第 1、2 条」
            item(key = "title") { Text(x.text, style = MaterialTheme.typography.titleMedium) }
            items(x.targets, key = { it.toString() }) { t ->
                when (t) {
                    is XrefTarget.ToEntry -> corpus.entry(t.id)?.let { e ->
                        val sec = corpus.sectionByN[e.sec]?.title?.let(EntryText::shortTitle).orEmpty()
                        EntryCard(e, meta = "$sec · 第 ${e.n} 条", onClick = { nav.go(t) })
                    }
                    is XrefTarget.ToSection -> corpus.sectionByN[t.n]?.let { s ->
                        AppCard(Modifier.fillMaxWidth(), onClick = { nav.go(t) }) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(s.title, style = MaterialTheme.typography.titleMedium)
                                Text("整节，共 ${s.entries.size} 条", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

