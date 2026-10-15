package com.typdevstudio.typos_movil.utilidades.licencia.nube

import android.content.Context

/** Lo guardado de la licencia nueva. Los campos ausentes o ilegibles llegan como `null`. */
data class DatosLicenciaGuardados(
    val clave: String?,
    val token: String?,
    val ultimaValidacionMs: Long?,
    /** Bloqueo comunicado por el servidor: se respeta aunque el token siga vigente. */
    val bloqueoServidor: MotivoRestriccion?,
    /** La hora más alta vista por la app, para la defensa contra reloj atrasado. */
    val ultimaFechaVistaMs: Long?,
    /** Mensaje de la API que acompañó al bloqueo, para mostrarlo tal cual al usuario. */
    val mensajeBloqueoServidor: String? = null,
    /** Registro del negocio esperando licencia: `pendiente` o `rechazado` (`null` si no hay registro). */
    val estadoRegistro: String? = null,
    /** Mensaje del servidor sobre el registro (el motivo, si lo rechazaron). */
    val mensajeRegistro: String? = null,
    /** Huella de los usuarios de la licencia que ya se aplicó en el teléfono. */
    val versionUsuarios: String? = null,
)

/**
 * Estado persistente de la licencia nueva. Vive fuera de Room a propósito: así no lo toca una migración
 * de la base ni lo revierte la restauración de un respaldo.
 */
class AlmacenLicencia(private val archivo: ArchivoSeguro) {

    @Synchronized
    fun leer() = DatosLicenciaGuardados(
        clave = archivo.leer(CLAVE),
        token = archivo.leer(TOKEN),
        ultimaValidacionMs = archivo.leer(ULTIMA_VALIDACION)?.toLongOrNull(),
        bloqueoServidor = archivo.leer(BLOQUEO_SERVIDOR)?.let { valor ->
            MotivoRestriccion.entries.firstOrNull { it.name == valor }
        },
        ultimaFechaVistaMs = archivo.leer(ULTIMA_FECHA_VISTA)?.toLongOrNull(),
        mensajeBloqueoServidor = archivo.leer(MENSAJE_BLOQUEO),
        estadoRegistro = archivo.leer(ESTADO_REGISTRO),
        mensajeRegistro = archivo.leer(MENSAJE_REGISTRO),
        versionUsuarios = archivo.leer(VERSION_USUARIOS),
    )

    /**
     * Guarda el resultado de una activación o validación exitosa (el token ya debe estar verificado).
     * Quita cualquier bloqueo previo y reinicia la defensa del reloj con la hora actual.
     */
    @Synchronized
    fun guardarValidacionExitosa(clave: String, token: String, ahoraMs: Long) {
        archivo.escribir(
            mapOf(
                CLAVE to clave,
                TOKEN to token,
                ULTIMA_VALIDACION to ahoraMs.toString(),
                BLOQUEO_SERVIDOR to null,
                MENSAJE_BLOQUEO to null,
                ULTIMA_FECHA_VISTA to ahoraMs.toString(),
                // Con licencia, el registro que esperaba ya no aplica.
                ESTADO_REGISTRO to null,
                MENSAJE_REGISTRO to null,
            )
        )
    }

    @Synchronized
    fun guardarBloqueoServidor(motivo: MotivoRestriccion, mensaje: String? = null) {
        archivo.escribir(mapOf(BLOQUEO_SERVIDOR to motivo.name, MENSAJE_BLOQUEO to mensaje))
    }

    /** Actualiza la hora más alta vista. Nunca la hace retroceder. */
    @Synchronized
    fun registrarFechaVista(ahoraMs: Long) {
        val anterior = archivo.leer(ULTIMA_FECHA_VISTA)?.toLongOrNull()
        val nueva = EvaluadorEstadoLicencia.nuevaUltimaFechaVista(anterior, ahoraMs)
        if (nueva != anterior) archivo.escribir(mapOf(ULTIMA_FECHA_VISTA to nueva.toString()))
    }

    /**
     * Olvida la licencia de este teléfono (tras desactivarlo). Conserva el `device_hash`
     * y la última fecha vista, que son del teléfono y no de la licencia.
     */
    @Synchronized
    fun borrarLicencia() {
        archivo.escribir(
            mapOf(
                CLAVE to null, TOKEN to null, ULTIMA_VALIDACION to null, BLOQUEO_SERVIDOR to null, MENSAJE_BLOQUEO to null,
                VERSION_USUARIOS to null,
            )
        )
    }

    /** Registro del negocio: `pendiente` o `rechazado` con su mensaje; `null` lo borra. */
    @Synchronized
    fun guardarRegistro(estado: String?, mensaje: String? = null) {
        archivo.escribir(mapOf(ESTADO_REGISTRO to estado, MENSAJE_REGISTRO to mensaje))
    }

    /** Huella de usuarios ya aplicada (tras sincronizar con éxito). */
    @Synchronized
    fun guardarVersionUsuarios(version: String?) {
        archivo.escribir(mapOf(VERSION_USUARIOS to version))
    }

    @Synchronized
    fun leerDeviceHash(): String? = archivo.leer(DEVICE_HASH)

    @Synchronized
    fun guardarDeviceHash(deviceHash: String) {
        archivo.escribir(mapOf(DEVICE_HASH to deviceHash))
    }

    companion object {
        private const val CLAVE = "clave"
        private const val TOKEN = "token"
        private const val ULTIMA_VALIDACION = "ultima_validacion"
        private const val BLOQUEO_SERVIDOR = "bloqueo_servidor"
        private const val MENSAJE_BLOQUEO = "mensaje_bloqueo"
        private const val ULTIMA_FECHA_VISTA = "ultima_fecha_vista"
        private const val DEVICE_HASH = "device_hash"
        private const val ESTADO_REGISTRO = "estado_registro"
        private const val MENSAJE_REGISTRO = "mensaje_registro"
        private const val VERSION_USUARIOS = "version_usuarios"

        fun crear(contexto: Context) = AlmacenLicencia(PreferenciasCifradas(contexto))
    }
}
