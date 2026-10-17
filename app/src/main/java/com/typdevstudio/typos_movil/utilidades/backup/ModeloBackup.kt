package com.typdevstudio.typos_movil.utilidades.backup

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Una copia de seguridad guardada en el teléfono: `.typosbk` (con fotos y manifiesto) o `.db` (formato anterior,
 * solo se restaura en el mismo teléfono).
 */
data class ArchivoBackupUi(
    val nombreArchivo: String,
    val nombreVisual: String,
    val fechaCreacion: Long,
    val tamanoBytes: Long,
    val archivo: File,
    val esAutomatico: Boolean,
    val manifiesto: ManifiestoBackup? = null,
    val esFormatoAntiguo: Boolean = false,
    /** `false` si la copia se hizo en otro teléfono (restaurarla pasa por la licencia). */
    val deEsteTelefono: Boolean = true,
    /** `false` si el archivo no se pudo leer (dañado o incompleto). */
    val valida: Boolean = true,
) {
    val tamanoFormateado: String
        get() {
            return when {
                tamanoBytes < 1024 -> "$tamanoBytes B"
                tamanoBytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", tamanoBytes / 1024.0)
                else -> String.format(Locale.US, "%.2f MB", tamanoBytes / (1024.0 * 1024.0))
            }
        }

    val fechaFormateada: String
        get() {
            val sdf = SimpleDateFormat("dd/MM/yyyy 'a las' hh:mm a", Locale("es", "DO"))
            return sdf.format(Date(fechaCreacion))
        }

    /** Nombre del teléfono donde se hizo la copia (o un texto genérico). */
    val nombreOrigen: String get() = manifiesto?.nombreDispositivoOrigen ?: "otro teléfono"

    /** "De este teléfono" o "De otro teléfono: Samsung A54". */
    val origenTexto: String
        get() = when {
            !valida -> "Archivo dañado o incompleto"
            deEsteTelefono -> if (esFormatoAntiguo) "De este teléfono (sin fotos)" else "De este teléfono"
            else -> "De otro teléfono: $nombreOrigen"
        }

    /** "120 productos · 850 ventas · 3 usuarios", o `null` si la copia no lo dice. */
    val resumen: String?
        get() = manifiesto?.conteos?.let { c ->
            listOf(
                plural(c.productos, "producto", "productos"),
                plural(c.ventas, "venta", "ventas"),
                plural(c.usuarios, "usuario", "usuarios"),
            ).joinToString(" · ")
        }

    private fun plural(n: Int, uno: String, varios: String) = "$n ${if (n == 1) uno else varios}"
}

/** Qué aviso se muestra antes de restaurar (según la tabla del diseño). */
sealed interface VarianteRestauracion {
    /** Copia de este teléfono, o este teléfono ya está en la licencia: confirmación normal. */
    data object Normal : VarianteRestauracion

    /** Copia de otro teléfono y hay cupo: este teléfono se agrega a la licencia y el otro sigue funcionando. */
    data class AgregarTelefono(val nombreOrigen: String, val quedariaEn: Int, val maximo: Int) : VarianteRestauracion

    /** Copia de otro teléfono y sin cupo: el otro teléfono se desvincula y queda en modo consulta. */
    data class Desvincular(val nombreOrigen: String, val maximo: Int) : VarianteRestauracion
}

/** Restauración pendiente de que el usuario confirme. */
data class ConfirmacionRestauracion(
    val backup: ArchivoBackupUi,
    val variante: VarianteRestauracion,
    val idOperacion: String,
    val deOtroTelefono: Boolean,
    /** `true` si hay que llamar a `/transferir` antes de restaurar (este teléfono todavía no está en la licencia). */
    val requiereTransferencia: Boolean = false,
)

/**
 * Estado general de la pantalla de gestión de copias de seguridad.
 */
data class BackupUiState(
    val backups: List<ArchivoBackupUi> = emptyList(),
    val estaCreandoBackup: Boolean = false,
    val estaRestaurandoBackup: Boolean = false,
    val estaImportando: Boolean = false,
    /** Qué se está haciendo mientras [estaRestaurandoBackup] (para mostrarlo al usuario). */
    val textoProgreso: String? = null,
    val confirmacion: ConfirmacionRestauracion? = null,
    val backupSeleccionadoParaEliminar: ArchivoBackupUi? = null,
    val backupParaCompartir: ArchivoBackupUi? = null,
    /** Restauración terminada: la app se reinicia al cerrar este aviso. */
    val mensajeReinicio: String? = null,
    val reinicioEsError: Boolean = false,
    val mensajeAlerta: String? = null,
    val esError: Boolean = false,
    val proximoBackupTexto: String = "Todos los días a las 7:30 p.m."
) {
    val totalBackups: Int get() = backups.size
    val maximoPermitido: Int get() = GestorBackup.MAXIMO_BACKUPS
    val ocupado: Boolean get() = estaCreandoBackup || estaRestaurandoBackup || estaImportando
}
