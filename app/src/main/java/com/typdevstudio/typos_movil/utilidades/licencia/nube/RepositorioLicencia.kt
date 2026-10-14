package com.typdevstudio.typos_movil.utilidades.licencia.nube

import android.content.Context
import android.util.Log
import com.typdevstudio.typos_movil.BuildConfig

/** Resultado de activar, validar o desactivar, listo para mostrar al usuario. */
sealed interface ResultadoLicencia {
    /** La operación se completó; [estado] es el estado resultante. */
    data class Exito(val estado: EstadoLicencia) : ResultadoLicencia

    /** La API rechazó la operación. [mensaje] viene de la API, en español y para el cliente final. */
    data class Rechazada(val codigo: String, val mensaje: String) : ResultadoLicencia

    /** No se pudo hablar con la API. No cambia nada guardado: se reintenta después. */
    data class SinConexion(val mensaje: String) : ResultadoLicencia

    /** La API respondió un token que no pasó los controles locales. No se guarda ni castiga. */
    data class TokenNoValido(val mensaje: String) : ResultadoLicencia

    /** No hay una licencia nueva guardada en este teléfono. */
    data object SinLicencia : ResultadoLicencia
}

/** Resultado de una operación de usuarios contra la API. */
sealed interface ResultadoUsuarios {
    /** La lista completa y vigente de usuarios de la licencia. */
    data class Exito(val lista: ListaUsuariosRemota) : ResultadoUsuarios

    /** La API rechazó la operación (límite del plan, usuario repetido...). [mensaje] es para el usuario. */
    data class Rechazada(val codigo: String, val mensaje: String) : ResultadoUsuarios

    data class SinConexion(val mensaje: String) : ResultadoUsuarios

    /** El teléfono no tiene licencia nueva: los usuarios todavía se manejan solo en el teléfono. */
    data object SinLicencia : ResultadoUsuarios
}

/** Bitácora de la licencia. Nunca recibe claves completas ni tokens. */
interface RegistroLicencia {
    fun info(mensaje: String)
    fun advertencia(mensaje: String)

    object Android : RegistroLicencia {
        private const val ETIQUETA = "LicenciaNube"
        override fun info(mensaje: String) { Log.i(ETIQUETA, mensaje) }
        override fun advertencia(mensaje: String) { Log.w(ETIQUETA, mensaje) }
    }
}

/**
 * Activa, valida y desactiva contra la API, verifica cada token antes de guardarlo y decide el estado.
 *
 * Regla de oro: los errores de negocio que restringen (suspendida, vencida, revocado, no registrado)
 * se guardan como bloqueo del servidor; las fallas de red, 5xx y 429 nunca cambian nada.
 *
 * Las operaciones bloquean el hilo (red y disco): usarlas fuera del hilo principal.
 */
