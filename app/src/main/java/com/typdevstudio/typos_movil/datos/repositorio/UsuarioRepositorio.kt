package com.typdevstudio.typos_movil.datos.repositorio

import com.typdevstudio.typos_movil.datos.local.daos.UsuarioDao
import com.typdevstudio.typos_movil.datos.local.entidades.UsuarioEntidad
import com.typdevstudio.typos_movil.utilidades.licencia.GestorUsuarios
import com.typdevstudio.typos_movil.utilidades.licencia.UsuariosLicencia
import com.typdevstudio.typos_movil.utilidades.licencia.nube.HashClave
import com.typdevstudio.typos_movil.utilidades.licencia.nube.ResultadoUsuarios
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Usuarios del teléfono. Las contraseñas se guardan como hash PBKDF2 ([HashClave]); las antiguas en texto plano
 * se siguen aceptando y se convierten en hash al abrir el login ([convertirClavesAntiguas]) o al iniciar sesión.
 *
 * Si el teléfono tiene licencia en la nube, crear usuarios y cambiar nombre o contraseña pasa por la licencia
 * (requiere internet) para que el cambio llegue a los demás teléfonos; sin licencia nueva se hace solo aquí.
 */
class UsuarioRepositorio(
    private val usuarioDao: UsuarioDao,
    private val usuariosLicencia: UsuariosLicencia = GestorUsuarios,
) {

    suspend fun autenticar(nombreUsuario: String, clave: String): ResultadoAutenticacion =
        withContext(Dispatchers.IO) {
            val usuario = usuarioDao.buscarPorNombreUsuarioInsensible(nombreUsuario.trim())
            val claveTrim = clave.trim()
            when {
                usuario == null -> ResultadoAutenticacion.Error("El usuario ingresado no existe")
                !ClaveLocal.coincide(claveTrim, usuario.clave) -> ResultadoAutenticacion.Error("La contraseña es incorrecta")
                !usuario.estaActivo -> ResultadoAutenticacion.Error("El usuario se encuentra inactivo")
                HashClave.esHash(usuario.clave) -> ResultadoAutenticacion.Exito(usuario)
                else -> {
                    // Contraseña antigua en texto plano: se reemplaza por su hash. La licencia la recibe ya en hash
                    // en la próxima sincronización (si el servidor ya tenía este usuario, su hash manda).
                    val convertido = usuario.copy(clave = HashClave.generar(claveTrim))
                    usuarioDao.actualizar(convertido)
                    ResultadoAutenticacion.Exito(convertido)
                }
            }
        }

    /**
     * Reemplaza por su hash toda contraseña que siga en texto plano (versiones anteriores, o un respaldo antiguo
     * restaurado), sin esperar a que ese usuario inicie sesión. Devuelve cuántas se convirtieron.
     */
    suspend fun convertirClavesAntiguas(): Int =
        withContext(Dispatchers.IO) {
            val pendientes = usuarioDao.obtenerTodos().filter { it.clave.isNotEmpty() && !HashClave.esHash(it.clave) }
            pendientes.forEach { usuarioDao.actualizar(it.copy(clave = HashClave.generar(it.clave))) }
            pendientes.size
        }

    fun obtenerUsuariosActivos(): Flow<List<UsuarioEntidad>> = usuarioDao.obtenerTodosActivos()

    suspend fun contarUsuarios(): Int =
        withContext(Dispatchers.IO) {
            usuarioDao.contarUsuarios()
        }

    /** Usuarios activos: los que cuentan contra el límite `max_usuarios` del plan. */
    suspend fun contarUsuariosActivos(): Int =
        withContext(Dispatchers.IO) {
            usuarioDao.contarUsuariosActivos()
        }

    suspend fun existeUsuario(nombreUsuario: String): Boolean =
        withContext(Dispatchers.IO) {
            usuarioDao.buscarPorNombreUsuarioInsensible(nombreUsuario.trim()) != null
        }

    /**
     * Crea un usuario. Con licencia en la nube lo crea el servidor (revisa el límite del plan y lo reparte a los
     * demás teléfonos); sin licencia nueva se guarda solo en este teléfono y se sube al activar.
     */
    suspend fun registrarNuevoUsuario(
        nombreUsuario: String,
        nombreCompleto: String,
        clave: String,
        rol: String
    ): ResultadoRegistroUsuario = withContext(Dispatchers.IO) {
        try {
            val nombre = nombreUsuario.trim().lowercase()
            val nombreFinal = nombreCompleto.trim().ifBlank { nombreUsuario.trim() }
            val rolFinal = if (rol.equals("ADMINISTRADOR", ignoreCase = true)) "ADMINISTRADOR" else "CAJERO"
            if (existeUsuario(nombre)) {
                return@withContext ResultadoRegistroUsuario.Error("El usuario '$nombre' ya está registrado.")
            }

            when (val remoto = usuariosLicencia.crear(nombre, nombreFinal, rolFinal, clave.trim())) {
                is ResultadoUsuarios.Exito -> ResultadoRegistroUsuario.Exito
                is ResultadoUsuarios.Rechazada -> ResultadoRegistroUsuario.Error(remoto.mensaje)
                is ResultadoUsuarios.SinConexion ->
                    ResultadoRegistroUsuario.Error("Para crear usuarios se necesita internet. ${remoto.mensaje}")
                ResultadoUsuarios.SinLicencia -> {
                    val nuevo = UsuarioEntidad(
                        nombreUsuario = nombre,
                        nombreCompleto = nombreFinal,
                        clave = HashClave.generar(clave.trim()),
                        rol = rolFinal,
                        estaActivo = true
                    )
                    if (usuarioDao.insertar(nuevo) > 0) ResultadoRegistroUsuario.Exito
                    else ResultadoRegistroUsuario.Error("No se pudo registrar el usuario.")
                }
            }
        } catch (e: Exception) {
            ResultadoRegistroUsuario.Error("No se pudo registrar el usuario. Es posible que ya exista.")
        }
    }

    /**
     * Cambia nombre, foto y contraseña del usuario. La foto es solo de este teléfono; el nombre y la contraseña,
     * con licencia en la nube, se cambian en la licencia (requiere internet).
     */
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

            var claveNueva: String? = null

            // Si se suministró nueva clave para cambiar
            if (!nuevaClave.isNullOrBlank()) {
                if (claveActual.isNullOrBlank()) {
                    return@withContext ResultadoActualizacionPerfil.Error("Ingresa tu contraseña o PIN actual")
                }
                if (!ClaveLocal.coincide(claveActual.trim(), usuario.clave)) {
                    return@withContext ResultadoActualizacionPerfil.Error("La contraseña actual es incorrecta")
                }
                if (nuevaClave.trim().length < 4) {
                    return@withContext ResultadoActualizacionPerfil.Error("La nueva contraseña debe tener al menos 4 caracteres")
                }
                claveNueva = nuevaClave.trim()
            }

            val nombreFinal = nuevoNombreCompleto.trim().ifBlank { usuario.nombreCompleto }
            val cambiaNombre = nombreFinal != usuario.nombreCompleto

            var base = usuario
            if (cambiaNombre || claveNueva != null) {
                when (val remoto = actualizarEnLicencia(usuario.nombreUsuario, nombreCompleto = nombreFinal.takeIf { cambiaNombre }, claveNueva = claveNueva)) {
                    // La licencia ya copió el nombre y la contraseña al teléfono.
                    is ResultadoUsuarios.Exito -> base = usuarioDao.obtenerPorId(usuarioId) ?: usuario
                    is ResultadoUsuarios.Rechazada -> return@withContext ResultadoActualizacionPerfil.Error(remoto.mensaje)
                    is ResultadoUsuarios.SinConexion -> return@withContext ResultadoActualizacionPerfil.Error(
                        "Para cambiar el nombre o la contraseña se necesita internet. ${remoto.mensaje}"
                    )
                    ResultadoUsuarios.SinLicencia -> base = usuario.copy(
                        nombreCompleto = nombreFinal,
                        clave = claveNueva?.let { HashClave.generar(it) } ?: usuario.clave
                    )
                }
            }

            val usuarioActualizado = base.copy(fotoUri = nuevaFotoUri)
            usuarioDao.actualizar(usuarioActualizado)
            ResultadoActualizacionPerfil.Exito(usuarioActualizado)
        } catch (e: Exception) {
            ResultadoActualizacionPerfil.Error("Error al guardar cambios: ${e.localizedMessage ?: "Error inesperado"}")
        }
    }

    /**
     * Cambios que hace un administrador sobre otro usuario: rol, activar/desactivar y restablecer la contraseña
     * (sin pedir la actual). Nunca deja el teléfono sin administrador activo ([ReglasUsuarios]).
     */
    suspend fun administrarUsuario(
        usuarioId: Long,
        rol: String? = null,
        activo: Boolean? = null,
        claveNueva: String? = null,
    ): ResultadoActualizacionPerfil = withContext(Dispatchers.IO) {
        try {
            val usuario = usuarioDao.obtenerPorId(usuarioId)
                ?: return@withContext ResultadoActualizacionPerfil.Error("El usuario no existe")
            val rolFinal = rol?.let { if (it.equals("ADMINISTRADOR", ignoreCase = true)) "ADMINISTRADOR" else "CAJERO" }
            val claveFinal = claveNueva?.trim()
            if (claveFinal != null && claveFinal.length < 4) {
                return@withContext ResultadoActualizacionPerfil.Error("La contraseña debe tener al menos 4 caracteres")
            }
            ReglasUsuarios.motivoParaNoCambiar(usuarioDao.obtenerTodos(), usuario, rolFinal, activo)?.let {
                return@withContext ResultadoActualizacionPerfil.Error(it)
            }

            when (val remoto = actualizarEnLicencia(usuario.nombreUsuario, rol = rolFinal, activo = activo, claveNueva = claveFinal)) {
                is ResultadoUsuarios.Exito -> ResultadoActualizacionPerfil.Exito(usuarioDao.obtenerPorId(usuarioId) ?: usuario)
                is ResultadoUsuarios.Rechazada -> ResultadoActualizacionPerfil.Error(remoto.mensaje)
                is ResultadoUsuarios.SinConexion -> ResultadoActualizacionPerfil.Error(
                    "Para cambiar usuarios se necesita internet. ${remoto.mensaje}"
                )
                ResultadoUsuarios.SinLicencia -> {
                    val actualizado = usuario.copy(
                        rol = rolFinal ?: usuario.rol,
                        estaActivo = activo ?: usuario.estaActivo,
                        clave = claveFinal?.let { HashClave.generar(it) } ?: usuario.clave,
                    )
                    usuarioDao.actualizar(actualizado)
                    ResultadoActualizacionPerfil.Exito(actualizado)
                }
            }
        } catch (e: Exception) {
            ResultadoActualizacionPerfil.Error("Error al guardar cambios: ${e.localizedMessage ?: "Error inesperado"}")
        }
    }

    /** Si la licencia todavía no conoce al usuario (nunca se subió), se sincroniza y se reintenta una vez. */
    private suspend fun actualizarEnLicencia(
        nombreUsuario: String,
        nombreCompleto: String? = null,
        claveNueva: String? = null,
        rol: String? = null,
        activo: Boolean? = null,
    ): ResultadoUsuarios {
        suspend fun intentar() = usuariosLicencia.actualizar(nombreUsuario, nombreCompleto, rol, claveNueva, activo)
        val primero = intentar()
        if (primero !is ResultadoUsuarios.Rechazada || primero.codigo != "USUARIO_NO_ENCONTRADO") return primero
        val sincronizado = usuariosLicencia.sincronizar()
        if (sincronizado !is ResultadoUsuarios.Exito) return sincronizado
        return intentar()
    }
}

