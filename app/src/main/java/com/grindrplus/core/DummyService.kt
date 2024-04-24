package com.grindrplus.core

import android.app.Service
import android.content.Intent
import android.os.IBinder

class DummyService : Service() {
    /**
     * Required for the original Grindr app to be able to
     * query the module APK. This service does nothing ;)
     */
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}