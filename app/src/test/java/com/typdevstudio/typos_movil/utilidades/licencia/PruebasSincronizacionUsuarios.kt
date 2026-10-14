package com.typdevstudio.typos_movil.utilidades.licencia

import com.typdevstudio.typos_movil.datos.local.entidades.UsuarioEntidad
import com.typdevstudio.typos_movil.utilidades.licencia.nube.ListaUsuariosRemota
import com.typdevstudio.typos_movil.utilidades.licencia.nube.UsuarioRemoto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PruebasSincronizacionUsuarios {

    private val hashA = "pbkdf2_sha256\$1000\$c2Fs\$aGFzaA"
    private val hashB = "pbkdf2_sha256\$1000\$c2FsMg\$aGFzaDI"

    private fun local(nombre: String, clave: String = hashA, rol: String = "CAJERO", activo: Boolean = true, id: Long = 0) =
        UsuarioEntidad(id = id, nombreCompleto = nombre.replaceFirstChar { it.uppercase() }, nombreUsuario = nombre, clave = clave, rol = rol, estaActivo = activo)

    private fun remoto(nombre: String, hash: String = hashA, rol: String = "CAJERO", activo: Boolean = true) =
        UsuarioRemoto(nombre, nombre.replaceFirstChar { it.uppercase() }, rol, hash, activo)

    private fun lista(vararg usuarios: UsuarioRemoto, rechazados: List<String> = emptyList()) =
        ListaUsuariosRemota(maxUsuarios = 5, usuariosActivos = usuarios.count { it.activo }, usuarios = usuarios.toList(), rechazados = rechazados)

    // ---------- Nombres ----------

    @Test
    fun `normaliza y valida nombres como la API`() {
        assertEquals("ana.lopez", SincronizacionUsuarios.normalizarNombre("  Ana.Lopez "))
        assertTrue(SincronizacionUsuarios.nombreValido("José_1"))
        assertTrue(SincronizacionUsuarios.nombreValido("caja@tienda-2"))
        assertFalse(SincronizacionUsuarios.nombreValido("con espacio"))
        assertFalse(SincronizacionUsuarios.nombreValido(""))
        assertFalse(SincronizacionUsuarios.nombreValido("a".repeat(51)))
    }

    // ---------- Subir los usuarios del teléfono ----------

    @Test
    fun `sube solo los locales que la licencia no conoce`() {
        val pendientes = SincronizacionUsuarios.pendientesDeSubir(
            locales = listOf(local("ana"), local("Beto")),
            remotos = listOf(remoto("ana")),
        )
        assertEquals(listOf("beto"), pendientes.map { it.nombreUsuario })
    }

    @Test
    fun `las claves en texto plano se suben como hash y los hash se respetan`() {
        val pendientes = SincronizacionUsuarios.pendientesDeSubir(
            locales = listOf(local("ana", clave = "1234"), local("beto", clave = hashB)),
            remotos = emptyList(),
            generarHash = { "hash-de-$it" },
        ).associateBy { it.nombreUsuario }
        assertEquals("hash-de-1234", pendientes["ana"]!!.hashClave)
        assertEquals(hashB, pendientes["beto"]!!.hashClave)
    }

    @Test
    fun `al subir se normalizan rol y estado y se omiten nombres invalidos`() {
        val pendientes = SincronizacionUsuarios.pendientesDeSubir(
            locales = listOf(
                local("jefe", rol = "administrador"),
                local("caja", rol = "OTRO", activo = false),
                local("mal nombre"),
            ),
            remotos = emptyList(),
        ).associateBy { it.nombreUsuario }
        assertEquals(setOf("jefe", "caja"), pendientes.keys)
        assertEquals("ADMINISTRADOR", pendientes["jefe"]!!.rol)
        assertEquals("CAJERO", pendientes["caja"]!!.rol)
        assertFalse(pendientes["caja"]!!.activo)
    }

    // ---------- Aplicar la lista de la licencia ----------

    @Test
    fun `inserta los usuarios nuevos de la licencia`() {
        val cambios = SincronizacionUsuarios.calcularCambios(emptyList(), lista(remoto("ana", rol = "ADMINISTRADOR")))
        assertEquals(1, cambios.insertar.size)
        with(cambios.insertar.single()) {
            assertEquals("ana", nombreUsuario)
            assertEquals(hashA, clave)
            assertEquals("ADMINISTRADOR", rol)
        }
        assertTrue(cambios.actualizar.isEmpty())
    }

    @Test
    fun `manda el servidor en nombre clave rol y estado`() {
        val ana = local("ana", clave = "1234", id = 7)
        val cambios = SincronizacionUsuarios.calcularCambios(
            listOf(ana),
            lista(UsuarioRemoto("ana", "Ana María", "ADMINISTRADOR", hashB, false)),
        )
        val actualizado = cambios.actualizar.single()
        assertEquals(7, actualizado.id)
        assertEquals("Ana María", actualizado.nombreCompleto)
        assertEquals(hashB, actualizado.clave)
        assertEquals("ADMINISTRADOR", actualizado.rol)
        assertFalse(actualizado.estaActivo)
        assertTrue(cambios.insertar.isEmpty())
    }

    @Test
    fun `sin diferencias no hay cambios y se conserva la foto local`() {
        val ana = local("ana", id = 1).copy(fotoUri = "foto://ana")
        val sinCambios = SincronizacionUsuarios.calcularCambios(listOf(ana), lista(remoto("ana")))
        assertTrue(sinCambios.insertar.isEmpty() && sinCambios.actualizar.isEmpty())

        val conCambio = SincronizacionUsuarios.calcularCambios(listOf(ana), lista(remoto("ana", rol = "ADMINISTRADOR")))
        assertEquals("foto://ana", conCambio.actualizar.single().fotoUri)
    }

    @Test
    fun `coincide sin importar mayusculas del nombre local antiguo`() {
        val cambios = SincronizacionUsuarios.calcularCambios(listOf(local("Ana", id = 3)), lista(remoto("ana", hash = hashB)))
        assertTrue(cambios.insertar.isEmpty())
        assertEquals(3, cambios.actualizar.single().id)
    }

    @Test
    fun `los rechazados por el limite quedan inactivos y los demas locales no se tocan`() {
        val cambios = SincronizacionUsuarios.calcularCambios(
            locales = listOf(local("ana", id = 1), local("beto", id = 2), local("mal nombre", id = 3)),
            lista = lista(remoto("ana"), rechazados = listOf("Beto")),
        )
        assertEquals(listOf(2L), cambios.actualizar.map { it.id })
        assertFalse(cambios.actualizar.single().estaActivo)
    }
}
