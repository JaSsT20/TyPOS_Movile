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
        var versionUsuarios: String? = null
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
            val version = versionUsuarios?.let { ""","version_usuarios":"$it"""" } ?: ""
            return RespuestaHttp(200, """{"ok":true,"token":"$token","valido_hasta":"x"$version}""")
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

    // ---------------- Migración de la licencia antigua ----------------

    @Test
    fun migrarGuardaLaLicenciaNuevaQueTraeElToken() {
        api.licenciaEnToken = clave // el servidor creó esta licencia nueva

        val resultado = repositorio.migrar(" 0F95001FCD859DEC ", "Samsung A54")

        assertTrue(resultado.toString(), resultado is ResultadoLicencia.Exito)
        assertEquals(listOf("migrar"), api.rutas)
        assertEquals(clave, almacen.leer().clave)
        assertTrue(repositorio.estadoActual() is EstadoLicencia.Activa)
        // Con la licencia nueva guardada, el siguiente paso normal es validar con ella.
        api.licenciaEnToken = null
        assertTrue(repositorio.validar() is ResultadoLicencia.Exito)
    }

    @Test
    fun migrarEnviaLaClaveAntiguaYElTelefono() {
        var cuerpo: JSONObject? = null
        val transporte = TransporteHttp { _, _, json ->
            cuerpo = JSONObject(json!!)
            RespuestaHttp(404, """{"ok":false,"codigo":"MIGRACION_NO_DISPONIBLE","mensaje":"No."}""")
        }
        ClienteApiLicencias("http://api/api/v1", transporte).migrar("0F95001FCD859DEC", "hash-1234", "Caja 1", "1.2.1")

        assertEquals("0F95001FCD859DEC", cuerpo!!.getString("clave_antigua"))
        assertEquals("hash-1234", cuerpo!!.getString("device_hash"))
        assertEquals("Caja 1", cuerpo!!.getString("nombre_dispositivo"))
        assertEquals("1.2.1", cuerpo!!.getString("version_app"))
    }

    @Test
    fun migracionRechazadaOSinRedNoGuardaNada() {
        for (respuesta in listOf(
            errorNegocio("MIGRACION_NO_DISPONIBLE", 404),
            errorNegocio("LICENCIA_VENCIDA", 403),
            { _: String -> throw java.net.UnknownHostException("sin red") },
        )) {
            api.siguiente = respuesta
            val resultado = repositorio.migrar("0F95001FCD859DEC", null)
            assertTrue(resultado.toString(), resultado is ResultadoLicencia.Rechazada || resultado is ResultadoLicencia.SinConexion)
        }
        assertNull(almacen.leer().clave)
        assertNull(almacen.leer().bloqueoServidor)
        assertEquals(EstadoLicencia.SinActivar, repositorio.estadoActual())
    }

    @Test
    fun migrarNoAceptaTokensFirmadosPorOtraLlave() {
        api.licenciaEnToken = clave
        api.clavePrivada = FabricaTokens.generarParLlaves().private

        assertTrue(repositorio.migrar("0F95001FCD859DEC", null) is ResultadoLicencia.TokenNoValido)
        assertNull(almacen.leer().clave)
    }

    @Test
    fun conLicenciaNuevaYaGuardadaNoSeMigra() {
        activar()
        api.rutas.clear()

        assertTrue(repositorio.migrar("0F95001FCD859DEC", null) is ResultadoLicencia.Exito)
        assertTrue(api.rutas.isEmpty())
    }

    // ---------------- Versión de usuarios ----------------

    @Test
    fun laValidacionTraeLaVersionDeUsuariosDelServidor() {
        activar()
        api.versionUsuarios = "a1b2c3d4e5f60718"

        val resultado = repositorio.validar()

        assertEquals("a1b2c3d4e5f60718", (resultado as ResultadoLicencia.Exito).versionUsuarios)
        repositorio.guardarVersionUsuarios(resultado.versionUsuarios)
        assertEquals("a1b2c3d4e5f60718", almacen.leer().versionUsuarios)
    }

    // ---------------- Registro de un negocio nuevo ----------------

    private val datosRegistro = DatosRegistro(nombreNegocio = "Colmado Juan", telefono = "809-555-0101", contactoNombre = "Juan")

    private fun respuestaRegistro(estado: String, mensaje: String = "Mensaje de $estado.", token: String? = null): (String) -> RespuestaHttp = {
        val campoToken = token?.let { ""","token":"$it","version_usuarios":"v1"""" } ?: ""
        RespuestaHttp(200, """{"ok":true,"estado":"$estado","mensaje":"$mensaje"$campoToken}""")
    }

    private fun tokenDeLaLicencia(): String = FabricaTokens.firmar(
        FabricaTokens.json(
            licencia = clave, deviceHash = FabricaTokens.DEVICE_HASH,
            emitido = ahora.toString(), validoHasta = ahora.plus(Duration.ofDays(15)).toString(),
        )
    )

    @Test
    fun registrarDejaElRegistroPendienteSinLicencia() {
        api.siguiente = respuestaRegistro("pendiente")

        val resultado = repositorio.registrar(datosRegistro, "Caja 1")

        assertTrue(resultado.toString(), resultado is ResultadoRegistro.Pendiente)
        assertEquals(listOf("registro"), api.rutas)
        assertEquals("pendiente", almacen.leer().estadoRegistro)
        assertNull(almacen.leer().clave)
        assertEquals(EstadoLicencia.SinActivar, repositorio.estadoActual())
    }

    @Test
    fun registrarEnviaLosDatosDelNegocio() {
        var cuerpo: JSONObject? = null
        val transporte = TransporteHttp { _, _, json ->
            cuerpo = JSONObject(json!!)
            RespuestaHttp(200, """{"ok":true,"estado":"pendiente","mensaje":"x"}""")
        }
        ClienteApiLicencias("http://api/api/v1", transporte)
            .registrar(datosRegistro.copy(rnc = "101-1"), "hash-1234", "Caja 1", "1.2.3")

        assertEquals("Colmado Juan", cuerpo!!.getString("nombre_negocio"))
        assertEquals("809-555-0101", cuerpo!!.getString("telefono"))
        assertEquals("101-1", cuerpo!!.getString("rnc"))
        assertEquals("Juan", cuerpo!!.getString("contacto_nombre"))
        assertTrue(cuerpo!!.isNull("email"))
        assertEquals("hash-1234", cuerpo!!.getString("device_hash"))
    }

    @Test
    fun cuandoElProveedorAsignaLaLicenciaElTelefonoQuedaActivado() {
        almacen.guardarRegistro("pendiente")
        api.siguiente = respuestaRegistro("asignado", token = tokenDeLaLicencia())

        val resultado = repositorio.consultarRegistro("Caja 1")

        val licencia = (resultado as ResultadoRegistro.Asignado).licencia
        assertTrue(licencia.toString(), licencia is ResultadoLicencia.Exito)
        assertEquals("v1", (licencia as ResultadoLicencia.Exito).versionUsuarios)
        assertEquals(listOf("estado"), api.rutas)
        assertEquals(clave, almacen.leer().clave)
        assertNull(almacen.leer().estadoRegistro)
        assertTrue(repositorio.estadoActual() is EstadoLicencia.Activa)
    }

    @Test
    fun unTokenDeOtraLlaveNoActivaElRegistro() {
        almacen.guardarRegistro("pendiente")
        val ajeno = FabricaTokens.firmar(FabricaTokens.json(licencia = clave), FabricaTokens.generarParLlaves().private)
        api.siguiente = respuestaRegistro("asignado", token = ajeno)

        val resultado = repositorio.consultarRegistro(null)

        assertTrue((resultado as ResultadoRegistro.Asignado).licencia is ResultadoLicencia.TokenNoValido)
        assertNull(almacen.leer().clave)
        assertEquals("pendiente", almacen.leer().estadoRegistro)
    }

    @Test
    fun registroRechazadoGuardaElMotivo() {
        almacen.guardarRegistro("pendiente")
        api.siguiente = respuestaRegistro("rechazado", mensaje = "Su registro no fue aprobado. Datos incompletos.")

        assertTrue(repositorio.consultarRegistro(null) is ResultadoRegistro.Rechazado)
        assertEquals("rechazado", almacen.leer().estadoRegistro)
        assertEquals("Su registro no fue aprobado. Datos incompletos.", almacen.leer().mensajeRegistro)
    }

    @Test
    fun sinRegistroGuardadoNoSeConsultaLaApi() {
        assertEquals(ResultadoRegistro.SinRegistro, repositorio.consultarRegistro(null))
        assertTrue(api.rutas.isEmpty())
    }

    @Test
    fun siElServidorYaNoTieneElRegistroSeOlvida() {
        almacen.guardarRegistro("pendiente")
        api.siguiente = errorNegocio("REGISTRO_NO_ENCONTRADO", 404)

        assertEquals(ResultadoRegistro.SinRegistro, repositorio.consultarRegistro(null))
        assertNull(almacen.leer().estadoRegistro)
    }

    @Test
    fun sinRedElRegistroSigueIgual() {
        almacen.guardarRegistro("pendiente")
        api.siguiente = { throw java.net.UnknownHostException("sin red") }

        assertTrue(repositorio.consultarRegistro(null) is ResultadoRegistro.SinConexion)
        assertEquals("pendiente", almacen.leer().estadoRegistro)
    }

    @Test
    fun respuestasDeRegistroMalFormadasNoCastigan() {
        val cliente = ClienteApiLicencias("http://api/api/v1", api)
        api.siguiente = { RespuestaHttp(200, """{"ok":true,"estado":"otro","mensaje":"x"}""") }
        assertTrue(cliente.estadoRegistro("h", null, "1") is RespuestaApi.FallaRed)
        api.siguiente = { RespuestaHttp(200, """{"ok":true,"estado":"asignado","mensaje":"x"}""") }
        assertTrue(cliente.estadoRegistro("h", null, "1") is RespuestaApi.FallaRed)
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
