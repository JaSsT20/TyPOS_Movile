package com.typdevstudio.typos_movil.datos.repositorio

import com.typdevstudio.typos_movil.datos.local.daos.ConfiguracionNegocioDao
import com.typdevstudio.typos_movil.datos.local.entidades.ConfiguracionNegocioEntidad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ConfiguracionRepositorio(private val configuracionDao: ConfiguracionNegocioDao) {

    fun obtenerConfiguracion(): Flow<ConfiguracionNegocioEntidad?> =
        configuracionDao.obtenerConfiguracion()

    suspend fun obtenerConfiguracionDirecta(): ConfiguracionNegocioEntidad = withContext(Dispatchers.IO) {
        configuracionDao.obtenerConfiguracionDirecta() ?: ConfiguracionNegocioEntidad(
            id = 1,
            nombreNegocio = "TyPOS Móvil",
            pieTicket = "¡Gracias por su compra!",
            tamanoPapelImpresora = 58
        )
    }

    suspend fun guardarConfiguracion(configuracion: ConfiguracionNegocioEntidad) = withContext(Dispatchers.IO) {
        configuracionDao.guardarConfiguracion(configuracion)
    }
}
