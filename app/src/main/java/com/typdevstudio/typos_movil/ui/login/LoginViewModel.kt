package com.typdevstudio.typos_movil.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.typdevstudio.typos_movil.datos.local.AppBaseDatos
import com.typdevstudio.typos_movil.datos.local.GestorSesion
import com.typdevstudio.typos_movil.datos.local.entidades.UsuarioEntidad
import com.typdevstudio.typos_movil.datos.repositorio.ResultadoAutenticacion
import com.typdevstudio.typos_movil.datos.repositorio.UsuarioRepositorio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val usuario: String = "",
    val clave: String = "",
    val errorUsuario: String? = null,
    val errorClave: String? = null,
    val mensajeErrorGeneral: String? = null,
    val estaCargando: Boolean = false,
    val loginExitoso: Boolean = false
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio: UsuarioRepositorio

    init {
        val baseDatos = AppBaseDatos.obtenerBaseDatos(application)
        repositorio = UsuarioRepositorio(baseDatos.usuarioDao())
    }

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsuarioCambiado(nuevoUsuario: String) {
        _uiState.update {
            it.copy(
                usuario = nuevoUsuario,
                errorUsuario = null,
                mensajeErrorGeneral = null
            )
        }
    }

    fun onClaveCambiada(nuevaClave: String) {
        _uiState.update {
            it.copy(
                clave = nuevaClave,
                errorClave = null,
                mensajeErrorGeneral = null
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

        _uiState.update { it.copy(estaCargando = true, mensajeErrorGeneral = null) }

        viewModelScope.launch {
            // Asegurar que exista al menos el admin inicial si es la primerísima ejecución
            if (repositorio.contarUsuarios() == 0) {
                repositorio.registrarUsuario(
                    UsuarioEntidad(
                        nombreCompleto = "Administrador",
                        nombreUsuario = "admin",
                        clave = "admin",
                        rol = "ADMINISTRADOR",
                        estaActivo = true
                    )
                )
            }

            val resultado = repositorio.autenticar(usuarioTrim, claveTrim)
            when (resultado) {
                is ResultadoAutenticacion.Exito -> {
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
        _uiState.value = LoginUiState()
    }
}
