package com.typdevstudio.typos_movil.utilidades.exportacion

import com.typdevstudio.typos_movil.datos.local.entidades.ProductoEntidad
import com.typdevstudio.typos_movil.utilidades.importacion.GestorPlantillaProductos
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Arma el archivo de exportación de productos (un .zip) con:
 * - `manifiesto.json`: qué es el archivo, cuándo y con qué versión se hizo, y cuántos productos y fotos trae.
 * - `productos.json`: todos los campos de cada producto con sus tipos reales y la lista de sus fotos.
 * - `productos.csv`: lo mismo para Excel, con las columnas de la plantilla de importación (se puede re-importar).
 * - `fotos/`: una copia de cada foto que todavía está en el teléfono.
 *
 * No depende de Android (solo archivos), así se prueba con pruebas unitarias.
 */
object GestorExportacionProductos {

    const val FORMATO = "typos-productos"
    const val VERSION_FORMATO = 1
    const val ENTRADA_MANIFIESTO = "manifiesto.json"
    const val ENTRADA_PRODUCTOS_JSON = "productos.json"
    const val ENTRADA_PRODUCTOS_CSV = "productos.csv"
    const val CARPETA_FOTOS = "fotos"

    val CABECERAS_CSV = GestorPlantillaProductos.CABECERAS_ESTANDAR + listOf("tipo_item", "esta_activo", "imagenes")

    /** Espacio de sobra que se pide además de lo que ocupan las fotos. */
    private const val MARGEN_ESPACIO_BYTES = 5L * 1024 * 1024

    private val BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

    fun nombreArchivo(fecha: Date = Date(), zona: TimeZone = TimeZone.getDefault()): String =
        "TyPOS_Productos_${SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).apply { timeZone = zona }.format(fecha)}.zip"

    fun fechaIso(milis: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(milis))

    /** Productos que entran según las opciones, en un orden estable (por nombre y luego id). */
    fun filtrarProductos(productos: List<ProductoEntidad>, opciones: OpcionesExportacion): List<ProductoEntidad> =
        productos
            .filter { opciones.incluirInactivos || it.estaActivo }
            .sortedWith(compareBy<ProductoEntidad>({ it.nombre.lowercase(Locale.ROOT) }, { it.id }))

    // ------------------------------------------------------------------
    // Fotos
    // ------------------------------------------------------------------

    data class PlanFotos(
        /** Fotos de cada producto (por id), en el mismo orden que `listaImagenes`. */
        val porProducto: Map<Long, List<FotoExportada>>,
        /** Rutas de fotos que no están en el teléfono. */
        val faltantes: List<String>,
    ) {
        val total: Int get() = porProducto.values.sumOf { it.size }
    }

    /**
     * Decide qué fotos se copian y con qué nombre: `<código de barras saneado o "id<id>">_<n>.<ext>`. Si dos productos
     * quedan con el mismo nombre (códigos repetidos o que se parecen al sanearlos) se agrega `-2`, `-3`... al final de
     * la base. Las fotos que ya no existen se cuentan como faltantes y se omiten.
     */
    fun planificarFotos(
        productos: List<ProductoEntidad>,
        existe: (File) -> Boolean = { it.isFile },
    ): PlanFotos {
        val usados = mutableSetOf<String>()
        val porProducto = mutableMapOf<Long, List<FotoExportada>>()
        val faltantes = mutableListOf<String>()

        for (producto in productos) {
            val fotos = mutableListOf<FotoExportada>()
            val base = baseNombreFoto(producto)
            for (ruta in producto.listaImagenes) {
                val archivo = archivoDeRuta(ruta)
                if (archivo == null || !existe(archivo)) {
                    faltantes.add(ruta)
                    continue
                }
                val n = fotos.size + 1
                val extension = extensionFoto(archivo.name)
                var nombre = "${base}_$n.$extension"
                var intento = 2
                while (nombre.lowercase(Locale.ROOT) in usados) {
                    nombre = "$base-${intento}_$n.$extension"
                    intento++
                }
                usados.add(nombre.lowercase(Locale.ROOT))
                fotos.add(FotoExportada(archivo, "$CARPETA_FOTOS/$nombre"))
            }
            porProducto[producto.id] = fotos
        }
        return PlanFotos(porProducto, faltantes)
    }

    /** Código de barras con solo letras, números, punto, guion y guion bajo; si no tiene, `id<id>`. */
    fun baseNombreFoto(producto: ProductoEntidad): String {
        val saneado = producto.codigoBarras.orEmpty().trim()
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trimStart('.')
            .take(60)
        return if (saneado.isBlank() || saneado.all { it == '_' }) "id${producto.id}" else saneado
    }

    private fun extensionFoto(nombre: String): String {
        val extension = nombre.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return if (extension.isNotEmpty() && extension.length <= 5 && extension.all { it.isLetterOrDigit() }) extension
        else "jpg"
    }

