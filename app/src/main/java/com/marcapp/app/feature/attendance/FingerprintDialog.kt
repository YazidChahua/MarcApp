package com.marcapp.app.feature.attendance

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.marcapp.app.core.biometric.AndroidVerificadorHuella
import com.marcapp.app.core.biometric.EstadoHuella
import com.marcapp.app.core.biometric.ResultadoHuella
import com.marcapp.app.core.designsystem.MarcAppColors as C
import com.marcapp.app.domain.model.TipoMarca

/** Diálogo de verificación de HUELLAS para una marcación, sin PIN ni rostro. */
@Composable
fun FingerprintDialog(
    tipo: TipoMarca,
    onVerificado: () -> Unit,
    onCancelar: () -> Unit
) {
    val contexto = LocalContext.current
    val verificador = remember(contexto) { AndroidVerificadorHuella(contexto.applicationContext) }
    val estado = remember(verificador) { verificador.estado() }
    val confirmarActual by rememberUpdatedState(onVerificado)
    var mensaje by remember(estado) {
        mutableStateOf(
            when (estado) {
                EstadoHuella.Lista -> "Coloca tu dedo en el lector para confirmar ${tipo.titulo.lowercase()}."
                EstadoHuella.SinRegistrar -> "Debes registrar tu huella en los ajustes de Android antes de marcar."
                EstadoHuella.SinSensor -> "Este teléfono no dispone de un lector de huellas compatible."
                EstadoHuella.NoDisponible -> "No está disponible la verificación por huella en este teléfono."
            }
        )
    }

    DisposableEffect(verificador, estado) {
        val cancelar = if (estado == EstadoHuella.Lista) {
            verificador.verificar { resultado ->
                when (resultado) {
                    ResultadoHuella.Aprobada -> confirmarActual()
                    ResultadoHuella.NoReconocida -> mensaje = "Huella no reconocida. Prueba otra vez con el mismo dedo."
                    is ResultadoHuella.Mensaje -> mensaje = resultado.texto
                    is ResultadoHuella.Error -> mensaje = "No se registró la marcación: ${resultado.texto}"
                }
            }
        } else null
        onDispose { cancelar?.invoke() }
    }

    AlertDialog(
        onDismissRequest = onCancelar,
        icon = { Icon(Icons.Default.Fingerprint, contentDescription = null, tint = C.VerdeAccion) },
        title = { Text("Verifica tu huella digital") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Acción: ${tipo.titulo}")
                Text(mensaje)
                Text("MarcApp no almacena tu huella. La verifica Android.", color = C.Secundario)
            }
        },
        confirmButton = {
            if (estado == EstadoHuella.SinRegistrar) {
                TextButton(onClick = {
                    // Abre los ajustes de seguridad; Android gestiona el registro de varios toques.
                    runCatching { contexto.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }
                        .onFailure { mensaje = "Abre Ajustes > Seguridad > Huella digital manualmente." }
                }) { Text("Configurar huella") }
            } else {
                TextButton(onClick = onCancelar) { Text("Cerrar") }
            }
        },
        dismissButton = {
            if (estado == EstadoHuella.SinRegistrar) {
                TextButton(onClick = onCancelar) { Text("Cancelar") }
            }
        }
    )
}
