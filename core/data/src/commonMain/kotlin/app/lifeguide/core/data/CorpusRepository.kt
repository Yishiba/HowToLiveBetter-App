package app.lifeguide.core.data

import app.lifeguide.core.corpus.Corpus
import app.lifeguide.core.corpus.CorpusParser
import app.lifeguide.core.corpus.Glossary
import app.lifeguide.core.corpus.Section
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class LoadedCorpus(val corpus: Corpus, val manifest: CorpusManifest, val source: CorpusSource)

/** 打进安装包的那份正文。由 App 模块用 compose 资源实现，路径相对正文根目录（README.md、book 目录下各节、manifest.json）。 */
fun interface BundledCorpusSource {
    suspend fun read(path: String): String
}

/**
 * 决定用哪份正文：下载的那份存在、读得出来、并且比内置的新，就用它，否则用内置的。
 * 装了新版 App、内置的反而更新时，清掉下载的旧快照。
 */
class CorpusRepository(private val store: CorpusStore, private val bundled: BundledCorpusSource) {
    private val _state = MutableStateFlow<LoadedCorpus?>(null)
    val state: StateFlow<LoadedCorpus?> = _state.asStateFlow()
    private val lock = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    /** 第一次调用时加载，之后直接返回；并发调用只加载一次 */
    suspend fun ensureLoaded(): LoadedCorpus = _state.value ?: lock.withLock { _state.value ?: reload() }

    /** 更新器装好新快照之后调用 */
    suspend fun reload(): LoadedCorpus = withContext(Dispatchers.Default) { pick() }.also { _state.value = it }

    private suspend fun pick(): LoadedCorpus {
        val bundledManifest = json.decodeFromString(CorpusManifest.serializer(), bundled.read("manifest.json"))
        val downloaded = store.activeManifest()
        if (downloaded != null && downloaded.isNewerThan(bundledManifest)) {
            val parsed = runCatching {
                val readme = store.read(downloaded.commit, "README.md")
                Corpus(CorpusParser.parseFiles(readme) { store.read(downloaded.commit, it) }, Glossary.parse(readme))
            }.getOrNull()
            if (parsed != null && CorpusValidator.check(parsed.sections, minEntries = 1) == null) {
                return LoadedCorpus(parsed, downloaded, CorpusSource.Downloaded)
            }
        }
        if (downloaded != null) store.clear()
        val readme = bundled.read("README.md")
        val files = CorpusParser.bookFiles(readme).associateWith { bundled.read(it) }
        val sections = CorpusParser.parseFiles(readme) { files.getValue(it) }
        return LoadedCorpus(Corpus(sections, Glossary.parse(readme)), bundledManifest, CorpusSource.Bundled)
    }
}

object CorpusValidator {
    /** 返回 null 表示通过，否则是不通过的原因 */
    fun check(sections: List<Section>, minEntries: Int): String? {
        val entries = sections.flatMap { it.entries }
        if (sections.size < 30) return "只解析出 ${sections.size} 节"
        if (entries.size < minEntries) return "只解析出 ${entries.size} 条，少于当前的九成（$minEntries）"
        if (entries.any { it.title.isEmpty() }) return "有条目没有标题"
        // 上游允许个别条目暂缺「说人话」，所以只要求收益和证据等级绝大多数都在
        val broken = entries.count { it.gain.isEmpty() || it.grade.isEmpty() }
        if (broken > entries.size / 20) return "$broken 条缺收益或证据等级，格式可能变了"
        return null
    }
}
