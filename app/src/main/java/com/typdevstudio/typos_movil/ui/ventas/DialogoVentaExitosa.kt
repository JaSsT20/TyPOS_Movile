package com.typdevstudio.typos_movil.ui.ventas

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.typdevstudio.typos_movil.datos.local.entidades.VentaEntidad
import com.typdevstudio.typos_movil.ui.componentes.BotonPos
import com.typdevstudio.typos_movil.ui.componentes.VarianteBoton
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.Blanco
import com.typdevstudio.typos_movil.ui.theme.VerdeExito
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DialogoVentaExitosa(
    venta: VentaEntidad?,
    alCerrar: () -> Unit,
    alImprimir: (VentaEntidad) -> Unit
) {
    if (venta == null) return

    Dialog(onDismissRequest = alCerrar) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Checkmark animado con trazo circular, salto/rebote elástico y dibujo de check
                CheckmarkAnimadoExito(modifier = Modifier.size(76.dp))

                Text(
                    text = "¡Venta Completada!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Factura: ${venta.numeroFactura}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AzulPrimario
                )

                // Resumen del cobro
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$${String.format("%.2f", venta.total)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Método de Pago:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(venta.metodoPago, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        if (venta.montoDevuelto > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Devuelta:", fontSize = 14.sp, color = VerdeExito, fontWeight = FontWeight.Bold)
                                Text("$${String.format("%.2f", venta.montoDevuelto)}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = VerdeExito)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Botones de acción
                BotonPos(
                    texto = "Imprimir Ticket",
                    alHacerClic = { alImprimir(venta) },
                    icono = Icons.Filled.Print,
                    variante = VarianteBoton.PRIMARIO
                )

                BotonPos(
                    texto = "Nueva Venta",
                    alHacerClic = alCerrar,
                    icono = Icons.Filled.ShoppingBag,
                    variante = VarianteBoton.SECUNDARIO
                )
            }
        }
    }
}

/**
 * Animación fluida y elástica estilo "Verified Checkmark":
 * 1. Dibuja el círculo exterior cargando (Loading ring).
 * 2. Hace un salto con rebote elástico (Spring Bounce / Pop) expandiendo el fondo verde.
 * 3. Dibuja dinámicamente el Check blanco con bordes redondeados.
 * 4. Expande una onda de choque sutil (Pulse ripple).
 */
@Composable
fun CheckmarkAnimadoExito(
    modifier: Modifier = Modifier,
    colorFondo: Color = VerdeExito,
    colorCheck: Color = Blanco
) {
    val progresoCirculo = remember { Animatable(0f) }
    val escalaRebote = remember { Animatable(0.2f) }
    val progresoCheck = remember { Animatable(0f) }
    val escalaOnda = remember { Animatable(0.8f) }
    val alfaOnda = remember { Animatable(0.6f) }

    LaunchedEffect(Unit) {
        // 1. Carga del anillo circular (0 a 360 grados)
        progresoCirculo.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
        )

        // 2. Salto y rebote elástico (Spring Pop)
        launch {
            escalaRebote.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        // Onda expansiva de celebración
        launch {
            escalaOnda.animateTo(1.35f, animationSpec = tween(380, easing = FastOutSlowInEasing))
        }
        launch {
            alfaOnda.animateTo(0f, animationSpec = tween(380, easing = FastOutSlowInEasing))
        }

        delay(60)

        // 3. Dibujo fluido del trazo del checkmark
        progresoCheck.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(76.dp)
                .scale(escalaRebote.value)
        ) {
            val centro = Offset(size.width / 2f, size.height / 2f)
            val radio = (size.minDimension / 2f) - 6.dp.toPx()

            // Onda de pulso externa
            if (alfaOnda.value > 0f) {
                drawCircle(
                    color = colorFondo.copy(alpha = alfaOnda.value * 0.35f),
                    radius = radio * escalaOnda.value,
                    center = centro,
                    style = Fill
                )
            }

            // Anillo exterior de carga
            if (progresoCirculo.value < 1f) {
                drawArc(
                    color = colorFondo,
                    startAngle = -90f,
                    sweepAngle = 360f * progresoCirculo.value,
                    useCenter = false,
                    topLeft = Offset(centro.x - radio, centro.y - radio),
                    size = androidx.compose.ui.geometry.Size(radio * 2f, radio * 2f),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            } else {
                // Círculo relleno con rebote
                drawCircle(
                    color = colorFondo,
                    radius = radio,
                    center = centro,
                    style = Fill
                )
            }

            // Trazo del Checkmark blanco
            if (progresoCheck.value > 0f) {
                val pathCompleto = Path().apply {
                    val startX = size.width * 0.30f
                    val startY = size.height * 0.52f
                    val midX = size.width * 0.45f
                    val midY = size.height * 0.67f
                    val endX = size.width * 0.72f
                    val endY = size.height * 0.36f

                    moveTo(startX, startY)
                    lineTo(midX, midY)
                    lineTo(endX, endY)
                }

                val pathMeasure = PathMeasure()
                pathMeasure.setPath(pathCompleto, false)
                val longitudTotal = pathMeasure.length

                val pathSegmento = Path()
                pathMeasure.getSegment(
                    startDistance = 0f,
                    stopDistance = longitudTotal * progresoCheck.value,
                    destination = pathSegmento,
                    startWithMoveTo = true
                )

                drawPath(
                    path = pathSegmento,
                    color = colorCheck,
                    style = Stroke(
                        width = 4.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}
