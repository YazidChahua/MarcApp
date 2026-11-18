package com.marcapp.app.core.notifications

import com.marcapp.app.core.notifications.ProgramadorNotificaciones.MOMENTO_ALISTARSE
import com.marcapp.app.core.notifications.ProgramadorNotificaciones.MOMENTO_ANTES

/**
 * Textos de las notificaciones. Cada una tiene un título corto y un contenido cálido.
 * A la hora exacta: mensaje de ánimo si el trabajador ya marcó, recordatorio si aún no.
 */
object MensajesNotificacion {

    fun componer(tipo: String, momento: String, minAntes: Int, yaMarco: Boolean): Pair<String, String> =
        when (momento) {
            MOMENTO_ALISTARSE -> "¡Arriba, campeón! 🌅" to
                "Tu turno empieza pronto. Ve alistándote con calma para llegar fresco y a tiempo."
            MOMENTO_ANTES -> antes(tipo, minAntes)
            else -> hora(tipo, yaMarco)
        }

    private fun antes(tipo: String, min: Int): Pair<String, String> = when (tipo) {
        "ENTRADA" -> "Faltan $min min para tu entrada ⏰" to
            "Ten listo el celular para marcar tu asistencia apenas llegues. ¡Tú puedes! 💪"
        "SALIDA_REFRIGERIO" -> "Tu refrigerio se acerca 🍽️" to
            "En $min min podrás marcar tu salida a refrigerio. Ve cerrando lo que tengas en mano."
        "REGRESO_REFRIGERIO" -> "El refrigerio termina pronto ⏳" to
            "En $min min marca tu retorno para no pasarte del tiempo. ¡Con energía renovada!"
        "SALIDA" -> "Tu jornada casi termina 🏁" to
            "En $min min podrás marcar tu salida. Ve guardando tus cosas con calma."
        else -> "Recordatorio ⏰" to "Prepárate para tu próxima marcación."
    }

    private fun hora(tipo: String, yaMarco: Boolean): Pair<String, String> = when (tipo) {
        "ENTRADA" ->
            if (yaMarco) "¡Buen día! ☀️ Gracias por marcar" to
                "Ya registraste tu entrada. Que tengas una jornada excelente. ¡A darlo todo! 💚"
            else "¡Buen día! ☀️" to
                "Es hora de marcar tu entrada. No lo olvides para empezar con el pie derecho 🙌"
        "SALIDA_REFRIGERIO" ->
            if (yaMarco) "¡Buen provecho! 🍽️" to
                "Disfruta tu refrigerio, te lo ganaste. Recarga energías para la segunda mitad."
            else "Hora de tu refrigerio 🍽️" to
                "Marca tu salida a refrigerio y ve a recargar energías. ¡Te lo mereces!"
        "REGRESO_REFRIGERIO" ->
            if (yaMarco) "¡De vuelta con todo! 🔋" to
                "Gracias por volver a tiempo. ¡A seguir con la mejor actitud!"
            else "Fin del refrigerio ⏳" to
                "Marca tu retorno para continuar tu jornada. ¡Vamos que se puede!"
        "SALIDA" ->
            if (yaMarco) "¡Excelente jornada! 🎉" to
                "Registraste tu salida. Descansa y desconéctate, te lo ganaste. Hasta mañana 👋"
            else "Fin de jornada 🏁" to
                "No olvides marcar tu salida antes de irte. ¡Gran trabajo hoy!"
        else -> "MarcApp" to "Tienes una marcación pendiente."
    }
}
