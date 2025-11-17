package com.example.appmovilagenda

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SemanaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_semana)
        title = "Semana"
    }
}