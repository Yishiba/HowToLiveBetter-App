package app.lifeguide.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.lifeguide.filter.FilterState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

enum class ThemeMode(val label: String) { System("跟随系统"), Light("浅色"), Dark("深色") }

class Prefs(private val ds: DataStore<Preferences>) {
    private object K {
        val filter = stringPreferencesKey("filter")
        val theme = stringPreferencesKey("theme")
        val autoUpdate = booleanPreferencesKey("auto_update")
        val lastCheck = longPreferencesKey("last_check")
        val disclaimer = booleanPreferencesKey("disclaimer_accepted")
    }
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun filter(): FilterState = ds.data.first()[K.filter]
        ?.let { runCatching { json.decodeFromString(FilterState.serializer(), it) }.getOrNull() }
        ?: FilterState()

    suspend fun setFilter(f: FilterState) = ds.edit { it[K.filter] = json.encodeToString(FilterState.serializer(), f) }

    val theme: Flow<ThemeMode> = ds.data.map { p -> ThemeMode.entries.firstOrNull { it.name == p[K.theme] } ?: ThemeMode.System }
    suspend fun setTheme(t: ThemeMode) = ds.edit { it[K.theme] = t.name }

    val autoUpdate: Flow<Boolean> = ds.data.map { it[K.autoUpdate] ?: true }
    suspend fun setAutoUpdate(v: Boolean) = ds.edit { it[K.autoUpdate] = v }

    suspend fun lastCheck(): Long = ds.data.first()[K.lastCheck] ?: 0L
    suspend fun setLastCheck(ms: Long) = ds.edit { it[K.lastCheck] = ms }

    val disclaimerAccepted: Flow<Boolean> = ds.data.map { it[K.disclaimer] ?: false }
    suspend fun acceptDisclaimer() = ds.edit { it[K.disclaimer] = true }
}
