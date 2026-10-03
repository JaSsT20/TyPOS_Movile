package com.typdevstudio.typos_movil.datos.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.typdevstudio.typos_movil.datos.local.entidades.ProductoEntidad
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {

    @Query("SELECT * FROM productos WHERE esta_activo = 1 ORDER BY nombre ASC")
    fun obtenerTodosActivos(): Flow<List<ProductoEntidad>>

    @Query("SELECT * FROM productos WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: Long): ProductoEntidad?

    @Query("SELECT * FROM productos WHERE codigo_barras = :codigoBarras AND esta_activo = 1 LIMIT 1")
    suspend fun obtenerPorCodigoBarras(codigoBarras: String): ProductoEntidad?

    @Query("SELECT * FROM productos WHERE esta_activo = 1 AND (nombre LIKE '%' || :busqueda || '%' OR codigo_barras LIKE '%' || :busqueda || '%') ORDER BY nombre ASC")
    fun buscarProductos(busqueda: String): Flow<List<ProductoEntidad>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(producto: ProductoEntidad): Long

    @Update
    suspend fun actualizar(producto: ProductoEntidad)

    @Query("UPDATE productos SET stock = stock - :cantidadVendida WHERE id = :idProducto")
    suspend fun descontarStock(idProducto: Long, cantidadVendida: Double)

    @Query("UPDATE productos SET esta_activo = 0 WHERE id = :id")
    suspend fun desactivar(id: Long)
}
