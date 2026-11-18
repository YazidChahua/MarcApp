package com.marcapp.app.feature.attendance

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcapp.app.core.designsystem.MarcAppColors as C
import com.marcapp.app.core.notifications.MarcAppNotificaciones
import com.marcapp.app.core.notifications.ProgramadorNotificaciones
import com.marcapp.app.core.ui.*
import com.marcapp.app.domain.model.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun prefsPerfil(context: Context) = context.getSharedPreferences("marcapp_perfil", Context.MODE_PRIVATE)
private fun correoValido(c: String) = c.trim().matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))

@Composable
fun PerfilScreen(usuario: Usuario, onCambiarCorreo: (String) -> Unit, salir: () -> Unit) {
    val contexto = LocalContext.current
    val prefs = remember { prefsPerfil(contexto) }
    val scope = rememberCoroutineScope()

    var foto by remember { mutableStateOf(prefs.getString("foto", null)) }
    var correo by remember(usuario.correo) { mutableStateOf(usuario.correo) }
    var mostrarCambioCorreo by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf<String?>(null) }

    // Elegir foto de perfil (se copia al almacenamiento interno para que quede guardada).
    val elegirFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val nombre = "perfil_${System.currentTimeMillis()}.jpg"
            val guardada = withContext(Dispatchers.IO) { copiarAInterno(contexto, uri, nombre) }
            if (guardada != null) {
                prefs.edit().putString("foto", guardada).apply()
                foto = guardada
            }
        }
    }

    Column(
        Modifier.fillMaxSize().background(C.Fondo).verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TituloSeccion("Mi perfil")

        // ---- Tarjeta de identidad ----
        Tarjeta {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    FotoPerfil(foto, usuario.nombre.take(1).uppercase(), tamano = 96.dp)
                    Surface(
                        color = C.VerdeAccion, shape = CircleShape,
                        modifier = Modifier.size(32.dp).clickable {
                            elegirFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    ) {
                        Icon(Icons.Default.CameraAlt, "Cambiar foto", tint = Color.White, modifier = Modifier.padding(7.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(usuario.nombre, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = C.Texto)
                Text(
                    if (usuario.rol == Rol.TRABAJADOR) "Colaborador" else "Administrador",
                    fontSize = 13.sp, color = C.Secundario
                )
                Text("Toca la cámara para cambiar tu foto (opcional).", fontSize = 11.sp, color = C.Secundario)
            }
        }

        // ---- Cuenta ----
        Tarjeta {
            Text("Mi cuenta", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = C.Texto)
            Separador()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Email, null, tint = C.Secundario, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Correo electrónico", fontSize = 12.sp, color = C.Secundario)
                    Text(correo, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.Texto)
                }
            }
            OutlinedButton(onClick = { mostrarCambioCorreo = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Cambiar correo")
            }
        }

        // ---- Notificaciones (solo colaboradores) ----
        if (usuario.rol == Rol.TRABAJADOR) {
            TarjetaNotificaciones(contexto)
        }

        aviso?.let { Aviso(it) }

        BotonPrincipal("Cerrar sesión", salir, Modifier.fillMaxWidth())
    }

    if (mostrarCambioCorreo) {
        DialogoCambioCorreo(
            correoActual = correo,
            onConfirmado = { nuevo ->
                onCambiarCorreo(nuevo)
                correo = nuevo
                mostrarCambioCorreo = false
                aviso = "Tu correo se actualizó a $nuevo."
            },
            onCerrar = { mostrarCambioCorreo = false }
        )
    }
}

/** Opciones de "aviso para alistarte": minutos antes de la entrada (0 = desactivado). */
private val OPCIONES_ALISTARSE = listOf(0, 15, 30, 45, 60, 90)

/**
 * Configuración de notificaciones que el trabajador controla desde su perfil.
 * Puede encender/apagar los avisos y elegir con cuánta anticipación quiere el aviso para
 * alistarse antes de su entrada. Los recordatorios del sistema (minutos antes de cada evento,
 * que define el administrador) y los mensajes de ánimo a la hora exacta son automáticos.
 */
@Composable
private fun TarjetaNotificaciones(contexto: Context) {
    var activadas by remember {
        mutableStateOf(
            ProgramadorNotificaciones.estanActivadas(contexto) && MarcAppNotificaciones.permisoConcedido(contexto)
        )
    }
    var alistarse by remember { mutableStateOf(ProgramadorNotificaciones.minutosAlistarse(contexto)) }
    val horaIngreso = remember { ProgramadorNotificaciones.horaIngresoGuardada(contexto) }
    val tieneTurno = remember { ProgramadorNotificaciones.tieneTurno(contexto) }
    val minAntesAdmin = remember { ProgramadorNotificaciones.minutosAntes(contexto) }

    // Permiso de notificaciones (Android 13+). Al concederlo, se activan de verdad.
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) { ProgramadorNotificaciones.setActivadas(contexto, true); activadas = true }
        else activadas = false
    }

    fun activar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !MarcAppNotificaciones.permisoConcedido(contexto)) {
            pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            ProgramadorNotificaciones.setActivadas(contexto, true); activadas = true
        }
    }

    Tarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Notifications, null, tint = C.VerdeOscuro, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Notificaciones", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = C.Texto)
        }
        Separador()

        // Interruptor maestro
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Recibir notificaciones", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = C.Texto)
                Text("Recordatorios para marcar y mensajes de ánimo durante tu jornada.",
                    fontSize = 12.sp, color = C.Secundario)
            }
            Switch(checked = activadas, onCheckedChange = { marcar ->
                if (marcar) activar()
                else { ProgramadorNotificaciones.setActivadas(contexto, false); activadas = false }
            })
        }

        if (activadas) {
            Separador()
            Text("Aviso para alistarte", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C.Texto)
            Text("Un aviso extra antes de tu entrada para que te prepares con calma.",
                fontSize = 12.sp, color = C.Secundario)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OPCIONES_ALISTARSE.forEach { min ->
                    FilterChip(
                        selected = alistarse == min,
                        onClick = { ProgramadorNotificaciones.setMinutosAlistarse(contexto, min); alistarse = min },
                        label = { Text(if (min == 0) "No" else "$min min") }
                    )
                }
            }
            // Si conocemos la hora de entrada, mostramos a qué hora llegaría el aviso.
            val horaAviso = calcularHoraAviso(horaIngreso, alistarse)
            if (alistarse > 0 && horaAviso != null) {
                Spacer(Modifier.height(6.dp))
                Text("Te avisaremos cerca de las $horaAviso.", fontSize = 12.sp, color = C.VerdeOscuro)
            }

            Separador()
            val detalle = buildString {
                append("Automático: aviso $minAntesAdmin min antes de cada evento (lo define tu empresa) ")
                append("y un mensaje a la hora de tu entrada, refrigerio y salida.")
            }
            Text(detalle, fontSize = 12.sp, color = C.Secundario)
            if (!tieneTurno) {
                Spacer(Modifier.height(6.dp))
                Text("Aún no tienes un turno asignado. Cuando tu administrador te lo asigne, los avisos se activan solos.",
                    fontSize = 12.sp, color = C.Amarillo)
            }
        }
    }
}

