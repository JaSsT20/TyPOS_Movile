package com.typdevstudio.typos_movil.datos.impresion

import java.io.ByteArrayOutputStream

/**
 * Codificador especializado para impresoras térmicas ESC/POS.
 * 
 * Resuelve problemas de caracteres especiales y tildes en español (á, é, í, ó, ú, Á, É, Í, Ó, Ú, ñ, Ñ, ¡, ¿, etc.)
 * cambiando la tabla de caracteres (Code Page) de la impresora a CP850 (Multilingual Latin I)
 * y mapeando directamente los bytes correspondientes para evitar que salgan símbolos extraños como '≤'.
 */
object CodificadorEscPos {

    // Comando ESC/POS para seleccionar la tabla de caracteres Code Page 850 (Multilingual Latin I)
    // ESC t 2 (0x1B, 0x74, 0x02)
    val COMANDO_SELECCIONAR_CP850 = byteArrayOf(0x1B, 0x74, 0x02)

    // Comando ESC/POS para seleccionar la tabla de caracteres Code Page 437 (USA / OEM)
    val COMANDO_SELECCIONAR_CP437 = byteArrayOf(0x1B, 0x74, 0x00)

    // Comando ESC/POS para seleccionar la tabla de caracteres Windows-1252
    val COMANDO_SELECCIONAR_WCP1252 = byteArrayOf(0x1B, 0x74, 0x10)

    /**
     * Convierte una cadena de texto en un array de bytes codificado según la tabla CP850 de ESC/POS.
     * Mapea con precisión absoluta todos los caracteres especiales en español.
     */
    fun aBytes(texto: String): ByteArray {
        val buffer = ByteArrayOutputStream(texto.length)

        for (char in texto) {
            when (char) {
                // Vocales minúsculas con tilde (CP850 / CP437)
                'á' -> buffer.write(0xA0)
                'é' -> buffer.write(0x82)
                'í' -> buffer.write(0xA1)
                'ó' -> buffer.write(0xA2)
                'ú' -> buffer.write(0xA3)

                // Vocales mayúsculas con tilde (CP850)
                'Á' -> buffer.write(0xB5)
                'É' -> buffer.write(0x90)
                'Í' -> buffer.write(0xD6)
                'Ó' -> buffer.write(0xE0)
                'Ú' -> buffer.write(0xE9)

                // Letra Ñ / ñ
                'ñ' -> buffer.write(0xA4)
                'Ñ' -> buffer.write(0xA5)

                // Diéresis
                'ü' -> buffer.write(0x81)
                'Ü' -> buffer.write(0x9A)

                // Signos de puntuación en español
                '¡' -> buffer.write(0xAD)
                '¿' -> buffer.write(0xA8)
                'º' -> buffer.write(0xA7)
                'ª' -> buffer.write(0xA6)

                // Caracteres ASCII estándar (0..127)
                in '\u0000'..'\u007F' -> buffer.write(char.code)

                // Fallback para otros caracteres especiales fuera de ASCII básico
                'à', 'â', 'ä' -> buffer.write(0x85)
                'è', 'ê', 'ë' -> buffer.write(0x8A)
                'ì', 'î', 'ï' -> buffer.write(0x8D)
                'ò', 'ô', 'ö' -> buffer.write(0x95)
                'ù', 'û' -> buffer.write(0x97)
                'ç' -> buffer.write(0x87)
                'Ç' -> buffer.write(0x80)

                else -> {
                    // Si el carácter no tiene mapeo directo, enviar su representación más cercana o '?'
                    val codigo = char.code
                    if (codigo in 32..126) {
                        buffer.write(codigo)
                    } else {
                        buffer.write(' '.code)
                    }
                }
            }
        }

        return buffer.toByteArray()
    }
}
