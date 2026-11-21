package com.rabindra.focustime

import android.app.Application

class FocusTimeApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notificationHelper.createChannels()
        container.controller.initialize()
    }
}
