package com.typdevstudio.typos_movil.ui.metricas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.typdevstudio.typos_movil.datos.local.AppBaseDatos
import com.typdevstudio.typos_movil.datos.repositorio.VentaRepositorio
import com.typdevstudio.typos_movil.utilidades.reportes.CalculadoraMetricas
import com.typdevstudio.typos_movil.utilidades.reportes.RangoFechas
import com.typdevstudio.typos_movil.utilidades.reportes.ResumenVentas
import com.typdevstudio.typos_movil.utilidades.reportes.inicioDelDia
import com.typdevstudio.typos_movil.utilidades.reportes.sumarDias
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class PeriodoMetricas(val etiqueta: String, val comparacion: String) {
    HOY("Hoy", "vs. día anterior"),
    SEMANA("Semana", "vs. semana anterior"),
    MES("Mes", "vs. mes anterior"),
    RANGO("Rango", "vs. período anterior")
}

data class BarraDia(
    val etiqueta: String,
    val total: Double,
    val totalAnterior: Double,
    val esHoy: Boolean,
    val descripcion: String // Nombre largo del día para el aviso de "mejor día"
)

data class MetricasUi(
    val periodo: PeriodoMetricas = PeriodoMetricas.SEMANA,
    val rango: RangoFechas = RangoFechas.hoy(),
    val tituloRango: String = "",
    val resumen: ResumenVentas = ResumenVentas(),
    val resumenAnterior: ResumenVentas = ResumenVentas(),
    val barras: List<BarraDia> = emptyList(),
    val puedeAvanzar: Boolean = false,
    val cargando: Boolean = true
)

val LOCALE_ES: Locale = Locale("es", "DO")

class MetricasViewModel(application: Application) : AndroidViewModel(application) {

    private val ventaRepositorio = VentaRepositorio(AppBaseDatos.obtenerBaseDatos(application).ventaDao())