/** Reglas que protegen el acceso al teléfono al administrar usuarios (las mismas que aplica la API). */
object ReglasUsuarios {
    /**
     * Por qué no se puede aplicar el cambio, o `null` si se puede. No se permite dejar sin ningún administrador
     * activo a un teléfono que lo tenía: nadie podría volver a entrar a Licencia ni a Configuración.
     */
    fun motivoParaNoCambiar(todos: List<UsuarioEntidad>, usuario: UsuarioEntidad, rol: String?, activo: Boolean?): String? {
        val esAdminActivo = usuario.estaActivo && usuario.rol == "ADMINISTRADOR"
        val seguiraAdminActivo = (activo ?: usuario.estaActivo) && (rol ?: usuario.rol) == "ADMINISTRADOR"
        val otrosAdmins = todos.count { it.id != usuario.id && it.estaActivo && it.rol == "ADMINISTRADOR" }
        return if (esAdminActivo && !seguiraAdminActivo && otrosAdmins == 0)
            "Debe quedar al menos un administrador activo. Nombra otro administrador antes de hacer este cambio."
        else null
    }
}

/** Comparación de la contraseña escrita con la guardada en el teléfono. */
object ClaveLocal {
    /** Acepta el hash actual y, por compatibilidad, la contraseña antigua en texto plano. */
    fun coincide(clave: String, guardada: String): Boolean =
        if (HashClave.esHash(guardada)) HashClave.verificar(clave, guardada)
        else guardada.isNotEmpty() && guardada == clave
}

sealed interface ResultadoAutenticacion {
    data class Exito(val usuario: UsuarioEntidad) : ResultadoAutenticacion
    data class Error(val mensaje: String) : ResultadoAutenticacion
}

sealed interface ResultadoRegistroUsuario {
    data object Exito : ResultadoRegistroUsuario
    data class Error(val mensaje: String) : ResultadoRegistroUsuario
}

sealed interface ResultadoActualizacionPerfil {
    data class Exito(val usuario: UsuarioEntidad) : ResultadoActualizacionPerfil
    data class Error(val mensaje: String) : ResultadoActualizacionPerfil
}
