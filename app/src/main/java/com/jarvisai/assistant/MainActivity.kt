package com.jarvisai.assistant

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.provider.ContactsContract
import android.provider.CallLog
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

    private val CONTACTS_PERMISSION = 201
    private val CALL_LOG_PERMISSION = 202
    private val RECENT_CONTACTS_PERMISSION = 203
    private data class PhoneContact(val name: String, val number: String, val photoUri: String?)
    private data class RecentCall(
        val number: String,
        val name: String?,
        val type: Int,
        val date: Long,
        val duration: Long
    )
    private var allContacts = mutableListOf<PhoneContact>()
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
        handleDialIntent(intent)
    }

    private fun handleDialIntent(incoming: Intent?) {
        if (incoming?.action == Intent.ACTION_DIAL) {
            showDialPad()
            val number = incoming.data?.schemeSpecificPart.orEmpty()
            if (number.isNotEmpty()) dialNumber.text = number
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDialIntent(intent)
    }

    private fun requestDefaultDialer() {
        if (android.os.Build.VERSION.SDK_INT < 29) {
            Toast.makeText(this, "Android version support nahi karta", Toast.LENGTH_LONG).show()
            return
        }

        val roleManager = getSystemService(RoleManager::class.java)

        if (!roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
            Toast.makeText(this, "Default Dialer role available nahi hai", Toast.LENGTH_LONG).show()
            return
        }

        if (roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
            Toast.makeText(this, "Jarvis already Default Phone App hai", Toast.LENGTH_SHORT).show()
            return
        }

        startActivityForResult(
            roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER),
            300
        )
    }

    private fun showHome() {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_CONTACTS),
                RECENT_CONTACTS_PERMISSION
            )
            return
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

        val recent = makeButton("📞  RECENT CALLS")
        val recentParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(48)
        )
        recentParams.topMargin = dp(8)
        root.addView(recent, recentParams)
        recent.setOnClickListener { showRecentCalls() }

        val defaultPhone = makeButton("⚙  SET DEFAULT PHONE APP")
        val defaultParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(48)
        )
        defaultParams.topMargin = dp(8)
        root.addView(defaultPhone, defaultParams)
        defaultPhone.setOnClickListener { requestDefaultDialer() }

        setContentView(root)
    }


    private fun showRecentCalls() {
        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_CALL_LOG),
                CALL_LOG_PERMISSION
            )
            return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(12))
            setBackgroundColor(bg)
        }

        val title = TextView(this).apply {
            text = "RECENT CALLS"
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(green)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(12))
        }
        root.addView(title)

        val back = makeButton("← BACK TO JARVIS")
        root.addView(back, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(46)
        ))
        back.setOnClickListener { showHome() }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, dp(10))
        }
        val scrollView = ScrollView(this)
        scrollView.addView(content)
        root.addView(scrollView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val loading = TextView(this).apply {
            text = "Loading call history..."
            textSize = 15f
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(0, dp(25), 0, dp(25))
        }
        content.addView(loading)
        setContentView(root)

        Thread {
            val calls = mutableListOf<RecentCall>()
            val savedNumbers = mutableSetOf<String>()
            var error: String? = null
            try {
                val projection = arrayOf(
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION
                )
                contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    null,
                    null,
                    CallLog.Calls.DATE + " DESC"
                )?.use { cursor ->
                    val ni = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                    val ci = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                    val ti = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                    val di = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                    val du = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)

                    var count = 0
                    while (cursor.moveToNext() && count < 300) {
                        calls.add(
                            RecentCall(
                                cursor.getString(ni) ?: "Unknown number",
                                cursor.getString(ci),
                                cursor.getInt(ti),
                                cursor.getLong(di),
                                cursor.getLong(du)
                            )
                        )
                        count++
                    }
                }
            } catch (e: Exception) {
                error = e.message ?: "Call history access failed"
            }

            if (checkSelfPermission(Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED) {
                try {
                    val projection = arrayOf(
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )
                    contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        projection,
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        val index = cursor.getColumnIndexOrThrow(
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                        )
                        while (cursor.moveToNext()) {
                            val key = (cursor.getString(index) ?: "")
                                .filter { it.isDigit() }.takeLast(10)
                            if (key.isNotEmpty()) savedNumbers.add(key)
                        }
                    }
                } catch (_: Exception) {
                }
            }

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                content.removeAllViews()

                if (error != null) {
                    addCallHeading(content, "Call history access nahi hui")
                    addCallText(content, error!!)
                    return@runOnUiThread
                }


                val unknown = calls.filter { call ->
                    val key = call.number.filter { it.isDigit() }.takeLast(10)
                    call.name.isNullOrBlank() && key.isNotEmpty() &&
                        !savedNumbers.contains(key)
                }

                addCallHeading(content, "ALL CALLS (${calls.size})")
                if (calls.isEmpty()) addCallText(content, "Abhi koi call history nahi hai.")
                calls.forEach { addRecentCallRow(content, it, false) }

                addCallHeading(content, "UNKNOWN NUMBERS (${unknown.size})")
                if (unknown.isEmpty()) addCallText(content, "Unknown numbers nahi mile.")
                unknown.forEach { addRecentCallRow(content, it, true) }
            }
        }.start()
    }

    private fun addCallHeading(parent: LinearLayout, label: String) {
        val heading = TextView(this).apply {
            text = label
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(green)
            setPadding(0, dp(16), 0, dp(8))
        }
        parent.addView(heading)
    }

    private fun addCallText(parent: LinearLayout, label: String) {
        val view = TextView(this).apply {
            text = label
            textSize = 14f
            setTextColor(white)
            setPadding(0, dp(8), 0, dp(8))
        }
        parent.addView(view)
    }

    private fun addRecentCallRow(
        parent: LinearLayout,
        call: RecentCall,
        isUnknown: Boolean
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = shape(Color.rgb(15, 28, 35), 12)
        }

        val kind = when (call.type) {
            CallLog.Calls.INCOMING_TYPE -> "Incoming"
            CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
            CallLog.Calls.MISSED_TYPE -> "Missed"
            CallLog.Calls.REJECTED_TYPE -> "Rejected"
            CallLog.Calls.BLOCKED_TYPE -> "Blocked"
            else -> "Other"
        }

        val displayName = if (isUnknown) "Unknown Number" else
            call.name?.takeIf { it.isNotBlank() } ?: call.number

        val name = TextView(this).apply {
            text = displayName
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(white)
        }
        card.addView(name)

        val detail = TextView(this).apply {
            val dateText = java.text.SimpleDateFormat(
                "dd MMM yyyy, hh:mm a", Locale.getDefault()
            ).format(java.util.Date(call.date))
            val mins = call.duration / 60
            val secs = call.duration % 60
            text = "$kind  •  ${call.number}\n$dateText  •  ${mins}m ${secs}s"
            textSize = 12f
            setTextColor(Color.rgb(150, 190, 205))
            setPadding(0, dp(5), 0, dp(7))
        }
        card.addView(detail)

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val callButton = makeButton("CALL")
        actions.addView(callButton, LinearLayout.LayoutParams(0, dp(42), 1f))
        callButton.setOnClickListener { makePhoneCall(call.number) }

        if (isUnknown) {
            val saveButton = makeButton("SAVE CONTACT")
            val params = LinearLayout.LayoutParams(0, dp(42), 1f)
            params.leftMargin = dp(8)
            actions.addView(saveButton, params)
            saveButton.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_INSERT).apply {
                        type = ContactsContract.Contacts.CONTENT_TYPE
                        putExtra(ContactsContract.Intents.Insert.PHONE, call.number)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Contact save nahi ho paya", Toast.LENGTH_SHORT).show()
                }
            }
        }

        card.addView(actions)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, dp(4), 0, dp(4))
        parent.addView(card, params)
    }

    private fun showDialPad() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(18), dp(20), dp(15))
            setBackgroundColor(bg)
        }

        root.addView(
            TextView(this),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(95)
            )
        )

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

        val contactsButton = makeButton("👤 CONTACTS")
        root.addView(contactsButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(50)
        ).apply {
            setMargins(dp(5), dp(5), dp(5), dp(10))
        })
        contactsButton.setOnClickListener { showContacts() }

        val back = makeButton("← BACK TO JARVIS")
        root.addView(back, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(48)
        ))
        back.setOnClickListener { showHome() }

        setContentView(root)
    }

    private fun showContacts() {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_CONTACTS),
                CONTACTS_PERMISSION
            )
            return
        }

        allContacts.clear()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(16))
            setBackgroundColor(bg)
        }

        val title = TextView(this).apply {
            text = "👤  JARVIS CONTACTS"
            textSize = 23f
            setTextColor(green)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(18))
        }
        root.addView(title)

        val search = EditText(this).apply {
            hint = "Search name or number..."
            setHintTextColor(Color.GRAY)
            setTextColor(white)
            textSize = 16f
            setSingleLine(true)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setBackgroundColor(Color.rgb(15, 28, 45))
        }
        root.addView(search, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(50)
        ))

        val count = TextView(this).apply {
            setTextColor(green)
            textSize = 13f
            setPadding(dp(4), dp(10), 0, dp(8))
        }
        root.addView(count)

        val listScroll = ScrollView(this)
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        listScroll.addView(list)
        root.addView(listScroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val back = makeButton("← BACK TO DIAL PAD")
        root.addView(back, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(48)
        ))
        back.setOnClickListener { showDialPad() }

        fun renderContacts(filter: String) {
            list.removeAllViews()
            val results = allContacts.filter {
                it.name.contains(filter, ignoreCase = true) ||
                    it.number.contains(filter, ignoreCase = true)
            }
            count.text = "${results.size} CONTACTS"

            if (results.isEmpty()) {
                val empty = TextView(this).apply {
                    text = if (allContacts.isEmpty()) {
                        "No saved phone contacts found."
                    } else {
                        "No matching contacts found."
                    }
                    setTextColor(white)
                    gravity = Gravity.CENTER
                    setPadding(dp(10), dp(35), dp(10), dp(35))
                }
                list.addView(empty)
            }

            for (contact in results) {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(10), dp(9), dp(10), dp(9))
                    setBackgroundColor(Color.rgb(7, 19, 39))
                }

                val photo = ImageView(this).apply {
                    if (!contact.photoUri.isNullOrEmpty()) {
                        try {
                            setImageURI(Uri.parse(contact.photoUri))
                        } catch (_: Exception) {
                            setImageResource(android.R.drawable.sym_def_app_icon)
                        }
                    } else {
                        setImageResource(android.R.drawable.sym_def_app_icon)
                    }
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }
                row.addView(photo, LinearLayout.LayoutParams(dp(48), dp(48)))

                val details = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(12), 0, 0, 0)
                }
                val name = TextView(this).apply {
                    text = contact.name
                    textSize = 16f
                    setTextColor(white)
                    typeface = Typeface.DEFAULT_BOLD
                }
                val number = TextView(this).apply {
                    text = contact.number
                    textSize = 14f
                    setTextColor(Color.rgb(130, 183, 225))
                }
                details.addView(name)
                details.addView(number)
                row.addView(details, LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                ))

                row.setOnClickListener { makePhoneCall(contact.number) }

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, dp(5), 0, dp(5))
                list.addView(row, params)
            }
        }

        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                renderContacts(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        count.text = "Loading contacts..."
        setContentView(root)

        Thread {
            val unique = linkedMapOf<String, PhoneContact>()
            var loadError: String? = null

            try {
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_URI
                )

                contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    null,
                    null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE NOCASE ASC"
                )?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                    )
                    val numberIndex = cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )
                    val photoIndex = cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.PHOTO_URI
                    )

                    while (cursor.moveToNext()) {
                        val name = cursor.getString(nameIndex) ?: "Unknown"
                        val number = cursor.getString(numberIndex) ?: continue
                        val photo = if (photoIndex >= 0) cursor.getString(photoIndex) else null
                        val key = number.filter { it.isDigit() }

                        if (key.isNotEmpty() && !unique.containsKey(key)) {
                            unique[key] = PhoneContact(name, number, photo)
                        }
                    }
                }
            } catch (e: Exception) {
                loadError = e.message ?: "Unknown error"
            }

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread

                allContacts.clear()
                allContacts.addAll(unique.values)
                renderContacts(search.text.toString())

                if (loadError != null) {
                    Toast.makeText(
                        this,
                        "Contacts load nahi hue: $loadError",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
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
        if (requestCode == CALL_LOG_PERMISSION) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showRecentCalls()
            } else {
                Toast.makeText(this, "Call history permission nahi mili", Toast.LENGTH_LONG).show()
            }
            return
        }
        if (requestCode == RECENT_CONTACTS_PERMISSION) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showRecentCalls()
            } else {
                Toast.makeText(this, "Saved contacts pehchanne ki permission nahi mili", Toast.LENGTH_LONG).show()
            }
            return
        }
        if (requestCode == CONTACTS_PERMISSION) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showContacts()
            } else {
                Toast.makeText(this, "Contacts permission nahi mili", Toast.LENGTH_SHORT).show()
            }
            return
        }
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
