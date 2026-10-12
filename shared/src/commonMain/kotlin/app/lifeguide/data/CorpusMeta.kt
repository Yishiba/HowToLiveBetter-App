package app.lifeguide.data

import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** 一份正文快照的身份：上游 commit 与提交时间。内置快照和下载的快照用同一种 manifest。 */
@Serializable
data class CorpusManifest(
    val commit: String,
    val commitDate: String,
    val files: List<String>,
    val entryCount: Int,
) {
    @OptIn(ExperimentalTime::class)
    val instant: Instant? get() = runCatching { Instant.parse(commitDate) }.getOrNull()

    /** 比较新旧只看提交时间；解析不了的一律当最旧，宁可不切换 */
    @OptIn(ExperimentalTime::class)
    fun isNewerThan(other: CorpusManifest): Boolean {
        val a = instant ?: return false
        val b = other.instant ?: return true
        return a > b
    }
}

enum class CorpusSource { Bundled, Downloaded }
