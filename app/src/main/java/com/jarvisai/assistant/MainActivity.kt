package com.jarvisai.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.widget.*
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var chat: LinearLayout
    private lateinit var input: EditText
    private lateinit var scroll: ScrollView
    private lateinit var dialNumber: TextView
    private var tts: TextToSpeech? = null
    private var pendingNumber = ""

    private val bg = Color.rgb(5, 9, 15)
    private val green = Color.rgb(0, 255, 150)
    private val white = Color.WHITE
    private val CALL_PERMISSION = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg

        tts = TextToSpeech(this) {
            if (it == TextToSpeech.SUCCESS) {
                tts?.language = Locale("hi", "IN")
            }
        }
        showHome()
    }

    private fun showHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(15), dp(18), dp(12))
            setBackgroundColor(bg)
        }

        val header = TextView(this).apply {
            text = "JARVIS AI"
            textSize = 27f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(green)
            gravity = Gravity.CENTER
            setPadding(0, dp(15), 0, dp(5))
        }
        root.addView(header)

        val status = TextView(this).apply {
            text = "● YOUR PERSONAL AI ASSISTANT"
            textSize = 11f
            setTextColor(green)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(15))
        }
        root.addView(status)

        val orb = TextView(this).apply {
            text = "◉"
            textSize = 65f
            setTextColor(green)
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(10))
        }
        root.addView(orb)

        val welcome = TextView(this).apply {
            text = "Hello! Main Jarvis hoon.\nAapki kya madad kar sakta hoon?"
            textSize = 16f
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(0, dp(5), 0, dp(15))
        }
        root.addView(welcome)

        scroll = ScrollView(this)
        chat = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(5), dp(5), dp(5), dp(10))
        }
        scroll.addView(chat)
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        input = EditText(this).apply {
            hint = "Message Jarvis..."
            setHintTextColor(Color.GRAY)
            setTextColor(white)
            textSize = 15f
            setSingleLine(true)
            setPadding(dp(12), 0, dp(8), 0)
            background = shape(Color.rgb(22, 30, 39), 18)
        }

        val send = makeButton("SEND")
        val mic = makeButton("🎙")

        row.addView(input, LinearLayout.LayoutParams(0, dp(52), 1f))
        row.addView(mic, LinearLayout.LayoutParams(dp(58), dp(52)))
        row.addView(send, LinearLayout.LayoutParams(dp(70), dp(52)))
        root.addView(row)

        val call = makeButton("☎  JARVIS DIAL PAD")
        val callParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(50)
        )
        callParams.topMargin = dp(10)
        root.addView(call, callParams)

        send.setOnClickListener { sendMessage() }
        input.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }
        mic.setOnClickListener { startVoiceInput() }
        call.setOnClickListener { showDialPad() }

        setContentView(root)
    }

    private fun showDialPad() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(18), dp(20), dp(15))
            setBackgroundColor(bg)
        }

        val title = TextView(this).apply {
            text = "JARVIS PHONE"
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(green)
            gravity = Gravity.CENTER
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "IN-APP DIAL PAD"
            textSize = 11f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }
        root.addView(subtitle)

        dialNumber = TextView(this).apply {
            text = ""
            hint = "Enter phone number"
            textSize = 27f
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(dp(5), dp(25), dp(5), dp(25))
            isSingleLine = true
        }
        root.addView(dialNumber, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(85)
        ))

        val keys = arrayOf(
            arrayOf("1", "2", "3"),
            arrayOf("4", "5", "6"),
            arrayOf("7", "8", "9"),
            arrayOf("*", "0", "#")
        )

        for (keyRow in keys) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            for (key in keyRow) {
                val button = makeButton(key).apply {
                    textSize = 24f
                }
                val params = LinearLayout.LayoutParams(0, dp(65), 1f)
                params.setMargins(dp(5), dp(5), dp(5), dp(5))
                row.addView(button, params)
                button.setOnClickListener {
                    dialNumber.append(key)
                }
            }
            root.addView(row)
        }

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val delete = makeButton("⌫ DELETE")
        val call = makeButton("☎ CALL")
        val deleteParams = LinearLayout.LayoutParams(0, dp(55), 1f)
        deleteParams.setMargins(dp(5), dp(10), dp(5), dp(10))
        val callParams = LinearLayout.LayoutParams(0, dp(55), 1f)
        callParams.setMargins(dp(5), dp(10), dp(5), dp(10))

        actions.addView(delete, deleteParams)
        actions.addView(call, callParams)
        root.addView(actions)

        delete.setOnClickListener {
            val current = dialNumber.text.toString()
            if (current.isNotEmpty()) {
                dialNumber.text = current.dropLast(1)
            }
        }

        call.setOnClickListener {
            makePhoneCall(dialNumber.text.toString())
        }

        val back = makeButton("← BACK TO JARVIS")
        root.addView(back, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(48)
        ))
        back.setOnClickListener { showHome() }

        setContentView(root)
    }

    private fun makePhoneCall(number: String) {
        val cleanNumber = number.trim()
        if (cleanNumber.isEmpty()) {
            Toast.makeText(this, "Pehle number enter karein", Toast.LENGTH_SHORT).show()
            return
        }

        pendingNumber = cleanNumber

        if (checkSelfPermission(Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.CALL_PHONE),
                CALL_PERMISSION
            )
            return
        }

        try {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanNumber"))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Call nahi ho payi: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Deprecated Android callback")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CALL_PERMISSION) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                makePhoneCall(pendingNumber)
            } else {
                Toast.makeText(this, "Call permission nahi mili", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendMessage() {
        val message = input.text.toString().trim()
        if (message.isEmpty()) return

        addMessage("YOU: $message", green)
        input.setText("")

        val reply = "Main Jarvis hoon. Aapka message mila hai. " +
                "Asli AI service connect hone ke baad main aapke sawalon ka jawab de sakunga."

        addMessage("JARVIS: $reply", white)
        tts?.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply")
    }

    private fun addMessage(message: String, color: Int) {
        val text = TextView(this).apply {
            this.text = message
            textSize = 15f
            setTextColor(color)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = shape(Color.rgb(18, 27, 35), 14)
        }
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, dp(5), 0, dp(5))
        chat.addView(text, params)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun startVoiceInput() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Jarvis se boliye")
            startActivityForResult(intent, 100)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice input available nahi hai", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated Android callback")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            input.setText(result?.firstOrNull() ?: "")
            sendMessage()
        }
    }

    private fun makeButton(label: String): Button {
        return Button(this).apply {
            text = label
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(green)
            background = shape(Color.rgb(20, 38, 39), 14)
            isAllCaps = false
        }
    }

    private fun shape(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), green)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
