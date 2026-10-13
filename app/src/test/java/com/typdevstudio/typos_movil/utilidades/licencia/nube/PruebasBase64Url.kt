package com.typdevstudio.typos_movil.utilidades.licencia.nube

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.random.Random

class PruebasBase64Url {

    @Test
    fun codificaLosVectoresDelRfc4648SinRelleno() {
        val vectores = mapOf(
            "" to "", "f" to "Zg", "fo" to "Zm8", "foo" to "Zm9v",
            "foob" to "Zm9vYg", "fooba" to "Zm9vYmE", "foobar" to "Zm9vYmFy",
        )
        for ((texto, esperado) in vectores) {
            assertEquals(esperado, Base64Url.codificar(texto.toByteArray()))
            assertEquals(texto, String(Base64Url.decodificar(esperado)))
        }
    }

    @Test
    fun aceptaRellenoYAlfabetoEstandar() {
        assertEquals("foob", String(Base64Url.decodificar("Zm9vYg==")))
        assertEquals("fooba", String(Base64Url.decodificar("Zm9vYmE=")))
        val bytes = byteArrayOf(0xFB.toByte(), 0xFF.toByte())
        assertEquals("-_8", Base64Url.codificar(bytes))
        assertArrayEquals(bytes, Base64Url.decodificar("-_8"))
        assertArrayEquals(bytes, Base64Url.decodificar("+/8="))
    }

    @Test
    fun idaYVueltaConDatosAleatorios() {
        val aleatorio = Random(20261013)
        for (largo in 0..200) {
            val bytes = aleatorio.nextBytes(largo)
            val codificado = Base64Url.codificar(bytes)
            assertEquals(java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes), codificado)
            assertArrayEquals(bytes, Base64Url.decodificar(codificado))
        }
    }

    @Test
    fun rechazaTextosInvalidos() {
        val invalidos = listOf(
            "Zm9v Yg", // espacio
            "Zm9v*", // carácter fuera del alfabeto
            "Zm9vñ", // no ASCII
            "Z", // longitud imposible
            "Zg=", // relleno sin completar el bloque
            "Zg===", // demasiado relleno
            "Zh", // bits sobrantes distintos de cero
        )
        for (texto in invalidos) {
            assertThrows(texto, IllegalArgumentException::class.java) { Base64Url.decodificar(texto) }
        }
    }
}
