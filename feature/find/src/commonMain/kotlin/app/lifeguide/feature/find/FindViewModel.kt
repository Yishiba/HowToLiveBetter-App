package app.lifeguide.feature.find

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lifeguide.core.corpus.Entry
import app.lifeguide.core.corpus.Filter
import app.lifeguide.core.corpus.FilterState
import app.lifeguide.core.corpus.Scenes
import app.lifeguide.core.corpus.Section
import app.lifeguide.core.corpus.Situations
import app.lifeguide.core.data.CorpusRepository
import app.lifeguide.core.data.FilterRepository
import app.lifeguide.core.data.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FindUi(
    val total: Int,
    /** 章节目录：场景 → 章节 */
    val scenes: List<Pair<String, List<Section>>>,
    /** 「遇到事了」：显示名 → 节号（只留正文里存在的节） */
    val situations: List<Pair<String, String>>,
    val sectionTitles: Map<String, String>,
    /** 有关键词或筛选条件时的结果；null 表示显示目录 */
    val results: List<Entry>?,
    val terms: List<String>,
    val newKeys: Set<String>,
)

@OptIn(FlowPreview::class)
class FindViewModel(
    private val corpusRepository: CorpusRepository,
    private val filters: FilterRepository,
    prefs: UserPreferences,
) : ViewModel() {
    val query: StateFlow<String> = filters.query
    val filter: StateFlow<FilterState> = filters.filter

    // 搜索框每敲一个字都更新 query，参与检索的是防抖后的
    private val debouncedQuery = filters.query.debounce(150).onStart { emit(filters.query.value) }

    val ui: StateFlow<FindUi?> = combine(
        corpusRepository.state.filterNotNull(), filters.filter, debouncedQuery, prefs.recentChanges,
    ) { loaded, f, q, changes ->
        val c = loaded.corpus
        val state = f.copy(q = q, sec = emptySet())
        val searching = state.terms.isNotEmpty() || state.activeCount > 0
        FindUi(
            total = c.entries.size,
            scenes = Scenes.group(c.sections),
            situations = Situations.ALL.filter { c.sectionByN[it.second] != null },
            sectionTitles = c.sections.associate { it.n to it.title },
            results = if (searching) Filter.apply(c.entries, state) else null,
            terms = state.terms,
            newKeys = changes?.changes?.let { (it.added + it.changed).toSet() }.orEmpty(),
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { corpusRepository.ensureLoaded() }
    }

    fun setQuery(q: String) = filters.setQuery(q)
    fun update(transform: (FilterState) -> FilterState) = filters.update(transform)
    fun clearAll() = filters.clearAll()
}
