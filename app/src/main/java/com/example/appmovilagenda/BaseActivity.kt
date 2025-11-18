package com.example.appmovilagenda

import android.content.Context
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        // Forzamos español de Chile para toda la app
        val ctx = LocaleUtils.applyLocale(newBase, "es-CL")
        super.attachBaseContext(ctx)
    }
}