package com.typdevstudio.typos_movil.utilidades.exportacion

import android.util.Log

/**
 * Registra todo lo que pasa con una exportación (iniciada, completada, fallida, compartida, guardada) con su
 * id de operación, código y detalle técnico.
 *
 * Pendiente de la API: `/eventos/backup/lote` solo acepta los tipos de copias de seguridad
 * (`TiposEvento.DeBackup` en LicenciasApi) y descarta los demás, así que por ahora los eventos solo quedan en el
 * registro del teléfono. Cuando la API acepte los tipos `exportacion_*`, basta con llamar aquí a
 * `ReportadorEventosBackup.registrar(contexto, tipo, idOperacion, codigo, paso, detalleTecnico)`.
 */
object ReportadorEventosExportacion {

    private const val ETIQUETA = "EventosExportacion"

    /** Nunca lanza excepciones. */
    fun registrar(
        tipo: String,
        idOperacion: String,
        codigo: String? = null,
        paso: String? = null,
        detalleTecnico: String? = null,
    ) {
        try {
            val linea = "$tipo codigo=${codigo ?: "-"} paso=${paso ?: "-"} operacion=$idOperacion" +
                (detalleTecnico?.let { " detalle=${it.replace(Regex("\\s+"), " ").take(500)}" } ?: "")
            if (tipo == TiposEventoExportacion.FALLIDA) Log.w(ETIQUETA, linea) else Log.i(ETIQUETA, linea)
        } catch (e: Exception) {
            // Log no está disponible en las pruebas unitarias; registrar nunca debe romper la exportación.
        }
    }
}
