package com.kickstra.presentation

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Trampoline activity launched from the playback notification. It forces the task out of
 * Picture-in-Picture (since this Activity does not support PiP) and forwards all extras
 * to MainActivity, then finishes immediately.
 */
class ExpandNowPlayingActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val forward = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            intent?.extras?.let { putExtras(it) }
        }
        startActivity(forward)
        finish()
    }
}
