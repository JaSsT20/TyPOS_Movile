package com.typdevstudio.typos_movil.utilidades.actualizador

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import com.typdevstudio.typos_movil.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
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
 * Servicio encargado de consultar las nuevas versiones publicadas en GitHub Releases.
 * Funciona de forma no intrusiva y sin bloquear la aplicación cuando no hay conexión.
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
                connectTimeout = 4500
                readTimeout = 4500
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
            // Error silencioso para no afectar la experiencia offline del usuario
            null
        } finally {
            conexion?.disconnect()
        }
    }

    /**
     * Compara dos números de versión semántica (ej. "v1.2.0" contra "1.0").
     * Retorna true si [remota] es estrictamente mayor que [local].
     */
    fun esVersionMasReciente(remota: String, local: String): Boolean {
        try {
            val limpiaRemota = remota.removePrefix("v").removePrefix("V").trim()
            val limpiaLocal = local.removePrefix("v").removePrefix("V").trim()

            val partesRemotas = limpiaRemota.split(".").mapNotNull { it.takeWhile { c -> c.isDigit() }.toIntOrNull() }
            val partesLocales = limpiaLocal.split(".").mapNotNull { it.takeWhile { c -> c.isDigit() }.toIntOrNull() }

            val maxLongitud = maxOf(partesRemotas.size, partesLocales.size)
            for (i in 0 until maxLongitud) {
                val numRemoto = partesRemotas.getOrElse(i) { 0 }
                val numLocal = partesLocales.getOrElse(i) { 0 }
                if (numRemoto > numLocal) return true
                if (numRemoto < numLocal) return false
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Abre la URL del APK o la release en el navegador / gestor de descargas del sistema.
     */
    fun iniciarDescarga(contexto: Context, urlDescarga: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlDescarga)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            contexto.startActivity(intent)
        } catch (e: Exception) {
            // Intent alternativo si falla el directo
        }
    }
}
