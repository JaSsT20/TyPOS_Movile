package com.typdevstudio.typos_movil.datos.repositorio

import com.typdevstudio.typos_movil.datos.local.daos.UsuarioDao
import com.typdevstudio.typos_movil.datos.local.entidades.UsuarioEntidad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class UsuarioRepositorio(private val usuarioDao: UsuarioDao) {

    suspend fun autenticar(nombreUsuario: String, clave: String): ResultadoAutenticacion =
        withContext(Dispatchers.IO) {
            val usuario = usuarioDao.obtenerPorNombreUsuario(nombreUsuario.trim())
            if (usuario == null) {
                ResultadoAutenticacion.Error("El usuario ingresado no existe")
            } else if (usuario.clave != clave.trim()) {
                ResultadoAutenticacion.Error("La contraseña es incorrecta")
            } else if (!usuario.estaActivo) {
                ResultadoAutenticacion.Error("El usuario se encuentra inactivo")
            } else {
                ResultadoAutenticacion.Exito(usuario)
            }
        }

    fun obtenerUsuariosActivos(): Flow<List<UsuarioEntidad>> = usuarioDao.obtenerTodosActivos()

    suspend fun registrarUsuario(usuario: UsuarioEntidad): Long =
        withContext(Dispatchers.IO) {
            usuarioDao.insertar(usuario)
        }

    suspend fun contarUsuarios(): Int =
        withContext(Dispatchers.IO) {
            usuarioDao.contarUsuarios()
        }

    suspend fun existeUsuario(nombreUsuario: String): Boolean =
        withContext(Dispatchers.IO) {
            usuarioDao.buscarPorNombreUsuarioInsensible(nombreUsuario.trim()) != null
        }

    suspend fun registrarNuevoUsuario(
        nombreUsuario: String,
        nombreCompleto: String,
        clave: String,
        rol: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (existeUsuario(nombreUsuario)) {
                return@withContext false
            }
            val nuevo = UsuarioEntidad(
                nombreUsuario = nombreUsuario.trim().lowercase(),
                nombreCompleto = nombreCompleto.trim().ifBlank { nombreUsuario.trim() },
                clave = clave.trim(),
                rol = if (rol.equals("ADMINISTRADOR", ignoreCase = true)) "ADMINISTRADOR" else "CAJERO",
                estaActivo = true
            )
            usuarioDao.insertar(nuevo) > 0
        } catch (e: Exception) {
            false
        }
    }

    suspend fun actualizarPerfil(
        usuarioId: Long,
        nuevoNombreCompleto: String,
        nuevaFotoUri: String?,
        claveActual: String? = null,
        nuevaClave: String? = null
    ): ResultadoActualizacionPerfil = withContext(Dispatchers.IO) {
        try {
            val usuario = usuarioDao.obtenerPorId(usuarioId)
                ?: return@withContext ResultadoActualizacionPerfil.Error("El usuario no existe")

            var claveFinal = usuario.clave

            // Si se suministró nueva clave para cambiar
            if (!nuevaClave.isNullOrBlank()) {
                if (claveActual.isNullOrBlank()) {
                    return@withContext ResultadoActualizacionPerfil.Error("Ingresa tu contraseña o PIN actual")
                }
                if (usuario.clave != claveActual.trim()) {
                    return@withContext ResultadoActualizacionPerfil.Error("La contraseña actual es incorrecta")
                }
                if (nuevaClave.trim().length < 4) {
                    return@withContext ResultadoActualizacionPerfil.Error("La nueva contraseña debe tener al menos 4 caracteres")
                }
                claveFinal = nuevaClave.trim()
            }

            val nombreFinal = nuevoNombreCompleto.trim().ifBlank { usuario.nombreCompleto }

            val usuarioActualizado = usuario.copy(
                nombreCompleto = nombreFinal,
                fotoUri = nuevaFotoUri,
                clave = claveFinal
            )

            usuarioDao.actualizar(usuarioActualizado)
            ResultadoActualizacionPerfil.Exito(usuarioActualizado)
        } catch (e: Exception) {
            ResultadoActualizacionPerfil.Error("Error al guardar cambios: ${e.localizedMessage ?: "Error inesperado"}")
        }
    }
}

sealed interface ResultadoAutenticacion {
    data class Exito(val usuario: UsuarioEntidad) : ResultadoAutenticacion
    data class Error(val mensaje: String) : ResultadoAutenticacion
}

sealed interface ResultadoActualizacionPerfil {
    data class Exito(val usuario: UsuarioEntidad) : ResultadoActualizacionPerfil
    data class Error(val mensaje: String) : ResultadoActualizacionPerfil
}
