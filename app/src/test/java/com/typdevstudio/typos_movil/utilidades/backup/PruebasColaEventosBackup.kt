package com.typdevstudio.typos_movil.utilidades.backup

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File
import java.nio.file.Files

class PruebasColaEventosBackup {

    private val carpeta: File = Files.createTempDirectory("cola_eventos").toFile()
    private val archivo = File(carpeta, "cola.json")

    @After
    fun limpiar() {
        carpeta.deleteRecursively()
    }

    private fun evento(n: Int, tipo: String = TiposEventoBackup.BACKUP_CREADO) = EventoBackup(
        tipo = tipo,
        idOperacion = "op-$n",
        ocurridoEn = "2026-10-17T23:30:0${n % 10}Z",
        deviceHash = "ab12cd34ef56",
        clave = if (n % 2 == 0) "K7QX-9M2D-PL4A" else null,
        codigo = if (tipo == TiposEventoBackup.RESTAURACION_FALLIDA) CodigosBackup.ESPACIO_INSUFICIENTE else null,
        paso = "extraer",
        detalleTecnico = "x".repeat(800),
        versionApp = "1.3.0",
    )

    @Test
    fun guardaEnOrdenYSobreviveAUnaInstanciaNueva() {
        val cola = ColaEventosBackup(archivo)
        (1..3).forEach { cola.agregar(evento(it)) }

        val otra = ColaEventosBackup(archivo)
        assertEquals(listOf("op-1", "op-2", "op-3"), otra.primeros(10).map { it.idOperacion })
        assertEquals("K7QX-9M2D-PL4A", otra.primeros(2)[1].clave)
    }

    @Test
    fun quitarLosEnviadosDejaElResto() {
        val cola = ColaEventosBackup(archivo)
        (1..5).forEach { cola.agregar(evento(it)) }

        cola.quitar(cola.primeros(3))

        assertEquals(listOf("op-4", "op-5"), cola.primeros(50).map { it.idOperacion })
        cola.quitar(cola.primeros(2))
        assertEquals(0, cola.tamano())
        assertFalse(archivo.exists())
    }

    @Test
    fun siSeDescartanViejosMientrasSeEnviaNoSePierdenLosNuevos() {
        val cola = ColaEventosBackup(archivo, maximo = 3)
        (1..3).forEach { cola.agregar(evento(it)) }
        val lote = cola.primeros(2) // op-1, op-2 en camino

        (4..5).forEach { cola.agregar(evento(it)) } // se descartan op-1 y op-2 por exceso
        cola.quitar(lote)

        assertEquals(listOf("op-3", "op-4", "op-5"), cola.primeros(10).map { it.idOperacion })
    }

    @Test
    fun conDemasiadosSeDescartanLosMasViejos() {
        val cola = ColaEventosBackup(archivo, maximo = 3)
        (1..5).forEach { cola.agregar(evento(it)) }

        assertEquals(listOf("op-3", "op-4", "op-5"), cola.primeros(10).map { it.idOperacion })
    }

    @Test
    fun unArchivoDanadoNoTrabaLaCola() {
        archivo.writeText("{esto no es json")
        val cola = ColaEventosBackup(archivo)

        cola.agregar(evento(1))

        assertEquals(1, cola.tamano())
    }

    @Test
    fun elJsonLlevaLosCamposDelContratoYRecortaElDetalle() {
        val json = evento(2, TiposEventoBackup.RESTAURACION_FALLIDA).aJson()

        assertEquals("restauracion_fallida", json.getString("tipo"))
        assertEquals("BK_ESPACIO_INSUFICIENTE", json.getString("codigo"))
        assertEquals("extraer", json.getString("paso"))
        assertEquals(EventoBackup.MAXIMO_DETALLE, json.getString("detalle_tecnico").length)
        assertEquals(
            setOf("tipo", "id_operacion", "ocurrido_en", "device_hash", "clave", "codigo", "paso", "detalle_tecnico", "version_app"),
            json.keys().asSequence().toSet(),
        )
    }
}
