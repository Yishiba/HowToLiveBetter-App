package app.lifeguide.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lifeguide.core.corpus.Entry
import app.lifeguide.core.corpus.Filter
import app.lifeguide.core.corpus.FilterState
import app.lifeguide.core.corpus.Section
import app.lifeguide.core.data.CorpusRepository
import app.lifeguide.core.data.FilterRepository
import app.lifeguide.core.data.LoadedCorpus
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

data class SectionHits(val section: Section, val entries: List<Entry>)

data class ListUi(
    val total: Int,
    val shown: Int,
    val groups: List<SectionHits>,
    /** 不计章节条件时每节的命中数（章节选择器里显示） */
    val perSection: Map<String, Int>,
    val matchedAllSections: Int,
    val terms: List<String>,
)

@OptIn(FlowPreview::class)
class BrowseViewModel(
    private val corpusRepository: CorpusRepository,
    private val filters: FilterRepository,
) : ViewModel() {
    val corpus: StateFlow<LoadedCorpus?> = corpusRepository.state
    val filter: StateFlow<FilterState> = filters.filter
    val query: StateFlow<String> = filters.query

    // 搜索框每敲一个字都更新 query，参与筛选的是防抖后的
    private val debouncedQuery = filters.query.debounce(150).onStart { emit(filters.query.value) }

    val list: StateFlow<ListUi?> = combine(corpus.filterNotNull(), filter, debouncedQuery) { c, f, q ->
        compute(c, f.copy(q = q))
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch { corpusRepository.ensureLoaded() }
    }

    fun setQuery(q: String) = filters.setQuery(q)
    fun update(transform: (FilterState) -> FilterState) = filters.update(transform)
    fun toggle(dim: String, v: String) = filters.toggle(dim, v)
    fun clearAll() = filters.clearAll()

    private fun compute(c: LoadedCorpus, f: FilterState): ListUi {
        val terms = f.terms
        val per = mutableMapOf<String, Int>()
        var matched = 0
        val groups = mutableListOf<SectionHits>()
        for (s in c.corpus.sections) {
            val hits = s.entries.filter { Filter.matchesExceptSec(it, f, terms) }
            per[s.n] = hits.size
            matched += hits.size
            if (hits.isNotEmpty() && (f.sec.isEmpty() || s.n in f.sec)) groups += SectionHits(s, hits)
        }
        return ListUi(c.corpus.entries.size, groups.sumOf { it.entries.size }, groups, per, matched, terms)
    }
}
