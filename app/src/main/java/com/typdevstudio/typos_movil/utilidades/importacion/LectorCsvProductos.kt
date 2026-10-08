package com.typdevstudio.typos_movil.utilidades.importacion

import android.content.Context
import android.net.Uri
import com.typdevstudio.typos_movil.datos.local.entidades.ProductoEntidad
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Locale

object LectorCsvProductos {

    /**
     * Procesa un archivo CSV desde un URI de Android, validando cada fila contra los productos existentes en la base de datos.
     */
    fun leerDesdeUri(
        contexto: Context,
        uri: Uri,
        productosExistentes: List<ProductoEntidad>
    ): List<ProductoImportadoUi> {
        return contexto.contentResolver.openInputStream(uri)?.use { inputStream ->
            leerDesdeInputStream(inputStream, productosExistentes)
        } ?: emptyList()
    }

    /**
     * Parsea un flujo de entrada (InputStream) decodificando CSV con soporte para delimitadores coma (,) y punto y coma (;).
     */
    fun leerDesdeInputStream(
        inputStream: InputStream,
        productosExistentes: List<ProductoEntidad>
    ): List<ProductoImportadoUi> {
        val lineasCrudas = mutableListOf<String>()

        BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { lector ->
            var linea: String? = lector.readLine()

            // Eliminar posible marca BOM UTF-8 del inicio si está presente
            if (linea != null && linea.startsWith("\uFEFF")) {
                linea = linea.substring(1)
            }

            while (linea != null) {
                if (linea.isNotBlank()) {
                    lineasCrudas.add(linea)
                }
                linea = lector.readLine()
            }
        }

        if (lineasCrudas.isEmpty()) {
            return emptyList()
        }

        // 1. Detectar el delimitador basado en la fila de encabezados
        val lineaEncabezado = lineasCrudas.first()
        val delimitador = detectarDelimitador(lineaEncabezado)

        // 2. Parsear y mapear índices de columnas
        val encabezados = parsearFilaCsv(lineaEncabezado, delimitador).map { normalizarTexto(it) }
        val mapaColumnas = mapearIndicesColumnas(encabezados)

        // Mapeo rápido de productos existentes por código de barras y nombre
        val mapaPorCodigo = productosExistentes
            .filter { !it.codigoBarras.isNullOrBlank() }
            .associateBy { it.codigoBarras!!.trim().lowercase(Locale.ROOT) }

        val mapaPorNombre = productosExistentes
            .associateBy { it.nombre.trim().lowercase(Locale.ROOT) }

        val listaResultados = mutableListOf<ProductoImportadoUi>()

        // 3. Procesar filas de datos (a partir de la línea 1)
        for (i in 1 until lineasCrudas.size) {
            val lineaActual = lineasCrudas[i]
            if (lineaActual.isBlank()) continue

            val campos = parsearFilaCsv(lineaActual, delimitador)
            if (campos.all { it.isBlank() }) continue // Fila vacía

            val producto = construirYValidarProducto(
                campos = campos,
                mapaColumnas = mapaColumnas,
                mapaPorCodigo = mapaPorCodigo,
                mapaPorNombre = mapaPorNombre
            )

            listaResultados.add(producto)
        }

        return listaResultados
    }

