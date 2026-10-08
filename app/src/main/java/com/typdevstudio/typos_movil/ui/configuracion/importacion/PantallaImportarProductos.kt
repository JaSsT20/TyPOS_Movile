package com.typdevstudio.typos_movil.ui.configuracion.importacion

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.typdevstudio.typos_movil.ui.componentes.BotonPos
import com.typdevstudio.typos_movil.ui.componentes.VarianteBoton
import com.typdevstudio.typos_movil.ui.theme.AmarilloAdvertencia
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.AzulPrimarioClaro
import com.typdevstudio.typos_movil.ui.theme.Blanco
import com.typdevstudio.typos_movil.ui.theme.CelesteInformacion
import com.typdevstudio.typos_movil.ui.theme.GrisClaro
import com.typdevstudio.typos_movil.ui.theme.GrisMedio
import com.typdevstudio.typos_movil.ui.theme.GrisSecundario
import com.typdevstudio.typos_movil.ui.theme.GrisTexto
import com.typdevstudio.typos_movil.ui.theme.RojoError
import com.typdevstudio.typos_movil.ui.theme.VerdeExito
import com.typdevstudio.typos_movil.utilidades.importacion.ImportacionUiState
import com.typdevstudio.typos_movil.utilidades.importacion.PasoImportacion
import com.typdevstudio.typos_movil.utilidades.importacion.ProductoImportadoUi
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaImportarProductos(
    alVolver: () -> Unit,
    alVerCatalogo: () -> Unit,
    importacionViewModel: ImportacionProductosViewModel = viewModel()
) {
    val estado by importacionViewModel.uiState.collectAsState()
    val contexto = LocalContext.current

    // Selector de archivo del sistema para CSV / Excel / TXT
    val launcherSelectorArchivo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            var nombreArchivo: String? = null
            try {
                contexto.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val indexNombre = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (indexNombre != -1) {
                            nombreArchivo = cursor.getString(indexNombre)
                        }
                    }
                }
            } catch (e: Exception) {
                nombreArchivo = "archivo_productos.csv"
            }
            importacionViewModel.cargarArchivoCsv(it, nombreArchivo ?: "archivo_productos.csv")
        }
    }

    // Modal de edición in-place si hay un producto seleccionado
    estado.productoParaEditar?.let { producto ->
        DialogoEditarProductoImportado(
            producto = producto,
            alGuardar = { importacionViewModel.guardarEdicionProducto(it) },
            alDescartar = { importacionViewModel.cancelarEdicionProducto() }
        )
    }

    // Manejo de botón atrás físico según el paso
    BackHandler {
        when (estado.pasoActual) {
            PasoImportacion.VISTA_PREVIA -> importacionViewModel.reiniciarImportacion()
            PasoImportacion.RESULTADO -> alVolver()
            else -> alVolver()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Importar Productos",
                            fontWeight = FontWeight.Bold,
                            color = Blanco,
                            fontSize = 19.sp
                        )
                        if (estado.pasoActual == PasoImportacion.VISTA_PREVIA && !estado.nombreArchivo.isNullOrBlank()) {
                            Text(
                                text = estado.nombreArchivo!!,
                                color = Blanco.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            when (estado.pasoActual) {
                                PasoImportacion.VISTA_PREVIA -> importacionViewModel.reiniciarImportacion()
                                else -> alVolver()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Blanco
                        )
                    }
                },
                actions = {
                    if (estado.pasoActual == PasoImportacion.VISTA_PREVIA) {
                        IconButton(onClick = { launcherSelectorArchivo.launch("*/*") }) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Cargar otro archivo",
                                tint = Blanco
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AzulPrimario)
            )
        },
        bottomBar = {
            if (estado.pasoActual == PasoImportacion.VISTA_PREVIA) {
                BarraInferiorVistaPrevia(
                    estado = estado,
                    alImportar = { importacionViewModel.ejecutarImportacion() },
                    alAgregarManual = { importacionViewModel.agregarProductoManual() }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValores ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValores)
        ) {
            when (estado.pasoActual) {
                PasoImportacion.SELECCION_ARCHIVO -> {
                    VistaSeleccionArchivo(
                        estado = estado,
                        alSeleccionarArchivo = { launcherSelectorArchivo.launch("*/*") },
                        alCompartirPlantilla = { importacionViewModel.compartirPlantilla() },
                        alDescargarPlantilla = { importacionViewModel.descargarPlantilla() },
                        alAbrirPlantilla = { importacionViewModel.abrirPlantillaDescargada() },
                        alDescartarNotificacionDescarga = { importacionViewModel.descartarNotificacionDescarga() }
                    )
                }
                PasoImportacion.VISTA_PREVIA -> {
                    VistaPreviaImportacion(
                        estado = estado,
                        alFiltroTextoCambiado = { importacionViewModel.onFiltroTextoCambiado(it) },
                        alAlternarSoloErrores = { importacionViewModel.onAlternarMostrarSoloErrores() },
                        alAlternarActualizarExistentes = { importacionViewModel.onAlternarActualizarExistentes() },
                        alEditarProducto = { importacionViewModel.iniciarEdicionProducto(it) },
                        alEliminarProducto = { importacionViewModel.eliminarProductoDeLista(it) }
                    )
                }
                PasoImportacion.PROCESANDO -> {
                    VistaProcesandoImportacion()
                }
                PasoImportacion.RESULTADO -> {
                    VistaResultadoImportacion(
                        estado = estado,
                        alVerCatalogo = alVerCatalogo,
                        alImportarOtro = { importacionViewModel.reiniciarImportacion() }
                    )
                }
            }
        }
    }
}

