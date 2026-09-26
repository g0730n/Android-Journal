package com.example.journalappv1

import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LockActivity : AppCompatActivity() {

    private lateinit var pinEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val density = resources.displayMetrics.density
        val pad = (32 * density).toInt()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
        }

        val title = TextView(this).apply {
            text = "Enter PIN"
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, pad)
        }

        pinEditText = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            gravity = Gravity.CENTER
        }

        val unlockButton = Button(this).apply {
            text = "Unlock"
            setOnClickListener { attemptUnlock() }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = pad; gravity = Gravity.CENTER }
            layoutParams = lp
        }

        layout.addView(title)
        layout.addView(pinEditText)
        layout.addView(unlockButton)
        setContentView(layout)
    }

    private fun attemptUnlock() {
        val entered = pinEditText.text.toString()
        if (entered.isNotEmpty() && PinManager.verifyPin(this, entered)) {
            AppLockState.isUnlocked = true
            setResult(RESULT_OK)
            finish()
        } else {
            pinEditText.text.clear()
            Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        // Don't let back-press fall through into the unlocked journal.
        moveTaskToBack(true)
    }
}