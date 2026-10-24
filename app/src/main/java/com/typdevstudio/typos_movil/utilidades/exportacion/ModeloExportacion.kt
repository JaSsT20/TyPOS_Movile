package com.typdevstudio.typos_movil.utilidades.exportacion

import java.io.File
import java.util.UUID

/**
 * Catálogo de errores de la exportación de productos. El código aparece entre paréntesis al final del mensaje para el
 * usuario y en el evento que se reporta; el detalle técnico solo viaja en el evento.
 */
object CodigosExportacion {
    const val SIN_PRODUCTOS = "EXP-001"
    const val SIN_ESPACIO = "EXP-002"
    const val ERROR_ESCRITURA = "EXP-003"

    /** Advertencia, no error: una foto del producto ya no está en el teléfono y se dejó fuera. */
    const val FOTO_FALTANTE = "EXP-004"
    const val ERROR_COMPARTIR = "EXP-005"
    const val ERROR_GUARDAR = "EXP-006"

    fun mensaje(codigo: String): String = when (codigo) {
        SIN_PRODUCTOS -> "No hay productos para exportar. Agregue productos o incluya los inactivos."
        SIN_ESPACIO -> "No se pudo crear el archivo de exportación. Verifique que tenga espacio disponible."
        ERROR_ESCRITURA -> "No se pudo crear el archivo de exportación. Inténtelo de nuevo."
        FOTO_FALTANTE -> "Algunas fotos ya no están en el teléfono y no se incluyeron."
        ERROR_COMPARTIR -> "No se pudo compartir el archivo de exportación. Inténtelo de nuevo."
        ERROR_GUARDAR -> "No se pudo guardar el archivo en Descargas. Pruebe con el botón Compartir."
        else -> "Ocurrió un problema con la exportación. Inténtelo de nuevo."
    } + " ($codigo)"

    fun nuevaOperacion(): String = UUID.randomUUID().toString()
}

/**
 * Falla de una exportación con su código del catálogo. [detalleTecnico] es para el evento (nunca se muestra al
 * usuario). [paso] dice dónde falló.
 */
class ErrorExportacion(
    val codigo: String,
    val detalleTecnico: String? = null,
    causa: Throwable? = null,
    val paso: String? = null,
) : Exception("$codigo${detalleTecnico?.let { ": $it" } ?: ""}", causa) {
    val mensajeUsuario: String get() = CodigosExportacion.mensaje(codigo)
}

/** Tipos de evento de la exportación (la API todavía no los acepta; ver ReportadorEventosExportacion). */
object TiposEventoExportacion {
    const val INICIADA = "exportacion_iniciada"
    const val COMPLETADA = "exportacion_completada"
    const val FALLIDA = "exportacion_fallida"
    const val COMPARTIDA = "exportacion_compartida"
    const val GUARDADA = "exportacion_guardada"
}

data class OpcionesExportacion(
    val incluirInactivos: Boolean = true,
    val incluirFotos: Boolean = true,
)

/** Una foto que va dentro del archivo: de dónde se lee y con qué ruta queda adentro (ej. `fotos/750103_1.jpg`). */
data class FotoExportada(
    val origen: File,
    val rutaEnArchivo: String,
) {
    val nombre: String get() = rutaEnArchivo.substringAfterLast('/')
}

data class ProgresoExportacion(
    val texto: String,
    val actual: Int,
    val total: Int,
) {
    val fraccion: Float get() = if (total <= 0) 0f else (actual.toFloat() / total).coerceIn(0f, 1f)
}

data class ResultadoExportacion(
    val archivo: File,
    val cantidadProductos: Int,
    val cantidadFotos: Int,
    /** Fotos que estaban en el producto pero no en el teléfono (o no se pudieron leer). */
    val fotosFaltantes: List<String>,
) {
    val mensajeExito: String
        get() {
            val base = "Listo. Se exportaron ${plural(cantidadProductos, "producto", "productos")} y " +
                "${plural(cantidadFotos, "foto", "fotos")}."
            return if (fotosFaltantes.isEmpty()) base
            else "$base ${CodigosExportacion.mensaje(CodigosExportacion.FOTO_FALTANTE)}"
        }

    private fun plural(n: Int, uno: String, varios: String) = "$n ${if (n == 1) uno else varios}"
}

data class ExportacionUiState(
    val opciones: OpcionesExportacion = OpcionesExportacion(),
    val estaExportando: Boolean = false,
    val progreso: ProgresoExportacion? = null,
    val resultado: ResultadoExportacion? = null,
    val estaGuardando: Boolean = false,
    val mensajeAlerta: String? = null,
    val esError: Boolean = false,
)
