package com.typdevstudio.typos_movil.ui.principal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.typdevstudio.typos_movil.datos.local.AppBaseDatos
import com.typdevstudio.typos_movil.datos.repositorio.VentaRepositorio
import com.typdevstudio.typos_movil.ui.metricas.formatearMonto
import com.typdevstudio.typos_movil.ui.theme.AzulPrimarioOscuro
import com.typdevstudio.typos_movil.ui.theme.VerdeExito
import com.typdevstudio.typos_movil.utilidades.reportes.CalculadoraMetricas
import com.typdevstudio.typos_movil.utilidades.reportes.RangoFechas
import com.typdevstudio.typos_movil.utilidades.reportes.sumarDias

/** Mini resumen de las ventas de hoy con las barras de los últimos 7 días. Abre Métricas al tocarlo. */
@Composable
fun TarjetaVentasHoy(alHacerClic: () -> Unit) {
    val contexto = LocalContext.current
    val ultimos7Dias = remember {
        val hoy = System.currentTimeMillis()
        RangoFechas.deDias(sumarDias(hoy, -6), hoy)
    }
    val flujoVentas = remember {
        VentaRepositorio(AppBaseDatos.obtenerBaseDatos(contexto).ventaDao())
            .obtenerVentasEntreFechas(ultimos7Dias.desde, ultimos7Dias.hasta)
    }
    val ventas by flujoVentas.collectAsState(initial = emptyList())
    val inicioHoy = sumarDias(ultimos7Dias.hasta, -1)
    val resumenHoy = remember(ventas) { CalculadoraMetricas.calcularResumen(ventas.filter { it.venta.fecha >= inicioHoy }) }
    val totalesDias = remember(ventas) { CalculadoraMetricas.totalesPorDia(ventas, ultimos7Dias) }

    val colorPrimario = MaterialTheme.colorScheme.primary
    val colorDias = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)

    Card(
        onClick = alHacerClic,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.5.dp, colorPrimario.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = "Ventas de hoy", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colorPrimario)
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = formatearMonto(resumenHoy.total), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = VerdeExito)
                    Text(
                        text = if (resumenHoy.cantidadFacturas == 0) "Aún no hay ventas hoy"
                        else "${resumenHoy.cantidadFacturas} facturas · ticket prom. ${formatearMonto(resumenHoy.ticketPromedio)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val maximo = totalesDias.maxOrNull()?.takeIf { it > 0 } ?: 1.0
                Canvas(
                    modifier = Modifier
                        .width(70.dp)
                        .height(40.dp)
                ) {
                    val anchoColumna = size.width / totalesDias.size.coerceAtLeast(1)
                    val anchoBarra = anchoColumna * 0.7f
                    totalesDias.forEachIndexed { i, total ->
                        val alto = (total / maximo * size.height).toFloat().coerceAtLeast(2f)
                        drawRoundRect(
                            color = if (i == totalesDias.lastIndex) AzulPrimarioOscuro else colorDias,
                            topLeft = Offset(anchoColumna * i, size.height - alto),
                            size = Size(anchoBarra, alto),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}
