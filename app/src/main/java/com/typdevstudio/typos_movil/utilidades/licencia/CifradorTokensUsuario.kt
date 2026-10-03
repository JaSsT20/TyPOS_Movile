package com.typdevstudio.typos_movil.utilidades.licencia

import java.util.Locale

/**
 * Información decodificada de un token corto de activación de usuario.
 */
data class InfoTokenUsuario(
    val esValido: Boolean,
    val nombreUsuario: String = "",
    val rol: String = "CAJERO",
    val fechaEmisionMs: Long = 0L,
    val horasValidez: Int = 0,
    val estaExpirado: Boolean = false,
    val mensajeError: String = ""
)

/**
 * Cifrador simétrico ultracompacto para Tokens de Activación de Usuario en TyPOS Móvil.
 * Formato: USR-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX (24 caracteres hexadecimales en 6 bloques).
 * 100% compatible con TyPOS_Licenciador (C#).
 */
object CifradorTokensUsuario {

    private val CLAVE_USUARIOS = longArrayOf(
        0x4E7A91B2L,
        0x83DF10CAL,
        0x5C2B7E9FL,
        0x19A4D388L
    )

    private const val CONSTANTE_MAGICA = 0x55L
    // Epoch base: 2025-01-01 00:00:00 UTC
    private const val FECHA_BASE_EPOCH_MS = 1735689600000L
    private const val MS_POR_DIA = 86400000L

    /**
     * Valida y descifra un token corto de activación de usuario.
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

            if (limpia.length != 24) {
                return InfoTokenUsuario(
                    esValido = false,
                    mensajeError = "El código debe contener 24 caracteres (formato: USR-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX)"
                )
            }

            var v0 = limpia.substring(0, 8).toLongOrNull(16)
                ?: return InfoTokenUsuario(esValido = false, mensajeError = "Código inválido")
            var v1 = limpia.substring(8, 16).toLongOrNull(16)
                ?: return InfoTokenUsuario(esValido = false, mensajeError = "Código inválido")
            var v2 = limpia.substring(16, 24).toLongOrNull(16)
                ?: return InfoTokenUsuario(esValido = false, mensajeError = "Código inválido")

            // Descifrado de 3 palabras Feistel (32 rondas inversas)
            for (r in 31 downTo 0) {
                val k = CLAVE_USUARIOS[r and 3]
                val f2 = (((v0 shl 4) xor (v1 ushr 5)) + k) and 0xFFFFFFFFL
                v2 = (v2 - f2) and 0xFFFFFFFFL

                val f1 = (((v2 shl 4) xor (v0 ushr 5)) + k) and 0xFFFFFFFFL
                v1 = (v1 - f1) and 0xFFFFFFFFL

                val f0 = (((v1 shl 4) xor (v2 ushr 5)) + k) and 0xFFFFFFFFL
                v0 = (v0 - f0) and 0xFFFFFFFFL
            }

            // Desempaquetar 12 bytes
            val bytes = ByteArray(12)
            bytes[0] = ((v0 ushr 24) and 0xFFL).toByte()
            bytes[1] = ((v0 ushr 16) and 0xFFL).toByte()
            bytes[2] = ((v0 ushr 8) and 0xFFL).toByte()
            bytes[3] = (v0 and 0xFFL).toByte()

            bytes[4] = ((v1 ushr 24) and 0xFFL).toByte()
            bytes[5] = ((v1 ushr 16) and 0xFFL).toByte()
            bytes[6] = ((v1 ushr 8) and 0xFFL).toByte()
            bytes[7] = (v1 and 0xFFL).toByte()

            bytes[8] = ((v2 ushr 24) and 0xFFL).toByte()
            bytes[9] = ((v2 ushr 16) and 0xFFL).toByte()
            bytes[10] = ((v2 ushr 8) and 0xFFL).toByte()
            bytes[11] = (v2 and 0xFFL).toByte()

            val usernameBytes = bytes.copyOfRange(0, 8)
            val username = String(usernameBytes, Charsets.US_ASCII).trimEnd { it == '\u0000' || it == ' ' }
            val byteRolVigencia = bytes[8].toInt() and 0xFF
            val byteEpochHi = bytes[9].toInt() and 0xFF
            val byteEpochLo = bytes[10].toInt() and 0xFF
            val checksumLeido = bytes[11].toInt() and 0xFF

            val checksumEsperado = calcularChecksum(v0, v1, bytes[8], bytes[9], bytes[10])
            if (checksumLeido != checksumEsperado || username.isBlank()) {
                return InfoTokenUsuario(
                    esValido = false,
                    mensajeError = "Código de usuario inválido o corrupto"
                )
            }

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
                    1 -> 2L   // 24 horas -> 2 días de validez segura contra diferencia horaria
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

    private fun calcularChecksum(v0: Long, v1: Long, b8: Byte, b9: Byte, b10: Byte): Int {
        var hash = (v0 xor v1 xor ((b8.toLong() and 0xFFL) shl 16) xor ((b9.toLong() and 0xFFL) shl 8) xor (b10.toLong() and 0xFFL)) xor CONSTANTE_MAGICA
        hash = (hash xor (hash ushr 16) xor (hash ushr 8)) and 0xFFL
        return hash.toInt()
    }
}
