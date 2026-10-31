package com.typdevstudio.typos_movil.utilidades.exportacion

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

/**
 * La parte de Android de la exportación: dónde se crea el archivo (caché de la app), cómo se comparte (Share Sheet
 * vía FileProvider) y cómo se guarda en Descargas (MediaStore en Android 10+), igual que la plantilla de importación.
 */
object GestorArchivoExportacion {

    private const val CARPETA_CACHE = "exportaciones"
    private const val TIPO_MIME = "application/zip"

    private fun carpeta(contexto: Context) = File(contexto.cacheDir, CARPETA_CACHE)

    /** Borra las exportaciones anteriores de la caché y devuelve dónde escribir la nueva. */
    fun prepararDestino(contexto: Context, nombreArchivo: String): File {
        val carpeta = carpeta(contexto)
        carpeta.listFiles()?.forEach { it.delete() }
        carpeta.mkdirs()
        return File(carpeta, nombreArchivo)
    }

    private fun uri(contexto: Context, archivo: File): Uri =
        FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", archivo)

    /** Abre el selector del sistema (WhatsApp, Correo, Drive...). Lanza [ErrorExportacion] si no se pudo. */
    fun compartir(contexto: Context, archivo: File) {
        try {
            val uri = uri(contexto, archivo)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = TIPO_MIME
                clipData = ClipData.newRawUri(archivo.name, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Productos - TyPOS Móvil")
                putExtra(Intent.EXTRA_TEXT, "Adjunto la lista de productos con sus fotos exportada desde TyPOS Móvil.")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            contexto.startActivity(
                Intent.createChooser(intent, "Enviar exportación por...").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            )
        } catch (e: Exception) {
            throw ErrorExportacion(CodigosExportacion.ERROR_COMPARTIR, "${e.javaClass.simpleName}: ${e.message}", e, "compartir")
        }
    }

    /** Copia el archivo a Descargas. Lanza [ErrorExportacion] si no se pudo. */
    fun guardarEnDescargas(contexto: Context, archivo: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val valores = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, archivo.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, TIPO_MIME)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val resolver = contexto.contentResolver
                val destino = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores)
                    ?: throw IllegalStateException("MediaStore no devolvió una ubicación")
                try {
                    val salida = resolver.openOutputStream(destino)
                        ?: throw IllegalStateException("no se pudo abrir la ubicación en Descargas")
                    salida.use { os -> archivo.inputStream().use { it.copyTo(os) } }
                    resolver.update(destino, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
                } catch (e: Exception) {
                    resolver.delete(destino, null, null)
                    throw e
                }
            } else {
                val descargas = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!descargas.exists()) descargas.mkdirs()
                archivo.copyTo(File(descargas, archivo.name), overwrite = true)
            }
        } catch (e: Exception) {
            val codigo = if (GestorExportacionProductos.esFaltaDeEspacio(e)) CodigosExportacion.SIN_ESPACIO
            else CodigosExportacion.ERROR_GUARDAR
            throw ErrorExportacion(codigo, "${e.javaClass.simpleName}: ${e.message}", e, "guardar_descargas")
        }
    }
}
