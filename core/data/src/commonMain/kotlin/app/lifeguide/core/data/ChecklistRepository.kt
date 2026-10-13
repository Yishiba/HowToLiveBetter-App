package app.lifeguide.core.data

import app.lifeguide.core.corpus.Corpus
import app.lifeguide.core.corpus.Entry
import app.lifeguide.core.corpus.Filter
import app.lifeguide.core.corpus.isCore
import app.lifeguide.core.corpus.key
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/** 条目页底部按钮要的状态：在不在清单、做到没有、是不是用户自己加的（只有自己加的能移出） */
data class EntryListState(val inList: Boolean, val done: Boolean, val addedByUser: Boolean)

/** 清单里的一条 */
data class ChecklistItem(val entry: Entry, val done: Boolean, val addedByUser: Boolean)

data class Checklist(
    /** 核心条目（性价比极高且证据 A 级，按「我的情况」过滤），保持正文顺序 */
    val core: List<ChecklistItem>,
    /** 用户从别处加入的、不在核心里的条目 */
    val added: List<ChecklistItem>,
) {
    val all: List<ChecklistItem> get() = core + added
    val doneCount: Int get() = all.count { it.done }
}

/**
 * 清单 = 核心条目 + 用户加入的条目，勾选状态按条目标题记（见 [key]）。
 * 写操作跑在进程级作用域里：点完勾立刻离开页面也不会丢。
 */
class ChecklistRepository(
    private val corpusRepository: CorpusRepository,
    private val prefs: UserPreferences,
    private val scope: CoroutineScope,
) {
    val checklist: Flow<Checklist> = combine(
        corpusRepository.state.filterNotNull(), prefs.profile, prefs.done, prefs.added,
    ) { loaded, profile, done, added -> build(loaded.corpus, profile, done, added) }

    /** 某条目在清单里的状态 */
    fun stateOf(entry: Entry): Flow<EntryListState> =
        combine(prefs.profile, prefs.done, prefs.added) { profile, done, added ->
            val auto = entry.isCore && Filter.fitsProfile(entry, profile)
            EntryListState(inList = auto || entry.key in added, done = entry.key in done, addedByUser = !auto && entry.key in added)
        }

    fun setDone(entry: Entry, value: Boolean) {
        scope.launch { prefs.setDone(entry.key, value) }
    }

    fun setAdded(entry: Entry, value: Boolean) {
        scope.launch {
            prefs.setAdded(entry.key, value)
            if (!value) prefs.setDone(entry.key, false)
        }
    }

    companion object {
        fun build(corpus: Corpus, profile: Set<String>, done: Set<String>, added: Set<String>): Checklist {
            val core = corpus.entries.filter { it.isCore && Filter.fitsProfile(it, profile) }
            val coreKeys = core.mapTo(HashSet()) { it.key }
            val extra = corpus.entries.filter { it.key in added && it.key !in coreKeys }
            fun item(e: Entry, byUser: Boolean) = ChecklistItem(e, e.key in done, byUser)
            return Checklist(core.map { item(it, false) }, extra.map { item(it, true) })
        }
    }
}
