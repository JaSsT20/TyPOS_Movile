package com.typdevstudio.typos_movil.ui.login

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.typdevstudio.typos_movil.datos.local.AppBaseDatos
import com.typdevstudio.typos_movil.datos.local.GestorSesion
import com.typdevstudio.typos_movil.datos.local.entidades.UsuarioEntidad
import com.typdevstudio.typos_movil.datos.repositorio.EstadoLicencia
import com.typdevstudio.typos_movil.datos.repositorio.LicenciaRepositorio
import com.typdevstudio.typos_movil.datos.repositorio.ResultadoAutenticacion
import com.typdevstudio.typos_movil.datos.repositorio.UsuarioRepositorio
import com.typdevstudio.typos_movil.utilidades.licencia.IdDispositivo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val usuario: String = "",
    val clave: String = "",
    val recordar: Boolean = false,
    val errorUsuario: String? = null,
    val errorClave: String? = null,
    val mensajeErrorGeneral: String? = null,
    val mensajeExito: String? = null,
    val estaCargando: Boolean = false,
    val loginExitoso: Boolean = false
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    val repositorio: UsuarioRepositorio
    val licenciaRepositorio: LicenciaRepositorio
    private val serialDispositivo: String = IdDispositivo.obtenerSerialDispositivo(application)
    private val prefs = application.getSharedPreferences("typos_credenciales_pref", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        val baseDatos = AppBaseDatos.obtenerBaseDatos(application)
        repositorio = UsuarioRepositorio(baseDatos.usuarioDao())
        licenciaRepositorio = LicenciaRepositorio(baseDatos.licenciaDao(), serialDispositivo)
        cargarCredencialesGuardadas()
    }

    private fun cargarCredencialesGuardadas() {
        val recordarGuardado = prefs.getBoolean("recordar", false)
        if (recordarGuardado) {
            val usuarioGuardado = prefs.getString("usuario", "") ?: ""
            val claveGuardada = prefs.getString("clave", "") ?: ""
            _uiState.update {
                it.copy(
                    usuario = usuarioGuardado,
                    clave = claveGuardada,
                    recordar = true
                )
            }
        }
    }

    fun onUsuarioCambiado(nuevoUsuario: String) {
        _uiState.update {
            it.copy(
                usuario = nuevoUsuario,
                errorUsuario = null,
                mensajeErrorGeneral = null,
                mensajeExito = null
            )
        }
    }

    fun onClaveCambiada(nuevaClave: String) {
        _uiState.update {
            it.copy(
                clave = nuevaClave,
                errorClave = null,
                mensajeErrorGeneral = null,
                mensajeExito = null
            )
        }
    }

    fun onRecordarCambiado(nuevoRecordar: Boolean) {
        _uiState.update {
            it.copy(recordar = nuevoRecordar)
        }
        if (!nuevoRecordar) {
            prefs.edit()
                .putBoolean("recordar", false)
                .remove("usuario")
                .remove("clave")
                .apply()
        }
    }

    fun onUsuarioRegistradoConExito(nombreUsuario: String) {
        _uiState.update {
            it.copy(
                usuario = nombreUsuario,
                clave = "",
                errorUsuario = null,
                errorClave = null,
                mensajeErrorGeneral = null,
                mensajeExito = "¡Usuario '$nombreUsuario' activado con éxito! Ingresa tu contraseña para entrar."
            )
        }
    }

    fun iniciarSesion() {
        val estadoActual = _uiState.value
        val usuarioTrim = estadoActual.usuario.trim()
        val claveTrim = estadoActual.clave.trim()

        var hayError = false
        var errorUsuario: String? = null
        var errorClave: String? = null

        if (usuarioTrim.isEmpty()) {
            errorUsuario = "Ingresa tu usuario"
            hayError = true
        }

        if (claveTrim.isEmpty()) {
            errorClave = "Ingresa tu contraseña o PIN"
            hayError = true
        }

        if (hayError) {
            _uiState.update {
                it.copy(
                    errorUsuario = errorUsuario,
                    errorClave = errorClave
                )
            }
            return
        }

        _uiState.update { it.copy(estaCargando = true, mensajeErrorGeneral = null, mensajeExito = null) }

        viewModelScope.launch {
            // 1. Control estricto de licenciamiento: Bloquear si no hay licencia o está vencida
            val estadoLicencia = licenciaRepositorio.verificarEstadoLicencia()
            if (estadoLicencia !is EstadoLicencia.Activa) {
                val mensajeBloqueo = when (estadoLicencia) {
                    is EstadoLicencia.Vencida -> "Tu licencia ha vencido. Debes renovarla para poder iniciar sesión."
                    is EstadoLicencia.RelojAlterado -> "Fecha del sistema alterada. Sincroniza la hora de red para continuar."
                    else -> "Este dispositivo requiere una licencia activa para iniciar sesión."
                }
                _uiState.update {
                    it.copy(
                        estaCargando = false,
                        mensajeErrorGeneral = mensajeBloqueo
                    )
                }
                return@launch
            }

            // 2. Autenticar credenciales
            val resultado = repositorio.autenticar(usuarioTrim, claveTrim)
            when (resultado) {
                is ResultadoAutenticacion.Exito -> {
                    if (estadoActual.recordar) {
                        prefs.edit()
                            .putBoolean("recordar", true)
                            .putString("usuario", usuarioTrim)
                            .putString("clave", claveTrim)
                            .apply()
                    } else {
                        prefs.edit()
                            .putBoolean("recordar", false)
                            .remove("usuario")
                            .remove("clave")
                            .apply()
                    }

                    GestorSesion.iniciarSesion(resultado.usuario)
                    _uiState.update {
                        it.copy(
                            estaCargando = false,
                            loginExitoso = true
                        )
                    }
                }
                is ResultadoAutenticacion.Error -> {
                    _uiState.update {
                        it.copy(
                            estaCargando = false,
                            mensajeErrorGeneral = resultado.mensaje
                        )
                    }
                }
            }
        }
    }

    fun reiniciarEstado() {
        val recordarGuardado = prefs.getBoolean("recordar", false)
        if (recordarGuardado) {
            _uiState.value = LoginUiState(
                usuario = prefs.getString("usuario", "") ?: "",
                clave = prefs.getString("clave", "") ?: "",
                recordar = true
            )
        } else {
            _uiState.value = LoginUiState()
        }
    }
}
