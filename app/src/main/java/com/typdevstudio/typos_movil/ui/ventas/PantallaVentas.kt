package com.typdevstudio.typos_movil.ui.ventas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.typdevstudio.typos_movil.datos.local.entidades.ProductoEntidad
import com.typdevstudio.typos_movil.ui.componentes.BotonPos
import com.typdevstudio.typos_movil.ui.componentes.DialogoEscanerCodigoBarras
import com.typdevstudio.typos_movil.ui.componentes.VarianteBoton
import com.typdevstudio.typos_movil.ui.componentes.paginacion.ControlPaginacionPos
import com.typdevstudio.typos_movil.ui.componentes.paginacion.rememberEstadoPaginacion
import com.typdevstudio.typos_movil.ui.theme.AmarilloAdvertencia
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.AzulPrimarioClaro
import com.typdevstudio.typos_movil.ui.theme.Blanco
import com.typdevstudio.typos_movil.ui.theme.CelesteInformacion
import com.typdevstudio.typos_movil.ui.theme.GrisClaro
import com.typdevstudio.typos_movil.ui.theme.GrisMedio
import com.typdevstudio.typos_movil.ui.theme.GrisSecundario
import com.typdevstudio.typos_movil.ui.theme.RojoError
import com.typdevstudio.typos_movil.ui.theme.VerdeExito
import com.typdevstudio.typos_movil.utilidades.ImagenProductoTarjeta
import com.typdevstudio.typos_movil.utilidades.MiniaturaProducto

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

    var modoVistaCuadricula by remember { mutableStateOf(true) }
    var mostrarEscanerCamara by remember { mutableStateOf(false) }
    var mostrarModalCarrito by remember { mutableStateOf(false) }
    var mostrarModalCobro by remember { mutableStateOf(false) }
    var productoParaCantidad by remember { mutableStateOf<ProductoEntidad?>(null) }
    var cantidadInicialDialogo by remember { mutableDoubleStateOf(1.0) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Controlador de paginación reactivo para optimizar memoria en gama baja
    val estadoPaginacion = rememberEstadoPaginacion(
        tamanioPaginaInicial = 16,
        claveReinicio = busqueda + categoriaSeleccionada
    )

    // Subconjunto paginado visible actualmente
    val productosPaginados = remember(productos, estadoPaginacion.paginaActual, estadoPaginacion.tamanioPagina) {
        estadoPaginacion.obtenerPagina(productos)
    }

    LaunchedEffect(mensajeAlerta) {
        mensajeAlerta?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarAlerta()
        }
    }

    val totalVenta by remember(carrito) { derivedStateOf { carrito.sumOf { it.total } } }
    val cantidadArticulos by remember(carrito) { derivedStateOf { carrito.sumOf { it.cantidad.toInt() } } }

    // Diálogo para especificar cantidad exacta de producto (ej: 100 unidades)
    DialogoCantidadProducto(
        mostrar = productoParaCantidad != null,
        producto = productoParaCantidad,
        cantidadInicial = cantidadInicialDialogo,
        alConfirmar = { prod, nuevaCantidad ->
            viewModel.agregarOActualizarCantidad(prod, nuevaCantidad)
            productoParaCantidad = null
        },
        alDescartar = {
            productoParaCantidad = null
        }
    )

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

    val contexto = LocalContext.current

    // Modal de Venta Exitosa
    DialogoVentaExitosa(
        venta = ultimaVenta,
        alCerrar = { viewModel.cerrarModalVentaExitosa() },
        alImprimir = {
            viewModel.imprimirTicketUltimaVenta { resultado ->
                // Feedback de impresión
            }
        },
        alCompartirPdf = {
            viewModel.compartirFacturaPdf(contexto)
        },
        alVerPdf = {
            viewModel.verFacturaPdf(contexto)
        }
    )

    // Modal Desplegable del Carrito de Compras
    if (mostrarModalCarrito) {
        ModalBottomSheet(
            onDismissRequest = { mostrarModalCarrito = false },
            containerColor = MaterialTheme.colorScheme.surface,
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
                alEspecificarCantidad = { prod, cant ->
                    cantidadInicialDialogo = cant
                    productoParaCantidad = prod
                },
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
                        text = "Facturación",
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
            // Barra inferior flotante de Carrito / Cobro estilo Amazon
            if (carrito.isNotEmpty()) {
                BarraInferiorCarrito(
                    cantidadArticulos = cantidadArticulos,
                    total = totalVenta,
                    alVerCarrito = { mostrarModalCarrito = true },
                    alCobrar = { mostrarModalCobro = true }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValores ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValores)
        ) {
            // Buscador con Escáner y Categorías estilo Amazon
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { viewModel.onBusquedaCambiada(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar en catálogo o código...", fontSize = 14.sp) },
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
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        focusedBorderColor = AzulPrimario,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                        cursorColor = AzulPrimario
                    )
                )

                // Filtro horizontal de Categorías (Pills)
                if (categoriasDisponibles.size > 1) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categoriasDisponibles) { cat ->
                            val seleccionada = cat == categoriaSeleccionada
                            FilterChip(
                                selected = seleccionada,
                                onClick = { viewModel.onCategoriaSeleccionada(cat) },
                                label = { Text(cat, fontSize = 12.5.sp, fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AzulPrimario,
                                    selectedLabelColor = Blanco,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }

            // Barra de resultados y conmutador de vista (Cuadrícula Amazon / Lista)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val totalPags = estadoPaginacion.calcularTotalPaginas(productos.size)
                Text(
                    text = "${productos.size} resultados" + if (totalPags > 1) " • Pág. ${estadoPaginacion.paginaActual}/$totalPags" else "",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { modoVistaCuadricula = true },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GridView,
                            contentDescription = "Vista Cuadrícula",
                            tint = if (modoVistaCuadricula) AzulPrimario else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { modoVistaCuadricula = false },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = "Vista Lista",
                            tint = if (!modoVistaCuadricula) AzulPrimario else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Contenedor principal de productos
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No hay productos disponibles",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else if (modoVistaCuadricula) {
                // VISTA CUADRÍCULA ESTILO AMAZON (2 Columnas)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = productosPaginados,
                        key = { it.id }
                    ) { producto ->
                        val itemEnCarrito = carrito.find { it.producto.id == producto.id }
                        TarjetaProductoAmazonGrid(
                            producto = producto,
                            cantidadEnCarrito = itemEnCarrito?.cantidad ?: 0.0,
                            alAgregar = { viewModel.agregarAlCarrito(producto) },
                            alDecrementar = { viewModel.decrementarCantidad(producto.id) },
                            alEspecificarCantidad = {
                                cantidadInicialDialogo = itemEnCarrito?.cantidad ?: 1.0
                                productoParaCantidad = producto
                            }
                        )
                    }

                    // Paginador al final de la cuadrícula
                    item(span = { GridItemSpan(2) }) {
                        ControlPaginacionPos(
                            estado = estadoPaginacion,
                            totalElementos = productos.size,
                            mostrarSelectorTamanio = true,
                            opcionesTamanio = listOf(12, 16, 24, 48)
                        )
                    }

                    item(span = { GridItemSpan(2) }) {
                        Spacer(modifier = Modifier.height(if (carrito.isNotEmpty()) 100.dp else 20.dp))
                    }
                }
            } else {
                // VISTA LISTA HORIZONTAL ESTILO AMAZON
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = productosPaginados,
                        key = { it.id }
                    ) { producto ->
                        val itemEnCarrito = carrito.find { it.producto.id == producto.id }
                        TarjetaProductoAmazonLista(
                            producto = producto,
                            cantidadEnCarrito = itemEnCarrito?.cantidad ?: 0.0,
                            alAgregar = { viewModel.agregarAlCarrito(producto) },
                            alDecrementar = { viewModel.decrementarCantidad(producto.id) },
                            alEspecificarCantidad = {
                                cantidadInicialDialogo = itemEnCarrito?.cantidad ?: 1.0
                                productoParaCantidad = producto
                            }
                        )
                    }

                    // Paginador al final de la lista
                    item {
                        ControlPaginacionPos(
                            estado = estadoPaginacion,
                            totalElementos = productos.size,
                            mostrarSelectorTamanio = true,
                            opcionesTamanio = listOf(10, 20, 50)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(if (carrito.isNotEmpty()) 100.dp else 20.dp))
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta de producto para Cuadrícula (2 columnas) con estética de compras tipo Amazon:
 * - Fotografía grande superior con badges de stock o servicio.
 * - Categoría, título (2 líneas) y precio prominente con desglose de moneda.
 * - Stepper interactivo directamente en la tarjeta (- [cant] +) o botón grande de añadir.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TarjetaProductoAmazonGrid(
    producto: ProductoEntidad,
    cantidadEnCarrito: Double,
    alAgregar: () -> Unit,
    alDecrementar: () -> Unit,
    alEspecificarCantidad: () -> Unit
) {
    val estaAgotado = producto.tipoItem == "Producto" && producto.controlaStock && producto.stock <= 0
    val limiteStockAlcanzado = producto.tipoItem == "Producto" && producto.controlaStock && cantidadEnCarrito >= producto.stock

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = { if (!estaAgotado) alAgregar() },
                onLongClick = { if (!estaAgotado) alEspecificarCantidad() }
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Contenedor visual superior con foto y badges superpuestos
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
            ) {
                ImagenProductoTarjeta(
                    fotoUri = producto.imagenPrincipal,
                    nombre = producto.nombre,
                    alto = 115.dp,
                    forma = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
                )

                // Badge de Stock / Estado en la esquina superior izquierda
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    if (producto.tipoItem == "Servicio") {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CelesteInformacion,
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = "Servicio",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Blanco,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (producto.controlaStock) {
                        when {
                            producto.stock <= 0 -> {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = RojoError,
                                    shadowElevation = 2.dp
                                ) {
                                    Text(
                                        text = "Agotado",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Blanco,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            producto.stock <= 5 -> {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AmarilloAdvertencia,
                                    shadowElevation = 2.dp
                                ) {
                                    Text(
                                        text = "¡Solo ${producto.stock.toInt()}!",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Blanco,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            else -> {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = VerdeExito.copy(alpha = 0.9f),
                                    shadowElevation = 1.dp
                                ) {
                                    Text(
                                        text = "Stock: ${producto.stock.toInt()}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Blanco,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Badge si ya está en carrito (esquina superior derecha)
                if (cantidadEnCarrito > 0) {
                    val textoCant = if (cantidadEnCarrito % 1.0 == 0.0) "${cantidadEnCarrito.toInt()}" else "$cantidadEnCarrito"
                    Surface(
                        shape = CircleShape,
                        color = AzulPrimario,
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = textoCant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Blanco
                            )
                        }
                    }
                }
            }

            // Cuerpo informativo de la tarjeta
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                // Categoría
                Text(
                    text = producto.categoria.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GrisSecundario,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Nombre del producto (hasta 2 líneas)
                Text(
                    text = producto.nombre,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    minLines = 2,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Precio estilo Amazon ($ grande y decimales compactos)
                val partesPrecio = String.format(java.util.Locale.US, "%.2f", producto.precioVenta).split(".")
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "$",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AzulPrimario,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Text(
                        text = partesPrecio.getOrNull(0) ?: "0",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AzulPrimario
                    )
                    Text(
                        text = ".${partesPrecio.getOrNull(1) ?: "00"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AzulPrimario,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    if (producto.exentoItbis) {
                        Text(
                            text = "Exento",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CelesteInformacion,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    } else if (!producto.itbisIncluido) {
                        Text(
                            text = "+ ITBIS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmarilloAdvertencia,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Control inferior estilo Amazon: Stepper o Botón Añadir
                if (estaAgotado) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Block,
                                contentDescription = null,
                                tint = RojoError,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Agotado",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = RojoError
                            )
                        }
                    }
                } else if (cantidadEnCarrito > 0) {
                    // Stepper interactivo directamente en la tarjeta Amazon (- [cant] +)
                    val textoCant = if (cantidadEnCarrito % 1.0 == 0.0) "${cantidadEnCarrito.toInt()}" else "$cantidadEnCarrito"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AzulPrimarioClaro.copy(alpha = 0.35f))
                            .border(1.dp, AzulPrimario.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = alDecrementar,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (cantidadEnCarrito == 1.0) Icons.Filled.Delete else Icons.Filled.Remove,
                                contentDescription = "Quitar 1",
                                tint = if (cantidadEnCarrito == 1.0) RojoError else AzulPrimario,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = textoCant,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AzulPrimario,
                            modifier = Modifier
                                .clickable(onClick = alEspecificarCantidad)
                                .padding(horizontal = 4.dp)
                        )

                        IconButton(
                            onClick = alAgregar,
                            enabled = !limiteStockAlcanzado,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Añadir 1",
                                tint = if (!limiteStockAlcanzado) AzulPrimario else GrisMedio,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    // Botón directo "Añadir" estilo Amazon
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AzulPrimario,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .clickable(onClick = alAgregar)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddShoppingCart,
                                contentDescription = null,
                                tint = Blanco,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Añadir",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Blanco
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta de producto para Lista Horizontal con estética tipo Amazon:
 * - Miniatura a la izquierda (74x74.dp).
 * - Columna central con título (2 líneas), badges y precio destacado.
 * - Columna derecha con stepper o botón de añadir.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TarjetaProductoAmazonLista(
    producto: ProductoEntidad,
    cantidadEnCarrito: Double,
    alAgregar: () -> Unit,
    alDecrementar: () -> Unit,
    alEspecificarCantidad: () -> Unit
) {
    val estaAgotado = producto.tipoItem == "Producto" && producto.controlaStock && producto.stock <= 0
    val limiteStockAlcanzado = producto.tipoItem == "Producto" && producto.controlaStock && cantidadEnCarrito >= producto.stock

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = { if (!estaAgotado) alAgregar() },
                onLongClick = { if (!estaAgotado) alEspecificarCantidad() }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Miniatura destacada a la izquierda
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                MiniaturaProducto(
                    fotoUri = producto.imagenPrincipal,
                    nombre = producto.nombre,
                    tamano = 74.dp,
                    forma = RoundedCornerShape(10.dp)
                )

                if (cantidadEnCarrito > 0) {
                    val textoCant = if (cantidadEnCarrito % 1.0 == 0.0) "${cantidadEnCarrito.toInt()}" else "$cantidadEnCarrito"
                    Surface(
                        shape = CircleShape,
                        color = AzulPrimario,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(3.dp)
                            .size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = textoCant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Blanco
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Información central
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = producto.categoria.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GrisSecundario,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = producto.nombre,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Precio Amazon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "$${String.format(java.util.Locale.US, "%.2f", producto.precioVenta)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AzulPrimario
                    )

                    if (producto.tipoItem == "Servicio") {
                        Text(
                            text = "Servicio",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CelesteInformacion
                        )
                    } else if (producto.controlaStock) {
                        val colorStock = if (producto.stock <= 0) RojoError else if (producto.stock <= 5) AmarilloAdvertencia else VerdeExito
                        Text(
                            text = if (producto.stock <= 0) "Agotado" else "Stock: ${producto.stock.toInt()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorStock
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Control de acción a la derecha
            if (estaAgotado) {
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
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else if (cantidadEnCarrito > 0) {
                val textoCant = if (cantidadEnCarrito % 1.0 == 0.0) "${cantidadEnCarrito.toInt()}" else "$cantidadEnCarrito"
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AzulPrimarioClaro.copy(alpha = 0.35f))
                        .border(1.dp, AzulPrimario.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = alDecrementar,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (cantidadEnCarrito == 1.0) Icons.Filled.Delete else Icons.Filled.Remove,
                            contentDescription = "Quitar",
                            tint = if (cantidadEnCarrito == 1.0) RojoError else AzulPrimario,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Text(
                        text = textoCant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AzulPrimario,
                        modifier = Modifier
                            .clickable(onClick = alEspecificarCantidad)
                            .padding(horizontal = 4.dp)
                    )

                    IconButton(
                        onClick = alAgregar,
                        enabled = !limiteStockAlcanzado,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Añadir",
                            tint = if (!limiteStockAlcanzado) AzulPrimario else GrisMedio,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AzulPrimario,
                    modifier = Modifier
                        .height(34.dp)
                        .clickable(onClick = alAgregar)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddShoppingCart,
                            contentDescription = null,
                            tint = Blanco,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Añadir",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Blanco
                        )
                    }
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
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
                // Resumen de Carrito
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$${String.format(java.util.Locale.US, "%.2f", total)}",
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
    alEspecificarCantidad: (ProductoEntidad, Double) -> Unit,
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
                color = MaterialTheme.colorScheme.onSurface
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { alEspecificarCantidad(item.producto, item.cantidad) }
                        ) {
                            Text(
                                text = item.producto.nombre,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = "$${String.format(java.util.Locale.US, "%.2f", item.precioUnitario)} c/u  |  Total: $${String.format(java.util.Locale.US, "%.2f", item.total)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Controles de cantidad (- / [cant (editable)] / +)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable { alDecrementar(item.producto.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (item.cantidad == 1.0) Icons.Filled.Delete else Icons.Filled.Remove,
                                    contentDescription = "Disminuir",
                                    tint = if (item.cantidad == 1.0) RojoError else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Botón/Caja de cantidad con indicador visual de edición
                            val textoCant = if (item.cantidad % 1.0 == 0.0) "${item.cantidad.toInt()}" else "${item.cantidad}"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, AzulPrimario.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .clickable { alEspecificarCantidad(item.producto, item.cantidad) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = textoCant,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = AzulPrimario
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Modificar cantidad",
                                        tint = AzulPrimario.copy(alpha = 0.7f),
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }

                            val limiteAlcanzado = item.producto.controlaStock && item.cantidad >= item.producto.stock
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (limiteAlcanzado) MaterialTheme.colorScheme.outlineVariant else AzulPrimario)
                                    .clickable { alIncrementar(item.producto.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Aumentar",
                                    tint = if (limiteAlcanzado) MaterialTheme.colorScheme.onSurfaceVariant else Blanco,
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
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("$${String.format(java.util.Locale.US, "%.2f", subtotal)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("ITBIS (18%):", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("$${String.format(java.util.Locale.US, "%.2f", itbis)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TOTAL:", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AzulPrimario)
                    Text("$${String.format(java.util.Locale.US, "%.2f", total)}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = AzulPrimario)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        BotonPos(
            texto = "Proceder al Cobro ($${String.format(java.util.Locale.US, "%.2f", total)})",
            alHacerClic = alProcederCobro,
            variante = VarianteBoton.EXITO,
            icono = Icons.Filled.Payments
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
