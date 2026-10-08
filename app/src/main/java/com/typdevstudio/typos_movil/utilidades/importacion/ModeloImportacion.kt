package com.typdevstudio.typos_movil.utilidades.importacion

import java.util.UUID

/**
 * Estados del flujo de importación de productos.
 */
enum class PasoImportacion {
    SELECCION_ARCHIVO,
    VISTA_PREVIA,
    PROCESANDO,
    RESULTADO
}

/**
 * Representa una fila leída desde el archivo CSV/Excel para previsualizar y editar en UI.
 */
data class ProductoImportadoUi(
    val idTemporal: String = UUID.randomUUID().toString(),
    val idExistente: Long? = null,
    val codigoBarras: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val precioCosto: Double = 0.0,
    val precioVenta: Double = 0.0,
    val stock: Double = 0.0,
    val controlaStock: Boolean = true,
    val categoria: String = "General",
    val exentoItbis: Boolean = false,
    val itbisIncluido: Boolean = true,
    val tasaItbis: Double = 18.0,
    val esValido: Boolean = true,
    val esActualizacion: Boolean = false,
    val errores: List<String> = emptyList(),
    val advertencias: List<String> = emptyList()
) {
    /**
     * Calcula el margen de ganancia porcentual estimado.
     */
    val margenGananciaPorcentaje: Double
        get() {
            if (precioVenta <= 0.0 || precioCosto <= 0.0) return 0.0
            val ganancia = precioVenta - precioCosto
            return (ganancia / precioCosto) * 100.0
        }
}

/**
 * Estado general de la interfaz de usuario para la pantalla de importación.
 */
data class ImportacionUiState(
    val pasoActual: PasoImportacion = PasoImportacion.SELECCION_ARCHIVO,
    val nombreArchivo: String? = null,
    val totalLineasLeidas: Int = 0,
    val productos: List<ProductoImportadoUi> = emptyList(),
    val filtroTexto: String = "",
    val mostrarSoloErrores: Boolean = false,
    val actualizarExistentes: Boolean = true,
    val productoParaEditar: ProductoImportadoUi? = null,
    val estaCargandoArchivo: Boolean = false,
    val estaGuardandoEnBd: Boolean = false,
    val totalImportados: Int = 0,
    val totalActualizados: Int = 0,
    val totalCreados: Int = 0,
    val mensajeError: String? = null,
    val mensajeExito: String? = null,
    val uriPlantillaDescargada: android.net.Uri? = null,
    val mostrarNotificacionDescarga: Boolean = false
) {
    val totalProductos: Int get() = productos.size
    val totalValidos: Int get() = productos.count { it.esValido }
    val totalConErrores: Int get() = productos.count { !it.esValido }
    val totalConAdvertencias: Int get() = productos.count { it.esValido && it.advertencias.isNotEmpty() }
    val totalNuevos: Int get() = productos.count { it.esValido && !it.esActualizacion }
    val totalExistentes: Int get() = productos.count { it.esValido && it.esActualizacion }

    /**
     * Lista filtrada según búsqueda y filtro de errores.
     */
    val productosFiltrados: List<ProductoImportadoUi>
        get() {
            return productos.filter { producto ->
                val coincideTexto = filtroTexto.isBlank() ||
                        producto.nombre.contains(filtroTexto, ignoreCase = true) ||
                        producto.codigoBarras.contains(filtroTexto, ignoreCase = true) ||
                        producto.categoria.contains(filtroTexto, ignoreCase = true)

                val coincideError = !mostrarSoloErrores || !producto.esValido

                coincideTexto && coincideError
            }
        }
}
