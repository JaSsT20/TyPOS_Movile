package com.typdevstudio.typos_movil.datos.local

import com.typdevstudio.typos_movil.datos.local.daos.UsuarioDao
import com.typdevstudio.typos_movil.datos.local.entidades.UsuarioEntidad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object GestorSesion {
    private val _usuarioActivo = MutableStateFlow<UsuarioEntidad?>(null)
    val usuarioActivo: StateFlow<UsuarioEntidad?> = _usuarioActivo.asStateFlow()

    fun iniciarSesion(usuario: UsuarioEntidad) {
        _usuarioActivo.value = usuario
    }

    fun cerrarSesion() {
        _usuarioActivo.value = null
    }

    private val _avisoSesionCerrada = MutableStateFlow<String?>(null)

    /** Motivo por el que se cerró la sesión desde la licencia (usuario desactivado en el portal o en otro teléfono). */
    val avisoSesionCerrada: StateFlow<String?> = _avisoSesionCerrada.asStateFlow()

    /** Cierra la sesión porque la licencia ya no permite a este usuario; la app lo avisa y vuelve al inicio de sesión. */
    fun cerrarPorLicencia(motivo: String) {
        _usuarioActivo.value = null
        _avisoSesionCerrada.value = motivo
    }

    fun descartarAvisoSesionCerrada() {
        _avisoSesionCerrada.value = null
    }

    fun esAdministrador(): Boolean {
        return _usuarioActivo.value?.rol == "ADMINISTRADOR"
    }

    /**
     * Acceso a secciones de administrador (licencia, configuración, respaldos). Si en el teléfono no existe
     * ningún administrador activo se permite a cualquiera, para que nadie quede sin acceso.
     */
    suspend fun tieneAccesoAdministrador(usuarioDao: UsuarioDao): Boolean =
        esAdministrador() || usuarioDao.contarAdministradoresActivos() == 0

    fun obtenerNombreUsuario(): String {
        return _usuarioActivo.value?.nombreCompleto ?: "Sin usuario"
    }
}
