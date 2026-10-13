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
    /** null 表示还没读到偏好：这时什么都不画，免得老用户每次启动闪一下引导页 */
    val onboarded = prefs.onboarded.map<Boolean, Boolean?> { it }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

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
}