    /** Las fotos se guardan como ruta absoluta; por si acaso se acepta también `file://`. Otras URIs no se pueden leer. */
    private fun archivoDeRuta(ruta: String): File? = when {
        ruta.startsWith("file://") -> File(ruta.removePrefix("file://"))
        ruta.contains("://") -> null
        else -> File(ruta).takeIf { it.isAbsolute }
    }

    // ------------------------------------------------------------------
    // Contenido
    // ------------------------------------------------------------------

    fun generarProductosJson(productos: List<ProductoEntidad>, fotos: Map<Long, List<FotoExportada>>): String {
        val arreglo = JSONArray()
        for (p in productos) {
            arreglo.put(
                JSONObject()
                    .put("id", p.id)
                    .put("codigo_barras", p.codigoBarras ?: JSONObject.NULL)
                    .put("nombre", p.nombre)
                    .put("descripcion", p.descripcion ?: JSONObject.NULL)
                    .put("precio_costo", p.precioCosto)
                    .put("precio_venta", p.precioVenta)
                    .put("exento_itbis", p.exentoItbis)
                    .put("itbis_incluido", p.itbisIncluido)
                    .put("tasa_itbis", p.tasaItbis)
                    .put("stock", p.stock)
                    .put("controla_stock", p.controlaStock)
                    .put("tipo_item", p.tipoItem)
                    .put("categoria", p.categoria)
                    .put("esta_activo", p.estaActivo)
                    .put("creado_en", fechaIso(p.creadoEn))
                    .put("actualizado_en", fechaIso(p.actualizadoEn))
                    .put("fotos", JSONArray(fotos[p.id].orEmpty().map { it.rutaEnArchivo }))
            )
        }
        return arreglo.toString(2)
    }

