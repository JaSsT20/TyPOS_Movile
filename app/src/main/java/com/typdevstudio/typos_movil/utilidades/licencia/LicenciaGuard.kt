package com.typdevstudio.typos_movil.utilidades.licencia

import android.content.Context
import android.util.Log
import com.typdevstudio.typos_movil.BuildConfig
import com.typdevstudio.typos_movil.datos.local.AppBaseDatos
import com.typdevstudio.typos_movil.datos.repositorio.LicenciaRepositorio
import com.typdevstudio.typos_movil.utilidades.licencia.nube.EstadoLicencia
import com.typdevstudio.typos_movil.utilidades.licencia.nube.HoraRD
import com.typdevstudio.typos_movil.utilidades.licencia.nube.MotivoRestriccion
import com.typdevstudio.typos_movil.utilidades.licencia.nube.PayloadLicencia
import com.typdevstudio.typos_movil.utilidades.licencia.nube.RepositorioLicencia
import com.typdevstudio.typos_movil.utilidades.licencia.nube.ResultadoLicencia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import com.typdevstudio.typos_movil.datos.repositorio.EstadoLicencia as EstadoLicenciaAntigua

/** Lo que importa de la licencia antigua (clave XTEA guardada en Room). */
data class LicenciaAntigua(val vigente: Boolean, val venceMs: Long)

/**
 * Situación de la licencia que ve toda la app: el estado de la licencia nueva y, mientras dure la transición,
 * la licencia antigua. Es la única fuente para decidir si se puede emitir.
 */
