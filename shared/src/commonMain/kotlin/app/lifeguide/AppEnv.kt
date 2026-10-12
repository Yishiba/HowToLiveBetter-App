package app.lifeguide

import io.ktor.client.HttpClient
import okio.FileSystem
import okio.Path

/** 平台差异都收在这里：数据目录、HTTP 引擎、系统分享。由各平台入口创建一次。 */
interface AppEnv {
    val fileSystem: FileSystem get() = FileSystem.SYSTEM
    /** App 私有、不会被系统随手清掉的目录 */
    val dataDir: Path
    fun httpClient(): HttpClient
    fun share(text: String)
}

object Links {
    const val OWNER = "eternity4719"
    const val REPO_NAME = "HowToLiveBetter"
    const val REPO = "https://github.com/$OWNER/$REPO_NAME"
    const val SITE = "https://$OWNER.github.io/$REPO_NAME/"
    const val ISSUES = "$REPO/issues"
    const val LICENSE = "https://creativecommons.org/licenses/by/4.0/deed.zh-hans"
    fun entryOnSite(sec: String, n: String) = "${SITE}#e-$sec-$n"
}
