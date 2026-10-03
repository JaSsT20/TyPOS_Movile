package com.typdevstudio.typos_movil.ui.ventas

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.typdevstudio.typos_movil.datos.local.entidades.ProductoEntidad
import com.typdevstudio.typos_movil.ui.componentes.BotonPos
import com.typdevstudio.typos_movil.ui.componentes.DialogoEscanerCodigoBarras
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaVentas(
    viewModel: VentasViewModel,
    alVolver: () -> Unit
) {
    val productos by viewModel.productos.collectAsState()
    val carrito by viewModel.carrito.collectAsState()
    val busqueda by viewModel.busqueda.collectAsState()
    val categoriasDisponibles by viewModel.categoriasDisponibles.collectAsState()
    val categoriaSeleccionada by viewModel.categoriaSeleccionada.collectAsState()
    val ultimaVenta by viewModel.ultimaVentaRealizada.collectAsState()
    val mensajeAlerta by viewModel.mensajeAlerta.collectAsState()

    var mostrarEscanerCamara by remember { mutableStateOf(false) }
    var mostrarModalCarrito by remember { mutableStateOf(false) }
    var mostrarModalCobro by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(mensajeAlerta) {
        mensajeAlerta?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarAlerta()
        }
    }

    val totalVenta by remember(carrito) { derivedStateOf { carrito.sumOf { it.total } } }
    val subtotalNeto by remember(carrito) { derivedStateOf { carrito.sumOf { it.subTotalNeto } } }
    val itbisTotal by remember(carrito) { derivedStateOf { carrito.sumOf { it.montoItbis } } }
    val cantidadArticulos by remember(carrito) { derivedStateOf { carrito.sumOf { it.cantidad.toInt() } } }

    // Escáner de Código de Barras en Caja (Agrega al instante)
    DialogoEscanerCodigoBarras(
        mostrar = mostrarEscanerCamara,
        alDetectarCodigo = { codigo ->
            viewModel.escanearYAgregarAlCarrito(codigo)
            mostrarEscanerCamara = false
        },
        alCerrar = { mostrarEscanerCamara = false }
    )

    // Modal de Cobro
    DialogoCobro(
        mostrar = mostrarModalCobro,
        viewModel = viewModel,
        alConfirmarExito = {
            mostrarModalCobro = false
            mostrarModalCarrito = false
        },
        alDescartar = { mostrarModalCobro = false }
    )

    // Modal de Venta Exitosa
    DialogoVentaExitosa(
        venta = ultimaVenta,
        alCerrar = { viewModel.cerrarModalVentaExitosa() },
        alImprimir = {
            viewModel.imprimirTicketUltimaVenta { resultado ->
                // Feedback de impresión
            }
        }
    )

    // Modal Desplegable del Carrito de Compras
    if (mostrarModalCarrito) {
        ModalBottomSheet(
            onDismissRequest = { mostrarModalCarrito = false },
            containerColor = Blanco,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            HojaDetalleCarrito(
                carrito = carrito,
                subtotal = viewModel.obtenerSubTotalNeto(),
                itbis = viewModel.obtenerMontoItbis(),
                total = totalVenta,
                alIncrementar = { viewModel.incrementarCantidad(it) },
                alDecrementar = { viewModel.decrementarCantidad(it) },
                alEliminar = { viewModel.eliminarDelCarrito(it) },
                alVaciar = { viewModel.vaciarCarrito() },
                alProcederCobro = {
                    mostrarModalCarrito = false
                    mostrarModalCobro = true
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Caja y Ventas",
                        fontWeight = FontWeight.Bold,
                        color = Blanco
                    )
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
                actions = {
                    if (carrito.isNotEmpty()) {
                        IconButton(onClick = { viewModel.vaciarCarrito() }) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = "Vaciar carrito",
                                tint = Blanco
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AzulPrimario)
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            // Barra inferior flotante de Carrito / Cobro
            if (carrito.isNotEmpty()) {
                BarraInferiorCarrito(
                    cantidadArticulos = cantidadArticulos,
                    total = totalVenta,
                    alVerCarrito = { mostrarModalCarrito = true },
                    alCobrar = { mostrarModalCobro = true }
                )
            }
        },
        containerColor = FondoClaro
    ) { paddingValores ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValores)
        ) {
            // Buscador con Escáner y Categorías
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Blanco)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { viewModel.onBusquedaCambiada(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar producto para vender...", fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Buscar",
                            tint = AzulPrimario
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (busqueda.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onBusquedaCambiada("") }) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = "Limpiar",
                                        tint = GrisMedio
                                    )
                                }
                            }
                            IconButton(onClick = { mostrarEscanerCamara = true }) {
                                Icon(
                                    imageVector = Icons.Filled.QrCodeScanner,
                                    contentDescription = "Escanear para agregar",
                                    tint = AzulPrimario
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AzulPrimario,
                        unfocusedBorderColor = GrisClaro,
                        focusedContainerColor = FondoClaro,
                        unfocusedContainerColor = FondoClaro
                    )
                )

                // Filtro de Categorías
                if (categoriasDisponibles.size > 1) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categoriasDisponibles) { cat ->
                            val seleccionada = cat == categoriaSeleccionada
                            FilterChip(
                                selected = seleccionada,
                                onClick = { viewModel.onCategoriaSeleccionada(cat) },
                                label = { Text(cat, fontSize = 13.sp) },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AzulPrimario,
                                    selectedLabelColor = Blanco,
                                    containerColor = FondoClaro,
                                    labelColor = GrisTexto
                                )
                            )
                        }
                    }
                }
            }

            // Lista de productos para agregar al carrito
            if (productos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Inventory2,
                            contentDescription = null,
                            tint = GrisMedio,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No hay productos disponibles",
                            fontSize = 15.sp,
                            color = GrisMedio,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = productos,
                        key = { it.id }
                    ) { producto ->
                        val itemEnCarrito = carrito.find { it.producto.id == producto.id }
                        TarjetaProductoVenta(
                            producto = producto,
                            cantidadEnCarrito = itemEnCarrito?.cantidad?.toInt() ?: 0,
                            alAgregar = { viewModel.agregarAlCarrito(producto) }
                        )
                    }
                    item {
                        // Espacio inferior para no tapar con la barra de carrito
                        Spacer(modifier = Modifier.height(if (carrito.isNotEmpty()) 100.dp else 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TarjetaProductoVenta(
    producto: ProductoEntidad,
    cantidadEnCarrito: Int,
    alAgregar: () -> Unit
) {
    Card(
        onClick = alAgregar,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Blanco),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Inicial
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(AzulPrimarioClaro.copy(alpha = 0.3f), shape = RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = producto.nombre.take(1).uppercase(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AzulPrimario
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = producto.nombre,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = GrisTexto,
                    maxLines = 1
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$${String.format("%.2f", producto.precioVenta)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AzulPrimario
                    )

                    if (producto.controlaStock) {
                        val colorStock = if (producto.stock <= 0) RojoError else if (producto.stock <= 5) AmarilloAdvertencia else VerdeExito
                        Text(
                            text = "Stock: ${producto.stock.toInt()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorStock
                        )
                    }

                    if (producto.exentoItbis) {
                        Box(
                            modifier = Modifier
                                .background(CelesteInformacion.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("Exento", fontSize = 10.sp, color = CelesteInformacion, fontWeight = FontWeight.Bold)
                        }
                    } else if (!producto.itbisIncluido) {
                        Box(
                            modifier = Modifier
                                .background(AmarilloAdvertencia.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("+18% ITBIS", fontSize = 10.sp, color = GrisTexto, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

                // Indicador de cuántos hay en carrito o botón de agregar
                val estaAgotado = producto.controlaStock && producto.stock <= 0
                if (cantidadEnCarrito > 0) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(AzulPrimario, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$cantidadEnCarrito",
                            color = Blanco,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                } else if (estaAgotado) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(RojoError.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Block,
                            contentDescription = "Agotado",
                            tint = RojoError,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(AzulPrimarioClaro.copy(alpha = 0.35f), shape = RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Agregar",
                            tint = AzulPrimario,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
        }
    }
}

@Composable
fun BarraInferiorCarrito(
    cantidadArticulos: Int,
    total: Double,
    alVerCarrito: () -> Unit,
    alCobrar: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        colors = CardDefaults.cardColors(containerColor = Blanco),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    // Si se arrastra/hala hacia arriba (dragAmount negativo significativo)
                    if (dragAmount < -8f) {
                        alVerCarrito()
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Indicador visual superior / Manija de arrastre (Drag handle)
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 2.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .background(GrisMedio.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                    .clip(RoundedCornerShape(2.dp))
                    .clickable(onClick = alVerCarrito)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Resumen de Carrito (toda la sección izquierda cliqueable para abrir)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = alVerCarrito)
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = AzulPrimario,
                                contentColor = Blanco
                            ) {
                                Text("$cantidadArticulos")
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ShoppingCart,
                            contentDescription = "Carrito",
                            tint = AzulPrimario,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Total a Pagar",
                            fontSize = 11.sp,
                            color = GrisSecundario
                        )
                        Text(
                            text = "$${String.format("%.2f", total)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AzulPrimario
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Botón Cobrar
                BotonPos(
                    texto = "Cobrar",
                    alHacerClic = alCobrar,
                    variante = VarianteBoton.EXITO,
                    icono = Icons.Filled.Payments,
                    modifier = Modifier.width(140.dp)
                )
            }
        }
    }
}

@Composable
fun HojaDetalleCarrito(
    carrito: List<ItemCarrito>,
    subtotal: Double,
    itbis: Double,
    total: Double,
    alIncrementar: (Long) -> Unit,
    alDecrementar: (Long) -> Unit,
    alEliminar: (Long) -> Unit,
    alVaciar: () -> Unit,
    alProcederCobro: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Cabecera del Carrito
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Carrito de Compras (${carrito.sumOf { it.cantidad.toInt() }})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = GrisTexto
            )
            Text(
                text = "Vaciar",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = RojoError,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = alVaciar)
                    .padding(6.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Lista de Artículos en Carrito
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(carrito, key = { it.producto.id }) { item ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = FondoClaro),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.producto.nombre,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = GrisTexto,
                                maxLines = 1
                            )
                            Text(
                                text = "$${String.format("%.2f", item.precioUnitario)} c/u  |  Total: $${String.format("%.2f", item.total)}",
                                fontSize = 12.sp,
                                color = GrisSecundario
                            )
                        }

                        // Controles de cantidad (- / +)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Blanco)
                                    .clickable { alDecrementar(item.producto.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (item.cantidad == 1.0) Icons.Filled.Delete else Icons.Filled.Remove,
                                    contentDescription = "Disminuir",
                                    tint = if (item.cantidad == 1.0) RojoError else GrisTexto,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Text(
                                text = "${item.cantidad.toInt()}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = GrisTexto,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            val limiteAlcanzado = item.producto.controlaStock && item.cantidad >= item.producto.stock
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (limiteAlcanzado) GrisClaro else AzulPrimario)
                                    .clickable { alIncrementar(item.producto.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Aumentar",
                                    tint = if (limiteAlcanzado) GrisMedio else Blanco,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Desglose de Totales e ITBIS
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AzulPrimarioClaro.copy(alpha = 0.25f))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal:", fontSize = 13.sp, color = GrisTexto)
                    Text("$${String.format("%.2f", subtotal)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = GrisTexto)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("ITBIS (18%):", fontSize = 13.sp, color = GrisTexto)
                    Text("$${String.format("%.2f", itbis)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = GrisTexto)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TOTAL:", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AzulPrimario)
                    Text("$${String.format("%.2f", total)}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = AzulPrimario)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        BotonPos(
            texto = "Proceder al Cobro ($${String.format("%.2f", total)})",
            alHacerClic = alProcederCobro,
            variante = VarianteBoton.EXITO,
            icono = Icons.Filled.Payments
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
