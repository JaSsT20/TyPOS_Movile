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

    suspend fun eliminarProducto(id: Long) = withContext(Dispatchers.IO) {
        productoDao.desactivar(id)
    }
}
