package com.example.appmovilagenda

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TareaAdapter(
    private var tareas: List<Tarea>,
    private val onItemClick: (Tarea) -> Unit = {}
) : RecyclerView.Adapter<TareaAdapter.TareaViewHolder>() {

    private val sdfHora = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val sdfFecha = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())

    inner class TareaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtTitulo: TextView = itemView.findViewById(R.id.txtTitulo)
        private val txtDescripcion: TextView = itemView.findViewById(R.id.txtDescripcion)
        private val txtFecha: TextView = itemView.findViewById(R.id.txtFecha)
        private val txtHoraExtra: TextView? = itemView.findViewById(R.id.txtHora) // si tienes este TextView
        private val txtRecordatorio: TextView? = itemView.findViewById(R.id.txtRecordatorio) // opcional agregar en layout

        fun bind(tarea: Tarea) {
            txtTitulo.text = tarea.titulo
            txtDescripcion.text = tarea.descripcion

            val baseFecha = "Fecha: ${tarea.fecha}"
            val horaPart = if (tarea.hora.isNotBlank()) " - ${tarea.hora}" else ""
            txtFecha.text = baseFecha + horaPart

            txtHoraExtra?.let {
                if (tarea.hora.isBlank()) it.visibility = View.GONE
                else {
                    it.visibility = View.VISIBLE
                    it.text = "Hora: ${tarea.hora}"
                }
            }

            txtRecordatorio?.let {
                val millis = tarea.recordatorioMillis
                if (millis != null && millis > 0) {
                    val fechaStr = sdfFecha.format(Date(millis))
                    it.visibility = View.VISIBLE
                    it.text = "Recordatorio: $fechaStr"
                } else it.visibility = View.GONE
            }

            itemView.setOnClickListener { onItemClick(tarea) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TareaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tarea, parent, false)
        return TareaViewHolder(view)
    }

    override fun onBindViewHolder(holder: TareaViewHolder, position: Int) {
        holder.bind(tareas[position])
    }

    override fun getItemCount(): Int = tareas.size

    fun actualizarLista(nuevas: List<Tarea>) {
        tareas = nuevas
        notifyDataSetChanged()
    }

    fun getItemAt(position: Int): Tarea = tareas[position]
}