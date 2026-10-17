package com.typdevstudio.typos_movil.utilidades.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.typdevstudio.typos_movil.datos.local.AppBaseDatos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Crear una copia y restaurarla en el mismo teléfono, con la base real de Room: cierra la instancia, guarda la copia
 * de emergencia, reemplaza base y fotos y reabre. Al final los datos quedan como al empezar.
 */
@RunWith(AndroidJUnit4::class)
class PruebasRestauracionSegura {

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private fun contarProductos(): Int =
        AppBaseDatos.obtenerBaseDatos(contexto).openHelper.readableDatabase
            .query("SELECT COUNT(*) FROM productos").use { it.moveToFirst(); it.getInt(0) }

    @Test
    fun crearYRestaurarEnElMismoTelefonoDejaLosDatosDeLaCopia() {
        val fotos = File(contexto.filesDir, FormatoBackup.CARPETA_FOTOS).apply { mkdirs() }
        val foto = File(fotos, "prueba_restauracion.jpg").apply { writeText("foto de prueba") }
        val productosAntes = contarProductos()

        val copia = GestorBackup.crearBackup(contexto, idOperacion = "00000000-0000-0000-0000-000000000001").getOrThrow()
        try {
            assertTrue(copia.archivo.name.endsWith(FormatoBackup.EXTENSION))
            assertEquals(productosAntes, copia.manifiesto!!.conteos.productos)
            assertTrue("fotos_productos/prueba_restauracion.jpg" in copia.manifiesto!!.archivos)

            // Cambios después de la copia: la restauración debe deshacerlos.
            AppBaseDatos.obtenerBaseDatos(contexto).openHelper.writableDatabase.execSQL(
                "INSERT INTO productos (nombre, precio_costo, precio_venta, exento_itbis, itbis_incluido, tasa_itbis, stock, " +
                    "controla_stock, tipo_item, categoria, esta_activo, creado_en, actualizado_en) " +
                    "VALUES ('Producto después de la copia', 0, 10, 0, 1, 18, 0, 0, 'Producto', 'General', 1, 0, 0)"
            )
            foto.delete()
            assertEquals(productosAntes + 1, contarProductos())

            val resultado = GestorBackup.restaurarBackup(contexto, copia, deOtroTelefono = false, idOperacion = "00000000-0000-0000-0000-000000000002")

            assertEquals(ResultadoRestauracion.Exito, resultado)
            // Room se reabre con la base restaurada (en la app real, aquí se reinicia).
            assertEquals(productosAntes, contarProductos())
            assertEquals("foto de prueba", foto.readText())
            assertFalse(File(contexto.filesDir, "restauracion_emergencia").exists())
        } finally {
            copia.archivo.delete()
            foto.delete()
        }
    }

    @Test
    fun siLaAppSeCierraAMitadDeRestaurarAlAbrirVuelvenLosDatos() {
        val productosAntes = contarProductos()
        val archivoBd = contexto.getDatabasePath(AppBaseDatos.NOMBRE_BD)
        val emergencia = File(contexto.filesDir, "restauracion_emergencia")

        // Estado en que queda el teléfono si se apaga justo después de poner la base nueva en su lugar.
        AppBaseDatos.cerrarInstancia()
        emergencia.mkdirs()
        File(emergencia, ".en_curso").writeText("00000000-0000-0000-0000-000000000004")
        listOf(archivoBd, File(archivoBd.path + "-wal"), File(archivoBd.path + "-shm"))
            .filter { it.exists() }
            .forEach { assertTrue(it.renameTo(File(emergencia, it.name))) }
        archivoBd.writeText("base a medio poner")

        val idOperacion = GestorBackup.recuperarRestauracionInterrumpida(contexto)

        assertEquals("00000000-0000-0000-0000-000000000004", idOperacion)
        assertFalse(emergencia.exists())
        assertEquals(productosAntes, contarProductos())
        // Sin restauración pendiente no hace nada.
        assertEquals(null, GestorBackup.recuperarRestauracionInterrumpida(contexto))
    }

    @Test
    fun unaCopiaDanadaNoTocaLosDatos() {
        val productosAntes = contarProductos()
        val danada = File(GestorBackup.obtenerDirectorioBackups(contexto), "TyPOS_BK_01012020_00-00-00.typosbk")
        danada.writeText("esto no es una copia")
        try {
            val backup = ArchivoBackupUi(
                nombreArchivo = danada.name, nombreVisual = danada.name, fechaCreacion = 0, tamanoBytes = danada.length(),
                archivo = danada, esAutomatico = false,
            )

            val resultado = GestorBackup.restaurarBackup(contexto, backup, deOtroTelefono = false, idOperacion = "00000000-0000-0000-0000-000000000003")

            val fallo = resultado as ResultadoRestauracion.Fallo
            assertEquals(CodigosBackup.ARCHIVO_NO_VALIDO, fallo.error.codigo)
            assertFalse(fallo.requiereReinicio)
            assertEquals(productosAntes, contarProductos())
        } finally {
            danada.delete()
        }
    }
}
