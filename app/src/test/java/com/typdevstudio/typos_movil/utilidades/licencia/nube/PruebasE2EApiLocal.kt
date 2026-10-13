package com.typdevstudio.typos_movil.utilidades.licencia.nube

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Flujo completo contra una API de licencias real (local). Se salta si no están las variables:
 * - `LICENCIAS_E2E_URL`: por ejemplo `http://localhost:5109/api/v1`
 * - `LICENCIAS_E2E_CLAVE_PUBLICA`: contenido de `clave_publica_base64.txt` de la llave con que firma la API
 * - `LICENCIAS_E2E_ADMIN`: valor del header `X-Clave-Admin`
 *
 * Crea un cliente y una licencia de prueba con la API de administración (no se pueden borrar por la API).
 */
class PruebasE2EApiLocal {

    private val url = System.getenv("LICENCIAS_E2E_URL").orEmpty()
    private val clavePublica = System.getenv("LICENCIAS_E2E_CLAVE_PUBLICA").orEmpty()
    private val claveAdmin = System.getenv("LICENCIAS_E2E_ADMIN").orEmpty()

    private fun admin(metodo: String, ruta: String, cuerpo: JSONObject? = null): String {
        val conexion = URL("$url/admin$ruta").openConnection() as HttpURLConnection
        conexion.requestMethod = metodo
        conexion.setRequestProperty("X-Clave-Admin", claveAdmin)
        if (cuerpo != null) {
            conexion.doOutput = true
            conexion.setRequestProperty("Content-Type", "application/json")
            conexion.outputStream.use { it.write(cuerpo.toString().toByteArray()) }
        }
        val codigo = conexion.responseCode
        val texto = (if (codigo >= 400) conexion.errorStream else conexion.inputStream).use { it.readBytes().toString(Charsets.UTF_8) }
        assertTrue("$metodo $ruta -> $codigo $texto", codigo in 200..299)
        return texto
    }

    @Test
    fun flujoCompletoContraLaApi() {
        assumeTrue("Variables LICENCIAS_E2E_* no definidas", url.isNotBlank() && clavePublica.isNotBlank() && claveAdmin.isNotBlank())

        val planes = JSONArray(admin("GET", "/planes"))
        val planBasico = (0 until planes.length()).map { planes.getJSONObject(it) }.first { it.getString("codigo") == "basico" }
        val cliente = JSONObject(admin("POST", "/clientes", JSONObject().put("nombre", "E2E App Movil")))
        val licencia = JSONObject(
            admin(
                "POST", "/licencias",
                JSONObject()
                    .put("cliente_id", cliente.getString("id"))
                    .put("plan_id", planBasico.getString("id"))
                    .put("fecha_vencimiento", "2027-12-31")
                    .put("notas", "Prueba E2E de la app móvil")
            )
        )
        val idLicencia = licencia.getString("id")
        val clave = licencia.getString("clave")

        val almacen = AlmacenLicencia(ArchivoEnMemoria())
        val deviceHash = UUID.randomUUID().toString().replace("-", "").repeat(2)
        val repositorio = RepositorioLicencia(
            cliente = ClienteApiLicencias(url),
            verificador = VerificadorToken.desdeClavePublicaBase64(clavePublica),
            almacen = almacen,
            deviceHash = { deviceHash },
            versionApp = "1.1.3-e2e",
            registro = RegistroEnMemoria(),
        )

        // Activar con la clave en minúsculas y espacios: la app la normaliza.
        val activacion = repositorio.activar(" ${clave.lowercase()} ", "Emulador E2E")
        assertTrue(activacion.toString(), activacion is ResultadoLicencia.Exito)
        val estado = repositorio.estadoActual() as EstadoLicencia.Activa
        assertEquals("E2E App Movil", estado.payload.cliente)
        assertEquals("basico", estado.payload.plan)
        assertEquals(listOf("inventario", "facturacion"), estado.payload.modulos)
        assertEquals(2, estado.payload.maxUsuarios)
        assertEquals(HoraRD.parsearFecha("2027-12-31"), estado.payload.vence)

        // Plan básico = 1 dispositivo: otro teléfono no entra.
        val otro = RepositorioLicencia(
            ClienteApiLicencias(url), VerificadorToken.desdeClavePublicaBase64(clavePublica),
            AlmacenLicencia(ArchivoEnMemoria()), { "f".repeat(64) }, "1.1.3-e2e", registro = RegistroEnMemoria(),
        )
        assertEquals("LIMITE_DISPOSITIVOS", (otro.activar(clave, "Otro") as ResultadoLicencia.Rechazada).codigo)

        assertTrue(repositorio.validar() is ResultadoLicencia.Exito)

        // Suspender en el servidor: la próxima validación bloquea aunque el token siga vigente.
        admin("POST", "/licencias/$idLicencia/suspender")
        val rechazo = repositorio.validar() as ResultadoLicencia.Rechazada
        assertEquals("LICENCIA_SUSPENDIDA", rechazo.codigo)
        assertEquals(MotivoRestriccion.SUSPENDIDA, (repositorio.estadoActual() as EstadoLicencia.Restringida).motivo)

        // Reactivar: "Validar ahora" quita el bloqueo.
        admin("POST", "/licencias/$idLicencia/reactivar")
        assertTrue(repositorio.validar() is ResultadoLicencia.Exito)
        assertTrue(repositorio.estadoActual() is EstadoLicencia.Activa)

        // Desactivar libera el cupo: el otro teléfono ya puede activarse.
        assertTrue(repositorio.desactivar() is ResultadoLicencia.Exito)
        assertEquals(EstadoLicencia.SinActivar, repositorio.estadoActual())
        assertTrue(otro.activar(clave, "Otro") is ResultadoLicencia.Exito)
        assertTrue(otro.desactivar() is ResultadoLicencia.Exito)
    }
}
