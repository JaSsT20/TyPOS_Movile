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
}

sealed interface ResultadoAutenticacion {
    data class Exito(val usuario: UsuarioEntidad) : ResultadoAutenticacion
    data class Error(val mensaje: String) : ResultadoAutenticacion
}