// =========================================================================
// PASO 1: SELECCIÓN DE ARCHIVO Y DESCARGA DE PLANTILLA
// =========================================================================
@Composable
private fun VistaSeleccionArchivo(
    estado: ImportacionUiState,
    alSeleccionarArchivo: () -> Unit,
    alCompartirPlantilla: () -> Unit,
    alDescargarPlantilla: () -> Unit,
    alAbrirPlantilla: () -> Unit,
    alDescartarNotificacionDescarga: () -> Unit
) {
    var mostrarGuiaColumnas by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner interactivo de descarga exitosa con botón para Abrir o Compartir
        AnimatedVisibility(visible = estado.mostrarNotificacionDescarga) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = VerdeExito.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, VerdeExito.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = VerdeExito,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "¡Plantilla lista para usar!",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = VerdeExito
                            )
                            Text(
                                text = "Guardada en Descargas/TyPOS_PlantillaProductos.csv",
                                fontSize = 11.sp,
                                color = GrisTexto
                            )
                        }
                        IconButton(
                            onClick = alDescartarNotificacionDescarga,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Cerrar",
                                tint = GrisTexto,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BotonPos(
                            texto = "Abrir en Excel / Sheets",
                            icono = Icons.Filled.FileOpen,
                            alHacerClic = alAbrirPlantilla,
                            variante = VarianteBoton.EXITO,
                            modifier = Modifier.weight(1.3f)
                        )

                        BotonPos(
                            texto = "Compartir",
                            icono = Icons.Filled.Share,
                            alHacerClic = alCompartirPlantilla,
                            variante = VarianteBoton.SECUNDARIO,
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                }
            }
        }

        // Banner de error si falló la lectura
        if (estado.mensajeError != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = RojoError.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, RojoError.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = null,
                        tint = RojoError,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = estado.mensajeError,
                        color = RojoError,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 1. Tarjeta Paso 1: Descargar / Compartir Plantilla
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF00ACC1).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TableChart,
                            contentDescription = null,
                            tint = Color(0xFF00838F),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Paso 1: Obtén la plantilla estándar",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Usa nuestra plantilla oficial compatible con Excel y Sheets",
                            fontSize = 12.sp,
                            color = GrisTexto
                        )
                    }
                }

                Text(
                    text = "La plantilla incluye columnas predefinidas (código, nombre, precios, stock, ITBIS) con productos de ejemplo para que solo tengas que llenarla.",
                    fontSize = 13.sp,
                    color = GrisTexto,
                    lineHeight = 18.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BotonPos(
                        texto = "Descargar CSV",
                        icono = Icons.Filled.Download,
                        alHacerClic = alDescargarPlantilla,
                        variante = VarianteBoton.SECUNDARIO,
                        modifier = Modifier.weight(1f)
                    )

                    BotonPos(
                        texto = "Compartir",
                        icono = Icons.Filled.Share,
                        alHacerClic = alCompartirPlantilla,
                        variante = VarianteBoton.PRIMARIO,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. Tarjeta Paso 2: Subir archivo
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(AzulPrimario.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudUpload,
                            contentDescription = null,
                            tint = AzulPrimario,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Paso 2: Carga tu archivo",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Selecciona el archivo CSV o Excel completado",
                            fontSize = 12.sp,
                            color = GrisTexto
                        )
                    }
                }

                // Zona táctil de selección de archivo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(AzulPrimario.copy(alpha = 0.05f))
                        .border(
                            width = 1.5.dp,
                            color = AzulPrimario.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable(onClick = alSeleccionarArchivo)
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (estado.estaCargandoArchivo) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = AzulPrimario,
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = "Leyendo y validando productos...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = AzulPrimario
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FileOpen,
                                contentDescription = null,
                                tint = AzulPrimario,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "Toca para buscar tu archivo",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = AzulPrimario
                            )
                            Text(
                                text = "Soporta formatos .csv, .txt delimitados por coma o punto y coma",
                                fontSize = 11.sp,
                                color = GrisTexto,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // 3. Tarjeta desplegable: Guía de columnas y compatibilidad
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { mostrarGuiaColumnas = !mostrarGuiaColumnas },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            tint = GrisSecundario,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "¿Cómo debe estructurarse el archivo?",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        imageVector = if (mostrarGuiaColumnas) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = GrisSecundario
                    )
                }

                AnimatedVisibility(visible = mostrarGuiaColumnas) {
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalDivider(color = GrisClaro)

                        ItemExplicacionColumna("nombre", "Obligatorio. Nombre o descripción del artículo.")
                        ItemExplicacionColumna("precio_venta", "Obligatorio. Precio al cliente mayor a 0 (ej. 150.00).")
                        ItemExplicacionColumna("codigo_barras", "Opcional. Código numérico o SKU para escanear.")
                        ItemExplicacionColumna("precio_costo", "Opcional. Costo de compra para calcular ganancias.")
                        ItemExplicacionColumna("stock", "Opcional. Cantidad en existencia (por defecto 0).")
                        ItemExplicacionColumna("controla_stock", "Opcional. 'SI' para descontar con ventas o 'NO' para servicios.")
                        ItemExplicacionColumna("categoria", "Opcional. Categoría para filtros (ej. Bebidas, Farmacia).")
                        ItemExplicacionColumna("exento_itbis", "Opcional. 'SI' para 0% impuesto o 'NO' para gravado.")
                        ItemExplicacionColumna("itbis_incluido", "Opcional. 'SI' si el precio ya incluye el 18% de ITBIS.")
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemExplicacionColumna(columna: String, descripcion: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "• $columna: ",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = AzulPrimario
        )
        Text(
            text = descripcion,
            fontSize = 12.sp,
            color = GrisTexto
        )
    }
}

