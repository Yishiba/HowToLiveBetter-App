package app.lifeguide.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lifeguide.core.data.ContentUpdateService
import app.lifeguide.core.data.CorpusRepository
import app.lifeguide.core.data.LoadedCorpus
import app.lifeguide.core.data.ThemeMode
import app.lifeguide.core.data.UpdateStatus
import app.lifeguide.core.data.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    corpusRepository: CorpusRepository,
    private val prefs: UserPreferences,
    private val updates: ContentUpdateService,
) : ViewModel() {
    val corpus: StateFlow<LoadedCorpus?> = corpusRepository.state
    val updateStatus: StateFlow<UpdateStatus> = updates.status
    val theme = prefs.theme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.System)
    val autoUpdate = prefs.autoUpdate.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setTheme(t: ThemeMode) = viewModelScope.launch { prefs.setTheme(t) }
    fun setAutoUpdate(v: Boolean) = viewModelScope.launch { prefs.setAutoUpdate(v) }
    /** 结果经 [ContentUpdateService.events] 由 App 层统一弹提示 */
    fun checkNow() = updates.checkNow()
}
