package app.lifeguide

import android.content.Context
import android.content.Intent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import okio.Path
import okio.Path.Companion.toOkioPath

class AndroidAppEnv(context: Context) : AppEnv {
    private val app = context.applicationContext
    override val dataDir: Path = app.filesDir.toOkioPath()

    override fun httpClient() = HttpClient(OkHttp) {
        install(HttpTimeout) { requestTimeoutMillis = 30_000; connectTimeoutMillis = 15_000 }
    }

    override fun share(text: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        app.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
