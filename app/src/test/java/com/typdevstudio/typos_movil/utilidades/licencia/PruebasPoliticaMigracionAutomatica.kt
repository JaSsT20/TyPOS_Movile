package com.typdevstudio.typos_movil.utilidades.licencia

import com.typdevstudio.typos_movil.utilidades.licencia.PoliticaMigracionAutomatica.ESPERA_TRAS_RECHAZO_MS
import com.typdevstudio.typos_movil.utilidades.licencia.PoliticaMigracionAutomatica.debeIntentar
import com.typdevstudio.typos_movil.utilidades.licencia.PoliticaMigracionAutomatica.esRechazoDeLicencia
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PruebasPoliticaMigracionAutomatica {

    private val ahora = 1_800_000_000_000L
    private val version = 26

    @Test
    fun sinRechazosPreviosSeIntenta() = assertTrue(debeIntentar(null, null, version, ahora))

    @Test
    fun trasUnRechazoEsperaUnDia() {
        assertFalse(debeIntentar(ahora - ESPERA_TRAS_RECHAZO_MS + 1, version, version, ahora))
        assertTrue(debeIntentar(ahora - ESPERA_TRAS_RECHAZO_MS, version, version, ahora))
        // Si el reloj retrocedió, se intenta: no hay que quedarse esperando un intervalo imposible.
        assertTrue(debeIntentar(ahora + 60_000, version, version, ahora))
    }

    @Test
    fun alActualizarLaAppSeIntentaEnseguida() {
        // Rechazo guardado por la versión anterior (o por una que no guardaba la versión).
        assertTrue(debeIntentar(ahora - 1_000, version - 1, version, ahora))
        assertTrue(debeIntentar(ahora - 1_000, null, version, ahora))
    }

    @Test
    fun soloLasRespuestasSobreLaLicenciaCuentanComoRechazo() {
        for (codigo in listOf("MIGRACION_NO_DISPONIBLE", "LICENCIA_VENCIDA", "LICENCIA_SUSPENDIDA", "LIMITE_DISPOSITIVOS")) {
            assertTrue(codigo, esRechazoDeLicencia(codigo))
        }
        // El servidor todavía sin el endpoint /migrar (lo que pasó al publicar la app antes que la API).
        assertFalse(esRechazoDeLicencia("RUTA_NO_ENCONTRADA"))
        assertFalse(esRechazoDeLicencia("METODO_NO_PERMITIDO"))
        assertFalse(esRechazoDeLicencia("ERROR_INTERNO"))
    }
}
