package app.lifeguide.core.data.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.lifeguide.core.data.AppDirs
import app.lifeguide.core.data.ContentUpdateService
import app.lifeguide.core.data.CorpusRepository
import app.lifeguide.core.data.CorpusStore
import app.lifeguide.core.data.CorpusUpdater
import app.lifeguide.core.data.FilterRepository
import app.lifeguide.core.data.UserPreferences
import app.lifeguide.core.data.createHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.FileSystem
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** 进程级协程作用域：跑不该随某个页面结束而取消的活（下载更新、写偏好） */
val AppScope = named("AppScope")

/**
 * 数据层的全部单例。需要外部提供：[AppDirs]（平台入口）和 [app.lifeguide.core.data.BundledCorpusSource]（App 模块）。
 * DataStore 同一个文件只能有一个实例，所以必须是 single。
 */
val dataModule = module {
    single(AppScope) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single<FileSystem> { FileSystem.SYSTEM }
    single { createHttpClient() }
    single {
        val dirs = get<AppDirs>()
        get<FileSystem>().createDirectories(dirs.data)
        UserPreferences(PreferenceDataStoreFactory.createWithPath(produceFile = { dirs.data / "prefs.preferences_pb" }))
    }
    single { CorpusStore(get(), get<AppDirs>().data) }
    singleOf(::CorpusRepository)
    single { CorpusUpdater(get(), get(), current = { get<CorpusRepository>().state.value }) }
    single { FilterRepository(get(), get(AppScope)) }
    single { ContentUpdateService(get(), get(), get(), get(AppScope)) }
}
