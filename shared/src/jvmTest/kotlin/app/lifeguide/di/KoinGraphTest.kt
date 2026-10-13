package app.lifeguide.di

import app.lifeguide.AppViewModel
import app.lifeguide.core.corpus.CorpusParser
import app.lifeguide.core.data.AppDirs
import app.lifeguide.core.data.BundledCorpusSource
import app.lifeguide.core.data.ContentUpdateService
import app.lifeguide.core.data.CorpusManifest
import app.lifeguide.core.data.CorpusRepository
import app.lifeguide.core.data.FilterRepository
import app.lifeguide.core.ui.Sharer
import app.lifeguide.feature.browse.BrowseViewModel
import app.lifeguide.feature.entry.EntryViewModel
import app.lifeguide.feature.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import okio.Path.Companion.toOkioPath
import org.koin.core.context.stopKoin
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 依赖图完整：每个单例、每个 ViewModel 都能从 Koin 里造出来；内置正文确实打进了资源 */
@OptIn(ExperimentalCoroutinesApi::class)
class KoinGraphTest {
    private val dir = Files.createTempDirectory("lifeguide-koin").toOkioPath()
    private val koin = initKoin(
        module {
            single { AppDirs(dir) }
            single<Sharer> { Sharer {} }
        },
    ).koin

    @BeforeTest fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun everyDefinitionResolves() {
        koin.get<CorpusRepository>()
        koin.get<FilterRepository>()
        koin.get<ContentUpdateService>()
        koin.get<AppViewModel>()
        koin.get<BrowseViewModel>()
        koin.get<SettingsViewModel>()
        koin.get<EntryViewModel> { parametersOf("1-1") }
    }

    @Test
    fun bundledCorpusIsPackaged() = runTest {
        val bundled = koin.get<BundledCorpusSource>()
        val manifest = Json.decodeFromString(CorpusManifest.serializer(), bundled.read("manifest.json"))
        assertEquals(manifest.files, CorpusParser.bookFiles(bundled.read("README.md")))
        assertTrue(manifest.entryCount > 600, "内置正文只有 ${manifest.entryCount} 条")
    }
}
