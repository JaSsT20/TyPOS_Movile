package com.typdevstudio.typos_movil.ui.metricas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.typdevstudio.typos_movil.ui.theme.AmarilloAdvertencia
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.AzulPrimarioOscuro
import com.typdevstudio.typos_movil.ui.theme.GrisMedio
import com.typdevstudio.typos_movil.ui.theme.RojoError
import com.typdevstudio.typos_movil.ui.theme.VerdeExito
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil

fun formatearMonto(monto: Double): String = "$" + String.format(Locale.US, "%,.2f", monto)

/** Monto corto para las etiquetas de las barras: 950, 12.4k, 1.2M. */
fun formatearMontoCorto(monto: Double): String = when {
    monto >= 1_000_000 -> String.format(Locale.US, "%.1fM", monto / 1_000_000)
    monto >= 1_000 -> String.format(Locale.US, "%.1fk", monto / 1_000)
    else -> String.format(Locale.US, "%.0f", monto)
}

fun colorMetodoPago(metodo: String): Color = when (metodo) {
    "Efectivo" -> AzulPrimario
    "Transferencia" -> AmarilloAdvertencia
    "Cheque" -> VerdeExito
    else -> GrisMedio
}

/** Los DatePicker de Material devuelven la medianoche UTC del día elegido; se pasa a la medianoche local. */
fun fechaPickerALocal(milisUtc: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = milisUtc }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

fun fechaLocalAPicker(milisLocal: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = milisLocal }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

