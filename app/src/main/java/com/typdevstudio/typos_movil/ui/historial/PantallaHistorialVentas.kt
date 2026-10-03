package com.typdevstudio.typos_movil.ui.historial

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.typdevstudio.typos_movil.datos.local.entidades.DetalleVentaEntidad
import com.typdevstudio.typos_movil.datos.local.entidades.VentaEntidad
import com.typdevstudio.typos_movil.datos.local.relaciones.VentaConDetalles
import com.typdevstudio.typos_movil.ui.componentes.BotonPos
import com.typdevstudio.typos_movil.ui.componentes.CampoTextoPos
import com.typdevstudio.typos_movil.ui.componentes.VarianteBoton
import com.typdevstudio.typos_movil.ui.theme.AmarilloAdvertencia
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.AzulPrimarioClaro
import com.typdevstudio.typos_movil.ui.theme.Blanco
import com.typdevstudio.typos_movil.ui.theme.CelesteInformacion
import com.typdevstudio.typos_movil.ui.theme.FondoClaro
import com.typdevstudio.typos_movil.ui.theme.GrisClaro
import com.typdevstudio.typos_movil.ui.theme.GrisMedio
import com.typdevstudio.typos_movil.ui.theme.GrisSecundario
import com.typdevstudio.typos_movil.ui.theme.GrisTexto
import com.typdevstudio.typos_movil.ui.theme.RojoError
import com.typdevstudio.typos_movil.ui.theme.VerdeExito
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaHistorialVentas(
    viewModel: HistorialViewModel,
    alVolver: () -> Unit
) {
    val ventas by viewModel.ventas.collectAsState()
    val resumen by viewModel.resumen.collectAsState()
    val busqueda by viewModel.busqueda.collectAsState()
    val filtroFecha by viewModel.filtroFecha.collectAsState()
    val filtroEstado by viewModel.filtroEstado.collectAsState()
    val ventaSeleccionada by viewModel.ventaSeleccionada.collectAsState()
    val mostrarDialogoAnular by viewModel.mostrarDialogoAnular.collectAsState()
    val motivoAnulacion by viewModel.motivoAnulacion.collectAsState()
    val estaAnulando by viewModel.estaAnulando.collectAsState()
    val estaReimprimiendo by viewModel.estaReimprimiendo.collectAsState()
    val mensajeAlerta by viewModel.mensajeAlerta.collectAsState()
    val esErrorAlerta by viewModel.esErrorAlerta.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(mensajeAlerta) {
        mensajeAlerta?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarAlerta()
        }
    }

    // Modal de Detalle de Venta
    if (ventaSeleccionada != null) {
        DialogoDetalleVenta(
            ventaConDetalles = ventaSeleccionada!!,
            esVentaDeHoy = viewModel.esVentaDeHoy(ventaSeleccionada!!.venta.fecha),
            estaReimprimiendo = estaReimprimiendo,
            alReimprimir = { viewModel.reimprimirTicket(ventaSeleccionada!!) },
            alIniciarAnulacion = { viewModel.iniciarAnulacion() },
            alCerrar = { viewModel.seleccionarVenta(null) }
        )
    }

    // Diálogo de Confirmación de Anulación
    if (mostrarDialogoAnular && ventaSeleccionada != null) {
        DialogoConfirmarAnulacion(
            numeroFactura = ventaSeleccionada!!.venta.numeroFactura,
            motivo = motivoAnulacion,
            estaProcesando = estaAnulando,
            alCambiarMotivo = { viewModel.onMotivoAnulacionCambiado(it) },
            alConfirmar = { viewModel.confirmarAnulacionVenta() },
            alCancelar = { viewModel.cancelarAnulacion() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Historial de Ventas",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Blanco
                        )
                        Text(
                            text = "${resumen.cantidadCompletadas} ventas realizadas (${filtroFecha.etiqueta})",
                            fontSize = 12.sp,
                            color = AzulPrimarioClaro
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = alVolver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Blanco
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AzulPrimario)
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = FondoClaro
    ) { paddingValores ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValores)
        ) {
            // Tarjeta Superior de Resumen de Ventas
            Card(
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Blanco),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Tarjetas de Métricas Rápidas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Total Facturado
                        TarjetaMetrica(
                            titulo = "Total Facturado",
                            monto = "$${String.format("%.2f", resumen.totalFacturado)}",
                            subtitulo = "${resumen.cantidadCompletadas} facturas",
                            colorPrimario = VerdeExito,
                            modifier = Modifier.weight(1f)
                        )

                        // Total ITBIS
                        TarjetaMetrica(
                            titulo = "ITBIS Cobrado",
                            monto = "$${String.format("%.2f", resumen.totalItbis)}",
                            subtitulo = "Impuesto 18%",
                            colorPrimario = AzulPrimario,
                            modifier = Modifier.weight(1f)
                        )

                        // Anuladas
                        TarjetaMetrica(
                            titulo = "Anuladas",
                            monto = "${resumen.cantidadCanceladas}",
                            subtitulo = "Canceladas",
                            colorPrimario = if (resumen.cantidadCanceladas > 0) RojoError else GrisSecundario,
                            modifier = Modifier.weight(0.9f)
                        )
                    }

                    // Buscador de Factura / Cliente
                    OutlinedTextField(
                        value = busqueda,
                        onValueChange = { viewModel.onBusquedaCambiada(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Buscar por factura, cliente o RNC...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Buscar",
                                tint = AzulPrimario
                            )
                        },
                        trailingIcon = {
                            if (busqueda.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onBusquedaCambiada("") }) {
                                    Icon(imageVector = Icons.Filled.Clear, contentDescription = "Limpiar", tint = GrisMedio)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = GrisTexto,
                            unfocusedTextColor = GrisTexto,
                            focusedPlaceholderColor = GrisMedio,
                            unfocusedPlaceholderColor = GrisMedio,
                            focusedBorderColor = AzulPrimario,
                            unfocusedBorderColor = GrisClaro,
                            focusedContainerColor = FondoClaro,
                            unfocusedContainerColor = FondoClaro,
                            cursorColor = AzulPrimario
                        )
                    )

                    // Filtros de Rango de Fecha (Chips horizontales)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FiltroFechaHistorial.entries.forEach { filtro ->
                            val seleccionado = filtro == filtroFecha
                            FilterChip(
                                selected = seleccionado,
                                onClick = { viewModel.onFiltroFechaSeleccionado(filtro) },
                                label = { Text(filtro.etiqueta, fontSize = 12.sp, fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AzulPrimario,
                                    selectedLabelColor = Blanco,
                                    containerColor = FondoClaro,
                                    labelColor = GrisTexto
                                )
                            )
                        }
                    }

                    // Filtros de Estado de Venta
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FiltroEstadoHistorial.entries.forEach { filtro ->
                            val seleccionado = filtro == filtroEstado
                            FilterChip(
                                selected = seleccionado,
                                onClick = { viewModel.onFiltroEstadoSeleccionado(filtro) },
                                label = { Text(filtro.etiqueta, fontSize = 12.sp, fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when (filtro) {
                                        FiltroEstadoHistorial.COMPLETADAS -> VerdeExito
                                        FiltroEstadoHistorial.CANCELADAS -> RojoError
                                        FiltroEstadoHistorial.TODAS -> GrisTexto
                                    },
                                    selectedLabelColor = Blanco,
                                    containerColor = FondoClaro,
                                    labelColor = GrisTexto
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Lista de Ventas
            if (ventas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = GrisMedio,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No se encontraron ventas",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrisTexto
                        )
                        Text(
                            text = "Ajusta los filtros de fecha o búsqueda para ver otros registros",
                            fontSize = 12.sp,
                            color = GrisSecundario,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = ventas,
                        key = { it.venta.id }
                    ) { itemVenta ->
                        TarjetaVentaHistorial(
                            ventaConDetalles = itemVenta,
                            alHacerClic = { viewModel.seleccionarVenta(itemVenta) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TarjetaMetrica(
    titulo: String,
    monto: String,
    subtitulo: String,
    colorPrimario: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colorPrimario.copy(alpha = 0.08f))
            .border(1.dp, colorPrimario.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        Column {
            Text(
                text = titulo,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = GrisSecundario,
                maxLines = 1
            )
            Text(
                text = monto,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colorPrimario,
                maxLines = 1
            )
            Text(
                text = subtitulo,
                fontSize = 9.sp,
                color = GrisMedio,
                maxLines = 1
            )
        }
    }
}

@Composable
fun TarjetaVentaHistorial(
    ventaConDetalles: VentaConDetalles,
    alHacerClic: () -> Unit
) {
    val venta = ventaConDetalles.venta
    val esCancelada = venta.estadoVenta.equals("Cancelada", ignoreCase = true)
    val fechaTexto = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(venta.fecha))
    val cantidadTotalArticulos = ventaConDetalles.detalles.sumOf { it.cantidad }.toInt()

    Card(
        onClick = alHacerClic,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Blanco),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (esCancelada) RojoError.copy(alpha = 0.3f) else GrisClaro
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Número de Factura
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Receipt,
                        contentDescription = null,
                        tint = if (esCancelada) RojoError else AzulPrimario,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = venta.numeroFactura,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (esCancelada) RojoError else GrisTexto
                    )
                }

                // Badge Estado
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (esCancelada) RojoError.copy(alpha = 0.12f) else VerdeExito.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (esCancelada) "ANULADA" else "COMPLETADA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (esCancelada) RojoError else VerdeExito
                    )
                }
            }

            // Cliente y Fecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = venta.nombreCliente ?: "Consumidor Final",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = GrisTexto,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = fechaTexto,
                    fontSize = 11.sp,
                    color = GrisSecundario
                )
            }

            HorizontalDivider(color = GrisClaro.copy(alpha = 0.6f))

            // Totales, Método de Pago y Cantidad de Artículos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Badge Método de Pago
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AzulPrimarioClaro.copy(alpha = 0.25f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = venta.metodoPago,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AzulPrimario
                        )
                    }

                    Text(
                        text = "$cantidadTotalArticulos arts.",
                        fontSize = 12.sp,
                        color = GrisSecundario
                    )
                }

                // Monto Total
                Text(
                    text = "$${String.format("%.2f", venta.total)}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (esCancelada) RojoError else AzulPrimario
                )
            }
        }
    }
}

