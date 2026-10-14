package com.typdevstudio.typos_movil.utilidades.licencia

import android.util.Log
import com.typdevstudio.typos_movil.datos.local.GestorSesion
import com.typdevstudio.typos_movil.datos.local.daos.UsuarioDao
import com.typdevstudio.typos_movil.utilidades.licencia.nube.HashClave
import com.typdevstudio.typos_movil.utilidades.licencia.nube.ListaUsuariosRemota
import com.typdevstudio.typos_movil.utilidades.licencia.nube.RepositorioLicencia
import com.typdevstudio.typos_movil.utilidades.licencia.nube.ResultadoUsuarios
import com.typdevstudio.typos_movil.utilidades.licencia.nube.UsuarioRemoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Lo que dice la licencia sobre sus usuarios (para mostrar "3 de 5"). */
data class ResumenUsuariosLicencia(val maxUsuarios: Int, val usuariosActivos: Int)

/** Operaciones de usuarios contra la licencia; [GestorUsuarios] es la real (se abstrae para las pruebas). */
interface UsuariosLicencia {
    suspend fun sincronizar(): ResultadoUsuarios
    suspend fun crear(nombreUsuario: String, nombreCompleto: String, rol: String, clave: String): ResultadoUsuarios
    suspend fun actualizar(
        nombreUsuario: String,
        nombreCompleto: String? = null,
        rol: String? = null,
        claveNueva: String? = null,
        activo: Boolean? = null,
    ): ResultadoUsuarios
}

/**
 * Usuarios de la licencia: cualquier usuario puede iniciar sesión en cualquier teléfono de su licencia.
 * La licencia (API) es la fuente de verdad y cada teléfono guarda una copia en Room para iniciar sesión sin
 * internet. Crear o cambiar usuarios requiere internet; se sincroniza después de cada validación.
 */
object GestorUsuarios : UsuariosLicencia {

    private const val ETIQUETA_LOG = "GestorUsuarios"

    private lateinit var repositorio: RepositorioLicencia
    private lateinit var usuarioDao: UsuarioDao
    private val candado = Mutex()

    private val _resumen = MutableStateFlow<ResumenUsuariosLicencia?>(null)
    /** `null` hasta la primera sincronización con la licencia. */
    val resumen: StateFlow<ResumenUsuariosLicencia?> = _resumen.asStateFlow()

    fun inicializar(repositorio: RepositorioLicencia, usuarioDao: UsuarioDao) {
        this.repositorio = repositorio
        this.usuarioDao = usuarioDao
    }

    /**
     * Sube los usuarios del teléfono que la licencia todavía no conoce (migración del cliente actual o un
     * administrador creado antes de activar) y aplica la lista de la licencia en Room.
     */
    override suspend fun sincronizar(): ResultadoUsuarios = conCandado {
        val listado = repositorio.listarUsuarios()
        if (listado !is ResultadoUsuarios.Exito) return@conCandado listado

        val pendientes = withContext(Dispatchers.Default) {
            SincronizacionUsuarios.pendientesDeSubir(usuarioDao.obtenerTodos(), listado.lista.usuarios)
        }
        val final = if (pendientes.isEmpty()) listado else repositorio.importarUsuarios(pendientes)
        if (final is ResultadoUsuarios.Exito) aplicar(final.lista)
        final
    }

    /** Crea un usuario en la licencia (el servidor revisa el límite del plan) y lo copia al teléfono. */
    override suspend fun crear(nombreUsuario: String, nombreCompleto: String, rol: String, clave: String): ResultadoUsuarios {
        val hash = withContext(Dispatchers.Default) { HashClave.generar(clave) }
        return conCandado {
            val usuario = UsuarioRemoto(SincronizacionUsuarios.normalizarNombre(nombreUsuario), nombreCompleto.trim(), rol, hash, true)
            repositorio.crearUsuario(usuario).also { if (it is ResultadoUsuarios.Exito) aplicar(it.lista) }
        }
    }

    /** Cambia nombre, rol, contraseña o estado en la licencia y lo copia al teléfono. */
    override suspend fun actualizar(
        nombreUsuario: String,
        nombreCompleto: String?,
        rol: String?,
        claveNueva: String?,
        activo: Boolean?,
    ): ResultadoUsuarios {
        val hash = claveNueva?.let { withContext(Dispatchers.Default) { HashClave.generar(it) } }
        return conCandado {
            repositorio.actualizarUsuario(SincronizacionUsuarios.normalizarNombre(nombreUsuario), nombreCompleto, rol, hash, activo)
                .also { if (it is ResultadoUsuarios.Exito) aplicar(it.lista) }
        }
    }

    private suspend fun aplicar(lista: ListaUsuariosRemota) {
        val cambios = SincronizacionUsuarios.calcularCambios(usuarioDao.obtenerTodos(), lista)
        cambios.insertar.forEach { usuarioDao.insertar(it) }
        cambios.actualizar.forEach { usuarioDao.actualizar(it) }
        _resumen.value = ResumenUsuariosLicencia(lista.maxUsuarios, lista.usuariosActivos)

        // La sesión abierta refleja los cambios de nombre o rol que vengan de otro teléfono.
        GestorSesion.usuarioActivo.value?.let { actual ->
            usuarioDao.obtenerPorId(actual.id)?.let { GestorSesion.iniciarSesion(it) }
        }
        if (cambios.insertar.isNotEmpty() || cambios.actualizar.isNotEmpty()) {
            Log.i(ETIQUETA_LOG, "Usuarios sincronizados: ${cambios.insertar.size} nuevos, ${cambios.actualizar.size} actualizados")
        }
    }

    private suspend fun conCandado(operacion: suspend () -> ResultadoUsuarios): ResultadoUsuarios =
        withContext(Dispatchers.IO) {
            candado.withLock {
                try {
                    operacion()
                } catch (e: Exception) {
                    // Nunca debe cerrar la app (el capturador global mata el proceso).
                    Log.e(ETIQUETA_LOG, "Error inesperado con los usuarios", e)
                    ResultadoUsuarios.SinConexion("Ocurrió un error inesperado. Intenta de nuevo.")
                }
            }
        }
}
