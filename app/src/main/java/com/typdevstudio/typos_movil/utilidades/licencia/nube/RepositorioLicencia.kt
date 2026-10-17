package com.typdevstudio.typos_movil.utilidades.licencia.nube

import android.content.Context
import android.util.Log
import com.typdevstudio.typos_movil.BuildConfig

/** Resultado de activar, validar o desactivar, listo para mostrar al usuario. */
sealed interface ResultadoLicencia {
    /**
     * La operación se completó; [estado] es el estado resultante. [versionUsuarios] es la huella de los usuarios de la
     * licencia que informó la API (`null` si no la envió): si difiere de la aplicada, hay que sincronizar usuarios.
     */
    data class Exito(val estado: EstadoLicencia, val versionUsuarios: String? = null) : ResultadoLicencia

    /** La API rechazó la operación. [mensaje] viene de la API, en español y para el cliente final. */
    data class Rechazada(val codigo: String, val mensaje: String) : ResultadoLicencia

    /** No se pudo hablar con la API. No cambia nada guardado: se reintenta después. */
    data class SinConexion(val mensaje: String) : ResultadoLicencia

    /** La API respondió un token que no pasó los controles locales. No se guarda ni castiga. */
    data class TokenNoValido(val mensaje: String) : ResultadoLicencia

    /** No hay una licencia nueva guardada en este teléfono. */
    data object SinLicencia : ResultadoLicencia
}

/** Resultado de registrar el negocio o de consultar cómo va el registro. */
sealed interface ResultadoRegistro {
    /** El proveedor todavía no asigna la licencia: el teléfono sigue en modo consulta. */
    data class Pendiente(val mensaje: String) : ResultadoRegistro

    /** El proveedor rechazó el registro. [mensaje] trae el motivo, si lo dio. */
    data class Rechazado(val mensaje: String) : ResultadoRegistro

    /** Ya hay licencia: [licencia] es el resultado de aceptar su token (normalmente [ResultadoLicencia.Exito]). */
    data class Asignado(val licencia: ResultadoLicencia) : ResultadoRegistro

    /** La API rechazó la solicitud (datos inválidos, teléfono ya activado...). [mensaje] es para el usuario. */
    data class Rechazada(val codigo: String, val mensaje: String) : ResultadoRegistro

    data class SinConexion(val mensaje: String) : ResultadoRegistro

    /** Este teléfono no tiene un registro esperando licencia. */
    data object SinRegistro : ResultadoRegistro
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

/** Restaurar en este teléfono una copia de otro: qué respondió la consulta previa (solo lectura). */
sealed interface ResultadoConsultaTransferencia {
    data class Disponible(val consulta: RespuestaApi.ConsultaTransferencia) : ResultadoConsultaTransferencia

    /** [mensaje] viene de la API, para el usuario. */
    data class Rechazada(val codigo: String, val mensaje: String) : ResultadoConsultaTransferencia

    data class SinConexion(val mensaje: String) : ResultadoConsultaTransferencia
}

/** Resultado de agregar este teléfono a la licencia de la copia. */
sealed interface ResultadoTransferencia {
    /** El token de este teléfono ya está verificado y guardado. [desvinculado]: teléfono que salió de la licencia. */
    data class Exito(val licencia: ResultadoLicencia.Exito, val desvinculado: String?) : ResultadoTransferencia

    data class Rechazada(val codigo: String, val mensaje: String) : ResultadoTransferencia

    data class SinConexion(val mensaje: String) : ResultadoTransferencia

