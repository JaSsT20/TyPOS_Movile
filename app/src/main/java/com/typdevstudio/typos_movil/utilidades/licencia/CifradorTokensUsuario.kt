package com.typdevstudio.typos_movil.utilidades.licencia

import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Información decodificada de un token de activación de usuario.
 */
data class InfoTokenUsuario(
    val esValido: Boolean,
    val nombreUsuario: String = "",
    val nombreCompleto: String = "",
    val rol: String = "CAJERO",
    val fechaEmisionMs: Long = 0L,
    val horasValidez: Int = 0,
    val estaExpirado: Boolean = false,
    val mensajeError: String = ""
)

/**
 * Cifrador seguro AES-128 para Tokens de Activación de Usuario en TyPOS Móvil.
 * 100% compatible con TyPOS_Licenciador (C#).
 */
object CifradorTokensUsuario {

    private val CLAVE_AES = byteArrayOf(
        0x54, 0x79, 0x50, 0x4F, 0x53, 0x5F, 0x55, 0x73, 0x65, 0x72, 0x5F, 0x4B, 0x65, 0x79, 0x32, 0x36
    )

    private const val CONSTANTE_MAGICA = 0x55
    // Epoch base: 2025-01-01 00:00:00 UTC
    private const val FECHA_BASE_EPOCH_MS = 1735689600000L
    private const val MS_POR_DIA = 86400000L

    /**
     * Valida y descifra un token de activación de usuario.
     */
    fun validarToken(token: String, fechaActualMs: Long = System.currentTimeMillis()): InfoTokenUsuario {
        try {
            if (token.isBlank()) {
                return InfoTokenUsuario(esValido = false, mensajeError = "El código no puede estar vacío")
            }

            var limpia = token.trim().uppercase(Locale.ROOT)
            if (limpia.startsWith("USR-") || limpia.startsWith("USR")) {
                limpia = limpia.removePrefix("USR-").removePrefix("USR")
            }
            limpia = limpia.replace("-", "").replace(" ", "").trim()

            if (limpia.length != 64) {
                return InfoTokenUsuario(
                    esValido = false,
                    mensajeError = "El código de invitación debe contener 64 caracteres"
                )
            }

            val bytesCifrados = hexStringToByteArray(limpia)
            val buffer = descifrarAes(bytesCifrados)

            val checksumLeido = buffer[31].toInt() and 0xFF
            val checksumEsperado = calcularChecksum(buffer)

            if (checksumLeido != checksumEsperado) {
                return InfoTokenUsuario(
                    esValido = false,
                    mensajeError = "Código de usuario inválido o corrupto"
                )
            }

            val userBytes = buffer.copyOfRange(0, 8)
            val username = String(userBytes, Charsets.UTF_8).trimEnd { it == '\u0000' || it == ' ' }

            val nomBytes = buffer.copyOfRange(8, 24)
            val nombreCompleto = String(nomBytes, Charsets.UTF_8).trimEnd { it == '\u0000' || it == ' ' }

            val byteRolVigencia = buffer[24].toInt() and 0xFF
            val byteEpochHi = buffer[25].toInt() and 0xFF
            val byteEpochLo = buffer[26].toInt() and 0xFF

            val rolVal = (byteRolVigencia ushr 4) and 0x0F
            val vigenciaPreset = byteRolVigencia and 0x0F
            val epochDays = ((byteEpochHi shl 8) or byteEpochLo).toLong()

            val rol = if (rolVal == 1) "ADMINISTRADOR" else "CAJERO"
            val fechaEmisionMs = FECHA_BASE_EPOCH_MS + (epochDays * MS_POR_DIA)

            val horasValidez = when (vigenciaPreset) {
                1 -> 24
                2 -> 48
                3 -> 24 * 7
                4 -> 24 * 30
                else -> 0
            }

            var estaExpirado = false
            if (vigenciaPreset > 0) {
                val diasValidez = when (vigenciaPreset) {
                    1 -> 2L   // 24 horas -> 2 días de validez segura
                    2 -> 3L   // 48 horas -> 3 días
                    3 -> 8L   // 7 días -> 8 días
                    4 -> 31L  // 30 días -> 31 días
                    else -> 0L
                }
                val fechaExpiracionMs = fechaEmisionMs + (diasValidez * MS_POR_DIA)
                if (fechaActualMs > fechaExpiracionMs) {
                    estaExpirado = true
                }
            }

            return InfoTokenUsuario(
                esValido = !estaExpirado,
                nombreUsuario = username.lowercase(Locale.ROOT),
                nombreCompleto = if (nombreCompleto.isBlank()) username else nombreCompleto,
                rol = rol,
                fechaEmisionMs = fechaEmisionMs,
                horasValidez = horasValidez,
                estaExpirado = estaExpirado,
                mensajeError = if (estaExpirado) "Este código de invitación ha expirado" else ""
            )
        } catch (e: Exception) {
            return InfoTokenUsuario(esValido = false, mensajeError = "Error al descifrar el código: ${e.localizedMessage}")
        }
    }

    private fun calcularChecksum(buffer: ByteArray): Int {
        var hash = 0x811C9DC5L
        for (i in 0 until 31) {
            hash = hash xor (buffer[i].toLong() and 0xFFL)
            hash = (hash * 0x01000193L) and 0xFFFFFFFFL
        }
        hash = hash xor CONSTANTE_MAGICA.toLong()
        return (hash and 0xFFL).toInt()
    }

    private fun descifrarAes(data: ByteArray): ByteArray {
        val keySpec = SecretKeySpec(CLAVE_AES, "AES")
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keySpec)
        return cipher.doFinal(data)
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
