package com.typdevstudio.typos_movil.utilidades.licencia.nube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class PruebasEvaluadorEstadoLicencia {

    /** 2026-10-13 08:00 en RD. */
    private val ahora: Instant = Instant.parse("2026-10-13T12:00:00Z")

    private fun evaluar(
        payload: PayloadLicencia? = FabricaTokens.payload(),
        momento: Instant = ahora,
        bloqueo: MotivoRestriccion? = null,
        ultimaFechaVista: Instant? = null,
        deviceHashLocal: String = FabricaTokens.DEVICE_HASH,
        token: ResultadoVerificacion? = payload?.let { ResultadoVerificacion.Valido(it) },
    ) = EvaluadorEstadoLicencia.evaluar(token, deviceHashLocal, momento.toEpochMilli(), bloqueo, ultimaFechaVista?.toEpochMilli())

    private fun assertRestringida(motivo: MotivoRestriccion, estado: EstadoLicencia) {
        assertTrue("Se esperaba Restringida($motivo): $estado", estado is EstadoLicencia.Restringida)
        assertEquals(motivo, (estado as EstadoLicencia.Restringida).motivo)
        assertFalse(estado.permiteEmitir)
    }

    @Test
    fun tokenVigenteConMargenEsActiva() {
        val estado = evaluar()
        assertTrue(estado is EstadoLicencia.Activa)
        assertTrue(estado.permiteEmitir)
    }

    @Test
    fun conTresDiasOMenosParaValidoHastaPideValidar() {
        val exactamenteTresDias = FabricaTokens.payload(validoHasta = ahora.plus(Duration.ofDays(3)))
        assertTrue(evaluar(exactamenteTresDias) is EstadoLicencia.ActivaPorValidar)

        val dosHoras = FabricaTokens.payload(validoHasta = ahora.plus(Duration.ofHours(2)))
        val estado = evaluar(dosHoras)
        assertTrue(estado is EstadoLicencia.ActivaPorValidar)
        assertTrue(estado.permiteEmitir)

        val tresDiasYUnSegundo = FabricaTokens.payload(validoHasta = ahora.plus(Duration.ofDays(3).plusSeconds(1)))
        assertTrue(evaluar(tresDiasYUnSegundo) is EstadoLicencia.Activa)
    }

    @Test
    fun venceHoyEsValidaHastaElFinDelDiaEnRD() {
        val payload = FabricaTokens.payload(vence = "2026-10-13", validoHasta = Instant.parse("2026-10-20T00:00:00Z"))
        // 23:59:59 del 13 en RD.
        assertTrue(evaluar(payload, momento = Instant.parse("2026-10-14T03:59:59Z")) is EstadoLicencia.Activa)
        // 00:00 del 14 en RD: ya venció, entra en cortesía.
        assertTrue(evaluar(payload, momento = Instant.parse("2026-10-14T04:00:00Z")) is EstadoLicencia.EnCortesia)
    }

    @Test
    fun vencidaDentroDeLaCortesiaCalculaLosDiasRestantes() {
        // Venció ayer (12) con 3 días de cortesía: le quedan 13→15, es decir, 2 días después de hoy.
        val payload = FabricaTokens.payload(vence = "2026-10-12", diasCortesia = 3, validoHasta = Instant.parse("2026-10-16T04:00:00Z"))
        val estado = evaluar(payload)
        assertTrue(estado is EstadoLicencia.EnCortesia)
        assertEquals(2, (estado as EstadoLicencia.EnCortesia).diasRestantes)
        assertTrue(estado.permiteEmitir)

        val ultimoDia = evaluar(payload, momento = Instant.parse("2026-10-16T03:00:00Z"))
        assertEquals(0, (ultimoDia as EstadoLicencia.EnCortesia).diasRestantes)
    }

    @Test
    fun cortesiaAgotadaEsVencidaAunqueElTokenSigaVigente() {
        // Defensa adicional: el servidor nunca emitiría este valido_hasta.
        val payload = FabricaTokens.payload(vence = "2026-10-01", diasCortesia = 3)
        assertRestringida(MotivoRestriccion.VENCIDA, evaluar(payload))

        val sinCortesia = FabricaTokens.payload(vence = "2026-10-12", diasCortesia = 0)
        assertRestringida(MotivoRestriccion.VENCIDA, evaluar(sinCortesia))
    }

    @Test
    fun alLlegarAValidoHastaElTokenExpira() {
        val payload = FabricaTokens.payload(validoHasta = ahora)
        assertRestringida(MotivoRestriccion.TOKEN_EXPIRADO, evaluar(payload))
        assertTrue(evaluar(payload, momento = ahora.minusMillis(1)) is EstadoLicencia.ActivaPorValidar)
    }

    @Test
    fun elBloqueoDelServidorSeRespetaAunqueElTokenSigaVigente() {
        for (motivo in listOf(MotivoRestriccion.SUSPENDIDA, MotivoRestriccion.VENCIDA, MotivoRestriccion.REVOCADO, MotivoRestriccion.SIN_ACTIVAR)) {
            val estado = evaluar(bloqueo = motivo)
            assertRestringida(motivo, estado)
            assertEquals(FabricaTokens.payload(), (estado as EstadoLicencia.Restringida).payload)
        }
        assertRestringida(MotivoRestriccion.REVOCADO, evaluar(payload = null, bloqueo = MotivoRestriccion.REVOCADO))
    }

    @Test
    fun sinTokenEstaSinActivar() {
        val estado = evaluar(payload = null)
        assertEquals(EstadoLicencia.SinActivar, estado)
        assertFalse(estado.permiteEmitir)
    }

    @Test
    fun tokenConFirmaInvalidaRestringe() {
        val estado = evaluar(token = ResultadoVerificacion.Invalido("La firma del token no es válida."))
        assertRestringida(MotivoRestriccion.FIRMA_INVALIDA, estado)
        assertNull((estado as EstadoLicencia.Restringida).payload)
    }

    @Test
    fun tokenDeOtroDispositivoRestringe() {
        assertRestringida(MotivoRestriccion.DISPOSITIVO_DISTINTO, evaluar(deviceHashLocal = "0".repeat(64)))
        // El hash hexadecimal se compara sin distinguir mayúsculas.
        assertTrue(evaluar(deviceHashLocal = FabricaTokens.DEVICE_HASH.uppercase()) is EstadoLicencia.Activa)
    }

    @Test
    fun relojAtrasadoRespectoALaUltimaFechaVistaRestringe() {
        assertRestringida(MotivoRestriccion.RELOJ_ALTERADO, evaluar(ultimaFechaVista = ahora.plus(Duration.ofHours(1))))
        // Diferencias pequeñas quedan dentro de la tolerancia.
        assertTrue(evaluar(ultimaFechaVista = ahora.plus(Duration.ofMinutes(9))) is EstadoLicencia.Activa)
        assertTrue(evaluar(ultimaFechaVista = ahora.minus(Duration.ofDays(2))) is EstadoLicencia.Activa)
    }

    @Test
    fun relojAnteriorALaEmisionDelTokenRestringe() {
        val emitidoEnElFuturo = FabricaTokens.payload(emitido = ahora.plus(Duration.ofMinutes(11)))
        assertRestringida(MotivoRestriccion.RELOJ_ALTERADO, evaluar(emitidoEnElFuturo))

        val emitidoNueveMinutosDespues = FabricaTokens.payload(emitido = ahora.plus(Duration.ofMinutes(9)))
        assertTrue(evaluar(emitidoNueveMinutosDespues) is EstadoLicencia.Activa)
    }

    @Test
    fun laUltimaFechaVistaNuncaRetrocede() {
        assertEquals(100L, EvaluadorEstadoLicencia.nuevaUltimaFechaVista(null, 100L))
        assertEquals(200L, EvaluadorEstadoLicencia.nuevaUltimaFechaVista(100L, 200L))
        assertEquals(200L, EvaluadorEstadoLicencia.nuevaUltimaFechaVista(200L, 150L))
    }

    @Test
    fun traduceLosCodigosDeErrorDelServidor() {
        assertEquals(MotivoRestriccion.SUSPENDIDA, MotivoRestriccion.desdeCodigoServidor("LICENCIA_SUSPENDIDA"))
        assertEquals(MotivoRestriccion.VENCIDA, MotivoRestriccion.desdeCodigoServidor("LICENCIA_VENCIDA"))
        assertEquals(MotivoRestriccion.REVOCADO, MotivoRestriccion.desdeCodigoServidor("DISPOSITIVO_REVOCADO"))
        assertEquals(MotivoRestriccion.SIN_ACTIVAR, MotivoRestriccion.desdeCodigoServidor("LICENCIA_NO_ENCONTRADA"))
        assertEquals(MotivoRestriccion.SIN_ACTIVAR, MotivoRestriccion.desdeCodigoServidor("DISPOSITIVO_NO_REGISTRADO"))
        // Fallas técnicas y de red nunca restringen.
        for (codigo in listOf("ERROR_INTERNO", "SOLICITUD_INVALIDA", "LIMITE_DISPOSITIVOS", "OTRO", null)) {
            assertNull(codigo, MotivoRestriccion.desdeCodigoServidor(codigo))
        }
    }
}
