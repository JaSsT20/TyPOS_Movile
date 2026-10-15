package com.typdevstudio.typos_movil.ui.login

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.typdevstudio.typos_movil.datos.local.AppBaseDatos
import com.typdevstudio.typos_movil.datos.local.GestorSesion
import com.typdevstudio.typos_movil.datos.local.entidades.UsuarioEntidad
import com.typdevstudio.typos_movil.datos.repositorio.ResultadoAutenticacion
import com.typdevstudio.typos_movil.datos.repositorio.ResultadoRegistroUsuario
import com.typdevstudio.typos_movil.datos.repositorio.UsuarioRepositorio
import com.typdevstudio.typos_movil.ui.licencia.LicenciaViewModel
import com.typdevstudio.typos_movil.utilidades.licencia.LicenciaGuard
import com.typdevstudio.typos_movil.utilidades.licencia.SincronizacionUsuarios
import com.typdevstudio.typos_movil.utilidades.licencia.nube.ResultadoLicencia
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

/** Qué formulario de bienvenida está abierto cuando el teléfono no tiene usuarios. */
enum class FormularioBienvenida { CREAR_ADMINISTRADOR, ACTIVAR_LICENCIA }

/** Primer uso del teléfono (sin usuarios): crear el administrador o traer los usuarios de una licencia. */
data class BienvenidaUiState(
    val formulario: FormularioBienvenida? = null,
    val nombreUsuario: String = "",
    val nombreCompleto: String = "",
    val clave: String = "",
    val confirmarClave: String = "",
    val claveLicencia: String = "",
    val nombreTelefono: String = "",
    val estaGuardando: Boolean = false,
    val error: String? = null,
    /** Aviso tras activar la licencia (por ejemplo, que todavía no tiene usuarios). */
    val aviso: String? = null,
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    val repositorio: UsuarioRepositorio
    private val prefs = application.getSharedPreferences("typos_credenciales_pref", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** `true` si el teléfono no tiene ningún usuario (primer uso); `null` mientras se consulta. */
    val sinUsuarios: StateFlow<Boolean?>

    private val _bienvenida = MutableStateFlow(BienvenidaUiState())
    val bienvenida: StateFlow<BienvenidaUiState> = _bienvenida.asStateFlow()

    init {
        val baseDatos = AppBaseDatos.obtenerBaseDatos(application)
        repositorio = UsuarioRepositorio(baseDatos.usuarioDao())
        sinUsuarios = baseDatos.usuarioDao().observarTodos()
            .map { it.isEmpty() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
        cargarCredencialesGuardadas()
        viewModelScope.launch { repositorio.convertirClavesAntiguas() }
    }

    /**
     * "Recordar" guarda solo el nombre de usuario: la contraseña nunca se guarda.
     * Las versiones anteriores la guardaban en texto plano; si quedó alguna, se borra aquí.
     */
    private fun cargarCredencialesGuardadas() {
        if (prefs.contains(CLAVE_ANTIGUA)) {
            prefs.edit().remove(CLAVE_ANTIGUA).commit()
        }
        _uiState.value = estadoInicial()
    }

    private fun estadoInicial(): LoginUiState =
        if (prefs.getBoolean("recordar", false)) {
            LoginUiState(usuario = prefs.getString("usuario", "") ?: "", recordar = true)
        } else {
            LoginUiState()
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
            // La licencia no bloquea el inicio de sesión: en modo restringido se entra para consultar
            // la información (LicenciaGuard decide qué acciones se permiten).
            val resultado = repositorio.autenticar(usuarioTrim, claveTrim)
            when (resultado) {
                is ResultadoAutenticacion.Exito -> {
                    if (estadoActual.recordar) {
                        prefs.edit()
                            .putBoolean("recordar", true)
                            .putString("usuario", usuarioTrim)
                            .apply()
                    } else {
                        prefs.edit()
                            .putBoolean("recordar", false)
                            .remove("usuario")
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

    // ---------- Primer uso: teléfono sin usuarios ----------

    fun abrirBienvenida(formulario: FormularioBienvenida) = _bienvenida.update {
        BienvenidaUiState(
            formulario = formulario,
            nombreTelefono = LicenciaViewModel.nombreTelefonoPorDefecto(),
            aviso = it.aviso,
        )
    }

    fun cerrarBienvenida() = _bienvenida.update { if (it.estaGuardando) it else it.copy(formulario = null, error = null) }

    fun editarBienvenida(cambio: (BienvenidaUiState) -> BienvenidaUiState) =
        _bienvenida.update { cambio(it).copy(error = null) }

    /** Crea el primer administrador y entra con él. Con licencia en la nube, se crea en la licencia. */
    fun crearAdministrador() {
        val f = _bienvenida.value
        val error = when {
            !SincronizacionUsuarios.nombreValido(f.nombreUsuario) ->
                "El usuario debe tener de 1 a 50 letras, números o . _ @ - (sin espacios)."
            f.nombreCompleto.isBlank() -> "Ingresa tu nombre completo."
            f.clave.trim().length < 4 -> "La contraseña debe tener al menos 4 caracteres."
            f.clave != f.confirmarClave -> "Las contraseñas no coinciden."
            else -> null
        }
        if (error != null) {
            _bienvenida.update { it.copy(error = error) }
            return
        }
        _bienvenida.update { it.copy(estaGuardando = true) }
        viewModelScope.launch {
            val resultado = repositorio.registrarNuevoUsuario(f.nombreUsuario, f.nombreCompleto, f.clave, "ADMINISTRADOR")
            if (resultado is ResultadoRegistroUsuario.Error) {
                _bienvenida.update { it.copy(estaGuardando = false, error = resultado.mensaje) }
                return@launch
            }
            when (val acceso = repositorio.autenticar(f.nombreUsuario, f.clave)) {
                is ResultadoAutenticacion.Exito -> {
                    _bienvenida.value = BienvenidaUiState()
                    GestorSesion.iniciarSesion(acceso.usuario)
                    _uiState.update { it.copy(loginExitoso = true) }
                }
                is ResultadoAutenticacion.Error -> {
                    // No debería pasar: se deja en el inicio de sesión normal con el usuario escrito.
                    _bienvenida.value = BienvenidaUiState()
                    _uiState.update { it.copy(usuario = f.nombreUsuario.trim().lowercase(), mensajeErrorGeneral = acceso.mensaje) }
                }
            }
        }
    }

    /** Activa la licencia en este teléfono y trae sus usuarios para iniciar sesión con uno de ellos. */
    fun activarLicencia() {
        val f = _bienvenida.value
        val clave = LicenciaViewModel.claveNormalizada(f.claveLicencia)
        if (clave == null) {
            _bienvenida.update { it.copy(error = "Ingresa la clave completa: 12 letras o números (XXXX-XXXX-XXXX).") }
            return
        }
        _bienvenida.update { it.copy(estaGuardando = true, claveLicencia = clave) }
        viewModelScope.launch {
            val resultado = LicenciaGuard.activar(clave, f.nombreTelefono)
            if (resultado !is ResultadoLicencia.Exito) {
                _bienvenida.update { it.copy(estaGuardando = false, error = LicenciaViewModel.mensajeDe(resultado, "")) }
                return@launch
            }
            if (repositorio.contarUsuarios() > 0) {
                // Llegaron los usuarios de la licencia: la pantalla pasa sola al inicio de sesión.
                _bienvenida.value = BienvenidaUiState()
                _uiState.update { it.copy(mensajeExito = "¡Licencia activada! Inicia sesión con tu usuario.") }
            } else {
                _bienvenida.value = BienvenidaUiState(
                    aviso = "¡Licencia activada! Tu licencia todavía no tiene usuarios: crea el administrador. " +
                        "Si ya tenía usuarios, revisa tu conexión y vuelve a intentarlo."
                )
            }
        }
    }

    fun reiniciarEstado() {
        _uiState.value = estadoInicial()
    }

    private companion object {
        /** Preferencia donde las versiones anteriores guardaban la contraseña en texto plano. */
        const val CLAVE_ANTIGUA = "clave"
    }
}