// =========================================================================
// PASO 2: VISTA PREVIA COMPLETA Y EDICIÓN IN-PLACE
// =========================================================================
@Composable
private fun VistaPreviaImportacion(
    estado: ImportacionUiState,
    alFiltroTextoCambiado: (String) -> Unit,
    alAlternarSoloErrores: () -> Unit,
    alAlternarActualizarExistentes: () -> Unit,
    alEditarProducto: (ProductoImportadoUi) -> Unit,
    alEliminarProducto: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // Banner de resumen con contadores visuales
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TarjetaMetricaResumen(
                titulo = "Detectados",
                valor = "${estado.totalProductos}",
                colorFondo = AzulPrimario.copy(alpha = 0.12f),
                colorTexto = AzulPrimario,
                icono = Icons.Filled.TableChart,
                modifier = Modifier.weight(1f)
            )

            TarjetaMetricaResumen(
                titulo = "Válidos",
                valor = "${estado.totalValidos}",
                colorFondo = VerdeExito.copy(alpha = 0.12f),
                colorTexto = VerdeExito,
                icono = Icons.Filled.CheckCircle,
                modifier = Modifier.weight(1f)
            )

            if (estado.totalConErrores > 0) {
                TarjetaMetricaResumen(
                    titulo = "Con Error",
                    valor = "${estado.totalConErrores}",
                    colorFondo = RojoError.copy(alpha = 0.12f),
                    colorTexto = RojoError,
                    icono = Icons.Filled.Error,
                    modifier = Modifier.weight(1f)
                )
            } else if (estado.totalExistentes > 0) {
                TarjetaMetricaResumen(
                    titulo = "Existentes",
                    valor = "${estado.totalExistentes}",
                    colorFondo = AmarilloAdvertencia.copy(alpha = 0.12f),
                    colorTexto = Color(0xFFE65100),
                    icono = Icons.Filled.Sync,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Opción: Actualizar existentes vs Crear nuevos
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, GrisClaro),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Actualizar productos existentes",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Si el código de barras ya existe, se actualizarán precios y stock",
                        fontSize = 11.sp,
                        color = GrisTexto
                    )
                }
                Switch(
                    checked = estado.actualizarExistentes,
                    onCheckedChange = { alAlternarActualizarExistentes() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Blanco,
                        checkedTrackColor = AzulPrimario
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Barra de búsqueda y Filtro de solo errores
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = estado.filtroTexto,
                onValueChange = alFiltroTextoCambiado,
                placeholder = { Text("Buscar en la lista...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = GrisSecundario,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (estado.filtroTexto.isNotEmpty()) {
                        IconButton(onClick = { alFiltroTextoCambiado("") }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Limpiar",
                                tint = GrisSecundario,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AzulPrimario,
                    unfocusedBorderColor = GrisClaro,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.weight(1f)
            )

            if (estado.totalConErrores > 0) {
                FilterChip(
                    selected = estado.mostrarSoloErrores,
                    onClick = alAlternarSoloErrores,
                    label = {
                        Text(
                            text = "Errores (${estado.totalConErrores})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = null,
                            tint = if (estado.mostrarSoloErrores) Blanco else RojoError,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RojoError,
                        selectedLabelColor = Blanco,
                        containerColor = RojoError.copy(alpha = 0.1f),
                        labelColor = RojoError
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Lista deslizable con LazyColumn optimizada
        val productosMostrados = estado.productosFiltrados

        if (productosMostrados.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.FilterList,
                        contentDescription = null,
                        tint = GrisMedio,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No hay productos que coincidan con el filtro",
                        fontSize = 14.sp,
                        color = GrisTexto
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = productosMostrados,
                    key = { it.idTemporal }
                ) { producto ->
                    TarjetaProductoImportado(
                        producto = producto,
                        alEditar = { alEditarProducto(producto) },
                        alEliminar = { alEliminarProducto(producto.idTemporal) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaMetricaResumen(
    titulo: String,
    valor: String,
    colorFondo: Color,
    colorTexto: Color,
    icono: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colorFondo,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = colorTexto,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = valor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorTexto
                )
                Text(
                    text = titulo,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorTexto.copy(alpha = 0.85f)
                )
            }
        }
    }
}

// Tarjeta individual de producto en la vista previa
@Composable
private fun TarjetaProductoImportado(
    producto: ProductoImportadoUi,
    alEditar: () -> Unit,
    alEliminar: () -> Unit
) {
    val colorBorde = when {
        !producto.esValido -> RojoError.copy(alpha = 0.7f)
        producto.esActualizacion -> AmarilloAdvertencia.copy(alpha = 0.7f)
        else -> GrisClaro
    }

    val colorFondoCard = when {
        !producto.esValido -> RojoError.copy(alpha = 0.04f)
        producto.esActualizacion -> AmarilloAdvertencia.copy(alpha = 0.04f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorFondoCard),
        border = BorderStroke(1.dp, colorBorde),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Fila Superior: Nombre del producto, Badges y Botones de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = producto.nombre.ifBlank { "SIN NOMBRE (REQUERIDO)" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (producto.nombre.isBlank()) RojoError else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Badge de código de barras
                        if (producto.codigoBarras.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = GrisClaro
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.QrCode,
                                        contentDescription = null,
                                        tint = GrisTexto,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = producto.codigoBarras,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = GrisTexto
                                    )
                                }
                            }
                        }

                        // Badge de Estado: Actualización vs Nuevo vs Error
                        if (!producto.esValido) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = RojoError.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Error",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RojoError,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else if (producto.esActualizacion) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AmarilloAdvertencia.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "Actualizará ID #${producto.idExistente ?: ""}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = VerdeExito.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Nuevo",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VerdeExito,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Categoría
                        Text(
                            text = "• ${producto.categoria}",
                            fontSize = 11.sp,
                            color = GrisTexto
                        )
                    }
                }

                // Acciones rápidas: Editar y Eliminar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = alEditar,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Editar producto",
                            tint = AzulPrimario,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = alEliminar,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Eliminar de la lista",
                            tint = RojoError.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = GrisClaro.copy(alpha = 0.7f))

            // Precios, Stock e Impuestos
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Precio de Venta
                Column {
                    Text("Precio Venta", fontSize = 10.sp, color = GrisTexto)
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", producto.precioVenta)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (producto.precioVenta <= 0.0) RojoError else AzulPrimario
                    )
                }

                // Precio de Costo
                Column {
                    Text("Costo", fontSize = 10.sp, color = GrisTexto)
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", producto.precioCosto)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GrisTexto
                    )
                }

                // Stock
                Column {
                    Text("Stock", fontSize = 10.sp, color = GrisTexto)
                    Text(
                        text = if (producto.controlaStock) "${producto.stock.toInt()}" else "Sin stock",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GrisTexto
                    )
                }

                // ITBIS
                Column {
                    Text("ITBIS", fontSize = 10.sp, color = GrisTexto)
                    Text(
                        text = if (producto.exentoItbis) "Exento" else "${producto.tasaItbis.toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = GrisTexto
                    )
                }
            }

            // Lista de errores si no es válido
            if (producto.errores.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(RojoError.copy(alpha = 0.08f))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    producto.errores.forEach { error ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Error,
                                contentDescription = null,
                                tint = RojoError,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = error, color = RojoError, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

// Barra inferior fija con botones de Importar y Agregar
@Composable
private fun BarraInferiorVistaPrevia(
    estado: ImportacionUiState,
    alImportar: () -> Unit,
    alAgregarManual: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = alAgregarManual,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = AzulPrimario,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Añadir", color = AzulPrimario, fontSize = 13.sp)
                }

                BotonPos(
                    texto = if (estado.totalValidos > 0) "Importar ${estado.totalValidos} Productos" else "No hay productos válidos",
                    icono = Icons.Filled.CloudUpload,
                    alHacerClic = alImportar,
                    estaHabilitado = estado.totalValidos > 0 && !estado.estaGuardandoEnBd,
                    estaCargando = estado.estaGuardandoEnBd,
                    variante = VarianteBoton.PRIMARIO,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// =========================================================================
// PASO 3: PROCESANDO IMPORTACIÓN
// =========================================================================
@Composable
private fun VistaProcesandoImportacion() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(56.dp),
                color = AzulPrimario,
                strokeWidth = 4.dp
            )

            Text(
                text = "Guardando productos...",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Insertando en lote en la base de datos local...",
                fontSize = 13.sp,
                color = GrisTexto,
                textAlign = TextAlign.Center
            )
        }
    }
}

