package app.lifeguide.feature.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lifeguide.core.corpus.Entry
import app.lifeguide.core.corpus.FilterState
import app.lifeguide.core.corpus.InlineMd
import app.lifeguide.core.corpus.Links
import app.lifeguide.core.corpus.Section
import app.lifeguide.core.corpus.XrefContext
import app.lifeguide.core.data.CorpusRepository
import app.lifeguide.core.data.ChecklistRepository
import app.lifeguide.core.data.FilterRepository
import app.lifeguide.core.ui.Sharer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface EntryUi {
    data object Loading : EntryUi
    /** 当前版本的正文里没有这一条（被合并或删掉了） */
    data object Missing : EntryUi
    data class Ready(
        val entry: Entry,
        val section: Section?,
        val terms: List<String>,
        /** 在不在清单里（核心条目按「我的情况」自动在，其他条目要用户加） */
        val inList: Boolean,
        val done: Boolean,
        val addedByUser: Boolean,
        /** 解析正文里「见第 X 节第 Y 条」用 */
        val xref: XrefContext,
    ) : EntryUi
}

class EntryViewModel(
    private val id: String,
    corpusRepository: CorpusRepository,
    filters: FilterRepository,
    private val checklist: ChecklistRepository,
    private val sharer: Sharer,
) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val ui: StateFlow<EntryUi> = combine(corpusRepository.state, filters.query) { loaded, q -> loaded to q }
        .flatMapLatest { (loaded, q) ->
            val corpus = loaded?.corpus ?: return@flatMapLatest flowOf(EntryUi.Loading)
            val e = corpus.entry(id) ?: return@flatMapLatest flowOf(EntryUi.Missing)
            // 从搜索结果进来时，条目页也高亮同样的词
            checklist.stateOf(e).map { st ->
                EntryUi.Ready(e, corpus.sectionByN[e.sec], FilterState(q = q).terms, st.inList, st.done, st.addedByUser, XrefContext(corpus, e.sec))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EntryUi.Loading)

    /** 底部主按钮：在清单里就切换「做到了」，不在就加入清单 */
    fun primaryAction(s: EntryUi.Ready) {
        if (s.inList) checklist.setDone(s.entry, !s.done) else checklist.setAdded(s.entry, true)
    }

    /** 用户自己加的条目可以移出清单；自动在清单里的核心条目只能取消勾选 */
    fun removeFromList(e: Entry) = checklist.setAdded(e, false)

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
