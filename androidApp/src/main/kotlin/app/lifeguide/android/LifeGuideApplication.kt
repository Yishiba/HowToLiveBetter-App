package app.lifeguide.android

import android.app.Application
import app.lifeguide.androidPlatformModule
import app.lifeguide.di.initKoin

class LifeGuideApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(androidPlatformModule(this))
    }
}
