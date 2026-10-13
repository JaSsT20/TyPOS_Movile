package com.typdevstudio.typos_movil.utilidades.licencia

import com.typdevstudio.typos_movil.utilidades.licencia.PoliticaValidacionAutomatica.MINIMO_ENTRE_INTENTOS_MS
import com.typdevstudio.typos_movil.utilidades.licencia.PoliticaValidacionAutomatica.debeIntentar
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PruebasPoliticaValidacionAutomatica {

    private val ahora = 1_800_000_000_000L

    @Test
    fun soloIntentaSiHaceFaltaValidar() {
        assertFalse(debeIntentar(necesitaValidar = false, ultimoIntentoMs = null, ahoraMs = ahora))
        assertTrue(debeIntentar(necesitaValidar = true, ultimoIntentoMs = null, ahoraMs = ahora))
    }

    @Test
    fun noRepiteIntentosSeguidos() {
        assertFalse(debeIntentar(true, ahora - MINIMO_ENTRE_INTENTOS_MS + 1, ahora))
        assertTrue(debeIntentar(true, ahora - MINIMO_ENTRE_INTENTOS_MS, ahora))
        // Si el reloj retrocedió, se intenta: no hay que quedarse esperando un intervalo imposible.
        assertTrue(debeIntentar(true, ahora + 60_000, ahora))
    }
}
