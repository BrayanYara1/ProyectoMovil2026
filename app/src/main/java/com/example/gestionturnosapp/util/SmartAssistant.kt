package com.example.gestionturnosapp.util

import android.content.Context
import com.example.gestionturnosapp.R
import com.example.gestionturnosapp.data.Medicamento
import com.example.gestionturnosapp.data.Turno

object SmartAssistant {

    fun generateResponse(context: Context, query: String, turnos: List<Turno>, meds: List<Medicamento>): String {
        val q = query.lowercase()

        return when {
            q.contains("hola") || q.contains("buenos días") || q.contains("buenas tardes") -> 
                context.getString(R.string.ai_welcome)

            q.contains("cita") || q.contains("turno") || q.contains("cuando") -> {
                val next = turnos.asSequence()
                    .filter { it.estado.lowercase() in listOf("pendiente", "pending") }
                    .minByOrNull { "${it.fecha} ${it.hora}" }

                if (next != null) {
                    context.getString(R.string.ai_next_appointment, 
                        next.especialidad ?: context.getString(R.string.label_default_specialty), 
                        next.doctor ?: context.getString(R.string.label_default_doctor), 
                        next.fecha, 
                        next.hora)
                } else {
                    context.getString(R.string.ai_no_appointments)
                }
            }

            q.contains("medicamento") || q.contains("pastilla") || q.contains("toma") -> {
                if (meds.isNotEmpty()) {
                    val medList = meds.joinToString("\n") { "- ${it.nombre} (${it.dosis}): ${it.frecuencia}" }
                    context.getString(R.string.ai_meds_list, medList)
                } else {
                    context.getString(R.string.ai_no_meds)
                }
            }

            q.contains("qué es") || q.contains("que es") || q.contains("significa") -> {
                explainMedicalTerm(context, q)
            }

            q.contains("gracias") || q.contains("gracia") -> 
                context.getString(R.string.ai_thanks_reply)

            else -> context.getString(R.string.ai_generic_recommendation)
        }
    }

    private fun explainMedicalTerm(context: Context, query: String): String {
        return when {
            query.contains("ecografía") -> context.getString(R.string.ai_term_ecografia)
            query.contains("ayunas") -> context.getString(R.string.ai_term_ayunas)
            query.contains("presión") || query.contains("tensión") -> context.getString(R.string.ai_term_presion)
            query.contains("diabetes") -> context.getString(R.string.ai_term_diabetes)
            else -> context.getString(R.string.ai_term_generic)
        }
    }
}
