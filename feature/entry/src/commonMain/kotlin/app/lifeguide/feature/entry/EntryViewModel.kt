package app.lifeguide.feature.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lifeguide.core.corpus.Entry
import app.lifeguide.core.corpus.FilterState
import app.lifeguide.core.corpus.InlineMd
import app.lifeguide.core.corpus.Links
import app.lifeguide.core.corpus.Section
import app.lifeguide.core.data.CorpusRepository
import app.lifeguide.core.data.FilterRepository
import app.lifeguide.core.ui.Sharer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface EntryUi {
    data object Loading : EntryUi
    /** 当前版本的正文里没有这一条（被合并或删掉了） */
    data object Missing : EntryUi
    data class Ready(val entry: Entry, val section: Section?, val terms: List<String>) : EntryUi
}

class EntryViewModel(
    private val id: String,
    corpusRepository: CorpusRepository,
    filters: FilterRepository,
    private val sharer: Sharer,
) : ViewModel() {
    val ui: StateFlow<EntryUi> = combine(corpusRepository.state, filters.query) { loaded, q ->
        val corpus = loaded?.corpus ?: return@combine EntryUi.Loading
        val e = corpus.entry(id) ?: return@combine EntryUi.Missing
        // 从列表页带着关键词进来时，详情页也高亮同样的词
        EntryUi.Ready(e, corpus.sectionByN[e.sec], FilterState(q = q).terms)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EntryUi.Loading)

    init {
        viewModelScope.launch { corpusRepository.ensureLoaded() }
    }

    fun share(e: Entry) = sharer.share(
        buildString {
            append(InlineMd.plain(e.title))
            if (e.human.isNotEmpty()) append("\n").append(InlineMd.plain(e.human))
            append("\n").append(Links.entryOnSite(e.sec, e.n))
        },
    )
}
