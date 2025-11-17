package com.example.appmovilagenda

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Fuerza la app a español
        val appLocale = LocaleListCompat.forLanguageTags("es")
        AppCompatDelegate.setApplicationLocales(appLocale)
    }
}