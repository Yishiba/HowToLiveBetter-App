package app.lifeguide.data

import app.lifeguide.corpus.Corpus
import app.lifeguide.corpus.CorpusParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class LoadedCorpus(val corpus: Corpus, val manifest: CorpusManifest, val source: CorpusSource)

/**
 * 决定用哪份正文：下载的那份存在、读得出来、并且比内置的新，就用它，否则用内置的。
 * 装了新版 App、内置的反而更新时，清掉下载的旧快照。
 */
class CorpusRepository(private val store: CorpusStore) {
    private val _state = MutableStateFlow<LoadedCorpus?>(null)
    val state: StateFlow<LoadedCorpus?> = _state.asStateFlow()

    suspend fun load() {
        _state.value = withContext(Dispatchers.Default) { pick() }
    }

    /** 更新器装好新快照之后调用 */
    suspend fun reloadDownloaded() = load()

    private suspend fun pick(): LoadedCorpus {
        val bundled = BundledCorpus.manifest()
        val downloaded = store.activeManifest()
        if (downloaded != null && downloaded.isNewerThan(bundled)) {
            val parsed = runCatching {
                CorpusParser.parseFiles(store.read(downloaded.commit, "README.md")) { store.read(downloaded.commit, it) }
            }.getOrNull()
            if (parsed != null && CorpusValidator.check(parsed, minEntries = 1) == null) {
                return LoadedCorpus(Corpus(parsed), downloaded, CorpusSource.Downloaded)
            }
        }
        if (downloaded != null) store.clear()
        val readme = BundledCorpus.read("README.md")
        val files = CorpusParser.bookFiles(readme).associateWith { BundledCorpus.read(it) }
        val sections = CorpusParser.parseFiles(readme) { files.getValue(it) }
        return LoadedCorpus(Corpus(sections), bundled, CorpusSource.Bundled)
    }
}

object CorpusValidator {
    /** 返回 null 表示通过，否则是不通过的原因 */
    fun check(sections: List<app.lifeguide.corpus.Section>, minEntries: Int): String? {
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
