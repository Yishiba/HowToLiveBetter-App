package app.lifeguide.di

import app.lifeguide.AppViewModel
import app.lifeguide.core.data.BundledCorpusSource
import app.lifeguide.core.data.di.dataModule
import app.lifeguide.feature.checklist.checklistModule
import app.lifeguide.feature.find.findModule
import app.lifeguide.feature.entry.entryModule
import app.lifeguide.feature.settings.settingsModule
import app.lifeguide.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** 内置正文快照：:shared:syncCorpus 生成，作为 compose 资源打进安装包 */
@OptIn(ExperimentalResourceApi::class)
private val bundledCorpus = BundledCorpusSource { path -> Res.readBytes("files/corpus/$path").decodeToString() }

private val appModule = module {
    single { bundledCorpus }
    viewModelOf(::AppViewModel)
}

/**
 * 进程启动时调一次。[platform] 由各平台入口提供：[app.lifeguide.core.data.AppDirs] 与 [app.lifeguide.core.ui.Sharer]。
 * 新功能模块在这里登记它的 Koin 模块。
 */
fun initKoin(platform: Module): KoinApplication = startKoin {
    modules(listOf(platform) + appModules)
}

/** 除平台模块外的全部模块；测试也用它校验依赖图是否完整 */
internal val appModules: List<Module> = listOf(dataModule, appModule, checklistModule, findModule, entryModule, settingsModule)
