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
import com.typdevstudio.typos_movil.utilidades.licencia.nube.ResultadoLicencia
import java.util.concurrent.TimeUnit

/** Cuándo vale la pena intentar una validación automática (pura, para probarla). */
object PoliticaValidacionAutomatica {

    /** Evita repetir intentos fallidos cada vez que se abre la app sin internet. */
    const val MINIMO_ENTRE_INTENTOS_MS = 15L * 60 * 1000

    fun debeIntentar(necesitaValidar: Boolean, ultimoIntentoMs: Long?, ahoraMs: Long): Boolean =
        necesitaValidar && (ultimoIntentoMs == null || ahoraMs - ultimoIntentoMs >= MINIMO_ENTRE_INTENTOS_MS || ahoraMs < ultimoIntentoMs)
}

/** Cuándo reintentar pasar la licencia antigua a la nube después de que el servidor la rechazó (pura). */
object PoliticaMigracionAutomatica {

    /** Un rechazo (licencia no reconocida, vencida...) no cambia de un minuto a otro: se espera un día. */
    const val ESPERA_TRAS_RECHAZO_MS = 24L * 60 * 60 * 1000

    fun debeIntentar(ultimoRechazoMs: Long?, ahoraMs: Long): Boolean =
        ultimoRechazoMs == null || ahoraMs - ultimoRechazoMs >= ESPERA_TRAS_RECHAZO_MS || ahoraMs < ultimoRechazoMs
}

/**
 * Valida la licencia en segundo plano si pasaron más de 12 horas desde la última validación exitosa, o pasa la
 * licencia antigua a la nube si el teléfono todavía no tiene la nueva.
 * Las fallas de red piden reintento (con espera creciente) y nunca cambian el estado guardado.
 */
class TrabajadorValidacionLicencia(contexto: Context, parametros: WorkerParameters) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        LicenciaGuard.inicializar(applicationContext)
        val resultado = when {
            LicenciaGuard.debeMigrar() -> LicenciaGuard.migrarAhora()
            LicenciaGuard.necesitaValidar() -> LicenciaGuard.validarAhora()
            else -> return Result.success()
        }
        return when (resultado) {
            is ResultadoLicencia.SinConexion -> if (runAttemptCount < MAXIMO_REINTENTOS) Result.retry() else Result.success()
            else -> Result.success()
        }
    }

    private companion object {
        const val MAXIMO_REINTENTOS = 8
    }
}

/** Programa las validaciones automáticas con WorkManager (solo con red conectada). */
object PlanificadorValidacionLicencia {

    private const val TRABAJO_PERIODICO = "licencia_validacion_periodica"
    private const val TRABAJO_RECONEXION = "licencia_validacion_reconexion"

    private val conRed = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /**
     * Cada 6 horas revisa si hace falta validar (umbral de 12 horas). Así, aun con la app cerrada,
     * un bloqueo del servidor o una renovación llegan al teléfono en menos de un día.
     */
    fun programarPeriodica(contexto: Context) {
        val trabajo = PeriodicWorkRequestBuilder<TrabajadorValidacionLicencia>(6, TimeUnit.HOURS)
            .setConstraints(conRed)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(contexto)
            .enqueueUniquePeriodicWork(TRABAJO_PERIODICO, ExistingPeriodicWorkPolicy.KEEP, trabajo)
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
