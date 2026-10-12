package app.lifeguide.data

import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path

/**
 * 下载下来的正文快照：dataDir/corpus/{commit}/ 下放 README.md、book 目录下各节 md、manifest.json，
 * dataDir/corpus/active 记当前用哪一份。先写临时目录再整体改名，写到一半断电也不会留下半份正文。
 */
class CorpusStore(private val fs: FileSystem, dataDir: Path) {
    private val root = dataDir / "corpus"
    private val activeFile = root / "active"
    private val json = Json { ignoreUnknownKeys = true }

    fun activeManifest(): CorpusManifest? = runCatching {
        val commit = fs.read(activeFile) { readUtf8() }.trim()
        val m = json.decodeFromString(CorpusManifest.serializer(), fs.read(root / commit / "manifest.json") { readUtf8() })
        m.takeIf { it.commit == commit && m.files.all { f -> fs.exists(root / commit / f) } }
    }.getOrNull()

    fun read(commit: String, path: String): String = fs.read(root / commit / path) { readUtf8() }

    fun install(manifest: CorpusManifest, files: Map<String, String>) {
        val tmp = root / "tmp-${manifest.commit}"
        val dest = root / manifest.commit
        fs.deleteRecursively(tmp)
        for ((path, text) in files) {
            val p = tmp / path
            p.parent?.let { fs.createDirectories(it) }
            fs.write(p) { writeUtf8(text) }
        }
        fs.write(tmp / "manifest.json") { writeUtf8(json.encodeToString(CorpusManifest.serializer(), manifest)) }
        fs.deleteRecursively(dest)
        fs.atomicMove(tmp, dest)

        val activeTmp = root / "active.tmp"
        fs.write(activeTmp) { writeUtf8(manifest.commit) }
        fs.atomicMove(activeTmp, activeFile)
        prune(keep = manifest.commit)
    }

    /** 内置快照比下载的还新（装了新版 App）时，作废下载的那份 */
    fun clear() {
        fs.delete(activeFile, mustExist = false)
        prune(keep = null)
    }

    private fun prune(keep: String?) {
        if (!fs.exists(root)) return
        for (p in fs.list(root)) {
            if (p.name == "active" || p.name == keep) continue
            fs.deleteRecursively(p)
        }
    }
}