@Composable
fun TarjetaSeccionMetricas(
    titulo: String,
    subtitulo: String,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    accion: (@Composable () -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icono != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(AzulPrimario.copy(alpha = 0.14f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icono, contentDescription = null, tint = AzulPrimario, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = titulo, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(text = subtitulo, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                accion?.invoke()
            }
            contenido()
        }
    }
}

@Composable
fun TarjetaKpi(
    titulo: String,
    valor: String,
    colorValor: Color,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    variacion: Double? = null, // Porcentaje contra el período anterior; null si no hay con qué comparar
    textoComparacion: String = ""
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = titulo, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(text = valor, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = colorValor, maxLines = 1)
            if (variacion != null) {
                val sube = variacion >= 0
                val color = if (sube) VerdeExito else RojoError
                Text(
                    text = "${if (sube) "▲" else "▼"} ${String.format(Locale.US, "%.1f", kotlin.math.abs(variacion))}% $textoComparacion",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    maxLines = 1,
                    modifier = Modifier
                        .background(color.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            } else if (subtitulo != null) {
                Text(text = subtitulo, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
fun LeyendaColor(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = texto, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Barras por día: la del período actual al lado de la del período anterior. */
@Composable
fun GraficoBarrasDias(
    barras: List<BarraDia>,
    modifier: Modifier = Modifier,
    altoGrafico: Dp = 170.dp
) {
    if (barras.isEmpty()) return
    val colorActual = MaterialTheme.colorScheme.primary
    val colorHoy = AzulPrimarioOscuro
    val colorAnterior = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    val colorGuia = MaterialTheme.colorScheme.outlineVariant
    val colorBase = MaterialTheme.colorScheme.outline
    val maximo = barras.maxOf { maxOf(it.total, it.totalAnterior) }.takeIf { it > 0 } ?: 1.0
    val pocasBarras = barras.size <= 7
    // Con muchos días solo se rotulan algunos para que no se encimen
    val pasoEtiqueta = if (pocasBarras) 1 else ceil(barras.size / 8.0).toInt()
    val descripcion = barras.joinToString { "${it.etiqueta}: ${formatearMonto(it.total)}" }

    Column(modifier = modifier.fillMaxWidth()) {
        if (pocasBarras) {
            Row(modifier = Modifier.fillMaxWidth()) {
                barras.forEach {
                    Text(
                        text = if (it.total > 0) formatearMontoCorto(it.total) else "",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(altoGrafico)
                .semantics { contentDescription = descripcion }
        ) {
            val guia = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            listOf(0.25f, 0.5f, 0.75f).forEach { f ->
                drawLine(colorGuia, Offset(0f, size.height * f), Offset(size.width, size.height * f), strokeWidth = 1f, pathEffect = guia)
            }
            val anchoColumna = size.width / barras.size
            val anchoBarra = (anchoColumna * if (pocasBarras) 0.3f else 0.36f).coerceAtMost(18.dp.toPx())
            val radio = CornerRadius(minOf(anchoBarra / 3f, 4.dp.toPx()))
            barras.forEachIndexed { i, barra ->
                val centro = anchoColumna * i + anchoColumna / 2
                val altoAnterior = (barra.totalAnterior / maximo * size.height).toFloat()
                val altoActual = (barra.total / maximo * size.height).toFloat()
                drawRoundRect(colorAnterior, Offset(centro - anchoBarra - 1.5f, size.height - altoAnterior), Size(anchoBarra, altoAnterior), radio)
                drawRoundRect(if (barra.esHoy) colorHoy else colorActual, Offset(centro + 1.5f, size.height - altoActual), Size(anchoBarra, altoActual), radio)
            }
            drawLine(colorBase, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            barras.forEachIndexed { i, barra ->
                Text(
                    text = when {
                        barra.esHoy && pocasBarras -> "Hoy"
                        i % pasoEtiqueta == 0 -> barra.etiqueta
                        else -> ""
                    },
                    fontSize = if (pocasBarras) 11.sp else 9.sp,
                    fontWeight = if (barra.esHoy) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

data class SegmentoDona(val etiqueta: String, val valor: Double, val color: Color)

@Composable
fun GraficoDona(
    segmentos: List<SegmentoDona>,
    textoCentro: String,
    subtextoCentro: String,
    modifier: Modifier = Modifier,
    tamano: Dp = 120.dp
) {
    val total = segmentos.sumOf { it.valor }.takeIf { it > 0 } ?: 1.0
    val colorVacio = MaterialTheme.colorScheme.surfaceVariant
    val descripcion = segmentos.joinToString { "${it.etiqueta} ${String.format(Locale.US, "%.0f", it.valor / total * 100)}%" }
    Box(modifier = modifier.size(tamano), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .size(tamano)
                .semantics { contentDescription = descripcion }
        ) {
            val grosor = 16.dp.toPx()
            val area = Size(size.width - grosor, size.height - grosor)
            val origen = Offset(grosor / 2, grosor / 2)
            if (segmentos.isEmpty()) {
                drawArc(colorVacio, 0f, 360f, false, origen, area, style = Stroke(grosor))
            }
            var inicio = -90f
            segmentos.forEach { segmento ->
                val barrido = (segmento.valor / total * 360f).toFloat()
                drawArc(segmento.color, inicio, barrido, false, origen, area, style = Stroke(grosor))
                inicio += barrido
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = textoCentro, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtextoCentro, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Facturas por hora. Se muestran las horas con ventas, como mínimo de 8 am a 8 pm. */
@Composable
fun GraficoHoras(facturasPorHora: List<Int>, modifier: Modifier = Modifier) {
    val horasConVentas = facturasPorHora.indices.filter { facturasPorHora[it] > 0 }
    val desde = minOf(horasConVentas.minOrNull() ?: 8, 8)
    val hasta = maxOf(horasConVentas.maxOrNull() ?: 20, 20)
    val horas = (desde..hasta).toList()
    val maximo = horas.maxOf { facturasPorHora[it] }.coerceAtLeast(1)
    // Las horas por encima del 75% del máximo se resaltan como horas pico
    val umbralPico = maximo * 0.75
    val colorPico = MaterialTheme.colorScheme.primary
    val colorNormal = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    val descripcion = horas.filter { facturasPorHora[it] > 0 }.joinToString { "${etiquetaHora(it)}: ${facturasPorHora[it]} facturas" }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .semantics { contentDescription = descripcion }
        ) {
            val anchoColumna = size.width / horas.size
            val anchoBarra = anchoColumna * 0.7f
            horas.forEachIndexed { i, hora ->
                val valor = facturasPorHora[hora]
                val alto = (valor.toFloat() / maximo * size.height).coerceAtLeast(if (valor > 0) 3f else 0f)
                drawRoundRect(
                    color = if (valor >= umbralPico && valor > 0) colorPico else colorNormal,
                    topLeft = Offset(anchoColumna * i + (anchoColumna - anchoBarra) / 2, size.height - alto),
                    size = Size(anchoBarra, alto),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val paso = ((horas.size - 1) / 4).coerceAtLeast(1)
            horas.filterIndexed { i, _ -> i % paso == 0 }.forEach {
                Text(text = etiquetaHora(it), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

fun etiquetaHora(hora: Int): String = when {
    hora == 0 -> "12am"
    hora < 12 -> "${hora}am"
    hora == 12 -> "12pm"
    else -> "${hora - 12}pm"
}