    /**
     * Construye y valida una fila individual para la vista previa y edición en pantalla.
     */
    fun construirYValidarProducto(
        campos: List<String>,
        mapaColumnas: Map<String, Int>,
        mapaPorCodigo: Map<String, ProductoEntidad>,
        mapaPorNombre: Map<String, ProductoEntidad>
    ): ProductoImportadoUi {
        fun obtenerCampo(clave: String): String {
            val indice = mapaColumnas[clave] ?: return ""
            return if (indice in campos.indices) campos[indice].trim() else ""
        }

        val codigoBarras = obtenerCampo("codigo_barras")
        val nombre = obtenerCampo("nombre")
        val descripcion = obtenerCampo("descripcion")
        val precioCostoStr = obtenerCampo("precio_costo")
        val precioVentaStr = obtenerCampo("precio_venta")
        val stockStr = obtenerCampo("stock")
        val controlaStockStr = obtenerCampo("controla_stock")
        val categoriaStr = obtenerCampo("categoria")
        val exentoItbisStr = obtenerCampo("exento_itbis")
        val itbisIncluidoStr = obtenerCampo("itbis_incluido")
        val tasaItbisStr = obtenerCampo("tasa_itbis")

        val precioCosto = parsearNumero(precioCostoStr, porDefecto = 0.0)
        val precioVenta = parsearNumero(precioVentaStr, porDefecto = 0.0)
        val stock = parsearNumero(stockStr, porDefecto = 0.0)
        val controlaStock = parsearBooleano(controlaStockStr, porDefecto = true)
        val categoria = categoriaStr.ifBlank { "General" }
        val exentoItbis = parsearBooleano(exentoItbisStr, porDefecto = false)
        val itbisIncluido = parsearBooleano(itbisIncluidoStr, porDefecto = true)
        val tasaItbis = if (exentoItbis) 0.0 else parsearNumero(tasaItbisStr, porDefecto = 18.0)

        // Validación y detección de inconsistencias
        val errores = mutableListOf<String>()
        val advertencias = mutableListOf<String>()

        if (nombre.isBlank()) {
            errores.add("El nombre del producto es obligatorio.")
        }

        if (precioVenta <= 0.0) {
            errores.add("El precio de venta debe ser un número mayor que 0.")
        }

        if (precioCosto < 0.0) {
            errores.add("El precio de costo no puede ser negativo.")
        }

        if (precioCosto > 0.0 && precioVenta > 0.0 && precioCosto > precioVenta) {
            advertencias.add("El costo ($${formatearMoneda(precioCosto)}) es mayor al precio de venta ($${formatearMoneda(precioVenta)}).")
        }

        if (codigoBarras.isBlank()) {
            advertencias.add("No tiene código de barras.")
        }

        // Detección de producto existente para actualización
        var idExistente: Long? = null
        var esActualizacion = false

        if (codigoBarras.isNotBlank()) {
            val productoExistente = mapaPorCodigo[codigoBarras.lowercase(Locale.ROOT)]
            if (productoExistente != null) {
                idExistente = productoExistente.id
                esActualizacion = true
                advertencias.add("Actualizará el producto existente: \"${productoExistente.nombre}\" (ID: ${productoExistente.id}).")
            }
        } else if (nombre.isNotBlank()) {
            val productoPorNombre = mapaPorNombre[nombre.lowercase(Locale.ROOT)]
            if (productoPorNombre != null) {
                advertencias.add("Existe otro producto con este mismo nombre (ID: ${productoPorNombre.id}).")
            }
        }

        return ProductoImportadoUi(
            idExistente = idExistente,
            codigoBarras = codigoBarras,
            nombre = nombre,
            descripcion = descripcion,
            precioCosto = precioCosto,
            precioVenta = precioVenta,
            stock = stock,
            controlaStock = controlaStock,
            categoria = categoria,
            exentoItbis = exentoItbis,
            itbisIncluido = itbisIncluido,
            tasaItbis = tasaItbis,
            esValido = errores.isEmpty(),
            esActualizacion = esActualizacion,
            errores = errores,
            advertencias = advertencias
        )
    }

