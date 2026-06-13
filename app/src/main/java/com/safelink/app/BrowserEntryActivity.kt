package com.safelink.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class BrowserEntryActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target = if (intent.data != null) {
            Intent(this, LinkReviewActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = intent.data
            }
        } else {
            Intent(this, MainActivity::class.java)
        }
        startActivity(target)
        finish()
    }
}
