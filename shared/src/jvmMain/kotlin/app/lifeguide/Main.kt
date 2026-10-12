package app.lifeguide

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import okio.Path
import okio.Path.Companion.toPath

/** 桌面端只用来在开发时快速看界面，不发布 */
class JvmAppEnv : AppEnv {
    override val dataDir: Path = (System.getProperty("user.home") + "/.lifeguide-dev").toPath()
    override fun httpClient() = HttpClient(CIO) { install(HttpTimeout) { requestTimeoutMillis = 30_000 } }
    override fun share(text: String) = println("[share] $text")
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "人生指南", state = rememberWindowState(width = 420.dp, height = 860.dp)) {
        App { JvmAppEnv() }
    }
}
