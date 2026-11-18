package com.jarvisai.assistant

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class CallScreenActivity : Activity() {

    private val green = Color.rgb(0, 255, 150)
    private val bg = Color.rgb(5, 9, 15)
    private val white = Color.WHITE

    private lateinit var status: TextView
    private lateinit var number: TextView
    private lateinit var contactName: TextView
    private lateinit var controls: LinearLayout
    private lateinit var keypad: LinearLayout

    private var muted = false
    private var speaker = false
    private var keypadVisible = false
    private var currentCall: Call? = null

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            runOnUiThread { updateScreen() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(35), dp(24), dp(25))
            setBackgroundColor(bg)
        }

        contactName = TextView(this).apply {
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(0, dp(35), 0, dp(4))
        }
        root.addView(contactName)

        number = TextView(this).apply {
            textSize = 25f
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(0, dp(35), 0, dp(12))
        }
        root.addView(number)

        status = TextView(this).apply {
            textSize = 16f
            setTextColor(green)
            gravity = Gravity.CENTER
        }
        root.addView(status)

        controls = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, dp(35), 0, dp(10))
        }
        root.addView(controls)

        keypad = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        root.addView(keypad)

        setContentView(root)
        updateScreen()
    }

    private fun findContactName(phoneNumber: String?): String? {
        if (phoneNumber.isNullOrBlank()) return null

        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) return null

        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )

            contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (e: SecurityException) {
            null
        }
    }

    private fun addButton(parent: LinearLayout, label: String, action: () -> Unit) {
        val button = Button(this).apply {
            text = label
            textSize = 14f
            isAllCaps = false
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 0)
            minHeight = dp(58)
            includeFontPadding = false
            setTextColor(green)
            setBackgroundColor(Color.rgb(20, 38, 39))
            setOnClickListener { action() }
        }
        parent.addView(button, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(58)
        ).apply {
            setMargins(0, dp(6), 0, dp(6))
        })
    }

    private fun addGridButton(parent: LinearLayout, label: String, action: () -> Unit) {
        val button = Button(this).apply {
            text = label
            textSize = 13f
            isAllCaps = false
            minWidth = 0
            minHeight = dp(78)
            setTextColor(white)
            setBackgroundColor(Color.rgb(25, 29, 38))
            setOnClickListener { action() }
        }
        parent.addView(button, LinearLayout.LayoutParams(
            0, dp(82), 1f
        ).apply {
            setMargins(dp(4), dp(4), dp(4), dp(4))
        })
    }

    private fun updateScreen() {
        val call = JarvisCallManager.activeCall
        currentCall = call

        if (call == null) {
            status.text = "Call ended"
            contactName.text = ""
            number.text = ""
            controls.removeAllViews()
            keypad.removeAllViews()
            return
        }

        val phoneNumber = call.details.handle?.schemeSpecificPart
        val savedName = findContactName(phoneNumber)
        contactName.text = savedName ?: (phoneNumber ?: "Unknown number")
        number.text = if (savedName != null) phoneNumber.orEmpty() else ""

        controls.removeAllViews()
        keypad.removeAllViews()

        when (call.state) {
            Call.STATE_RINGING -> {
                status.text = "INCOMING CALL"
                addButton(controls, "✓ ANSWER") {
                    call.answer(VideoProfile.STATE_AUDIO_ONLY)
                }
                addButton(controls, "✕ REJECT") {
                    call.disconnect()
                }
            }

            Call.STATE_DIALING, Call.STATE_CONNECTING -> {
                status.text = "CALLING..."

                val row1 = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                }
                addGridButton(row1, "🎙\nMUTE") {
                    muted = !muted
                    JarvisCallManager.service?.setMuted(muted)
                    updateScreen()
                }
                addGridButton(row1, "⌨\nKEYPAD") {
                    keypadVisible = !keypadVisible
                    updateScreen()
                }
                addGridButton(row1, "🔊\nSPEAKER") {
                    speaker = !speaker
                    JarvisCallManager.service?.setAudioRoute(
                        if (speaker) CallAudioState.ROUTE_SPEAKER
                        else CallAudioState.ROUTE_EARPIECE
                    )
                    updateScreen()
                }
                controls.addView(row1)

                val row2 = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                }
                addGridButton(row2, "➕\nADD CALL") {
                    startActivity(Intent(Intent.ACTION_DIAL))
                }
                addGridButton(row2, "👤\nCONTACTS") {
                    startActivity(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI))
                }
                controls.addView(row2)

                if (keypadVisible) showKeypad()

                addButton(controls, "✉  SEND MESSAGE") {
                    if (!phoneNumber.isNullOrBlank()) {
                        startActivity(Intent(
                            Intent.ACTION_SENDTO,
                            Uri.parse("smsto:${Uri.encode(phoneNumber)}")
                        ))
                    }
                }

                addButton(controls, "✕ CANCEL CALL") {
                    call.disconnect()
                }
            }

            Call.STATE_ACTIVE, Call.STATE_HOLDING -> {
                status.text = if (call.state == Call.STATE_HOLDING) "ON HOLD" else "CONNECTED"

                val row1 = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                }
                addGridButton(row1, if (muted) "🎙\nMIC ON" else "🎙\nMUTE") {
                    muted = !muted
                    JarvisCallManager.service?.setMuted(muted)
                    updateScreen()
                }
                addGridButton(row1, "⌨\nKEYPAD") {
                    keypadVisible = !keypadVisible
                    updateScreen()
                }
                addGridButton(row1, if (speaker) "🔈\nEARPIECE" else "🔊\nSPEAKER") {
                    speaker = !speaker
                    JarvisCallManager.service?.setAudioRoute(
                        if (speaker) CallAudioState.ROUTE_SPEAKER
                        else CallAudioState.ROUTE_EARPIECE
                    )
                    updateScreen()
                }
                controls.addView(row1)

                val row2 = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                }
                addGridButton(row2, "➕\nADD CALL") {
                    startActivity(Intent(Intent.ACTION_DIAL))
                }
                addGridButton(row2, "👤\nCONTACTS") {
                    startActivity(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI))
                }
                controls.addView(row2)

                if (keypadVisible) showKeypad()

                addButton(controls, "✉  SEND MESSAGE") {
                    if (!phoneNumber.isNullOrBlank()) {
                        val smsIntent = Intent(
                            Intent.ACTION_SENDTO,
                            Uri.parse("smsto:${Uri.encode(phoneNumber)}")
                        )
                        startActivity(smsIntent)
                    }
                }

                addButton(controls, "✕ END CALL") {
                    call.disconnect()
                }
            }

            Call.STATE_DISCONNECTED -> {
                status.text = "CALL ENDED"
                Handler(Looper.getMainLooper()).postDelayed({
                    if (!isFinishing) finish()
                }, 1200)
            }

            else -> {
                status.text = "CONNECTING..."
                addButton(controls, "✕ END CALL") {
                    call.disconnect()
                }
            }
        }
    }

    private fun showKeypad() {
        val rows = arrayOf(
            arrayOf("1", "2", "3"),
            arrayOf("4", "5", "6"),
            arrayOf("7", "8", "9"),
            arrayOf("*", "0", "#")
        )

        for (rowValues in rows) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            for (digit in rowValues) {
                val button = Button(this).apply {
                    text = digit
                    setTextColor(green)
                    textSize = 20f
                    setOnClickListener {
                        currentCall?.playDtmfTone(digit[0])
                        currentCall?.stopDtmfTone()
                    }
                }
                row.addView(button, LinearLayout.LayoutParams(
                    0, dp(55), 1f
                ))
            }
            keypad.addView(row)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onResume() {
        super.onResume()
        currentCall = JarvisCallManager.activeCall
        currentCall?.registerCallback(callCallback)
        updateScreen()
    }

    override fun onDestroy() {
        currentCall?.unregisterCallback(callCallback)
        super.onDestroy()
    }
}
