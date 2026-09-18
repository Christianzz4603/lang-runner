package com.langrunner.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Plain, dependency-free crash screen shown instead of the system "has
 * stopped" dialog. Deliberately avoids view binding, app themes, and app
 * resources, since those might be involved in whatever just crashed — this
 * has to work even when everything else in the app is broken.
 */
class CrashActivity : Activity() {

    companion object {
        const val EXTRA_TRACE = "trace"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val trace = intent.getStringExtra(EXTRA_TRACE) ?: "Unknown crash (no stack trace captured)"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(32, 64, 32, 32)
        }

        val title = TextView(this).apply {
            text = "Lang Runner crashed"
            setTextColor(Color.rgb(255, 100, 100))
            textSize = 18f
            setPadding(0, 0, 0, 16)
        }

        val subtitle = TextView(this).apply {
            text = "Copy this and send it back so the bug can be fixed:"
            setTextColor(Color.LTGRAY)
            textSize = 13f
            setPadding(0, 0, 0, 16)
        }

        val traceView = TextView(this).apply {
            text = trace
            setTextColor(Color.GREEN)
            setTextIsSelectable(true)
            typeface = Typeface.MONOSPACE
            textSize = 11f
        }

        val scroll = ScrollView(this).apply {
            addView(traceView)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }

        val copyButton = Button(this).apply {
            text = "Copy crash log"
            setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("crash log", trace))
                Toast.makeText(this@CrashActivity, "Copied", Toast.LENGTH_SHORT).show()
            }
        }

        val restartButton = Button(this).apply {
            text = "Restart app"
            setOnClickListener {
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(launchIntent)
                finish()
            }
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(scroll)
        root.addView(copyButton)
        root.addView(restartButton)

        setContentView(root)
    }
}
