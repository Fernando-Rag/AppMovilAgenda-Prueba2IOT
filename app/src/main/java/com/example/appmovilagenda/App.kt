package com.example.appmovilagenda

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.firebase.FirebaseApp

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // Inicializa Firebase lo antes posible
        FirebaseApp.initializeApp(this)

        // Fuerza la app a español
        val appLocale = LocaleListCompat.forLanguageTags("es")
        AppCompatDelegate.setApplicationLocales(appLocale)
    }
}