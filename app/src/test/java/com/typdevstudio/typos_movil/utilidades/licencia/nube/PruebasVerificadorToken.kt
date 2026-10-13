package com.typdevstudio.typos_movil.utilidades.licencia.nube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.time.Instant

class PruebasVerificadorToken {

    private fun valido(resultado: ResultadoVerificacion): PayloadLicencia {
        assertTrue("Se esperaba un token válido: $resultado", resultado is ResultadoVerificacion.Valido)
        return (resultado as ResultadoVerificacion.Valido).payload
    }

    private fun assertInvalido(resultado: ResultadoVerificacion) =
        assertTrue("Se esperaba un token inválido: $resultado", resultado is ResultadoVerificacion.Invalido)

    @Test
    fun verificaUnTokenFirmadoEnDerYLeeElPayload() {
        val payload = valido(FabricaTokens.verificador().verificar(FabricaTokens.firmar(FabricaTokens.json())))

        assertEquals("K7QX-9M2D-PL4A", payload.licencia)
        assertEquals(FabricaTokens.DEVICE_HASH, payload.deviceHash)
        assertEquals("Ferretería López", payload.cliente)
        assertEquals("pro", payload.plan)
        assertEquals(3, payload.maxDispositivos)
        assertEquals(10, payload.maxUsuarios)
        assertEquals(listOf("inventario", "facturacion", "cxc"), payload.modulos)
        assertEquals(HoraRD.parsearFecha("2026-12-31"), payload.vence)
        assertEquals(3, payload.diasCortesia)
        assertEquals(Instant.parse("2026-10-12T22:00:00Z").toEpochMilli(), payload.emitidoMs)
        assertEquals(Instant.parse("2026-10-27T22:00:00Z").toEpochMilli(), payload.validoHastaMs)
    }

    @Test
    fun alterarUnSoloCaracterDelPayloadInvalidaLaFirma() {
        val token = FabricaTokens.firmar(FabricaTokens.json())
        val verificador = FabricaTokens.verificador()
        val finPayload = token.indexOf('.')
        for (posicion in listOf(0, finPayload / 2, finPayload - 1)) {
            val original = token[posicion]
            val reemplazo = if (original == 'A') 'B' else 'A'
            val alterado = token.substring(0, posicion) + reemplazo + token.substring(posicion + 1)
            assertInvalido(verificador.verificar(alterado))
        }
    }

    @Test
    fun alterarLaFirmaLaInvalida() {
        val token = FabricaTokens.firmar(FabricaTokens.json())
        val posicion = token.length - 5
        val alterado = token.substring(0, posicion) + (if (token[posicion] == 'A') 'B' else 'A') + token.substring(posicion + 1)
        assertInvalido(FabricaTokens.verificador().verificar(alterado))
    }

    @Test
    fun rechazaUnTokenFirmadoConOtraClave() {
        val otra = FabricaTokens.generarParLlaves()
        assertInvalido(FabricaTokens.verificador().verificar(FabricaTokens.firmar(FabricaTokens.json(), otra.private)))
    }

    @Test
    fun rechazaUnaFirmaEnFormatoP1363() {
        // .NET firma en P1363 por defecto; la API debe usar DER. Si no, la app lo rechaza.
        val token = FabricaTokens.firmar(FabricaTokens.json(), algoritmo = "SHA256withECDSAinP1363Format")
        assertInvalido(FabricaTokens.verificador().verificar(token))
    }

    @Test
    fun rechazaTokensMalFormados() {
        val verificador = FabricaTokens.verificador()
        val token = FabricaTokens.firmar(FabricaTokens.json())
        for (malo in listOf("", "abc", ".", "abc.", ".abc", "$token.extra", token.replace('.', '!'), "a".repeat(9000) + ".a")) {
            assertInvalido(verificador.verificar(malo))
        }
    }