class RepositorioLicencia(
    private val cliente: ClienteApiLicencias,
    private val verificador: VerificadorToken,
    private val almacen: AlmacenLicencia,
    private val deviceHash: () -> String,
    private val versionApp: String,
    private val reloj: () -> Long = System::currentTimeMillis,
    private val registro: RegistroLicencia = RegistroLicencia.Android,
) {

    @Synchronized
    fun activar(claveIngresada: String, nombreDispositivo: String?): ResultadoLicencia {
        val clave = normalizarClave(claveIngresada)
        if (!FORMATO_CLAVE.matches(clave)) {
            return ResultadoLicencia.Rechazada("SOLICITUD_INVALIDA", "La clave de licencia debe tener el formato XXXX-XXXX-XXXX.")
        }
        val nombre = nombreDispositivo?.trim()?.take(MAXIMO_NOMBRE_DISPOSITIVO)?.takeIf { it.isNotEmpty() }

        return when (val respuesta = cliente.activar(clave, deviceHash(), nombre, versionApp.take(MAXIMO_VERSION))) {
            is RespuestaApi.Token -> aceptarToken(clave, respuesta.token, "activación")
            is RespuestaApi.ErrorNegocio -> {
                // En la activación el error solo se muestra: todavía no hay nada que bloquear.
                registro.advertencia("Activación rechazada (${enmascararClave(clave)}): ${respuesta.codigo}")
                ResultadoLicencia.Rechazada(respuesta.codigo, respuesta.mensaje)
            }
            is RespuestaApi.FallaRed -> sinConexion("activación", respuesta)
            // Respuestas que no corresponden a este endpoint.
            RespuestaApi.Ok, is RespuestaApi.Usuarios -> ResultadoLicencia.SinConexion(MENSAJE_SIN_CONEXION)
        }
    }

    @Synchronized
    fun validar(): ResultadoLicencia {
        val clave = almacen.leer().clave ?: return ResultadoLicencia.SinLicencia

        return when (val respuesta = cliente.validar(clave, deviceHash(), versionApp.take(MAXIMO_VERSION))) {
            is RespuestaApi.Token -> aceptarToken(clave, respuesta.token, "validación")
            is RespuestaApi.ErrorNegocio -> {
                val motivo = MotivoRestriccion.desdeCodigoServidor(respuesta.codigo)
                if (motivo != null) {
                    almacen.guardarBloqueoServidor(motivo, respuesta.mensaje)
                    registro.advertencia("Validación: el servidor bloqueó la licencia ${enmascararClave(clave)} (${respuesta.codigo}).")
                } else {
                    registro.advertencia("Validación rechazada sin bloqueo (${enmascararClave(clave)}): ${respuesta.codigo}")
                }
                ResultadoLicencia.Rechazada(respuesta.codigo, respuesta.mensaje)
            }
            is RespuestaApi.FallaRed -> sinConexion("validación", respuesta)
            // Respuestas que no corresponden a este endpoint.
            RespuestaApi.Ok, is RespuestaApi.Usuarios -> ResultadoLicencia.SinConexion(MENSAJE_SIN_CONEXION)
        }
    }

    /** Libera el cupo de este teléfono en el servidor y olvida la licencia local. */
    @Synchronized
    fun desactivar(): ResultadoLicencia {
        val clave = almacen.leer().clave ?: return ResultadoLicencia.SinLicencia

        return when (val respuesta = cliente.desactivar(clave, deviceHash())) {
            RespuestaApi.Ok -> olvidarLicencia(clave)
            is RespuestaApi.ErrorNegocio -> when (respuesta.codigo) {
                // El servidor ya no tiene este teléfono en esa licencia: el cupo ya está libre.
                "LICENCIA_NO_ENCONTRADA", "DISPOSITIVO_NO_REGISTRADO" -> olvidarLicencia(clave)
                else -> ResultadoLicencia.Rechazada(respuesta.codigo, respuesta.mensaje)
            }
            is RespuestaApi.FallaRed -> sinConexion("desactivación", respuesta)
            is RespuestaApi.Token, is RespuestaApi.Usuarios -> ResultadoLicencia.SinConexion(MENSAJE_SIN_CONEXION)
        }
    }

    /** Estado actual según lo guardado y el reloj. También actualiza la hora más alta vista. */
    @Synchronized
    fun estadoActual(): EstadoLicencia {
        val ahora = reloj()
        val datos = almacen.leer()
        val estado = EvaluadorEstadoLicencia.evaluar(
            token = datos.token?.let(verificador::verificar),
            deviceHashLocal = deviceHash(),
            ahoraMs = ahora,
            bloqueoServidor = datos.bloqueoServidor,
            ultimaFechaVistaMs = datos.ultimaFechaVistaMs,
        )
        almacen.registrarFechaVista(ahora)
        return estado
    }

    // ------------------------------------------------------------------
    // Usuarios de la licencia (comparten la regla de oro: red caída no castiga)
    // ------------------------------------------------------------------

    @Synchronized
    fun listarUsuarios(): ResultadoUsuarios = operarUsuarios("listar usuarios") { clave, hash ->
        cliente.listarUsuarios(clave, hash)
    }

    @Synchronized
    fun crearUsuario(usuario: UsuarioRemoto): ResultadoUsuarios = operarUsuarios("crear usuario") { clave, hash ->
        cliente.crearUsuario(clave, hash, usuario)
    }

    @Synchronized
    fun actualizarUsuario(
        nombreUsuario: String,
        nombreCompleto: String? = null,
        rol: String? = null,
        hashClave: String? = null,
        activo: Boolean? = null,
    ): ResultadoUsuarios = operarUsuarios("actualizar usuario") { clave, hash ->
        cliente.actualizarUsuario(clave, hash, nombreUsuario, nombreCompleto, rol, hashClave, activo)
    }

    @Synchronized
    fun importarUsuarios(usuarios: List<UsuarioRemoto>): ResultadoUsuarios = operarUsuarios("importar usuarios") { clave, hash ->
        cliente.importarUsuarios(clave, hash, usuarios)
    }

    private fun operarUsuarios(operacion: String, llamada: (String, String) -> RespuestaApi): ResultadoUsuarios {
        val clave = almacen.leer().clave ?: return ResultadoUsuarios.SinLicencia
        return when (val respuesta = llamada(clave, deviceHash())) {
            is RespuestaApi.Usuarios -> ResultadoUsuarios.Exito(respuesta.lista)
            is RespuestaApi.ErrorNegocio -> {
                // Un bloqueo comunicado aquí vale igual que en /validar (suspendida, revocado...).
                MotivoRestriccion.desdeCodigoServidor(respuesta.codigo)?.let { almacen.guardarBloqueoServidor(it, respuesta.mensaje) }
                registro.advertencia("$operacion rechazado (${enmascararClave(clave)}): ${respuesta.codigo}")
                ResultadoUsuarios.Rechazada(respuesta.codigo, respuesta.mensaje)
            }
            is RespuestaApi.FallaRed -> {
                registro.advertencia("$operacion sin conexión: ${respuesta.detalle}")
                ResultadoUsuarios.SinConexion(MENSAJE_SIN_CONEXION)
            }
            else -> ResultadoUsuarios.SinConexion(MENSAJE_SIN_CONEXION)
        }
    }

    /** Lo guardado (clave, última validación...), para mostrar en la pantalla de licencia. */
    fun datosGuardados(): DatosLicenciaGuardados = almacen.leer()

    /** `device_hash` de este teléfono. */
    fun deviceHashActual(): String = deviceHash()

    /**
     * `true` si hay licencia guardada y pasaron más de 12 horas desde la última validación exitosa
     * (o el reloj quedó antes de esa validación).
     */
    fun necesitaValidar(): Boolean {
        val datos = almacen.leer()
        if (datos.clave == null) return false
        val ultima = datos.ultimaValidacionMs ?: return true
        val ahora = reloj()
        return ahora - ultima >= INTERVALO_VALIDACION_MS || ahora < ultima
    }

    /** Verifica el token recibido (firma, clave, dispositivo y reloj) y solo entonces lo guarda. */
    private fun aceptarToken(clave: String, token: String, operacion: String): ResultadoLicencia {
        val ahora = reloj()
        val verificacion = verificador.verificar(token)
        val payload = (verificacion as? ResultadoVerificacion.Valido)?.payload
        if (payload == null || payload.licencia != clave) {
            registro.advertencia("$operacion: token rechazado (${enmascararClave(clave)}): ${(verificacion as? ResultadoVerificacion.Invalido)?.motivo ?: "clave distinta"}")
            return ResultadoLicencia.TokenNoValido(MENSAJE_TOKEN_NO_VALIDO)
        }

        // Se evalúa sin bloqueo ni fecha vista previos: una validación exitosa los restablece.
        val estado = EvaluadorEstadoLicencia.evaluar(verificacion, deviceHash(), ahora, null, null)
        if (estado is EstadoLicencia.Restringida) {
            registro.advertencia("$operacion: token no aplicable en este teléfono (${enmascararClave(clave)}): ${estado.motivo}")
            return ResultadoLicencia.TokenNoValido(
                when (estado.motivo) {
                    MotivoRestriccion.RELOJ_ALTERADO, MotivoRestriccion.TOKEN_EXPIRADO -> MENSAJE_RELOJ
                    else -> MENSAJE_TOKEN_NO_VALIDO
                }
            )
        }

        almacen.guardarValidacionExitosa(clave, token, ahora)
        registro.info("$operacion exitosa (${enmascararClave(clave)}).")
        return ResultadoLicencia.Exito(estado)
    }

    private fun olvidarLicencia(clave: String): ResultadoLicencia {
        almacen.borrarLicencia()
        registro.info("Dispositivo desactivado (${enmascararClave(clave)}).")
        return ResultadoLicencia.Exito(EstadoLicencia.SinActivar)
    }

    private fun sinConexion(operacion: String, falla: RespuestaApi.FallaRed): ResultadoLicencia {
        registro.advertencia("$operacion sin conexión: ${falla.detalle}")
        return ResultadoLicencia.SinConexion(MENSAJE_SIN_CONEXION)
    }

    companion object {
        const val INTERVALO_VALIDACION_MS = 12L * 60 * 60 * 1000

        private val FORMATO_CLAVE = Regex("[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}")
        private const val MAXIMO_NOMBRE_DISPOSITIVO = 100
        private const val MAXIMO_VERSION = 50

        private const val MENSAJE_SIN_CONEXION =
            "No se pudo conectar con el servidor de licencias. Revisa tu conexión a internet e intenta de nuevo."
        private const val MENSAJE_TOKEN_NO_VALIDO =
            "La respuesta del servidor de licencias no es válida para este teléfono. Actualiza la app o contacta a soporte."
        private const val MENSAJE_RELOJ =
            "La fecha y hora del teléfono no son correctas. Corrígelas (activa la hora automática) e intenta de nuevo."

        /** Mayúsculas y sin espacios, como la normaliza el servidor. */
        fun normalizarClave(clave: String): String = clave.filterNot { it.isWhitespace() }.uppercase()

        /** `K7QX-9M2D-PL4A` → `K7QX-****-****`, para los logs. */
        fun enmascararClave(clave: String): String = if (clave.length <= 4) "****" else clave.take(4) + "-****-****"

        /** Repositorio real, con la URL y la clave pública de `BuildConfig`. */
        fun crear(contexto: Context): RepositorioLicencia {
            val aplicacion = contexto.applicationContext
            val almacen = AlmacenLicencia.crear(aplicacion)
            return RepositorioLicencia(
                cliente = ClienteApiLicencias(BuildConfig.URL_API_LICENCIAS),
                verificador = VerificadorToken.desdeClavePublicaBase64(BuildConfig.CLAVE_PUBLICA_LICENCIAS),
                almacen = almacen,
                deviceHash = { HuellaDispositivo.obtener(aplicacion, almacen) },
                versionApp = BuildConfig.VERSION_NAME,
            )
        }
    }
}
