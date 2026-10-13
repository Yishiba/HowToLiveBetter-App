package app.lifeguide.core.data

import app.lifeguide.core.corpus.CorpusParser
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNull

/**
 * upstream/ 这份正文能不能通过 App 联网更新时的校验。夜间任务会把 upstream/ 换成上游最新的 main 再跑：
 * 解析和网页一致（GoldenTest）还不够，校验不过的话用户会一直停在旧版本，而且没人知道。
 */
class UpstreamAcceptanceTest {
    @Test
    fun updateValidatorAcceptsUpstream() {
        val dir = File(System.getProperty("upstream") ?: error("要经 Gradle 的 jvmTest 跑"))
        val readme = dir.resolve("README.md").readText()
        val sections = CorpusParser.parseFiles(readme) { dir.resolve(it).readText() }
        // 600：远低于现在的条数（680 多），只防「格式变了、只解析出一小部分」
        assertNull(CorpusValidator.check(sections, minEntries = 600))
    }
}
