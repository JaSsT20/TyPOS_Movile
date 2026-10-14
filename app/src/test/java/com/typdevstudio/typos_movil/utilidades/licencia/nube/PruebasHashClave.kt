package com.typdevstudio.typos_movil.utilidades.licencia.nube

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PruebasHashClave {

    @Test
    fun coincideConElPbkdf2DeJava() {
        val sal = "sal-de-prueba-16".toByteArray()
        for ((clave, iteraciones) in listOf("1234" to 1, "1234" to 2, "contraseña" to 1000, "x" to 4096)) {
            val esperado = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(PBEKeySpec(clave.toCharArray(), sal, iteraciones, 256)).encoded
            assertArrayEquals("$clave/$iteraciones", esperado, HashClave.pbkdf2Sha256(clave.toByteArray(), sal, iteraciones, 32))
        }
    }

    @Test
    fun generaYVerificaConElFormatoQueAceptaLaApi() {
        val hash = HashClave.generar("1234")
        assertTrue(hash, Regex("^pbkdf2_sha256\\$[0-9]{4,7}\\$[A-Za-z0-9_-]{16,64}\\$[A-Za-z0-9_-]{32,128}$").matches(hash))
        assertTrue(HashClave.esHash(hash))
        assertTrue(HashClave.verificar("1234", hash))
        assertFalse(HashClave.verificar("12345", hash))
        assertFalse(HashClave.verificar("", hash))
    }

    @Test
    fun cadaHashLlevaSuPropiaSal() {
        val a = HashClave.generar("1234", iteraciones = 1000)
        val b = HashClave.generar("1234", iteraciones = 1000)
        assertNotEquals(a, b)
        assertTrue(HashClave.verificar("1234", a) && HashClave.verificar("1234", b))
    }

    @Test
    fun valoresQueNoSonHashNuncaCoinciden() {
        assertFalse(HashClave.esHash("1234"))
        for (malo in listOf("1234", "pbkdf2_sha256\$abc\$x\$y", "pbkdf2_sha256\$1000\$!!\$!!", "otro\$1000\$aa\$bb", "")) {
            assertFalse(malo, HashClave.verificar("1234", malo))
        }
    }
}
