package com.marcapp.app.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color as AColor
import android.location.Location
import android.os.Looper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.marcapp.app.core.designsystem.MarcAppColors as C
import com.marcapp.app.core.ui.Tarjeta
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

/**
 * Mapa real (OpenStreetMap) que muestra:
 *  - Dónde está parado el trabajador en este momento (se actualiza solo).
 *  - El área de marcación (círculo) y a cuántos metros está de ella.
 *
 * NOTA TEMPORAL: el centro del área se toma de la primera ubicación capturada
 * y se guarda en el teléfono. Más adelante esto lo definirá el administrador
 * desde el panel web; cuando eso exista, se reemplaza [leerCentro]/[guardarCentro].
 */
@SuppressLint("MissingPermission")
@Composable
fun MapaMarcacion(radioMetros: Int = 150, modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    val prefs = remember { contexto.getSharedPreferences("marcapp_mapa", Context.MODE_PRIVATE) }

    fun leerCentro(): GeoPoint? = prefs.getString("centro", null)?.split(",")?.let {
        runCatching { GeoPoint(it[0].toDouble(), it[1].toDouble()) }.getOrNull()
    }
    fun guardarCentro(p: GeoPoint) {
        prefs.edit().putString("centro", "${p.latitude},${p.longitude}").apply()
    }

    var centro by remember { mutableStateOf(leerCentro()) }
    var usuario by remember { mutableStateOf<GeoPoint?>(null) }
    var distancia by remember { mutableStateOf<Float?>(null) }
    val yaCentrado = remember { mutableStateOf(false) }

    val mapa = remember {
        // osmdroid se inicializa una sola vez antes de crear el mapa.
        Configuration.getInstance().apply {
            load(contexto, contexto.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            userAgentValue = contexto.packageName
        }
        MapView(contexto).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(17.0)
        }
    }

    // Ciclo de vida del mapa.
    DisposableEffect(Unit) {
        mapa.onResume()
        onDispose { mapa.onPause(); mapa.onDetach() }
    }

    // Lectura de ubicación en vivo.
    DisposableEffect(Unit) {
        val cliente = LocationServices.getFusedLocationProviderClient(contexto)
        val solicitud = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L).build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(resultado: LocationResult) {
                val loc = resultado.lastLocation ?: return
                val punto = GeoPoint(loc.latitude, loc.longitude)
                usuario = punto
                if (centro == null) { guardarCentro(punto); centro = punto }
                centro?.let { c ->
                    val salida = FloatArray(1)
                    Location.distanceBetween(loc.latitude, loc.longitude, c.latitude, c.longitude, salida)
                    distancia = salida[0]
                }
            }
        }
        try {
            cliente.requestLocationUpdates(solicitud, callback, Looper.getMainLooper())
        } catch (_: SecurityException) { /* sin permiso aún */ }
        onDispose { cliente.removeLocationUpdates(callback) }
    }

    val dentro = distancia != null && distancia!! <= radioMetros

    Tarjeta(modifier) {
        Text("Tu ubicación y el área de marcación", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = C.Texto)

        AndroidView(
            factory = { mapa },
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(14.dp)),
            update = { mv ->
                mv.overlays.clear()
                centro?.let { c ->
                    mv.overlays.add(Polygon(mv).apply {
                        points = Polygon.pointsAsCircle(c, radioMetros.toDouble())
                        fillPaint.color = AColor.argb(40, 25, 190, 134)
                        outlinePaint.color = AColor.rgb(25, 190, 134)
                        outlinePaint.strokeWidth = 5f
                    })
                    mv.overlays.add(Marker(mv).apply {
                        position = c
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Área de marcación"
                    })
                }
                usuario?.let { u ->
                    mv.overlays.add(Marker(mv).apply {
                        position = u
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Estás aquí"
                    })
                }
                val foco = usuario ?: centro
                if (!yaCentrado.value && foco != null) {
                    mv.controller.setCenter(foco)
                    yaCentrado.value = true
                }
                mv.invalidate()
            }
        )

        if (distancia == null) {
            Text("Obteniendo tu ubicación…", fontSize = 13.sp, color = C.Secundario)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Estás a ${distancia!!.toInt()} m del área de marcación",
                    fontSize = 13.sp, color = C.Texto, fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = if (dentro) Color(0xFFE8F8F2) else Color(0xFFFFF4E5),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        if (dentro) "Dentro del área" else "Fuera del área",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = if (dentro) C.VerdeOscuro else Color(0xFF8A5A17)
                    )
                }
            }
        }
    }
}
