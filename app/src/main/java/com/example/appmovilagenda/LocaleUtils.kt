package com.example.appmovilagenda

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleUtils {
    fun applyLocale(base: Context, langTag: String): Context {
        val locale = Locale.forLanguageTag(langTag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}