    fun generarManifiestoJson(
        fechaGeneracion: Date,
        versionApp: String,
        nombreNegocio: String?,
        cantidadProductos: Int,
        cantidadFotos: Int,
    ): String = JSONObject()
        .put("formato", FORMATO)
        .put("version_formato", VERSION_FORMATO)
        .put("generado_en", fechaIso(fechaGeneracion.time))
        .put("version_app", versionApp)
        .put("negocio", nombreNegocio?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
        .put("cantidad_productos", cantidadProductos)
        .put("cantidad_fotos", cantidadFotos)
        .toString(2)

    /** CSV con las columnas de la plantilla (SI/NO en los booleanos), separado por comas y con fin de línea CRLF. */
    fun generarCsv(productos: List<ProductoEntidad>, fotos: Map<Long, List<FotoExportada>>): String {
        val sb = StringBuilder()
        sb.append(CABECERAS_CSV.joinToString(",")).append("\r\n")
        for (p in productos) {
            val valores = listOf(
                p.codigoBarras.orEmpty(),
                p.nombre,
                p.descripcion.orEmpty(),
                numero(p.precioCosto),
                numero(p.precioVenta),
                numero(p.stock),
                siNo(p.controlaStock),
                p.categoria,
                siNo(p.exentoItbis),
                siNo(p.itbisIncluido),
                numero(p.tasaItbis),
                p.tipoItem,
                siNo(p.estaActivo),
                fotos[p.id].orEmpty().joinToString("|") { it.nombre },
            )
            sb.append(valores.joinToString(",") { campoCsv(it) }).append("\r\n")
        }
        return sb.toString()
    }

    /** Pone el campo entre comillas si tiene coma, punto y coma, comillas, saltos de línea o espacios en los bordes. */
    fun campoCsv(valor: String): String {
        val normalizado = valor.replace("\r\n", "\n").replace('\r', '\n')
        val necesitaComillas = normalizado.any { it == ',' || it == ';' || it == '"' || it == '\n' } ||
            normalizado != normalizado.trim()
        return if (necesitaComillas) "\"${normalizado.replace("\"", "\"\"")}\"" else normalizado
    }

    private fun numero(valor: Double): String =
        if (valor.isNaN() || valor.isInfinite()) "0" else BigDecimal.valueOf(valor).stripTrailingZeros().toPlainString()

    private fun siNo(valor: Boolean) = if (valor) "SI" else "NO"

    // ------------------------------------------------------------------
    // Archivo
    // ------------------------------------------------------------------

    /**
     * Escribe el archivo de exportación en [destino]. Primero se escribe en un temporal y al final se renombra, así
     * nunca queda un archivo a medias con el nombre final. Lanza [ErrorExportacion] con su código si algo falla.
     */
    fun exportar(
        productos: List<ProductoEntidad>,
        opciones: OpcionesExportacion,
        destino: File,
        versionApp: String,
        nombreNegocio: String?,
        fecha: Date = Date(),
        alProgreso: (ProgresoExportacion) -> Unit = {},
    ): ResultadoExportacion {
        val elegidos = filtrarProductos(productos, opciones)
        if (elegidos.isEmpty()) {
            throw ErrorExportacion(
                CodigosExportacion.SIN_PRODUCTOS,
                "productos=${productos.size} incluir_inactivos=${opciones.incluirInactivos}",
                paso = "filtrar",
            )
        }

        alProgreso(ProgresoExportacion("Revisando fotos…", 0, 1))
        val plan = if (opciones.incluirFotos) planificarFotos(elegidos) else PlanFotos(emptyMap(), emptyList())

        val carpeta = destino.absoluteFile.parentFile
            ?: throw ErrorExportacion(CodigosExportacion.ERROR_ESCRITURA, "destino sin carpeta", paso = "preparar")
        if (!carpeta.isDirectory && !carpeta.mkdirs()) {
            throw ErrorExportacion(CodigosExportacion.ERROR_ESCRITURA, "no se pudo crear la carpeta", paso = "preparar")
        }
        val bytesFotos = plan.porProducto.values.flatten().sumOf { it.origen.length() }
        val disponible = carpeta.usableSpace
        if (disponible in 1 until bytesFotos + MARGEN_ESPACIO_BYTES) {
            throw ErrorExportacion(
                CodigosExportacion.SIN_ESPACIO,
                "se necesitan ${bytesFotos + MARGEN_ESPACIO_BYTES} bytes, hay $disponible",
                paso = "preparar",
            )
        }

        val temporal = File(carpeta, destino.name + ".tmp")
        var paso = "fotos"
        try {
            val fotosIncluidas = mutableMapOf<Long, List<FotoExportada>>()
            val faltantes = plan.faltantes.toMutableList()
            val totalPasos = plan.total + 3

            val resultado = ZipOutputStream(FileOutputStream(temporal)).use { zip ->
                var hechos = 0
                for (producto in elegidos) {
                    val incluidas = mutableListOf<FotoExportada>()
                    for (foto in plan.porProducto[producto.id].orEmpty()) {
                        hechos++
                        alProgreso(ProgresoExportacion("Copiando fotos ($hechos de ${plan.total})…", hechos, totalPasos))
                        // Se lee antes de abrir la entrada: una foto que no se puede leer se omite sin dañar el archivo.
                        val bytes = try {
                            foto.origen.readBytes()
                        } catch (e: IOException) {
                            if (esFaltaDeEspacio(e)) throw e
                            faltantes.add(foto.origen.path)
                            null
                        } ?: continue
                        zip.putNextEntry(ZipEntry(foto.rutaEnArchivo))
                        zip.write(bytes)
                        zip.closeEntry()
                        incluidas.add(foto)
                    }
                    fotosIncluidas[producto.id] = incluidas
                }
                val cantidadFotos = fotosIncluidas.values.sumOf { it.size }

                paso = "productos_json"
                alProgreso(ProgresoExportacion("Guardando la lista de productos…", totalPasos - 2, totalPasos))
                escribirTexto(zip, ENTRADA_PRODUCTOS_JSON, generarProductosJson(elegidos, fotosIncluidas))

                paso = "productos_csv"
                alProgreso(ProgresoExportacion("Preparando la hoja para Excel…", totalPasos - 1, totalPasos))
                zip.putNextEntry(ZipEntry(ENTRADA_PRODUCTOS_CSV))
                zip.write(BOM)
                zip.write(generarCsv(elegidos, fotosIncluidas).toByteArray(StandardCharsets.UTF_8))
                zip.closeEntry()

                paso = "manifiesto"
                escribirTexto(
                    zip, ENTRADA_MANIFIESTO,
                    generarManifiestoJson(fecha, versionApp, nombreNegocio, elegidos.size, cantidadFotos),
                )
                alProgreso(ProgresoExportacion("Terminando…", totalPasos, totalPasos))

                paso = "cerrar"
                zip.finish()
                ResultadoExportacion(destino, elegidos.size, cantidadFotos, faltantes)
            }
            paso = "renombrar"
            if (destino.exists()) destino.delete()
            if (!temporal.renameTo(destino)) throw IOException("no se pudo renombrar el temporal")
            return resultado
        } catch (e: ErrorExportacion) {
            temporal.delete()
            throw e
        } catch (e: Exception) {
            temporal.delete()
            val codigo = if (esFaltaDeEspacio(e)) CodigosExportacion.SIN_ESPACIO else CodigosExportacion.ERROR_ESCRITURA
            throw ErrorExportacion(codigo, "${e.javaClass.simpleName}: ${e.message}", e, paso)
        }
    }

    private fun escribirTexto(zip: ZipOutputStream, nombre: String, texto: String) {
        zip.putNextEntry(ZipEntry(nombre))
        zip.write(texto.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }

    fun esFaltaDeEspacio(e: Throwable): Boolean =
        generateSequence(e) { it.cause }.any { it.message?.contains("ENOSPC") == true || it.message?.contains("No space") == true }
}
