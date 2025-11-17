package com.example.appmovilagenda

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DiaActivity : AppCompatActivity() {

    private lateinit var chipMes: TextView
    private lateinit var chipSemana: TextView
    private lateinit var chipDia: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dia)
        title = "Día"

        chipMes = findViewById(R.id.chipMes)
        chipSemana = findViewById(R.id.chipSemana)
        chipDia = findViewById(R.id.chipDia)

        // Esta pantalla representa "Dia"
        setChipSelected(chipMes, false)
        setChipSelected(chipSemana, false)
        setChipSelected(chipDia, true)

        chipMes.setOnClickListener {
            startActivity(Intent(this, CalendarioActivity::class.java))
            // finish() si quieres que el back no regrese aquí
        }
        chipSemana.setOnClickListener {
            startActivity(Intent(this, SemanaActivity::class.java))
            // finish()
        }
        chipDia.setOnClickListener {
            // Ya estás en Dia
        }
    }

    private fun setChipSelected(view: TextView, selected: Boolean) {
        if (selected) {
            view.setBackgroundResource(R.drawable.chip_brown)
            view.setTextColor(0xFF6B4E2E.toInt())
        } else {
            view.setBackgroundResource(R.drawable.chip_green)
            view.setTextColor(0xFF1F4226.toInt())
        }
    }
}