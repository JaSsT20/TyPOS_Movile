package com.typdevstudio.typos_movil.ui.historial

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.typdevstudio.typos_movil.datos.impresion.ResultadoImpresion
import com.typdevstudio.typos_movil.datos.impresion.ServicioImpresoraBluetooth
import com.typdevstudio.typos_movil.datos.local.AppBaseDatos
import com.typdevstudio.typos_movil.datos.local.relaciones.VentaConDetalles
import com.typdevstudio.typos_movil.datos.repositorio.ConfiguracionRepositorio
import com.typdevstudio.typos_movil.datos.repositorio.VentaRepositorio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

enum class FiltroFechaHistorial(val etiqueta: String) {
    HOY("Hoy"),
    ULTIMOS_7_DIAS("Últimos 7 días"),
    ESTE_MES("Este Mes"),
    TODAS("Todas las fechas")
}

enum class FiltroEstadoHistorial(val etiqueta: String) {
    TODAS("Todas"),
    COMPLETADAS("Completadas"),
    CANCELADAS("Anuladas")
}

data class ResumenVentasUi(
    val totalFacturado: Double = 0.0,
    val totalItbis: Double = 0.0,
    val cantidadCompletadas: Int = 0,
    val cantidadCanceladas: Int = 0
)

class HistorialViewModel(application: Application) : AndroidViewModel(application) {

    private val ventaRepositorio: VentaRepositorio
    private val configuracionRepositorio: ConfiguracionRepositorio

    init {
        val bd = AppBaseDatos.obtenerBaseDatos(application)
        ventaRepositorio = VentaRepositorio(bd.ventaDao())
        configuracionRepositorio = ConfiguracionRepositorio(bd.configuracionNegocioDao())
    }

    private val _busqueda = MutableStateFlow("")
    val busqueda: StateFlow<String> = _busqueda.asStateFlow()

    private val _filtroFecha = MutableStateFlow(FiltroFechaHistorial.HOY)
    val filtroFecha: StateFlow<FiltroFechaHistorial> = _filtroFecha.asStateFlow()

    private val _filtroEstado = MutableStateFlow(FiltroEstadoHistorial.TODAS)
    val filtroEstado: StateFlow<FiltroEstadoHistorial> = _filtroEstado.asStateFlow()

    private val _ventasOriginales = ventaRepositorio.obtenerTodasLasVentas()

