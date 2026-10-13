package com.typdevstudio.typos_movil.utilidades.licencia.nube

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.PrivateKey
import java.time.Duration
import java.time.Instant

class PruebasRepositorioLicencia {

    private val clave = "K7QX-9M2D-PL4A"
    private var ahora: Instant = Instant.parse("2026-10-13T12:00:00Z")

    /** API falsa: firma tokens para el device_hash que recibe, o devuelve la respuesta programada. */
    private inner class ApiFalsa : TransporteHttp {
        var siguiente: ((String) -> RespuestaHttp)? = null
        var clavePrivada: PrivateKey = FabricaTokens.parLlaves.private
        var deviceHashEnToken: String? = null
        var licenciaEnToken: String? = null
        val rutas = mutableListOf<String>()

        override fun enviar(metodo: String, url: String, cuerpoJson: String?): RespuestaHttp {
            val ruta = url.substringAfterLast('/')
            rutas += ruta
            siguiente?.let { return it(ruta) }
            if (ruta == "desactivar") return RespuestaHttp(200, """{"ok":true,"mensaje":"Dispositivo desactivado."}""")
            val solicitud = JSONObject(cuerpoJson!!)
            val token = FabricaTokens.firmar(
                FabricaTokens.json(
                    licencia = licenciaEnToken ?: solicitud.getString("clave"),
                    deviceHash = deviceHashEnToken ?: solicitud.getString("device_hash"),
                    emitido = ahora.toString(),
                    validoHasta = ahora.plus(Duration.ofDays(15)).toString(),
                ),
                clavePrivada,
            )
            return RespuestaHttp(200, """{"ok":true,"token":"$token","valido_hasta":"x"}""")
        }
    }

    private val api = ApiFalsa()
    private val archivo = ArchivoEnMemoria()
    private val almacen = AlmacenLicencia(archivo)
    private val registro = RegistroEnMemoria()
    private val repositorio = RepositorioLicencia(
        cliente = ClienteApiLicencias("http://api/api/v1", api),
        verificador = FabricaTokens.verificador(),
        almacen = almacen,
        deviceHash = { FabricaTokens.DEVICE_HASH },
        versionApp = "1.1.3",
        reloj = { ahora.toEpochMilli() },
        registro = registro,
    )

    private fun errorNegocio(codigo: String, http: Int = 403): (String) -> RespuestaHttp =
        { RespuestaHttp(http, """{"ok":false,"codigo":"$codigo","mensaje":"Mensaje de $codigo."}""") }

    private fun activar(): ResultadoLicencia {
        api.siguiente = null
        return repositorio.activar(clave, "Samsung A54")
    }

    @Test
    fun activarNormalizaLaClaveVerificaYGuarda() {
        val resultado = repositorio.activar("  k7qx-9m2d -pl4a ", "  Samsung A54  ")

        assertTrue(resultado.toString(), resultado is ResultadoLicencia.Exito)
        assertTrue((resultado as ResultadoLicencia.Exito).estado is EstadoLicencia.Activa)
        val datos = almacen.leer()
        assertEquals(clave, datos.clave)
        assertEquals(ahora.toEpochMilli(), datos.ultimaValidacionMs)
        assertTrue(repositorio.estadoActual() is EstadoLicencia.Activa)
    }

    @Test
    fun claveConFormatoInvalidoNoLlamaALaApi() {
        val resultado = repositorio.activar("K7QX9M2DPL4A", null)
        assertEquals("SOLICITUD_INVALIDA", (resultado as ResultadoLicencia.Rechazada).codigo)
        assertTrue(api.rutas.isEmpty())
    }

    @Test
    fun activacionRechazadaMuestraElMensajeYNoGuardaNada() {
        api.siguiente = errorNegocio("LIMITE_DISPOSITIVOS", 409)
        val resultado = repositorio.activar(clave, null)

        assertEquals(ResultadoLicencia.Rechazada("LIMITE_DISPOSITIVOS", "Mensaje de LIMITE_DISPOSITIVOS."), resultado)
        assertEquals(DatosLicenciaGuardados(null, null, null, null, null), almacen.leer())
    }

    @Test
    fun noGuardaTokensQueNoPasanLosControlesLocales() {
        api.clavePrivada = FabricaTokens.generarParLlaves().private
        assertTrue(repositorio.activar(clave, null) is ResultadoLicencia.TokenNoValido)

        api.clavePrivada = FabricaTokens.parLlaves.private
        api.deviceHashEnToken = "0".repeat(64)
        assertTrue(repositorio.activar(clave, null) is ResultadoLicencia.TokenNoValido)

        api.deviceHashEnToken = null
        api.licenciaEnToken = "OTRA-CLAV-EXXX"
        assertTrue(repositorio.activar(clave, null) is ResultadoLicencia.TokenNoValido)

        assertNull(almacen.leer().token)
    }

    @Test
    fun conElRelojDelTelefonoMuyAtrasadoPideCorregirlo() {
        api.siguiente = {
            val token = FabricaTokens.firmar(FabricaTokens.json(emitido = ahora.plus(Duration.ofDays(2)).toString(), validoHasta = ahora.plus(Duration.ofDays(17)).toString()))
            RespuestaHttp(200, """{"ok":true,"token":"$token"}""")
        }
        val resultado = repositorio.activar(clave, null) as ResultadoLicencia.TokenNoValido
        assertTrue(resultado.mensaje.contains("fecha y hora"))
        assertNull(almacen.leer().token)
    }

