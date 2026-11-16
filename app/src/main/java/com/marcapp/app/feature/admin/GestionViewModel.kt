package com.marcapp.app.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcapp.app.data.remote.BackendApi
import com.marcapp.app.data.remote.JustificacionRequest
import com.marcapp.app.data.remote.JustificacionResponse
import com.marcapp.app.data.remote.MarcAppHttp
import com.marcapp.app.data.remote.SesionRemota
import com.marcapp.app.data.remote.mensajeServidor
import com.marcapp.app.domain.model.Justificacion
import com.marcapp.app.domain.model.Usuario
import com.marcapp.app.domain.repository.GestionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class GestionViewModel(
    val repository: GestionRepository,
    private val api: BackendApi = MarcAppHttp.api
) : ViewModel() {
    val solicitudes = repository.solicitudes
    val trabajadores = repository.trabajadores
    val turnos = repository.turnos
    val feriados = repository.feriados
    val auditoria = repository.auditoria
    val tolerancia = repository.tolerancia
    val radioGeocerca = repository.radioGeocerca

    // --- Justificaciones reales (servidor) del trabajador con sesión iniciada ---
    private val _misSolicitudes = MutableStateFlow<List<Justificacion>>(emptyList())
    val misSolicitudes: StateFlow<List<Justificacion>> = _misSolicitudes.asStateFlow()

    private val _mensajeJustificacion = MutableStateFlow<String?>(null)
    val mensajeJustificacion: StateFlow<String?> = _mensajeJustificacion.asStateFlow()

    private val _enviando = MutableStateFlow(false)
    val enviando: StateFlow<Boolean> = _enviando.asStateFlow()

    fun limpiarMensaje() { _mensajeJustificacion.value = null }

    /** Carga "Mis solicitudes" desde el backend (solo con sesión real). */
    fun cargarMisSolicitudes(usuario: Usuario) {
        if (usuario.esDemo) return
        val bearer = SesionRemota.bearer() ?: return
        viewModelScope.launch {
            try {
                _misSolicitudes.value = api.misJustificaciones(bearer).map { it.aDominio(usuario.correo) }
            } catch (_: Exception) {
                // Silencioso: si falla la carga, mantenemos lo que ya haya en pantalla.
            }
        }
    }

    /** Envía una justificación. En modo demo usa memoria; con sesión real va al backend. */
    fun enviarJustificacion(usuario: Usuario, fecha: String, motivo: String, detalle: String) {
        if (fecha.isBlank() || detalle.isBlank()) {
            _mensajeJustificacion.value = "Completa la fecha y el sustento."
            return
        }
        if (usuario.esDemo) {
            _mensajeJustificacion.value = repository.enviarJustificacion(usuario.correo, fecha, motivo, detalle)
            return
        }
        val bearer = SesionRemota.bearer()
        if (bearer == null) {
            _mensajeJustificacion.value = "Tu sesión expiró. Cierra sesión e ingresa de nuevo."
            return
        }
        viewModelScope.launch {
            _enviando.value = true
            _mensajeJustificacion.value = try {
                api.enviarJustificacion(bearer, JustificacionRequest(fecha, motivo, detalle))
                cargarMisSolicitudes(usuario)
                "Solicitud enviada al servidor correctamente."
            } catch (e: HttpException) {
                e.mensajeServidor(
                    when (e.code()) {
                        401 -> "Tu sesión expiró (15 min). Cierra sesión e ingresa de nuevo."
                        else -> "El servidor rechazó la solicitud (HTTP ${e.code()})."
                    }
                )
            } catch (_: Exception) {
                "No se pudo enviar. Comprueba tu conexión e inténtalo de nuevo."
            }
            _enviando.value = false
        }
    }

    private fun JustificacionResponse.aDominio(correo: String): Justificacion = Justificacion(
        id = id,
        usuario = correo,
        fecha = fecha ?: "",
        motivo = tipo ?: "Tardanza",
        detalle = mensaje ?: "",
        estado = estado ?: "Pendiente"
    )
}
