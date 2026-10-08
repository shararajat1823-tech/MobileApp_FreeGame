package com.miniplay.app

import android.app.Application
import com.miniplay.app.di.AppContainer

/**
 * Owns the single [AppContainer] for the process. Activities and the Compose
 * tree reach it through this class (see [MainActivity]).
 */
class MiniPlayApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.start()
    }
}
