package com.typdevstudio.typos_movil.datos.repositorio

import com.typdevstudio.typos_movil.datos.local.daos.VentaDao
import com.typdevstudio.typos_movil.datos.local.entidades.DetalleVentaEntidad
import com.typdevstudio.typos_movil.datos.local.entidades.VentaEntidad
import com.typdevstudio.typos_movil.datos.local.relaciones.VentaConDetalles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class VentaRepositorio(private val ventaDao: VentaDao) {

    fun obtenerTodasLasVentas(): Flow<List<VentaConDetalles>> = ventaDao.obtenerTodasLasVentas()

    suspend fun obtenerVentaPorId(idVenta: Long): VentaConDetalles? = withContext(Dispatchers.IO) {
        ventaDao.obtenerVentaPorId(idVenta)
    }

    suspend fun generarNumeroFactura(): String = withContext(Dispatchers.IO) {
        val conteo = ventaDao.contarVentas() + 1
        "FAC-${conteo.toString().padStart(6, '0')}"
    }

    suspend fun registrarVenta(
        venta: VentaEntidad,
        detalles: List<DetalleVentaEntidad>
    ): Long = withContext(Dispatchers.IO) {
        ventaDao.procesarVentaCompleta(venta, detalles)
    }

    suspend fun anularVenta(idVenta: Long, motivo: String?) = withContext(Dispatchers.IO) {
        ventaDao.anularVentaYRestituirStock(idVenta, motivo)
    }
}
