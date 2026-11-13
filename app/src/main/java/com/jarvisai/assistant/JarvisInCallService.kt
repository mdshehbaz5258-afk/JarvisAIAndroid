package com.jarvisai.assistant

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService

object JarvisCallManager {
    @Volatile
    var activeCall: Call? = null

    @Volatile
    var service: JarvisInCallService? = null
}

class JarvisInCallService : InCallService() {

    override fun onCreate() {
        super.onCreate()
        JarvisCallManager.service = this
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        JarvisCallManager.activeCall = call

        val intent = Intent(this, CallScreenActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)

        if (JarvisCallManager.activeCall === call) {
            JarvisCallManager.activeCall = null
        }
    }

    override fun onDestroy() {
        JarvisCallManager.service = null
        super.onDestroy()
    }
}
