package com.typdevstudio.typos_movil.utilidades.licencia

import com.typdevstudio.typos_movil.utilidades.licencia.PoliticaMigracionAutomatica.ESPERA_TRAS_RECHAZO_MS
import com.typdevstudio.typos_movil.utilidades.licencia.PoliticaMigracionAutomatica.debeIntentar
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PruebasPoliticaMigracionAutomatica {

    private val ahora = 1_800_000_000_000L

    @Test
    fun sinRechazosPreviosSeIntenta() = assertTrue(debeIntentar(ultimoRechazoMs = null, ahoraMs = ahora))

    @Test
    fun trasUnRechazoEsperaUnDia() {
        assertFalse(debeIntentar(ahora - ESPERA_TRAS_RECHAZO_MS + 1, ahora))
        assertTrue(debeIntentar(ahora - ESPERA_TRAS_RECHAZO_MS, ahora))
        // Si el reloj retrocedió, se intenta: no hay que quedarse esperando un intervalo imposible.
        assertTrue(debeIntentar(ahora + 60_000, ahora))
    }
}
