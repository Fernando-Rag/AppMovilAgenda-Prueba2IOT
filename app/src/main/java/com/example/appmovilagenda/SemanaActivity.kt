package com.example.appmovilagenda

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SemanaActivity : AppCompatActivity() {

    private lateinit var chipMes: TextView
    private lateinit var chipSemana: TextView
    private lateinit var chipDia: TextView
    private val dbg = "SEMANA_DBG"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_semana)
        title = "Semana"

        chipMes = findViewById(R.id.chipMes)
        chipSemana = findViewById(R.id.chipSemana)
        chipDia = findViewById(R.id.chipDia)

        Log.d(dbg, "Refs => mes=$chipMes semana=$chipSemana dia=$chipDia")

        setChipSelected(chipMes, false)
        setChipSelected(chipSemana, true)
        setChipSelected(chipDia, false)

        chipMes.setOnClickListener {
            Log.d(dbg, "Click chipMes -> Calendario")
            startActivity(Intent(this, CalendarioActivity::class.java))
        }
        chipSemana.setOnClickListener {
            Log.d(dbg, "Click chipSemana (ya aquí)")
            // no navega
        }
        chipDia.setOnClickListener {
            Log.d(dbg, "Click chipDia -> Dia")
            startActivity(Intent(this, DiaActivity::class.java))
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