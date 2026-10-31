package com.typdevstudio.typos_movil.ui.metricas

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.typdevstudio.typos_movil.datos.impresion.DatosReporteVentas
import com.typdevstudio.typos_movil.datos.impresion.OpcionesReporteVentas
import com.typdevstudio.typos_movil.datos.impresion.ResultadoImpresion
import com.typdevstudio.typos_movil.datos.impresion.ServicioImpresoraBluetooth
import com.typdevstudio.typos_movil.datos.local.AppBaseDatos
import com.typdevstudio.typos_movil.datos.local.GestorSesion
import com.typdevstudio.typos_movil.datos.local.relaciones.VentaConDetalles
import com.typdevstudio.typos_movil.datos.repositorio.ConfiguracionRepositorio
import com.typdevstudio.typos_movil.datos.repositorio.VentaRepositorio
import com.typdevstudio.typos_movil.ui.ventas.EstadoImpresionUi
import com.typdevstudio.typos_movil.utilidades.reportes.CalculadoraMetricas
import com.typdevstudio.typos_movil.utilidades.reportes.RangoFechas
import com.typdevstudio.typos_movil.utilidades.reportes.ResumenVentas
import com.typdevstudio.typos_movil.utilidades.reportes.inicioDelDia
import com.typdevstudio.typos_movil.utilidades.reportes.sumarDias
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

enum class AtajoFechaReporte(val etiqueta: String) {
    HOY("Hoy"),
    AYER("Ayer"),
    ESTA_SEMANA("Esta semana"),
    ESTE_MES("Este mes"),
    PERSONALIZADO("Personalizado")
}

data class ReporteVentasUi(
    val atajo: AtajoFechaReporte = AtajoFechaReporte.HOY,
    val rango: RangoFechas = RangoFechas.hoy(),
    val cajero: String? = null,
    val cajerosDisponibles: List<String> = emptyList(),
    val resumen: ResumenVentas = ResumenVentas(),
    val ventas: List<VentaConDetalles> = emptyList()
)

class ReporteVentasViewModel(application: Application) : AndroidViewModel(application) {

    private val ventaRepositorio: VentaRepositorio
    private val configuracionRepositorio: ConfiguracionRepositorio
    private val preferencias = application.getSharedPreferences(PREFS_REPORTE, Context.MODE_PRIVATE)

    init {
        val bd = AppBaseDatos.obtenerBaseDatos(application)
        ventaRepositorio = VentaRepositorio(bd.ventaDao())
        configuracionRepositorio = ConfiguracionRepositorio(bd.configuracionNegocioDao())
    }

