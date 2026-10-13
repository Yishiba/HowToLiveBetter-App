package app.lifeguide.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lifeguide.core.ui.AppCard
import app.lifeguide.core.ui.Icons
import org.koin.compose.viewmodel.koinViewModel

/**
 * 整张术语表，照 README 里的顺序（作者按「统计 → 医学 → 钱和法律」排的）。
 * 表里一格写了几个词的（「d、g」）在这里合回一张卡片。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GlossaryScreen(onBack: () -> Unit, vm: SettingsViewModel = koinViewModel()) {
    val corpus by vm.corpus.collectAsStateWithLifecycle()
    val rows = remember(corpus) {
        corpus?.corpus?.glossary?.terms.orEmpty()
            .groupBy { it.meaning }
            .map { (meaning, ts) -> ts.joinToString("、") { it.term } to meaning }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("读懂数字") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Back, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "intro") {
                Text(
                    "正文尽量用日常说法，引用研究时免不了出现几个统计名词。条目里带虚线的词点一下也能看到解释。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
            items(rows, key = { it.first }) { (terms, meaning) ->
                AppCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(terms, style = MaterialTheme.typography.titleMedium)
                        Text(meaning, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
