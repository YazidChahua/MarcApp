package com.marcapp.app.feature.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcapp.app.domain.model.*
import com.marcapp.app.domain.repository.AsistenciaRepository
import com.marcapp.app.domain.usecase.RegistrarMarcacionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AttendanceViewModel(repository: AsistenciaRepository) : ViewModel() {
    val marcaciones = repository.marcaciones
    private val mensajeMutable = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = mensajeMutable
    private val registrar = RegistrarMarcacionUseCase(repository)
    fun marcar(tipo: TipoMarca, usuario: Usuario) {
        viewModelScope.launch { mensajeMutable.value = registrar(tipo, usuario) }
    }
    fun limpiarMensaje() { mensajeMutable.value = null }
}
