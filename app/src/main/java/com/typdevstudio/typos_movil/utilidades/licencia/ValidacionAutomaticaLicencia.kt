package com.typdevstudio.typos_movil.utilidades.licencia

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** Qué hace un ciclo de sincronización según el estado del teléfono. */
enum class AccionSincronizacion { VALIDAR, CONSULTAR_REGISTRO, MIGRAR, NADA }

/** Cómo terminó un ciclo: [SIN_CONEXION] programa un reintento para cuando vuelva la red. */
enum class ResultadoSincronizacion { HECHA, SIN_CONEXION, OMITIDA }

/** Reglas de la sincronización con la API (puras, para probarlas). */
object PoliticaSincronizacion {

    /** Con la app abierta: un cambio hecho en el portal llega en 5 minutos o menos. */
    const val INTERVALO_CON_APP_ABIERTA_MS = 5L * 60 * 1000

    /** En segundo plano (WorkManager). Es el mínimo que permite Android para un trabajo periódico. */
    const val INTERVALO_EN_SEGUNDO_PLANO_MIN = 15L

    /** Evita ciclos seguidos (abrir y cerrar la app, recuperar la red varias veces...). */
    const val MINIMO_ENTRE_CICLOS_MS = 30L * 1000

    fun debeIntentar(ultimoCicloMs: Long?, ahoraMs: Long): Boolean =
        ultimoCicloMs == null || ahoraMs - ultimoCicloMs >= MINIMO_ENTRE_CICLOS_MS || ahoraMs < ultimoCicloMs

    /** Con licencia se valida; si no, se consulta el registro pendiente o se migra la licencia antigua. */
    fun accion(tieneLicencia: Boolean, registroPendiente: Boolean, puedeMigrar: Boolean): AccionSincronizacion = when {
        tieneLicencia -> AccionSincronizacion.VALIDAR
        registroPendiente -> AccionSincronizacion.CONSULTAR_REGISTRO
        puedeMigrar -> AccionSincronizacion.MIGRAR
        else -> AccionSincronizacion.NADA
    }
}

/** Cuándo reintentar pasar la licencia antigua a la nube después de que el servidor la rechazó (pura). */
object PoliticaMigracionAutomatica {

    /** Un rechazo (licencia no reconocida, vencida...) no cambia de un minuto a otro: se espera un día. */
    const val ESPERA_TRAS_RECHAZO_MS = 24L * 60 * 60 * 1000

    /**
     * Respuestas del servidor que sí dicen algo de la licencia. Cualquier otra (por ejemplo `RUTA_NO_ENCONTRADA`
     * porque el servidor todavía no tiene la migración) no es culpa de la licencia y se reintenta pronto.
     */
    private val CODIGOS_DE_RECHAZO = setOf(
        "MIGRACION_NO_DISPONIBLE", "LICENCIA_VENCIDA", "LICENCIA_SUSPENDIDA",
        "LIMITE_DISPOSITIVOS", "DISPOSITIVO_REVOCADO", "SOLICITUD_INVALIDA",
    )

    fun esRechazoDeLicencia(codigo: String): Boolean = codigo in CODIGOS_DE_RECHAZO

    /**
     * Un rechazo guardado por otra versión de la app no cuenta: al actualizar se intenta enseguida (la versión nueva
     * puede corregir lo que falló, y el proveedor pudo cargar la licencia mientras tanto).
     */
    fun debeIntentar(ultimoRechazoMs: Long?, versionDelRechazo: Int?, versionActual: Int, ahoraMs: Long): Boolean =
        ultimoRechazoMs == null || versionDelRechazo != versionActual ||
            ahoraMs - ultimoRechazoMs >= ESPERA_TRAS_RECHAZO_MS || ahoraMs < ultimoRechazoMs
}

/**
 * Sincroniza en segundo plano (cada 15 minutos, con red): valida la licencia, consulta el registro pendiente o pasa
 * la licencia antigua a la nube.
 * Las fallas de red piden reintento (con espera creciente) y nunca cambian el estado guardado.
 */
class TrabajadorValidacionLicencia(contexto: Context, parametros: WorkerParameters) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        LicenciaGuard.inicializar(applicationContext)
        return when (LicenciaGuard.sincronizarAhora()) {
            ResultadoSincronizacion.SIN_CONEXION -> if (runAttemptCount < MAXIMO_REINTENTOS) Result.retry() else Result.success()
            else -> Result.success()
        }
    }

    private companion object {
        const val MAXIMO_REINTENTOS = 8
    }
}

/** Programa la sincronización en segundo plano con WorkManager (solo con red conectada). */
object PlanificadorValidacionLicencia {

    private const val TRABAJO_PERIODICO = "licencia_validacion_periodica"
    private const val TRABAJO_RECONEXION = "licencia_validacion_reconexion"

    private val conRed = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /**
     * Cada 15 minutos con red, aun con la app cerrada. `UPDATE` reemplaza el intervalo de versiones anteriores
     * (antes era cada 6 horas) sin duplicar el trabajo.
     */
    fun programarPeriodica(contexto: Context) {
        val trabajo = PeriodicWorkRequestBuilder<TrabajadorValidacionLicencia>(PoliticaSincronizacion.INTERVALO_EN_SEGUNDO_PLANO_MIN, TimeUnit.MINUTES)
            .setConstraints(conRed)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(contexto)
            .enqueueUniquePeriodicWork(TRABAJO_PERIODICO, ExistingPeriodicWorkPolicy.UPDATE, trabajo)
    }

    /** Una validación que espera a que vuelva la conexión (se usa cuando un intento falló por red). */
    fun programarAlReconectar(contexto: Context) {
        val trabajo = OneTimeWorkRequestBuilder<TrabajadorValidacionLicencia>()
            .setConstraints(conRed)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(contexto)
            .enqueueUniqueWork(TRABAJO_RECONEXION, ExistingWorkPolicy.KEEP, trabajo)
    }
}
