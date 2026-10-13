package com.typdevstudio.typos_movil.ui.licencia

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.typdevstudio.typos_movil.datos.local.AppBaseDatos
import com.typdevstudio.typos_movil.utilidades.licencia.LicenciaGuard
import com.typdevstudio.typos_movil.utilidades.licencia.SituacionLicencia
import com.typdevstudio.typos_movil.utilidades.licencia.nube.ResultadoLicencia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LicenciaUiState(
    val claveIngresada: String = "",
    val nombreDispositivo: String = "",
    val identificadorDispositivo: String = "",
    /** Usuarios activos en este teléfono (cuentan contra el límite del plan). */
    val usuariosActivos: Int? = null,
    val estaActivando: Boolean = false,
    val estaValidando: Boolean = false,
    val estaDesactivando: Boolean = false,
    val confirmarDesactivacion: Boolean = false,
    val mensaje: String? = null,
    val esError: Boolean = false,
)

/** Pantalla de licencia: activar, validar ahora y desactivar este teléfono. Toda la lógica vive en [LicenciaGuard]. */
class LicenciaViewModel(application: Application) : AndroidViewModel(application) {

    val situacion: StateFlow<SituacionLicencia> = LicenciaGuard.situacion

    private val _uiState = MutableStateFlow(LicenciaUiState(nombreDispositivo = nombrePorDefecto()))
    val uiState: StateFlow<LicenciaUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val identificador = LicenciaGuard.identificadorDispositivo()
            val usuarios = AppBaseDatos.obtenerBaseDatos(getApplication()).usuarioDao().contarUsuariosActivos()
            _uiState.update { it.copy(identificadorDispositivo = identificador, usuariosActivos = usuarios) }
            LicenciaGuard.actualizar()
        }
    }

    /**
     * Se guarda tal como se escribe: transformar el texto mientras se teclea (mayúsculas, guiones) hace que el
     * teclado pierda o desordene caracteres. Mayúsculas y guiones se aplican al activar ([claveNormalizada]).
     */
    fun onClaveCambiada(texto: String) {
        _uiState.update { it.copy(claveIngresada = texto.take(30), mensaje = null) }
    }

    fun onNombreDispositivoCambiado(nombre: String) {
        _uiState.update { it.copy(nombreDispositivo = nombre.take(100)) }
    }

    fun activar() {
        val estado = _uiState.value
        val clave = claveNormalizada(estado.claveIngresada)
        if (clave == null) {
            _uiState.update { it.copy(mensaje = "Ingresa la clave completa: 12 letras o números (XXXX-XXXX-XXXX).", esError = true) }
            return
        }
        _uiState.update { it.copy(estaActivando = true, claveIngresada = clave, mensaje = null) }
        viewModelScope.launch {
            val resultado = LicenciaGuard.activar(clave, estado.nombreDispositivo)
            val exito = resultado is ResultadoLicencia.Exito
            _uiState.update {
                it.copy(
                    estaActivando = false,
                    claveIngresada = if (exito) "" else it.claveIngresada,
                    mensaje = mensajeDe(resultado, "¡Licencia activada! Ya puedes usar TyPOS Móvil con normalidad."),
                    esError = !exito,
                )
            }
        }
    }

    fun validarAhora() {
        _uiState.update { it.copy(estaValidando = true, mensaje = null) }
        viewModelScope.launch {
            val resultado = LicenciaGuard.validarAhora()
            _uiState.update {
                it.copy(
                    estaValidando = false,
                    mensaje = mensajeDe(resultado, "Licencia validada correctamente."),
                    esError = resultado !is ResultadoLicencia.Exito,
                )
            }
        }
    }

    fun pedirDesactivacion() = _uiState.update { it.copy(confirmarDesactivacion = true) }

    fun cancelarDesactivacion() = _uiState.update { it.copy(confirmarDesactivacion = false) }

    fun confirmarDesactivacion() {
        _uiState.update { it.copy(confirmarDesactivacion = false, estaDesactivando = true, mensaje = null) }
        viewModelScope.launch {
            val resultado = LicenciaGuard.desactivar()
            _uiState.update {
                it.copy(
                    estaDesactivando = false,
                    mensaje = mensajeDe(resultado, "Este teléfono se desactivó y liberó su lugar en la licencia."),
                    esError = resultado !is ResultadoLicencia.Exito,
                )
            }
        }
    }

    fun limpiarMensaje() = _uiState.update { it.copy(mensaje = null) }

    private fun nombrePorDefecto(): String {
        val fabricante = Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() }
        val modelo = Build.MODEL.orEmpty()
        return (if (modelo.startsWith(fabricante, ignoreCase = true)) modelo else "$fabricante $modelo").trim().take(100)
    }

    companion object {
        /** `6f3m drbx-anru` → `6F3M-DRBX-ANRU`, o `null` si no son exactamente 12 letras o números. */
        fun claveNormalizada(texto: String): String? {
            val alfanumerica = texto.filter { it.isLetterOrDigit() && it.code < 128 }.uppercase()
            return if (alfanumerica.length == 12) alfanumerica.chunked(4).joinToString("-") else null
        }

        /** Mensaje para el usuario de una operación de licencia. */
        fun mensajeDe(resultado: ResultadoLicencia, mensajeExito: String): String = when (resultado) {
            is ResultadoLicencia.Exito -> mensajeExito
            is ResultadoLicencia.Rechazada -> resultado.mensaje
            is ResultadoLicencia.SinConexion -> resultado.mensaje
            is ResultadoLicencia.TokenNoValido -> resultado.mensaje
            ResultadoLicencia.SinLicencia -> "Este teléfono todavía no tiene una licencia activada."
        }
    }
}
