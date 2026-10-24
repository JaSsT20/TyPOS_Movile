package com.typdevstudio.typos_movil.utilidades.exportacion

import com.typdevstudio.typos_movil.datos.local.entidades.ProductoEntidad
import com.typdevstudio.typos_movil.utilidades.importacion.GestorPlantillaProductos
import com.typdevstudio.typos_movil.utilidades.importacion.LectorCsvProductos
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.util.Date
import java.util.TimeZone
import java.util.zip.ZipFile

class PruebasGestorExportacionProductos {

    private val carpeta: File = Files.createTempDirectory("exportacion").toFile()
    private val fotos = File(carpeta, "fotos_productos").apply { mkdirs() }

    @After
    fun limpiar() {
        carpeta.deleteRecursively()
    }

    private fun foto(nombre: String, contenido: String = "imagen-$nombre"): String =
        File(fotos, nombre).apply { writeText(contenido) }.absolutePath

    private fun producto(
        id: Long,
        nombre: String,
        codigo: String? = null,
        imagenes: List<String> = emptyList(),
        activo: Boolean = true,
        descripcion: String? = null,
    ) = ProductoEntidad(
        id = id,
        idUsuario = 99,
        codigoBarras = codigo,
        nombre = nombre,
        descripcion = descripcion,
        precioCosto = 25.5,
        precioVenta = 45.0,
        exentoItbis = false,
        itbisIncluido = true,
        tasaItbis = 18.0,
        stock = 12.0,
        controlaStock = true,
        tipoItem = "Producto",
        categoria = "Bebidas",
        estaActivo = activo,
        imagenesUri = imagenes.joinToString(",").ifEmpty { null },
        creadoEn = 1_760_000_000_000,
        actualizadoEn = 1_760_000_060_000,
    )

    private fun leerZip(archivo: File): Map<String, ByteArray> = ZipFile(archivo).use { zip ->
        zip.entries().toList().associate { it.name to zip.getInputStream(it).readBytes() }
    }

    private fun texto(bytes: ByteArray) = String(bytes, Charsets.UTF_8)

    // ------------------------------------------------------------------
    // Nombre y fotos
    // ------------------------------------------------------------------

    @Test
    fun `el nombre del archivo lleva fecha y hora`() {
        val fecha = Date(1_761_318_720_000) // 2025-10-24 15:12 UTC
        assertEquals(
            "TyPOS_Productos_2025-10-24_1512.zip",
            GestorExportacionProductos.nombreArchivo(fecha, TimeZone.getTimeZone("UTC")),
        )
    }

    @Test
    fun `los nombres de las fotos no chocan aunque los codigos se repitan o se parezcan`() {
        val productos = listOf(
            producto(1, "A", "7501031311309", listOf(foto("a1.jpg"), foto("a2.png"))),
            producto(2, "B", "7501031311309", listOf(foto("b1.jpg"))),
            producto(3, "C", "ab/cd", listOf(foto("c1.jpg"))),
            producto(4, "D", "ab:cd", listOf(foto("d1.JPG"))),
            producto(5, "E", null, listOf(foto("e1"))),
            producto(6, "F", "  ", listOf(foto("f1.webp"))),
        )
        val plan = GestorExportacionProductos.planificarFotos(productos)

        val rutas = plan.porProducto.values.flatten().map { it.rutaEnArchivo }
        assertEquals(rutas.size, rutas.map { it.lowercase() }.toSet().size)
        assertEquals(
            listOf("fotos/7501031311309_1.jpg", "fotos/7501031311309_2.png"),
            plan.porProducto[1]!!.map { it.rutaEnArchivo },
        )
        assertEquals(listOf("fotos/7501031311309-2_1.jpg"), plan.porProducto[2]!!.map { it.rutaEnArchivo })
        assertEquals(listOf("fotos/ab_cd_1.jpg"), plan.porProducto[3]!!.map { it.rutaEnArchivo })
        assertEquals(listOf("fotos/ab_cd-2_1.jpg"), plan.porProducto[4]!!.map { it.rutaEnArchivo })
        assertEquals(listOf("fotos/id5_1.jpg"), plan.porProducto[5]!!.map { it.rutaEnArchivo })
        assertEquals(listOf("fotos/id6_1.webp"), plan.porProducto[6]!!.map { it.rutaEnArchivo })
        assertTrue(plan.faltantes.isEmpty())
    }

