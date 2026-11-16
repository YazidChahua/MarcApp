package com.marcapp.app.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color as AColor
import android.location.Location
import android.os.Looper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.marcapp.app.data.remote.MarcAppHttp
import com.marcapp.app.data.remote.SesionRemota
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

/**
 * Mapa real (OpenStreetMap) que muestra:
 *  - El lugar de trabajo (geocerca) que el ADMINISTRADOR definió en el panel web.
 *    Se lee del backend (GET /api/marcapp/geocerca); si el admin lo cambia, aquí se ve.
 *  - Dónde está parado el trabajador ahora mismo (se actualiza solo).
 *  - La distancia en vivo entre el trabajador y el lugar de trabajo, y si está dentro o fuera.
 *
 * Si no hay sesión/conexión (p. ej. modo demo), usa como centro la primera ubicación capturada.
 */
@SuppressLint("MissingPermission")
@Composable
fun MapaMarcacion(modifier: Modifier = Modifier, radioPorDefecto: Int = 150) {
    val contexto = LocalContext.current

    var sede by remember { mutableStateOf<GeoPoint?>(null) }
    var radio by remember { mutableIntStateOf(radioPorDefecto) }
    var nombreSede by remember { mutableStateOf<String?>(null) }
    var esRespaldoLocal by remember { mutableStateOf(false) }
    var usuario by remember { mutableStateOf<GeoPoint?>(null) }
    var distancia by remember { mutableStateOf<Float?>(null) }
    val yaCentrado = remember { mutableStateOf(false) }

    // 1) Traer el lugar de trabajo definido en el panel (backend).
    LaunchedEffect(Unit) {
        val bearer = SesionRemota.bearer()
        if (bearer != null) {
            try {
                val g = MarcAppHttp.api.geocerca(bearer)
                sede = GeoPoint(g.latitud, g.longitud)
                radio = if (g.radioMetros > 0) g.radioMetros else radioPorDefecto
                nombreSede = g.nombre
            } catch (_: Exception) {
                // Sin conexión o sesión: se usará la ubicación del teléfono como respaldo.
            }
        }
    }

    val mapa = remember {
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

    DisposableEffect(Unit) {
        mapa.onResume()
        onDispose { mapa.onPause(); mapa.onDetach() }
    }

    // 2) Ubicación del trabajador en vivo + distancia al lugar de trabajo.
    DisposableEffect(Unit) {
        val cliente = LocationServices.getFusedLocationProviderClient(contexto)
        val solicitud = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L).build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(resultado: LocationResult) {
                val loc = resultado.lastLocation ?: return
                val punto = GeoPoint(loc.latitude, loc.longitude)
                usuario = punto
                // Si el backend no dio el centro, usa la primera ubicación como respaldo.
                if (sede == null) { sede = punto; esRespaldoLocal = true }
                sede?.let { c ->
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

    val dentro = distancia != null && distancia!! <= radio

    Tarjeta(modifier) {
        Text(
            if (nombreSede.isNullOrBlank()) "Tu ubicación y el área de marcación" else "Lugar de trabajo: ${nombreSede}",
            fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = C.Texto
        )

        AndroidView(
            factory = { mapa },
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clip(RoundedCornerShape(14.dp)),
            update = { mv ->
                mv.overlays.clear()
                sede?.let { c ->
                    mv.overlays.add(Polygon(mv).apply {
                        points = Polygon.pointsAsCircle(c, radio.toDouble())
                        fillPaint.color = AColor.argb(40, 25, 190, 134)
                        outlinePaint.color = AColor.rgb(25, 190, 134)
                        outlinePaint.strokeWidth = 5f
                    })
                    mv.overlays.add(Marker(mv).apply {
                        position = c
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Lugar de trabajo"
                    })
                }
                usuario?.let { u ->
                    mv.overlays.add(Marker(mv).apply {
                        position = u
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Estás aquí"
                    })
                }
                val foco = sede ?: usuario
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
                    "Estás a ${formatearDistancia(distancia!!)} del lugar de trabajo",
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
            if (esRespaldoLocal) {
                Text(
                    "Mostrando una ubicación de referencia; al conectarse se usará el lugar que fijó el administrador.",
                    fontSize = 11.sp, color = C.Secundario
                )
            }
        }
    }
}

/** Metros si es cerca, kilómetros si está lejos. */
private fun formatearDistancia(metros: Float): String =
    if (metros >= 1000f) String.format("%.1f km", metros / 1000f) else "${metros.toInt()} m"
