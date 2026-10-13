package app.lifeguide.core.data

import app.lifeguide.core.corpus.FilterState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * 当前的筛选条件与搜索框文字，进程内唯一一份：列表页改它，详情页用它高亮关键词。
 * 启动时从 [UserPreferences] 恢复，之后每次变化（防抖 500ms）写回，下次打开接着用。
 */
@OptIn(FlowPreview::class)
class FilterRepository(private val prefs: UserPreferences, scope: CoroutineScope) {
    private val _filter = MutableStateFlow(FilterState())
    /** 除关键词外的条件；关键词在 [query] 里 */
    val filter: StateFlow<FilterState> = _filter.asStateFlow()

    private val _query = MutableStateFlow("")
    /** 搜索框里的原文，即时更新；参与筛选前由调用方自行防抖 */
    val query: StateFlow<String> = _query.asStateFlow()

    init {
        scope.launch {
            val saved = prefs.filter()
            _filter.value = saved.copy(q = "")
            _query.value = saved.q
            combine(_filter, _query) { f, q -> f.copy(q = q) }.drop(1).debounce(500).collect { prefs.setFilter(it) }
        }
    }

    fun setQuery(q: String) { _query.value = q }

    fun update(transform: (FilterState) -> FilterState) { _filter.value = transform(_filter.value) }

    fun clearAll() { _filter.value = FilterState(); _query.value = "" }
}