/** Devuelve "HH:mm" del aviso para alistarse (entrada menos los minutos elegidos), o null. */
private fun calcularHoraAviso(horaIngreso: String?, minutosAntes: Int): String? {
    if (horaIngreso.isNullOrBlank() || minutosAntes <= 0) return null
    return try {
        val f = DateTimeFormatter.ofPattern("HH:mm")
        LocalTime.parse(horaIngreso, f).minusMinutes(minutosAntes.toLong()).format(f)
    } catch (_: Exception) { null }
}

/**
 * Flujo de cambio de correo con código de validación.
 *
 * INTEGRACIÓN: hoy el código se genera y muestra en el teléfono (DEMO) para poder probar.
 * Cuando conectes Resend/SMTP, reemplaza [enviarCodigoPorCorreo] por la llamada real que
 * envía el código al correo nuevo; el resto del flujo queda igual.
 */
@Composable
private fun DialogoCambioCorreo(
    correoActual: String,
    onConfirmado: (String) -> Unit,
    onCerrar: () -> Unit
) {
    var paso by remember { mutableStateOf(1) }          // 1 = ingresar correo, 2 = ingresar código
    var nuevoCorreo by remember { mutableStateOf("") }
    var codigoIngresado by remember { mutableStateOf("") }
    var codigoEsperado by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (paso == 1) "Cambiar correo" else "Verifica tu correo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (paso == 1) {
                    Text("Ingresa tu nuevo correo. Te enviaremos un código de verificación.", fontSize = 13.sp, color = C.Secundario)
                    OutlinedTextField(
                        value = nuevoCorreo, onValueChange = { nuevoCorreo = it; error = null },
                        label = { Text("Nuevo correo") }, singleLine = true,
                        isError = error != null, modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text("Escribe el código que enviamos a $nuevoCorreo.", fontSize = 13.sp, color = C.Secundario)
                    OutlinedTextField(
                        value = codigoIngresado,
                        onValueChange = { if (it.length <= 6) codigoIngresado = it.filter(Char::isDigit); error = null },
                        label = { Text("Código de 6 dígitos") }, singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = error != null, modifier = Modifier.fillMaxWidth()
                    )
                    info?.let { Text(it, fontSize = 12.sp, color = C.VerdeOscuro) }
                }
                error?.let { Text(it, fontSize = 12.sp, color = Color(0xFFB3261E)) }
            }
        },
        confirmButton = {
            if (paso == 1) {
                TextButton(onClick = {
                    val limpio = nuevoCorreo.trim()
                    when {
                        !correoValido(limpio) -> error = "Correo no válido."
                        limpio.equals(correoActual, ignoreCase = true) -> error = "Es el mismo correo actual."
                        else -> {
                            codigoEsperado = enviarCodigoPorCorreo(limpio)
                            info = "Código (DEMO): $codigoEsperado — con Resend llegará al correo."
                            paso = 2
                        }
                    }
                }) { Text("Enviar código") }
            } else {
                TextButton(onClick = {
                    if (codigoIngresado == codigoEsperado) onConfirmado(nuevoCorreo.trim())
                    else error = "Código incorrecto."
                }) { Text("Confirmar") }
            }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}

/** DEMO: genera un código de 6 dígitos. Reemplazar por el envío real vía Resend/SMTP. */
private fun enviarCodigoPorCorreo(correo: String): String =
    (100000..999999).random().toString()
