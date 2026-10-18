package com.typdevstudio.typos_movil.ui.productos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.typdevstudio.typos_movil.datos.local.AppBaseDatos
import com.typdevstudio.typos_movil.datos.local.GestorSesion
import com.typdevstudio.typos_movil.datos.local.entidades.ProductoEntidad
import com.typdevstudio.typos_movil.datos.repositorio.ProductoRepositorio
import com.typdevstudio.typos_movil.utilidades.licencia.LicenciaGuard
import com.typdevstudio.typos_movil.utilidades.licencia.ModulosLicencia
import com.typdevstudio.typos_movil.utilidades.licencia.SituacionLicencia
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
    PRODUCTOS("Productos"),
    SERVICIOS("Servicios"),
    EN_STOCK("En Stock"),
    STOCK_BAJO("Stock Bajo (≤ 5)"),
    AGOTADOS("Agotados"),
    SIN_CONTROL_STOCK("Sin Control de Stock"),

    /** Productos desactivados: no se usan en ninguna parte de la app; desde aquí se reactivan o se eliminan. */
    DESACTIVADOS("Desactivados")
}

data class FormularioProductoUiState(
    val id: Long = 0L,
    val nombre: String = "",
    val tipoItem: String = "Producto", // "Producto" o "Servicio"
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
    val imagenes: List<String> = emptyList(),
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

    // Activos y desactivados: el filtro "Desactivados" muestra solo estos; el resto, solo los activos.
    private val _productosOriginales = repositorio.obtenerTodos()

    // Lista de categorías únicas para los filtros rápidos
    val categoriasDisponibles: StateFlow<List<String>> = _productosOriginales
        .combine(_categoriaSeleccionada) { lista, _ ->
            val categorias = lista.filter { it.estaActivo }.map { it.categoria.trim().ifEmpty { "General" } }.distinct().sorted()
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

            // Los desactivados solo aparecen en su propio filtro; el resto de filtros muestra solo activos.
            val coincideEstado = producto.estaActivo != (stockFilter == FiltroStock.DESACTIVADOS)

            // Filtro por stock / tipo
            val coincideStock = when (stockFilter) {
                FiltroStock.TODOS, FiltroStock.DESACTIVADOS -> true
                FiltroStock.PRODUCTOS -> producto.tipoItem.equals("Producto", ignoreCase = true)
                FiltroStock.SERVICIOS -> producto.tipoItem.equals("Servicio", ignoreCase = true)
                FiltroStock.EN_STOCK -> producto.tipoItem.equals("Producto", ignoreCase = true) && producto.controlaStock && producto.stock > 5
                FiltroStock.STOCK_BAJO -> producto.tipoItem.equals("Producto", ignoreCase = true) && producto.controlaStock && producto.stock in 0.01..5.0
                FiltroStock.AGOTADOS -> producto.tipoItem.equals("Producto", ignoreCase = true) && producto.controlaStock && producto.stock <= 0
                FiltroStock.SIN_CONTROL_STOCK -> producto.tipoItem.equals("Producto", ignoreCase = true) && !producto.controlaStock
            }

            coincideTexto && coincideCategoria && coincideEstado && coincideStock
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
            tipoItem = producto.tipoItem,
            codigoBarras = producto.codigoBarras ?: "",
            descripcion = producto.descripcion ?: "",
            precioCosto = if (producto.precioCosto > 0) producto.precioCosto.toString() else "",
            precioVenta = producto.precioVenta.toString(),
            itbisIncluido = producto.itbisIncluido,
            exentoItbis = producto.exentoItbis,
            tasaItbis = producto.tasaItbis,
            stock = producto.stock.toString(),
            controlaStock = producto.controlaStock,
            categoria = producto.categoria,
            imagenes = producto.listaImagenes
        )
    }

    fun onImagenesAgregadas(nuevasRutas: List<String>) {
        _formularioState.update { estado ->
            val listaCombinada = (estado.imagenes + nuevasRutas).distinct().take(3)
            estado.copy(imagenes = listaCombinada)
        }
    }

    fun onEliminarImagen(indice: Int) {
        _formularioState.update { estado ->
            if (indice in estado.imagenes.indices) {
                val nuevaLista = estado.imagenes.toMutableList().apply { removeAt(indice) }
                estado.copy(imagenes = nuevaLista)
            } else estado
        }
    }

    fun onMoverImagenAPrincipal(indice: Int) {
        _formularioState.update { estado ->
            if (indice in 1 until estado.imagenes.size) {
                val elemento = estado.imagenes[indice]
                val nuevaLista = mutableListOf(elemento) + estado.imagenes.filterIndexed { i, _ -> i != indice }
                estado.copy(imagenes = nuevaLista)
            } else estado
        }
    }

    fun onTipoItemCambiado(tipo: String) {
        _formularioState.update { estado ->
            estado.copy(
                tipoItem = tipo,
                controlaStock = if (tipo == "Servicio") false else estado.controlaStock
            )
        }
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
            errorNombre = "El nombre del producto o servicio es obligatorio"
            hayError = true
        }

        if (precioVentaDouble == null || precioVentaDouble <= 0.0) {
            errorPrecioVenta = "Ingrese un precio de venta válido (> 0)"
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
            if (!LicenciaGuard.puedeEmitir()) {
                _formularioState.update {
                    it.copy(estaGuardando = false, mensajeErrorGeneral = LicenciaGuard.mensajeAccionBloqueada())
                }
                return@launch
            }
            // Sin el módulo de inventario un producto nuevo no controla stock (si no, quedaría con 0 unidades y no se
            // podría vender). Los productos existentes conservan lo que ya tenían: el formulario no muestra esos campos.
            val sinInventario = !LicenciaGuard.situacion.value.moduloIncluido(ModulosLicencia.INVENTARIO)
            val esNuevo = estado.id == 0L
            try {
                val usuarioActivo = GestorSesion.usuarioActivo.value
                val producto = ProductoEntidad(
                    id = estado.id,
                    idUsuario = usuarioActivo?.id,
                    codigoBarras = estado.codigoBarras.trim().ifEmpty { null },
                    nombre = nombreTrim,
                    tipoItem = estado.tipoItem,
                    descripcion = estado.descripcion.trim().ifEmpty { null },
                    precioCosto = precioCostoDouble,
                    precioVenta = precioVentaDouble!!,
                    exentoItbis = estado.exentoItbis,
                    itbisIncluido = estado.itbisIncluido,
                    tasaItbis = if (estado.exentoItbis) 0.0 else 18.0,
                    stock = if (sinInventario && esNuevo) 0.0 else stockDouble,
                    controlaStock = if (sinInventario && esNuevo) false else estado.controlaStock,
                    categoria = estado.categoria.trim().ifEmpty { "General" },
                    estaActivo = true,
                    imagenesUri = if (estado.imagenes.isNotEmpty()) estado.imagenes.joinToString(",") else null,
                    actualizadoEn = System.currentTimeMillis()
                )

                repositorio.guardarProducto(producto)
                _formularioState.update { it.copy(estaGuardando = false, guardadoExitoso = true) }
            } catch (e: Exception) {
                _formularioState.update {
                    it.copy(
                        estaGuardando = false,
                        mensajeErrorGeneral = "Error al guardar: ${e.localizedMessage ?: "Error desconocido"}"
                    )
                }
            }
        }
    }

    /** Deja de usarse en toda la app (ventas, búsqueda, importación) y pasa al filtro "Desactivados". */
    fun desactivarProducto(producto: ProductoEntidad) = conLicencia {
        repositorio.desactivarProducto(producto.id)
        "\"${producto.nombre}\" se desactivó. Lo encuentras en el filtro Desactivados."
    }

    fun reactivarProducto(producto: ProductoEntidad) = conLicencia {
        repositorio.reactivarProducto(producto.id) ?: "\"${producto.nombre}\" está activo otra vez."
    }

    /** Solo para productos desactivados. Las ventas ya hechas no se pierden. */
    fun eliminarDefinitivamente(producto: ProductoEntidad) = conLicencia {
        if (repositorio.eliminarDefinitivamente(producto.id)) "\"${producto.nombre}\" se eliminó."
        else "Solo se pueden eliminar productos desactivados."
    }

    /** Las acciones que modifican el catálogo respetan el modo solo consulta; [accion] devuelve el aviso a mostrar. */
    private fun conLicencia(accion: suspend () -> String) {
        viewModelScope.launch {
            _mensajeAviso.value =
                if (!LicenciaGuard.puedeEmitir()) SituacionLicencia.MENSAJE_ACCION_BLOQUEADA_CORTO else accion()
        }
    }

    // Avisos puntuales de la lista de productos (por ejemplo, una acción bloqueada por la licencia)
    private val _mensajeAviso = MutableStateFlow<String?>(null)
    val mensajeAviso: StateFlow<String?> = _mensajeAviso.asStateFlow()

    fun limpiarAviso() {
        _mensajeAviso.value = null
    }
}
