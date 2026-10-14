package com.typdevstudio.typos_movil.ui.usuarios

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.typdevstudio.typos_movil.datos.local.AppBaseDatos
import com.typdevstudio.typos_movil.datos.local.GestorSesion
import com.typdevstudio.typos_movil.datos.local.entidades.UsuarioEntidad
import com.typdevstudio.typos_movil.datos.repositorio.ResultadoActualizacionPerfil
import com.typdevstudio.typos_movil.datos.repositorio.ResultadoRegistroUsuario
import com.typdevstudio.typos_movil.datos.repositorio.UsuarioRepositorio
import com.typdevstudio.typos_movil.utilidades.licencia.GestorUsuarios
import com.typdevstudio.typos_movil.utilidades.licencia.LicenciaGuard
import com.typdevstudio.typos_movil.utilidades.licencia.ResumenUsuariosLicencia
import com.typdevstudio.typos_movil.utilidades.licencia.SincronizacionUsuarios
import com.typdevstudio.typos_movil.utilidades.licencia.nube.ResultadoUsuarios
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Formulario para crear un usuario o restablecer la contraseña de uno existente. */
data class FormularioUsuario(
    /** `null` = crear uno nuevo; si no, restablecer la contraseña de este usuario. */
    val usuario: UsuarioEntidad? = null,
    val nombreUsuario: String = "",
    val nombreCompleto: String = "",
    val rol: String = "CAJERO",
    val clave: String = "",
    val confirmarClave: String = "",
    val error: String? = null,
)

data class UsuariosUiState(
    val estaSincronizando: Boolean = false,
    val estaGuardando: Boolean = false,
    /** La lista viene de la licencia; `false` = solo este teléfono (sin licencia nueva). */
    val conLicencia: Boolean = false,
    val formulario: FormularioUsuario? = null,
    /** Usuario cuyas acciones (rol, activar, contraseña) se están mostrando. */
    val seleccionado: UsuarioEntidad? = null,
    val mensaje: String? = null,
    val esError: Boolean = false,
)

/**
 * Pantalla Usuarios (solo administrador). La licencia manda: crear y cambiar usuarios pasa por la API y la lista
 * se copia a todos los teléfonos. Sin licencia nueva, los usuarios se manejan solo en este teléfono.
 */
class UsuariosViewModel(application: Application) : AndroidViewModel(application) {

    private val usuarioDao = AppBaseDatos.obtenerBaseDatos(application).usuarioDao()
    private val repositorio = UsuarioRepositorio(usuarioDao)

    val usuarios: StateFlow<List<UsuarioEntidad>> = usuarioDao.observarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val resumen: StateFlow<ResumenUsuariosLicencia?> = GestorUsuarios.resumen

    private val _uiState = MutableStateFlow(UsuariosUiState(conLicencia = LicenciaGuard.situacion.value.clave != null))
    val uiState: StateFlow<UsuariosUiState> = _uiState.asStateFlow()

    init {
        sincronizar(silencioso = true)
    }

    /** Trae de la licencia los cambios hechos en otros teléfonos. */
    fun sincronizar(silencioso: Boolean = false) {
        if (LicenciaGuard.situacion.value.clave == null) {
            _uiState.update { it.copy(conLicencia = false) }
            return
        }
        _uiState.update { it.copy(estaSincronizando = true, conLicencia = true) }
        viewModelScope.launch {
            val resultado = GestorUsuarios.sincronizar()
            _uiState.update {
                when (resultado) {
                    is ResultadoUsuarios.Exito -> it.copy(
                        estaSincronizando = false,
                        mensaje = if (silencioso) it.mensaje else "Usuarios actualizados.",
                        esError = if (silencioso) it.esError else false,
                    )
                    is ResultadoUsuarios.Rechazada -> it.copy(estaSincronizando = false, mensaje = resultado.mensaje, esError = true)
                    is ResultadoUsuarios.SinConexion -> it.copy(
                        estaSincronizando = false,
                        mensaje = "Sin conexión: se muestra la copia guardada en este teléfono.",
                        esError = true,
                    )
                    ResultadoUsuarios.SinLicencia -> it.copy(estaSincronizando = false, conLicencia = false)
                }
            }
        }
    }

    // ---------- Crear usuario y restablecer contraseña ----------

    fun abrirNuevo() {
        viewModelScope.launch {
            val motivo = LicenciaGuard.actualizar().motivoParaNoCrearUsuario(usuarioDao.contarUsuariosActivos())
            if (motivo != null) mostrarMensaje(motivo, esError = true)
            else _uiState.update { it.copy(formulario = FormularioUsuario(), mensaje = null) }
        }
    }

    fun abrirRestablecerClave(usuario: UsuarioEntidad) =
        _uiState.update { it.copy(seleccionado = null, formulario = FormularioUsuario(usuario = usuario, rol = usuario.rol)) }

    fun cerrarFormulario() = _uiState.update { it.copy(formulario = null) }