data class SituacionLicencia(
    val nube: EstadoLicencia,
    val antigua: LicenciaAntigua?,
    /** `true` si se opera con la licencia antigua porque la nueva todavía no se activó. */
    val usaLicenciaAntigua: Boolean,
    /** `false` solo antes de la primera evaluación, al abrir la app. */
    val verificada: Boolean = true,
    val clave: String? = null,
    val ultimaValidacionMs: Long? = null,
    /** Mensaje de la API que acompañó al bloqueo vigente. Si existe, es el que se muestra al usuario. */
    val mensajeServidor: String? = null,
) {
    /** Ventas, anulaciones, productos, importación, restauración y usuarios nuevos. */
    val permiteEmitir: Boolean get() = usaLicenciaAntigua || nube.permiteEmitir

    /** Motivo del modo restringido, o `null` si se puede emitir. */
    val motivoRestriccion: MotivoRestriccion?
        get() = if (permiteEmitir) null else (nube as? EstadoLicencia.Restringida)?.motivo ?: MotivoRestriccion.SIN_ACTIVAR

    val payload: PayloadLicencia?
        get() = when (val estado = nube) {
            is EstadoLicencia.Activa -> estado.payload
            is EstadoLicencia.ActivaPorValidar -> estado.payload
            is EstadoLicencia.EnCortesia -> estado.payload
            is EstadoLicencia.Restringida -> estado.payload
            EstadoLicencia.SinActivar -> null
        }

    /**
     * Día (epoch, hora RD) en que vence la licencia que autoriza a trabajar, para los avisos de vencimiento.
     * `null` si no se puede emitir (ya hay un aviso de modo restringido) o si no hay fecha.
     */
    val venceDia: Long?
        get() = when {
            !verificada || !permiteEmitir -> null
            usaLicenciaAntigua -> antigua?.venceMs?.takeIf { it > 0 }?.let { HoraRD.diaEnRD(it - 1) }
            else -> payload?.vence
        }

    /**
     * `true` si el plan incluye el módulo. Con la licencia antigua (transición) o sin token, todos los módulos
     * siguen como hoy: lo que no se pueda hacer lo decide el modo restringido.
     */
    fun moduloIncluido(codigo: String): Boolean =
        usaLicenciaAntigua || payload?.modulos?.contains(codigo) ?: true

    /** Límite de usuarios del plan, o `null` si no aplica (licencia antigua o sin token). */
    val maxUsuarios: Int?
        get() = if (usaLicenciaAntigua) null else payload?.maxUsuarios

    /**
     * Por qué no se puede crear otro usuario, o `null` si se puede. El primer usuario siempre se permite:
     * sin él nadie podría entrar a activar la licencia. No se borra ni desactiva nada si ya se pasó del límite.
     */
    fun motivoParaNoCrearUsuario(usuariosActivos: Int): String? {
        val maximo = maxUsuarios
        return when {
            usuariosActivos == 0 -> null
            !permiteEmitir -> mensajeAccionBloqueada()
            maximo != null && usuariosActivos >= maximo ->
                "Tu plan permite hasta $maximo ${if (maximo == 1) "usuario" else "usuarios"} y ya tienes $usuariosActivos. " +
                    "Para agregar más, mejora tu plan con ${BuildConfig.CONTACTO_LICENCIA_NOMBRE} al ${BuildConfig.CONTACTO_LICENCIA_TELEFONO}."
            else -> null
        }
    }

    /** Texto para el usuario cuando está en modo restringido. */
    fun mensajeRestriccion(): String = mensajeServidor ?: when (motivoRestriccion) {
        null -> ""
        MotivoRestriccion.SIN_ACTIVAR ->
            if (antigua != null && !antigua.vigente) "Tu licencia anterior venció. Activa tu nueva licencia para seguir vendiendo."
            else "Este teléfono no tiene una licencia activa. Ingresa tu clave de licencia para empezar a vender."
        MotivoRestriccion.TOKEN_EXPIRADO ->
            "Pasaron demasiados días sin validar la licencia. Conéctate a internet y toca «Validar ahora»."
        MotivoRestriccion.SUSPENDIDA -> "La licencia está suspendida. Comunícate con tu proveedor."
        MotivoRestriccion.VENCIDA -> "La licencia venció. Renuévala con tu proveedor y luego toca «Validar ahora»."
        MotivoRestriccion.REVOCADO -> "Este teléfono fue bloqueado en la licencia. Comunícate con tu proveedor."
        MotivoRestriccion.FIRMA_INVALIDA, MotivoRestriccion.DISPOSITIVO_DISTINTO ->
            "La licencia guardada no es válida para este teléfono. Vuelve a ingresar tu clave de licencia."
        MotivoRestriccion.RELOJ_ALTERADO ->
            "La fecha y hora del teléfono no son correctas. Corrígelas (activa la hora automática) y toca «Validar ahora»."
    }

    /** Mensaje para una acción bloqueada por el modo restringido. */
    fun mensajeAccionBloqueada(): String =
        "Modo solo consulta: ${mensajeRestriccion()} Puedes consultar y respaldar tu información, pero no registrar ni modificar datos."

    companion object {
        /** Versión corta de [mensajeAccionBloqueada], para avisos breves (Toast) donde el texto largo se corta. */
        const val MENSAJE_ACCION_BLOQUEADA_CORTO = "Modo solo consulta: activa o valida la licencia para hacer cambios."

        val INICIAL = SituacionLicencia(EstadoLicencia.SinActivar, null, usaLicenciaAntigua = false, verificada = false)

        /**
         * Combina la licencia nueva con la antigua. La antigua solo cuenta si la nueva nunca se activó
         * ([EstadoLicencia.SinActivar]): cualquier token o bloqueo del servidor manda sobre ella.
         */
        fun combinar(nube: EstadoLicencia, antigua: LicenciaAntigua?, usarAntigua: Boolean): SituacionLicencia =
            SituacionLicencia(
                nube = nube,
                antigua = antigua,
                usaLicenciaAntigua = usarAntigua && nube == EstadoLicencia.SinActivar && antigua?.vigente == true,
            )
    }
}

/** Códigos de módulo del plan que la app reconoce (los mismos que la API guarda en `planes.modulos`). */
object ModulosLicencia {
    /** Facturación: cobrar y emitir ventas. */
    const val FACTURACION = "facturacion"

    /** Control de existencias (stock) e importación de productos. El catálogo siempre está disponible. */
    const val INVENTARIO = "inventario"

    /** Cuentas por cobrar: la app todavía no tiene este módulo. */
    const val CXC = "cxc"

    fun mensajeNoIncluido(nombreModulo: String): String =
        "El módulo $nombreModulo no está incluido en tu plan. Para agregarlo, comunícate con " +
            "${BuildConfig.CONTACTO_LICENCIA_NOMBRE} al ${BuildConfig.CONTACTO_LICENCIA_TELEFONO}."
}

enum class TipoAvisoVencimiento { PROXIMO_A_VENCER, VENCE_HOY }

data class AvisoVencimiento(val tipo: TipoAvisoVencimiento, val diasRestantes: Int, val venceDia: Long) {
    /** Identifica el aviso para mostrarlo una sola vez por fecha de vencimiento (una renovación vuelve a avisar). */
    val clave: String get() = "${tipo.name}_$venceDia"
}

