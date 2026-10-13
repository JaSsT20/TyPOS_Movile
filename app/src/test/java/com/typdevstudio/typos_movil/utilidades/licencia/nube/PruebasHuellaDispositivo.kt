package com.typdevstudio.typos_movil.utilidades.licencia.nube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import java.util.Locale

class PruebasHuellaDispositivo {

    private val idWidevine = ByteArray(32) { (it * 7 + 3).toByte() }

    /** Copia de `IdDispositivo.bytesToHex16`: el serial de la licencia antigua. */
    private fun serialAntiguo(id: ByteArray): String {
        val sha = MessageDigest.getInstance("SHA-256").digest(id)
        return (0 until 8).joinToString("") { String.format(Locale.ROOT, "%02X", sha[it]) }
    }

    @Test
    fun conWidevineEsElSha256CompletoEnHexMinuscula() {
        val hash = HuellaDispositivo.calcular(idWidevine, "a1b2c3d4e5f60718")!!
        assertTrue(hash, Regex("[0-9a-f]{64}").matches(hash))
        assertEquals(hash, HuellaDispositivo.calcular(idWidevine.copyOf(), null))
    }

    @Test
    fun susPrimeros16CaracteresSonElSerialAntiguo() {
        val hash = HuellaDispositivo.calcular(idWidevine, null)!!
        assertEquals(serialAntiguo(idWidevine), hash.take(16).uppercase(Locale.ROOT))
    }

    @Test
    fun sinWidevineUsaAndroidId() {
        val hash = HuellaDispositivo.calcular(null, "a1b2c3d4e5f60718")!!
        assertTrue(Regex("[0-9a-f]{64}").matches(hash))
        assertEquals(hash, HuellaDispositivo.calcular(ByteArray(0), "A1B2C3D4E5F60718"))
        assertNotEquals(hash, HuellaDispositivo.calcular(null, "a1b2c3d4e5f60719"))
    }

    @Test
    fun sinIdentificadorUtilizableDevuelveNulo() {
        for (androidId in listOf(null, "", "   ", "9774d56d682e549c")) {
            assertNull(androidId, HuellaDispositivo.calcular(null, androidId))
        }
    }
}
