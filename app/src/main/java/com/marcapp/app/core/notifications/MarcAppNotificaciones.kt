package com.marcapp.app.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.marcapp.app.MainActivity
import com.marcapp.app.R

/**
 * Centraliza la creación y el disparo de notificaciones locales de MarcApp.
 * Diseño: ícono/logo de la app a la izquierda, un título y debajo el contenido
 * (estilo WhatsApp/TikTok), con el verde de la marca como color de acento.
 */
object MarcAppNotificaciones {

    const val CANAL_ID = "marcapp_recordatorios"
    private const val CANAL_NOMBRE = "Recordatorios de asistencia"
    private const val CANAL_DESC = "Avisos para marcar tu entrada, refrigerio y salida, con mensajes de ánimo."

    /** Crea el canal de notificaciones (idempotente). Necesario desde Android 8. */
    fun crearCanal(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CANAL_ID) != null) return
        val canal = NotificationChannel(CANAL_ID, CANAL_NOMBRE, NotificationManager.IMPORTANCE_HIGH).apply {
            description = CANAL_DESC
            enableVibration(true)
            setShowBadge(true)
        }
        manager.createNotificationChannel(canal)
    }

    /** ¿El usuario concedió el permiso para mostrar notificaciones? (Android 13+). */
    fun permisoConcedido(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        else true

    /** Muestra una notificación bonita. [id] distingue cada aviso para que no se pisen. */
    fun mostrar(context: Context, id: Int, titulo: String, contenido: String) {
        crearCanal(context)
        if (!permisoConcedido(context)) return

        val abrir = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context, id, abrir,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(R.drawable.ic_noti)
            .setContentTitle(titulo)
            .setContentText(contenido)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contenido))
            .setColor(0xFF19BE86.toInt()) // Verde de acento MarcApp
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pi)

        logoApp(context)?.let { builder.setLargeIcon(it) }

        try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        } catch (_: SecurityException) {
            // Permiso revocado entre la verificación y el envío: se ignora sin romper la app.
        }
    }

    /** Logo de la app (ic_launcher) convertido a bitmap para usarlo como ícono grande. */
    private fun logoApp(context: Context): Bitmap? = try {
        drawableABitmap(ContextCompat.getDrawable(context, R.mipmap.ic_launcher))
    } catch (_: Exception) { null }

    private fun drawableABitmap(d: Drawable?): Bitmap? {
        if (d == null) return null
        if (d is BitmapDrawable && d.bitmap != null) return d.bitmap
        val ancho = d.intrinsicWidth.takeIf { it > 0 } ?: 128
        val alto = d.intrinsicHeight.takeIf { it > 0 } ?: 128
        val bmp = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        d.setBounds(0, 0, canvas.width, canvas.height)
        d.draw(canvas)
        return bmp
    }
}
