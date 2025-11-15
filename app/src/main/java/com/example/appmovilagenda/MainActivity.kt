package com.example.appmovilagenda

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

// Redirige inmediatamente a InicioTareas si alguien abre esta Activity.

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, InicioTareas::class.java))
        finish()
    }
}