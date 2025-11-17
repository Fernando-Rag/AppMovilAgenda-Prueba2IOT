package com.example.appmovilagenda

import android.content.Context
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        val ctx = LocaleUtils.applyLocale(newBase, "es")
        super.attachBaseContext(ctx)
    }
}