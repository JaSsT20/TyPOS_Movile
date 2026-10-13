package com.typdevstudio.typos_movil.utilidades.licencia.nube

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Cifrado real con el Android Keystore: solo se puede probar en un teléfono o emulador. */
@RunWith(AndroidJUnit4::class)
class PruebasPreferenciasCifradas {

    private val contexto: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val crudo get() = contexto.getSharedPreferences("licencia_nube", Context.MODE_PRIVATE)

    @Before
    @After
    fun limpiar() {
        crudo.edit().clear().commit()
    }

    @Test
    fun guardaCifradoYLeeElMismoValor() {
        val archivo = PreferenciasCifradas(contexto)
        archivo.escribir(mapOf("token" to "PAYLOAD.FIRMA-secreta", "clave" to "K7QX-9M2D-PL4A"))

        assertEquals("PAYLOAD.FIRMA-secreta", archivo.leer("token"))
        assertEquals("K7QX-9M2D-PL4A", PreferenciasCifradas(contexto).leer("clave"))

        val enDisco = crudo.all.values.joinToString()
        assertFalse(enDisco, enDisco.contains("PAYLOAD") || enDisco.contains("K7QX"))

        archivo.escribir(mapOf("token" to null))
        assertNull(archivo.leer("token"))
    }

    @Test
    fun unValorMovidoAOtroCampoOAlteradoNoSeDescifra() {
        val archivo = PreferenciasCifradas(contexto)
        archivo.escribir(mapOf("bloqueo_servidor" to "SUSPENDIDA"))
        val cifrado = crudo.getString("bloqueo_servidor", null)!!

        crudo.edit().putString("token", cifrado).commit()
        assertNull(archivo.leer("token"))

        val alterado = cifrado.dropLast(2) + if (cifrado.endsWith("AA")) "BA" else "AA"
        crudo.edit().putString("bloqueo_servidor", alterado).commit()
        assertNull(archivo.leer("bloqueo_servidor"))

        crudo.edit().putString("bloqueo_servidor", "no-es-base64!").commit()
        assertNull(archivo.leer("bloqueo_servidor"))
    }

    @Test
    fun elDeviceHashEsEstableYSeGuarda() {
        val almacen = AlmacenLicencia.crear(contexto)
        val hash = HuellaDispositivo.obtener(contexto, almacen)

        assertTrue(hash, Regex("[0-9a-f]{64}").matches(hash))
        assertEquals(hash, HuellaDispositivo.obtener(contexto, AlmacenLicencia.crear(contexto)))
        assertNotNull(crudo.getString("device_hash", null))
    }
}
