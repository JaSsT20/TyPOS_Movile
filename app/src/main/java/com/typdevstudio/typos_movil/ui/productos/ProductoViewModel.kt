package com.typdevstudio.typos_movil.ui.productos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.typdevstudio.typos_movil.datos.local.AppBaseDatos
import com.typdevstudio.typos_movil.datos.local.GestorSesion
import com.typdevstudio.typos_movil.datos.local.entidades.ProductoEntidad
import com.typdevstudio.typos_movil.datos.repositorio.ProductoRepositorio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FiltroStock(val etiqueta: String) {
    TODOS("Todos"),
    EN_STOCK("En Stock"),
    STOCK_BAJO("Stock Bajo (≤ 5)"),
    AGOTADOS("Agotados"),
    SIN_STOCK("Servicios (Sin stock)")
}

data class FormularioProductoUiState(
    val id: Long = 0L,
    val nombre: String = "",
    val codigoBarras: String = "",
    val descripcion: String = "",
    val precioCosto: String = "",
    val precioVenta: String = "",
    val itbisIncluido: Boolean = true,
    val exentoItbis: Boolean = false,
    val tasaItbis: Double = 18.0,
    val stock: String = "0",
    val controlaStock: Boolean = true,
    val categoria: String = "General",
    val errorNombre: String? = null,
    val errorPrecioVenta: String? = null,
    val errorPrecioCosto: String? = null,
    val errorStock: String? = null,
    val mensajeErrorGeneral: String? = null,
    val estaGuardando: Boolean = false,
    val guardadoExitoso: Boolean = false
)

class ProductoViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio: ProductoRepositorio

    init {
        val baseDatos = AppBaseDatos.obtenerBaseDatos(application)
        repositorio = ProductoRepositorio(baseDatos.productoDao())
    }

    private val _busqueda = MutableStateFlow("")
    val busqueda: StateFlow<String> = _busqueda.asStateFlow()

    private val _categoriaSeleccionada = MutableStateFlow("Todas")
    val categoriaSeleccionada: StateFlow<String> = _categoriaSeleccionada.asStateFlow()

    private val _filtroStock = MutableStateFlow(FiltroStock.TODOS)
    val filtroStock: StateFlow<FiltroStock> = _filtroStock.asStateFlow()

    private val _productosOriginales = repositorio.obtenerTodosActivos()

    // Lista de categorías únicas para los filtros rápidos
    val categoriasDisponibles: StateFlow<List<String>> = _productosOriginales
        .combine(_categoriaSeleccionada) { lista, _ ->
            val categorias = lista.map { it.categoria.trim().ifEmpty { "General" } }.distinct().sorted()
            listOf("Todas") + categorias
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf("Todas")
        )

    // Lista de productos con filtro combinado (Búsqueda + Categoría + Estado de Stock)
    val productos: StateFlow<List<ProductoEntidad>> = combine(
        _productosOriginales,
        _busqueda,
        _categoriaSeleccionada,
        _filtroStock
    ) { lista, query, cat, stockFilter ->
        lista.filter { producto ->
            // Filtro de texto (nombre o código de barra)
            val coincideTexto = query.isBlank() ||
                    producto.nombre.contains(query, ignoreCase = true) ||
                    (producto.codigoBarras?.contains(query, ignoreCase = true) == true)

            // Filtro por categoría
            val coincideCategoria = cat == "Todas" || producto.categoria.equals(cat, ignoreCase = true)

            // Filtro por stock
            val coincideStock = when (stockFilter) {
                FiltroStock.TODOS -> true
                FiltroStock.EN_STOCK -> producto.controlaStock && producto.stock > 5
                FiltroStock.STOCK_BAJO -> producto.controlaStock && producto.stock in 0.01..5.0
                FiltroStock.AGOTADOS -> producto.controlaStock && producto.stock <= 0
                FiltroStock.SIN_STOCK -> !producto.controlaStock
            }

            coincideTexto && coincideCategoria && coincideStock
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _formularioState = MutableStateFlow(FormularioProductoUiState())
    val formularioState: StateFlow<FormularioProductoUiState> = _formularioState.asStateFlow()

    fun onBusquedaCambiada(nuevaBusqueda: String) {
        _busqueda.value = nuevaBusqueda
    }

    fun onCategoriaFiltroSeleccionada(categoria: String) {
        _categoriaSeleccionada.value = categoria
    }

    fun onFiltroStockSeleccionado(filtro: FiltroStock) {
        _filtroStock.value = filtro
    }

    fun limpiarFormulario() {
        _formularioState.value = FormularioProductoUiState()
    }

    fun cargarParaEditar(producto: ProductoEntidad) {
        _formularioState.value = FormularioProductoUiState(
            id = producto.id,
            nombre = producto.nombre,
            codigoBarras = producto.codigoBarras ?: "",
            descripcion = producto.descripcion ?: "",
            precioCosto = if (producto.precioCosto > 0) producto.precioCosto.toString() else "",
            precioVenta = producto.precioVenta.toString(),
            itbisIncluido = producto.itbisIncluido,
            exentoItbis = producto.exentoItbis,
            tasaItbis = producto.tasaItbis,
            stock = producto.stock.toString(),
            controlaStock = producto.controlaStock,
            categoria = producto.categoria
        )
    }

    fun onNombreCambiado(nombre: String) {
        _formularioState.update { it.copy(nombre = nombre, errorNombre = null) }
    }

    fun onCodigoBarrasCambiado(codigo: String) {
        _formularioState.update { it.copy(codigoBarras = codigo) }
    }

    fun onDescripcionCambiada(descripcion: String) {
        _formularioState.update { it.copy(descripcion = descripcion) }
    }

    fun onPrecioCostoCambiado(precio: String) {
        _formularioState.update { it.copy(precioCosto = precio, errorPrecioCosto = null) }
    }

    fun onPrecioVentaCambiado(precio: String) {
        _formularioState.update { it.copy(precioVenta = precio, errorPrecioVenta = null) }
    }

    fun onItbisIncluidoCambiado(incluido: Boolean) {
        _formularioState.update { it.copy(itbisIncluido = incluido) }
    }

    fun onExentoItbisCambiado(exento: Boolean) {
        _formularioState.update { it.copy(exentoItbis = exento) }
    }

    fun onStockCambiado(stock: String) {
        _formularioState.update { it.copy(stock = stock, errorStock = null) }
    }

    fun onControlaStockCambiado(controla: Boolean) {
        _formularioState.update { it.copy(controlaStock = controla) }
    }

    fun onCategoriaCambiada(categoria: String) {
        _formularioState.update { it.copy(categoria = categoria) }
    }

    fun guardarProducto() {
        val estado = _formularioState.value
        val nombreTrim = estado.nombre.trim()
        val precioVentaDouble = estado.precioVenta.toDoubleOrNull()
        val precioCostoDouble = estado.precioCosto.toDoubleOrNull() ?: 0.0
        val stockDouble = estado.stock.toDoubleOrNull() ?: 0.0

        var hayError = false
        var errorNombre: String? = null
        var errorPrecioVenta: String? = null

        if (nombreTrim.isEmpty()) {
            errorNombre = "El nombre del producto es obligatorio"
            hayError = true
        }

        if (precioVentaDouble == null || precioVentaDouble <= 0.0) {
            errorPrecioVenta = "Ingresa un precio de venta válido (> 0)"
            hayError = true
        }

        if (hayError) {
            _formularioState.update {
                it.copy(
                    errorNombre = errorNombre,
                    errorPrecioVenta = errorPrecioVenta
                )
            }
            return
        }

        _formularioState.update { it.copy(estaGuardando = true, mensajeErrorGeneral = null) }

        viewModelScope.launch {
            try {
                val usuarioActivo = GestorSesion.usuarioActivo.value
                val producto = ProductoEntidad(
                    id = estado.id,
                    idUsuario = usuarioActivo?.id,
                    codigoBarras = estado.codigoBarras.trim().ifEmpty { null },
                    nombre = nombreTrim,
                    descripcion = estado.descripcion.trim().ifEmpty { null },
                    precioCosto = precioCostoDouble,
                    precioVenta = precioVentaDouble!!,
                    exentoItbis = estado.exentoItbis,
                    itbisIncluido = estado.itbisIncluido,
                    tasaItbis = if (estado.exentoItbis) 0.0 else 18.0,
                    stock = stockDouble,
                    controlaStock = estado.controlaStock,
                    categoria = estado.categoria.trim().ifEmpty { "General" },
                    estaActivo = true,
                    actualizadoEn = System.currentTimeMillis()
                )

                repositorio.guardarProducto(producto)
                _formularioState.update { it.copy(estaGuardando = false, guardadoExitoso = true) }
            } catch (e: Exception) {
                _formularioState.update {
                    it.copy(
                        estaGuardando = false,
                        mensajeErrorGeneral = "Error al guardar el producto: ${e.localizedMessage ?: "Error desconocido"}"
                    )
                }
            }
        }
    }

    fun eliminarProducto(id: Long) {
        viewModelScope.launch {
            repositorio.eliminarProducto(id)
        }
    }
}
