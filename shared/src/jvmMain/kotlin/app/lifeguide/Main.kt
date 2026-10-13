package app.lifeguide

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.lifeguide.core.data.AppDirs
import app.lifeguide.core.ui.Sharer
import app.lifeguide.di.initKoin
import okio.Path.Companion.toPath
import org.koin.dsl.module

/** 桌面端只用来在开发时快速看界面，不发布 */
fun main() {
    initKoin(
        module {
            single { AppDirs((System.getProperty("user.home") + "/.lifeguide-dev").toPath()) }
            single<Sharer> { Sharer { println("[share] $it") } }
        },
    )
    application {
        Window(onCloseRequest = ::exitApplication, title = "人生指南", state = rememberWindowState(width = 420.dp, height = 860.dp)) {
            App()
        }
    }
}