/** Avisos de vencimiento: uno cuando faltan 5 días o menos y otro el mismo día del vencimiento. */
object CalculadoraAvisoVencimiento {

    const val DIAS_ANTICIPACION = 5

    fun calcular(venceDia: Long?, hoy: Long, yaMostrados: Set<String>): AvisoVencimiento? {
        if (venceDia == null) return null
        val diasRestantes = venceDia - hoy
        val tipo = when (diasRestantes) {
            0L -> TipoAvisoVencimiento.VENCE_HOY
            in 1L..DIAS_ANTICIPACION.toLong() -> TipoAvisoVencimiento.PROXIMO_A_VENCER
            else -> return null
        }
        return AvisoVencimiento(tipo, diasRestantes.toInt(), venceDia).takeIf { it.clave !in yaMostrados }
    }
}

/**
 * Guardia central de licencia. Todos los puntos que emiten o modifican datos preguntan aquí ([puedeEmitir]);
 * ninguna pantalla decide por su cuenta.
 */
object LicenciaGuard {

    /**
     * Transición: mientras sea `true`, un teléfono sin licencia nueva sigue funcionando con su licencia antigua
     * hasta que esta venza. Se retirará en una versión futura, cuando todos los clientes hayan activado.
     */
    const val USAR_LICENCIA_ANTIGUA = true

    private const val ETIQUETA_LOG = "LicenciaGuard"
    private const val INTERVALO_REEVALUACION_MS = 60_000L

    private val _situacion = MutableStateFlow(SituacionLicencia.INICIAL)
    val situacion: StateFlow<SituacionLicencia> = _situacion.asStateFlow()

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val candado = Mutex()

    private lateinit var repositorio: RepositorioLicencia
    private lateinit var repositorioAntiguo: LicenciaRepositorio
    private lateinit var aplicacion: Context

    private val validacionAutomaticaEnCurso = AtomicBoolean(false)
    @Volatile
    private var ultimoIntentoAutomaticoMs: Long? = null

    /**
     * Se llama una vez en `TyPosApp.onCreate`. Evalúa el estado local (no espera la red), lo reevalúa cada minuto,
     * programa la validación periódica y, si pasaron más de 12 horas, valida en segundo plano.
     */
    @Synchronized
    fun inicializar(contexto: Context) {
        if (::repositorio.isInitialized) return
        aplicacion = contexto.applicationContext
        repositorio = RepositorioLicencia.crear(aplicacion)
        val baseDatos = AppBaseDatos.obtenerBaseDatos(aplicacion)
        repositorioAntiguo = LicenciaRepositorio(
            baseDatos.licenciaDao(),
            IdDispositivo.obtenerSerialDispositivo(aplicacion),
        )
        GestorUsuarios.inicializar(repositorio, baseDatos.usuarioDao())
        alcance.launch {
            actualizar()
            validarSiHaceFalta()
            while (true) {
                delay(INTERVALO_REEVALUACION_MS)
                actualizar()
            }
        }
        try {
            PlanificadorValidacionLicencia.programarPeriodica(aplicacion)
        } catch (e: Exception) {
            Log.e(ETIQUETA_LOG, "No se pudo programar la validación periódica", e)
        }
    }

    /**
     * Al abrir la app o volver a ella: si pasaron más de 12 horas desde la última validación, valida en segundo
     * plano. Nunca bloquea ni retrasa la pantalla. Si no hay red, deja programado un intento para cuando vuelva.
     */
    fun validarSiHaceFalta() {
        if (!::repositorio.isInitialized || !validacionAutomaticaEnCurso.compareAndSet(false, true)) return
        alcance.launch {
            try {
                val ahora = System.currentTimeMillis()
                if (!PoliticaValidacionAutomatica.debeIntentar(necesitaValidar(), ultimoIntentoAutomaticoMs, ahora)) return@launch
                ultimoIntentoAutomaticoMs = ahora
                if (validarAhora() is ResultadoLicencia.SinConexion) {
                    PlanificadorValidacionLicencia.programarAlReconectar(aplicacion)
                }
            } catch (e: Exception) {
                Log.e(ETIQUETA_LOG, "Error en la validación automática", e)
            } finally {
                validacionAutomaticaEnCurso.set(false)
            }
        }
    }

    /** `true` si hay licencia nueva y pasaron más de 12 horas desde la última validación exitosa. */
    suspend fun necesitaValidar(): Boolean = withContext(Dispatchers.IO) { repositorio.necesitaValidar() }

