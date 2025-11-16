package com.example.appmovilagenda

import com.google.firebase.Timestamp

data class Tarea(
    var id: String = "",
    var titulo: String = "",
    var descripcion: String = "",
    var fecha: String = "",            // dd-MM-yyyy
    var hora: String = "",             // HH:mm (hora de la tarea)
    var recordatorioMillis: Long? = null, // momento exacto del recordatorio
    var createdAt: Timestamp? = null,
    var userId: String = ""
)