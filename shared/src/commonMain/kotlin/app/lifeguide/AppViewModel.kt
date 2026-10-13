package app.lifeguide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lifeguide.core.data.ContentUpdateService
import app.lifeguide.core.data.CorpusRepository
import app.lifeguide.core.data.ThemeMode
import app.lifeguide.core.data.UpdateEvent
import app.lifeguide.core.data.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** App 级状态：主题、免责声明、全局提示；启动时加载正文并按需检查更新 */
class AppViewModel(
    private val prefs: UserPreferences,
    corpusRepository: CorpusRepository,
    updates: ContentUpdateService,
) : ViewModel() {
    val theme = prefs.theme.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.System)
    // 读到之前先当已同意，免得每次启动闪一下对话框
    val disclaimerAccepted = prefs.disclaimerAccepted.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val messages: Flow<String> = updates.events.map { e ->
        when (e) {
            is UpdateEvent.Updated -> "正文已更新到 ${e.manifest.commitDate.take(10)} 的版本"
            UpdateEvent.UpToDate -> "已经是最新的正文"
            is UpdateEvent.Failed -> "这次没更新成功：${e.reason}"
        }
    }

    init {
        viewModelScope.launch {
            corpusRepository.ensureLoaded()
            updates.checkIfDue()
        }
    }

    fun acceptDisclaimer() {
        viewModelScope.launch { prefs.acceptDisclaimer() }
    }
}
