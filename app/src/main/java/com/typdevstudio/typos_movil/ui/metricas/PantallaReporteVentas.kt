package com.typdevstudio.typos_movil.ui.metricas

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.typdevstudio.typos_movil.datos.impresion.LIMITE_PRODUCTOS_REPORTE
import com.typdevstudio.typos_movil.datos.impresion.OpcionesReporteVentas
import com.typdevstudio.typos_movil.ui.componentes.BotonPos
import com.typdevstudio.typos_movil.ui.historial.TarjetaMetrica
import com.typdevstudio.typos_movil.ui.theme.AmarilloAdvertencia
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.Blanco
import com.typdevstudio.typos_movil.ui.theme.GrisSecundario
import com.typdevstudio.typos_movil.ui.theme.RojoError
import com.typdevstudio.typos_movil.ui.theme.VerdeExito
import com.typdevstudio.typos_movil.ui.ventas.DialogoEstadoImpresion
import com.typdevstudio.typos_movil.utilidades.reportes.sumarDias
import java.text.SimpleDateFormat
import java.util.Date

private enum class CampoFecha { DESDE, HASTA }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaReporteVentas(
    viewModel: ReporteVentasViewModel,
    alVolver: () -> Unit
) {
    val estado by viewModel.estado.collectAsState()
    val opciones by viewModel.opciones.collectAsState()
    val estadoImpresion by viewModel.estadoImpresion.collectAsState()
    val nombreImpresora by viewModel.nombreImpresora.collectAsState()
    var campoFechaAbierto by remember { mutableStateOf<CampoFecha?>(null) }
    var mostrarMenuCajero by remember { mutableStateOf(false) }
    val resumen = estado.resumen
    val ultimoDia = sumarDias(estado.rango.hasta, -1)
    val formatoFecha = remember { SimpleDateFormat("dd/MM/yyyy", LOCALE_ES) }

    DialogoEstadoImpresion(
        estado = estadoImpresion,
        alCerrar = { viewModel.cerrarEstadoImpresion() },
        alReintentar = {
            viewModel.cerrarEstadoImpresion()
            viewModel.imprimirReporte()
        }
    )

    campoFechaAbierto?.let { campo ->
        val estadoSelector = rememberDatePickerState(
            initialSelectedDateMillis = fechaLocalAPicker(if (campo == CampoFecha.DESDE) estado.rango.desde else ultimoDia)
        )
        DatePickerDialog(
            onDismissRequest = { campoFechaAbierto = null },
            confirmButton = {
                TextButton(onClick = {
                    estadoSelector.selectedDateMillis?.let { seleccion ->
                        val dia = fechaPickerALocal(seleccion)
                        if (campo == CampoFecha.DESDE) viewModel.cambiarDesde(dia) else viewModel.cambiarHasta(dia)
                    }
                    campoFechaAbierto = null
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { campoFechaAbierto = null }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = estadoSelector)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Reporte de Ventas", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Blanco)
                        Text(text = "Resumen para imprimir en la impresora", fontSize = 12.sp, color = Blanco.copy(alpha = 0.9f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = alVolver) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Blanco)
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
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Período del reporte
            TarjetaSeccionMetricas(
                titulo = "Período del reporte",
                subtitulo = "Por defecto: el día de hoy",
                icono = Icons.Filled.CalendarMonth
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AtajoFechaReporte.entries.filter { it != AtajoFechaReporte.PERSONALIZADO }.forEach { atajo ->
                        val seleccionado = atajo == estado.atajo
                        FilterChip(
                            selected = seleccionado,
                            onClick = { viewModel.seleccionarAtajo(atajo) },
                            label = { Text(atajo.etiqueta, fontSize = 12.sp, fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SelectorCampo(
                        etiqueta = "Desde",
                        valor = formatoFecha.format(Date(estado.rango.desde)),
                        icono = Icons.Filled.CalendarMonth,
                        alHacerClic = { campoFechaAbierto = CampoFecha.DESDE },
                        modifier = Modifier.weight(1f)
                    )
                    SelectorCampo(
                        etiqueta = "Hasta",
                        valor = formatoFecha.format(Date(ultimoDia)),
                        icono = Icons.Filled.CalendarMonth,
                        alHacerClic = { campoFechaAbierto = CampoFecha.HASTA },
                        modifier = Modifier.weight(1f)
                    )
                }

                Box {
                    SelectorCampo(
                        etiqueta = "Cajero",
                        valor = estado.cajero ?: "Todos los cajeros",
                        icono = Icons.Filled.ArrowDropDown,
                        alHacerClic = { mostrarMenuCajero = true }
                    )
                    DropdownMenu(expanded = mostrarMenuCajero, onDismissRequest = { mostrarMenuCajero = false }) {
                        DropdownMenuItem(
                            text = { Text("Todos los cajeros") },
                            onClick = {
                                viewModel.seleccionarCajero(null)
                                mostrarMenuCajero = false
                            }
                        )
                        // Si el cajero elegido no vendió en el nuevo rango igual se muestra para poder quitarlo
                        (estado.cajerosDisponibles + listOfNotNull(estado.cajero)).distinct().forEach { cajero ->
                            DropdownMenuItem(
                                text = { Text(cajero) },
                                onClick = {
                                    viewModel.seleccionarCajero(cajero)
                                    mostrarMenuCajero = false
                                }
                            )
                        }
                    }
                }
            }

            // Resumen en pantalla
            TarjetaSeccionMetricas(
                titulo = "Resumen",
                subtitulo = if (estado.rango.cantidadDias == 1) {
                    SimpleDateFormat("EEEE d 'de' MMMM", LOCALE_ES).format(Date(estado.rango.desde)).replaceFirstChar { it.uppercase() }
                } else {
                    "${formatoFecha.format(Date(estado.rango.desde))} al ${formatoFecha.format(Date(ultimoDia))}"
                }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TarjetaMetrica(
                        titulo = "Total vendido",
                        monto = formatearMonto(resumen.total),
                        subtitulo = "${resumen.cantidadFacturas} facturas",
                        colorPrimario = VerdeExito,
                        modifier = Modifier.weight(1f)
                    )
                    TarjetaMetrica(
                        titulo = "ITBIS cobrado",
                        monto = formatearMonto(resumen.itbis),
                        subtitulo = "Impuesto",
                        colorPrimario = AzulPrimario,
                        modifier = Modifier.weight(1f)
                    )
                    TarjetaMetrica(
                        titulo = "Anuladas",
                        monto = "${resumen.cantidadAnuladas}",
                        subtitulo = formatearMonto(resumen.montoAnulado),
                        colorPrimario = if (resumen.cantidadAnuladas > 0) RojoError else GrisSecundario,
                        modifier = Modifier.weight(0.8f)
                    )
                }
                if (resumen.metodosPago.isEmpty()) {
                    Text(text = "No hay ventas en este período.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                resumen.metodosPago.forEach { metodo ->
                    FilaMonto("${metodo.metodo} (${metodo.cantidadVentas})", formatearMonto(metodo.monto))
                }
                if (resumen.montoPendiente > 0.005) {
                    FilaMonto("Pendiente de cobro", formatearMonto(resumen.montoPendiente))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                FilaMonto("Efectivo en caja", formatearMonto(resumen.efectivoEnCaja), resaltado = true)
            }

            // Secciones a imprimir
            TarjetaSeccionMetricas(titulo = "Incluir en el reporte", subtitulo = "Se recuerda su elección para la próxima vez") {
                Column {
                    FilaInterruptor("Métodos de pago", "Efectivo, transferencia y cheque", opciones.metodosPago) {
                        viewModel.cambiarOpciones(opciones.copy(metodosPago = it))
                    }
                    FilaInterruptor("Ventas por cajero", "Cuánto vendió cada usuario", opciones.cajeros) {
                        viewModel.cambiarOpciones(opciones.copy(cajeros = it))
                    }
                    FilaInterruptor("Productos más vendidos", "Top $LIMITE_PRODUCTOS_REPORTE por monto", opciones.productos) {
                        viewModel.cambiarOpciones(opciones.copy(productos = it))
                    }
                    FilaInterruptor("Facturas anuladas", "Número, motivo y monto", opciones.anuladas) {
                        viewModel.cambiarOpciones(opciones.copy(anuladas = it))
                    }
                    FilaInterruptor("Detalle de cada factura", "Lista completa (ticket más largo)", opciones.detalleFacturas) {
                        viewModel.cambiarOpciones(opciones.copy(detalleFacturas = it))
                    }
                }
            }

            // Impresora configurada
            val hayImpresora = nombreImpresora != null
            val colorEstado = if (hayImpresora) VerdeExito else AmarilloAdvertencia
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorEstado.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                    .border(1.dp, colorEstado.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = if (hayImpresora) Icons.Filled.Bluetooth else Icons.Filled.BluetoothDisabled,
                    contentDescription = null,
                    tint = colorEstado,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (hayImpresora) "Impresora: $nombreImpresora" else "No hay impresora configurada (Configuración > Impresora)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            BotonPos(
                texto = "Imprimir Reporte",
                alHacerClic = { viewModel.imprimirReporte() },
                icono = Icons.Filled.Print,
                tamanoTexto = 16.sp,
                estaCargando = estadoImpresion?.terminado == false,
                modifier = Modifier.heightIn(min = 54.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SelectorCampo(
    etiqueta: String,
    valor: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    alHacerClic: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = etiqueta, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(
            onClick = alHacerClic,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Text(
                text = valor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(imageVector = icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun FilaMonto(etiqueta: String, monto: String, resaltado: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = etiqueta,
            fontSize = 12.sp,
            fontWeight = if (resaltado) FontWeight.Bold else FontWeight.Normal,
            color = if (resaltado) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = monto, fontSize = 12.sp, fontWeight = if (resaltado) FontWeight.Bold else FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun FilaInterruptor(titulo: String, detalle: String, activo: Boolean, alCambiar: (Boolean) -> Unit) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = activo, role = Role.Switch, onValueChange = alCambiar)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = titulo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = detalle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = activo, onCheckedChange = null)
    }
}
