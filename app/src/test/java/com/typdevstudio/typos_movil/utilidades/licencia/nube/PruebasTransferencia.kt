package com.typdevstudio.typos_movil.utilidades.licencia.nube

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

/** Restaurar en este teléfono una copia de otro: cliente HTTP y repositorio. */
class PruebasTransferencia {

    private val ahora: Instant = Instant.parse("2026-10-13T12:00:00Z")

    private class TransporteFalso(var respuesta: (String, String?) -> RespuestaHttp) : TransporteHttp {
        val solicitudes = mutableListOf<Pair<String, String?>>()
        override fun enviar(metodo: String, url: String, cuerpoJson: String?): RespuestaHttp {
            solicitudes += url to cuerpoJson
            return respuesta(url, cuerpoJson)
        }
    }

    private fun tokenDeEsteTelefono(licencia: String = "K7QX-9M2D-PL4A") = FabricaTokens.firmar(
        FabricaTokens.json(
            licencia = licencia,
            emitido = ahora.toString(),
            validoHasta = ahora.plus(Duration.ofDays(15)).toString(),
        )
    )

    private fun repositorio(transporte: TransporteHttp, almacen: AlmacenLicencia = AlmacenLicencia(ArchivoEnMemoria())) =
        RepositorioLicencia(
            cliente = ClienteApiLicencias("http://api/api/v1", transporte),
            verificador = FabricaTokens.verificador(),
            almacen = almacen,
            deviceHash = { FabricaTokens.DEVICE_HASH },
            versionApp = "1.3.0",
            reloj = { ahora.toEpochMilli() },
            registro = RegistroEnMemoria(),
        )

    @Test
    fun consultarEnviaElContratoYLeeLaRespuesta() {
        val transporte = TransporteFalso { _, _ ->
            RespuestaHttp(
                200,
                """{"ok":true,"cliente":"Ferretería López","ya_registrado":false,"hay_cupo":false,"requiere_desvincular":true,""" +
                    """"dispositivo_origen":{"nombre":"Samsung A54 - Caja 1","revocado":false},"max_dispositivos":1,"en_uso":1}""",
            )
        }
        val respuesta = ClienteApiLicencias("http://api/api/v1", transporte).consultarTransferencia("A.B", "hash-b", "op-1")

        assertEquals(
            RespuestaApi.ConsultaTransferencia("Ferretería López", false, false, true, "Samsung A54 - Caja 1", 1, 1),
            respuesta,
        )
        val (url, cuerpo) = transporte.solicitudes.single()
        assertEquals("http://api/api/v1/transferir/consultar", url)
        assertEquals(setOf("token_origen", "device_hash", "id_operacion"), JSONObject(cuerpo!!).keys().asSequence().toSet())
    }

    @Test
    fun elLimiteDeTransferenciasEsUnErrorDeNegocioYNoUnaFallaDeRed() {
        val limite = TransporteFalso { _, _ ->
            RespuestaHttp(429, """{"ok":false,"codigo":"LIMITE_TRANSFERENCIAS","mensaje":"Ya cambió de teléfono varias veces este mes."}""")
        }
        val respuesta = ClienteApiLicencias("http://api/api/v1", limite).transferir("A.B", "hash-b", null, "1.3.0", true, "op-1")
        assertEquals("LIMITE_TRANSFERENCIAS", (respuesta as RespuestaApi.ErrorNegocio).codigo)

        val demasiadas = TransporteFalso { _, _ ->
            RespuestaHttp(429, """{"ok":false,"codigo":"DEMASIADAS_SOLICITUDES","mensaje":"Espere."}""")
        }
        assertTrue(ClienteApiLicencias("http://api/api/v1", demasiadas).transferir("A.B", "h", null, "1", true, "op") is RespuestaApi.FallaRed)
    }

