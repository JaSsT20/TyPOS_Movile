package com.typdevstudio.typos_movil.utilidades.actualizador

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.typdevstudio.typos_movil.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Modelo de datos con la información de actualización disponible.
 */
data class InfoActualizacion(
    val hayActualizacion: Boolean,
    val versionRemota: String,
    val versionActual: String,
    val urlDescarga: String,
    val tituloRelease: String,
    val notasCambio: String,
    val tamanoMb: Double = 0.0
)

/**
 * Estado y progreso de la descarga en tiempo real.
 */
data class ProgresoDescarga(
    val estaDescargando: Boolean = false,
    val bytesDescargados: Long = 0L,
    val totalBytes: Long = 0L,
    val porcentaje: Float = 0f,
    val mbDescargados: Double = 0.0,
    val mbTotales: Double = 0.0,
    val completado: Boolean = false,
    val error: String? = null
)

/**
 * Servicio encargado de consultar las nuevas versiones publicadas en GitHub Releases
 * y descargar/instalar los APKs de forma nativa e integrada dentro de la aplicación.
 */
object ActualizadorApp {

    // Repositorio de GitHub oficial para consultar las releases y actualizaciones
    var githubOwner: String = "JaSsT20"
    var githubRepo: String = "TyPOS_Movile"

    /**
     * Verifica si el dispositivo tiene acceso activo a internet.
     */
    fun hayConexionInternet(contexto: Context): Boolean {
        val connectivityManager = contexto.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val redActiva = connectivityManager.activeNetwork ?: return false
            val capacidades = connectivityManager.getNetworkCapabilities(redActiva) ?: return false
            capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    (capacidades.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capacidades.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capacidades.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        } else {
            @Suppress("DEPRECATION")
            val infoRed = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            infoRed != null && infoRed.isConnected
        }
    }

    /**
     * Consulta asíncronamente en GitHub Releases si existe una versión más reciente.
     * Retorna [InfoActualizacion] con los detalles, o null si falla o no hay conexión.
     */
    suspend fun verificarActualizaciones(contexto: Context): InfoActualizacion? = withContext(Dispatchers.IO) {
        if (!hayConexionInternet(contexto)) {
            return@withContext null
        }

        var conexion: HttpURLConnection? = null
        try {
            val urlApi = "https://api.github.com/repos/$githubOwner/$githubRepo/releases/latest"
            val url = URL(urlApi)
            conexion = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "TyPOS_Movil_Android_App")
            }

            if (conexion.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext null
            }

            val reader = BufferedReader(InputStreamReader(conexion.inputStream))
            val respuesta = reader.use { it.readText() }
            val json = JSONObject(respuesta)

            val tagRemoto = json.optString("tag_name", "").trim()
            val tituloRelease = json.optString("name", tagRemoto)
            val notas = json.optString("body", "Mejoras de rendimiento y corrección de errores.")
            val releaseUrl = json.optString("html_url", "")

            var urlDescargaDirecta = releaseUrl
            var tamanoBytes = 0L

