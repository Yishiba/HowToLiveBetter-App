package app.lifeguide.feature.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lifeguide.core.corpus.Entry
import app.lifeguide.core.corpus.Lens
import app.lifeguide.core.corpus.key
import app.lifeguide.core.data.ChecklistItem
import app.lifeguide.core.data.ChecklistRepository
import app.lifeguide.core.data.CorpusRepository
import app.lifeguide.core.data.RecentChanges
import app.lifeguide.core.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChecklistUi(
    /** 当前分段下的核心条目，没做到的在前 */
    val core: List<ChecklistItem>,
    /** 当前分段下用户加入的条目 */
    val added: List<ChecklistItem>,
    /** 分段：全部 + 各口径（只列有条目的），带条数 */
    val lenses: List<Pair<Lens, Int>>,
    val selected: Lens?,
    val done: Int,
    val total: Int,
    val sectionTitles: Map<String, String>,
    val changes: RecentChanges?,
) {
    val newKeys: Set<String> = changes?.changes?.let { (it.added + it.changed).toSet() }.orEmpty()
}

class ChecklistViewModel(
    corpusRepository: CorpusRepository,
    private val checklist: ChecklistRepository,
    private val prefs: UserPreferences,
) : ViewModel() {
    private val selected = MutableStateFlow<Lens?>(null)

    val ui: StateFlow<ChecklistUi?> = combine(
        checklist.checklist, corpusRepository.state.filterNotNull(), prefs.recentChanges, selected,
    ) { list, loaded, changes, lens ->
        fun List<ChecklistItem>.inLens() = if (lens == null) this else filter { Lens.of(it.entry.lens) == lens }
        fun List<ChecklistItem>.undoneFirst() = sortedBy { it.done }
        val all = list.all
        ChecklistUi(
            core = list.core.inLens().undoneFirst(),
            added = list.added.inLens().undoneFirst(),
            lenses = Lens.entries.map { l -> l to all.count { Lens.of(it.entry.lens) == l } }.filter { it.second > 0 },
            selected = lens,
            done = list.doneCount,
            total = all.size,
            sectionTitles = loaded.corpus.sections.associate { it.n to it.title },
            changes = changes,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { corpusRepository.ensureLoaded() }
    }

    fun select(lens: Lens?) { selected.value = lens }

    fun setDone(e: Entry, value: Boolean) = checklist.setDone(e, value)

    private val corpus = corpusRepository.state

    /** 更新提示里列出的条目，按键找回当前正文里的那一条（找不到的跳过） */
    fun entriesFor(keys: List<String>): List<Entry> {
        val byKey = corpus.value?.corpus?.entries?.associateBy { it.key } ?: return emptyList()
        return keys.mapNotNull { byKey[it] }
    }

    fun dismissChanges() {
        viewModelScope.launch { prefs.setRecentChanges(null) }
    }
}