    fun editarFormulario(cambio: (FormularioUsuario) -> FormularioUsuario) =
        _uiState.update { estado -> estado.copy(formulario = estado.formulario?.let { cambio(it).copy(error = null) }) }

    fun guardarFormulario() {
        val formulario = _uiState.value.formulario ?: return
        validar(formulario)?.let { error ->
            _uiState.update { it.copy(formulario = formulario.copy(error = error)) }
            return
        }
        _uiState.update { it.copy(estaGuardando = true) }
        viewModelScope.launch {
            val existente = formulario.usuario
            val error: String? = if (existente == null) {
                when (val r = repositorio.registrarNuevoUsuario(
                    formulario.nombreUsuario, formulario.nombreCompleto, formulario.clave, formulario.rol
                )) {
                    ResultadoRegistroUsuario.Exito -> null
                    is ResultadoRegistroUsuario.Error -> r.mensaje
                }
            } else {
                when (val r = repositorio.administrarUsuario(existente.id, claveNueva = formulario.clave)) {
                    is ResultadoActualizacionPerfil.Exito -> null
                    is ResultadoActualizacionPerfil.Error -> r.mensaje
                }
            }
            _uiState.update {
                if (error == null) {
                    val texto = if (existente == null) "Usuario '${SincronizacionUsuarios.normalizarNombre(formulario.nombreUsuario)}' creado."
                    else "Contraseña de '${existente.nombreUsuario}' cambiada."
                    it.copy(estaGuardando = false, formulario = null, mensaje = texto, esError = false)
                } else {
                    it.copy(estaGuardando = false, formulario = formulario.copy(error = error))
                }
            }
        }
    }

    private fun validar(f: FormularioUsuario): String? = when {
        f.usuario == null && !SincronizacionUsuarios.nombreValido(f.nombreUsuario) ->
            "El usuario debe tener de 1 a 50 letras, números o . _ @ - (sin espacios)."
        f.usuario == null && f.nombreCompleto.isBlank() -> "Ingresa el nombre completo."
        f.usuario == null && f.nombreCompleto.trim().length > 100 -> "El nombre completo admite hasta 100 caracteres."
        f.clave.trim().length < 4 -> "La contraseña debe tener al menos 4 caracteres."
        f.clave != f.confirmarClave -> "Las contraseñas no coinciden."
        else -> null
    }

    // ---------- Acciones sobre un usuario ----------

    fun seleccionar(usuario: UsuarioEntidad?) = _uiState.update { it.copy(seleccionado = usuario) }

    fun cambiarRol(usuario: UsuarioEntidad) {
        val nuevo = if (usuario.rol == "ADMINISTRADOR") "CAJERO" else "ADMINISTRADOR"
        aplicar(usuario, "Ahora '${usuario.nombreUsuario}' es ${if (nuevo == "ADMINISTRADOR") "administrador" else "cajero"}.") {
            repositorio.administrarUsuario(usuario.id, rol = nuevo)
        }
    }

    fun cambiarActivo(usuario: UsuarioEntidad) {
        val activar = !usuario.estaActivo
        if (!activar && usuario.id == GestorSesion.usuarioActivo.value?.id) {
            _uiState.update { it.copy(seleccionado = null) }
            mostrarMensaje("No puedes desactivar el usuario con el que iniciaste sesión.", esError = true)
            return
        }
        viewModelScope.launch {
            // Reactivar vuelve a contar contra el límite del plan.
            val motivo = if (activar) LicenciaGuard.actualizar().motivoParaNoCrearUsuario(usuarioDao.contarUsuariosActivos()) else null
            if (motivo != null) {
                _uiState.update { it.copy(seleccionado = null) }
                mostrarMensaje(motivo, esError = true)
                return@launch
            }
            aplicar(usuario, "Usuario '${usuario.nombreUsuario}' ${if (activar) "activado" else "desactivado"}.") {
                repositorio.administrarUsuario(usuario.id, activo = activar)
            }
        }
    }

    private fun aplicar(usuario: UsuarioEntidad, textoExito: String, operacion: suspend () -> ResultadoActualizacionPerfil) {
        _uiState.update { it.copy(seleccionado = null, estaGuardando = true) }
        viewModelScope.launch {
            val resultado = operacion()
            if (resultado is ResultadoActualizacionPerfil.Exito && usuario.id == GestorSesion.usuarioActivo.value?.id) {
                GestorSesion.iniciarSesion(resultado.usuario)
            }
            _uiState.update { it.copy(estaGuardando = false) }
            when (resultado) {
                is ResultadoActualizacionPerfil.Exito -> mostrarMensaje(textoExito, esError = false)
                is ResultadoActualizacionPerfil.Error -> mostrarMensaje(resultado.mensaje, esError = true)
            }
        }
    }

    fun limpiarMensaje() = _uiState.update { it.copy(mensaje = null) }

    private fun mostrarMensaje(mensaje: String, esError: Boolean) =
        _uiState.update { it.copy(mensaje = mensaje, esError = esError) }
}