    // El ViewModel vive con la pantalla, así que cada vez que se abre arranca en el día de hoy
    private val _atajo = MutableStateFlow(AtajoFechaReporte.HOY)
    private val _rango = MutableStateFlow(RangoFechas.hoy())
    private val _cajero = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<ReporteVentasUi> = combine(_atajo, _rango, _cajero) { atajo, rango, cajero ->
        Triple(atajo, rango, cajero)
    }.flatMapLatest { (atajo, rango, cajero) ->
        ventaRepositorio.obtenerVentasEntreFechas(rango.desde, rango.hasta).map { ventas ->
            val delCajero = if (cajero == null) ventas else ventas.filter { it.venta.usuario == cajero }
            ReporteVentasUi(
                atajo = atajo,
                rango = rango,
                cajero = cajero,
                cajerosDisponibles = ventas.map { it.venta.usuario }.filter { it.isNotBlank() }.distinct().sorted(),
                resumen = CalculadoraMetricas.calcularResumen(delCajero),
                ventas = delCajero
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReporteVentasUi())

    // Para mostrar a qué impresora se enviará el reporte
    val nombreImpresora: StateFlow<String?> = configuracionRepositorio.obtenerConfiguracion()
        .map { config -> config?.nombreImpresora?.takeIf { !config.direccionMacImpresora.isNullOrBlank() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _opciones = MutableStateFlow(leerOpciones())
    val opciones: StateFlow<OpcionesReporteVentas> = _opciones.asStateFlow()

    private val _estadoImpresion = MutableStateFlow<EstadoImpresionUi?>(null)
    val estadoImpresion: StateFlow<EstadoImpresionUi?> = _estadoImpresion.asStateFlow()

    fun seleccionarAtajo(atajo: AtajoFechaReporte) {
        val hoy = inicioDelDia(System.currentTimeMillis())
        val rango = when (atajo) {
            AtajoFechaReporte.HOY -> RangoFechas.deDias(hoy, hoy)
            AtajoFechaReporte.AYER -> sumarDias(hoy, -1).let { RangoFechas.deDias(it, it) }
            AtajoFechaReporte.ESTA_SEMANA -> {
                val calendario = Calendar.getInstance().apply { timeInMillis = hoy }
                val retroceso = (calendario.get(Calendar.DAY_OF_WEEK) - calendario.firstDayOfWeek + 7) % 7
                RangoFechas.deDias(sumarDias(hoy, -retroceso), hoy)
            }
            AtajoFechaReporte.ESTE_MES -> {
                val inicioMes = Calendar.getInstance().apply {
                    timeInMillis = hoy
                    set(Calendar.DAY_OF_MONTH, 1)
                }.timeInMillis
                RangoFechas.deDias(inicioMes, hoy)
            }
            AtajoFechaReporte.PERSONALIZADO -> _rango.value
        }
        _rango.value = rango
        _atajo.value = atajo
    }

    fun cambiarDesde(dia: Long) {
        val hasta = sumarDias(_rango.value.hasta, -1)
        _rango.value = RangoFechas.deDias(dia, maxOf(dia, hasta))
        _atajo.value = AtajoFechaReporte.PERSONALIZADO
    }

    fun cambiarHasta(dia: Long) {
        _rango.value = RangoFechas.deDias(minOf(_rango.value.desde, dia), dia)
        _atajo.value = AtajoFechaReporte.PERSONALIZADO
    }

    fun seleccionarCajero(cajero: String?) {
        _cajero.value = cajero
    }

    fun cambiarOpciones(opciones: OpcionesReporteVentas) {
        _opciones.value = opciones
        preferencias.edit()
            .putBoolean(CLAVE_METODOS, opciones.metodosPago)
            .putBoolean(CLAVE_CAJEROS, opciones.cajeros)
            .putBoolean(CLAVE_PRODUCTOS, opciones.productos)
            .putBoolean(CLAVE_ANULADAS, opciones.anuladas)
            .putBoolean(CLAVE_DETALLE, opciones.detalleFacturas)
            .apply()
    }

    private fun leerOpciones() = OpcionesReporteVentas(
        metodosPago = preferencias.getBoolean(CLAVE_METODOS, true),
        cajeros = preferencias.getBoolean(CLAVE_CAJEROS, true),
        productos = preferencias.getBoolean(CLAVE_PRODUCTOS, true),
        anuladas = preferencias.getBoolean(CLAVE_ANULADAS, true),
        detalleFacturas = preferencias.getBoolean(CLAVE_DETALLE, false)
    )

    fun imprimirReporte() {
        // Evita abrir dos conexiones si se pulsa "Imprimir" otra vez mientras imprime.
        if (_estadoImpresion.value?.terminado == false) return
        _estadoImpresion.value = EstadoImpresionUi(nombreImpresora = null, direccionMac = null)

        viewModelScope.launch {
            val config = configuracionRepositorio.obtenerConfiguracionDirecta()
            val mac = config.direccionMacImpresora

            if (mac.isNullOrBlank()) {
                _estadoImpresion.value = EstadoImpresionUi(
                    nombreImpresora = null,
                    direccionMac = null,
                    terminado = true,
                    mensajeError = "No hay ninguna impresora guardada. Vaya a Configuración > Impresora, " +
                        "seleccione su impresora y pulse Guardar."
                )
                return@launch
            }

            _estadoImpresion.value = EstadoImpresionUi(nombreImpresora = config.nombreImpresora, direccionMac = mac)

            // Se consulta de nuevo para imprimir lo que hay en este momento, aunque la pantalla no se haya refrescado
            val actual = estado.value
            val ventas = ventaRepositorio.obtenerVentasEntreFechasDirecto(actual.rango.desde, actual.rango.hasta)
                .let { lista -> if (actual.cajero == null) lista else lista.filter { it.venta.usuario == actual.cajero } }

            val datos = DatosReporteVentas(
                desde = actual.rango.desde,
                hasta = actual.rango.hasta,
                esUnSoloDia = actual.rango.cantidadDias == 1,
                cajero = actual.cajero,
                impresoPor = GestorSesion.obtenerNombreUsuario(),
                resumen = CalculadoraMetricas.calcularResumen(ventas),
                ventas = ventas.map { it.venta },
                opciones = _opciones.value
            )

            val resultado = ServicioImpresoraBluetooth.imprimirReporteVentas(
                direccionMac = mac,
                configuracion = config,
                datos = datos,
                alCambiarEtapa = { etapa -> _estadoImpresion.update { it?.copy(etapa = etapa) } }
            )

            _estadoImpresion.update {
                it?.copy(
                    terminado = true,
                    mensajeError = (resultado as? ResultadoImpresion.Error)?.mensaje
                )
            }
        }
    }

    fun cerrarEstadoImpresion() {
        if (_estadoImpresion.value?.terminado == true) _estadoImpresion.value = null
    }

    private companion object {
        const val PREFS_REPORTE = "reporte_ventas"
        const val CLAVE_METODOS = "incluir_metodos_pago"
        const val CLAVE_CAJEROS = "incluir_cajeros"
        const val CLAVE_PRODUCTOS = "incluir_productos"
        const val CLAVE_ANULADAS = "incluir_anuladas"
        const val CLAVE_DETALLE = "incluir_detalle_facturas"
    }
}
