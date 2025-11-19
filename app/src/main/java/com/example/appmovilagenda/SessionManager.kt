package com.example.appmovilagenda

import android.content.Context
import android.content.SharedPreferences

object SessionManager {
    private const val PREF_NAME = "session_prefs"
    private const val KEY_LOGGED_IN = "logged_in"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun setLoggedIn(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_LOGGED_IN, value).apply()
    }

    fun isLoggedIn(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_LOGGED_IN, false)
}