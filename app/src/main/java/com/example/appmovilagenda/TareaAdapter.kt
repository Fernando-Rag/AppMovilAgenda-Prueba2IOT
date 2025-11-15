package com.example.appmovilagenda

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TareaAdapter(
    private var tareas: List<Tarea>,
    private val onItemClick: (Tarea) -> Unit = {}
) : RecyclerView.Adapter<TareaAdapter.TareaViewHolder>() {

    inner class TareaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtTitulo: TextView = itemView.findViewById(R.id.txtTitulo)
        private val txtDescripcion: TextView = itemView.findViewById(R.id.txtDescripcion)
        private val txtFecha: TextView = itemView.findViewById(R.id.txtFecha)
        private val txtHora: TextView = itemView.findViewById(R.id.txtHora)

        fun bind(tarea: Tarea) {
            txtTitulo.text = tarea.titulo
            txtDescripcion.text = tarea.descripcion
            txtFecha.text = "Fecha: ${tarea.fecha}"
            txtHora.text = "Hora: ${tarea.hora}"
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
}