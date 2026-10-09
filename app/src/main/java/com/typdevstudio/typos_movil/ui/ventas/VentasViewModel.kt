package com.typdevstudio.typos_movil.ui.ventas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.typdevstudio.typos_movil.datos.local.AppBaseDatos
import com.typdevstudio.typos_movil.datos.local.GestorSesion
import com.typdevstudio.typos_movil.datos.local.entidades.DetalleVentaEntidad
import com.typdevstudio.typos_movil.datos.local.entidades.ProductoEntidad
import com.typdevstudio.typos_movil.datos.local.entidades.VentaEntidad
import com.typdevstudio.typos_movil.datos.repositorio.ProductoRepositorio
import com.typdevstudio.typos_movil.datos.repositorio.VentaRepositorio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FormularioCobroUiState(
    val metodoPago: String = "Efectivo", // "Efectivo", "Transferencia", "Cheque", "Mixto"
    val montoRecibido: String = "",
    val montoEfectivo: String = "",
    val montoTransferencia: String = "",
    val referenciaTransferencia: String = "",
    val montoCheque: String = "",
    val numeroCheque: String = "",
    val bancoCheque: String = "",
    val nombreCliente: String = "",
    val rncCedulaCliente: String = "",
    val mensajeError: String? = null,
    val estaProcesando: Boolean = false
)

class VentasViewModel(application: Application) : AndroidViewModel(application) {

    private val productoRepositorio: ProductoRepositorio
    private val ventaRepositorio: VentaRepositorio
    private val configuracionRepositorio: com.typdevstudio.typos_movil.datos.repositorio.ConfiguracionRepositorio

    init {
        val bd = AppBaseDatos.obtenerBaseDatos(application)
        productoRepositorio = ProductoRepositorio(bd.productoDao())
        ventaRepositorio = VentaRepositorio(bd.ventaDao())
        configuracionRepositorio = com.typdevstudio.typos_movil.datos.repositorio.ConfiguracionRepositorio(bd.configuracionNegocioDao())
    }

    private val _busqueda = MutableStateFlow("")
    val busqueda: StateFlow<String> = _busqueda.asStateFlow()

    private val _categoriaSeleccionada = MutableStateFlow("Todas")
    val categoriaSeleccionada: StateFlow<String> = _categoriaSeleccionada.asStateFlow()

    private val _productosOriginales = productoRepositorio.obtenerTodosActivos()

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

