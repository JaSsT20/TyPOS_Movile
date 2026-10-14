package com.typdevstudio.typos_movil.datos.repositorio

import com.typdevstudio.typos_movil.datos.local.daos.UsuarioDao
import com.typdevstudio.typos_movil.datos.local.entidades.UsuarioEntidad
import com.typdevstudio.typos_movil.utilidades.licencia.UsuariosLicencia
import com.typdevstudio.typos_movil.utilidades.licencia.nube.HashClave
import com.typdevstudio.typos_movil.utilidades.licencia.nube.ListaUsuariosRemota
import com.typdevstudio.typos_movil.utilidades.licencia.nube.ResultadoUsuarios
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PruebasUsuarioRepositorio {

    private class DaoFalso : UsuarioDao {
        val filas = MutableStateFlow<List<UsuarioEntidad>>(emptyList())
        private var siguienteId = 1L

        override suspend fun obtenerPorNombreUsuario(nombreUsuario: String) =
            filas.value.firstOrNull { it.nombreUsuario == nombreUsuario && it.estaActivo }
        override suspend fun buscarPorNombreUsuarioInsensible(nombreUsuario: String) =
            filas.value.firstOrNull { it.nombreUsuario.equals(nombreUsuario, ignoreCase = true) }
        override suspend fun obtenerPorId(id: Long) = filas.value.firstOrNull { it.id == id }
        override fun obtenerTodosActivos(): Flow<List<UsuarioEntidad>> = filas.map { l -> l.filter { it.estaActivo } }
        override suspend fun contarUsuarios() = filas.value.size
        override suspend fun obtenerTodos() = filas.value
        override fun observarTodos(): Flow<List<UsuarioEntidad>> = filas
        override suspend fun contarUsuariosActivos() = filas.value.count { it.estaActivo }
        override suspend fun contarAdministradoresActivos() = filas.value.count { it.estaActivo && it.rol == "ADMINISTRADOR" }
        override suspend fun insertar(usuario: UsuarioEntidad): Long {
            val id = siguienteId++
            filas.value = filas.value + usuario.copy(id = id)
            return id
        }
        override suspend fun actualizar(usuario: UsuarioEntidad) {
            filas.value = filas.value.map { if (it.id == usuario.id) usuario else it }
        }
        override suspend fun desactivarUsuario(id: Long) {
            filas.value = filas.value.map { if (it.id == id) it.copy(estaActivo = false) else it }
        }
    }

    /** Simula la licencia: responde siempre lo mismo y anota las llamadas. */
    private class LicenciaFalsa(var respuesta: ResultadoUsuarios = ResultadoUsuarios.SinLicencia) : UsuariosLicencia {
        val llamadas = mutableListOf<String>()
        var claveRecibida: String? = null
        override suspend fun sincronizar(): ResultadoUsuarios { llamadas += "sincronizar"; return respuesta }
        override suspend fun crear(nombreUsuario: String, nombreCompleto: String, rol: String, clave: String): ResultadoUsuarios {
            llamadas += "crear"; claveRecibida = clave; return respuesta
        }
        override suspend fun actualizar(nombreUsuario: String, nombreCompleto: String?, rol: String?, claveNueva: String?, activo: Boolean?): ResultadoUsuarios {
            llamadas += "actualizar"; claveRecibida = claveNueva; return respuesta
        }
    }

    private val dao = DaoFalso()
    private val licencia = LicenciaFalsa()
    private val repo = UsuarioRepositorio(dao, licencia)

    private fun usuario(clave: String, activo: Boolean = true, nombre: String = "ana") =
        runBlocking { dao.insertar(UsuarioEntidad(nombreCompleto = "Ana", nombreUsuario = nombre, clave = clave, estaActivo = activo)) }

    @Test
    fun `clave local acepta hash y texto plano antiguo`() {
        val hash = HashClave.generar("1234", iteraciones = 1000)
        assertTrue(ClaveLocal.coincide("1234", hash))
        assertFalse(ClaveLocal.coincide("4321", hash))
        assertTrue(ClaveLocal.coincide("1234", "1234"))
        assertFalse(ClaveLocal.coincide("", ""))
    }

    @Test
    fun `login con texto plano convierte la clave en hash`() = runBlocking {
        val id = usuario("1234")
        val resultado = repo.autenticar(" Ana ", " 1234 ")
        assertTrue(resultado is ResultadoAutenticacion.Exito)
        val guardada = dao.obtenerPorId(id)!!.clave
        assertTrue(HashClave.esHash(guardada))
        assertTrue(HashClave.verificar("1234", guardada))
        // Con la clave ya convertida se sigue entrando.
        assertTrue(repo.autenticar("ana", "1234") is ResultadoAutenticacion.Exito)
    }

    @Test
    fun `login con hash y clave incorrecta o usuario inactivo`() = runBlocking {
        usuario(HashClave.generar("1234", iteraciones = 1000))
        usuario("9999", activo = false, nombre = "beto")
        assertTrue(repo.autenticar("ana", "1234") is ResultadoAutenticacion.Exito)
        assertEquals("La contraseña es incorrecta", (repo.autenticar("ana", "0000") as ResultadoAutenticacion.Error).mensaje)
        assertEquals("El usuario se encuentra inactivo", (repo.autenticar("beto", "9999") as ResultadoAutenticacion.Error).mensaje)
        // Un inactivo con clave antigua no se convierte.
        assertEquals("9999", dao.buscarPorNombreUsuarioInsensible("beto")!!.clave)
    }

    @Test
    fun `sin licencia nueva el usuario se crea en el telefono con hash`() = runBlocking {
        val resultado = repo.registrarNuevoUsuario("Caja1", "Caja", "1234", "cajero")
        assertEquals(ResultadoRegistroUsuario.Exito, resultado)
        val creado = dao.buscarPorNombreUsuarioInsensible("caja1")!!
        assertEquals("caja1", creado.nombreUsuario)
        assertEquals("CAJERO", creado.rol)
        assertTrue(HashClave.verificar("1234", creado.clave))
        assertEquals(listOf("crear"), licencia.llamadas)
    }

    @Test
    fun `con licencia nueva crear depende del servidor`() = runBlocking {
        licencia.respuesta = ResultadoUsuarios.Rechazada("LIMITE_USUARIOS", "Se alcanzó el máximo de usuarios.")
        assertEquals(ResultadoRegistroUsuario.Error("Se alcanzó el máximo de usuarios."), repo.registrarNuevoUsuario("caja1", "Caja", "1234", "CAJERO"))

        licencia.respuesta = ResultadoUsuarios.SinConexion("Sin conexión.")
        val sinRed = repo.registrarNuevoUsuario("caja1", "Caja", "1234", "CAJERO") as ResultadoRegistroUsuario.Error
        assertTrue(sinRed.mensaje.startsWith("Para crear usuarios se necesita internet"))

        // Nada se guardó en el teléfono: lo copia la licencia cuando acepta.
        assertEquals(0, dao.contarUsuarios())
    }

    @Test
    fun `cambiar contrasena sin licencia nueva guarda hash y exige la actual`() = runBlocking {
        val id = usuario("1234")
        val malo = repo.actualizarPerfil(id, "Ana", null, claveActual = "0000", nuevaClave = "5678")
        assertEquals("La contraseña actual es incorrecta", (malo as ResultadoActualizacionPerfil.Error).mensaje)

        val bueno = repo.actualizarPerfil(id, "Ana María", "foto://1", claveActual = "1234", nuevaClave = "5678")
        assertTrue(bueno is ResultadoActualizacionPerfil.Exito)
        val guardado = dao.obtenerPorId(id)!!
        assertEquals("Ana María", guardado.nombreCompleto)
        assertEquals("foto://1", guardado.fotoUri)
        assertTrue(HashClave.verificar("5678", guardado.clave))
        assertEquals("5678", licencia.claveRecibida)
    }

    @Test
    fun `con licencia nueva sin internet no se cambia la contrasena pero la foto si`() = runBlocking {
        val id = usuario("1234")
        licencia.respuesta = ResultadoUsuarios.SinConexion("Sin conexión.")
        val sinRed = repo.actualizarPerfil(id, "Ana", null, claveActual = "1234", nuevaClave = "5678")
        assertTrue(sinRed is ResultadoActualizacionPerfil.Error)
        assertEquals("1234", dao.obtenerPorId(id)!!.clave)

        // Solo la foto: no hace falta la licencia.
        licencia.llamadas.clear()
        assertTrue(repo.actualizarPerfil(id, "Ana", "foto://2") is ResultadoActualizacionPerfil.Exito)
        assertEquals("foto://2", dao.obtenerPorId(id)!!.fotoUri)
        assertTrue(licencia.llamadas.isEmpty())
    }

    @Test
    fun `no se puede quitar el ultimo administrador activo`() = runBlocking {
        val admin = runBlocking { dao.insertar(UsuarioEntidad(nombreCompleto = "Jefe", nombreUsuario = "jefe", clave = "1", rol = "ADMINISTRADOR")) }
        usuario("1234")
        val sinAdmin = repo.administrarUsuario(admin, rol = "CAJERO") as ResultadoActualizacionPerfil.Error
        assertTrue(sinAdmin.mensaje.startsWith("Debe quedar al menos un administrador"))
        assertTrue(repo.administrarUsuario(admin, activo = false) is ResultadoActualizacionPerfil.Error)
        assertTrue(licencia.llamadas.isEmpty())

        // Con otro administrador activo sí se puede.
        val ana = dao.buscarPorNombreUsuarioInsensible("ana")!!.id
        assertTrue(repo.administrarUsuario(ana, rol = "administrador") is ResultadoActualizacionPerfil.Exito)
        assertTrue(repo.administrarUsuario(admin, activo = false) is ResultadoActualizacionPerfil.Exito)
        assertFalse(dao.obtenerPorId(admin)!!.estaActivo)
        assertEquals("ADMINISTRADOR", dao.obtenerPorId(ana)!!.rol)
    }

    @Test
    fun `restablecer contrasena sin licencia nueva guarda hash`() = runBlocking {
        val id = usuario("1234")
        assertTrue(repo.administrarUsuario(id, claveNueva = "12") is ResultadoActualizacionPerfil.Error)
        assertTrue(repo.administrarUsuario(id, claveNueva = " 9999 ") is ResultadoActualizacionPerfil.Exito)
        assertTrue(HashClave.verificar("9999", dao.obtenerPorId(id)!!.clave))
        assertEquals("9999", licencia.claveRecibida)
    }

    @Test
    fun `usuario que la licencia no conoce se sincroniza y se reintenta`() = runBlocking {
        val id = usuario("1234")
        val exito = ResultadoUsuarios.Exito(ListaUsuariosRemota(maxUsuarios = 5, usuariosActivos = 1, usuarios = emptyList(), rechazados = emptyList()))
        val licenciaReintento = object : UsuariosLicencia {
            val llamadas = mutableListOf<String>()
            override suspend fun sincronizar(): ResultadoUsuarios { llamadas += "sincronizar"; return exito }
            override suspend fun crear(nombreUsuario: String, nombreCompleto: String, rol: String, clave: String) = exito
            override suspend fun actualizar(nombreUsuario: String, nombreCompleto: String?, rol: String?, claveNueva: String?, activo: Boolean?): ResultadoUsuarios {
                llamadas += "actualizar"
                return if (llamadas.count { it == "actualizar" } == 1) ResultadoUsuarios.Rechazada("USUARIO_NO_ENCONTRADO", "No existe.") else exito
            }
        }
        val resultado = UsuarioRepositorio(dao, licenciaReintento).actualizarPerfil(id, "Ana B", null)
        assertTrue(resultado is ResultadoActualizacionPerfil.Exito)
        assertEquals(listOf("actualizar", "sincronizar", "actualizar"), licenciaReintento.llamadas)
    }
}
