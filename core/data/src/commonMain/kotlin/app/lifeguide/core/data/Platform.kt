package app.lifeguide.core.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout
import okio.Path

/** App 私有、不会被系统随手清掉的数据目录。由各平台入口提供。 */
class AppDirs(val data: Path)

/** 各平台的 HTTP 引擎：Android 用 OkHttp，iOS 用 Darwin（NSURLSession），JVM 用 CIO */
internal expect fun httpEngine(): HttpClientEngineFactory<*>

internal fun createHttpClient(): HttpClient = HttpClient(httpEngine()) {
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 15_000
    }
}
