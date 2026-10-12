package app.lifeguide

import androidx.compose.ui.window.ComposeUIViewController
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.popoverPresentationController

class IosAppEnv : AppEnv {
    override val dataDir: Path =
        (NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String).toPath() / "LifeGuide"

    override fun httpClient() = HttpClient(Darwin) {
        install(HttpTimeout) { requestTimeoutMillis = 30_000; connectTimeoutMillis = 15_000 }
    }

    @Suppress("DEPRECATION")
    override fun share(text: String) {
        val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
        var top = root
        while (top.presentedViewController != null) top = top.presentedViewController!!
        val vc = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
        vc.popoverPresentationController?.sourceView = top.view
        top.presentViewController(vc, animated = true, completion = null)
    }
}

fun MainViewController() = ComposeUIViewController { App { IosAppEnv() } }
