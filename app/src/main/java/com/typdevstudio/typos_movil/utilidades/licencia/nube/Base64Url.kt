package com.typdevstudio.typos_movil.utilidades.licencia.nube

/**
 * Codificador/decodificador Base64 propio y sin dependencias de Android.
 *
 * `java.util.Base64` exige API 26 (la app soporta desde la 24) y `android.util.Base64`
 * no funciona en las pruebas unitarias de JVM, así que el núcleo de licencias usa este.
 *
 * Al decodificar acepta el alfabeto URL (`-` `_`) y el estándar (`+` `/`), con o sin relleno `=`.
 * Así sirve tanto para las partes del token (Base64URL sin relleno) como para la clave pública
 * (Base64 estándar con relleno). Rechaza espacios, caracteres extraños y bits sobrantes.
 */
object Base64Url {

    private const val ALFABETO_URL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

    private val VALORES = IntArray(128) { -1 }.also { tabla ->
        ALFABETO_URL.forEachIndexed { indice, caracter -> tabla[caracter.code] = indice }
        tabla['+'.code] = 62
        tabla['/'.code] = 63
    }

    /** Codifica en Base64URL sin relleno (el formato de las partes del token). */
    fun codificar(bytes: ByteArray): String {
        val salida = StringBuilder((bytes.size * 4 + 2) / 3)
        var i = 0
        while (i + 3 <= bytes.size) {
            val bloque = ((bytes[i].toInt() and 0xFF) shl 16) or
                ((bytes[i + 1].toInt() and 0xFF) shl 8) or
                (bytes[i + 2].toInt() and 0xFF)
            salida.append(ALFABETO_URL[bloque shr 18 and 0x3F])
            salida.append(ALFABETO_URL[bloque shr 12 and 0x3F])
            salida.append(ALFABETO_URL[bloque shr 6 and 0x3F])
            salida.append(ALFABETO_URL[bloque and 0x3F])
            i += 3
        }
        when (bytes.size - i) {
            1 -> {
                val bloque = (bytes[i].toInt() and 0xFF) shl 16
                salida.append(ALFABETO_URL[bloque shr 18 and 0x3F])
                salida.append(ALFABETO_URL[bloque shr 12 and 0x3F])
            }
            2 -> {
                val bloque = ((bytes[i].toInt() and 0xFF) shl 16) or ((bytes[i + 1].toInt() and 0xFF) shl 8)
                salida.append(ALFABETO_URL[bloque shr 18 and 0x3F])
                salida.append(ALFABETO_URL[bloque shr 12 and 0x3F])
                salida.append(ALFABETO_URL[bloque shr 6 and 0x3F])
            }
        }
        return salida.toString()
    }

    /** @throws IllegalArgumentException si el texto no es Base64 válido. */
    fun decodificar(texto: String): ByteArray {
        var fin = texto.length
        while (fin > 0 && texto[fin - 1] == '=') fin--
        val relleno = texto.length - fin
        require(relleno <= 2) { "Relleno Base64 inválido." }
        require(relleno == 0 || texto.length % 4 == 0) { "Relleno Base64 inválido." }
        val resto = fin % 4
        require(resto != 1) { "Longitud Base64 inválida." }

        val salida = ByteArray(fin / 4 * 3 + if (resto == 0) 0 else resto - 1)
        var acumulado = 0
        var bits = 0
        var j = 0
        for (i in 0 until fin) {
            val codigo = texto[i].code
            val valor = if (codigo < VALORES.size) VALORES[codigo] else -1
            require(valor >= 0) { "Carácter Base64 inválido en la posición $i." }
            acumulado = (acumulado shl 6) or valor
            bits += 6
            if (bits >= 8) {
                bits -= 8
                salida[j++] = (acumulado shr bits).toByte()
                acumulado = acumulado and ((1 shl bits) - 1)
            }
        }
        // Una codificación canónica deja en cero los bits sobrantes del último carácter.
        require(acumulado == 0) { "Base64 no canónico." }
        return salida
    }
}
