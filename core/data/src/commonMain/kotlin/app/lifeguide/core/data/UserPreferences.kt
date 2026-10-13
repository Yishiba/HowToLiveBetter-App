package app.lifeguide.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import app.lifeguide.core.corpus.CorpusChanges
import app.lifeguide.core.corpus.FilterState
import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

enum class ThemeMode(val label: String) { System("跟随系统"), Light("浅色"), Dark("深色") }

/** 本机偏好：设置项、上次检查更新的时间、筛选条件。只存在本机，不上传。 */
class UserPreferences(private val ds: DataStore<Preferences>) {
    private object K {
        val filter = stringPreferencesKey("filter")
        val theme = stringPreferencesKey("theme")
        val autoUpdate = booleanPreferencesKey("auto_update")
        val lastCheck = longPreferencesKey("last_check")
        // 键名沿用旧版：首次启动页里包含免责说明，旧版点过「我知道了」的不用再看一次
        val onboarded = booleanPreferencesKey("disclaimer_accepted")
        val profile = stringSetPreferencesKey("profile")
        val done = stringSetPreferencesKey("checklist_done")
        val added = stringSetPreferencesKey("checklist_added")
        val changes = stringPreferencesKey("recent_changes")
    }
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun filter(): FilterState = ds.data.first()[K.filter]
        ?.let { runCatching { json.decodeFromString(FilterState.serializer(), it) }.getOrNull() }
        ?: FilterState()

    suspend fun setFilter(f: FilterState) {
        ds.edit { it[K.filter] = json.encodeToString(FilterState.serializer(), f) }
    }

    val theme: Flow<ThemeMode> = ds.data.map { p -> ThemeMode.entries.firstOrNull { it.name == p[K.theme] } ?: ThemeMode.System }
    suspend fun setTheme(t: ThemeMode) {
        ds.edit { it[K.theme] = t.name }
    }

    val autoUpdate: Flow<Boolean> = ds.data.map { it[K.autoUpdate] ?: true }
    suspend fun setAutoUpdate(v: Boolean) {
        ds.edit { it[K.autoUpdate] = v }
    }

    suspend fun lastCheck(): Long = ds.data.first()[K.lastCheck] ?: 0L
    suspend fun setLastCheck(ms: Long) {
        ds.edit { it[K.lastCheck] = ms }
    }

    /** 看过首次启动页（免责说明 + 我的情况） */
    val onboarded: Flow<Boolean> = ds.data.map { it[K.onboarded] ?: false }
    suspend fun finishOnboarding() {
        ds.edit { it[K.onboarded] = true }
    }

    /** 我的情况：原书人群标签里符合自己的那几个（怀孕、婴幼儿、孩子、老人、35岁、60岁、女性） */
    val profile: Flow<Set<String>> = ds.data.map { it[K.profile] ?: emptySet() }
    suspend fun setProfile(v: Set<String>) {
        ds.edit { it[K.profile] = v }
    }

    /** 清单里打了勾的条目，按 [app.lifeguide.core.corpus.key] 记 */
    val done: Flow<Set<String>> = ds.data.map { it[K.done] ?: emptySet() }
    /** 用户从别处加入清单的条目 */
    val added: Flow<Set<String>> = ds.data.map { it[K.added] ?: emptySet() }

    suspend fun setDone(key: String, value: Boolean) {
        ds.edit { p -> p[K.done] = (p[K.done] ?: emptySet()).let { if (value) it + key else it - key } }
    }

    suspend fun setAdded(key: String, value: Boolean) {
        ds.edit { p -> p[K.added] = (p[K.added] ?: emptySet()).let { if (value) it + key else it - key } }
    }

    /** 最近一次联网更新带来的改动，用户关掉提示前一直在 */
    val recentChanges: Flow<RecentChanges?> = ds.data.map { p ->
        p[K.changes]?.let { runCatching { json.decodeFromString(RecentChanges.serializer(), it) }.getOrNull() }
    }

    suspend fun setRecentChanges(v: RecentChanges?) {
        ds.edit { p -> if (v == null) p.remove(K.changes) else p[K.changes] = json.encodeToString(RecentChanges.serializer(), v) }
    }
}

@Serializable
data class RecentChanges(val commitDate: String, val changes: CorpusChanges)