@Composable
fun DialogoDetalleVenta(
    ventaConDetalles: VentaConDetalles,
    esVentaDeHoy: Boolean,
    estaReimprimiendo: Boolean,
    alReimprimir: () -> Unit,
    alIniciarAnulacion: () -> Unit,
    alCerrar: () -> Unit
) {
    val venta = ventaConDetalles.venta
    val detalles = ventaConDetalles.detalles
    val esCancelada = venta.estadoVenta.equals("Cancelada", ignoreCase = true)
    val fechaTexto = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(venta.fecha))

    Dialog(
        onDismissRequest = alCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Blanco),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Encabezado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Factura ${venta.numeroFactura}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AzulPrimario
                        )
                        Text(
                            text = fechaTexto,
                            fontSize = 12.sp,
                            color = GrisSecundario
                        )
                    }

                    IconButton(onClick = alCerrar) {
                        Icon(imageVector = Icons.Filled.Clear, contentDescription = "Cerrar", tint = GrisTexto)
                    }
                }

                // Banner de Venta Anulada
                if (esCancelada) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(RojoError.copy(alpha = 0.12f))
                            .border(1.dp, RojoError.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Filled.Cancel, contentDescription = null, tint = RojoError, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "VENTA ANULADA",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RojoError
                                )
                                Text(
                                    text = "Motivo: ${venta.motivoAnulacion ?: "Sin motivo especificado"}",
                                    fontSize = 11.sp,
                                    color = GrisTexto
                                )
                                Text(
                                    text = "El inventario de estos productos fue restituido al stock.",
                                    fontSize = 10.sp,
                                    color = GrisSecundario
                                )
                            }
                        }
                    }
                }

                // Datos de la Operación y Cliente
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(FondoClaro)
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Cliente:", fontSize = 12.sp, color = GrisSecundario)
                            Text(text = venta.nombreCliente ?: "Consumidor Final", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GrisTexto)
                        }
                        if (!venta.rncCedulaCliente.isNullOrBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "RNC / Cédula:", fontSize = 12.sp, color = GrisSecundario)
                                Text(text = venta.rncCedulaCliente, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GrisTexto)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Cajero / Usuario:", fontSize = 12.sp, color = GrisSecundario)
                            Text(text = venta.usuario ?: "Administrador", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GrisTexto)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Método de Pago:", fontSize = 12.sp, color = GrisSecundario)
                            Text(text = venta.metodoPago, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AzulPrimario)
                        }
                    }
                }

                Text(
                    text = "Artículos Vendidos (${detalles.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AzulPrimario
                )

                // Lista de Items de la Factura
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(FondoClaro)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    detalles.forEach { detalle ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = detalle.nombre,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GrisTexto
                                )
                                val cantStr = if (detalle.cantidad % 1.0 == 0.0) detalle.cantidad.toInt().toString() else String.format("%.2f", detalle.cantidad)
                                Text(
                                    text = "$cantStr x $${String.format("%.2f", detalle.precio)} ${if (detalle.montoItbis > 0) "(ITBIS incl.)" else "(Exento)"}",
                                    fontSize = 11.sp,
                                    color = GrisSecundario
                                )
                            }
                            Text(
                                text = "$${String.format("%.2f", detalle.total)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GrisTexto
                            )
                        }
                        HorizontalDivider(color = GrisClaro.copy(alpha = 0.5f))
                    }
                }

                // Resumen Fiscal y Totales
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Subtotal Neto:", fontSize = 13.sp, color = GrisSecundario)
                        Text(text = "$${String.format("%.2f", venta.subTotalNeto)}", fontSize = 13.sp, color = GrisTexto)
                    }
                    if (venta.montoItbis > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "ITBIS (18%):", fontSize = 13.sp, color = GrisSecundario)
                            Text(text = "$${String.format("%.2f", venta.montoItbis)}", fontSize = 13.sp, color = AzulPrimario)
                        }
                    }
                    if (venta.descuento > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Descuento:", fontSize = 13.sp, color = RojoError)
                            Text(text = "-$${String.format("%.2f", venta.descuento)}", fontSize = 13.sp, color = RojoError)
                        }
                    }
                    HorizontalDivider(color = GrisClaro)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "TOTAL A PAGAR:", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = GrisTexto)
                        Text(text = "$${String.format("%.2f", venta.total)}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = if (esCancelada) RojoError else AzulPrimario)
                    }
                    if (venta.metodoPago == "Efectivo") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Efectivo Recibido:", fontSize = 12.sp, color = GrisSecundario)
                            Text(text = "$${String.format("%.2f", venta.montoRecibido)}", fontSize = 12.sp, color = GrisTexto)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Cambio / Devuelta:", fontSize = 12.sp, color = GrisSecundario)
                            Text(text = "$${String.format("%.2f", venta.montoDevuelto)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = VerdeExito)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Botones de Acción: Reimprimir y Anular
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Botón Reimprimir
                    BotonPos(
                        texto = if (estaReimprimiendo) "Imprimiendo..." else "Reimprimir",
                        alHacerClic = alReimprimir,
                        variante = VarianteBoton.PRIMARIO,
                        icono = Icons.Filled.Print,
                        estaCargando = estaReimprimiendo,
                        modifier = Modifier.weight(1f)
                    )

                    // Botón Anular Venta (Solo si no está cancelada y es de hoy)
                    if (!esCancelada) {
                        if (esVentaDeHoy) {
                            BotonPos(
                                texto = "Anular Venta",
                                alHacerClic = alIniciarAnulacion,
                                variante = VarianteBoton.PELIGRO,
                                icono = Icons.Filled.Block,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            // Desactivado si es de días anteriores
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GrisClaro)
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No anulable (Solo del día)",
                                    fontSize = 11.sp,
                                    color = GrisSecundario,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DialogoConfirmarAnulacion(
    numeroFactura: String,
    motivo: String,
    estaProcesando: Boolean,
    alCambiarMotivo: (String) -> Unit,
    alConfirmar: () -> Unit,
    alCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!estaProcesando) alCancelar() },
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = RojoError,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "¿Anular Factura $numeroFactura?",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Esta acción es irreversible. Todos los productos de esta venta se reincorporarán automáticamente al stock del inventario.",
                    fontSize = 13.sp,
                    color = GrisTexto
                )
                CampoTextoPos(
                    valor = motivo,
                    alCambiarValor = alCambiarMotivo,
                    etiqueta = "Motivo de anulación (Opcional)",
                    iconoInicio = Icons.Filled.Info
                )
            }
        },
        confirmButton = {
            BotonPos(
                texto = if (estaProcesando) "Anulando..." else "Confirmar Anulación",
                alHacerClic = alConfirmar,
                variante = VarianteBoton.PELIGRO,
                estaCargando = estaProcesando
            )
        },
        dismissButton = {
            TextButton(
                onClick = alCancelar,
                enabled = !estaProcesando
            ) {
                Text(text = "Cancelar", color = GrisTexto)
            }
        }
    )
}
