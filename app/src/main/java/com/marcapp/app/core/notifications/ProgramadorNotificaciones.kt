package com.marcapp.app.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.marcapp.app.domain.model.HorarioTurno
import com.marcapp.app.domain.model.Marcacion
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Programa las notificaciones locales del día con la alarma interna de Android (AlarmManager).
 * No usa servidores de push: todo se agenda en el teléfono. Los avisos se recalculan cuando
 * el trabajador abre la app (se baja su turno) y tras un reinicio del equipo.
 *
 * Por cada evento del turno (entrada, salida a refrigerio, retorno y salida) agenda:
 *   - un aviso del sistema [MOMENTO_ANTES] "minutosAntes" antes (lo fija el admin, por defecto 10),
 *   - un mensaje a la hora exacta [MOMENTO_HORA] (ánimo si ya marcó, recordatorio si no).
 * Además, un aviso para alistarse antes de la entrada, que el usuario configura en su Perfil.
 */
object ProgramadorNotificaciones {

    const val ACCION = "com.marcapp.app.action.NOTIFICAR"
    const val EXTRA_TIPO = "tipo"
    const val EXTRA_MOMENTO = "momento"
    const val EXTRA_MIN_ANTES = "minAntes"

    const val MOMENTO_ALISTARSE = "ALISTARSE"
    const val MOMENTO_ANTES = "ANTES"
    const val MOMENTO_HORA = "HORA"

    // Orden fijo: también define el código de alarma de cada evento.
    private val TIPOS = listOf("ENTRADA", "SALIDA_REFRIGERIO", "REGRESO_REFRIGERIO", "SALIDA")
    private val MOMENTOS = listOf(MOMENTO_ALISTARSE, MOMENTO_ANTES, MOMENTO_HORA)

    private const val PREFS = "marcapp_notif"
    private const val K_ACTIVADAS = "activadas"
    private const val K_ALISTARSE = "alistarse_min"   // 0 = desactivado
    private const val K_FECHA = "fecha"
    private const val K_TRABAJA = "trabaja_hoy"
    private const val K_TURNO = "nombre_turno"
    private const val K_TIENE = "tiene_turno"
    private const val K_MIN_ANTES = "min_antes"
    private val K_HORA = mapOf(
        "ENTRADA" to "h_ingreso",
        "SALIDA_REFRIGERIO" to "h_ini_ref",
        "REGRESO_REFRIGERIO" to "h_fin_ref",
        "SALIDA" to "h_salida",
    )

    private const val DEFECTO_ALISTARSE = 30
    private val HHMM = DateTimeFormatter.ofPattern("HH:mm")

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---- Preferencias que el usuario controla desde Perfil ----

    fun estanActivadas(c: Context): Boolean = prefs(c).getBoolean(K_ACTIVADAS, true)

    fun setActivadas(c: Context, valor: Boolean) {
        prefs(c).edit().putBoolean(K_ACTIVADAS, valor).apply()
        reprogramar(c)
    }

    /** Minutos antes de la entrada para el aviso "alístate". 0 = desactivado. */
    fun minutosAlistarse(c: Context): Int = prefs(c).getInt(K_ALISTARSE, DEFECTO_ALISTARSE)

    fun setMinutosAlistarse(c: Context, minutos: Int) {
        prefs(c).edit().putInt(K_ALISTARSE, minutos.coerceIn(0, 180)).apply()
        reprogramar(c)
    }

    fun horaIngresoGuardada(c: Context): String? = prefs(c).getString(K_HORA["ENTRADA"], null)?.ifBlank { null }
    fun trabajaHoy(c: Context): Boolean = prefs(c).getBoolean(K_TRABAJA, false)
    fun tieneTurno(c: Context): Boolean = prefs(c).getBoolean(K_TIENE, false)
    fun minutosAntes(c: Context): Int = prefs(c).getInt(K_MIN_ANTES, 10)

    // ---- Horario del día (lo baja la app del backend) ----

