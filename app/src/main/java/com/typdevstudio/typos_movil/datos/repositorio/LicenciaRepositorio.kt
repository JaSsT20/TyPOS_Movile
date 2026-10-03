package com.typdevstudio.typos_movil.datos.repositorio

import com.typdevstudio.typos_movil.datos.local.daos.LicenciaDao
import com.typdevstudio.typos_movil.datos.local.entidades.LicenciaEntidad
import com.typdevstudio.typos_movil.utilidades.licencia.CifradorLicencias
import com.typdevstudio.typos_movil.utilidades.licencia.InfoLicencia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

sealed class EstadoLicencia {
    data object SinLicencia : EstadoLicencia()
    data class Activa(val diasRestantes: Int, val fechaVencimiento: Long, val clave: String) : EstadoLicencia()
    data class Vencida(val fechaVencimiento: Long, val clave: String) : EstadoLicencia()
    data class RelojAlterado(val ultimaFechaUso: Long) : EstadoLicencia()
}

class LicenciaRepositorio(private val licenciaDao: LicenciaDao) {

    fun obtenerLicencia(): Flow<LicenciaEntidad?> = licenciaDao.obtenerLicencia()

    suspend fun verificarEstadoLicencia(): EstadoLicencia = withContext(Dispatchers.IO) {
        val licencia = licenciaDao.obtenerLicenciaDirecta() ?: return@withContext EstadoLicencia.SinLicencia

        val ahora = System.currentTimeMillis()

        // Protección contra manipulación de reloj (fecha hacia atrás por más de 5 minutos)
        if (ahora < (licencia.ultimaFechaUso - 300_000L)) {
            return@withContext EstadoLicencia.RelojAlterado(licencia.ultimaFechaUso)
        }

        val info = CifradorLicencias.validarClaveLicencia(licencia.claveLicencia, ahora)

        if (!info.esValida || info.estaVencida || ahora >= licencia.fechaVencimiento) {
            if (licencia.estado != "VENCIDA") {
                licenciaDao.guardarLicencia(licencia.copy(estado = "VENCIDA", ultimaFechaUso = ahora))
            }
            return@withContext EstadoLicencia.Vencida(licencia.fechaVencimiento, licencia.claveLicencia)
        }

        // Actualizar última fecha de uso legítimo
        licenciaDao.guardarLicencia(licencia.copy(estado = "ACTIVA", ultimaFechaUso = ahora))

        EstadoLicencia.Activa(
            diasRestantes = info.diasRestantes,
            fechaVencimiento = licencia.fechaVencimiento,
            clave = licencia.claveLicencia
        )
    }

    suspend fun activarLicencia(clave: String): InfoLicencia = withContext(Dispatchers.IO) {
        val ahora = System.currentTimeMillis()
        val info = CifradorLicencias.validarClaveLicencia(clave, ahora)

        if (!info.esValida) {
            return@withContext info
        }

        if (info.estaVencida) {
            return@withContext info.copy(mensajeError = "La clave ingresada ya ha vencido")
        }

        val entidad = LicenciaEntidad(
            id = 1,
            claveLicencia = clave.replace("-", "").replace(" ", "").trim().uppercase(),
            fechaEmision = info.fechaEmision,
            fechaActivacion = ahora,
            fechaVencimiento = info.fechaVencimiento,
            diasTotales = info.diasValidez,
            estado = "ACTIVA",
            ultimaFechaUso = ahora
        )

        licenciaDao.guardarLicencia(entidad)
        info
    }
}
