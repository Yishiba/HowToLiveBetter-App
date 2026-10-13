package app.lifeguide

import android.content.Context
import android.content.Intent
import app.lifeguide.core.data.AppDirs
import app.lifeguide.core.ui.Sharer
import okio.Path.Companion.toOkioPath
import org.koin.dsl.module

/** 传 Application，不要传 Activity：这些单例活得比任何一个 Activity 都久 */
fun androidPlatformModule(app: Context) = module {
    single { AppDirs(app.filesDir.toOkioPath()) }
    single<Sharer> {
        Sharer { text ->
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
            app.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