// =========================================================================
// PASO 4: RESULTADO FINAL Y ÉXITO
// =========================================================================
@Composable
private fun VistaResultadoImportacion(
    estado: ImportacionUiState,
    alVerCatalogo: () -> Unit,
    alImportarOtro: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .background(VerdeExito.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = VerdeExito,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "¡Importación Exitosa!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Los productos han sido integrados a tu catálogo de venta",
            fontSize = 13.sp,
            color = GrisTexto,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Tarjeta de estadísticas de la importación
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Resumen de la Importación",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                HorizontalDivider(color = GrisClaro)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total procesados:", fontSize = 13.sp, color = GrisTexto)
                    Text(
                        "${estado.totalImportados} productos",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AzulPrimario
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Nuevos creados:", fontSize = 13.sp, color = GrisTexto)
                    Text(
                        "${estado.totalCreados}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = VerdeExito
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Actualizados:", fontSize = 13.sp, color = GrisTexto)
                    Text(
                        "${estado.totalActualizados}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        BotonPos(
            texto = "Ver Catálogo de Productos",
            icono = Icons.Filled.Inventory,
            alHacerClic = alVerCatalogo,
            variante = VarianteBoton.PRIMARIO,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        BotonPos(
            texto = "Importar Otro Archivo",
            icono = Icons.Filled.Refresh,
            alHacerClic = alImportarOtro,
            variante = VarianteBoton.SECUNDARIO,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
