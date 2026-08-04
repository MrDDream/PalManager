package com.paladmin

import android.app.Application
import com.paladmin.crash.CrashHandler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PalAdminApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
    }
}