    private val _periodo = MutableStateFlow(PeriodoMetricas.SEMANA)
    // 0 = período actual, -1 = el anterior, etc. No aplica al rango personalizado.
    private val _desplazamiento = MutableStateFlow(0)
    private val _rangoPersonalizado = MutableStateFlow(RangoFechas.deDias(sumarDias(System.currentTimeMillis(), -6), System.currentTimeMillis()))

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<MetricasUi> = combine(_periodo, _desplazamiento, _rangoPersonalizado) { periodo, desplazamiento, personalizado ->
        val rango = calcularRango(periodo, desplazamiento, personalizado)
        Triple(periodo, rango, rangoAnterior(periodo, rango))
    }.flatMapLatest { (periodo, rango, anterior) ->
        combine(
            ventaRepositorio.obtenerVentasEntreFechas(rango.desde, rango.hasta),
            ventaRepositorio.obtenerVentasEntreFechas(anterior.desde, anterior.hasta)
        ) { ventas, ventasAnteriores ->
            val totales = CalculadoraMetricas.totalesPorDia(ventas, rango)
            val totalesAnteriores = CalculadoraMetricas.totalesPorDia(ventasAnteriores, anterior)
            val hoy = inicioDelDia(System.currentTimeMillis())
            val barras = totales.mapIndexed { i, total ->
                val dia = sumarDias(rango.desde, i)
                BarraDia(
                    etiqueta = etiquetaBarra(periodo, rango, dia),
                    total = total,
                    totalAnterior = totalesAnteriores.getOrElse(i) { 0.0 },
                    esHoy = dia == hoy,
                    descripcion = SimpleDateFormat("EEEE d 'de' MMMM", LOCALE_ES).format(Date(dia)).replaceFirstChar { it.uppercase() }
                )
            }
            MetricasUi(
                periodo = periodo,
                rango = rango,
                tituloRango = tituloRango(periodo, rango),
                resumen = CalculadoraMetricas.calcularResumen(ventas),
                resumenAnterior = CalculadoraMetricas.calcularResumen(ventasAnteriores),
                barras = barras,
                puedeAvanzar = periodo != PeriodoMetricas.RANGO && rango.hasta <= hoy,
                cargando = false
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MetricasUi())

    fun seleccionarPeriodo(periodo: PeriodoMetricas) {
        _periodo.value = periodo
        _desplazamiento.value = 0
    }

    fun irAnterior() {
        _desplazamiento.value -= 1
    }

    fun irSiguiente() {
        if (_desplazamiento.value < 0) _desplazamiento.value += 1
    }

    fun seleccionarRangoPersonalizado(desdeDia: Long, hastaDia: Long) {
        _rangoPersonalizado.value = RangoFechas.deDias(desdeDia, hastaDia)
        _periodo.value = PeriodoMetricas.RANGO
    }

    private fun calcularRango(periodo: PeriodoMetricas, desplazamiento: Int, personalizado: RangoFechas): RangoFechas {
        val hoy = inicioDelDia(System.currentTimeMillis())
        return when (periodo) {
            PeriodoMetricas.HOY -> {
                val dia = sumarDias(hoy, desplazamiento)
                RangoFechas(dia, sumarDias(dia, 1))
            }
            PeriodoMetricas.SEMANA -> {
                val calendario = Calendar.getInstance().apply { timeInMillis = hoy }
                val retroceso = (calendario.get(Calendar.DAY_OF_WEEK) - calendario.firstDayOfWeek + 7) % 7
                val inicio = sumarDias(hoy, -retroceso + desplazamiento * 7)
                RangoFechas(inicio, sumarDias(inicio, 7))
            }
            PeriodoMetricas.MES -> {
                val calendario = Calendar.getInstance().apply {
                    timeInMillis = hoy
                    set(Calendar.DAY_OF_MONTH, 1)
                    add(Calendar.MONTH, desplazamiento)
                }
                val inicio = calendario.timeInMillis
                calendario.add(Calendar.MONTH, 1)
                RangoFechas(inicio, calendario.timeInMillis)
            }
            PeriodoMetricas.RANGO -> personalizado
        }
    }

    private fun rangoAnterior(periodo: PeriodoMetricas, rango: RangoFechas): RangoFechas = when (periodo) {
        PeriodoMetricas.MES -> {
            val calendario = Calendar.getInstance().apply {
                timeInMillis = rango.desde
                add(Calendar.MONTH, -1)
            }
            RangoFechas(calendario.timeInMillis, rango.desde)
        }
        else -> RangoFechas(sumarDias(rango.desde, -rango.cantidadDias), rango.desde)
    }

    private fun etiquetaBarra(periodo: PeriodoMetricas, rango: RangoFechas, dia: Long): String =
        if (periodo == PeriodoMetricas.SEMANA || (periodo == PeriodoMetricas.RANGO && rango.cantidadDias <= 7)) {
            SimpleDateFormat("EEE", LOCALE_ES).format(Date(dia)).replace(".", "").replaceFirstChar { it.uppercase() }
        } else {
            SimpleDateFormat("d", LOCALE_ES).format(Date(dia))
        }

    private fun tituloRango(periodo: PeriodoMetricas, rango: RangoFechas): String {
        val ultimoDia = sumarDias(rango.hasta, -1)
        return when (periodo) {
            PeriodoMetricas.HOY -> SimpleDateFormat("EEEE d MMM yyyy", LOCALE_ES).format(Date(rango.desde)).replaceFirstChar { it.uppercase() }
            PeriodoMetricas.MES -> SimpleDateFormat("MMMM yyyy", LOCALE_ES).format(Date(rango.desde)).replaceFirstChar { it.uppercase() }
            else -> {
                val formato = SimpleDateFormat("d MMM", LOCALE_ES)
                "${formato.format(Date(rango.desde))} – ${formato.format(Date(ultimoDia))} ${SimpleDateFormat("yyyy", LOCALE_ES).format(Date(ultimoDia))}"
            }
        }
    }
}
