package com.example.appmovilagenda

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        val ctx = LocaleUtils.applyLocale(newBase, "es-CL")
        super.attachBaseContext(ctx)
    }

    // Única función para cerrar sesión
    protected fun performLogout() {
        // Cierra sesión en Firebase y limpia flag local
        FirebaseAuth.getInstance().signOut()
        SessionManager.setLoggedIn(this, false)

        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }
}