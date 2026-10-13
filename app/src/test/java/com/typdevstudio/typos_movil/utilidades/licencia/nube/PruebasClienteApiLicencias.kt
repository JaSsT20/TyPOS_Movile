package com.typdevstudio.typos_movil.utilidades.licencia.nube

import com.sun.net.httpserver.HttpServer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.InetSocketAddress
import java.net.SocketTimeoutException

class PruebasClienteApiLicencias {

    private class TransporteFalso(var respuesta: () -> RespuestaHttp) : TransporteHttp {
        val solicitudes = mutableListOf<Triple<String, String, String?>>()
        override fun enviar(metodo: String, url: String, cuerpoJson: String?): RespuestaHttp {
            solicitudes += Triple(metodo, url, cuerpoJson)
            return respuesta()
        }
    }

    private fun cliente(codigo: Int, cuerpo: String) =
        ClienteApiLicencias("http://api/api/v1", TransporteFalso { RespuestaHttp(codigo, cuerpo) })

    @Test
    fun enviaLosCamposDelContratoEnSnakeCase() {
        val transporte = TransporteFalso { RespuestaHttp(200, """{"ok":true,"token":"P.F","valido_hasta":"2026-10-27T22:00:00Z"}""") }
        val cliente = ClienteApiLicencias("http://api/api/v1/", transporte)

        assertEquals(RespuestaApi.Token("P.F"), cliente.activar("K7QX-9M2D-PL4A", "ab12", "Samsung A54", "1.1.3"))
        cliente.validar("K7QX-9M2D-PL4A", "ab12", "1.1.3")
        cliente.activar("K7QX-9M2D-PL4A", "ab12", null, "1.1.3")

        val (metodo, url, cuerpo) = transporte.solicitudes[0]
        assertEquals("POST", metodo)
        assertEquals("http://api/api/v1/activar", url)
        val json = JSONObject(cuerpo!!)
        assertEquals("K7QX-9M2D-PL4A", json.getString("clave"))
        assertEquals("ab12", json.getString("device_hash"))
        assertEquals("Samsung A54", json.getString("nombre_dispositivo"))
        assertEquals("1.1.3", json.getString("version_app"))

        assertEquals("http://api/api/v1/validar", transporte.solicitudes[1].second)
        assertEquals(setOf("clave", "device_hash", "version_app"), JSONObject(transporte.solicitudes[1].third!!).keys().asSequence().toSet())
        assertTrue(JSONObject(transporte.solicitudes[2].third!!).isNull("nombre_dispositivo"))
    }

    @Test
    fun desactivarExitosoEsOk() {
        assertEquals(RespuestaApi.Ok, cliente(200, """{"ok":true,"mensaje":"Dispositivo desactivado."}""").desactivar("K7QX-9M2D-PL4A", "ab12"))
    }

    @Test
    fun losErroresDeNegocioTraenCodigoYMensaje() {
        val respuesta = cliente(403, """{"ok":false,"codigo":"LICENCIA_SUSPENDIDA","mensaje":"La licencia está suspendida."}""")
            .validar("K7QX-9M2D-PL4A", "ab12", "1.1.3")
        assertEquals(RespuestaApi.ErrorNegocio("LICENCIA_SUSPENDIDA", "La licencia está suspendida.", 403), respuesta)
    }

    @Test
    fun fallasTecnicasSonFallaDeRed() {
        val casos = listOf(
            500 to """{"ok":false,"codigo":"ERROR_INTERNO","mensaje":"Error interno."}""",
            503 to "Service Unavailable",
            429 to """{"ok":false,"codigo":"DEMASIADAS_SOLICITUDES","mensaje":"Espere."}""",
            200 to "<html>Inicia sesión en el Wi-Fi del hotel</html>", // portal cautivo
            200 to """{"ok":true}""", // sin token
            404 to "<html>Not Found</html>",
            400 to """{"ok":false}""",
        )
        for ((codigo, cuerpo) in casos) {
            val respuesta = cliente(codigo, cuerpo).validar("K7QX-9M2D-PL4A", "ab12", "1.1.3")
            assertTrue("$codigo $cuerpo -> $respuesta", respuesta is RespuestaApi.FallaRed)
        }
        val sinRed = ClienteApiLicencias("http://api/api/v1", TransporteFalso { throw SocketTimeoutException("timeout") })
        assertTrue(sinRed.activar("K7QX-9M2D-PL4A", "ab12", null, "1.1.3") is RespuestaApi.FallaRed)
    }

    @Test
    fun transporteRealEnviaYLeeRespuestasYErrores() {
        val servidor = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        servidor.createContext("/api/v1/validar") { intercambio ->
            val recibido = intercambio.requestBody.readBytes().toString(Charsets.UTF_8)
            val tipo = intercambio.requestHeaders.getFirst("Content-Type")
            val (codigo, cuerpo) = if (JSONObject(recibido).getString("clave") == "K7QX-9M2D-PL4A" && tipo.startsWith("application/json")) {
                200 to """{"ok":true,"token":"P.F","valido_hasta":"2026-10-27T22:00:00Z"}"""
            } else {
                403 to """{"ok":false,"codigo":"LICENCIA_VENCIDA","mensaje":"La licencia venció el 2026-10-01 y terminó su período de cortesía."}"""
            }
            val bytes = cuerpo.toByteArray(Charsets.UTF_8)
            intercambio.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
            intercambio.sendResponseHeaders(codigo, bytes.size.toLong())
            intercambio.responseBody.use { it.write(bytes) }
        }
        servidor.createContext("/api/v1/salud") { intercambio ->
            val bytes = """{"estado":"ok"}""".toByteArray()
            intercambio.sendResponseHeaders(200, bytes.size.toLong())
            intercambio.responseBody.use { it.write(bytes) }
        }
        servidor.createContext("/api/v1/activar") { Thread.sleep(2_000) }
        servidor.start()
        try {
            val url = "http://127.0.0.1:${servidor.address.port}/api/v1"
            val cliente = ClienteApiLicencias(url, TransporteHttpUrlConnection(timeoutMs = 500))

            assertEquals(RespuestaApi.Token("P.F"), cliente.validar("K7QX-9M2D-PL4A", "ab12", "1.1.3"))
            val error = cliente.validar("ZZZZ-ZZZZ-ZZZZ", "ab12", "1.1.3") as RespuestaApi.ErrorNegocio
            assertEquals("LICENCIA_VENCIDA", error.codigo)
            assertTrue(error.mensaje.contains("período de cortesía"))
            assertTrue(cliente.salud())
            assertTrue(cliente.activar("K7QX-9M2D-PL4A", "ab12", null, "1.1.3") is RespuestaApi.FallaRed)
        } finally {
            servidor.stop(0)
        }
    }

    @Test
    fun saludSinServidorEsFalso() {
        val cliente = ClienteApiLicencias("http://api", TransporteFalso { throw IOException("sin red") })
        assertFalse(cliente.salud())
    }
}
