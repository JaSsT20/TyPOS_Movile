package com.typdevstudio.typos_movil.utilidades.licencia.nube

import org.json.JSONArray
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
    /**
     * `/activar`, `/validar` o `/migrar` exitoso. El token todavía NO está verificado. [versionUsuarios] es la huella
     * de los usuarios de la licencia: si cambió respecto a la última aplicada, hay que descargar la lista.
     */
    data class Token(val token: String, val versionUsuarios: String? = null) : RespuestaApi

    /**
     * `/registro` o `/registro/estado`: [estado] es `pendiente`, `rechazado` o `asignado`. Con `asignado` viene el
     * token de la licencia (todavía NO verificado).
     */
    data class Registro(val estado: String, val mensaje: String, val token: String?, val versionUsuarios: String?) : RespuestaApi

    /** `/desactivar` exitoso. */
    data object Ok : RespuestaApi

    /** Operación de usuarios exitosa: la lista completa y vigente de la licencia. */
    data class Usuarios(val lista: ListaUsuariosRemota) : RespuestaApi

    /** `/eventos/backup/lote`: cuántos se guardaron y cuántos rechazó (los rechazados no se reintentan). */
    data class LoteEventos(val guardados: Int, val rechazados: Int) : RespuestaApi

    /**
     * `/transferir/consultar`: qué pasaría al restaurar en este teléfono una copia de otro. Solo lectura.
     * [requiereDesvincular] = no hay cupo y el teléfono de origen quedaría desvinculado.
     */
    data class ConsultaTransferencia(
        val cliente: String,
        val yaRegistrado: Boolean,
        val hayCupo: Boolean,
        val requiereDesvincular: Boolean,
        val nombreOrigen: String?,
        val maxDispositivos: Int,
        val enUso: Int,
    ) : RespuestaApi

    /** `/transferir` exitoso: el token de este teléfono (todavía NO verificado) y el teléfono desvinculado, si hubo. */
    data class Transferencia(val token: String, val versionUsuarios: String?, val desvinculado: String?) : RespuestaApi

    /** Error de negocio con el formato único de la API (`ok: false`). */
    data class ErrorNegocio(val codigo: String, val mensaje: String, val estadoHttp: Int) : RespuestaApi

    /** Sin red, timeout, 5xx, 429 o respuesta que no es de la API (por ejemplo un portal cautivo). Nunca castiga. */
    data class FallaRed(val detalle: String) : RespuestaApi
}

/** Qué espera cada endpoint en una respuesta exitosa. */
enum class TipoRespuesta { TOKEN, OK, USUARIOS, REGISTRO, LOTE_EVENTOS, CONSULTA_TRANSFERENCIA, TRANSFERENCIA }

/** Datos del negocio que se registra desde la app (los mismos que salen en su factura) y su contacto. */
data class DatosRegistro(
    val nombreNegocio: String,
    val telefono: String,
    val email: String? = null,
    val rnc: String? = null,
    val direccion: String? = null,
    val slogan: String? = null,
    val contactoNombre: String? = null,
    val contactoTelefono: String? = null,
    val adminUsuario: String? = null,
    val adminNombre: String? = null,
)

/** Usuario de la licencia tal como lo guarda la API (la contraseña viene con hash). */
data class UsuarioRemoto(
    val nombreUsuario: String,
    val nombreCompleto: String,
    val rol: String,
    val hashClave: String,
    val activo: Boolean,
)

