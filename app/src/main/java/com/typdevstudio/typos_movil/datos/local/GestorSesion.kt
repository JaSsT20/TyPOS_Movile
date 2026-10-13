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
