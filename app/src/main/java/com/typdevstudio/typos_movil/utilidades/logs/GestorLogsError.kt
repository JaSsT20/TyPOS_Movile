package com.typdevstudio.typos_movil.utilidades.logs

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GestorLogsError {

    private const val TAG = "GestorLogsError"
    private const val CARPETA_LOGS = "logs"
    private const val NOMBRE_ULTIMO_LOG = "ultimo_error.txt"
    private const val NOMBRE_HISTORIAL_LOG = "historial_errores.txt"

    /**
     * Obtiene el directorio para almacenar los archivos de logs.
     * Prefiere el almacenamiento externo específico de la app (accesible sin permisos)
     * y como alternativa usa el almacenamiento interno.
     */
    fun obtenerDirectorioLogs(contexto: Context): File {
        val dir = contexto.getExternalFilesDir(CARPETA_LOGS) ?: File(contexto.filesDir, CARPETA_LOGS)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Obtiene el archivo del último error registrado.
     */
    fun obtenerArchivoUltimoLog(contexto: Context): File {
        return File(obtenerDirectorioLogs(contexto), NOMBRE_ULTIMO_LOG)
    }

    /**
     * Guarda el detalle completo de una excepción en archivos de texto (.txt).
     * Retorna el archivo generado con el reporte.
     */
    fun guardarError(contexto: Context, throwable: Throwable, hilo: Thread? = null): File {
        val dir = obtenerDirectorioLogs(contexto)
        val archivoUltimo = File(dir, NOMBRE_ULTIMO_LOG)
        val archivoHistorial = File(dir, NOMBRE_HISTORIAL_LOG)

        val reporte = construirReporteError(contexto, throwable, hilo)

        try {
            // Guardar o sobreescribir el último log para acceso rápido
            archivoUltimo.writeText(reporte)

            // Adjuntar al historial general acumulado
            FileWriter(archivoHistorial, true).use { writer ->
                writer.append("\n\n=================================================================\n")
                writer.append(reporte)
                writer.append("\n=================================================================\n")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al escribir el archivo de log", e)
        }

        return archivoUltimo
    }

    /**
     * Construye un informe formateado con diagnóstico completo del sistema y el stack trace.
     */
    fun construirReporteError(contexto: Context, throwable: Throwable, hilo: Thread? = null): String {
        val fechaFormateada = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val versionApp = try {
            val pInfo = contexto.packageManager.getPackageInfo(contexto.packageName, 0)
            "${pInfo.versionName} (Código ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pInfo.longVersionCode else @Suppress("DEPRECATION") pInfo.versionCode})"
        } catch (e: Exception) {
            "Desconocida"
        }

        val stringWriter = StringWriter()
        throwable.printStackTrace(PrintWriter(stringWriter))
        val stackTrace = stringWriter.toString()

        val runtime = Runtime.getRuntime()
        val memoriaUsadaMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val memoriaMaxMb = runtime.maxMemory() / (1024 * 1024)

        return """
        =================================================================
                       REPORTE DE ERROR - TyPOS MÓVIL
        =================================================================
        Fecha y Hora: $fechaFormateada
        Hilo: ${hilo?.name ?: Thread.currentThread().name}
        
        --- INFORMACIÓN DE LA APLICACIÓN ---
        Paquete: ${contexto.packageName}
        Versión: $versionApp
        
        --- INFORMACIÓN DEL DISPOSITIVO ---
        Fabricante: ${Build.MANUFACTURER}
        Modelo: ${Build.MODEL} (${Build.PRODUCT})
        Dispositivo / Hardware: ${Build.DEVICE} / ${Build.HARDWARE}
        Versión Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
        ABIs soportadas: ${Build.SUPPORTED_ABIS.joinToString(", ")}
        Memoria RAM App: ${memoriaUsadaMb} MB usados de ${memoriaMaxMb} MB máx.
        
        --- DETALLES DEL ERROR ---
        Tipo de Excepción: ${throwable.javaClass.name}
        Mensaje: ${throwable.localizedMessage ?: throwable.message ?: "Sin mensaje detallado"}
        
        --- STACK TRACE COMPLETO ---
        $stackTrace
        =================================================================
        """.trimIndent()
    }

    /**
     * Abre el menú nativo para compartir el archivo de log (WhatsApp, Gmail, Bluetooth, etc.).
     */
    fun compartirLog(contexto: Context, archivo: File = obtenerArchivoUltimoLog(contexto)) {
        if (!archivo.exists()) {
            return
        }

        try {
            val uri: Uri = FileProvider.getUriForFile(
                contexto,
                "${contexto.packageName}.fileprovider",
                archivo
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Reporte de Error TyPOS Móvil - ${archivo.name}")
                putExtra(Intent.EXTRA_TEXT, "Adjunto reporte de error generado en TyPOS Móvil.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "Compartir Reporte de Error").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            contexto.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo compartir el archivo de log", e)
        }
    }
}
