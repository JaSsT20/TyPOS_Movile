package com.typdevstudio.typos_movil.ui.productos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.typdevstudio.typos_movil.datos.local.entidades.ProductoEntidad
import com.typdevstudio.typos_movil.ui.componentes.DialogoEscanerCodigoBarras
import com.typdevstudio.typos_movil.ui.theme.AmarilloAdvertencia
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.AzulPrimarioClaro
import com.typdevstudio.typos_movil.ui.theme.Blanco
import com.typdevstudio.typos_movil.ui.theme.CelesteInformacion
import com.typdevstudio.typos_movil.ui.theme.GrisMedio
import com.typdevstudio.typos_movil.ui.theme.GrisSecundario
import com.typdevstudio.typos_movil.ui.theme.RojoError
import com.typdevstudio.typos_movil.ui.theme.VerdeExito

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaProductos(
    viewModel: ProductoViewModel,
    alCrearProducto: () -> Unit,
    alEditarProducto: (ProductoEntidad) -> Unit,
    alVolver: () -> Unit
) {
    val productos by viewModel.productos.collectAsState()
    val busqueda by viewModel.busqueda.collectAsState()
    val categoriasDisponibles by viewModel.categoriasDisponibles.collectAsState()
    val categoriaSeleccionada by viewModel.categoriaSeleccionada.collectAsState()
    val filtroStock by viewModel.filtroStock.collectAsState()

    var productoAEliminar by remember { mutableStateOf<ProductoEntidad?>(null) }
    var mostrarEscanerBusqueda by remember { mutableStateOf(false) }

    // Escáner de código de barras para búsqueda rápida
    DialogoEscanerCodigoBarras(
        mostrar = mostrarEscanerBusqueda,
        alDetectarCodigo = { codigo ->
            viewModel.onBusquedaCambiada(codigo)
            mostrarEscanerBusqueda = false
        },
        alCerrar = { mostrarEscanerBusqueda = false }
    )

    if (productoAEliminar != null) {
        AlertDialog(
            onDismissRequest = { productoAEliminar = null },
            title = { Text("Eliminar Producto", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de que deseas eliminar permanentemente \"${productoAEliminar?.nombre}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        productoAEliminar?.let { viewModel.eliminarProducto(it.id) }
                        productoAEliminar = null
                    }
                ) {
                    Text("Eliminar", color = RojoError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { productoAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Catálogo de Productos",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AzulPrimario)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.limpiarFormulario()
                    alCrearProducto()
                },
                containerColor = AzulPrimario,
                contentColor = Blanco,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Nuevo producto",
                    modifier = Modifier.size(28.dp)
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
            // Contenedor superior con búsqueda y filtros
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Buscador moderno con acceso al escáner de cámara
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { viewModel.onBusquedaCambiada(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar por nombre o código...", fontSize = 14.sp) },
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
                                        contentDescription = "Limpiar búsqueda",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { mostrarEscanerBusqueda = true }) {
                                Icon(
                                    imageVector = Icons.Filled.QrCodeScanner,
                                    contentDescription = "Escanear para buscar",
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

                // Filtros de Categorías (Chips con scroll horizontal)
                if (categoriasDisponibles.size > 1) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categoriasDisponibles) { categoria ->
                            val estaSeleccionada = categoria == categoriaSeleccionada
                            FilterChip(
                                selected = estaSeleccionada,
                                onClick = { viewModel.onCategoriaFiltroSeleccionada(categoria) },
                                label = {
                                    Text(
                                        text = categoria,
                                        fontSize = 13.sp,
                                        fontWeight = if (estaSeleccionada) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AzulPrimario,
                                    selectedLabelColor = Blanco,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = estaSeleccionada,
                                    borderColor = if (estaSeleccionada) AzulPrimario else MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }
                    }
                }

                // Filtros de Estado de Stock
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FiltroStock.entries.forEach { filtro ->
                        val seleccionado = filtro == filtroStock
                        FilterChip(
                            selected = seleccionado,
                            onClick = { viewModel.onFiltroStockSeleccionado(filtro) },
                            label = {
                                Text(
                                    text = filtro.etiqueta,
                                    fontSize = 12.sp,
                                    fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Blanco,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = seleccionado,
                                borderColor = if (seleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }
                }
            }

            // Indicador de resultados
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mostrando ${productos.size} productos",
                    fontSize = 13.sp,
                    color = GrisSecundario,
                    fontWeight = FontWeight.Medium
                )
            }

            // Lista de productos
            if (productos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Inventory2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No se encontraron productos",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Prueba cambiando los filtros o agrega un nuevo producto",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = productos,
                        key = { it.id }
                    ) { producto ->
                        TarjetaProductoModerna(
                            producto = producto,
                            alEditar = {
                                viewModel.cargarParaEditar(producto)
                                alEditarProducto(producto)
                            },
                            alEliminar = { productoAEliminar = producto }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TarjetaProductoModerna(
    producto: ProductoEntidad,
    alEditar: () -> Unit,
    alEliminar: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono / Avatar con la inicial del producto
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(AzulPrimarioClaro.copy(alpha = 0.35f), shape = RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = producto.nombre.take(1).uppercase(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AzulPrimario
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Información central con margen para no invadir botones
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp)
            ) {
                Text(
                    text = producto.nombre,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )

                if (!producto.codigoBarras.isNullOrBlank()) {
                    Text(
                        text = "SKU: ${producto.codigoBarras}",
                        fontSize = 12.sp,
                        color = GrisMedio
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Precio de venta
                    Text(
                        text = "$${String.format("%.2f", producto.precioVenta)}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AzulPrimario
                    )

                    // Badge de ITBIS
                    val textoItbis = when {
                        producto.exentoItbis -> "Exento"
                        producto.itbisIncluido -> "ITBIS inc."
                        else -> "+18% ITBIS"
                    }
                    Box(
                        modifier = Modifier
                            .background(AzulPrimarioClaro.copy(alpha = 0.25f), shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = textoItbis,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AzulPrimario
                        )
                    }

                    // Badge de estado de stock
                    if (producto.controlaStock) {
                        val (colorFondo, colorTexto, textoStock) = when {
                            producto.stock <= 0 -> Triple(RojoError.copy(alpha = 0.12f), RojoError, "Agotado")
                            producto.stock <= 5 -> Triple(AmarilloAdvertencia.copy(alpha = 0.15f), AmarilloAdvertencia, "Stock: ${producto.stock.toInt()}")
                            else -> Triple(VerdeExito.copy(alpha = 0.12f), VerdeExito, "Stock: ${producto.stock.toInt()}")
                        }
                        Box(
                            modifier = Modifier
                                .background(colorFondo, shape = RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = textoStock,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorTexto
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .background(CelesteInformacion.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Servicio",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CelesteInformacion
                            )
                        }
                    }
                }
            }

            // Botones de acción estilizados (sin solapamiento en pantallas pequeñas)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AzulPrimarioClaro.copy(alpha = 0.35f))
                        .clickable(onClick = alEditar),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Editar",
                        tint = AzulPrimario,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(RojoError.copy(alpha = 0.12f))
                        .clickable(onClick = alEliminar),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Eliminar",
                        tint = RojoError,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