    val productos: StateFlow<List<ProductoEntidad>> = combine(
        _productosOriginales,
        _busqueda,
        _categoriaSeleccionada
    ) { lista, query, cat ->
        lista.filter { producto ->
            val coincideTexto = query.isBlank() ||
                    producto.nombre.contains(query, ignoreCase = true) ||
                    (producto.codigoBarras?.contains(query, ignoreCase = true) == true)

            val coincideCategoria = cat == "Todas" || producto.categoria.equals(cat, ignoreCase = true)
            coincideTexto && coincideCategoria
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Estado del Carrito
    private val _carrito = MutableStateFlow<List<ItemCarrito>>(emptyList())
    val carrito: StateFlow<List<ItemCarrito>> = _carrito.asStateFlow()

    // Estado del Formulario de Cobro
    private val _cobroState = MutableStateFlow(FormularioCobroUiState())
    val cobroState: StateFlow<FormularioCobroUiState> = _cobroState.asStateFlow()

    // Venta completada para mostrar recibo / modal de éxito
    private val _ultimaVentaRealizada = MutableStateFlow<VentaEntidad?>(null)
    val ultimaVentaRealizada: StateFlow<VentaEntidad?> = _ultimaVentaRealizada.asStateFlow()

    private val _detallesUltimaVenta = MutableStateFlow<List<DetalleVentaEntidad>>(emptyList())
    val detallesUltimaVenta: StateFlow<List<DetalleVentaEntidad>> = _detallesUltimaVenta.asStateFlow()

    private val _mensajeAlerta = MutableStateFlow<String?>(null)
    val mensajeAlerta: StateFlow<String?> = _mensajeAlerta.asStateFlow()

    fun limpiarAlerta() {
        _mensajeAlerta.value = null
    }

    fun onBusquedaCambiada(nuevaBusqueda: String) {
        _busqueda.value = nuevaBusqueda
    }

    fun onCategoriaSeleccionada(categoria: String) {
        _categoriaSeleccionada.value = categoria
    }

    // Al escanear un código de barras con la cámara en Caja
    fun escanearYAgregarAlCarrito(codigoBarras: String) {
        viewModelScope.launch {
            val producto = productoRepositorio.obtenerPorCodigoBarras(codigoBarras)
            if (producto != null) {
                agregarAlCarrito(producto)
            } else {
                _busqueda.value = codigoBarras
                _mensajeAlerta.value = "No se encontró ningún producto con el código '$codigoBarras'"
            }
        }
    }

    fun agregarAlCarrito(producto: ProductoEntidad): Boolean {
        if (producto.controlaStock && producto.stock <= 0) {
            _mensajeAlerta.value = "¡Producto Agotado! No hay stock disponible para '${producto.nombre}'."
            return false
        }

        var agregadoConExito = true
        _carrito.update { listaActual ->
            val index = listaActual.indexOfFirst { it.producto.id == producto.id }
            if (index != -1) {
                val itemExistente = listaActual[index]
                val nuevaCantidad = itemExistente.cantidad + 1.0
                if (producto.controlaStock && nuevaCantidad > producto.stock) {
                    _mensajeAlerta.value = "Stock límite alcanzado: Solo hay ${producto.stock.toInt()} unidad(es) de '${producto.nombre}'."
                    agregadoConExito = false
                    listaActual
                } else {
                    val listaMutada = listaActual.toMutableList()
                    listaMutada[index] = itemExistente.copy(cantidad = nuevaCantidad)
                    listaMutada
                }
            } else {
                if (producto.controlaStock && 1.0 > producto.stock) {
                    _mensajeAlerta.value = "Stock insuficiente: '${producto.nombre}' tiene stock ${producto.stock.toInt()}."
                    agregadoConExito = false
                    listaActual
                } else {
                    listaActual + ItemCarrito(producto = producto, cantidad = 1.0)
                }
            }
        }
        return agregadoConExito
    }

    fun incrementarCantidad(idProducto: Long) {
        _carrito.update { lista ->
            lista.map { item ->
                if (item.producto.id == idProducto) {
                    val nuevaCantidad = item.cantidad + 1.0
                    if (item.producto.controlaStock && nuevaCantidad > item.producto.stock) {
                        _mensajeAlerta.value = "No puedes agregar más: Stock máximo disponible de '${item.producto.nombre}' es ${item.producto.stock.toInt()}."
                        item
                    } else {
                        item.copy(cantidad = nuevaCantidad)
                    }
                } else item
            }
        }
    }

    fun decrementarCantidad(idProducto: Long) {
        _carrito.update { lista ->
            lista.mapNotNull { item ->
                if (item.producto.id == idProducto) {
                    if (item.cantidad > 1.0) {
                        item.copy(cantidad = item.cantidad - 1.0)
                    } else null // Elimina del carrito si llega a 0
                } else item
            }
        }
    }

    fun eliminarDelCarrito(idProducto: Long) {
        _carrito.update { lista ->
            lista.filterNot { it.producto.id == idProducto }
        }
    }

    fun vaciarCarrito() {
        _carrito.value = emptyList()
    }

    // Cálculos de Totales de Venta
    fun obtenerSubTotal(): Double = _carrito.value.sumOf { it.subTotal }
    fun obtenerDescuentoTotal(): Double = _carrito.value.sumOf { it.descuento }
    fun obtenerSubTotalNeto(): Double = _carrito.value.sumOf { it.subTotalNeto }
    fun obtenerMontoItbis(): Double = _carrito.value.sumOf { it.montoItbis }
    fun obtenerTotal(): Double = _carrito.value.sumOf { it.total }
    fun obtenerCantidadArticulos(): Int = _carrito.value.sumOf { it.cantidad.toInt() }

    // Manejo de Estados de Cobro
    fun onMetodoPagoCambiado(metodo: String) {
        _cobroState.update { it.copy(metodoPago = metodo, mensajeError = null) }
    }

    fun onMontoRecibidoCambiado(monto: String) {
        _cobroState.update { it.copy(montoRecibido = monto, mensajeError = null) }
    }

    fun onMontoEfectivoCambiado(monto: String) {
        _cobroState.update { it.copy(montoEfectivo = monto, mensajeError = null) }
    }

    fun onMontoTransferenciaCambiado(monto: String) {
        _cobroState.update { it.copy(montoTransferencia = monto, mensajeError = null) }
    }

    fun onReferenciaTransferenciaCambiada(ref: String) {
        _cobroState.update { it.copy(referenciaTransferencia = ref, mensajeError = null) }
    }

    fun onMontoChequeCambiado(monto: String) {
        _cobroState.update { it.copy(montoCheque = monto, mensajeError = null) }
    }

    fun onNumeroChequeCambiado(num: String) {
        _cobroState.update { it.copy(numeroCheque = num, mensajeError = null) }
    }

    fun onBancoChequeCambiado(banco: String) {
        _cobroState.update { it.copy(bancoCheque = banco, mensajeError = null) }
    }

    fun onNombreClienteCambiado(nombre: String) {
        _cobroState.update { it.copy(nombreCliente = nombre) }
    }

    fun onRncCedulaClienteCambiado(rnc: String) {
        _cobroState.update { it.copy(rncCedulaCliente = rnc) }
    }

    fun procesarCobro(confirmarMenorMonto: Boolean = false, alTerminarExito: () -> Unit) {
        val totalVenta = obtenerTotal()
        val estadoCobro = _cobroState.value
        val metodo = estadoCobro.metodoPago

        var montoRecibidoDouble = 0.0
        var montoDevueltoDouble = 0.0
        var montoEfectivo = 0.0
        var montoTransferencia = 0.0
        var montoCheque = 0.0

        when (metodo) {
            "Efectivo" -> {
                montoRecibidoDouble = estadoCobro.montoRecibido.toDoubleOrNull() ?: totalVenta
                if (montoRecibidoDouble <= 0.0) {
                    _cobroState.update { it.copy(mensajeError = "Ingresa un monto recibido válido mayor a 0") }
                    return
                }
                if (montoRecibidoDouble < totalVenta && !confirmarMenorMonto) {
                    _cobroState.update { it.copy(mensajeError = "El monto recibido es menor al total a pagar ($${String.format("%.2f", totalVenta)})") }
                    return
                }
                montoEfectivo = if (montoRecibidoDouble >= totalVenta) totalVenta else montoRecibidoDouble
                montoDevueltoDouble = (montoRecibidoDouble - totalVenta).coerceAtLeast(0.0)
            }
            "Transferencia" -> {
                montoTransferencia = totalVenta
                montoRecibidoDouble = totalVenta
                montoDevueltoDouble = 0.0
            }
            "Cheque" -> {
                montoCheque = totalVenta
                montoRecibidoDouble = totalVenta
                montoDevueltoDouble = 0.0
            }
            "Mixto" -> {
                montoEfectivo = estadoCobro.montoEfectivo.toDoubleOrNull() ?: 0.0
                montoTransferencia = estadoCobro.montoTransferencia.toDoubleOrNull() ?: 0.0
                montoCheque = estadoCobro.montoCheque.toDoubleOrNull() ?: 0.0
                val sumaPagos = montoEfectivo + montoTransferencia + montoCheque

                if (sumaPagos <= 0.0) {
                    _cobroState.update { it.copy(mensajeError = "Ingresa al menos un monto de pago válido mayor a 0") }
                    return
                }
                if (sumaPagos < totalVenta && !confirmarMenorMonto) {
                    _cobroState.update { it.copy(mensajeError = "La suma de los montos ($${String.format("%.2f", sumaPagos)}) no cubre el total de la venta ($${String.format("%.2f", totalVenta)})") }
                    return
                }
                montoRecibidoDouble = sumaPagos
                montoDevueltoDouble = (sumaPagos - totalVenta).coerceAtLeast(0.0)
            }
        }

        _cobroState.update { it.copy(estaProcesando = true, mensajeError = null) }

        viewModelScope.launch {
            try {
                // Validación de stock en tiempo real
                val itemsActuales = _carrito.value
                for (item in itemsActuales) {
                    if (item.producto.controlaStock) {
                        val productoActualizado = productoRepositorio.obtenerPorId(item.producto.id)
                        val stockDisponible = productoActualizado?.stock ?: item.producto.stock
                        if (item.cantidad > stockDisponible) {
                            _cobroState.update {
                                it.copy(
                                    estaProcesando = false,
                                    mensajeError = "Stock insuficiente para '${item.producto.nombre}'. En carrito: ${item.cantidad.toInt()}, disponible: ${stockDisponible.toInt()}."
                                )
                            }
                            return@launch
                        }
                    }
                }

                val usuarioActual = GestorSesion.usuarioActivo.value
                val idUsuario = usuarioActual?.id ?: 1L
                val nombreUsuario = usuarioActual?.nombreCompleto ?: "Administrador"

                val numeroFactura = ventaRepositorio.generarNumeroFactura()

                val venta = VentaEntidad(
                    numeroFactura = numeroFactura,
                    fecha = System.currentTimeMillis(),
                    idUsuario = idUsuario,
                    usuario = nombreUsuario,
                    nombreCliente = estadoCobro.nombreCliente.trim().ifEmpty { null },
                    rncCedulaCliente = estadoCobro.rncCedulaCliente.trim().ifEmpty { null },
                    subTotal = obtenerSubTotal(),
                    descuento = obtenerDescuentoTotal(),
                    subTotalNeto = obtenerSubTotalNeto(),
                    montoItbis = obtenerMontoItbis(),
                    total = totalVenta,
                    montoRecibido = montoRecibidoDouble,
                    montoDevuelto = montoDevueltoDouble,
                    estadoVenta = "Completada",
                    metodoPago = metodo,
                    montoEfectivo = montoEfectivo,
                    montoTransferencia = montoTransferencia,
                    referenciaTransferencia = estadoCobro.referenciaTransferencia.trim().ifEmpty { null },
                    montoCheque = montoCheque,
                    numeroCheque = estadoCobro.numeroCheque.trim().ifEmpty { null },
                    bancoCheque = estadoCobro.bancoCheque.trim().ifEmpty { null }
                )

                val detalles = _carrito.value.map { item ->
                    DetalleVentaEntidad(
                        idVenta = 0L,
                        idProducto = item.producto.id,
                        codigoProducto = item.producto.codigoBarras,
                        nombre = item.producto.nombre,
                        precio = item.precioUnitario,
                        cantidad = item.cantidad,
                        subTotal = item.subTotal,
                        descuento = item.descuento,
                        subTotalNeto = item.subTotalNeto,
                        tasaItbis = item.tasaItbis,
                        montoItbis = item.montoItbis,
                        total = item.total,
                        tipo = "Producto"
                    )
                }

                val idVenta = ventaRepositorio.registrarVenta(venta, detalles)
                val ventaGuardada = venta.copy(id = idVenta)

                _ultimaVentaRealizada.value = ventaGuardada
                _detallesUltimaVenta.value = detalles
                _carrito.value = emptyList()
                _cobroState.value = FormularioCobroUiState()

                alTerminarExito()
            } catch (e: Exception) {
                _cobroState.update {
                    it.copy(
                        estaProcesando = false,
                        mensajeError = "Error al procesar la venta: ${e.localizedMessage ?: "Error desconocido"}"
                    )
                }
            }
        }
    }

    fun imprimirTicketUltimaVenta(alTerminar: (com.typdevstudio.typos_movil.datos.impresion.ResultadoImpresion) -> Unit) {
        val venta = _ultimaVentaRealizada.value ?: return
        val detalles = _detallesUltimaVenta.value

        viewModelScope.launch {
            val config = configuracionRepositorio.obtenerConfiguracionDirecta()
            val mac = config.direccionMacImpresora

            if (mac.isNullOrBlank()) {
                alTerminar(com.typdevstudio.typos_movil.datos.impresion.ResultadoImpresion.Error("No hay impresora Bluetooth vinculada en Configuración"))
                return@launch
            }

            val resultado = com.typdevstudio.typos_movil.datos.impresion.ServicioImpresoraBluetooth.imprimirTicketVenta(
                direccionMac = mac,
                tamanoPapel = config.tamanoPapelImpresora,
                configuracion = config,
                venta = venta,
                detalles = detalles
            )
            alTerminar(resultado)
        }
    }

    fun cerrarModalVentaExitosa() {
        _ultimaVentaRealizada.value = null
        _detallesUltimaVenta.value = emptyList()
    }
}
