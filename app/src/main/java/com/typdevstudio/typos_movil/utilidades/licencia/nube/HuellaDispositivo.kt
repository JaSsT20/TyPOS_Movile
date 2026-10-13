package com.typdevstudio.typos_movil.utilidades.licencia.nube

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaDrm
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

/**
 * `device_hash` de la licencia nueva: SHA-256 en hexadecimal (64 caracteres), nunca el identificador en crudo.
 *
 * Origen, en orden:
 * 1. ID único de Widevine (el mismo que usa la licencia antigua). Sobrevive a reinstalar la app y,
 *    normalmente, a un restablecimiento de fábrica. Sus primeros 16 caracteres son el serial antiguo
 *    (`IdDispositivo`), así que en el panel se reconoce el teléfono de un cliente actual.
 * 2. `ANDROID_ID`, si el teléfono no tiene Widevine. Cambia tras un restablecimiento de fábrica.
 * 3. Un valor aleatorio, si no hay ninguno de los dos.
 *
 * El primer valor calculado se guarda en [AlmacenLicencia] y se reutiliza: así un fallo pasajero
 * leyendo Widevine no cambia el hash de un teléfono ya activado.
 */
object HuellaDispositivo {

    private val WIDEVINE_UUID = UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed")

    /** `ANDROID_ID` defectuoso conocido de algunos teléfonos Android 2.2, repetido en muchos equipos. */
    private const val ANDROID_ID_DEFECTUOSO = "9774d56d682e549c"

    @Synchronized
    fun obtener(contexto: Context, almacen: AlmacenLicencia): String {
        almacen.leerDeviceHash()?.let { return it }
        val hash = calcular(leerIdWidevine(), leerAndroidId(contexto))
            ?: sha256Hex("aleatorio:${UUID.randomUUID()}".toByteArray(Charsets.UTF_8))
        almacen.guardarDeviceHash(hash)
        return hash
    }

    /** Parte pura del cálculo, o `null` si no hay ningún identificador utilizable. */
    fun calcular(idWidevine: ByteArray?, androidId: String?): String? = when {
        idWidevine != null && idWidevine.isNotEmpty() -> sha256Hex(idWidevine)
        androidId != null && androidId.isNotBlank() && !androidId.equals(ANDROID_ID_DEFECTUOSO, ignoreCase = true) ->
            sha256Hex("android_id:${androidId.trim().lowercase()}".toByteArray(Charsets.UTF_8))
        else -> null
    }

    private fun sha256Hex(datos: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(datos).joinToString("") { "%02x".format(Locale.ROOT, it) }

    private fun leerIdWidevine(): ByteArray? {
        var mediaDrm: MediaDrm? = null
        return try {
            mediaDrm = MediaDrm(WIDEVINE_UUID)
            mediaDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)
        } catch (e: Exception) {
            null
        } finally {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) mediaDrm?.close() else @Suppress("DEPRECATION") mediaDrm?.release()
            } catch (ignorado: Exception) {
            }
        }
    }

    @SuppressLint("HardwareIds") // Se usa solo para la licencia y siempre en hash.
    private fun leerAndroidId(contexto: Context): String? =
        try {
            Settings.Secure.getString(contexto.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            null
        }
}
