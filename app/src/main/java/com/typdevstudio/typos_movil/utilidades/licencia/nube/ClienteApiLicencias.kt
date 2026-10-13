package com.typdevstudio.typos_movil.utilidades.licencia.nube

import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/** Respuesta HTTP cruda: código de estado y cuerpo. */
data class RespuestaHttp(val codigo: Int, val cuerpo: String)

/** Envío HTTP abstraído para probar [ClienteApiLicencias] sin red. */
fun interface TransporteHttp {
    /** @throws IOException ante cualquier falla de red o timeout. */
    fun enviar(metodo: String, url: String, cuerpoJson: String?): RespuestaHttp
}

/** [TransporteHttp] con `HttpURLConnection`, igual que el actualizador de la app. Sin librerías extra. */
class TransporteHttpUrlConnection(private val timeoutMs: Int = 10_000) : TransporteHttp {

    override fun enviar(metodo: String, url: String, cuerpoJson: String?): RespuestaHttp {
        val conexion = URL(url).openConnection() as HttpURLConnection
        try {
            conexion.requestMethod = metodo
            conexion.connectTimeout = timeoutMs
            conexion.readTimeout = timeoutMs
            conexion.useCaches = false
            conexion.instanceFollowRedirects = false
            conexion.setRequestProperty("Accept", "application/json")
            if (cuerpoJson != null) {
                val bytes = cuerpoJson.toByteArray(Charsets.UTF_8)
                conexion.doOutput = true
                conexion.setFixedLengthStreamingMode(bytes.size)
                conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexion.outputStream.use { it.write(bytes) }
            }
            val codigo = conexion.responseCode
            val flujo = if (codigo >= 400) conexion.errorStream else conexion.inputStream
            return RespuestaHttp(codigo, flujo?.use { leerLimitado(it) } ?: "")
        } finally {
            conexion.disconnect()
        }
    }

    private fun leerLimitado(flujo: InputStream): String {
        val salida = ByteArrayOutputStream()
        val bufer = ByteArray(4096)
        while (true) {
            val leidos = flujo.read(bufer)
            if (leidos < 0) break
            salida.write(bufer, 0, leidos)
            if (salida.size() > MAXIMO_RESPUESTA) throw IOException("Respuesta demasiado grande.")
        }
        return salida.toString("UTF-8")
    }

    private companion object {
        const val MAXIMO_RESPUESTA = 64 * 1024
    }
}

/** Respuesta de la API ya interpretada. */
sealed interface RespuestaApi {
    /** `/activar` o `/validar` exitoso. El token todavía NO está verificado. */
    data class Token(val token: String) : RespuestaApi

    /** `/desactivar` exitoso. */
    data object Ok : RespuestaApi

    /** Error de negocio con el formato único de la API (`ok: false`). */
    data class ErrorNegocio(val codigo: String, val mensaje: String, val estadoHttp: Int) : RespuestaApi

    /** Sin red, timeout, 5xx, 429 o respuesta que no es de la API (por ejemplo un portal cautivo). Nunca castiga. */
    data class FallaRed(val detalle: String) : RespuestaApi
}

/**
 * Cliente de los endpoints públicos de la API de licencias (`/activar`, `/validar`, `/desactivar`, `/salud`).
 * Las llamadas bloquean el hilo: usarlas fuera del hilo principal.
 */
class ClienteApiLicencias(
    urlBase: String,
    private val transporte: TransporteHttp = TransporteHttpUrlConnection(),
) {
    private val urlBase = urlBase.trimEnd('/')

    fun activar(clave: String, deviceHash: String, nombreDispositivo: String?, versionApp: String): RespuestaApi =
        post(
            "/activar",
            JSONObject()
                .put("clave", clave)
                .put("device_hash", deviceHash)
                .put("nombre_dispositivo", nombreDispositivo ?: JSONObject.NULL)
                .put("version_app", versionApp),
            esperaToken = true,
        )

    fun validar(clave: String, deviceHash: String, versionApp: String): RespuestaApi =
        post(
            "/validar",
            JSONObject().put("clave", clave).put("device_hash", deviceHash).put("version_app", versionApp),
            esperaToken = true,
        )

    fun desactivar(clave: String, deviceHash: String): RespuestaApi =
        post("/desactivar", JSONObject().put("clave", clave).put("device_hash", deviceHash), esperaToken = false)

    /** `true` si la API responde y tiene conexión con su base de datos. Solo para diagnóstico. */
    fun salud(): Boolean = try {
        val respuesta = transporte.enviar("GET", "$urlBase/salud", null)
        respuesta.codigo == 200 && JSONObject(respuesta.cuerpo).optString("estado") == "ok"
    } catch (e: IOException) {
        false
    } catch (e: JSONException) {
        false
    }

    private fun post(ruta: String, cuerpo: JSONObject, esperaToken: Boolean): RespuestaApi {
        val respuesta = try {
            transporte.enviar("POST", urlBase + ruta, cuerpo.toString())
        } catch (e: IOException) {
            return RespuestaApi.FallaRed(e.javaClass.simpleName + (e.message?.let { ": $it" } ?: ""))
        }
        return interpretar(respuesta, esperaToken)
    }

    internal fun interpretar(respuesta: RespuestaHttp, esperaToken: Boolean): RespuestaApi {
        val codigo = respuesta.codigo
        if (codigo == 429) return RespuestaApi.FallaRed("Demasiadas solicitudes (HTTP 429).")
        if (codigo >= 500) return RespuestaApi.FallaRed("Falla del servidor (HTTP $codigo).")

        val json = try {
            JSONObject(respuesta.cuerpo)
        } catch (e: JSONException) {
            return RespuestaApi.FallaRed("Respuesta no reconocida (HTTP $codigo).")
        }

        if (codigo == 200 && json.optBoolean("ok", false)) {
            if (!esperaToken) return RespuestaApi.Ok
            val token = json.opt("token") as? String
            return if (token.isNullOrEmpty()) RespuestaApi.FallaRed("Respuesta sin token.") else RespuestaApi.Token(token)
        }

        val codigoError = json.opt("codigo") as? String
        if (codigoError.isNullOrEmpty() || json.optBoolean("ok", true)) {
            return RespuestaApi.FallaRed("Respuesta no reconocida (HTTP $codigo).")
        }
        val mensaje = (json.opt("mensaje") as? String)?.takeIf { it.isNotBlank() } ?: "La solicitud fue rechazada."
        return RespuestaApi.ErrorNegocio(codigoError, mensaje, codigo)
    }
}
