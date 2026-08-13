package io.lunosfer.dreamap

import android.app.Application

class DreamapApp : Application() {
    companion object {
        lateinit var instance: DreamapApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
