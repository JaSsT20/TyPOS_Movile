package com.typdevstudio.typos_movil.utilidades.backup

import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class PruebasFormatoBackup {

    private val carpeta: File = Files.createTempDirectory("formato_backup").toFile()

    @After
    fun limpiar() {
        carpeta.deleteRecursively()
    }

    private fun archivo(nombre: String, contenido: String) = File(carpeta, nombre).apply {
        parentFile?.mkdirs()
        writeText(contenido)
    }

    private fun manifiestoBase(versionBd: Int = 12) = ManifiestoBackup(
        versionFormato = 1,
        versionBd = versionBd,
        versionApp = "1.3.0",
        creadoEn = "2026-10-17T23:30:00Z",
        automatico = false,
        deviceHashOrigen = "ab12cd34ef56",
        nombreDispositivoOrigen = "Samsung A54 - Caja 1",
        tokenOrigen = "PAYLOAD.FIRMA",
        conteos = ConteosBackup(productos = 120, ventas = 850, usuarios = 3),
        archivos = emptyMap(),
    )

    private fun crearCopia(): File {
        val bd = archivo("origen/typos_movil_bd.db", "SQLite format 3 contenido de prueba")
        val foto1 = archivo("origen/fotos/prod_1.jpg", "foto uno")
        val foto2 = archivo("origen/fotos/prod_2.jpg", "foto dos")
        val raro = archivo("origen/fotos/nombre con espacios.jpg", "no entra")
        val destino = File(carpeta, "TyPOS_BK_17102026_19-30-00.typosbk")
        FormatoBackup.escribir(destino, bd, listOf(foto1, foto2, raro), manifiestoBase())
        return destino
    }

    private fun esperarCodigo(codigo: String, bloque: () -> Unit) {
        try {
            bloque()
            fail("Se esperaba $codigo")
        } catch (e: ErrorBackup) {
            assertEquals(codigo, e.codigo)
        }
    }

    @Test
    fun escribeElZipConManifiestoPrimeroBaseYFotos() {
        val copia = crearCopia()

        ZipFile(copia).use { zip ->
            val nombres = zip.entries().asSequence().map { it.name }.toList()
            assertEquals(
                listOf("manifiesto.json", "typos_movil_bd.db", "fotos_productos/prod_1.jpg", "fotos_productos/prod_2.jpg"),
                nombres,
            )
        }
        val manifiesto = FormatoBackup.verificar(copia)
        assertEquals(3, manifiesto.archivos.size)
        assertEquals("Samsung A54 - Caja 1", manifiesto.nombreDispositivoOrigen)
        assertEquals("PAYLOAD.FIRMA", manifiesto.tokenOrigen)
        assertEquals(ConteosBackup(120, 850, 3), manifiesto.conteos)
        assertEquals(64, manifiesto.archivos.getValue("typos_movil_bd.db").length)
    }

    @Test
    fun extraeYComprueba() {
        val copia = crearCopia()
        val destino = File(carpeta, "extraida")

        FormatoBackup.extraer(copia, destino)

        assertEquals("SQLite format 3 contenido de prueba", File(destino, "typos_movil_bd.db").readText())
        assertEquals("foto dos", File(destino, "fotos_productos/prod_2.jpg").readText())
    }

    @Test
    fun unArchivoModificadoEsArchivoDanado() {
        val copia = crearCopia()
        val alterada = File(carpeta, "alterada.typosbk")
        // Mismo manifiesto, base distinta: el SHA-256 no coincide.
        ZipFile(copia).use { zip ->
            ZipOutputStream(FileOutputStream(alterada)).use { salida ->
                zip.entries().asSequence().forEach { entrada ->
                    salida.putNextEntry(ZipEntry(entrada.name))
                    if (entrada.name == "typos_movil_bd.db") salida.write("otra base".toByteArray())
                    else zip.getInputStream(entrada).use { it.copyTo(salida) }
                    salida.closeEntry()
                }
            }
        }

        esperarCodigo(CodigosBackup.ARCHIVO_DANADO) { FormatoBackup.verificar(alterada) }
        esperarCodigo(CodigosBackup.ARCHIVO_DANADO) { FormatoBackup.extraer(alterada, File(carpeta, "x")) }
    }

    @Test
    fun unArchivoQueNoEsZipOSinManifiestoNoEsValido() {
        esperarCodigo(CodigosBackup.ARCHIVO_NO_VALIDO) { FormatoBackup.leerManifiesto(archivo("foto.typosbk", "no soy un zip")) }

        val sinManifiesto = File(carpeta, "sin.typosbk")
        ZipOutputStream(FileOutputStream(sinManifiesto)).use { it.putNextEntry(ZipEntry("otra.txt")); it.write(1); it.closeEntry() }
        esperarCodigo(CodigosBackup.ARCHIVO_NO_VALIDO) { FormatoBackup.leerManifiesto(sinManifiesto) }
    }

    @Test
    fun unManifiestoConRutasFueraDeLaCarpetaNoEsValido() {
        val malicioso = File(carpeta, "malicioso.typosbk")
        val manifiesto = manifiestoBase().copy(archivos = mapOf("typos_movil_bd.db" to "00", "../../shared_prefs/x.xml" to "00"))
        ZipOutputStream(FileOutputStream(malicioso)).use {
            it.putNextEntry(ZipEntry("manifiesto.json"))
            it.write(manifiesto.aJson().toByteArray())
            it.closeEntry()
        }

        esperarCodigo(CodigosBackup.ARCHIVO_NO_VALIDO) { FormatoBackup.extraer(malicioso, File(carpeta, "x")) }
        assertFalse(File(carpeta, "shared_prefs").exists())
    }

    @Test
    fun sinEspacioSuficienteNoExtraeNada() {
        val copia = crearCopia()
        val destino = File(carpeta, "sin_espacio")

        esperarCodigo(CodigosBackup.ESPACIO_INSUFICIENTE) { FormatoBackup.extraer(copia, destino) { 1024L } }
        assertFalse(File(destino, "typos_movil_bd.db").exists())
    }

    @Test
    fun unFormatoMasNuevoPideActualizar() {
        val copia = File(carpeta, "futura.typosbk")
        val bd = archivo("bd.db", "x")
        FormatoBackup.escribir(copia, bd, emptyList(), manifiestoBase().copy(versionFormato = 2))

        esperarCodigo(CodigosBackup.VERSION_NUEVA) { FormatoBackup.leerManifiesto(copia) }
    }

    @Test
    fun manifiestoIdaYVuelta() {
        val original = manifiestoBase().copy(archivos = mapOf("typos_movil_bd.db" to "ab", "fotos_productos/a.jpg" to "cd"))
        assertEquals(original, ManifiestoBackup.desdeJson(original.aJson()))

        val sinToken = original.copy(tokenOrigen = null, nombreDispositivoOrigen = null)
        val leido = ManifiestoBackup.desdeJson(sinToken.aJson())
        assertNull(leido.tokenOrigen)
        assertNull(leido.nombreDispositivoOrigen)
    }

    @Test
    fun reescribeLasRutasDeLasFotosConLaCarpetaDeEsteTelefono() {
        val carpetaFotos = "/data/user/0/com.typdevstudio.typos_movil/files/fotos_productos"
        val valor = "/data/user/0/com.otro/files/fotos_productos/prod_1.jpg, /data/user/0/com.otro/files/fotos_productos/prod_2.jpg"

        assertEquals("$carpetaFotos/prod_1.jpg,$carpetaFotos/prod_2.jpg", FormatoBackup.reescribirRutasImagenes(valor, carpetaFotos))
        // Lo que no está en fotos_productos queda igual.
        assertEquals("content://algo/imagen", FormatoBackup.reescribirRutasImagenes("content://algo/imagen", carpetaFotos))
        assertNull(FormatoBackup.reescribirRutasImagenes(null, carpetaFotos))
    }

    @Test
    fun codigoDeSoporteCorto() {
        assertEquals("BK-4F2A", CodigosBackup.codigoSoporte("4f2a1c9e-1111-2222-3333-444455556666"))
        assertEquals("TR-4F2A", CodigosBackup.codigoSoporte("4f2a1c9e-1111", "TR"))
        assertTrue(CodigosBackup.mensaje(CodigosBackup.SIN_INTERNET).startsWith("Conéctese"))
    }

    @Test
    fun sha256DelArchivo() {
        val f = archivo("abc.txt", "abc")
        assertArrayEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad".toByteArray(),
            FormatoBackup.sha256(f).toByteArray(),
        )
    }
}
