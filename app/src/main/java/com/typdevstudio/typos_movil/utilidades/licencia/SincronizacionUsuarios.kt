package com.typdevstudio.typos_movil.utilidades.licencia

import com.typdevstudio.typos_movil.datos.local.entidades.UsuarioEntidad
import com.typdevstudio.typos_movil.utilidades.licencia.nube.HashClave
import com.typdevstudio.typos_movil.utilidades.licencia.nube.ListaUsuariosRemota
import com.typdevstudio.typos_movil.utilidades.licencia.nube.UsuarioRemoto

/** Cambios que hay que hacer en Room para que el teléfono quede igual que la licencia. */
data class CambiosUsuarios(
    val insertar: List<UsuarioEntidad>,
    val actualizar: List<UsuarioEntidad>,
)

/**
 * Reglas puras de la sincronización de usuarios entre el teléfono (Room) y la licencia (API).
 * Manda el servidor: lo que diga la licencia reemplaza lo local. El servidor nunca borra usuarios
 * (solo los desactiva), así que un usuario local que no está en la licencia es uno que nunca se subió.
 */
object SincronizacionUsuarios {

    /** Mismo formato que acepta la API para `nombre_usuario` (ya en minúsculas). */
    private val FORMATO_NOMBRE = Regex("^[\\p{Ll}\\p{Lo}\\p{Nd}._@-]{1,50}$")

    fun normalizarNombre(nombre: String): String = nombre.trim().lowercase()

    fun nombreValido(nombre: String): Boolean = FORMATO_NOMBRE.matches(normalizarNombre(nombre))

    /**
     * Usuarios del teléfono que la licencia no conoce y se pueden subir. Las contraseñas antiguas en texto plano
     * se convierten en hash aquí: a la API nunca le llega la contraseña real.
     */
    fun pendientesDeSubir(
        locales: List<UsuarioEntidad>,
        remotos: List<UsuarioRemoto>,
        generarHash: (String) -> String = { HashClave.generar(it) },
    ): List<UsuarioRemoto> {
        val conocidos = remotos.map { normalizarNombre(it.nombreUsuario) }.toSet()
        return locales
            .filter { normalizarNombre(it.nombreUsuario) !in conocidos && nombreValido(it.nombreUsuario) }
            .map { local ->
                UsuarioRemoto(
                    nombreUsuario = normalizarNombre(local.nombreUsuario),
                    nombreCompleto = local.nombreCompleto.trim().ifBlank { local.nombreUsuario }.take(100),
                    rol = if (local.rol.equals("ADMINISTRADOR", ignoreCase = true)) "ADMINISTRADOR" else "CAJERO",
                    hashClave = if (HashClave.esHash(local.clave)) local.clave else generarHash(local.clave),
                    activo = local.estaActivo,
                )
            }
    }

    /**
     * Qué insertar y qué actualizar en Room según la lista de la licencia. Los usuarios locales que el servidor
     * rechazó por el límite del plan quedan inactivos en el teléfono. Los demás que no están en la lista
     * (nombres que la API no acepta) no se tocan, para no dejar a nadie fuera por un detalle de formato.
     */
    fun calcularCambios(locales: List<UsuarioEntidad>, lista: ListaUsuariosRemota): CambiosUsuarios {
        val porNombre = locales.associateBy { normalizarNombre(it.nombreUsuario) }
        val insertar = mutableListOf<UsuarioEntidad>()
        val actualizar = mutableListOf<UsuarioEntidad>()

        for (remoto in lista.usuarios) {
            val local = porNombre[normalizarNombre(remoto.nombreUsuario)]
            if (local == null) {
                insertar += UsuarioEntidad(
                    nombreCompleto = remoto.nombreCompleto,
                    nombreUsuario = normalizarNombre(remoto.nombreUsuario),
                    clave = remoto.hashClave,
                    rol = remoto.rol,
                    estaActivo = remoto.activo,
                )
            } else {
                val actualizado = local.copy(
                    nombreCompleto = remoto.nombreCompleto,
                    clave = remoto.hashClave,
                    rol = remoto.rol,
                    estaActivo = remoto.activo,
                )
                if (actualizado != local) actualizar += actualizado
            }
        }

        val rechazados = lista.rechazados.map(::normalizarNombre).toSet()
        locales
            .filter { normalizarNombre(it.nombreUsuario) in rechazados && it.estaActivo }
            .forEach { actualizar += it.copy(estaActivo = false) }

        return CambiosUsuarios(insertar, actualizar)
    }
}