    val ventas: StateFlow<List<VentaConDetalles>> = combine(
        _ventasOriginales,
        _busqueda,
        _filtroFecha,
        _filtroEstado
    ) { lista, query, fechaFiltro, estadoFiltro ->
        lista.filter { ventaConDetalles ->
            val venta = ventaConDetalles.venta
            val coincideQuery = query.isBlank() ||
                    venta.numeroFactura.contains(query, ignoreCase = true) ||
                    (venta.nombreCliente?.contains(query, ignoreCase = true) == true) ||
                    (venta.rncCedulaCliente?.contains(query, ignoreCase = true) == true) ||
                    (venta.usuario?.contains(query, ignoreCase = true) == true)

            val coincideFecha = coincideConFiltroFecha(venta.fecha, fechaFiltro)

            val coincideEstado = when (estadoFiltro) {
                FiltroEstadoHistorial.TODAS -> true
                FiltroEstadoHistorial.COMPLETADAS -> venta.estadoVenta.equals("Completada", ignoreCase = true)
                FiltroEstadoHistorial.CANCELADAS -> venta.estadoVenta.equals("Cancelada", ignoreCase = true)
            }

            coincideQuery && coincideFecha && coincideEstado
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val resumen: StateFlow<ResumenVentasUi> = ventas.combine(_filtroEstado) { lista, _ ->
        val completadas = lista.filter { it.venta.estadoVenta.equals("Completada", ignoreCase = true) }
        val canceladas = lista.filter { it.venta.estadoVenta.equals("Cancelada", ignoreCase = true) }
        ResumenVentasUi(
            totalFacturado = completadas.sumOf { it.venta.total },
            totalItbis = completadas.sumOf { it.venta.montoItbis },
            cantidadCompletadas = completadas.size,
            cantidadCanceladas = canceladas.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ResumenVentasUi()
    )

    // Detalle de Venta Seleccionada para Modal / Bottom Sheet
    private val _ventaSeleccionada = MutableStateFlow<VentaConDetalles?>(null)
    val ventaSeleccionada: StateFlow<VentaConDetalles?> = _ventaSeleccionada.asStateFlow()

    // Estado del proceso de anulación
    private val _mostrarDialogoAnular = MutableStateFlow(false)
    val mostrarDialogoAnular: StateFlow<Boolean> = _mostrarDialogoAnular.asStateFlow()

    private val _motivoAnulacion = MutableStateFlow("")
    val motivoAnulacion: StateFlow<String> = _motivoAnulacion.asStateFlow()

    private val _estaAnulando = MutableStateFlow(false)
    val estaAnulando: StateFlow<Boolean> = _estaAnulando.asStateFlow()

    private val _estaReimprimiendo = MutableStateFlow(false)
    val estaReimprimiendo: StateFlow<Boolean> = _estaReimprimiendo.asStateFlow()

    private val _mensajeAlerta = MutableStateFlow<String?>(null)
    val mensajeAlerta: StateFlow<String?> = _mensajeAlerta.asStateFlow()

    private val _esErrorAlerta = MutableStateFlow(false)
    val esErrorAlerta: StateFlow<Boolean> = _esErrorAlerta.asStateFlow()

    fun onBusquedaCambiada(query: String) {
        _busqueda.value = query
    }

    fun onFiltroFechaSeleccionado(filtro: FiltroFechaHistorial) {
        _filtroFecha.value = filtro
    }

    fun onFiltroEstadoSeleccionado(filtro: FiltroEstadoHistorial) {
        _filtroEstado.value = filtro
    }

    fun seleccionarVenta(ventaConDetalles: VentaConDetalles?) {
        _ventaSeleccionada.value = ventaConDetalles
    }

    fun iniciarAnulacion() {
        _motivoAnulacion.value = ""
        _mostrarDialogoAnular.value = true
    }

    fun cancelarAnulacion() {
        _mostrarDialogoAnular.value = false
        _motivoAnulacion.value = ""
    }

    fun onMotivoAnulacionCambiado(motivo: String) {
        _motivoAnulacion.value = motivo
    }

    fun esVentaDeHoy(fechaTimestamp: Long): Boolean {
        val calVenta = Calendar.getInstance().apply { timeInMillis = fechaTimestamp }
        val calHoy = Calendar.getInstance()
        return calVenta.get(Calendar.YEAR) == calHoy.get(Calendar.YEAR) &&
                calVenta.get(Calendar.DAY_OF_YEAR) == calHoy.get(Calendar.DAY_OF_YEAR)
    }

    private fun coincideConFiltroFecha(fechaTimestamp: Long, filtro: FiltroFechaHistorial): Boolean {
        val calVenta = Calendar.getInstance().apply { timeInMillis = fechaTimestamp }
        val calHoy = Calendar.getInstance()

        return when (filtro) {
            FiltroFechaHistorial.HOY -> esVentaDeHoy(fechaTimestamp)
            FiltroFechaHistorial.ULTIMOS_7_DIAS -> {
                val limite7Dias = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.DAY_OF_YEAR, -6)
                }
                fechaTimestamp >= limite7Dias.timeInMillis
            }
            FiltroFechaHistorial.ESTE_MES -> {
                calVenta.get(Calendar.YEAR) == calHoy.get(Calendar.YEAR) &&
                        calVenta.get(Calendar.MONTH) == calHoy.get(Calendar.MONTH)
            }
            FiltroFechaHistorial.TODAS -> true
        }
    }

    fun confirmarAnulacionVenta() {
        val ventaActual = _ventaSeleccionada.value ?: return
        val idVenta = ventaActual.venta.id

        if (ventaActual.venta.estadoVenta.equals("Cancelada", ignoreCase = true)) {
            _mensajeAlerta.value = "Esta venta ya se encuentra anulada."
            _esErrorAlerta.value = true
            _mostrarDialogoAnular.value = false
            return
        }

        if (!esVentaDeHoy(ventaActual.venta.fecha)) {
            _mensajeAlerta.value = "Solo está permitido anular ventas realizadas el día de hoy."
            _esErrorAlerta.value = true
            _mostrarDialogoAnular.value = false
            return
        }

        val motivo = _motivoAnulacion.value.trim().ifEmpty { "Anulada por solicitud de caja" }

        _estaAnulando.value = true
        _mostrarDialogoAnular.value = false

        viewModelScope.launch {
            try {
                ventaRepositorio.anularVenta(idVenta, motivo)
                val ventaActualizada = ventaRepositorio.obtenerVentaPorId(idVenta)
                _ventaSeleccionada.value = ventaActualizada
                _mensajeAlerta.value = "Venta anulada correctamente. El inventario ha sido restituido."
                _esErrorAlerta.value = false
            } catch (e: Exception) {
                _mensajeAlerta.value = "Error al anular la venta: ${e.localizedMessage ?: "Error desconocido"}"
                _esErrorAlerta.value = true
            } finally {
                _estaAnulando.value = false
            }
        }
    }

    fun reimprimirTicket(ventaConDetalles: VentaConDetalles) {
        viewModelScope.launch {
            _estaReimprimiendo.value = true
            try {
                val config = configuracionRepositorio.obtenerConfiguracionDirecta()
                val mac = config.direccionMacImpresora

                if (mac.isNullOrBlank()) {
                    _mensajeAlerta.value = "No hay impresora Bluetooth configurada. Ve a Configuración para vincularla."
                    _esErrorAlerta.value = true
                    _estaReimprimiendo.value = false
                    return@launch
                }

                val resultado = ServicioImpresoraBluetooth.imprimirTicketVenta(
                    direccionMac = mac,
                    tamanoPapel = config.tamanoPapelImpresora,
                    configuracion = config,
                    venta = ventaConDetalles.venta,
                    detalles = ventaConDetalles.detalles
                )

                when (resultado) {
                    is ResultadoImpresion.Exito -> {
                        _mensajeAlerta.value = "¡Ticket reimpreso exitosamente!"
                        _esErrorAlerta.value = false
                    }
                    is ResultadoImpresion.Error -> {
                        _mensajeAlerta.value = resultado.mensaje
                        _esErrorAlerta.value = true
                    }
                }
            } catch (e: Exception) {
                _mensajeAlerta.value = "Error de impresión: ${e.localizedMessage ?: "Error desconocido"}"
                _esErrorAlerta.value = true
            } finally {
                _estaReimprimiendo.value = false
            }
        }
    }

    fun limpiarAlerta() {
        _mensajeAlerta.value = null
    }
}