    @Test
    fun `una foto que no existe se omite y se cuenta sin fallar`() {
        val faltante = File(fotos, "borrada.jpg").absolutePath
        val productos = listOf(producto(1, "Refresco", "750", listOf(faltante, foto("ok.jpg"))))
        val destino = File(carpeta, "salida/export.zip")

        val resultado = GestorExportacionProductos.exportar(productos, OpcionesExportacion(), destino, "1.2.3", "Colmado")

        assertEquals(1, resultado.cantidadFotos)
        assertEquals(listOf(faltante), resultado.fotosFaltantes)
        assertTrue(resultado.mensajeExito.contains("(EXP-004)"))
        val zip = leerZip(destino)
        assertArrayEquals("imagen-ok.jpg".toByteArray(), zip["fotos/750_1.jpg"])
        val json = JSONArray(texto(zip.getValue("productos.json")))
        assertEquals(listOf("fotos/750_1.jpg"), json.getJSONObject(0).getJSONArray("fotos").let { a ->
            (0 until a.length()).map { a.getString(it) }
        })
    }

    // ------------------------------------------------------------------
    // Contenido
    // ------------------------------------------------------------------

    @Test
    fun `el archivo trae manifiesto, json, csv y fotos con los tipos reales`() {
        val productos = listOf(
            producto(1, "Refresco Cola", "7501031311309", listOf(foto("r1.jpg"), foto("r2.jpg"))),
            producto(2, "Arroz", null, descripcion = "Grano largo"),
        )
        val destino = File(carpeta, "TyPOS_Productos.zip")
        val fecha = Date(1_761_318_720_000)

        val resultado = GestorExportacionProductos.exportar(
            productos, OpcionesExportacion(), destino, "1.2.3C", "Colmado Don José", fecha,
        )

        assertEquals("Listo. Se exportaron 2 productos y 2 fotos.", resultado.mensajeExito)
        assertFalse(File(carpeta, "TyPOS_Productos.zip.tmp").exists())
        val zip = leerZip(destino)
        assertEquals(
            setOf("manifiesto.json", "productos.json", "productos.csv", "fotos/7501031311309_1.jpg", "fotos/7501031311309_2.jpg"),
            zip.keys,
        )

        val manifiesto = JSONObject(texto(zip.getValue("manifiesto.json")))
        assertEquals("typos-productos", manifiesto.getString("formato"))
        assertEquals(1, manifiesto.getInt("version_formato"))
        assertEquals("2025-10-24T15:12:00Z", manifiesto.getString("generado_en"))
        assertEquals("1.2.3C", manifiesto.getString("version_app"))
        assertEquals("Colmado Don José", manifiesto.getString("negocio"))
        assertEquals(2, manifiesto.getInt("cantidad_productos"))
        assertEquals(2, manifiesto.getInt("cantidad_fotos"))

        val arreglo = JSONArray(texto(zip.getValue("productos.json")))
        // Orden por nombre: Arroz, Refresco Cola
        val arroz = arreglo.getJSONObject(0)
        val refresco = arreglo.getJSONObject(1)
        assertEquals(2L, arroz.getLong("id"))
        assertTrue(arroz.isNull("codigo_barras"))
        assertEquals("Grano largo", arroz.getString("descripcion"))
        assertEquals(0, arroz.getJSONArray("fotos").length())
        assertEquals("7501031311309", refresco.getString("codigo_barras"))
        assertTrue(refresco.isNull("descripcion"))
        assertEquals(25.5, refresco.getDouble("precio_costo"), 0.0)
        assertEquals(45.0, refresco.getDouble("precio_venta"), 0.0)
        assertTrue(refresco.get("exento_itbis") is Boolean)
        assertEquals(false, refresco.getBoolean("exento_itbis"))
        assertEquals(true, refresco.getBoolean("itbis_incluido"))
        assertEquals(18.0, refresco.getDouble("tasa_itbis"), 0.0)
        assertEquals(12.0, refresco.getDouble("stock"), 0.0)
        assertEquals(true, refresco.getBoolean("controla_stock"))
        assertEquals("Producto", refresco.getString("tipo_item"))
        assertEquals("Bebidas", refresco.getString("categoria"))
        assertEquals(true, refresco.getBoolean("esta_activo"))
        assertEquals("2025-10-09T08:53:20Z", refresco.getString("creado_en"))
        assertEquals("2025-10-09T08:54:20Z", refresco.getString("actualizado_en"))
        assertEquals("fotos/7501031311309_1.jpg", refresco.getJSONArray("fotos").getString(0))
        // Nada del teléfono: ni el usuario ni las rutas internas
        assertFalse(refresco.has("id_usuario"))
        assertFalse(refresco.has("imagenes_uri"))
        assertFalse(texto(zip.getValue("productos.json")).contains(fotos.absolutePath.replace("\\", "\\\\")))
        assertFalse(texto(zip.getValue("productos.json")).contains(fotos.absolutePath))

        val csv = zip.getValue("productos.csv")
        assertArrayEquals(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()), csv.copyOfRange(0, 3))
        val lineas = texto(csv.copyOfRange(3, csv.size)).split("\r\n")
        assertEquals(
            (GestorPlantillaProductos.CABECERAS_ESTANDAR + listOf("tipo_item", "esta_activo", "imagenes")).joinToString(","),
            lineas[0],
        )
        assertEquals(",Arroz,Grano largo,25.5,45,12,SI,Bebidas,NO,SI,18,Producto,SI,", lineas[1])
        assertEquals(
            "7501031311309,Refresco Cola,,25.5,45,12,SI,Bebidas,NO,SI,18,Producto,SI,7501031311309_1.jpg|7501031311309_2.jpg",
            lineas[2],
        )
        assertEquals("", lineas[3])
    }

    @Test
    fun `el csv escapa comillas, comas y saltos de linea`() {
        assertEquals("simple", GestorExportacionProductos.campoCsv("simple"))
        assertEquals("\"a,b\"", GestorExportacionProductos.campoCsv("a,b"))
        assertEquals("\"a;b\"", GestorExportacionProductos.campoCsv("a;b"))
        assertEquals("\"TV 32\"\"\"", GestorExportacionProductos.campoCsv("TV 32\""))
        assertEquals("\"linea 1\nlinea 2\"", GestorExportacionProductos.campoCsv("linea 1\r\nlinea 2"))
        assertEquals("\" espacio \"", GestorExportacionProductos.campoCsv(" espacio "))
        assertEquals("", GestorExportacionProductos.campoCsv(""))
    }

    @Test
    fun `sin fotos no se copia ninguna y el csv queda sin imagenes`() {
        val productos = listOf(producto(1, "Refresco", "750", listOf(foto("r1.jpg"))))
        val destino = File(carpeta, "sin_fotos.zip")

        val resultado = GestorExportacionProductos.exportar(
            productos, OpcionesExportacion(incluirFotos = false), destino, "1.0", null,
        )

        assertEquals(0, resultado.cantidadFotos)
        val zip = leerZip(destino)
        assertEquals(setOf("manifiesto.json", "productos.json", "productos.csv"), zip.keys)
        assertTrue(JSONObject(texto(zip.getValue("manifiesto.json"))).isNull("negocio"))
    }

    @Test
    fun `los inactivos se incluyen por defecto y se pueden dejar fuera`() {
        val productos = listOf(producto(1, "Activo"), producto(2, "Inactivo", activo = false))

        assertEquals(2, GestorExportacionProductos.filtrarProductos(productos, OpcionesExportacion()).size)
        assertEquals(
            listOf(1L),
            GestorExportacionProductos.filtrarProductos(productos, OpcionesExportacion(incluirInactivos = false)).map { it.id },
        )
    }

    @Test
    fun `sin productos falla con EXP-001 y no deja archivo`() {
        val destino = File(carpeta, "vacio.zip")
        try {
            GestorExportacionProductos.exportar(
                listOf(producto(1, "Inactivo", activo = false)),
                OpcionesExportacion(incluirInactivos = false), destino, "1.0", null,
            )
            fail("Debió fallar")
        } catch (e: ErrorExportacion) {
            assertEquals(CodigosExportacion.SIN_PRODUCTOS, e.codigo)
            assertTrue(e.mensajeUsuario.endsWith("(EXP-001)"))
        }
        assertFalse(destino.exists())
    }

    @Test
    fun `si no se puede escribir falla con EXP-003`() {
        // La "carpeta" de destino es un archivo: no se puede escribir adentro.
        val bloqueo = File(carpeta, "bloqueo").apply { writeText("x") }
        try {
            GestorExportacionProductos.exportar(
                listOf(producto(1, "A")), OpcionesExportacion(), File(bloqueo, "x.zip"), "1.0", null,
            )
            fail("Debió fallar")
        } catch (e: ErrorExportacion) {
            assertEquals(CodigosExportacion.ERROR_ESCRITURA, e.codigo)
        }
    }

    @Test
    fun `la falta de espacio se reconoce como EXP-002`() {
        assertTrue(GestorExportacionProductos.esFaltaDeEspacio(java.io.IOException("write failed: ENOSPC (No space left on device)")))
        assertFalse(GestorExportacionProductos.esFaltaDeEspacio(java.io.IOException("Permission denied")))
        assertEquals(
            "No se pudo crear el archivo de exportación. Verifique que tenga espacio disponible. (EXP-002)",
            CodigosExportacion.mensaje(CodigosExportacion.SIN_ESPACIO),
        )
    }

    // ------------------------------------------------------------------
    // Re-importación
    // ------------------------------------------------------------------

    @Test
    fun `el csv exportado se vuelve a importar igual`() {
        val productos = listOf(
            producto(1, "Refresco, Cola \"Grande\"", "7501031311309", descripcion = "Línea 1\nLínea 2; con punto y coma"),
            producto(2, "Servicio de Delivery", "SERV-001").copy(
                precioCosto = 0.0, precioVenta = 100.0, stock = 0.0, controlaStock = false, categoria = "Servicios",
                exentoItbis = true, itbisIncluido = false, tasaItbis = 0.0, tipoItem = "Servicio",
            ),
            producto(3, "Queso de hoja 1/2 lb", null).copy(precioCosto = 1250.75, precioVenta = 1499.99, stock = 2.5),
        )
        val ordenados = GestorExportacionProductos.filtrarProductos(productos, OpcionesExportacion())
        val csv = GestorExportacionProductos.generarCsv(ordenados, emptyMap())
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + csv.toByteArray(Charsets.UTF_8)

        val importados = LectorCsvProductos.leerDesdeInputStream(ByteArrayInputStream(bytes), emptyList())

        assertEquals(ordenados.size, importados.size)
        for ((original, leido) in ordenados.zip(importados)) {
            assertTrue("${original.nombre}: ${leido.errores}", leido.esValido)
            assertEquals(original.codigoBarras.orEmpty(), leido.codigoBarras)
            assertEquals(original.nombre, leido.nombre)
            assertEquals(original.descripcion.orEmpty(), leido.descripcion)
            assertEquals(original.precioCosto, leido.precioCosto, 0.0)
            assertEquals(original.precioVenta, leido.precioVenta, 0.0)
            assertEquals(original.stock, leido.stock, 0.0)
            assertEquals(original.controlaStock, leido.controlaStock)
            assertEquals(original.categoria, leido.categoria)
            assertEquals(original.exentoItbis, leido.exentoItbis)
            assertEquals(original.itbisIncluido, leido.itbisIncluido)
            assertEquals(original.tasaItbis, leido.tasaItbis, 0.0)
        }
    }

    @Test
    fun `el lector sigue aceptando una comilla suelta dentro del texto`() {
        val csv = "nombre,precio_venta\r\nTV 32\",15000\r\nRadio,500\r\n"
        val importados = LectorCsvProductos.leerDesdeInputStream(ByteArrayInputStream(csv.toByteArray()), emptyList())

        assertEquals(listOf("TV 32\"", "Radio"), importados.map { it.nombre })
        assertEquals(15000.0, importados[0].precioVenta, 0.0)
        assertNull(importados[1].idExistente)
    }
}
