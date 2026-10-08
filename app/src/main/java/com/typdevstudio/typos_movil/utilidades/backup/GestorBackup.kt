package com.typdevstudio.typos_movil.utilidades.backup

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.typdevstudio.typos_movil.datos.local.AppBaseDatos
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GestorBackup {

    const val MAXIMO_BACKUPS = 7
    const val PREFIJO_BACKUP = "TyPOS_BK_"
    private const val NOMBRE_BD = "typos_movil_bd.db"

    /**
     * Directorio donde se almacenan las copias de seguridad de la base de datos.
     */
    fun obtenerDirectorioBackups(contexto: Context): File {
        val dir = File(contexto.getExternalFilesDir(null), "backups")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Realiza un checkpoint en la base de datos SQLite y copia el archivo .db con el formato solicitado:
     * Nombre en disco: TyPOS_BK_ddMMyyyy_HH-mm-ss.db
     * Nombre visual:   TyPOS_BK_ddMMyyyy_HH:mm:ss
     */
    fun crearBackup(contexto: Context, esAutomatico: Boolean = false): Result<ArchivoBackupUi> {
        return try {
            val baseDatos = AppBaseDatos.obtenerBaseDatos(contexto)

            // 1. Vaciar el archivo WAL a la base de datos principal (checkpoint completo)
            try {
                val db = baseDatos.openHelper.writableDatabase
                val cursor = db.query("PRAGMA wal_checkpoint(FULL)")
                cursor.moveToFirst()
                cursor.close()
            } catch (e: Exception) {
                // Continuar con la copia si el checkpoint ya estaba sincronizado
            }

            val archivoBdOriginal = contexto.getDatabasePath(NOMBRE_BD)
            if (!archivoBdOriginal.exists()) {
                return Result.failure(Exception("No se encontró el archivo de base de datos para respaldar."))
            }

            val ahora = Date()
            val formatoFecha = SimpleDateFormat("ddMMyyyy_HH-mm-ss", Locale.US)
            val timestampStr = formatoFecha.format(ahora)

            // Nombre de archivo seguro para sistemas de archivos de Android/Windows
            val nombreArchivo = "${PREFIJO_BACKUP}${timestampStr}.db"
            val nombreVisual = "${PREFIJO_BACKUP}${SimpleDateFormat("ddMMyyyy_HH:mm:ss", Locale.US).format(ahora)}"

            val dirBackups = obtenerDirectorioBackups(contexto)
            val archivoDestino = File(dirBackups, nombreArchivo)

            // 2. Copiar archivo de base de datos
            FileInputStream(archivoBdOriginal).use { input ->
                FileOutputStream(archivoDestino).use { output ->
                    input.copyTo(output)
                }
            }

            // 3. Aplicar política de retención estricta: conservar únicamente los últimos 7 backups
            aplicarPoliticaRetencion(contexto)

            val backupUi = ArchivoBackupUi(
                nombreArchivo = nombreArchivo,
                nombreVisual = nombreVisual,
                fechaCreacion = archivoDestino.lastModified(),
                tamanoBytes = archivoDestino.length(),
                archivo = archivoDestino,
                esAutomatico = esAutomatico
            )

            Result.success(backupUi)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mantiene únicamente las últimas 7 copias de seguridad ordenadas por fecha, eliminando las más antiguas.
     */
    fun aplicarPoliticaRetencion(contexto: Context) {
        try {
            val dir = obtenerDirectorioBackups(contexto)
            val archivos = dir.listFiles { file ->
                file.isFile && file.name.startsWith(PREFIJO_BACKUP) && file.name.endsWith(".db")
            }?.sortedByDescending { it.lastModified() } ?: return

            if (archivos.size > MAXIMO_BACKUPS) {
                for (i in MAXIMO_BACKUPS until archivos.size) {
                    archivos[i].delete()
                }
            }
        } catch (e: Exception) {
            // Ignorar errores menores al purgar archivos antiguos
        }
    }

    fun formatearNombreVisual(nombreArchivo: String): String {
        val sinExtension = nombreArchivo.removeSuffix(".db")
        val partes = sinExtension.split("_")
        return if (partes.size >= 4 && partes[0] == "TyPOS" && partes[1] == "BK") {
            val fecha = partes[2]
            val hora = partes[3].replace("-", ":")
            "TyPOS_BK_${fecha}_${hora}"
        } else {
            sinExtension
        }
    }

    /**
     * Obtiene la lista ordenada de los últimos backups disponibles (máximo 7).
     */
    fun obtenerListaBackups(contexto: Context): List<ArchivoBackupUi> {
        val dir = obtenerDirectorioBackups(contexto)
        val archivos = dir.listFiles { file ->
            file.isFile && file.name.startsWith(PREFIJO_BACKUP) && file.name.endsWith(".db")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()

        return archivos.take(MAXIMO_BACKUPS).map { file ->
            val nombreVisual = formatearNombreVisual(file.name)

            ArchivoBackupUi(
                nombreArchivo = file.name,
                nombreVisual = nombreVisual,
                fechaCreacion = file.lastModified(),
                tamanoBytes = file.length(),
                archivo = file,
                esAutomatico = file.name.contains("19-30") || file.name.contains("1930")
            )
        }
    }

    /**
     * Restaura una copia de seguridad seleccionada sobre la base de datos principal.
     */
    fun restaurarBackup(contexto: Context, archivoBackup: File): Boolean {
        return try {
            if (!archivoBackup.exists()) return false

            val archivoBdOriginal = contexto.getDatabasePath(NOMBRE_BD)
            val archivoWal = File(archivoBdOriginal.parentFile, "$NOMBRE_BD-wal")
            val archivoShm = File(archivoBdOriginal.parentFile, "$NOMBRE_BD-shm")

            // 1. Eliminar archivos temporales WAL y SHM
            if (archivoWal.exists()) archivoWal.delete()
            if (archivoShm.exists()) archivoShm.delete()

            // 2. Sobreescribir archivo principal .db
            FileInputStream(archivoBackup).use { input ->
                FileOutputStream(archivoBdOriginal).use { output ->
                    input.copyTo(output)
                }
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Comparte el archivo de backup a través del menú nativo (WhatsApp, Drive, Gmail, etc.).
     */
    fun compartirBackup(contexto: Context, archivo: File) {
        try {
            if (!archivo.exists()) {
                Toast.makeText(contexto, "El archivo de copia de seguridad no existe", Toast.LENGTH_SHORT).show()
                return
            }

            val uri: Uri = FileProvider.getUriForFile(
                contexto,
                "${contexto.packageName}.fileprovider",
                archivo
            )

            val intentCompartir = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                clipData = ClipData.newRawUri(archivo.name, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Copia de Seguridad - TyPOS Móvil (${archivo.name})")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Adjunto la copia de seguridad de la base de datos de TyPOS Móvil: ${archivo.name}"
                )
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intentCompartir, "Enviar copia de seguridad por...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            contexto.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(
                contexto,
                "Error al compartir: ${e.localizedMessage ?: "Error desconocido"}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Elimina un archivo de copia de seguridad.
     */
    fun eliminarBackup(archivo: File): Boolean {
        return try {
            if (archivo.exists()) {
                archivo.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}
