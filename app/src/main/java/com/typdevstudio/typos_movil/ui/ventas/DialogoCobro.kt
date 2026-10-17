package com.typdevstudio.typos_movil.ui.ventas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import java.util.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.typdevstudio.typos_movil.ui.componentes.BotonPos
import com.typdevstudio.typos_movil.ui.componentes.CampoTextoPos
import com.typdevstudio.typos_movil.ui.componentes.VarianteBoton
import com.typdevstudio.typos_movil.ui.theme.AmarilloAdvertencia
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.Blanco
import com.typdevstudio.typos_movil.ui.theme.GrisSecundario
import com.typdevstudio.typos_movil.ui.theme.RojoError
import com.typdevstudio.typos_movil.ui.theme.VerdeExito

@Composable
fun DialogoCobro(
    mostrar: Boolean,
    viewModel: VentasViewModel,
    alConfirmarExito: () -> Unit,
    alDescartar: () -> Unit
) {
    if (!mostrar) return

    val cobroState by viewModel.cobroState.collectAsState()
    val carrito by viewModel.carrito.collectAsState()

    val total by remember(carrito) { derivedStateOf { carrito.sumOf { it.total } } }
    val subtotal by remember(carrito) { derivedStateOf { carrito.sumOf { it.subTotalNeto } } }
    val itbis by remember(carrito) { derivedStateOf { carrito.sumOf { it.montoItbis } } }

    val metodosPago = listOf("Efectivo", "Transferencia", "Cheque", "Mixto")

    // Cálculo dinámico de cambio o faltante para efectivo
    val montoRecibidoDouble = cobroState.montoRecibido.toDoubleOrNull() ?: 0.0
    val cambio by remember(montoRecibidoDouble, total) {
        derivedStateOf { (montoRecibidoDouble - total).coerceAtLeast(0.0) }
    }
    val faltanteEfectivo by remember(montoRecibidoDouble, total) {
        derivedStateOf { (total - montoRecibidoDouble).coerceAtLeast(0.0) }
    }

    var mostrarConfirmacionMontoMenor by remember { mutableStateOf(false) }

    // Diálogo de Confirmación cuando el monto recibido es menor al total
    if (mostrarConfirmacionMontoMenor) {
        val montoRecibidoFinal = if (cobroState.metodoPago == "Efectivo") {
            cobroState.montoRecibido.toDoubleOrNull() ?: 0.0
        } else {
            val ef = cobroState.montoEfectivo.toDoubleOrNull() ?: 0.0
            val tr = cobroState.montoTransferencia.toDoubleOrNull() ?: 0.0
            val ch = cobroState.montoCheque.toDoubleOrNull() ?: 0.0
            ef + tr + ch
        }
        val diferenciaFaltante = (total - montoRecibidoFinal).coerceAtLeast(0.0)

        AlertDialog(
            onDismissRequest = { mostrarConfirmacionMontoMenor = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(AmarilloAdvertencia.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.WarningAmber,
                        contentDescription = "Advertencia",
                        tint = AmarilloAdvertencia,
                        modifier = Modifier.size(32.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Monto Recibido Menor al Total",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "El monto que está cobrando ($${String.format("%.2f", montoRecibidoFinal)}) es menor que el total de la venta ($${String.format("%.2f", total)}).",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AmarilloAdvertencia.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmarilloAdvertencia.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Faltante / Diferencia:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "-$${String.format("%.2f", diferenciaFaltante)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AmarilloAdvertencia
                            )
                        }
                    }

                    Text(
                        text = "¿Desea confirmar y procesar la venta con este monto de todas formas?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                BotonPos(
                    texto = "Sí, Confirmar Venta",
                    alHacerClic = {
                        mostrarConfirmacionMontoMenor = false
                        viewModel.procesarCobro(confirmarMenorMonto = true, alTerminarExito = alConfirmarExito)
                    },
                    variante = VarianteBoton.EXITO
                )
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacionMontoMenor = false }) {
                    Text("Ajustar Monto", color = GrisSecundario, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    val estadoScroll = rememberScrollState()
    val ambitoCorrutina = rememberCoroutineScope()

    Dialog(
        onDismissRequest = alDescartar,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(estadoScroll),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                // Cabecera del Diálogo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cobrar Venta",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AzulPrimario
                    )
                    IconButton(onClick = alDescartar) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Cerrar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Resumen Financiero con ITBIS
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal Neto:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("$${String.format("%.2f", subtotal)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ITBIS (18%):", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("$${String.format("%.2f", itbis)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TOTAL A PAGAR:", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AzulPrimario)
                            Text("$${String.format("%.2f", total)}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = AzulPrimario)
                        }
                    }
                }

                // Selector de Método de Pago
                Text(
                    text = "Método de Pago",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    metodosPago.forEach { metodo ->
                        val seleccionado = cobroState.metodoPago == metodo
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (seleccionado) AzulPrimario else MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = 1.dp,
                                    color = if (seleccionado) AzulPrimario else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.onMetodoPagoCambiado(metodo) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = metodo,
                                fontSize = 13.sp,
                                fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium,
                                color = if (seleccionado) Blanco else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Campos según método de pago
                when (cobroState.metodoPago) {
                    "Efectivo" -> {
                        CampoTextoPos(
                            valor = cobroState.montoRecibido,
                            alCambiarValor = { viewModel.onMontoRecibidoCambiado(it) },
                            etiqueta = "Monto Recibido ($)",
                            iconoInicio = Icons.Filled.Payments,
                            opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done)
                        )

                        // Botones rápidos de monto
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(total, 100.0, 200.0, 500.0, 1000.0, 2000.0).distinct().forEach { montoBoton ->
                                if (montoBoton >= total || montoBoton == total) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { viewModel.onMontoRecibidoCambiado(montoBoton.toInt().toString()) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (montoBoton == total) "Exacto ($${String.format("%.2f", total)})" else "$$montoBoton",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Indicador de Devuelta / Cambio Editable o Aviso de Monto Menor
                        if (montoRecibidoDouble >= total) {
                            TarjetaDevueltaEditable(
                                devueltaCalculada = cambio,
                                devueltaPersonalizada = cobroState.devueltaPersonalizada,
                                alCambiarDevuelta = { viewModel.onDevueltaCambiada(it) },
                                alEnfocar = {
                                    ambitoCorrutina.launch {
                                        delay(250)
                                        estadoScroll.animateScrollTo(estadoScroll.maxValue)
                                    }
                                }
                            )
                        } else if (montoRecibidoDouble > 0.0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AmarilloAdvertencia.copy(alpha = 0.15f))
                                    .border(1.dp, AmarilloAdvertencia.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Monto menor al total",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Faltante: $${String.format("%.2f", faltanteEfectivo)}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = AmarilloAdvertencia.copy(alpha = 0.25f)
                                    ) {
                                        Text(
                                            text = "Cobro Parcial",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    "Transferencia" -> {
                        CampoTextoPos(
                            valor = cobroState.referenciaTransferencia,
                            alCambiarValor = { viewModel.onReferenciaTransferenciaCambiada(it) },
                            etiqueta = "No. Comprobante / Referencia (Opcional)",
                            iconoInicio = Icons.Filled.Receipt
                        )
                    }

                    "Cheque" -> {
                        CampoTextoPos(
                            valor = cobroState.numeroCheque,
                            alCambiarValor = { viewModel.onNumeroChequeCambiado(it) },
                            etiqueta = "Número de Cheque *",
                            iconoInicio = Icons.Filled.Receipt
                        )
                        CampoTextoPos(
                            valor = cobroState.bancoCheque,
                            alCambiarValor = { viewModel.onBancoChequeCambiado(it) },
                            etiqueta = "Banco Emisor",
                            iconoInicio = Icons.Filled.AccountBalance
                        )
                    }

                    "Mixto" -> {
                        val ef = cobroState.montoEfectivo.toDoubleOrNull() ?: 0.0
                        val tr = cobroState.montoTransferencia.toDoubleOrNull() ?: 0.0
                        val ch = cobroState.montoCheque.toDoubleOrNull() ?: 0.0
                        val suma = ef + tr + ch
                        val restante = (total - suma).coerceAtLeast(0.0)

                        CampoTextoPos(
                            valor = cobroState.montoEfectivo,
                            alCambiarValor = { viewModel.onMontoEfectivoCambiado(it) },
                            etiqueta = "Monto Efectivo ($)",
                            iconoInicio = Icons.Filled.Payments,
                            opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        CampoTextoPos(
                            valor = cobroState.montoTransferencia,
                            alCambiarValor = { viewModel.onMontoTransferenciaCambiado(it) },
                            etiqueta = "Monto Transferencia ($)",
                            iconoInicio = Icons.Filled.AccountBalance,
                            opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        CampoTextoPos(
                            valor = cobroState.montoCheque,
                            alCambiarValor = { viewModel.onMontoChequeCambiado(it) },
                            etiqueta = "Monto Cheque ($)",
                            iconoInicio = Icons.Filled.Receipt,
                            opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )

                        // Indicador de cuadre
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (suma >= total) VerdeExito.copy(alpha = 0.12f) else AmarilloAdvertencia.copy(alpha = 0.15f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = if (suma >= total) "✓ Monto total cubierto ($${String.format("%.2f", suma)})" else "Faltante por cubrir: $${String.format("%.2f", restante)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (suma >= total) VerdeExito else AmarilloAdvertencia,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        val cambioMixto = (suma - total).coerceAtLeast(0.0)
                        if (suma >= total && (cambioMixto > 0.0 || cobroState.devueltaPersonalizada != null)) {
                            TarjetaDevueltaEditable(
                                devueltaCalculada = cambioMixto,
                                devueltaPersonalizada = cobroState.devueltaPersonalizada,
                                alCambiarDevuelta = { viewModel.onDevueltaCambiada(it) },
                                alEnfocar = {
                                    ambitoCorrutina.launch {
                                        delay(250)
                                        estadoScroll.animateScrollTo(estadoScroll.maxValue)
                                    }
                                }
                            )
                        }
                    }
                }

                // Datos Opcionales del Cliente
                Text(
                    text = "Datos del Cliente (Opcional)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GrisSecundario
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CampoTextoPos(
                        valor = cobroState.nombreCliente,
                        alCambiarValor = { viewModel.onNombreClienteCambiado(it) },
                        etiqueta = "Nombre Cliente",
                        iconoInicio = Icons.Filled.Person,
                        modifier = Modifier.weight(1f)
                    )
                    CampoTextoPos(
                        valor = cobroState.rncCedulaCliente,
                        alCambiarValor = { viewModel.onRncCedulaClienteCambiado(it) },
                        etiqueta = "RNC / Cédula",
                        modifier = Modifier.weight(1f)
                    )
                }

                if (cobroState.mensajeError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RojoError.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = cobroState.mensajeError!!,
                            color = RojoError,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Botón Confirmar Cobro
                BotonPos(
                    texto = "Confirmar y Finalizar Venta",
                    alHacerClic = {
                        val esEfectivoMenor = cobroState.metodoPago == "Efectivo" && montoRecibidoDouble > 0.0 && montoRecibidoDouble < total
                        val esMixtoMenor = cobroState.metodoPago == "Mixto" && run {
                            val ef = cobroState.montoEfectivo.toDoubleOrNull() ?: 0.0
                            val tr = cobroState.montoTransferencia.toDoubleOrNull() ?: 0.0
                            val ch = cobroState.montoCheque.toDoubleOrNull() ?: 0.0
                            val suma = ef + tr + ch
                            suma > 0.0 && suma < total
                        }

                        if (esEfectivoMenor || esMixtoMenor) {
                            mostrarConfirmacionMontoMenor = true
                        } else {
                            viewModel.procesarCobro(confirmarMenorMonto = false, alTerminarExito = alConfirmarExito)
                        }
                    },
                    estaCargando = cobroState.estaProcesando,
                    variante = VarianteBoton.EXITO,
                    icono = Icons.Filled.Check
                )
            }
        }
        }
    }
}

@Composable
private fun TarjetaDevueltaEditable(
    devueltaCalculada: Double,
    devueltaPersonalizada: String?,
    alCambiarDevuelta: (String?) -> Unit,
    alEnfocar: (() -> Unit)? = null
) {
    val textoCalculado = String.format(Locale.US, "%.2f", devueltaCalculada)
    val textoMostrado = devueltaPersonalizada ?: textoCalculado
    val estaPersonalizada = devueltaPersonalizada != null && devueltaPersonalizada != textoCalculado

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VerdeExito.copy(alpha = 0.12f))
            .border(1.dp, VerdeExito.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Cambio / Devuelta:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = VerdeExito
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Calculado a devolver: $$textoCalculado",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (estaPersonalizada) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { alCambiarDevuelta(null) }
                    ) {
                        Text(
                            text = "Editado • ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AzulPrimario
                        )
                        Text(
                            text = "Restablecer",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AzulPrimario,
                            style = TextStyle(textDecoration = TextDecoration.Underline)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Input sutil/disimulado pero editable al toque
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (estaPersonalizada) AzulPrimario else VerdeExito.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .widthIn(min = 105.dp, max = 140.dp)
                    .clickable { alEnfocar?.invoke() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "$",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (estaPersonalizada) AzulPrimario else VerdeExito
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    BasicTextField(
                        value = textoMostrado,
                        onValueChange = { nuevo ->
                            if (nuevo.isEmpty() || nuevo.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                                alCambiarDevuelta(nuevo)
                            }
                        },
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (estaPersonalizada) AzulPrimario else VerdeExito,
                            textAlign = TextAlign.End
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    alEnfocar?.invoke()
                                }
                            }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Editar devuelta",
                        tint = if (estaPersonalizada) AzulPrimario else VerdeExito.copy(alpha = 0.6f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}