    /** Vuelve a evaluar con el reloj y lo guardado ahora mismo. Nunca lanza excepciones. */
    suspend fun actualizar(): SituacionLicencia = withContext(Dispatchers.IO) {
        candado.withLock {
            try {
                val nube = repositorio.estadoActual()
                val antigua = if (USAR_LICENCIA_ANTIGUA && nube == EstadoLicencia.SinActivar) leerLicenciaAntigua() else null
                val datos = repositorio.datosGuardados()
                // El mensaje de la API solo aplica si el bloqueo que se está mostrando es el que ella comunicó.
                val mensajeServidor = datos.mensajeBloqueoServidor
                    ?.takeIf { (nube as? EstadoLicencia.Restringida)?.motivo == datos.bloqueoServidor }
                SituacionLicencia.combinar(nube, antigua, USAR_LICENCIA_ANTIGUA)
                    .copy(clave = datos.clave, ultimaValidacionMs = datos.ultimaValidacionMs, mensajeServidor = mensajeServidor)
                    .also { _situacion.value = it }
            } catch (e: Exception) {
                // Un error inesperado no debe cerrar la app (el capturador global mata el proceso): se conserva lo anterior.
                Log.e(ETIQUETA_LOG, "No se pudo evaluar la licencia", e)
                _situacion.value
            }
        }
    }

    /** `true` si se puede emitir una venta o modificar datos. Evalúa en el momento. */
    suspend fun puedeEmitir(): Boolean = actualizar().permiteEmitir

    /** Mensaje para mostrar cuando [puedeEmitir] devuelve `false`. */
    fun mensajeAccionBloqueada(): String = _situacion.value.mensajeAccionBloqueada()

    /**
     * Por qué no se puede emitir en ese módulo (modo restringido o módulo fuera del plan), o `null` si se puede.
     * Evalúa en el momento.
     */
    suspend fun motivoBloqueo(modulo: String, nombreModulo: String): String? {
        val situacion = actualizar()
        return when {
            !situacion.permiteEmitir -> situacion.mensajeAccionBloqueada()
            !situacion.moduloIncluido(modulo) -> ModulosLicencia.mensajeNoIncluido(nombreModulo)
            else -> null
        }
    }

    suspend fun activar(clave: String, nombreDispositivo: String?): ResultadoLicencia =
        operar { repositorio.activar(clave, nombreDispositivo) }.also { sincronizarUsuariosSiValida(it) }

    suspend fun validarAhora(): ResultadoLicencia = operar { repositorio.validar() }.also { sincronizarUsuariosSiValida(it) }

    /** Tras activar o validar con éxito, el teléfono se pone al día con los usuarios de la licencia. */
    private suspend fun sincronizarUsuariosSiValida(resultado: ResultadoLicencia) {
        if (resultado !is ResultadoLicencia.Exito) return
        GestorUsuarios.sincronizar()
        actualizar() // por si la API comunicó un bloqueo durante la sincronización
    }

    suspend fun desactivar(): ResultadoLicencia = operar { repositorio.desactivar() }

    /** Primeros 16 caracteres del `device_hash` (`XXXX-XXXX-XXXX-XXXX`), para soporte. */
    suspend fun identificadorDispositivo(): String = withContext(Dispatchers.IO) {
        repositorio.deviceHashActual().take(16).uppercase().chunked(4).joinToString("-")
    }

    private suspend fun operar(operacion: () -> ResultadoLicencia): ResultadoLicencia {
        val resultado = withContext(Dispatchers.IO) {
            try {
                operacion()
            } catch (e: Exception) {
                Log.e(ETIQUETA_LOG, "Error inesperado en una operación de licencia", e)
                ResultadoLicencia.SinConexion("Ocurrió un error inesperado. Intenta de nuevo.")
            }
        }
        actualizar()
        return resultado
    }

    private suspend fun leerLicenciaAntigua(): LicenciaAntigua? =
        when (val estado = repositorioAntiguo.verificarEstadoLicencia()) {
            is EstadoLicenciaAntigua.Activa -> LicenciaAntigua(vigente = true, venceMs = estado.fechaVencimiento)
            is EstadoLicenciaAntigua.Vencida -> LicenciaAntigua(vigente = false, venceMs = estado.fechaVencimiento)
            is EstadoLicenciaAntigua.RelojAlterado -> LicenciaAntigua(vigente = false, venceMs = 0L)
            EstadoLicenciaAntigua.SinLicencia -> null
        }
}
