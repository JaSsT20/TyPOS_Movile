package com.typdevstudio.typos_movil.utilidades.licencia

import android.content.Context
import android.media.MediaDrm
import android.os.Build
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

/**
 * Proveedor del Identificador de Hardware / Serial de Dispositivo único de TyPOS Móvil.
 * 
 * - Determinista e Invariable: No cambia entre compilaciones Debug y Release.
 * - Persistente: No cambia al desinstalar y reinstalar la aplicación.
 * - Seguro: No requiere permisos invasivos del sistema.
 */
object IdDispositivo {

    private val WIDEVINE_UUID = UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed")

    /**
     * Obtiene el identificador único del hardware/dispositivo formateado en 4 bloques de 4 caracteres
     * (Ejemplo: "9774-D56D-682E-549C").
     */
    fun obtenerSerialDispositivo(contexto: Context? = null): String {
        val rawId = obtenerRawId(contexto)
        return formatearSerial(rawId)
    }

    /**
     * Obtiene el identificador sin guiones ni espacios en mayúsculas (16 caracteres hexadecimales).
     */
    fun obtenerRawId(contexto: Context? = null): String {
        // 1. Intentar obtener el ID único de hardware de Widevine DRM (Invariable por firma o reinstalación)
        val drmId = obtenerDrmHardwareId()
        if (!drmId.isNullOrBlank()) {
            return drmId
        }

        // 2. Fallback determinista basado en huella de hardware del dispositivo
        return obtenerHuellaHardwareFallback()
    }

    /**
     * Obtiene el identificador único de hardware a través del subsistema DRM Widevine de Android.
     * Es inmutable, no depende de la clave de firma (Debug/Release) y no cambia al desinstalar.
     */
    private fun obtenerDrmHardwareId(): String? {
        var mediaDrm: MediaDrm? = null
        try {
            mediaDrm = MediaDrm(WIDEVINE_UUID)
            val idBytes = mediaDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)
            if (idBytes.isNotEmpty()) {
                val sha = MessageDigest.getInstance("SHA-256").digest(idBytes)
                return bytesToHex16(sha)
            }
        } catch (e: Throwable) {
            // Continuar con fallback en caso de error
        } finally {
            try {
                mediaDrm?.close()
            } catch (ignored: Throwable) {}
        }
        return null
    }

    /**
     * Genera un hash SHA-256 determinista a partir de los atributos inmutables de hardware del equipo.
     */
    private fun obtenerHuellaHardwareFallback(): String {
        val componentes = StringBuilder().apply {
            append(Build.BOARD).append("|")
            append(Build.BRAND).append("|")
            append(Build.DEVICE).append("|")
            append(Build.HARDWARE).append("|")
            append(Build.MANUFACTURER).append("|")
            append(Build.MODEL).append("|")
            append(Build.PRODUCT).append("|")
            append(Build.BOOTLOADER).append("|")
            append(Build.SUPPORTED_ABIS.joinToString(","))
        }.toString()

        val sha = MessageDigest.getInstance("SHA-256").digest(componentes.toByteArray(Charsets.UTF_8))
        return bytesToHex16(sha)
    }

    private fun bytesToHex16(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (i in 0 until 8.coerceAtMost(bytes.size)) {
            sb.append(String.format(Locale.ROOT, "%02X", bytes[i]))
        }
        return sb.toString().padStart(16, '0').take(16).uppercase(Locale.ROOT)
    }

    /**
     * Formatea una cadena hexadecimal de 16 caracteres en el estándar XXXX-XXXX-XXXX-XXXX.
     */
    fun formatearSerial(raw: String): String {
        val limpia = raw.replace("-", "").replace(" ", "").trim().uppercase(Locale.ROOT).padStart(16, '0').take(16)
        return "${limpia.substring(0, 4)}-${limpia.substring(4, 8)}-${limpia.substring(8, 12)}-${limpia.substring(12, 16)}"
    }
}
