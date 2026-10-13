package app.lifeguide.feature.find

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lifeguide.core.corpus.Entry
import app.lifeguide.core.corpus.Filter
import app.lifeguide.core.corpus.FilterState
import app.lifeguide.core.corpus.Section
import app.lifeguide.core.data.CorpusRepository
import app.lifeguide.core.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SectionUi(val section: Section, val entries: List<Entry>, val newKeys: Set<String>)

/** 一节的条目列表。筛选条件只在这一页有效，离开就清空，不影响查找页。 */
class SectionViewModel(
    private val n: String,
    corpusRepository: CorpusRepository,
    prefs: UserPreferences,
) : ViewModel() {
    private val _filter = MutableStateFlow(FilterState(sec = setOf(n)))
    val filter: StateFlow<FilterState> = _filter.asStateFlow()

    val ui: StateFlow<SectionUi?> = combine(corpusRepository.state.filterNotNull(), _filter, prefs.recentChanges) { loaded, f, changes ->
        val s = loaded.corpus.sectionByN[n] ?: return@combine null
        SectionUi(s, Filter.apply(s.entries, f), changes?.changes?.let { (it.added + it.changed).toSet() }.orEmpty())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { corpusRepository.ensureLoaded() }
    }

    fun update(transform: (FilterState) -> FilterState) { _filter.value = transform(_filter.value).copy(sec = setOf(n)) }

    /** 快捷开关「只看零成本」：钱、时间、毅力都是最低档 */
    fun setZeroCost(on: Boolean) = update {
        if (on) it.copy(money = setOf("0"), time = setOf("少"), will = setOf("否"))
        else it.copy(money = emptySet(), time = emptySet(), will = emptySet())
    }

    fun setGradeA(on: Boolean) = update { it.copy(grade = if (on) setOf("A") else emptySet()) }
}

internal val FilterState.zeroCost get() = money == setOf("0") && time == setOf("少") && will == setOf("否")
internal val FilterState.gradeAOnly get() = grade == setOf("A")
