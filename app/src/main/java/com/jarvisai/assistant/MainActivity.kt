
package com.jarvisai.assistant

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
    private var tts: TextToSpeech? = null

    private val bg = Color.rgb(5, 9, 15)
    private val green = Color.rgb(0, 255, 150)
    private val white = Color.WHITE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bg
        window.navigationBarColor = bg

        tts = TextToSpeech(this) {
            if (it == TextToSpeech.SUCCESS) {
                tts?.language = Locale("hi", "IN")
            }
        }

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
            text = "●  YOUR PERSONAL AI ASSISTANT"
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
        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        )

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        input = EditText(this).apply {
            hint = "Message Jarvis..."
            setHintTextColor(Color.GRAY)
            setTextColor(white)
            textSize = 15f
            singleLine = true
            setPadding(dp(12), 0, dp(8), 0)
            background = shape(Color.rgb(22, 30, 39), 18)
        }

        val send = makeButton("SEND")
        val mic = makeButton("🎙")

        row.addView(input, LinearLayout.LayoutParams(0, dp(52), 1f))
        row.addView(mic, LinearLayout.LayoutParams(dp(58), dp(52)))
        row.addView(send, LinearLayout.LayoutParams(dp(70), dp(52)))
        root.addView(row)

        val call = makeButton("☎  OPEN PHONE DIALER")
        val callParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(48)
        )
        callParams.topMargin = dp(10)
        root.addView(call, callParams)

        send.setOnClickListener { sendMessage() }
        input.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }

        mic.setOnClickListener { startVoiceInput() }

        call.setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL))
        }

        setContentView(root)
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
            val result = data?.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS
            )
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