/** Respuesta de los endpoints `/usuarios/...`: todos los usuarios de la licencia y el límite del plan. */
data class ListaUsuariosRemota(
    val maxUsuarios: Int,
    val usuariosActivos: Int,
    val usuarios: List<UsuarioRemoto>,
    /** Al importar: nombres que no entraron por el límite del plan. */
    val rechazados: List<String> = emptyList(),
) {
    companion object {
        /** @throws IllegalArgumentException si el JSON no tiene la forma esperada. */
        fun desdeJson(json: JSONObject): ListaUsuariosRemota {
            try {
                val usuarios = json.getJSONArray("usuarios")
                val rechazados = json.optJSONArray("rechazados")
                return ListaUsuariosRemota(
                    maxUsuarios = json.getInt("max_usuarios"),
                    usuariosActivos = json.getInt("usuarios_activos"),
                    usuarios = (0 until usuarios.length()).map { i ->
                        val u = usuarios.getJSONObject(i)
                        UsuarioRemoto(
                            nombreUsuario = u.getString("nombre_usuario"),
                            nombreCompleto = u.getString("nombre_completo"),
                            rol = u.getString("rol"),
                            hashClave = u.getString("hash_clave"),
                            activo = u.getBoolean("activo"),
                        )
                    },
                    rechazados = if (rechazados == null) emptyList() else (0 until rechazados.length()).map { rechazados.getString(it) },
                )
            } catch (e: JSONException) {
                throw IllegalArgumentException("Lista de usuarios no reconocida.", e)
            }
        }
    }
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
            tipo = TipoRespuesta.TOKEN,
        )

    /** Pasa la licencia antigua (16 caracteres) a la nube. Responde un token, igual que [activar]. */
    fun migrar(claveAntigua: String, deviceHash: String, nombreDispositivo: String?, versionApp: String): RespuestaApi =
        post(
            "/migrar",
            JSONObject()
                .put("clave_antigua", claveAntigua)
                .put("device_hash", deviceHash)
                .put("nombre_dispositivo", nombreDispositivo ?: JSONObject.NULL)
                .put("version_app", versionApp),
            tipo = TipoRespuesta.TOKEN,
        )

    /** Registra el negocio para que el proveedor le asigne una licencia. Reenviar no duplica. */
    fun registrar(datos: DatosRegistro, deviceHash: String, nombreDispositivo: String?, versionApp: String): RespuestaApi =
        post(
            "/registro",
            JSONObject()
                .put("device_hash", deviceHash)
                .put("nombre_dispositivo", nombreDispositivo ?: JSONObject.NULL)
                .put("version_app", versionApp)
                .put("nombre_negocio", datos.nombreNegocio)
                .put("telefono", datos.telefono)
                .put("email", datos.email ?: JSONObject.NULL)
                .put("rnc", datos.rnc ?: JSONObject.NULL)
                .put("direccion", datos.direccion ?: JSONObject.NULL)
                .put("slogan", datos.slogan ?: JSONObject.NULL)
                .put("contacto_nombre", datos.contactoNombre ?: JSONObject.NULL)
                .put("contacto_telefono", datos.contactoTelefono ?: JSONObject.NULL)
                .put("admin_usuario", datos.adminUsuario ?: JSONObject.NULL)
                .put("admin_nombre", datos.adminNombre ?: JSONObject.NULL),
            tipo = TipoRespuesta.REGISTRO,
        )

    /** Cómo va el registro de este teléfono; con licencia asignada trae el token. */
    fun estadoRegistro(deviceHash: String, nombreDispositivo: String?, versionApp: String): RespuestaApi =
        post(
            "/registro/estado",
            JSONObject()
                .put("device_hash", deviceHash)
                .put("nombre_dispositivo", nombreDispositivo ?: JSONObject.NULL)
                .put("version_app", versionApp),
            tipo = TipoRespuesta.REGISTRO,
        )

    fun validar(clave: String, deviceHash: String, versionApp: String): RespuestaApi =
        post(
            "/validar",
            JSONObject().put("clave", clave).put("device_hash", deviceHash).put("version_app", versionApp),
            tipo = TipoRespuesta.TOKEN,
        )

    fun desactivar(clave: String, deviceHash: String): RespuestaApi =
        post("/desactivar", JSONObject().put("clave", clave).put("device_hash", deviceHash), tipo = TipoRespuesta.OK)

    fun listarUsuarios(clave: String, deviceHash: String): RespuestaApi =
        post("/usuarios/listar", identificacion(clave, deviceHash), TipoRespuesta.USUARIOS)

    fun crearUsuario(clave: String, deviceHash: String, usuario: UsuarioRemoto): RespuestaApi =
        post(
            "/usuarios/crear",
            identificacion(clave, deviceHash)
                .put("nombre_usuario", usuario.nombreUsuario)
                .put("nombre_completo", usuario.nombreCompleto)
                .put("rol", usuario.rol)
                .put("hash_clave", usuario.hashClave),
            TipoRespuesta.USUARIOS,
        )

    /** Solo se envían (y cambian) los campos no nulos. */
    fun actualizarUsuario(
        clave: String,
        deviceHash: String,
        nombreUsuario: String,
        nombreCompleto: String? = null,
        rol: String? = null,
        hashClave: String? = null,
        activo: Boolean? = null,
    ): RespuestaApi {
        val cuerpo = identificacion(clave, deviceHash).put("nombre_usuario", nombreUsuario)
        nombreCompleto?.let { cuerpo.put("nombre_completo", it) }
        rol?.let { cuerpo.put("rol", it) }
        hashClave?.let { cuerpo.put("hash_clave", it) }
        activo?.let { cuerpo.put("activo", it) }
        return post("/usuarios/actualizar", cuerpo, TipoRespuesta.USUARIOS)
    }

    fun importarUsuarios(clave: String, deviceHash: String, usuarios: List<UsuarioRemoto>): RespuestaApi {
        val lista = JSONArray()
        usuarios.forEach { u ->
            lista.put(
                JSONObject()
                    .put("nombre_usuario", u.nombreUsuario)
                    .put("nombre_completo", u.nombreCompleto)
                    .put("rol", u.rol)
                    .put("hash_clave", u.hashClave)
                    .put("activo", u.activo)
            )
        }
        return post("/usuarios/importar", identificacion(clave, deviceHash).put("usuarios", lista), TipoRespuesta.USUARIOS)
    }

    /** Envía hasta 50 eventos de copias de seguridad juntos (la cola guardada sin internet). */
    fun enviarEventosBackup(eventos: JSONArray): RespuestaApi =
        post("/eventos/backup/lote", JSONObject().put("eventos", eventos), TipoRespuesta.LOTE_EVENTOS)

    /** Solo lectura: qué pasaría al restaurar en este teléfono ([deviceHash]) una copia con [tokenOrigen]. */
    fun consultarTransferencia(tokenOrigen: String, deviceHash: String, idOperacion: String): RespuestaApi =
        post(
            "/transferir/consultar",
            JSONObject().put("token_origen", tokenOrigen).put("device_hash", deviceHash).put("id_operacion", idOperacion),
            TipoRespuesta.CONSULTA_TRANSFERENCIA,
        )

    /** Agrega este teléfono a la licencia de la copia; sin cupo y con [permitirDesvincular], desvincula el de origen. */
    fun transferir(
        tokenOrigen: String,
        deviceHash: String,
        nombreDispositivo: String?,
        versionApp: String,
        permitirDesvincular: Boolean,
        idOperacion: String,
    ): RespuestaApi =
        post(
            "/transferir",
            JSONObject()
                .put("token_origen", tokenOrigen)
                .put("device_hash", deviceHash)
                .put("nombre_dispositivo", nombreDispositivo ?: JSONObject.NULL)
                .put("version_app", versionApp)
                .put("permitir_desvincular", permitirDesvincular)
                .put("id_operacion", idOperacion),
            TipoRespuesta.TRANSFERENCIA,
        )

    private fun identificacion(clave: String, deviceHash: String) =
        JSONObject().put("clave", clave).put("device_hash", deviceHash)

    /** `true` si la API responde y tiene conexión con su base de datos. Solo para diagnóstico. */
    fun salud(): Boolean = try {
        val respuesta = transporte.enviar("GET", "$urlBase/salud", null)
        respuesta.codigo == 200 && JSONObject(respuesta.cuerpo).optString("estado") == "ok"
    } catch (e: IOException) {
        false
    } catch (e: JSONException) {
        false
    }

    private fun post(ruta: String, cuerpo: JSONObject, tipo: TipoRespuesta): RespuestaApi {
        val respuesta = try {
            transporte.enviar("POST", urlBase + ruta, cuerpo.toString())
        } catch (e: IOException) {
            return RespuestaApi.FallaRed(e.javaClass.simpleName + (e.message?.let { ": $it" } ?: ""))
        }
        return interpretar(respuesta, tipo)
    }

    internal fun interpretar(respuesta: RespuestaHttp, tipo: TipoRespuesta): RespuestaApi {
        val codigo = respuesta.codigo
        // 429 es "espere un momento" salvo el límite de transferencias, que es una regla de negocio (llamar a soporte).
        if (codigo == 429 && !respuesta.cuerpo.contains("\"$CODIGO_LIMITE_TRANSFERENCIAS\"")) {
            return RespuestaApi.FallaRed("Demasiadas solicitudes (HTTP 429).")
        }
        if (codigo >= 500) return RespuestaApi.FallaRed("Falla del servidor (HTTP $codigo).")

        val json = try {
            JSONObject(respuesta.cuerpo)
        } catch (e: JSONException) {
            return RespuestaApi.FallaRed("Respuesta no reconocida (HTTP $codigo).")
        }

        if (codigo == 200 && json.optBoolean("ok", false)) {
            return when (tipo) {
                TipoRespuesta.OK -> RespuestaApi.Ok
                TipoRespuesta.TOKEN -> {
                    val token = json.opt("token") as? String
                    if (token.isNullOrEmpty()) RespuestaApi.FallaRed("Respuesta sin token.")
                    else RespuestaApi.Token(token, json.opt("version_usuarios") as? String)
                }
                TipoRespuesta.REGISTRO -> {
                    val estado = json.opt("estado") as? String
                    val token = json.opt("token") as? String
                    when {
                        estado !in ESTADOS_REGISTRO -> RespuestaApi.FallaRed("Estado de registro no reconocido.")
                        estado == "asignado" && token.isNullOrEmpty() -> RespuestaApi.FallaRed("Respuesta sin token.")
                        else -> RespuestaApi.Registro(
                            estado!!, (json.opt("mensaje") as? String).orEmpty(), token, json.opt("version_usuarios") as? String
                        )
                    }
                }
                TipoRespuesta.USUARIOS -> try {
                    RespuestaApi.Usuarios(ListaUsuariosRemota.desdeJson(json))
                } catch (e: IllegalArgumentException) {
                    RespuestaApi.FallaRed("Lista de usuarios no reconocida.")
                }
                TipoRespuesta.LOTE_EVENTOS ->
                    RespuestaApi.LoteEventos(json.optInt("guardados"), json.optJSONArray("rechazados")?.length() ?: 0)
                TipoRespuesta.CONSULTA_TRANSFERENCIA -> try {
                    RespuestaApi.ConsultaTransferencia(
                        cliente = json.optString("cliente"),
                        yaRegistrado = json.getBoolean("ya_registrado"),
                        hayCupo = json.getBoolean("hay_cupo"),
                        requiereDesvincular = json.getBoolean("requiere_desvincular"),
                        nombreOrigen = json.optJSONObject("dispositivo_origen")?.let { o ->
                            if (o.isNull("nombre")) null else o.optString("nombre").takeIf { it.isNotBlank() }
                        },
                        maxDispositivos = json.getInt("max_dispositivos"),
                        enUso = json.getInt("en_uso"),
                    )
                } catch (e: JSONException) {
                    RespuestaApi.FallaRed("Consulta de transferencia no reconocida.")
                }
                TipoRespuesta.TRANSFERENCIA -> {
                    val token = json.opt("token") as? String
                    if (token.isNullOrEmpty()) RespuestaApi.FallaRed("Respuesta sin token.")
                    else RespuestaApi.Transferencia(
                        token,
                        json.opt("version_usuarios") as? String,
                        json.optJSONObject("dispositivo_desvinculado")?.let { o ->
                            if (o.isNull("nombre")) "otro teléfono" else o.optString("nombre").ifBlank { "otro teléfono" }
                        },
                    )
                }
            }
        }

        val codigoError = json.opt("codigo") as? String
        if (codigoError.isNullOrEmpty() || json.optBoolean("ok", true)) {
            return RespuestaApi.FallaRed("Respuesta no reconocida (HTTP $codigo).")
        }
        val mensaje = (json.opt("mensaje") as? String)?.takeIf { it.isNotBlank() } ?: "La solicitud fue rechazada."
        return RespuestaApi.ErrorNegocio(codigoError, mensaje, codigo)
    }

    private companion object {
        val ESTADOS_REGISTRO = setOf("pendiente", "rechazado", "asignado")
        const val CODIGO_LIMITE_TRANSFERENCIAS = "LIMITE_TRANSFERENCIAS"
    }
}
