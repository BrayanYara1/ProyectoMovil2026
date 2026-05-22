package com.example.gestionturnosapp.util

import com.example.gestionturnosapp.data.Medicamento
import com.example.gestionturnosapp.data.Turno

object SmartAssistant {

    fun generateResponse(query: String, turnos: List<Turno>, meds: List<Medicamento>): String {
        val q = query.lowercase()

        return when {
            q.contains("hola") || q.contains("buenos días") || q.contains("buenas tardes") -> 
                "¡Hola! Soy tu asistente de Salud Activa. ¿En qué puedo ayudarte hoy? Puedo explicarte términos médicos, recordarte tus citas o ayudarte con tus medicamentos."

            q.contains("cita") || q.contains("turno") || q.contains("cuando") -> {
                val next = turnos.firstOrNull { it.estado.lowercase() in listOf("pendiente", "pending") }
                if (next != null) {
                    "Tu próxima cita es de ${next.especialidad} con el ${next.doctor} el día ${next.fecha} a las ${next.hora}. ¿Te gustaría que te de algunas recomendaciones para ese día?"
                } else {
                    "No tienes citas pendientes por ahora. ¿Quieres que te ayude a agendar una?"
                }
            }

            q.contains("medicamento") || q.contains("pastilla") || q.contains("toma") -> {
                if (meds.isNotEmpty()) {
                    val medList = meds.joinToString("\n") { "- ${it.nombre} (${it.dosis}): ${it.frecuencia}" }
                    "Actualmente tienes registrados estos medicamentos:\n$medList\nRecuerda seguir siempre las indicaciones de tu médico."
                } else {
                    "No tienes medicamentos registrados. Si estás tomando algo, puedes agregarlo en la sección de Medicamentos."
                }
            }

            q.contains("qué es") || q.contains("que es") || q.contains("significa") -> {
                explainMedicalTerm(q)
            }

            q.contains("gracias") || q.contains("gracia") -> 
                "¡De nada! Estoy aquí para cuidar de tu salud. ¿Necesitas algo más?"

            else -> "Interesante pregunta. Como asistente de salud, te recomiendo consultar siempre con un profesional para un diagnóstico preciso. ¿Quieres que te ayude a buscar un especialista?"
        }
    }

    private fun explainMedicalTerm(query: String): String {
        return when {
            query.contains("ecografía") -> "Una ecografía es un procedimiento que usa ondas de sonido de alta frecuencia para ver órganos y estructuras dentro del cuerpo."
            query.contains("ayunas") -> "Estar en ayunas significa no ingerir alimentos ni bebidas (excepto agua) por un periodo, usualmente de 8 a 12 horas, antes de un examen."
            query.contains("presión") || query.contains("tensión") -> "La presión arterial es la fuerza de su sangre al empujar contra las paredes de sus arterias."
            query.contains("diabetes") -> "La diabetes es una enfermedad en la que los niveles de glucosa (azúcar) de la sangre están muy altos."
            else -> "Ese es un término técnico importante. Por seguridad, te sugiero consultarlo directamente con tu médico en tu próxima cita para que te explique cómo aplica a tu caso particular."
        }
    }
}
