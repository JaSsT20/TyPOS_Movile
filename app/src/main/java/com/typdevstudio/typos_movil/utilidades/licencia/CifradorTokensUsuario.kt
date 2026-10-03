package com.typdevstudio.typos_movil.utilidades.licencia

import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Modelo de datos con la información descifrada de un token de invitación/activación de usuario.
 */
data class InfoTokenUsuario(
    val esValido: Boolean,
    val nombreUsuario: String = "",
    val nombreCompleto: String = "",
    val rol: String = "CAJERO", // "CAJERO" o "ADMINISTRADOR"
    val timestampEmision: Long = 0L,
    val horasValidez: Int = 0,
    val fechaExpiracion: Long? = null,
    val estaExpirado: Boolean = false,
    val mensajeError: String = ""
)

/**
 * Motor Criptográfico AES-128-CBC para validación y descifrado de códigos de activación de usuarios.
 * 100% interoperable con el generador en C# (.NET 10).
 */
object CifradorTokensUsuario {

    // Clave secreta de 128 bits: "TyPOS_User_Key26" (16 bytes)
    private val CLAVE_SECRETA = byteArrayOf(
        0x54, 0x79, 0x50, 0x4F, 0x53, 0x5F, 0x55, 0x73, 0x65, 0x72, 0x5F, 0x4B, 0x65, 0x79, 0x32, 0x36
    )

    private const val PREFIJO_TOKEN = "USR"

    /**
     * Valida y descifra un token de activación de usuario.
     */
    fun validarToken(token: String, timestampActualMs: Long = System.currentTimeMillis()): InfoTokenUsuario {
        try {
            if (token.isBlank()) {
                return InfoTokenUsuario(esValido = false, mensajeError = "El código está vacío")
            }

            var limpia = token.replace("-", "").replace(" ", "").trim().uppercase(Locale.ROOT)
            if (limpia.startsWith(PREFIJO_TOKEN)) {
                limpia = limpia.substring(PREFIJO_TOKEN.length)
            }

            if (limpia.length < 32 || limpia.length % 2 != 0) {
                return InfoTokenUsuario(esValido = false, mensajeError = "Formato de código inválido")
            }

            val cipherWithIv = hexToByteArray(limpia)
            if (cipherWithIv.size < 32) {
                return InfoTokenUsuario(esValido = false, mensajeError = "Código corrupto o incompleto")
            }

            val iv = ByteArray(16)
            System.arraycopy(cipherWithIv, 0, iv, 0, 16)

            val cipherLength = cipherWithIv.size - 16
            val cipherOnly = ByteArray(cipherLength)
            System.arraycopy(cipherWithIv, 16, cipherOnly, 0, cipherLength)

            val keySpec = SecretKeySpec(CLAVE_SECRETA, "AES")
            val ivSpec = IvParameterSpec(iv)
            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)

            val decryptedBytes = cipher.doFinal(cipherOnly)
            val jsonStr = String(decryptedBytes, Charsets.UTF_8)
            val json = JSONObject(jsonStr)

            val username = json.optString("u", "").trim().lowercase(Locale.ROOT)
            val nombreCompleto = json.optString("n", username)
            val rol = json.optString("r", "CAJERO").uppercase(Locale.ROOT)
            val timestampSec = json.optLong("t", 0L)
            val horasValidez = json.optInt("h", 0)

            if (username.isBlank()) {
                return InfoTokenUsuario(esValido = false, mensajeError = "Estructura del token no reconocida")
            }

            val fechaExpiracionMs: Long? = if (horasValidez > 0) (timestampSec + horasValidez * 3600L) * 1000L else null
            val expirado = fechaExpiracionMs != null && timestampActualMs > fechaExpiracionMs

            if (expirado) {
                val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
                val expTexto = sdf.format(Date(fechaExpiracionMs!!))
                return InfoTokenUsuario(
                    esValido = false,
                    nombreUsuario = username,
                    nombreCompleto = nombreCompleto,
                    rol = rol,
                    timestampEmision = timestampSec,
                    horasValidez = horasValidez,
                    fechaExpiracion = fechaExpiracionMs,
                    estaExpirado = true,
                    mensajeError = "El código de activación expiró el $expTexto"
                )
            }

            return InfoTokenUsuario(
                esValido = true,
                nombreUsuario = username,
                nombreCompleto = nombreCompleto,
                rol = if (rol == "ADMINISTRADOR") "ADMINISTRADOR" else "CAJERO",
                timestampEmision = timestampSec,
                horasValidez = horasValidez,
                fechaExpiracion = fechaExpiracionMs,
                estaExpirado = false
            )
        } catch (e: Exception) {
            return InfoTokenUsuario(
                esValido = false,
                mensajeError = "Código de activación incorrecto o alterado"
            )
        }
    }

    private fun hexToByteArray(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