    @Test
    fun validarReemplazaElTokenYQuitaElBloqueo() {
        activar()
        almacen.guardarBloqueoServidor(MotivoRestriccion.SUSPENDIDA)
        val tokenAnterior = almacen.leer().token
        ahora = ahora.plus(Duration.ofDays(1))

        assertTrue(repositorio.validar() is ResultadoLicencia.Exito)

        val datos = almacen.leer()
        assertNull(datos.bloqueoServidor)
        assertNull(datos.mensajeBloqueoServidor)
        assertTrue(datos.token != tokenAnterior)
        assertEquals(ahora.toEpochMilli(), datos.ultimaValidacionMs)
    }

    @Test
    fun losErroresDeNegocioDeLaValidacionBloqueanAunqueElTokenSigaVigente() {
        val casos = mapOf(
            "LICENCIA_SUSPENDIDA" to MotivoRestriccion.SUSPENDIDA,
            "LICENCIA_VENCIDA" to MotivoRestriccion.VENCIDA,
            "DISPOSITIVO_REVOCADO" to MotivoRestriccion.REVOCADO,
            "DISPOSITIVO_NO_REGISTRADO" to MotivoRestriccion.SIN_ACTIVAR,
            "LICENCIA_NO_ENCONTRADA" to MotivoRestriccion.SIN_ACTIVAR,
        )
        for ((codigo, motivo) in casos) {
            activar()
            api.siguiente = errorNegocio(codigo)
            assertEquals(ResultadoLicencia.Rechazada(codigo, "Mensaje de $codigo."), repositorio.validar())
            assertEquals(motivo, almacen.leer().bloqueoServidor)
            assertEquals("Mensaje de $codigo.", almacen.leer().mensajeBloqueoServidor)
            assertEquals(motivo, (repositorio.estadoActual() as EstadoLicencia.Restringida).motivo)
        }
    }

    @Test
    fun lasFallasDeRedNuncaCastigan() {
        activar()
        val antes = almacen.leer()
        for (falla in listOf<(String) -> RespuestaHttp>(
            { throw java.net.UnknownHostException("sin red") },
            { RespuestaHttp(500, """{"ok":false,"codigo":"ERROR_INTERNO","mensaje":"x"}""") },
            { RespuestaHttp(429, "") },
            { RespuestaHttp(200, "<html>portal</html>") },
        )) {
            api.siguiente = falla
            assertTrue(repositorio.validar() is ResultadoLicencia.SinConexion)
        }
        api.siguiente = errorNegocio("SOLICITUD_INVALIDA", 400)
        assertTrue(repositorio.validar() is ResultadoLicencia.Rechazada)

        assertEquals(antes, almacen.leer())
        assertTrue(repositorio.estadoActual() is EstadoLicencia.Activa)
    }

    @Test
    fun unTokenInvalidoEnLaValidacionNoReemplazaElBueno() {
        activar()
        val antes = almacen.leer()
        api.clavePrivada = FabricaTokens.generarParLlaves().private
        assertTrue(repositorio.validar() is ResultadoLicencia.TokenNoValido)
        assertEquals(antes, almacen.leer())
    }

    @Test
    fun desactivarLiberaYOlvidaLaLicencia() {
        activar()
        assertEquals(ResultadoLicencia.Exito(EstadoLicencia.SinActivar), repositorio.desactivar())
        assertNull(almacen.leer().clave)
        assertEquals(EstadoLicencia.SinActivar, repositorio.estadoActual())
        assertEquals(ResultadoLicencia.SinLicencia, repositorio.desactivar())
        assertEquals(ResultadoLicencia.SinLicencia, repositorio.validar())
    }

    @Test
    fun desactivarUnDispositivoQueElServidorYaNoTieneTambienOlvida() {
        activar()
        api.siguiente = errorNegocio("DISPOSITIVO_NO_REGISTRADO", 404)
        assertTrue(repositorio.desactivar() is ResultadoLicencia.Exito)
        assertNull(almacen.leer().clave)
    }

    @Test
    fun desactivarSinConexionConservaLaLicencia() {
        activar()
        api.siguiente = { throw java.net.ConnectException("sin red") }
        assertTrue(repositorio.desactivar() is ResultadoLicencia.SinConexion)
        assertEquals(clave, almacen.leer().clave)
    }

    @Test
    fun necesitaValidarCadaDoceHoras() {
        assertEquals(false, repositorio.necesitaValidar()) // sin licencia
        activar()
        assertEquals(false, repositorio.necesitaValidar())
        ahora = ahora.plus(Duration.ofHours(11))
        assertEquals(false, repositorio.necesitaValidar())
        ahora = ahora.plus(Duration.ofHours(1))
        assertEquals(true, repositorio.necesitaValidar())
        ahora = Instant.parse("2026-10-13T11:00:00Z") // reloj antes de la última validación
        assertEquals(true, repositorio.necesitaValidar())
    }

    @Test
    fun elEstadoActualRegistraLaHoraVistaYDetectaRelojAtrasado() {
        activar()
        ahora = ahora.plus(Duration.ofDays(2))
        assertTrue(repositorio.estadoActual() is EstadoLicencia.Activa)
        ahora = ahora.minus(Duration.ofDays(1))
        val estado = repositorio.estadoActual() as EstadoLicencia.Restringida
        assertEquals(MotivoRestriccion.RELOJ_ALTERADO, estado.motivo)
    }

    @Test
    fun losLogsNuncaLlevanLaClaveCompleta() {
        activar()
        api.siguiente = errorNegocio("LICENCIA_SUSPENDIDA")
        repositorio.validar()
        repositorio.desactivar()
        assertTrue(registro.mensajes.isNotEmpty())
        assertTrue(registro.mensajes.any { it.contains("K7QX-****-****") })
        assertTrue(registro.mensajes.none { it.contains(clave) || it.contains("9M2D") })
        assertEquals("K7QX-****-****", RepositorioLicencia.enmascararClave(clave))
        assertEquals("****", RepositorioLicencia.enmascararClave("K7Q"))
    }
}
