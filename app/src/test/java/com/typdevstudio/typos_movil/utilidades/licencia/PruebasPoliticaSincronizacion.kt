package com.typdevstudio.typos_movil.utilidades.licencia

import com.typdevstudio.typos_movil.utilidades.licencia.PoliticaSincronizacion.MINIMO_ENTRE_CICLOS_MS
import com.typdevstudio.typos_movil.utilidades.licencia.PoliticaSincronizacion.accion
import com.typdevstudio.typos_movil.utilidades.licencia.PoliticaSincronizacion.debeIntentar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PruebasPoliticaSincronizacion {

    private val ahora = 1_800_000_000_000L

    @Test
    fun noRepiteCiclosSeguidos() {
        assertTrue(debeIntentar(null, ahora))
        assertFalse(debeIntentar(ahora - MINIMO_ENTRE_CICLOS_MS + 1, ahora))
        assertTrue(debeIntentar(ahora - MINIMO_ENTRE_CICLOS_MS, ahora))
        // Si el reloj retrocedió, se intenta: no hay que quedarse esperando un intervalo imposible.
        assertTrue(debeIntentar(ahora + 60_000, ahora))
    }

    @Test
    fun conLicenciaValidaSinoConsultaElRegistroOMigra() {
        assertEquals(AccionSincronizacion.VALIDAR, accion(tieneLicencia = true, registroPendiente = true, puedeMigrar = true))
        assertEquals(AccionSincronizacion.CONSULTAR_REGISTRO, accion(tieneLicencia = false, registroPendiente = true, puedeMigrar = true))
        assertEquals(AccionSincronizacion.MIGRAR, accion(tieneLicencia = false, registroPendiente = false, puedeMigrar = true))
        assertEquals(AccionSincronizacion.NADA, accion(tieneLicencia = false, registroPendiente = false, puedeMigrar = false))
    }

    @Test
    fun conLaAppAbiertaLosCambiosLleganEnCincoMinutosYEnSegundoPlanoEnQuince() {
        assertEquals(5L * 60 * 1000, PoliticaSincronizacion.INTERVALO_CON_APP_ABIERTA_MS)
        assertEquals(15L, PoliticaSincronizacion.INTERVALO_EN_SEGUNDO_PLANO_MIN)
    }
}