    @Test
    fun rechazaUnPayloadFirmadoQueNoCumpleElContrato() {
        val verificador = FabricaTokens.verificador()
        val sinVence = FabricaTokens.json().replace(""""vence":"2026-12-31",""", "")
        val usuariosComoTexto = FabricaTokens.json().replace(""""max_usuarios":10""", """"max_usuarios":"10"""")
        val fechaMala = FabricaTokens.json(validoHasta = "2026-10-27 22:00")
        for (json in listOf(sinVence, usuariosComoTexto, fechaMala, "no es json", "[]")) {
            assertInvalido(verificador.verificar(FabricaTokens.firmar(json)))
        }
    }

    @Test
    fun sinDiasCortesiaSeAsumeCero() {
        val json = FabricaTokens.json().replace(""""dias_cortesia":3,""", "")
        assertEquals(0, valido(FabricaTokens.verificador().verificar(FabricaTokens.firmar(json))).diasCortesia)
    }

    /** Token generado con el ServicioFirma real de la API (.NET) y una llave desechable. */
    @Test
    fun verificaUnTokenEmitidoPorLaApiDotNet() {
        val verificador = VerificadorToken.desdeClavePublicaBase64(CLAVE_PUBLICA_DOTNET)
        val payload = valido(verificador.verificar(TOKEN_DOTNET))

        assertEquals("Ferretería López", payload.cliente)
        assertEquals(FabricaTokens.DEVICE_HASH, payload.deviceHash)
        assertEquals(listOf("inventario", "facturacion", "cxc"), payload.modulos)
        assertEquals(3, payload.diasCortesia)
        assertEquals(Instant.parse("2026-10-27T22:00:00Z").toEpochMilli(), payload.validoHastaMs)

        val alterado = "f" + TOKEN_DOTNET.substring(1)
        assertInvalido(verificador.verificar(alterado))
    }

    @Test
    fun cargarClavePublicaRechazaClavesQueNoSonP256() {
        val rsa = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val rsaBase64 = java.util.Base64.getEncoder().encodeToString(rsa.public.encoded)
        for (clave in listOf("", "no-es-base64!", "AAAA", rsaBase64)) {
            assertThrows(clave, IllegalArgumentException::class.java) { VerificadorToken.cargarClavePublica(clave) }
        }
    }

    private companion object {
        const val CLAVE_PUBLICA_DOTNET =
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEPbKgGRN/DkYh9BVnqGe0Ad3+Fe8jL+0vVYNZsbnDjpEuIsrxkIilPpojGbhfEudKfv7SD1+QdnfadxHHv01PFQ=="

        const val TOKEN_DOTNET =
            "eyJsaWNlbmNpYSI6Iks3UVgtOU0yRC1QTDRBIiwiZGV2aWNlX2hhc2giOiI5Zjg2ZDA4MTg4NGM3ZDY1OWEyZmVhYTBjNTVhZDAxNWEzYmY0ZjFiMmIwYjgyMmNkMTVkNmMxNWIwZjAwYTA4IiwiY2xpZW50ZSI6IkZlcnJldGVyw61hIEzDs3BleiIsInBsYW4iOiJwcm8iLCJtYXhfZGlzcG9zaXRpdm9zIjozLCJtYXhfdXN1YXJpb3MiOjEwLCJtb2R1bG9zIjpbImludmVudGFyaW8iLCJmYWN0dXJhY2lvbiIsImN4YyJdLCJ2ZW5jZSI6IjIwMjYtMTItMzEiLCJkaWFzX2NvcnRlc2lhIjozLCJlbWl0aWRvIjoiMjAyNi0xMC0xMlQyMjowMDowMFoiLCJ2YWxpZG9faGFzdGEiOiIyMDI2LTEwLTI3VDIyOjAwOjAwWiJ9" +
                ".MEQCIBoLSvax6KY9ayRzW8uLO6MqWRVG1SMVQrGWOKuVZeppAiBZB8eijq1zWUpQU9RnuX-yxsyPw1ThX8rXnkKTV-ltWw"
    }
}