    /**
     * Re-valida un producto importado modificado directamente desde la UI de vista previa.
     */
    fun revalidarProducto(
        producto: ProductoImportadoUi,
        productosExistentes: List<ProductoEntidad>
    ): ProductoImportadoUi {
        val errores = mutableListOf<String>()
        val advertencias = mutableListOf<String>()

        if (producto.nombre.trim().isBlank()) {
            errores.add("El nombre del producto es obligatorio.")
        }

        if (producto.precioVenta <= 0.0) {
            errores.add("El precio de venta debe ser mayor a 0.")
        }

        if (producto.precioCosto < 0.0) {
            errores.add("El precio de costo no puede ser negativo.")
        }

        if (producto.precioCosto > 0.0 && producto.precioVenta > 0.0 && producto.precioCosto > producto.precioVenta) {
            advertencias.add("El costo es mayor al precio de venta.")
        }

        if (producto.codigoBarras.trim().isBlank()) {
            advertencias.add("No tiene código de barras.")
        }

        var idExistente: Long? = producto.idExistente
        var esActualizacion = producto.esActualizacion

        if (producto.codigoBarras.trim().isNotBlank()) {
            val existente = productosExistentes.find {
                it.codigoBarras?.equals(producto.codigoBarras.trim(), ignoreCase = true) == true
            }
            if (existente != null) {
                idExistente = existente.id
                esActualizacion = true
                advertencias.add("Actualizará el producto: \"${existente.nombre}\".")
            } else {
                idExistente = null
                esActualizacion = false
            }
        }

        return producto.copy(
            codigoBarras = producto.codigoBarras.trim(),
            nombre = producto.nombre.trim(),
            descripcion = producto.descripcion.trim(),
            categoria = producto.categoria.trim().ifBlank { "General" },
            idExistente = idExistente,
            esActualizacion = esActualizacion,
            esValido = errores.isEmpty(),
            errores = errores,
            advertencias = advertencias
        )
    }

    /**
     * Mapea nombres de columnas flexibles a claves canónicas.
     */
    private fun mapearIndicesColumnas(encabezados: List<String>): Map<String, Int> {
        val mapa = mutableMapOf<String, Int>()

        for ((indice, col) in encabezados.withIndex()) {
            when {
                col in listOf("codigobarras", "codigo_barras", "codigobarra", "codigo", "barcode", "cod", "sku") ->
                    mapa["codigo_barras"] = indice

                col in listOf("nombre", "producto", "articulo", "item", "descripcioncorta", "nombreproducto") ->
                    mapa["nombre"] = indice

                col in listOf("descripcion", "detalle", "detalles", "info", "notas") ->
                    mapa["descripcion"] = indice

                col in listOf("preciocosto", "precio_costo", "costo", "costounitario", "costocompra") ->
                    mapa["precio_costo"] = indice

                col in listOf("precioventa", "precio_venta", "precio", "preciounitario", "pvp", "valor") ->
                    mapa["precio_venta"] = indice

                col in listOf("stock", "cantidad", "existencia", "inventario", "unidades", "qty") ->
                    mapa["stock"] = indice

                col in listOf("controlastock", "controla_stock", "controlainventario", "manejastock", "esinventariable") ->
                    mapa["controla_stock"] = indice

                col in listOf("categoria", "rubro", "departamento", "familia", "grupo", "seccion") ->
                    mapa["categoria"] = indice

                col in listOf("exentoitbis", "exento_itbis", "exento", "exentoimpuesto", "esexento") ->
                    mapa["exento_itbis"] = indice

                col in listOf("itbisincluido", "itbis_incluido", "impuestoincluido", "conitbis", "iva_incluido") ->
                    mapa["itbis_incluido"] = indice

                col in listOf("tasaitbis", "tasa_itbis", "porcentajeitbis", "itbis", "impuesto", "tasa") ->
                    mapa["tasa_itbis"] = indice
            }
        }

        // Si no encontró "nombre" por palabras clave, usar la primera columna que contenga "nom" o "prod" o el índice 1 si el índice 0 es código
        if (!mapa.containsKey("nombre")) {
            val indicePosible = encabezados.indexOfFirst { it.contains("nom") || it.contains("prod") }
            if (indicePosible != -1) {
                mapa["nombre"] = indicePosible
            } else if (encabezados.size > 1 && mapa["codigo_barras"] == 0) {
                mapa["nombre"] = 1
            } else if (encabezados.isNotEmpty() && !mapa.containsKey("codigo_barras")) {
                mapa["nombre"] = 0
            }
        }

        // Si no encontró "precio_venta", buscar "prec" o el siguiente índice numérico
        if (!mapa.containsKey("precio_venta")) {
            val indicePrecio = encabezados.indexOfFirst { it.contains("prec") }
            if (indicePrecio != -1) {
                mapa["precio_venta"] = indicePrecio
            }
        }

        return mapa
    }

