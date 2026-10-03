package com.typdevstudio.typos_movil.utilidades.licencia

import android.content.Context
import android.provider.Settings
import java.util.Locale
import java.util.UUID

/**
 * Proveedor del Identificador de Hardware / Serial de Dispositivo único de TyPOS Móvil.
 * Seguro, no requiere permisos invasivos y es persistente.
 */
object IdDispositivo {

    private const val PREFS_NAME = "typos_device_identity"
    private const val KEY_FALLBACK_ID = "fallback_device_id"

    /**
     * Obtiene el identificador único del hardware/dispositivo formateado en 4 bloques de 4 caracteres
     * (Ejemplo: "9774-D56D-682E-549C").
     */
    fun obtenerSerialDispositivo(contexto: Context): String {
        val rawId = obtenerRawId(contexto)
        return formatearSerial(rawId)
    }

    /**
     * Obtiene el identificador sin guiones ni espacios en mayúsculas (16 caracteres).
     */
    fun obtenerRawId(contexto: Context): String {
        return try {
            val androidId = Settings.Secure.getString(
                contexto.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c" && androidId.length >= 8) {
                androidId.padStart(16, '0').takeLast(16).uppercase(Locale.ROOT)
            } else {
                obtenerIdPersistenteFallback(contexto)
            }
        } catch (e: Exception) {
            obtenerIdPersistenteFallback(contexto)
        }
    }

    private fun obtenerIdPersistenteFallback(contexto: Context): String {
        val prefs = contexto.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var fallback = prefs.getString(KEY_FALLBACK_ID, null)
        if (fallback.isNullOrBlank()) {
            val uuid = UUID.randomUUID().toString().replace("-", "").take(16).uppercase(Locale.ROOT)
            prefs.edit().putString(KEY_FALLBACK_ID, uuid).apply()
            fallback = uuid
        }
        return fallback
    }

    fun formatearSerial(raw: String): String {
        val limpia = raw.replace("-", "").replace(" ", "").trim().uppercase(Locale.ROOT).padStart(16, '0').take(16)
        return "${limpia.substring(0, 4)}-${limpia.substring(4, 8)}-${limpia.substring(8, 12)}-${limpia.substring(12, 16)}"
    }
}
