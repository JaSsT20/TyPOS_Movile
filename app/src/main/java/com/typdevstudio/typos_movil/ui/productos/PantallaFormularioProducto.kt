package com.typdevstudio.typos_movil.ui.productos

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.typdevstudio.typos_movil.ui.componentes.BotonPos
import com.typdevstudio.typos_movil.ui.componentes.CampoTextoPos
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
import com.typdevstudio.typos_movil.utilidades.GestorImagenesProducto
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaFormularioProducto(
    viewModel: ProductoViewModel,
    alVolver: () -> Unit
) {
    val contexto = LocalContext.current
    val estado by viewModel.formularioState.collectAsState()

    var mostrarEscaner by remember { mutableStateOf(false) }
    var mostrarModalOrigenFoto by remember { mutableStateOf(false) }

    var archivoCamaraTemporal by remember { mutableStateOf<File?>(null) }
    var uriCamaraTemporal by remember { mutableStateOf<Uri?>(null) }

    // Launcher para tomar foto en vivo con la cámara
    val launcherCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { exito ->
        if (exito && archivoCamaraTemporal != null) {
            viewModel.onImagenesAgregadas(listOf(archivoCamaraTemporal!!.absolutePath))
        } else {
            archivoCamaraTemporal?.let { GestorImagenesProducto.eliminarImagen(it.absolutePath) }
        }
        archivoCamaraTemporal = null
        uriCamaraTemporal = null
    }

    // Launcher para seleccionar imágenes de la galería o explorador de archivos
    val launcherGaleria = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val rutasGuardadas = uris.mapNotNull { uri ->
                GestorImagenesProducto.guardarImagenProductoInterna(contexto, uri)
            }
            if (rutasGuardadas.isNotEmpty()) {
                viewModel.onImagenesAgregadas(rutasGuardadas)
            }
        }
    }

    LaunchedEffect(estado.guardadoExitoso) {
        if (estado.guardadoExitoso) {
            viewModel.limpiarFormulario()
            alVolver()
        }
    }

    // Modal de Escaneo con Cámara (Código de Barras)
    DialogoEscanerCodigoBarras(
        mostrar = mostrarEscaner,
        alDetectarCodigo = { codigoEscaneado ->
            viewModel.onCodigoBarrasCambiado(codigoEscaneado)
            mostrarEscaner = false
        },
        alCerrar = { mostrarEscaner = false }
    )

    // Modal Desplegable para Elegir Origen de la Foto (Cámara o Galería)
    if (mostrarModalOrigenFoto) {
        ModalBottomSheet(
            onDismissRequest = { mostrarModalOrigenFoto = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Añadir Foto del Producto",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Elige cómo deseas agregar la imagen (hasta 3 fotos por producto):",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Opción 1: Tomar Foto con la Cámara
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            mostrarModalOrigenFoto = false
                            val par = GestorImagenesProducto.crearArchivoTemporalParaCamara(contexto)
                            if (par != null) {
                                archivoCamaraTemporal = par.first
                                uriCamaraTemporal = par.second
                                launcherCamara.launch(par.second)
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(AzulPrimario, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = null,
                                tint = Blanco,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Tomar foto con la cámara",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Captura el producto en tiempo real",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Opción 2: Elegir de la Galería o Archivos
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            mostrarModalOrigenFoto = false
                            launcherGaleria.launch("image/*")
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(AzulPrimarioClaro.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoLibrary,
                                contentDescription = null,
                                tint = AzulPrimario,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Elegir de la galería o archivos",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Selecciona una o más fotos guardadas",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (estado.id == 0L) "Nuevo Producto" else "Editar Producto",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Blanco
                        )
                        Text(
                            text = if (estado.id == 0L) "Completa los datos del artículo" else "Modifica los detalles del producto",
                            fontSize = 12.sp,
                            color = Blanco.copy(alpha = 0.8f)
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
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValores ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValores)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // SECCIÓN 1: FOTOGRAFÍAS DEL PRODUCTO (MÁX 3)
            // ==========================================
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(AzulPrimarioClaro.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PhotoLibrary,
                                    contentDescription = null,
                                    tint = AzulPrimario,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Fotos del Producto",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Badge indicador de cantidad
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (estado.imagenes.isNotEmpty()) AzulPrimario.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "${estado.imagenes.size} / 3 fotos",
                                color = if (estado.imagenes.isNotEmpty()) AzulPrimario else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Añade hasta 3 fotos (con la cámara o galería). La primera será la foto principal en el catálogo y la caja.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Carrusel / Fila de Ranuras de Fotos
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Imágenes seleccionadas
                        estado.imagenes.forEachIndexed { index, rutaFoto ->
                            ItemRanuraImagen(
                                rutaFoto = rutaFoto,
                                esPrincipal = index == 0,
                                alEliminar = { viewModel.onEliminarImagen(index) },
                                alHacerPrincipal = { viewModel.onMoverImagenAPrincipal(index) }
                            )
                        }

                        // Botón para añadir foto si hay menos de 3
                        if (estado.imagenes.size < 3) {
                            BotonAgregarFotoRanura(
                                cantidadActual = estado.imagenes.size,
                                alHacerClic = { mostrarModalOrigenFoto = true }
                            )
                        }
                    }
                }
            }

            // ==========================================
            // SECCIÓN 2: INFORMACIÓN BÁSICA DEL PRODUCTO
            // ==========================================
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(AzulPrimarioClaro.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ShoppingBag,
                                contentDescription = null,
                                tint = AzulPrimario,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Información Básica",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Campo Código de Barras con Botón de Escáner Integrado
                    OutlinedTextField(
                        value = estado.codigoBarras,
                        onValueChange = { viewModel.onCodigoBarrasCambiado(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Código de barras / SKU (Opcional)") },
                        placeholder = { Text("Ej: 7501030420102") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AzulPrimario.copy(alpha = 0.1f),
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .clickable { mostrarEscaner = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.QrCodeScanner,
                                        contentDescription = "Escanear",
                                        tint = AzulPrimario,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Escanear",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AzulPrimario
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = AzulPrimario,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedLabelColor = AzulPrimario,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = AzulPrimario
                        )
                    )

                    // Nombre del Producto
                    CampoTextoPos(
                        valor = estado.nombre,
                        alCambiarValor = { viewModel.onNombreCambiado(it) },
                        etiqueta = "Nombre del producto *",
                        iconoInicio = Icons.Filled.ShoppingBag,
                        mensajeError = estado.errorNombre,
                        opcionesTeclado = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        )
                    )

                    // Categoría
                    CampoTextoPos(
                        valor = estado.categoria,
                        alCambiarValor = { viewModel.onCategoriaCambiada(it) },
                        etiqueta = "Categoría",
                        iconoInicio = Icons.Filled.Category,
                        opcionesTeclado = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        )
                    )

                    // Descripción
                    CampoTextoPos(
                        valor = estado.descripcion,
                        alCambiarValor = { viewModel.onDescripcionCambiada(it) },
                        etiqueta = "Descripción (Opcional)",
                        iconoInicio = Icons.Filled.Description,
                        opcionesTeclado = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        )
                    )
                }
            }

            // ==========================================
            // SECCIÓN 3: PRECIOS Y RENTABILIDAD
            // ==========================================
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(AzulPrimarioClaro.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AttachMoney,
                                contentDescription = null,
                                tint = AzulPrimario,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Precios y Rentabilidad",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Precio de Costo
                        CampoTextoPos(
                            valor = estado.precioCosto,
                            alCambiarValor = { viewModel.onPrecioCostoCambiado(it) },
                            etiqueta = "Precio Costo",
                            iconoInicio = Icons.Filled.AttachMoney,
                            modifier = Modifier.weight(1f),
                            opcionesTeclado = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            )
                        )

                        // Precio de Venta
                        CampoTextoPos(
                            valor = estado.precioVenta,
                            alCambiarValor = { viewModel.onPrecioVentaCambiado(it) },
                            etiqueta = "Precio Venta *",
                            iconoInicio = Icons.Filled.AttachMoney,
                            mensajeError = estado.errorPrecioVenta,
                            modifier = Modifier.weight(1f),
                            opcionesTeclado = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            )
                        )
                    }

                    // Calculadora de margen y ganancia en tiempo real
                    val costoNum = estado.precioCosto.toDoubleOrNull() ?: 0.0
                    val ventaNum = estado.precioVenta.toDoubleOrNull() ?: 0.0
                    if (costoNum > 0.0 && ventaNum > 0.0) {
                        val ganancia = ventaNum - costoNum
                        val margenPorcentaje = (ganancia / costoNum) * 100

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (ganancia >= 0) VerdeExito.copy(alpha = 0.12f) else RojoError.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (ganancia >= 0) VerdeExito.copy(alpha = 0.35f) else RojoError.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = if (ganancia >= 0) VerdeExito else RojoError,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Margen Bruto:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${if (margenPorcentaje >= 0) "+" else ""}${String.format("%.1f", margenPorcentaje)}%",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (ganancia >= 0) VerdeExito else RojoError
                                    )
                                }

                                Text(
                                    text = "Ganancia: $${String.format("%.2f", ganancia)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (ganancia >= 0) VerdeExito else RojoError
                                )
                            }
                        }
                    }

                    // Interruptor: ¿El precio de venta ya incluye ITBIS?
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (estado.itbisIncluido && !estado.exentoItbis) AzulPrimarioClaro.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = 1.dp,
                                color = if (estado.itbisIncluido && !estado.exentoItbis) AzulPrimario.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                if (!estado.exentoItbis) {
                                    viewModel.onItbisIncluidoCambiado(!estado.itbisIncluido)
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "Precio con ITBIS incluido (18%)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (estado.itbisIncluido && !estado.exentoItbis) AzulPrimario else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (estado.exentoItbis) {
                                        "No aplica (Producto configurado como exento de impuestos)"
                                    } else if (estado.itbisIncluido) {
                                        "El cliente paga exactamente el precio fijado. El 18% se desglosa del monto"
                                    } else {
                                        "El 18% de ITBIS se sumará adicionalmente al cobrar"
                                    },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = estado.itbisIncluido && !estado.exentoItbis,
                                onCheckedChange = { viewModel.onItbisIncluidoCambiado(it) },
                                enabled = !estado.exentoItbis,
                                thumbContent = {
                                    Icon(
                                        imageVector = if (estado.itbisIncluido && !estado.exentoItbis) Icons.Filled.Check else Icons.Filled.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (estado.itbisIncluido && !estado.exentoItbis) AzulPrimario else GrisMedio
                                    )
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Blanco,
                                    checkedTrackColor = AzulPrimario,
                                    uncheckedThumbColor = Blanco,
                                    uncheckedTrackColor = GrisMedio.copy(alpha = 0.5f),
                                    uncheckedBorderColor = GrisMedio
                                )
                            )
                        }
                    }

                    // Interruptor: Producto Exento de ITBIS (0%)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (estado.exentoItbis) AzulPrimarioClaro.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = 1.dp,
                                color = if (estado.exentoItbis) AzulPrimario.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.onExentoItbisCambiado(!estado.exentoItbis) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "Producto Exento de ITBIS (0%)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (estado.exentoItbis) AzulPrimario else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Activar para alimentos básicos o medicamentos sin impuesto",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = estado.exentoItbis,
                                onCheckedChange = { viewModel.onExentoItbisCambiado(it) },
                                thumbContent = {
                                    Icon(
                                        imageVector = if (estado.exentoItbis) Icons.Filled.Check else Icons.Filled.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (estado.exentoItbis) AzulPrimario else GrisMedio
                                    )
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Blanco,
                                    checkedTrackColor = AzulPrimario,
                                    uncheckedThumbColor = Blanco,
                                    uncheckedTrackColor = GrisMedio.copy(alpha = 0.5f),
                                    uncheckedBorderColor = GrisMedio
                                )
                            )
                        }
                    }

                    // Desglose en Tiempo Real del Precio
                    val precioVentaNum = estado.precioVenta.toDoubleOrNull() ?: 0.0
                    if (precioVentaNum > 0.0) {
                        val baseImp = if (estado.exentoItbis) precioVentaNum else if (estado.itbisIncluido) precioVentaNum / 1.18 else precioVentaNum
                        val itbisMonto = if (estado.exentoItbis) 0.0 else if (estado.itbisIncluido) precioVentaNum - baseImp else precioVentaNum * 0.18
                        val totalCobro = if (estado.exentoItbis || estado.itbisIncluido) precioVentaNum else precioVentaNum + itbisMonto

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AzulPrimarioClaro.copy(alpha = 0.15f))
                                .border(1.dp, AzulPrimario.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Desglose Fiscal Estimado por Unidad:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AzulPrimario
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Base: $${String.format("%.2f", baseImp)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text(text = "ITBIS: $${String.format("%.2f", itbisMonto)}", fontSize = 12.sp, color = if (itbisMonto > 0) AzulPrimario else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "Total Venta: $${String.format("%.2f", totalCobro)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AzulPrimario)
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // SECCIÓN 4: CONTROL DE INVENTARIO
            // ==========================================
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(AzulPrimarioClaro.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Inventory2,
                                contentDescription = null,
                                tint = AzulPrimario,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Control de Inventario",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (estado.controlaStock) AzulPrimarioClaro.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = 1.dp,
                                color = if (estado.controlaStock) AzulPrimario.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.onControlaStockCambiado(!estado.controlaStock) }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Controlar inventario (Stock)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (estado.controlaStock) AzulPrimario else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (estado.controlaStock) "Descuenta unidades automáticamente al vender" else "Desactivado (Producto o Servicio sin límite)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = estado.controlaStock,
                                onCheckedChange = { viewModel.onControlaStockCambiado(it) },
                                thumbContent = {
                                    Icon(
                                        imageVector = if (estado.controlaStock) Icons.Filled.Check else Icons.Filled.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (estado.controlaStock) AzulPrimario else GrisMedio
                                    )
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Blanco,
                                    checkedTrackColor = AzulPrimario,
                                    uncheckedThumbColor = Blanco,
                                    uncheckedTrackColor = GrisMedio.copy(alpha = 0.5f),
                                    uncheckedBorderColor = GrisMedio
                                )
                            )
                        }
                    }

                    if (estado.controlaStock) {
                        CampoTextoPos(
                            valor = estado.stock,
                            alCambiarValor = { viewModel.onStockCambiado(it) },
                            etiqueta = "Cantidad actual en inventario",
                            iconoInicio = Icons.Filled.Inventory2,
                            opcionesTeclado = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            )
                        )
                    }
                }
            }

            if (estado.mensajeErrorGeneral != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RojoError.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = RojoError)
                        Text(
                            text = estado.mensajeErrorGeneral!!,
                            color = RojoError,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Botón Guardar
            BotonPos(
                texto = if (estado.id == 0L) "Registrar Producto" else "Actualizar Producto",
                alHacerClic = { viewModel.guardarProducto() },
                estaCargando = estado.estaGuardando,
                icono = Icons.Filled.Save
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * Visualiza una foto cargada con opciones de eliminar y marcar como principal.
 */
@Composable
private fun ItemRanuraImagen(
    rutaFoto: String,
    esPrincipal: Boolean,
    alEliminar: () -> Unit,
    alHacerPrincipal: () -> Unit
) {
    val bitmap = remember(rutaFoto) {
        GestorImagenesProducto.decodificarMuestreado(rutaFoto, 220, 220)?.asImageBitmap()
    }

    Box(
        modifier = Modifier
            .size(105.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (esPrincipal) 2.dp else 1.dp,
                color = if (esPrincipal) AzulPrimario else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(14.dp)
            )
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Overlay y Badge Principal / Botón Hacer Principal
        if (esPrincipal) {
            Surface(
                shape = RoundedCornerShape(bottomEnd = 8.dp, topStart = 12.dp),
                color = AzulPrimario,
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Blanco,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = "Principal",
                        color = Blanco,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // Botón para convertir en principal
            Surface(
                shape = RoundedCornerShape(topStart = 8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .clickable(onClick = alHacerPrincipal)
            ) {
                Text(
                    text = "Hacer 1ª",
                    color = AzulPrimario,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Botón Eliminar en la esquina superior derecha
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(onClick = alEliminar),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Eliminar foto",
                tint = Blanco,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Ranura vacía que invita a agregar una nueva imagen al producto.
 */
@Composable
private fun BotonAgregarFotoRanura(
    cantidadActual: Int,
    alHacerClic: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(105.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = 1.5.dp,
                color = AzulPrimario.copy(alpha = 0.4f),
                shape = RoundedCornerShape(14.dp)
            )
            .background(AzulPrimarioClaro.copy(alpha = 0.15f))
            .clickable(onClick = alHacerClic),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(AzulPrimario, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AddPhotoAlternate,
                    contentDescription = "Añadir foto",
                    tint = Blanco,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = "+ Foto ${cantidadActual + 1}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AzulPrimario
            )
        }
    }
}
