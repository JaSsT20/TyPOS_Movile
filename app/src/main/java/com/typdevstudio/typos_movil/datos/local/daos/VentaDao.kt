package com.typdevstudio.typos_movil.datos.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.typdevstudio.typos_movil.datos.local.entidades.DetalleVentaEntidad
import com.typdevstudio.typos_movil.datos.local.entidades.VentaEntidad
import com.typdevstudio.typos_movil.datos.local.relaciones.VentaConDetalles
import kotlinx.coroutines.flow.Flow

@Dao
interface VentaDao {

    @Transaction
    @Query("SELECT * FROM ventas ORDER BY fecha DESC")
    fun obtenerTodasLasVentas(): Flow<List<VentaConDetalles>>

    @Transaction
    @Query("SELECT * FROM ventas WHERE id = :idVenta LIMIT 1")
    suspend fun obtenerVentaPorId(idVenta: Long): VentaConDetalles?

    @Query("SELECT COUNT(*) FROM ventas")
    suspend fun contarVentas(): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertarVenta(venta: VentaEntidad): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertarDetalles(detalles: List<DetalleVentaEntidad>)

    @Query("UPDATE ventas SET estado_venta = 'Cancelada', motivo_anulacion = :motivo WHERE id = :idVenta")
    suspend fun marcarVentaCancelada(idVenta: Long, motivo: String?)

    @Query("UPDATE productos SET stock = stock - :cantidad WHERE id = :idProducto AND controla_stock = 1")
    suspend fun descontarStockProducto(idProducto: Long, cantidad: Double)

    @Query("UPDATE productos SET stock = stock + :cantidad WHERE id = :idProducto AND controla_stock = 1")
    suspend fun restituirStockProducto(idProducto: Long, cantidad: Double)

    // Transacción Atómica: Registrar Venta y Descontar Stock
    @Transaction
    suspend fun procesarVentaCompleta(
        venta: VentaEntidad,
        detalles: List<DetalleVentaEntidad>
    ): Long {
        val idVentaGenerada = insertarVenta(venta)
        val detallesConVentaId = detalles.map { it.copy(idVenta = idVentaGenerada) }
        insertarDetalles(detallesConVentaId)

        // Descuenta inventario para cada producto que controle stock
        for (detalle in detallesConVentaId) {
            detalle.idProducto?.let { idProd ->
                descontarStockProducto(idProd, detalle.cantidad)
            }
        }
        return idVentaGenerada
    }

    // Transacción Atómica: Anular Venta y Restituir Stock
    @Transaction
    suspend fun anularVentaYRestituirStock(idVenta: Long, motivo: String?) {
        val ventaConDetalles = obtenerVentaPorId(idVenta) ?: return
        if (ventaConDetalles.venta.estadoVenta == "Cancelada") return

        marcarVentaCancelada(idVenta, motivo)

        // Restituye inventario para cada producto
        for (detalle in ventaConDetalles.detalles) {
            detalle.idProducto?.let { idProd ->
                restituirStockProducto(idProd, detalle.cantidad)
            }
        }
    }
}
