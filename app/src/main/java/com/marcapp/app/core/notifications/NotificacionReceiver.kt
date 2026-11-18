package com.marcapp.app.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.LocalDate

/**
 * Recibe las alarmas agendadas y muestra la notificación con el mensaje correcto.
 * A la hora exacta de cada evento decide entre un mensaje de ÁNIMO (si el trabajador ya marcó)
 * o un RECORDATORIO (si todavía no). También reprograma los avisos tras reiniciar el equipo.
 */
class NotificacionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Tras un reinicio, las alarmas se pierden: se vuelven a agendar las de hoy.
            ProgramadorNotificaciones.reprogramar(context)
            return
        }
        if (intent.action != ProgramadorNotificaciones.ACCION) return

        val tipo = intent.getStringExtra(ProgramadorNotificaciones.EXTRA_TIPO) ?: return
        val momento = intent.getStringExtra(ProgramadorNotificaciones.EXTRA_MOMENTO) ?: return
        val minAntes = intent.getIntExtra(ProgramadorNotificaciones.EXTRA_MIN_ANTES, 10)

        val yaMarco = ProgramadorNotificaciones.marcado(context, tipo, LocalDate.now().toString())
        val (titulo, contenido) = MensajesNotificacion.componer(tipo, momento, minAntes, yaMarco)

        MarcAppNotificaciones.mostrar(
            context,
            ProgramadorNotificaciones.idNotificacion(tipo, momento),
            titulo,
            contenido
        )
    }
}
