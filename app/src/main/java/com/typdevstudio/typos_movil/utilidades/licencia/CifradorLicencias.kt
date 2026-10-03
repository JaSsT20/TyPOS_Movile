package com.typdevstudio.typos_movil.utilidades.licencia

import java.util.Locale

/**
 * Resultado de la validación y decodificación de una clave de licencia.
 */
data class InfoLicencia(
    val esValida: Boolean,
    val fechaEmision: Long = 0L,
    val diasValidez: Int = 0,
    val fechaVencimiento: Long = 0L,
    val diasRestantes: Int = 0,
    val estaVencida: Boolean = false,
    val mensajeError: String = ""
)

/**
 * Motor criptográfico simétrico XTEA (128-bit) para TyPOS Móvil.
 * 100% compatible con el generador de licencias de escritorio en C#.
 */
object CifradorLicencias {

    // Clave Maestra de 128 bits idéntica al generador en C#
    private val CLAVE_MAESTRA = longArrayOf(
        0xA58F12D3L,
        0x7C4E90B1L,
        0x32DF5688L,
        0xFE89A410L
    )

    private const val CONSTANTE_MAGICA = 0x5459L // 'TY'
    // Epoch base: 2026-01-01 00:00:00 UTC (1767225600000 ms)
    private const val FECHA_BASE_EPOCH_MS = 1767225600000L
    private const val MS_POR_DIA = 86400000L

    /**
     * Descifra y valida una clave de 16 caracteres (ej: "A7B2-9F1C-4E80-D35A").
     */
    fun validarClaveLicencia(clave: String, fechaActualMs: Long = System.currentTimeMillis()): InfoLicencia {
        try {
            if (clave.isBlank()) {
                return InfoLicencia(esValida = false, mensajeError = "La clave no puede estar vacía")
            }

            val limpia = clave.replace("-", "").replace(" ", "").trim().uppercase(Locale.ROOT)
            if (limpia.length != 16) {
                return InfoLicencia(esValida = false, mensajeError = "La clave debe contener exactamente 16 caracteres")
            }

            val v0Str = limpia.substring(0, 8)
            val v1Str = limpia.substring(8, 16)

            var v0 = v0Str.toLongOrNull(16) ?: return InfoLicencia(esValida = false, mensajeError = "Formato de clave inválido")
            var v1 = v1Str.toLongOrNull(16) ?: return InfoLicencia(esValida = false, mensajeError = "Formato de clave inválido")

            // Descifrado XTEA (32 ciclos / 64 rondas)
            val delta = 0x9E3779B9L
            var sum = 0xC6EF3720L // (delta * 32) & 0xFFFFFFFFL

            for (i in 0 until 32) {
                val shift1 = (((v0 shl 4) xor (v0 ushr 5)) + v0) and 0xFFFFFFFFL
                val key1 = (sum + CLAVE_MAESTRA[((sum ushr 11) and 3).toInt()]) and 0xFFFFFFFFL
                v1 = (v1 - (shift1 xor key1)) and 0xFFFFFFFFL

                sum = (sum - delta) and 0xFFFFFFFFL

                val shift0 = (((v1 shl 4) xor (v1 ushr 5)) + v1) and 0xFFFFFFFFL
                val key0 = (sum + CLAVE_MAESTRA[(sum and 3).toInt()]) and 0xFFFFFFFFL
                v0 = (v0 - (shift0 xor key0)) and 0xFFFFFFFFL
            }

            val epochDays = v0 and 0xFFFFFFFFL
            val diasValidez = ((v1 ushr 16) and 0xFFFFL).toInt()
            val checksumLeido = (v1 and 0xFFFFL).toInt()

            val checksumEsperado = calcularChecksum(epochDays, diasValidez)
            if (checksumLeido != checksumEsperado) {
                return InfoLicencia(esValida = false, mensajeError = "Clave de licencia incorrecta o alterada")
            }

            val fechaEmisionMs = FECHA_BASE_EPOCH_MS + (epochDays * MS_POR_DIA)
            val fechaVencimientoMs = fechaEmisionMs + (diasValidez.toLong() * MS_POR_DIA)

            val diferenciaMs = fechaVencimientoMs - fechaActualMs
            val diasRestantes = (diferenciaMs / MS_POR_DIA).toInt()
            val estaVencida = fechaActualMs >= fechaVencimientoMs

            return InfoLicencia(
                esValida = true,
                fechaEmision = fechaEmisionMs,
                diasValidez = diasValidez,
                fechaVencimiento = fechaVencimientoMs,
                diasRestantes = if (estaVencida) 0 else diasRestantes.coerceAtLeast(0),
                estaVencida = estaVencida,
                mensajeError = if (estaVencida) "Esta licencia ha vencido" else ""
            )
        } catch (e: Exception) {
            return InfoLicencia(esValida = false, mensajeError = "Error al procesar la licencia: ${e.localizedMessage}")
        }
    }

    private fun calcularChecksum(epochDays: Long, dias: Int): Int {
        val hash = (epochDays * 31337L) xor (dias.toLong() * 7919L) xor CONSTANTE_MAGICA
        return (((hash xor (hash ushr 16)) and 0xFFFFL)).toInt()
    }
}
