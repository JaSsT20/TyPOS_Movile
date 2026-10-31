package com.typdevstudio.typos_movil.ui.metricas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.Blanco
import com.typdevstudio.typos_movil.ui.theme.GrisMedio
import com.typdevstudio.typos_movil.ui.theme.VerdeExito
import com.typdevstudio.typos_movil.utilidades.reportes.sumarDias
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMetricas(
    viewModel: MetricasViewModel,
    alVolver: () -> Unit,
    alAbrirReporte: () -> Unit
) {
    val estado by viewModel.estado.collectAsState()
    var mostrarSelectorRango by remember { mutableStateOf(false) }
    val resumen = estado.resumen
    val anterior = estado.resumenAnterior

    if (mostrarSelectorRango) {
        val estadoSelector = rememberDateRangePickerState(
            initialSelectedStartDateMillis = fechaLocalAPicker(estado.rango.desde),
            initialSelectedEndDateMillis = fechaLocalAPicker(sumarDias(estado.rango.hasta, -1))
        )
        DatePickerDialog(
            onDismissRequest = { mostrarSelectorRango = false },
            confirmButton = {
                TextButton(
                    enabled = estadoSelector.selectedStartDateMillis != null,
                    onClick = {
                        val desde = estadoSelector.selectedStartDateMillis
                        if (desde != null) {
                            val hasta = estadoSelector.selectedEndDateMillis ?: desde
                            viewModel.seleccionarRangoPersonalizado(fechaPickerALocal(desde), fechaPickerALocal(hasta))
                        }
                        mostrarSelectorRango = false
                    }
                ) { Text("Aplicar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarSelectorRango = false }) { Text("Cancelar") }
            }
        ) {
            DateRangePicker(
                state = estadoSelector,
                title = { Text("Elija el rango de fechas", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Métricas de Ventas", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Blanco)
                        Text(text = estado.tituloRango, fontSize = 12.sp, color = Blanco.copy(alpha = 0.9f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = alVolver) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Blanco)
                    }
                },
                actions = {
                    IconButton(onClick = alAbrirReporte) {
                        Icon(imageVector = Icons.Filled.Print, contentDescription = "Imprimir reporte de ventas", tint = Blanco)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AzulPrimario)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValores ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValores)
                .verticalScroll(rememberScrollState())
        ) {
            // Selector de período
            Card(
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PeriodoMetricas.entries.forEach { periodo ->
                            val seleccionado = periodo == estado.periodo
                            FilterChip(
                                selected = seleccionado,
                                onClick = {
                                    if (periodo == PeriodoMetricas.RANGO) mostrarSelectorRango = true
                                    else viewModel.seleccionarPeriodo(periodo)
                                },
                                label = { Text(periodo.etiqueta, fontSize = 12.sp, fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = if (periodo == PeriodoMetricas.RANGO) {
                                    { Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (estado.periodo != PeriodoMetricas.RANGO) {
                            IconButton(onClick = { viewModel.irAnterior() }) {
                                Icon(Icons.Filled.ChevronLeft, contentDescription = "Período anterior", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(
                            text = estado.tituloRango,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        if (estado.periodo == PeriodoMetricas.RANGO) {
                            TextButton(onClick = { mostrarSelectorRango = true }) { Text("Cambiar", fontSize = 12.sp) }
                        } else {
                            IconButton(onClick = { viewModel.irSiguiente() }, enabled = estado.puedeAvanzar) {
                                Icon(
                                    Icons.Filled.ChevronRight,
                                    contentDescription = "Período siguiente",
                                    tint = if (estado.puedeAvanzar) MaterialTheme.colorScheme.onSurfaceVariant else GrisMedio.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            if (estado.cargando) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val comparacion = estado.periodo.comparacion
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TarjetaKpi(
                        titulo = "Total vendido",
                        valor = formatearMonto(resumen.total),
                        colorValor = VerdeExito,
                        variacion = variacionPorcentual(resumen.total, anterior.total),
                        textoComparacion = comparacion,
                        subtitulo = "Sin datos para comparar",
                        modifier = Modifier.weight(1f)
                    )
                    TarjetaKpi(
                        titulo = "Facturas",
                        valor = "${resumen.cantidadFacturas}",
                        colorValor = MaterialTheme.colorScheme.primary,
                        subtitulo = if (resumen.cantidadAnuladas == 1) "1 anulada" else "${resumen.cantidadAnuladas} anuladas",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TarjetaKpi(
                        titulo = "Ticket promedio",
                        valor = formatearMonto(resumen.ticketPromedio),
                        colorValor = MaterialTheme.colorScheme.onSurface,
                        variacion = variacionPorcentual(resumen.ticketPromedio, anterior.ticketPromedio),
                        textoComparacion = comparacion,
                        subtitulo = "Por factura",
                        modifier = Modifier.weight(1f)
                    )
                    TarjetaKpi(
                        titulo = "ITBIS cobrado",
                        valor = formatearMonto(resumen.itbis),
                        colorValor = MaterialTheme.colorScheme.onSurface,
                        subtitulo = "Impuesto 18%",
                        modifier = Modifier.weight(1f)
                    )
                }

                if (!estado.cargando && resumen.cantidadFacturas == 0) {
                    EstadoSinVentas()
                } else {
                    if (estado.periodo != PeriodoMetricas.HOY) {
                        SeccionVentasPorDia(estado)
                    }
                    SeccionMetodosPago(estado)
                    SeccionHoras(estado)
                    SeccionTopProductos(estado)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

private fun variacionPorcentual(actual: Double, anterior: Double): Double? =
    if (anterior > 0) (actual - anterior) / anterior * 100 else null

@Composable
private fun EstadoSinVentas() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = Icons.Filled.Insights, contentDescription = null, tint = GrisMedio, modifier = Modifier.size(56.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "No hay ventas en este período", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = "Elija otro período para ver sus gráficos",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SeccionVentasPorDia(estado: MetricasUi) {
    TarjetaSeccionMetricas(
        titulo = "Ventas por día",
        subtitulo = "Monto facturado (sin anuladas)",
        icono = Icons.Filled.BarChart
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LeyendaColor(MaterialTheme.colorScheme.primary, "Este período")
            LeyendaColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.28f), "Período anterior")
        }
        GraficoBarrasDias(barras = estado.barras)

        val mejorDia = estado.barras.maxByOrNull { it.total }
        if (mejorDia != null && mejorDia.total > 0 && estado.barras.size > 1) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AzulPrimario.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                    .border(1.dp, AzulPrimario.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = AzulPrimario, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Mejor día: ${mejorDia.descripcion}, ${formatearMonto(mejorDia.total)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun SeccionMetodosPago(estado: MetricasUi) {
    val metodos = estado.resumen.metodosPago
    val totalPagado = metodos.sumOf { it.monto }.takeIf { it > 0 } ?: 1.0
    TarjetaSeccionMetricas(titulo = "Métodos de pago", subtitulo = "Cómo le pagaron sus clientes") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GraficoDona(
                segmentos = metodos.map { SegmentoDona(it.metodo, it.monto, colorMetodoPago(it.metodo)) },
                textoCentro = "${estado.resumen.cantidadFacturas}",
                subtextoCentro = "facturas"
            )
            Spacer(modifier = Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                metodos.forEach { metodo ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(colorMetodoPago(metodo.metodo), RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = metodo.metodo, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = formatearMonto(metodo.monto), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = String.format(Locale.US, "%.0f%%", metodo.monto / totalPagado * 100),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeccionHoras(estado: MetricasUi) {
    val porHora = estado.resumen.facturasPorHora
    val horaPico = porHora.indices.maxByOrNull { porHora[it] }
    TarjetaSeccionMetricas(titulo = "Horas de más venta", subtitulo = "Facturas por hora del día") {
        GraficoHoras(facturasPorHora = porHora)
        if (horaPico != null && porHora[horaPico] > 0) {
            Text(
                text = "Hora con más clientes: ${etiquetaHora(horaPico)} a ${etiquetaHora((horaPico + 1) % 24)} (${porHora[horaPico]} facturas)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SeccionTopProductos(estado: MetricasUi) {
    val top = estado.resumen.productos.take(5)
    if (top.isEmpty()) return
    val maximo = top.first().monto.takeIf { it > 0 } ?: 1.0
    TarjetaSeccionMetricas(
        titulo = "Productos más vendidos",
        subtitulo = "Por monto vendido",
        accion = {
            Text(
                text = "Top ${top.size}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    ) {
        top.forEachIndexed { i, producto ->
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${i + 1}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(20.dp)
                    )
                    Text(
                        text = producto.nombre,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = formatearMonto(producto.monto), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 20.dp)) {
                    LinearProgressIndicator(
                        progress = { (producto.monto / maximo).toFloat() },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                        drawStopIndicator = {}
                    )
                    Text(
                        text = "${formatearCantidad(producto.cantidad)} ${if (producto.esServicio) "serv." else "und."}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(72.dp)
                    )
                }
            }
        }
    }
}

fun formatearCantidad(cantidad: Double): String =
    if (cantidad % 1.0 == 0.0) cantidad.toInt().toString() else String.format(Locale.US, "%.2f", cantidad)