    data class TokenNoValido(val mensaje: String) : ResultadoTransferencia
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
            is RespuestaApi.Token -> aceptarToken(clave, respuesta.token, "activación", respuesta.versionUsuarios)
            is RespuestaApi.ErrorNegocio -> {
                // En la activación el error solo se muestra: todavía no hay nada que bloquear.
                registro.advertencia("Activación rechazada (${enmascararClave(clave)}): ${respuesta.codigo}")
                ResultadoLicencia.Rechazada(respuesta.codigo, respuesta.mensaje)
            }
            is RespuestaApi.FallaRed -> sinConexion("activación", respuesta)
            // Respuestas que no corresponden a este endpoint.
            RespuestaApi.Ok, is RespuestaApi.Usuarios, is RespuestaApi.Registro, is RespuestaApi.LoteEventos, is RespuestaApi.ConsultaTransferencia, is RespuestaApi.Transferencia ->
                ResultadoLicencia.SinConexion(MENSAJE_SIN_CONEXION)
        }
    }

    /**
     * Pasa la licencia antigua de este teléfono a la nube: el servidor crea (o reutiliza) la licencia nueva y
     * responde con su token. La clave nueva se toma del token ya verificado. Si este teléfono ya tiene licencia
     * nueva no hace nada.
     */
    @Synchronized
    fun migrar(claveAntigua: String, nombreDispositivo: String?): ResultadoLicencia {
        if (almacen.leer().clave != null) return ResultadoLicencia.Exito(estadoActual())
        val nombre = nombreDispositivo?.trim()?.take(MAXIMO_NOMBRE_DISPOSITIVO)?.takeIf { it.isNotEmpty() }

        return when (val respuesta = cliente.migrar(claveAntigua.trim(), deviceHash(), nombre, versionApp.take(MAXIMO_VERSION))) {
            is RespuestaApi.Token -> aceptarTokenConSuClave(respuesta.token, "migración", respuesta.versionUsuarios)
            is RespuestaApi.ErrorNegocio -> {
                // Como en la activación: no hay nada que bloquear, la licencia antigua sigue mandando.
                registro.advertencia("Migración rechazada: ${respuesta.codigo}")
                ResultadoLicencia.Rechazada(respuesta.codigo, respuesta.mensaje)
            }
            is RespuestaApi.FallaRed -> sinConexion("migración", respuesta)
            RespuestaApi.Ok, is RespuestaApi.Usuarios, is RespuestaApi.Registro, is RespuestaApi.LoteEventos, is RespuestaApi.ConsultaTransferencia, is RespuestaApi.Transferencia ->
                ResultadoLicencia.SinConexion(MENSAJE_SIN_CONEXION)
        }
    }

    /**
     * Registra el negocio para que el proveedor le asigne una licencia. Si este teléfono ya tiene licencia no hace
     * nada. Si el registro ya tenía licencia asignada (por ejemplo, tras reinstalar), la guarda de una vez.
     */
    @Synchronized
    fun registrar(datos: DatosRegistro, nombreDispositivo: String?): ResultadoRegistro {
        if (almacen.leer().clave != null) return ResultadoRegistro.Asignado(ResultadoLicencia.Exito(estadoActual()))
        return procesarRegistro(
            cliente.registrar(datos, deviceHash(), nombreLimpio(nombreDispositivo), versionApp.take(MAXIMO_VERSION))
        )
    }

    /**
     * Pregunta cómo va el registro pendiente de este teléfono. Con licencia asignada la guarda (el teléfono queda
     * activado sin escribir ninguna clave). Sin registro guardado no llama a la API.
     */
    @Synchronized
    fun consultarRegistro(nombreDispositivo: String?): ResultadoRegistro {
        val datos = almacen.leer()
        if (datos.clave != null || datos.estadoRegistro == null) return ResultadoRegistro.SinRegistro
        return procesarRegistro(
            cliente.estadoRegistro(deviceHash(), nombreLimpio(nombreDispositivo), versionApp.take(MAXIMO_VERSION))
        )
    }

    private fun procesarRegistro(respuesta: RespuestaApi): ResultadoRegistro = when (respuesta) {
        is RespuestaApi.Registro -> when (respuesta.estado) {
            ESTADO_ASIGNADO -> ResultadoRegistro.Asignado(
                aceptarTokenConSuClave(respuesta.token!!, "registro", respuesta.versionUsuarios)
            )
            ESTADO_RECHAZADO -> {
                almacen.guardarRegistro(ESTADO_RECHAZADO, respuesta.mensaje)
                registro.advertencia("Registro rechazado por el proveedor.")
                ResultadoRegistro.Rechazado(respuesta.mensaje)
            }
            else -> {
                almacen.guardarRegistro(ESTADO_PENDIENTE, respuesta.mensaje)
                ResultadoRegistro.Pendiente(respuesta.mensaje)
            }
        }
        is RespuestaApi.ErrorNegocio -> {
            // El servidor ya no tiene el registro (lo borraron): el teléfono vuelve a "sin licencia".
            if (respuesta.codigo == "REGISTRO_NO_ENCONTRADO") almacen.guardarRegistro(null)
            registro.advertencia("Registro rechazado: ${respuesta.codigo}")
            if (respuesta.codigo == "REGISTRO_NO_ENCONTRADO") ResultadoRegistro.SinRegistro
            else ResultadoRegistro.Rechazada(respuesta.codigo, respuesta.mensaje)
        }
        is RespuestaApi.FallaRed -> {
            registro.advertencia("registro sin conexión: ${respuesta.detalle}")
            ResultadoRegistro.SinConexion(MENSAJE_SIN_CONEXION)
        }
        RespuestaApi.Ok, is RespuestaApi.Token, is RespuestaApi.Usuarios, is RespuestaApi.LoteEventos,
        is RespuestaApi.ConsultaTransferencia, is RespuestaApi.Transferencia -> ResultadoRegistro.SinConexion(MENSAJE_SIN_CONEXION)
    }

    /** Solo lectura: qué pasaría al restaurar aquí la copia de otro teléfono ([tokenOrigen] viene en la copia). */
    fun consultarTransferencia(tokenOrigen: String, idOperacion: String): ResultadoConsultaTransferencia =
        when (val respuesta = cliente.consultarTransferencia(tokenOrigen, deviceHash(), idOperacion)) {
            is RespuestaApi.ConsultaTransferencia -> ResultadoConsultaTransferencia.Disponible(respuesta)
            is RespuestaApi.ErrorNegocio -> {
                registro.advertencia("Consulta de transferencia rechazada: ${respuesta.codigo} (operación $idOperacion)")
                ResultadoConsultaTransferencia.Rechazada(respuesta.codigo, respuesta.mensaje)
            }
            is RespuestaApi.FallaRed -> {
                registro.advertencia("Consulta de transferencia sin conexión: ${respuesta.detalle}")
                ResultadoConsultaTransferencia.SinConexion(MENSAJE_SIN_CONEXION)
            }
            else -> ResultadoConsultaTransferencia.SinConexion(MENSAJE_SIN_CONEXION)
        }

    /**
     * Agrega este teléfono a la licencia de la copia. Si no hay cupo y [permitirDesvincular], el servidor desvincula
     * el teléfono de origen. El token recibido se verifica y se guarda igual que al activar.
     */
    @Synchronized
    fun transferir(tokenOrigen: String, nombreDispositivo: String?, permitirDesvincular: Boolean, idOperacion: String): ResultadoTransferencia {
        val respuesta = cliente.transferir(
            tokenOrigen, deviceHash(), nombreLimpio(nombreDispositivo), versionApp.take(MAXIMO_VERSION), permitirDesvincular, idOperacion,
        )
        return when (respuesta) {
            is RespuestaApi.Transferencia -> when (val resultado = aceptarTokenConSuClave(respuesta.token, "transferencia", respuesta.versionUsuarios)) {
                is ResultadoLicencia.Exito -> ResultadoTransferencia.Exito(resultado, respuesta.desvinculado)
                else -> ResultadoTransferencia.TokenNoValido((resultado as? ResultadoLicencia.TokenNoValido)?.mensaje ?: MENSAJE_TOKEN_NO_VALIDO)
            }
            is RespuestaApi.ErrorNegocio -> {
                registro.advertencia("Transferencia rechazada: ${respuesta.codigo} (operación $idOperacion)")
                ResultadoTransferencia.Rechazada(respuesta.codigo, respuesta.mensaje)
            }
            is RespuestaApi.FallaRed -> {
                registro.advertencia("Transferencia sin conexión: ${respuesta.detalle}")
                ResultadoTransferencia.SinConexion(MENSAJE_SIN_CONEXION)
            }
            else -> ResultadoTransferencia.SinConexion(MENSAJE_SIN_CONEXION)
        }
    }

    /** Envía a la API un lote de eventos de copias de seguridad. `true` si la API los recibió (guardados o rechazados). */
    fun enviarEventosBackup(eventos: org.json.JSONArray): Boolean =
        when (val respuesta = cliente.enviarEventosBackup(eventos)) {
            is RespuestaApi.LoteEventos -> {
                if (respuesta.rechazados > 0) registro.advertencia("Eventos de copia rechazados por la API: ${respuesta.rechazados}")
                true
            }
            // Un error de formato no se arregla reintentando: se descartan para no trabar la cola.
            is RespuestaApi.ErrorNegocio -> {
                registro.advertencia("Lote de eventos de copia rechazado: ${respuesta.codigo}")
                true
            }
            else -> false
        }

    /** Para un token cuya clave el teléfono todavía no conoce (migración, registro): se toma del token verificado. */
    private fun aceptarTokenConSuClave(token: String, operacion: String, versionUsuarios: String?): ResultadoLicencia {
        val clave = (verificador.verificar(token) as? ResultadoVerificacion.Valido)?.payload?.licencia
            ?.takeIf { FORMATO_CLAVE.matches(it) }
        if (clave == null) {
            registro.advertencia("$operacion: token rechazado")
            return ResultadoLicencia.TokenNoValido(MENSAJE_TOKEN_NO_VALIDO)
        }
        return aceptarToken(clave, token, operacion, versionUsuarios)
    }

    private fun nombreLimpio(nombreDispositivo: String?): String? =
        nombreDispositivo?.trim()?.take(MAXIMO_NOMBRE_DISPOSITIVO)?.takeIf { it.isNotEmpty() }

    @Synchronized
    fun validar(): ResultadoLicencia {
        val clave = almacen.leer().clave ?: return ResultadoLicencia.SinLicencia

        return when (val respuesta = cliente.validar(clave, deviceHash(), versionApp.take(MAXIMO_VERSION))) {
            is RespuestaApi.Token -> aceptarToken(clave, respuesta.token, "validación", respuesta.versionUsuarios)
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
            RespuestaApi.Ok, is RespuestaApi.Usuarios, is RespuestaApi.Registro, is RespuestaApi.LoteEventos, is RespuestaApi.ConsultaTransferencia, is RespuestaApi.Transferencia ->
                ResultadoLicencia.SinConexion(MENSAJE_SIN_CONEXION)
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
            is RespuestaApi.Token, is RespuestaApi.Usuarios, is RespuestaApi.Registro, is RespuestaApi.LoteEventos, is RespuestaApi.ConsultaTransferencia, is RespuestaApi.Transferencia ->
                ResultadoLicencia.SinConexion(MENSAJE_SIN_CONEXION)
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

    /** Huella de usuarios ya aplicada en el teléfono (tras sincronizar con éxito). */
    fun guardarVersionUsuarios(version: String?) = almacen.guardarVersionUsuarios(version)

    /** `device_hash` de este teléfono. */
    fun deviceHashActual(): String = deviceHash()

    /** Licencia que dice el token de una copia (firma del servidor verificada), o `null` si no es válido. */
    fun licenciaDeToken(token: String): String? =
        (verificador.verificar(token) as? ResultadoVerificacion.Valido)?.payload?.licencia

    /** Verifica el token recibido (firma, clave, dispositivo y reloj) y solo entonces lo guarda. */
    private fun aceptarToken(clave: String, token: String, operacion: String, versionUsuarios: String? = null): ResultadoLicencia {
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
        return ResultadoLicencia.Exito(estado, versionUsuarios)
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
        private val FORMATO_CLAVE = Regex("[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}")
        private const val MAXIMO_NOMBRE_DISPOSITIVO = 100
        private const val MAXIMO_VERSION = 50
        const val ESTADO_PENDIENTE = "pendiente"
        const val ESTADO_RECHAZADO = "rechazado"
        private const val ESTADO_ASIGNADO = "asignado"

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
