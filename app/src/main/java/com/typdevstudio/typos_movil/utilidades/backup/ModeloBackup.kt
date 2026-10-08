package com.typdevstudio.typos_movil.utilidades.backup

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Representa una copia de seguridad física de la base de datos de TyPOS Móvil.
 */
data class ArchivoBackupUi(
    val nombreArchivo: String,
    val nombreVisual: String,
    val fechaCreacion: Long,
    val tamanoBytes: Long,
    val archivo: File,
    val esAutomatico: Boolean
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
            val sdf = SimpleDateFormat("dd/MM/yyyy 'a las' hh:mm:ss a", Locale("es", "DO"))
            return sdf.format(Date(fechaCreacion))
        }
}

/**
 * Estado general de la pantalla de gestión de copias de seguridad.
 */
data class BackupUiState(
    val backups: List<ArchivoBackupUi> = emptyList(),
    val estaCreandoBackup: Boolean = false,
    val estaRestaurandoBackup: Boolean = false,
    val backupSeleccionadoParaRestaurar: ArchivoBackupUi? = null,
    val backupSeleccionadoParaEliminar: ArchivoBackupUi? = null,
    val mensajeAlerta: String? = null,
    val esError: Boolean = false,
    val proximoBackupTexto: String = "Todos los días a las 7:30 p.m."
) {
    val totalBackups: Int get() = backups.size
    val maximoPermitido: Int get() = GestorBackup.MAXIMO_BACKUPS
}
