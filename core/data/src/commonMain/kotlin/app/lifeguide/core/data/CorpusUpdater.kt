package app.lifeguide.core.data

import app.lifeguide.core.corpus.Links
import app.lifeguide.core.corpus.CorpusParser
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed interface UpdateResult {
    data class Updated(val manifest: CorpusManifest) : UpdateResult
    data object UpToDate : UpdateResult
    data class Failed(val reason: String) : UpdateResult
}

/**
 * 联网更新正文：
 * 1. 问 GitHub API 上游 main 现在是哪个 commit；
 * 2. 按这个 commit 下载 README 和各节文件（jsDelivr 优先，失败依次换镜像），同一个 commit 的文件内容不会变；
 * 3. 解析、校验通过后整份装进 [CorpusStore]，任何一步不通过都保留现有版本。
 */
class CorpusUpdater(
    private val client: HttpClient,
    private val store: CorpusStore,
    private val current: () -> LoadedCorpus?,
    private val mirrors: List<(sha: String, path: String) -> String> = DEFAULT_MIRRORS,
    private val latestCommitUrl: String = "https://api.github.com/repos/${Links.OWNER}/${Links.REPO_NAME}/commits/main",
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun check(): UpdateResult = withContext(Dispatchers.Default) {
        try {
            val cur = current() ?: return@withContext UpdateResult.Failed("正文还没加载")
            val (sha, date) = latestCommit()
            val remote = CorpusManifest(sha, date, emptyList(), 0)
            if (sha == cur.manifest.commit || !remote.isNewerThan(cur.manifest)) return@withContext UpdateResult.UpToDate

            val readme = fetch(sha, "README.md")
            val files = CorpusParser.bookFiles(readme)
            if (files.isEmpty()) return@withContext UpdateResult.Failed("README 里没有找到正文文件")
            val gate = Semaphore(6)
            val texts = coroutineScope {
                files.map { f -> async { gate.withPermit { f to fetch(sha, f) } } }.awaitAll().toMap()
            }
            val sections = CorpusParser.parseFiles(readme) { texts.getValue(it) }
            val minEntries = cur.corpus.entries.size * 9 / 10
            CorpusValidator.check(sections, minEntries)?.let { return@withContext UpdateResult.Failed("校验没通过：$it") }

            val manifest = CorpusManifest(sha, date, files, sections.sumOf { it.entries.size })
            store.install(manifest, texts + ("README.md" to readme))
            UpdateResult.Updated(manifest)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UpdateResult.Failed(e.message ?: e::class.simpleName ?: "未知错误")
        }
    }

    private suspend fun latestCommit(): Pair<String, String> {
        val resp = client.get(latestCommitUrl) { header("Accept", "application/vnd.github+json") }
        if (!resp.status.isSuccess()) error("查询最新版本失败（HTTP ${resp.status.value}）")
        val o = json.parseToJsonElement(resp.bodyAsText()).jsonObject
        val sha = o.getValue("sha").jsonPrimitive.content
        val date = o.getValue("commit").jsonObject.getValue("committer").jsonObject.getValue("date").jsonPrimitive.content
        require(Regex("[0-9a-f]{40}").matches(sha)) { "返回的 commit 不对：$sha" }
        return sha to date
    }

    private suspend fun fetch(sha: String, path: String): String {
        var last: String? = null
        for (m in mirrors) {
            val url = m(sha, path)
            try {
                val resp = client.get(url)
                if (resp.status.isSuccess()) {
                    val text = resp.bodyAsText().replace("\r\n", "\n")
                    if (text.isNotBlank()) return text
                    last = "$url 内容为空"
                } else last = "$url HTTP ${resp.status.value}"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                last = "$url ${e.message}"
            }
        }
        error("下载 $path 失败：$last")
    }

    companion object {
        private const val GH = "${Links.OWNER}/${Links.REPO_NAME}"
        val DEFAULT_MIRRORS: List<(String, String) -> String> = listOf(
            { sha, path -> "https://cdn.jsdelivr.net/gh/$GH@$sha/${Links.encodePath(path)}" },
            { sha, path -> "https://fastly.jsdelivr.net/gh/$GH@$sha/${Links.encodePath(path)}" },
            { sha, path -> "https://raw.githubusercontent.com/$GH/$sha/${Links.encodePath(path)}" },
        )
    }
}
