package app.lifeguide.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lifeguide.AppGraph
import app.lifeguide.corpus.Entry
import app.lifeguide.corpus.Section
import app.lifeguide.data.LoadedCorpus
import app.lifeguide.data.ThemeMode
import app.lifeguide.data.UpdateResult
import app.lifeguide.filter.Filter
import app.lifeguide.filter.FilterState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

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

enum class UpdateStatus { Idle, Checking }

@OptIn(FlowPreview::class, ExperimentalTime::class)
class MainViewModel(private val graph: AppGraph) : ViewModel() {
    private val repo = graph.repository
    private val prefs = graph.prefs

    val corpus: StateFlow<LoadedCorpus?> = repo.state

    private val _filter = MutableStateFlow(FilterState())
    val filter: StateFlow<FilterState> = _filter.asStateFlow()

    /** 搜索框里的字，立即更新；参与筛选的是防抖后的 */
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    private val _updateStatus = MutableStateFlow(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    val theme = prefs.theme.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.System)
    val autoUpdate = prefs.autoUpdate.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val disclaimerAccepted = prefs.disclaimerAccepted.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val debouncedQuery = _query.debounce(150).onStart { emit(_query.value) }

    val list: StateFlow<ListUi?> = combine(corpus.filterNotNull(), _filter, debouncedQuery) { c, f, q ->
        compute(c, f.copy(q = q))
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            val saved = prefs.filter()
            _filter.value = saved.copy(q = "")
            _query.value = saved.q
            // 筛选条件落盘，下次打开时恢复
            combine(_filter, _query) { f, q -> f.copy(q = q) }.drop(1).debounce(500).collect { prefs.setFilter(it) }
        }
        viewModelScope.launch {
            if (repo.state.value == null) repo.load()
            maybeAutoUpdate()
        }
    }

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

    fun setQuery(q: String) { _query.value = q }

    fun update(f: (FilterState) -> FilterState) { _filter.value = f(_filter.value) }

    fun toggle(dim: String, v: String) = update { s ->
        fun Set<String>.flip() = if (v in this) this - v else this + v
        when (dim) {
            "sec" -> s.copy(sec = if (v in s.sec) emptySet() else setOf(v))
            "who" -> s.copy(who = s.who.flip())
            "ratio" -> s.copy(ratio = s.ratio.flip())
            "lens" -> s.copy(lens = s.lens.flip())
            "grade" -> s.copy(grade = s.grade.flip())
            "money" -> s.copy(money = s.money.flip())
            "time" -> s.copy(time = s.time.flip())
            "will" -> s.copy(will = s.will.flip())
            else -> s
        }
    }

    fun clearAll() { _filter.value = FilterState(); _query.value = "" }

    fun setTheme(t: ThemeMode) = viewModelScope.launch { prefs.setTheme(t) }
    fun setAutoUpdate(v: Boolean) = viewModelScope.launch { prefs.setAutoUpdate(v) }
    fun acceptDisclaimer() = viewModelScope.launch { prefs.acceptDisclaimer() }

    private suspend fun maybeAutoUpdate() {
        if (!prefs.autoUpdate.first()) return
        val now = Clock.System.now().toEpochMilliseconds()
        if (now - prefs.lastCheck() < CHECK_INTERVAL_MS) return
        runUpdate(manual = false)
    }

    fun checkNow() = viewModelScope.launch { runUpdate(manual = true) }

    private suspend fun runUpdate(manual: Boolean) {
        if (_updateStatus.value == UpdateStatus.Checking) return
        _updateStatus.value = UpdateStatus.Checking
        try {
            val r = graph.updater.check()
            when (r) {
                is UpdateResult.Updated -> {
                    repo.reloadDownloaded()
                    _messages.tryEmit("正文已更新到 ${r.manifest.commitDate.take(10)} 的版本")
                }
                UpdateResult.UpToDate -> if (manual) _messages.tryEmit("已经是最新的正文")
                is UpdateResult.Failed -> {
                    println("[LifeGuide] 更新正文失败：${r.reason}")
                    if (manual) _messages.tryEmit("这次没更新成功：${r.reason}")
                }
            }
            if (r !is UpdateResult.Failed) prefs.setLastCheck(Clock.System.now().toEpochMilliseconds())
        } finally {
            _updateStatus.value = UpdateStatus.Idle
        }
    }

    fun entry(id: String): Entry? = corpus.value?.corpus?.entry(id)

    companion object {
        const val CHECK_INTERVAL_MS = 12 * 60 * 60 * 1000L
    }
}