    @Test
    fun transferirGuardaElTokenConLaClaveQueTraeYDevuelveElDesvinculado() {
        val token = tokenDeEsteTelefono()
        val transporte = TransporteFalso { _, cuerpo ->
            val solicitud = JSONObject(cuerpo!!)
            assertTrue(solicitud.getBoolean("permitir_desvincular"))
            assertEquals(FabricaTokens.DEVICE_HASH, solicitud.getString("device_hash"))
            RespuestaHttp(
                200,
                """{"ok":true,"token":"$token","valido_hasta":"x","version_usuarios":"v1","dispositivo_desvinculado":{"nombre":"Caja 1"}}""",
            )
        }
        val almacen = AlmacenLicencia(ArchivoEnMemoria())

        val resultado = repositorio(transporte, almacen).transferir("TOKEN.ORIGEN", "Moto G", permitirDesvincular = true, idOperacion = "op-1")

        val exito = resultado as ResultadoTransferencia.Exito
        assertEquals("Caja 1", exito.desvinculado)
        assertEquals("v1", exito.licencia.versionUsuarios)
        assertEquals("K7QX-9M2D-PL4A", almacen.leer().clave)
        assertEquals(token, almacen.leer().token)
    }

    @Test
    fun transferirConCupoNoTraeDesvinculadoYUnRechazoNoGuardaNada() {
        val token = tokenDeEsteTelefono()
        val conCupo = TransporteFalso { _, _ ->
            RespuestaHttp(200, """{"ok":true,"token":"$token","valido_hasta":"x","dispositivo_desvinculado":null}""")
        }
        assertNull((repositorio(conCupo).transferir("T.O", null, false, "op") as ResultadoTransferencia.Exito).desvinculado)

        val almacen = AlmacenLicencia(ArchivoEnMemoria())
        val rechazo = TransporteFalso { _, _ ->
            RespuestaHttp(409, """{"ok":false,"codigo":"LIMITE_DISPOSITIVOS","mensaje":"Su plan ya no tiene espacio para otro teléfono."}""")
        }
        val resultado = repositorio(rechazo, almacen).transferir("T.O", null, false, "op")
        assertEquals("LIMITE_DISPOSITIVOS", (resultado as ResultadoTransferencia.Rechazada).codigo)
        assertNull(almacen.leer().clave)
    }

    @Test
    fun unTokenDeOtroTelefonoNoSeGuarda() {
        val ajeno = FabricaTokens.firmar(
            FabricaTokens.json(deviceHash = "otro-hash", emitido = ahora.toString(), validoHasta = ahora.plus(Duration.ofDays(1)).toString())
        )
        val transporte = TransporteFalso { _, _ -> RespuestaHttp(200, """{"ok":true,"token":"$ajeno","valido_hasta":"x"}""") }
        val almacen = AlmacenLicencia(ArchivoEnMemoria())

        assertTrue(repositorio(transporte, almacen).transferir("T.O", null, true, "op") is ResultadoTransferencia.TokenNoValido)
        assertNull(almacen.leer().clave)
    }

    @Test
    fun licenciaDeTokenSoloConFirmaValida() {
        val repo = repositorio(TransporteFalso { _, _ -> RespuestaHttp(500, "") })
        assertEquals("ABCD-EFGH-JKMN", repo.licenciaDeToken(tokenDeEsteTelefono("ABCD-EFGH-JKMN")))
        assertNull(repo.licenciaDeToken("no.valido"))
    }

    @Test
    fun loteDeEventos() {
        val transporte = TransporteFalso { url, _ ->
            assertEquals("http://api/api/v1/eventos/backup/lote", url)
            RespuestaHttp(200, """{"ok":true,"guardados":2,"rechazados":[{"indice":2,"codigo":"SOLICITUD_INVALIDA","mensaje":"x"}]}""")
        }
        val eventos = JSONArray().put(JSONObject().put("tipo", "backup_creado"))
        assertEquals(RespuestaApi.LoteEventos(2, 1), ClienteApiLicencias("http://api/api/v1", transporte).enviarEventosBackup(eventos))
        assertTrue(repositorio(transporte).enviarEventosBackup(eventos))

        val sinRed = TransporteFalso { _, _ -> throw java.io.IOException("sin red") }
        assertEquals(false, repositorio(sinRed).enviarEventosBackup(eventos))
    }
}
