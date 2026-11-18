package com.marcapp.app.feature.attendance

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcapp.app.core.notifications.ProgramadorNotificaciones
import com.marcapp.app.domain.model.*
import com.marcapp.app.domain.repository.AsistenciaRepository
import com.marcapp.app.domain.usecase.RegistrarMarcacionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AttendanceViewModel(private val repository: AsistenciaRepository) : ViewModel() {
    val marcaciones = repository.marcaciones
    private val mensajeMutable = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = mensajeMutable
    private val procesoMutable = MutableStateFlow(false)
    val enProceso: StateFlow<Boolean> = procesoMutable
    private val registrar = RegistrarMarcacionUseCase(repository)

    fun cargar(usuario: Usuario) {
        viewModelScope.launch {
            mensajeMutable.value = null
            val error = repository.cargar(usuario)
            if (error != null) mensajeMutable.value = error
        }
    }

    fun marcar(tipo: TipoMarca, usuario: Usuario, latitud: Double? = null, longitud: Double? = null) {
        if (procesoMutable.value) return
        viewModelScope.launch {
            procesoMutable.value = true
            try { mensajeMutable.value = registrar(tipo, usuario, latitud, longitud) }
            finally { procesoMutable.value = false }
        }
    }
    fun limpiarMensaje() { mensajeMutable.value = null }

    /**
     * Baja el turno de hoy del backend y agenda las notificaciones locales del trabajador.
     * Se llama al iniciar sesión. Usa el contexto de aplicación (no retiene el de la Activity).
     */
    fun configurarNotificaciones(appContext: Context, usuario: Usuario) {
        if (usuario.rol != Rol.TRABAJADOR) return
        viewModelScope.launch {
            val horario = repository.horarioHoy(usuario)
            if (horario != null) ProgramadorNotificaciones.guardarHorario(appContext, horario)
            ProgramadorNotificaciones.sincronizarMarcas(appContext, marcaciones.value)
        }
    }

    /** Refleja en las notificaciones qué marcó hoy el trabajador (ánimo vs recordatorio). */
    fun sincronizarMarcasNotificaciones(appContext: Context) {
        ProgramadorNotificaciones.sincronizarMarcas(appContext, marcaciones.value)
    }
}
