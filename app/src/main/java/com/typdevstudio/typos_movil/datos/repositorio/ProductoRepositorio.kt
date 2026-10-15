package com.typdevstudio.typos_movil.datos.repositorio

import com.typdevstudio.typos_movil.datos.local.daos.ProductoDao
import com.typdevstudio.typos_movil.datos.local.entidades.ProductoEntidad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ProductoRepositorio(private val productoDao: ProductoDao) {

    fun obtenerTodosActivos(): Flow<List<ProductoEntidad>> = productoDao.obtenerTodosActivos()

    fun buscarProductos(consulta: String): Flow<List<ProductoEntidad>> {
        return if (consulta.isBlank()) {
            productoDao.obtenerTodosActivos()
        } else {
            productoDao.buscarProductos(consulta.trim())
        }
    }

    suspend fun obtenerPorId(id: Long): ProductoEntidad? = withContext(Dispatchers.IO) {
        productoDao.obtenerPorId(id)
    }

    suspend fun obtenerPorCodigoBarras(codigo: String): ProductoEntidad? = withContext(Dispatchers.IO) {
        productoDao.obtenerPorCodigoBarras(codigo.trim())
    }

    suspend fun guardarProducto(producto: ProductoEntidad): Long = withContext(Dispatchers.IO) {
        if (producto.id == 0L) {
            productoDao.insertar(producto)
        } else {
            productoDao.actualizar(producto)
            producto.id
        }
    }

    suspend fun guardarProductosEnLote(productos: List<ProductoEntidad>): Int = withContext(Dispatchers.IO) {
        if (productos.isEmpty()) return@withContext 0
        productoDao.insertarTodos(productos).size
    }

    suspend fun obtenerTodosDirecto(): List<ProductoEntidad> = withContext(Dispatchers.IO) {
        productoDao.obtenerTodosDirecto()
    }

    /** Todos los productos, activos y desactivados (para el catálogo). */
    fun obtenerTodos(): Flow<List<ProductoEntidad>> = productoDao.obtenerTodos()

    /** Deja de usarse en toda la app (ventas, búsqueda, importación), pero se puede reactivar. */
    suspend fun desactivarProducto(id: Long) = withContext(Dispatchers.IO) {
        productoDao.desactivar(id)
    }

    /**
     * Vuelve a activar el producto. `null` si se pudo; si no, el motivo (otro producto activo ya usa su código de
     * barras).
     */
    suspend fun reactivarProducto(id: Long): String? = withContext(Dispatchers.IO) {
        val producto = productoDao.obtenerPorId(id) ?: return@withContext "El producto ya no existe."
        val codigo = producto.codigoBarras?.takeIf { it.isNotBlank() }
        val otro = codigo?.let { productoDao.obtenerPorCodigoBarras(it) }
        if (otro != null && otro.id != id) {
            return@withContext "No se puede reactivar: el producto \"${otro.nombre}\" ya usa el código $codigo."
        }
        productoDao.reactivar(id)
        null
    }

    /** Borra de verdad un producto desactivado. `false` si no estaba desactivado. */
    suspend fun eliminarDefinitivamente(id: Long): Boolean = withContext(Dispatchers.IO) {
        productoDao.eliminarDesactivado(id) > 0
    }
}
