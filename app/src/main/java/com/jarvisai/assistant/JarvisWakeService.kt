package com.jarvisai.assistant

import android.app.Service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import org.vosk.android.RecognitionListener
import org.json.JSONObject
import java.io.IOException

class JarvisWakeService : Service() {

    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var speechService: SpeechService? = null
    private var wakeDetected = false
    @Volatile private var stopping = false

    private val channelId = "jarvis_wake_channel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startNotification("Jarvis microphone is starting")
        loadHindiModel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Jarvis Voice Assistant",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Hey Jarvis background listening"
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun startNotification(message: String) {
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Jarvis AI")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                77,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(77, notification)
        }
    }

    private fun loadHindiModel() {
        StorageService.unpack(
            this,
            "model-hi",
            "model-hi",
            { loadedModel ->
                if (stopping) {
                    loadedModel.close()
                } else {
                    model = loadedModel
                    startRecognition()
                }
            },
            { exception ->
                updateNotification("Hindi model error: ${exception.message}")
                stopSelf()
            }
        )
    }

    private fun startRecognition() {
        try {
            val loadedModel = model ?: throw IllegalStateException("Model not loaded")
            val activeRecognizer = Recognizer(loadedModel, 16000.0f)
            recognizer = activeRecognizer
            speechService = SpeechService(activeRecognizer, 16000.0f)

            speechService?.startListening(object : RecognitionListener {

                override fun onPartialResult(hypothesis: String?) {
                    checkWakePhrase(hypothesis)
                }

                override fun onResult(hypothesis: String?) {
                    checkWakePhrase(hypothesis)
                }

                override fun onFinalResult(hypothesis: String?) {
                    checkWakePhrase(hypothesis)
                    wakeDetected = false
                }

                override fun onError(exception: Exception?) {
                    updateNotification("Microphone error - listening stopped")
                    stopping = true
                    stopSelf()
                }

                override fun onTimeout() {
                    if (!stopping) updateNotification("Jarvis listening...")
                }
            })

            updateNotification("Jarvis listening for Hey Jarvis")

        } catch (e: Exception) {
            updateNotification("Recognition error: ${e.message}")
            stopSelf()
        }
    }

    private fun checkWakePhrase(hypothesis: String?) {
        if (hypothesis.isNullOrBlank() || wakeDetected) return

        val text = try {
            JSONObject(hypothesis).optString("text")
                .ifBlank { JSONObject(hypothesis).optString("partial") }
                .lowercase()
        } catch (e: Exception) {
            return
        }

        if (
            text.contains("hey jarvis") ||
            text.contains("हे जार्विस") ||
            text.contains("जार्विस")
        ) {
            wakeDetected = true
            updateNotification("Hey Jarvis detected!")
        }
    }

    private fun updateNotification(message: String) {
        if (stopping) return
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Jarvis AI")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

        getSystemService(NotificationManager::class.java)
            .notify(77, notification)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        stopping = true
        speechService?.stop()
        speechService?.shutdown()
        speechService = null
        recognizer?.close()
        recognizer = null
        model?.close()
        model = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
