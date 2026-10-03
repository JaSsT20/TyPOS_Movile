package com.typdevstudio.typos_movil.ui.licencia

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.typdevstudio.typos_movil.datos.local.AppBaseDatos
import com.typdevstudio.typos_movil.datos.repositorio.EstadoLicencia
import com.typdevstudio.typos_movil.datos.repositorio.LicenciaRepositorio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LicenciaUiState(
    val estado: EstadoLicencia = EstadoLicencia.SinLicencia,
    val claveIngresada: String = "",
    val estaActivando: Boolean = false,
    val mensajeError: String? = null,
    val activacionExitosa: Boolean = false
)

class LicenciaViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio: LicenciaRepositorio

    init {
        val bd = AppBaseDatos.obtenerBaseDatos(application)
        repositorio = LicenciaRepositorio(bd.licenciaDao())
        verificarLicencia()
    }

    private val _uiState = MutableStateFlow(LicenciaUiState())
    val uiState: StateFlow<LicenciaUiState> = _uiState.asStateFlow()

    fun verificarLicencia() {
        viewModelScope.launch {
            val estado = repositorio.verificarEstadoLicencia()
            _uiState.update { it.copy(estado = estado) }
        }
    }

    fun onClaveIngresadaCambiada(nuevaClave: String) {
        val soloAlfanumerico = nuevaClave.replace("-", "").replace(" ", "").trim().uppercase().take(16)
        // Formatear con guiones cada 4 caracteres
        val formateada = soloAlfanumerico.chunked(4).joinToString("-")
        _uiState.update { it.copy(claveIngresada = formateada, mensajeError = null) }
    }

    fun activarLicencia() {
        val clave = _uiState.value.claveIngresada.trim()
        if (clave.isBlank()) {
            _uiState.update { it.copy(mensajeError = "Por favor ingresa tu clave de licencia") }
            return
        }

        _uiState.update { it.copy(estaActivando = true, mensajeError = null) }

        viewModelScope.launch {
            val resultado = repositorio.activarLicencia(clave)
            if (resultado.esValida && !resultado.estaVencida) {
                val nuevoEstado = repositorio.verificarEstadoLicencia()
                _uiState.update {
                    it.copy(
                        estaActivando = false,
                        estado = nuevoEstado,
                        activacionExitosa = true,
                        mensajeError = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        estaActivando = false,
                        mensajeError = resultado.mensajeError.ifBlank { "Clave de licencia incorrecta o inválida" },
                        activacionExitosa = false
                    )
                }
            }
        }
    }

    fun limpiarEstadoActivacion() {
        _uiState.update { it.copy(activacionExitosa = false, mensajeError = null) }
    }
}