    /**
     * Parsea una fila CSV manejando entrecomillados ("...") y caracteres especiales.
     */
    fun parsearFilaCsv(linea: String, delimitador: Char): List<String> {
        val resultado = mutableListOf<String>()
        val sb = java.lang.StringBuilder()
        var dentroDeComillas = false
        var i = 0

        while (i < linea.length) {
            val c = linea[i]

            if (c == '"') {
                if (dentroDeComillas && i + 1 < linea.length && linea[i + 1] == '"') {
                    // Doble comilla escapada: "" -> "
                    sb.append('"')
                    i++
                } else {
                    dentroDeComillas = !dentroDeComillas
                }
            } else if (c == delimitador && !dentroDeComillas) {
                resultado.add(sb.toString().trim())
                sb.setLength(0)
            } else {
                sb.append(c)
            }
            i++
        }

        resultado.add(sb.toString().trim())
        return resultado
    }

    /**
     * Detecta si el delimitador predominante es coma (,) o punto y coma (;).
     */
    private fun detectarDelimitador(linea: String): Char {
        val cantidadPuntoYComa = linea.count { it == ';' }
        val cantidadComa = linea.count { it == ',' }
        val cantidadTab = linea.count { it == '\t' }

        return when {
            cantidadPuntoYComa > cantidadComa && cantidadPuntoYComa > cantidadTab -> ';'
            cantidadTab > cantidadComa && cantidadTab > cantidadPuntoYComa -> '\t'
            else -> ','
        }
    }

    /**
     * Convierte texto a formato canónico sin acentos, mayúsculas ni espacios.
     */
    private fun normalizarTexto(texto: String): String {
        return texto.lowercase(Locale.ROOT)
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .replace("ñ", "n")
            .replace(" ", "")
            .replace("_", "")
            .replace("-", "")
            .replace(".", "")
            .trim()
    }

    /**
     * Parsea valores numéricos manejando formatos de moneda, comas y puntos decimales.
     */
    private fun parsearNumero(texto: String, porDefecto: Double): Double {
        if (texto.isBlank()) return porDefecto

        val limpio = texto
            .replace("RD$", "", ignoreCase = true)
            .replace("USD", "", ignoreCase = true)
            .replace("$", "")
            .replace("%", "")
            .replace(" ", "")
            .trim()

        // Si tiene coma y punto, determinar cuál es separador de miles y cuál decimal
        val numeroFinal = if (limpio.contains(",") && limpio.contains(".")) {
            if (limpio.indexOf('.') < limpio.indexOf(',')) {
                // Formato europeo: 1.250,50 -> 1250.50
                limpio.replace(".", "").replace(",", ".")
            } else {
                // Formato americano: 1,250.50 -> 1250.50
                limpio.replace(",", "")
            }
        } else if (limpio.contains(",")) {
            limpio.replace(",", ".")
        } else {
            limpio
        }

        return numeroFinal.toDoubleOrNull() ?: porDefecto
    }

    /**
     * Parsea valores booleanos flexibles (SI, NO, TRUE, FALSE, 1, 0, VERDADERO, FALSO).
     */
    private fun parsearBooleano(texto: String, porDefecto: Boolean): Boolean {
        if (texto.isBlank()) return porDefecto
        val t = normalizarTexto(texto)
        return when (t) {
            "si", "s", "true", "1", "verdadero", "yes", "y", "activo" -> true
            "no", "n", "false", "0", "falso", "inactivo" -> false
            else -> porDefecto
        }
    }

    private fun formatearMoneda(valor: Double): String {
        return String.format(Locale.US, "%.2f", valor)
    }
}
