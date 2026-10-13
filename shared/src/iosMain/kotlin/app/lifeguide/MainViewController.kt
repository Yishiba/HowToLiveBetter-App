package app.lifeguide

import androidx.compose.ui.window.ComposeUIViewController
import app.lifeguide.core.data.AppDirs
import app.lifeguide.core.ui.Sharer
import app.lifeguide.di.initKoin
import okio.Path.Companion.toPath
import org.koin.dsl.module
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.popoverPresentationController

private val iosPlatformModule = module {
    single {
        val support = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String
        AppDirs(support.toPath() / "LifeGuide")
    }
    single<Sharer> { Sharer(::presentShareSheet) }
}

@Suppress("DEPRECATION")
private fun presentShareSheet(text: String) {
    val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
    var top = root
    while (top.presentedViewController != null) top = top.presentedViewController!!
    val vc = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
    vc.popoverPresentationController?.sourceView = top.view
    top.presentViewController(vc, animated = true, completion = null)
}

/** Swift 侧 App 启动时调一次（Swift 里叫 doInitKoinIos） */
fun initKoinIos() {
    initKoin(iosPlatformModule)
}

fun MainViewController() = ComposeUIViewController { App() }