    fun guardarHorario(c: Context, horario: HorarioTurno) {
        prefs(c).edit().apply {
            putString(K_FECHA, LocalDate.now().toString())
            putBoolean(K_TIENE, horario.tieneTurno)
            putBoolean(K_TRABAJA, horario.trabajaHoy)
            putString(K_TURNO, horario.nombreTurno ?: "")
            putInt(K_MIN_ANTES, horario.minutosAntes.coerceIn(1, 60))
            putString(K_HORA["ENTRADA"], horario.horaIngreso ?: "")
            putString(K_HORA["SALIDA_REFRIGERIO"], horario.inicioRefrigerio ?: "")
            putString(K_HORA["REGRESO_REFRIGERIO"], horario.finRefrigerio ?: "")
            putString(K_HORA["SALIDA"], horario.horaSalida ?: "")
            apply()
        }
        reprogramar(c)
    }

    // ---- Marcaciones: para decidir ánimo vs recordatorio a la hora exacta ----

    fun marcado(c: Context, tipo: String, fecha: String): Boolean =
        prefs(c).getBoolean("marca_${tipo}_$fecha", false)

    /** Refleja en preferencias qué marcó hoy el trabajador (según el historial). */
    fun sincronizarMarcas(c: Context, marcaciones: List<Marcacion>) {
        val hoy = LocalDate.now()
        val editor = prefs(c).edit()
        for (m in marcaciones) {
            if (m.fechaHora.toLocalDate() == hoy) {
                editor.putBoolean("marca_${m.tipo.name}_$hoy", true)
            }
        }
        editor.apply()
    }

    // ---- Programación de alarmas ----

    fun reprogramar(c: Context) {
        cancelarTodo(c)
        val p = prefs(c)
        if (!p.getBoolean(K_ACTIVADAS, true)) return
        if (p.getString(K_FECHA, null) != LocalDate.now().toString()) return // horario de otro día
        if (!p.getBoolean(K_TRABAJA, false)) return

        val minAntes = p.getInt(K_MIN_ANTES, 10).coerceIn(1, 60)
        val alistarse = p.getInt(K_ALISTARSE, DEFECTO_ALISTARSE)
        val ahora = System.currentTimeMillis()

        for (tipo in TIPOS) {
            val hora = parseHora(p.getString(K_HORA[tipo], null)) ?: continue

            // Aviso del sistema, "minAntes" antes (inmodificable para el trabajador).
            agendar(c, tipo, MOMENTO_ANTES, minAntes, hora.minusMinutes(minAntes.toLong()), ahora)
            // Mensaje a la hora exacta.
            agendar(c, tipo, MOMENTO_HORA, minAntes, hora, ahora)

            // Aviso para alistarse, solo antes de la ENTRADA y si el usuario lo activó.
            if (tipo == "ENTRADA" && alistarse > 0) {
                agendar(c, tipo, MOMENTO_ALISTARSE, minAntes, hora.minusMinutes(alistarse.toLong()), ahora)
            }
        }
    }

    private fun agendar(c: Context, tipo: String, momento: String, minAntes: Int, hora: LocalTime, ahora: Long) {
        val disparo = LocalDateTime.of(LocalDate.now(), hora)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (disparo <= ahora) return // ya pasó hoy

        val am = c.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = pendingIntent(c, tipo, momento, minAntes)
        val exacta = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        try {
            if (exacta) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, disparo, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, disparo, pi)
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, disparo, pi)
        }
    }

    fun cancelarTodo(c: Context) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (tipo in TIPOS) for (momento in MOMENTOS) {
            am.cancel(pendingIntent(c, tipo, momento, 0))
        }
    }

    private fun codigo(tipo: String, momento: String): Int =
        100 + TIPOS.indexOf(tipo) * 10 + MOMENTOS.indexOf(momento)

    private fun pendingIntent(c: Context, tipo: String, momento: String, minAntes: Int): PendingIntent {
        val intent = Intent(c, NotificacionReceiver::class.java).apply {
            action = ACCION
            putExtra(EXTRA_TIPO, tipo)
            putExtra(EXTRA_MOMENTO, momento)
            putExtra(EXTRA_MIN_ANTES, minAntes)
        }
        return PendingIntent.getBroadcast(
            c, codigo(tipo, momento), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun idNotificacion(tipo: String, momento: String): Int = codigo(tipo, momento)

    private fun parseHora(valor: String?): LocalTime? =
        if (valor.isNullOrBlank()) null else try { LocalTime.parse(valor, HHMM) } catch (_: Exception) { null }
}
