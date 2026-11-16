package com.marcapp.app.feature.attendance

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcapp.app.core.designsystem.MarcAppColors as C
import com.marcapp.app.core.ui.*
import com.marcapp.app.domain.model.*
import com.marcapp.app.feature.admin.GestionViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val fechaDdMmAaaa = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolicitudesScreen(usuario: Usuario, vm: GestionViewModel) {
    val contexto = LocalContext.current
    val solicitudes by vm.solicitudes.collectAsState()
    val misRemotas by vm.misSolicitudes.collectAsState()
    val mensaje by vm.mensajeJustificacion.collectAsState()
    val enviando by vm.enviando.collectAsState()

    var fecha by remember { mutableStateOf(LocalDate.now()) }
    var motivo by remember { mutableStateOf("Tardanza") }
    var detalle by remember { mutableStateOf("") }
    var adjuntos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var mostrarPicker by remember { mutableStateOf(false) }
    var mesVisible by remember { mutableStateOf(YearMonth.now()) }

    // Trae del servidor las solicitudes del trabajador; limpia el formulario al enviarse bien.
    LaunchedEffect(usuario) { vm.cargarMisSolicitudes(usuario) }
    LaunchedEffect(mensaje) {
        if (mensaje?.startsWith("Solicitud enviada") == true) { detalle = ""; adjuntos = emptyList() }
    }

    // --- Selector de imágenes de la galería (varias a la vez) ---
    val galeria = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(5)
    ) { uris -> if (uris.isNotEmpty()) adjuntos = (adjuntos + uris).distinct().take(5) }

    // --- Cámara: toma una foto y la guarda en un Uri temporal ---
    var fotoPendiente by remember { mutableStateOf<Uri?>(null) }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) fotoPendiente?.let { adjuntos = (adjuntos + it).take(8) }
        fotoPendiente = null
    }

    // --- Documentos (PDF, Word, texto) ---
    val documentos = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { runCatching { contexto.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
        if (uris.isNotEmpty()) adjuntos = (adjuntos + uris).distinct().take(8)
    }
    val tiposDoc = arrayOf(
        "application/pdf", "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain"
    )

    Column(
        Modifier.fillMaxSize().background(C.Fondo).verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TituloSeccion("Justificaciones", "Solicitudes de tardanza e inasistencia")

        // ================= NUEVA SOLICITUD =================
        Tarjeta {
            Text("Nueva solicitud", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)

            // Fecha con calendario nativo de Android
            OutlinedButton(onClick = { mostrarPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CalendarMonth, null, tint = C.VerdeAccion)
                Spacer(Modifier.width(10.dp))
                Text("Fecha: ${fecha.format(fechaDdMmAaaa)}", color = C.Texto)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = motivo == "Tardanza", onClick = { motivo = "Tardanza" }, label = { Text("Tardanza") })
                FilterChip(selected = motivo == "Falta", onClick = { motivo = "Falta" }, label = { Text("Falta") })
            }

            CampoTexto("Motivo / sustento", detalle, { detalle = it }, unaLinea = false)

            // Adjuntar imágenes, foto o documentos
            Text("Adjuntar evidencia (opcional)", fontSize = 13.sp, color = C.Secundario)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, null, tint = C.VerdeAccion, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp)); Text("Galería", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { val uri = crearUriFoto(contexto); fotoPendiente = uri; camara.launch(uri) },
                    modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.PhotoCamera, null, tint = C.VerdeAccion, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp)); Text("Foto", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { documentos.launch(tiposDoc) },
                    modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.AttachFile, null, tint = C.VerdeAccion, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp)); Text("Archivo", fontSize = 12.sp)
                }
            }
            if (adjuntos.isNotEmpty()) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    adjuntos.forEach { uri ->
                        AdjuntoVista(uri.toString(), tamano = 72.dp) { adjuntos = adjuntos - uri }
                    }
                }
            }

            BotonPrincipal(if (enviando) "Enviando…" else "Enviar justificación", {
                vm.enviarJustificacion(usuario, fecha.toString(), motivo, detalle)
            }, Modifier.fillMaxWidth(), habilitado = detalle.isNotBlank() && !enviando)
        }
        mensaje?.let { Aviso(it, !it.startsWith("Solicitud enviada")) }

        // ================= MIS SOLICITUDES (por mes) =================
        TituloSeccion("Mis solicitudes")
        NavegadorMes(mes = mesVisible, onCambio = { mesVisible = it })

        val fuente = if (usuario.esDemo) solicitudes else misRemotas
        val mias = fuente
            .filter { it.usuario.equals(usuario.correo, ignoreCase = true) }
            .filter { runCatching { YearMonth.from(LocalDate.parse(it.fecha)) == mesVisible }.getOrDefault(false) }
            .sortedByDescending { it.id } // la más nueva primero, la más antigua al final

        if (mias.isEmpty()) {
            Tarjeta { Text("No tienes solicitudes en este mes.", color = C.Secundario) }
        } else {
            mias.forEach { s ->
                Tarjeta {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${s.motivo} · ${runCatching { LocalDate.parse(s.fecha).format(fechaDdMmAaaa) }.getOrDefault(s.fecha)}",
                            fontWeight = FontWeight.SemiBold)
                        Text(s.estado, color = if (s.estado == "Aprobada") C.VerdeAccion else C.Secundario, fontSize = 12.sp)
                    }
                    if (s.detalle.isNotBlank()) Text(s.detalle, fontSize = 12.sp, color = C.Secundario)
                    if (s.adjuntos.isNotEmpty()) {
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            s.adjuntos.forEach { AdjuntoVista(it, tamano = 64.dp) }
                        }
                    }
                }
            }
        }
    }

    // ---- Diálogo del calendario nativo ----
    if (mostrarPicker) {
        val estadoPicker = rememberDatePickerState(
            initialSelectedDateMillis = fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { mostrarPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoPicker.selectedDateMillis?.let {
                        fecha = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    mostrarPicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { mostrarPicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = estadoPicker, showModeToggle = false, title = null, headline = null) }
    }
}
