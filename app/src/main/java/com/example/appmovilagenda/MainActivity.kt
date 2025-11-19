package com.example.appmovilagenda

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

// Activity de arranque: decide según sesión, esperando a Firebase si es necesario.
class MainActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private var decided = false

    private val listener = FirebaseAuth.AuthStateListener { fa ->
        if (decided) return@AuthStateListener
        val target = if (fa.currentUser != null)
            InicioTareasActivity::class.java
        else
            LoginActivity::class.java
        decided = true
        startActivity(Intent(this, target))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No hace falta layout; esta activity solo redirige.
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(listener)
    }

    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(listener)
    }
}