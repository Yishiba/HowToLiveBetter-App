package app.lifeguide

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.lifeguide.data.CorpusRepository
import app.lifeguide.data.CorpusStore
import app.lifeguide.data.CorpusUpdater
import app.lifeguide.data.Prefs
import okio.Path.Companion.toPath

/** 进程级单例。DataStore 同一个文件只能有一个实例，Activity 重建时不能再建一次。 */
class AppGraph private constructor(val env: AppEnv) {
    init { env.fileSystem.createDirectories(env.dataDir) }

    val prefs = Prefs(PreferenceDataStoreFactory.createWithPath(produceFile = { (env.dataDir / "prefs.preferences_pb").toString().toPath() }))
    val store = CorpusStore(env.fileSystem, env.dataDir)
    val repository = CorpusRepository(store)
    val updater by lazy { CorpusUpdater(env.httpClient(), store, { repository.state.value }) }

    companion object {
        private var instance: AppGraph? = null
        fun get(env: () -> AppEnv): AppGraph = instance ?: AppGraph(env()).also { instance = it }
    }
}