            // Buscar si hay un APK en los assets adjuntos de la release
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val nombreAsset = asset.optString("name", "")
                    if (nombreAsset.endsWith(".apk", ignoreCase = true)) {
                        urlDescargaDirecta = asset.optString("browser_download_url", urlDescargaDirecta)
                        tamanoBytes = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            val versionActual = BuildConfig.VERSION_NAME
            val hayNueva = esVersionMasReciente(tagRemoto, versionActual)

            val tamanoMb = if (tamanoBytes > 0) tamanoBytes / (1024.0 * 1024.0) else 0.0

            InfoActualizacion(
                hayActualizacion = hayNueva,
                versionRemota = tagRemoto,
                versionActual = versionActual,
                urlDescarga = urlDescargaDirecta,
                tituloRelease = tituloRelease,
                notasCambio = notas,
                tamanoMb = tamanoMb
            )
        } catch (e: Exception) {
            null
        } finally {
            conexion?.disconnect()
        }
    }

    /**
     * Compara dos números de versión semántica (ej. "v1.2.0" contra "1.0", o "1.0.2A" contra "1.0.2").
     * Retorna true si [remota] es estrictamente mayor que [local].
     */
    fun esVersionMasReciente(remota: String, local: String): Boolean {
        try {
            val limpiaRemota = remota.removePrefix("v").removePrefix("V").trim()
            val limpiaLocal = local.removePrefix("v").removePrefix("V").trim()

            if (limpiaRemota.equals(limpiaLocal, ignoreCase = true)) {
                return false
            }

            val partesRemotas = limpiaRemota.split(".")
            val partesLocales = limpiaLocal.split(".")

            val maxLongitud = maxOf(partesRemotas.size, partesLocales.size)
            for (i in 0 until maxLongitud) {
                val strRemota = partesRemotas.getOrElse(i) { "0" }
                val strLocal = partesLocales.getOrElse(i) { "0" }

                val numRemoto = strRemota.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
                val numLocal = strLocal.takeWhile { it.isDigit() }.toIntOrNull() ?: 0

                if (numRemoto > numLocal) return true
                if (numRemoto < numLocal) return false

                // Si los números base son iguales, comparar sufijos (ej: "2A" vs "2" o "2B" vs "2A")
                val sufijoRemoto = strRemota.dropWhile { it.isDigit() }.trim()
                val sufijoLocal = strLocal.dropWhile { it.isDigit() }.trim()

                if (sufijoRemoto.isNotEmpty() && sufijoLocal.isEmpty()) {
                    return true
                } else if (sufijoRemoto.isEmpty() && sufijoLocal.isNotEmpty()) {
                    return false
                } else if (sufijoRemoto.isNotEmpty() && sufijoLocal.isNotEmpty()) {
                    val comp = sufijoRemoto.compareTo(sufijoLocal, ignoreCase = true)
                    if (comp > 0) return true
                    if (comp < 0) return false
                }
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Descarga el APK directamente dentro de la app con seguimiento de progreso en tiempo real.
     * Al completarse, lanza automáticamente el instalador del paquete de Android.
     */
    suspend fun descargarEInstalarApk(
        contexto: Context,
        urlDescarga: String,
        onProgreso: (ProgresoDescarga) -> Unit
    ) = withContext(Dispatchers.IO) {
        var conexion: HttpURLConnection? = null
        try {
            onProgreso(ProgresoDescarga(estaDescargando = true, porcentaje = 0f))

            var urlActual = urlDescarga
            var redirecciones = 0
            var conexionFinal: HttpURLConnection? = null

            // Seguir redirecciones (GitHub Releases redirige a AWS S3)
            while (redirecciones < 6) {
                val urlObj = URL(urlActual)
                val conn = (urlObj.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 15000
                    readTimeout = 20000
                    setRequestProperty("User-Agent", "TyPOS_Movil_Android_App")
                }
                val codigoEstado = conn.responseCode
                if (codigoEstado == HttpURLConnection.HTTP_MOVED_TEMP ||
                    codigoEstado == HttpURLConnection.HTTP_MOVED_PERM ||
                    codigoEstado == HttpURLConnection.HTTP_SEE_OTHER ||
                    codigoEstado == 307 || codigoEstado == 308
                ) {
                    val nuevaUrl = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (nuevaUrl.isNullOrBlank()) break
                    urlActual = nuevaUrl
                    redirecciones++
                } else {
                    conexionFinal = conn
                    break
                }
            }

            conexion = conexionFinal ?: (URL(urlActual).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 20000
                setRequestProperty("User-Agent", "TyPOS_Movil_Android_App")
            }

            if (conexion.responseCode !in 200..299) {
                onProgreso(ProgresoDescarga(estaDescargando = false, error = "Error del servidor (${conexion.responseCode})"))
                return@withContext
            }

            val totalBytes = conexion.contentLengthLong.let { if (it <= 0) conexion.contentLength.toLong() else it }
            val mbTotales = if (totalBytes > 0) totalBytes / (1024.0 * 1024.0) else 0.0

            val directorioDestino = File(contexto.cacheDir, "actualizaciones").apply {
                if (!exists()) mkdirs()
            }
            val archivoApk = File(directorioDestino, "TyPOS_Movil_Actualizacion.apk")
            if (archivoApk.exists()) {
                archivoApk.delete()
            }

            val input = BufferedInputStream(conexion.inputStream)
            val output = FileOutputStream(archivoApk)

            val buffer = ByteArray(8192)
            var bytesLeidos: Int
            var totalDescargado = 0L
            var ultimoReporteTiempo = System.currentTimeMillis()

            while (input.read(buffer).also { bytesLeidos = it } != -1) {
                output.write(buffer, 0, bytesLeidos)
                totalDescargado += bytesLeidos

                val ahora = System.currentTimeMillis()
                // Reportar progreso cada 100ms para fluidez visual sin saturar la UI
                if (ahora - ultimoReporteTiempo > 100 || totalDescargado == totalBytes) {
                    ultimoReporteTiempo = ahora
                    val porcentaje = if (totalBytes > 0) (totalDescargado.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
                    val mbDescargados = totalDescargado / (1024.0 * 1024.0)
                    onProgreso(
                        ProgresoDescarga(
                            estaDescargando = true,
                            bytesDescargados = totalDescargado,
                            totalBytes = totalBytes,
                            porcentaje = porcentaje,
                            mbDescargados = mbDescargados,
                            mbTotales = mbTotales
                        )
                    )
                }
            }

            output.flush()
            output.close()
            input.close()

            onProgreso(
                ProgresoDescarga(
                    estaDescargando = false,
                    completado = true,
                    porcentaje = 1f,
                    mbDescargados = totalDescargado / (1024.0 * 1024.0),
                    mbTotales = mbTotales
                )
            )

            // Abrir automáticamente el instalador nativo de Android
            lanzarInstaladorApk(contexto, archivoApk)

        } catch (e: Exception) {
            onProgreso(ProgresoDescarga(estaDescargando = false, error = "Error al descargar: ${e.localizedMessage ?: "Error de red"}"))
        } finally {
            conexion?.disconnect()
        }
    }

    /**
     * Dispara el Intent nativo de Android para instalar el APK descargado usando FileProvider.
     */
    fun lanzarInstaladorApk(contexto: Context, archivoApk: File) {
        try {
            val uriApk = FileProvider.getUriForFile(
                contexto,
                "${contexto.packageName}.fileprovider",
                archivoApk
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uriApk, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            contexto.startActivity(intent)
        } catch (e: Exception) {
            // Si el intent directo falla, abrir con el visor de archivos
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.fromFile(archivoApk), "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                contexto.startActivity(intent)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Método de respaldo: Abre la URL en el navegador externo si es necesario.
     */
    fun abrirEnNavegador(contexto: Context, urlDescarga: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlDescarga)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            contexto.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}
