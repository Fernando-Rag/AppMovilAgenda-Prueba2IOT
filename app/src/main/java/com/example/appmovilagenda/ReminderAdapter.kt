package com.example.appmovilagenda

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// Usa el mismo layout item_tarea para mantener diseño consistente
class ReminderAdapter(
    private var items: List<Tarea>
) : RecyclerView.Adapter<ReminderAdapter.VH>() {

    private val sdfFecha = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    private val sdfHora = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val sdfRec = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val txtTitulo: TextView = v.findViewById(R.id.txtTitulo)
        private val txtDescripcion: TextView = v.findViewById(R.id.txtDescripcion)
        private val txtFecha: TextView = v.findViewById(R.id.txtFecha)
        private val txtHoraExtra: TextView? = v.findViewById(R.id.txtHora)
        private val txtRecordatorio: TextView? = v.findViewById(R.id.txtRecordatorio)

        fun bind(t: Tarea) {
            txtTitulo.text = t.titulo
            txtDescripcion.text = t.descripcion

            // Entrega (si no hay hora, asumimos 23:59)
            val entregaLabel = buildEntregaLabel(t.fecha, t.hora)
            txtFecha.text = entregaLabel
            if (t.hora.isBlank()) txtHoraExtra?.visibility = View.GONE else {
                txtHoraExtra?.visibility = View.VISIBLE
                txtHoraExtra?.text = "Hora: ${t.hora}"
            }

            // Recordatorio
            val rec = t.recordatorioMillis
            val recStr = if (rec != null && rec > 0) sdfRec.format(Date(rec)) else "—"

            // Tiempo restante hacia la entrega
            val dueMillis = parseDueMillis(t.fecha, t.hora)
            val left = if (dueMillis != null) formatLeft(dueMillis - System.currentTimeMillis()) else "—"

            txtRecordatorio?.apply {
                visibility = View.VISIBLE
                text = "Recordatorio: $recStr • Falta: $left"
            }
        }

        private fun buildEntregaLabel(fecha: String, hora: String): String {
            val base = if (fecha.isNotBlank()) "Entrega: $fecha" else "Entrega: —"
            val h = if (hora.isNotBlank()) " - $hora" else ""
            return base + h
        }

        private fun parseDueMillis(fecha: String, hora: String): Long? {
            if (fecha.isBlank()) return null
            return try {
                val p = fecha.split("-")
                val d = p[0].toInt(); val m = p[1].toInt() - 1; val y = p[2].toInt()
                val cal = Calendar.getInstance()
                if (hora.isBlank()) cal.set(y, m, d, 23, 59, 0)
                else {
                    val h = hora.split(":")
                    cal.set(y, m, d, h[0].toInt(), h[1].toInt(), 0)
                }
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            } catch (_: Exception) { null }
        }

        private fun formatLeft(diffMs: Long): String {
            val neg = diffMs < 0
            val absMs = abs(diffMs)
            val min = absMs / 60000
            val d = min / (60 * 24)
            val h = (min % (60 * 24)) / 60
            val m = min % 60
            val base = "${d}d ${h}h ${m}m"
            return if (neg) "venció hace $base" else "$base"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_tarea, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    fun update(nuevos: List<Tarea>) {
        items = nuevos
        notifyDataSetChanged()
    }
}