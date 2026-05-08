package com.vectorminds.app

import android.app.Application
import com.vectorminds.core.service.VectorMindsAgentWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VectorMindsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Schedule the autonomous agent loop
        VectorMindsAgentWorker.schedule(this)
    }
}
