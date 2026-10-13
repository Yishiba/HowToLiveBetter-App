package app.lifeguide.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lifeguide.core.corpus.Links
import app.lifeguide.core.data.CorpusSource
import app.lifeguide.core.data.ThemeMode
import app.lifeguide.core.data.UpdateStatus
import app.lifeguide.core.ui.Icons
import app.lifeguide.core.ui.Labels
import app.lifeguide.core.ui.Pill
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import org.koin.compose.viewmodel.koinViewModel

internal const val DISCLAIMER =
    "本 App 的正文来自开源书《高性价比人生指南》，只供参考，不构成医疗、法律或财务建议。" +
        "内容可能过时，也可能有错；不少政策各地执行不一样。真去办事、看病、打官司时，以当地部门、医生和律师的说法为准。" +
        "遇到紧急情况，先打 120、110 或 119。"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    vm: SettingsViewModel = koinViewModel(),
) {
    val corpus by vm.corpus.collectAsStateWithLifecycle()
    val theme by vm.theme.collectAsStateWithLifecycle()
    val auto by vm.autoUpdate.collectAsStateWithLifecycle()
    val status by vm.updateStatus.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val uri = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于与设置") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Back, "返回") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Heading("我的情况")
            Body("带人群标签的建议（比如孕期、带娃、照顾老人），只有符合你的情况才会进清单。查找里始终能看到全部。")
            ProfileChips(profile, vm::toggleProfile)

            HorizontalDivider()
            Heading("正文版本")
            corpus?.let { c ->
                val src = if (c.source == CorpusSource.Bundled) "随安装包内置" else "已联网更新"
                Body("${c.manifest.commitDate.take(10)} · ${c.manifest.commit.take(7)} · $src · 共 ${c.corpus.sections.size} 节 ${c.corpus.entries.size} 条")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Body("自动检查正文更新（每 12 小时最多一次）", Modifier.weight(1f))
                Switch(checked = auto, onCheckedChange = vm::setAutoUpdate)
            }
            OutlinedButton(onClick = { vm.checkNow() }, enabled = status == UpdateStatus.Idle) {
                if (status == UpdateStatus.Checking) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("  正在检查")
                } else Text("现在检查更新")
            }

            HorizontalDivider()
            Heading("外观")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { i, m ->
                    SegmentedButton(
                        selected = theme == m,
                        onClick = { vm.setTheme(m) },
                        shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                    ) { Text(m.label) }
                }
            }

            HorizontalDivider()
            Heading("关于这本书")
            Body(
                "正文来自 eternity4719 编写的开源书《高性价比人生指南》，按 CC BY 4.0 许可使用。" +
                    "本 App 是非官方客户端，只改了排版（长段落按原书规则拆块显示），没有改动正文的文字。",
            )
            LinkRow("原书网页版") { uri.openUri(Links.SITE) }
            LinkRow("原书 GitHub 仓库") { uri.openUri(Links.REPO) }
            LinkRow("CC BY 4.0 许可协议") { uri.openUri(Links.LICENSE) }
            LinkRow("发现内容写错了？去原书提 issue") { uri.openUri(Links.ISSUES) }

            HorizontalDivider()
            Heading("免责声明")
            Body(DISCLAIMER)

            HorizontalDivider()
            Heading("隐私")
            Body(
                "本 App 不注册账号，不收集、不上传任何个人信息，也不含广告和统计 SDK。" +
                    "联网只做一件事：从 GitHub 和 jsDelivr 下载公开的正文文件。筛选条件和设置只保存在本机。",
            )

            HorizontalDivider()
            Heading("开源组件")
            Body(
                "Kotlin、Kotlin Coroutines、kotlinx.serialization、Compose Multiplatform、AndroidX（Lifecycle、Navigation、DataStore）、" +
                    "Ktor、Okio、Koin、Kermit、Material Icons，均为 Apache License 2.0。",
            )
        }
    }
}

@Composable
private fun Heading(t: String) = Text(t, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

@Composable
private fun Body(t: String, modifier: Modifier = Modifier) =
    Text(t, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)

@Composable
private fun LinkRow(t: String, onClick: () -> Unit) = Row(
    Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Text(t, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
    Icon(Icons.OpenInNew, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
}

/** 「我的情况」的选项：取自筛选里「我的情况」那组，去掉「以上都不是」 */
internal val PROFILE_OPTIONS = Labels.GROUPS.first { it.dim == "who" }.options.filter { it.value != "无" }

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfileChips(selected: Set<String>, onToggle: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (o in PROFILE_OPTIONS) Pill(o.label, o.value in selected, onClick = { onToggle(o.value) })
    }
}
