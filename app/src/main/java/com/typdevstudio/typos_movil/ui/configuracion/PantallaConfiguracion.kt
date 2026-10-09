package com.typdevstudio.typos_movil.ui.configuracion

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShortText
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.typdevstudio.typos_movil.BuildConfig
import com.typdevstudio.typos_movil.datos.repositorio.EstadoLicencia
import com.typdevstudio.typos_movil.ui.componentes.BotonPos
import com.typdevstudio.typos_movil.ui.componentes.CampoTextoPos
import com.typdevstudio.typos_movil.ui.componentes.DialogoActualizacion
import com.typdevstudio.typos_movil.ui.componentes.VarianteBoton
import com.typdevstudio.typos_movil.ui.theme.AmarilloAdvertencia
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.AzulPrimarioClaro
import com.typdevstudio.typos_movil.ui.theme.Blanco
import com.typdevstudio.typos_movil.ui.theme.GrisClaro
import com.typdevstudio.typos_movil.ui.theme.GrisMedio
import com.typdevstudio.typos_movil.ui.theme.GrisSecundario
import com.typdevstudio.typos_movil.ui.theme.GrisTexto
import com.typdevstudio.typos_movil.ui.theme.RojoError
import com.typdevstudio.typos_movil.ui.theme.VerdeExito
import com.typdevstudio.typos_movil.ui.configuracion.backup.SubPantallaCopiasSeguridad
import com.typdevstudio.typos_movil.ui.configuracion.importacion.PantallaImportarProductos
import com.typdevstudio.typos_movil.ui.configuracion.importacion.SubPantallaImportarDatos
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SeccionConfiguracion(val titulo: String) {
    MENU_PRINCIPAL("Configuración"),
    APARIENCIA("Apariencia y Tema"),
    NEGOCIO("Datos del Negocio"),
    DISENO_TICKET("Personalizar Ticket / Factura"),
    IMPRESORA("Impresora de Tickets"),
    IMPORTAR_DATOS("Importar Datos"),
    IMPORTAR_PRODUCTOS("Importar Productos"),
    COPIAS_SEGURIDAD("Copias de Seguridad"),
    INFO_SISTEMA("Información del Sistema")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaConfiguracion(
    viewModel: ConfiguracionViewModel,
    alVolver: () -> Unit,
    alNavegarAProductos: () -> Unit = {}
) {
    val estado by viewModel.uiState.collectAsState()
    val contexto = LocalContext.current
    var seccionActual by remember { mutableStateOf(SeccionConfiguracion.MENU_PRINCIPAL) }

    // Manejo del botón físico/gesto de atrás
    BackHandler(enabled = seccionActual != SeccionConfiguracion.MENU_PRINCIPAL) {
        if (seccionActual == SeccionConfiguracion.IMPORTAR_PRODUCTOS) {
            seccionActual = SeccionConfiguracion.IMPORTAR_DATOS
        } else {
            seccionActual = SeccionConfiguracion.MENU_PRINCIPAL
        }
    }

    val launcherPermisoBluetooth = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            viewModel.buscarDispositivosBluetooth()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(contexto, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                launcherPermisoBluetooth.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
    }

    Scaffold(
        topBar = {
            if (seccionActual != SeccionConfiguracion.IMPORTAR_PRODUCTOS) {
                TopAppBar(
                    title = {
                        Text(
                            text = seccionActual.titulo,
                            fontWeight = FontWeight.Bold,
                            color = Blanco
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (seccionActual == SeccionConfiguracion.MENU_PRINCIPAL) {
                                    alVolver()
                                } else if (seccionActual == SeccionConfiguracion.IMPORTAR_PRODUCTOS) {
                                    seccionActual = SeccionConfiguracion.IMPORTAR_DATOS
                                } else {
                                    seccionActual = SeccionConfiguracion.MENU_PRINCIPAL
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
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AzulPrimario)
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
            // Banner de Alerta Activa: visible en cualquier ventana donde ocurra la acción
            AnimatedVisibility(visible = estado.mensajeAlerta != null && seccionActual != SeccionConfiguracion.IMPORTAR_PRODUCTOS) {
                estado.mensajeAlerta?.let { mensaje ->
                    BannerAlerta(
                        mensaje = mensaje,
                        esError = estado.esErrorAlerta,
                        alCerrar = { viewModel.limpiarAlerta() }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AnimatedContent(
                    targetState = seccionActual,
                    transitionSpec = {
                        if (targetState == SeccionConfiguracion.MENU_PRINCIPAL) {
                            (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
                        } else {
                            (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
                        }
                    },
                    label = "TransicionConfiguracion"
                ) { seccion ->
                    when (seccion) {
                        SeccionConfiguracion.MENU_PRINCIPAL -> {
                            MenuPrincipalConfiguracion(
                                estado = estado,
                                alSeleccionarSeccion = { seccionActual = it }
                            )
                        }
                        SeccionConfiguracion.APARIENCIA -> {
                            SubPantallaApariencia(viewModel = viewModel, estado = estado)
                        }
                        SeccionConfiguracion.NEGOCIO -> {
                            SubPantallaNegocio(
                                viewModel = viewModel,
                                estado = estado,
                                alIrADiseno = { seccionActual = SeccionConfiguracion.DISENO_TICKET }
                            )
                        }
                        SeccionConfiguracion.DISENO_TICKET -> {
                            SubPantallaDisenoTicket(viewModel = viewModel, estado = estado)
                        }
                        SeccionConfiguracion.IMPRESORA -> {
                            SubPantallaImpresora(
                                viewModel = viewModel,
                                estado = estado,
                                alIrADiseno = { seccionActual = SeccionConfiguracion.DISENO_TICKET }
                            )
                        }
                        SeccionConfiguracion.IMPORTAR_DATOS -> {
                            SubPantallaImportarDatos(
                                alSeleccionarImportarProductos = {
                                    seccionActual = SeccionConfiguracion.IMPORTAR_PRODUCTOS
                                }
                            )
                        }
                        SeccionConfiguracion.IMPORTAR_PRODUCTOS -> {
                            PantallaImportarProductos(
                                alVolver = {
                                    seccionActual = SeccionConfiguracion.IMPORTAR_DATOS
                                },
                                alVerCatalogo = alNavegarAProductos
                            )
                        }
                        SeccionConfiguracion.COPIAS_SEGURIDAD -> {
                            SubPantallaCopiasSeguridad()
                        }
                        SeccionConfiguracion.INFO_SISTEMA -> {
                            SubPantallaInfoSistema(viewModel = viewModel, estado = estado)
                        }
                    }
                }

                // Modal emergente cuando se encuentra una actualización
                estado.infoActualizacion?.let { info ->
                    if (info.hayActualizacion) {
                        DialogoActualizacion(
                            info = info,
                            alDescartar = { viewModel.descartarModalActualizacion() }
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// BANNER DE FEEDBACK ELEGANTE
// =========================================================================
@Composable
private fun BannerAlerta(
    mensaje: String,
    esError: Boolean,
    alCerrar: () -> Unit
) {
    val colorBorde = if (esError) RojoError else VerdeExito
    val colorFondo = if (esError) RojoError.copy(alpha = 0.14f) else VerdeExito.copy(alpha = 0.14f)
    val colorTexto = if (esError) RojoError else VerdeExito

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(12.dp),
        color = colorFondo,
        border = BorderStroke(1.dp, colorBorde.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (esError) Icons.Filled.Error else Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = colorTexto,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = mensaje,
                color = colorTexto,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = alCerrar,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Cerrar",
                    tint = colorTexto.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// =========================================================================
// PANTALLA PRINCIPAL: MENÚ ESTILO AJUSTES ONE UI
// =========================================================================
@Composable
private fun MenuPrincipalConfiguracion(
    estado: ConfiguracionUiState,
    alSeleccionarSeccion: (SeccionConfiguracion) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- GRUPO 1: PREFERENCIAS Y NEGOCIO ---
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                ItemMenuAjustes(
                    icono = Icons.Filled.Palette,
                    colorIcono = Color(0xFF7C4DFF), // Violeta vibrante
                    titulo = "Apariencia y Tema",
                    subtitulo = when (estado.modoTema) {
                        1 -> "Tema Claro activado"
                        2 -> "Tema Oscuro activado"
                        else -> "Siguiendo el sistema"
                    },
                    alHacerClic = { alSeleccionarSeccion(SeccionConfiguracion.APARIENCIA) },
                    mostrarDivisor = true
                )

                ItemMenuAjustes(
                    icono = Icons.Filled.Store,
                    colorIcono = Color(0xFF00897B), // Verde Esmeralda
                    titulo = "Datos del Negocio",
                    subtitulo = "${estado.nombreNegocio.ifEmpty { "Mi Tienda" }} • RNC • Teléfono • Dirección",
                    alHacerClic = { alSeleccionarSeccion(SeccionConfiguracion.NEGOCIO) },
                    mostrarDivisor = false
                )
            }
        }

        // --- GRUPO 2: FACTURAS, TICKETS E IMPRESORAS ---
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                ItemMenuAjustes(
                    icono = Icons.Filled.ReceiptLong,
                    colorIcono = Color(0xFF0288D1), // Azul Claro vibrante
                    titulo = "Personalizar Ticket y Factura",
                    subtitulo = "Slogan • Tamaño del Nombre • Encabezado y Pie • Vista Previa Real",
                    alHacerClic = { alSeleccionarSeccion(SeccionConfiguracion.DISENO_TICKET) },
                    mostrarDivisor = true
                )

                val subImpresora = if (!estado.nombreImpresora.isNullOrBlank()) {
                    "${estado.nombreImpresora} • ${estado.anchoMilimetrosPersonalizado} mm"
                } else {
                    "Impresora Bluetooth • Papel 58/80 mm • Calibración"
                }

                ItemMenuAjustes(
                    icono = Icons.Filled.Print,
                    colorIcono = Color(0xFFF4511E), // Naranja Intenso
                    titulo = "Impresora de Tickets",
                    subtitulo = subImpresora,
                    alHacerClic = { alSeleccionarSeccion(SeccionConfiguracion.IMPRESORA) },
                    mostrarDivisor = false
                )
            }
        }

        // --- GRUPO 3: DATOS Y RESPALDOS ---
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                ItemMenuAjustes(
                    icono = Icons.Filled.CloudDownload,
                    colorIcono = Color(0xFF00ACC1), // Cyan / Teal moderno
                    titulo = "Importar Datos",
                    subtitulo = "Importar productos desde plantilla Excel / CSV",
                    alHacerClic = { alSeleccionarSeccion(SeccionConfiguracion.IMPORTAR_DATOS) },
                    mostrarDivisor = true
                )

                ItemMenuAjustes(
                    icono = Icons.Filled.Backup,
                    colorIcono = Color(0xFF673AB7), // Púrpura Deep Purple
                    titulo = "Copias de Seguridad",
                    subtitulo = "Respaldo automático 7:30 PM • Copias manuales y restauración",
                    alHacerClic = { alSeleccionarSeccion(SeccionConfiguracion.COPIAS_SEGURIDAD) },
                    mostrarDivisor = false
                )
            }
        }

        // --- GRUPO 4: SISTEMA Y SOPORTE ---
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                val subLicencia = when (val lic = estado.estadoLicencia) {
                    is EstadoLicencia.Activa -> "Licencia Activa (${lic.diasRestantes} días) • TyPOS Móvil v${BuildConfig.VERSION_NAME}"
                    is EstadoLicencia.Vencida -> "Licencia Vencida • Requiere renovación"
                    else -> "Sin licencia activa • TyPOS Móvil v${BuildConfig.VERSION_NAME}"
                }

                ItemMenuAjustes(
                    icono = Icons.Filled.Info,
                    colorIcono = Color(0xFF1E88E5), // Azul Cobalt
                    titulo = "Información del Sistema",
                    subtitulo = subLicencia,
                    alHacerClic = { alSeleccionarSeccion(SeccionConfiguracion.INFO_SISTEMA) },
                    mostrarDivisor = false
                )
            }
        }
    }
}

@Composable
private fun ItemMenuAjustes(
    icono: ImageVector,
    colorIcono: Color,
    titulo: String,
    subtitulo: String,
    alHacerClic: () -> Unit,
    mostrarDivisor: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = alHacerClic)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(colorIcono, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = Blanco,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitulo,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(15.dp)
        )
    }

    if (mostrarDivisor) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            modifier = Modifier.padding(start = 74.dp, end = 16.dp)
        )
    }
}

// =========================================================================
// SUB-PANTALLA 1: APARIENCIA Y TEMA
// =========================================================================
@Composable
private fun SubPantallaApariencia(viewModel: ConfiguracionViewModel, estado: ConfiguracionUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
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
                Text(
                    text = "Selecciona el tema visual de la aplicación:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val opcionesTema = listOf(
                    Triple(0, "Sistema", Icons.Filled.BrightnessAuto),
                    Triple(1, "Claro", Icons.Filled.LightMode),
                    Triple(2, "Oscuro", Icons.Filled.DarkMode)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    opcionesTema.forEach { (modo, nombre, icono) ->
                        val seleccionado = estado.modoTema == modo
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (seleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(
                                width = if (seleccionado) 0.dp else 1.dp,
                                color = if (seleccionado) Color.Transparent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.onModoTemaCambiado(modo) }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = icono,
                                    contentDescription = null,
                                    tint = if (seleccionado) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = nombre,
                                    fontSize = 13.sp,
                                    fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium,
                                    color = if (seleccionado) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// SUB-PANTALLA 2: DATOS DEL NEGOCIO
// =========================================================================
@Composable
private fun SubPantallaNegocio(
    viewModel: ConfiguracionViewModel,
    estado: ConfiguracionUiState,
    alIrADiseno: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
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
                CampoTextoPos(
                    valor = estado.nombreNegocio,
                    alCambiarValor = { viewModel.onNombreNegocioCambiado(it) },
                    etiqueta = "Nombre del Negocio / Tienda *",
                    iconoInicio = Icons.Filled.Store
                )

                CampoTextoPos(
                    valor = estado.slogan,
                    alCambiarValor = { viewModel.onSloganCambiado(it) },
                    etiqueta = "Slogan o Lema del Negocio (Opcional)",
                    iconoInicio = Icons.Filled.ShortText
                )

                CampoTextoPos(
                    valor = estado.rncCedula,
                    alCambiarValor = { viewModel.onRncCedulaCambiado(it) },
                    etiqueta = "RNC / Cédula / Identificación Fiscal",
                    iconoInicio = Icons.Filled.Business
                )

                CampoTextoPos(
                    valor = estado.telefono,
                    alCambiarValor = { viewModel.onTelefonoCambiado(it) },
                    etiqueta = "Teléfono de Contacto",
                    iconoInicio = Icons.Filled.Phone,
                    opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                CampoTextoPos(
                    valor = estado.direccion,
                    alCambiarValor = { viewModel.onDireccionCambiada(it) },
                    etiqueta = "Dirección Física",
                    iconoInicio = Icons.Filled.LocationOn
                )

                CampoTextoPos(
                    valor = estado.pieTicket,
                    alCambiarValor = { viewModel.onPieTicketCambiado(it) },
                    etiqueta = "Pie del Ticket (Mensaje final)",
                    iconoInicio = Icons.Filled.Receipt
                )
            }
        }

        // Acceso directo a personalizar diseño del ticket
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = alIrADiseno)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = AzulPrimario,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Personalizar Diseño del Ticket",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = AzulPrimario
                        )
                        Text(
                            text = "Ajustar tamaños, orden y vista previa en vivo",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = AzulPrimario,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        BotonPos(
            texto = if (estado.estaGuardando) "Guardando..." else "Guardar Datos del Negocio",
            alHacerClic = { viewModel.guardarConfiguracion() },
            estaCargando = estado.estaGuardando,
            icono = Icons.Filled.Save,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// =========================================================================
// SUB-PANTALLA: PERSONALIZAR TICKET Y FACTURA (CON VISTA PREVIA EN VIVO)
// =========================================================================
@Composable
private fun SubPantallaDisenoTicket(viewModel: ConfiguracionViewModel, estado: ConfiguracionUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. VISTA PREVIA EN TIEMPO REAL ---
        PrevisualizacionTicket(
            nombreNegocio = estado.nombreNegocio,
            slogan = estado.slogan,
            tamanoNombreNegocio = estado.tamanoNombreNegocio,
            mostrarSlogan = estado.mostrarSlogan,
            rncCedula = estado.rncCedula,
            posicionRnc = estado.posicionRnc,
            direccion = estado.direccion,
            posicionDireccion = estado.posicionDireccion,
            telefono = estado.telefono,
            posicionTelefono = estado.posicionTelefono,
            mostrarCajero = estado.mostrarCajero,
            mostrarCliente = estado.mostrarCliente,
            pieTicket = estado.pieTicket,
            tamanoPapel = estado.tamanoPapel,
            columnas = estado.columnasPersonalizadas,
            alCambiarTamanoPapel = { viewModel.onTamanoPapelPresetSeleccionado(it) }
        )

        // --- 2. ENCABEZADO DEL TICKET (HEADER) ---
        Card(
            shape = RoundedCornerShape(16.dp),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Store,
                        contentDescription = null,
                        tint = AzulPrimario,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Encabezado del Negocio",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AzulPrimario
                    )
                }

                CampoTextoPos(
                    valor = estado.nombreNegocio,
                    alCambiarValor = { viewModel.onNombreNegocioCambiado(it) },
                    etiqueta = "Nombre del Negocio *",
                    iconoInicio = Icons.Filled.Store
                )

                // Selector de Tamaño del Nombre del Negocio
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Tamaño del Nombre:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val opcionesTamano = listOf(
                        Triple(0, "Normal (1x)", "Compacto (32 cols)"),
                        Triple(1, "⭐ Mediano", "Doble Alto (Recomendado)"),
                        Triple(2, "Grande", "Doble Alto/Ancho")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        opcionesTamano.forEach { (tamano, label, desc) ->
                            val seleccionado = estado.tamanoNombreNegocio == tamano
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (seleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(
                                    width = if (seleccionado) 0.dp else 1.dp,
                                    color = if (seleccionado) Color.Transparent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.onTamanoNombreNegocioCambiado(tamano) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (seleccionado) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = desc,
                                        fontSize = 9.sp,
                                        color = if (seleccionado) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Slogan
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mostrar Slogan / Lema",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = estado.mostrarSlogan,
                            onCheckedChange = { viewModel.onMostrarSloganCambiado(it) }
                        )
                    }

                    if (estado.mostrarSlogan) {
                        CampoTextoPos(
                            valor = estado.slogan,
                            alCambiarValor = { viewModel.onSloganCambiado(it) },
                            etiqueta = "Slogan o frase del negocio (ej. Calidad y servicio)",
                            iconoInicio = Icons.Filled.ShortText
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // RNC / Cédula
                CampoTextoPos(
                    valor = estado.rncCedula,
                    alCambiarValor = { viewModel.onRncCedulaCambiado(it) },
                    etiqueta = "RNC / Cédula",
                    iconoInicio = Icons.Filled.Business
                )

                SelectorUbicacionElemento(
                    etiqueta = "Ubicación del RNC:",
                    posicionActual = estado.posicionRnc,
                    opciones = listOf(0 to "Encabezado", 1 to "Ocultar"),
                    alCambiar = { viewModel.onPosicionRncCambiada(it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Teléfono
                CampoTextoPos(
                    valor = estado.telefono,
                    alCambiarValor = { viewModel.onTelefonoCambiado(it) },
                    etiqueta = "Teléfono de Contacto",
                    iconoInicio = Icons.Filled.Phone,
                    opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                SelectorUbicacionElemento(
                    etiqueta = "Ubicación del Teléfono:",
                    posicionActual = estado.posicionTelefono,
                    opciones = listOf(0 to "Encabezado", 1 to "Pie de Página", 2 to "Ocultar"),
                    alCambiar = { viewModel.onPosicionTelefonoCambiada(it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Dirección
                CampoTextoPos(
                    valor = estado.direccion,
                    alCambiarValor = { viewModel.onDireccionCambiada(it) },
                    etiqueta = "Dirección Física",
                    iconoInicio = Icons.Filled.LocationOn
                )

                SelectorUbicacionElemento(
                    etiqueta = "Ubicación de la Dirección:",
                    posicionActual = estado.posicionDireccion,
                    opciones = listOf(0 to "Encabezado", 1 to "Pie de Página", 2 to "Ocultar"),
                    alCambiar = { viewModel.onPosicionDireccionCambiada(it) }
                )
            }
        }

        // --- 3. DATOS DE LA FACTURA Y CUERPO ---
        Card(
            shape = RoundedCornerShape(16.dp),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = AzulPrimario,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Datos de Venta y Cuerpo",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AzulPrimario
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mostrar Cajero / Vendedor", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Text("Imprime el nombre del usuario activo", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = estado.mostrarCajero,
                        onCheckedChange = { viewModel.onMostrarCajeroCambiado(it) }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mostrar Nombre de Cliente", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Text("Imprime el cliente si está asignado a la venta", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = estado.mostrarCliente,
                        onCheckedChange = { viewModel.onMostrarClienteCambiado(it) }
                    )
                }
            }
        }

        // --- 4. PIE DE TICKET Y BRANDING (FOOTER) ---
        Card(
            shape = RoundedCornerShape(16.dp),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Receipt,
                        contentDescription = null,
                        tint = AzulPrimario,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pie de Ticket y Firma",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AzulPrimario
                    )
                }

                CampoTextoPos(
                    valor = estado.pieTicket,
                    alCambiarValor = { viewModel.onPieTicketCambiado(it) },
                    etiqueta = "Mensaje de Despedida",
                    iconoInicio = Icons.Filled.Receipt
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = null,
                            tint = AzulPrimario,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Firma fija: 'Powered by TyPOS Móvil' (Fuente B discreta)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Botones de Acción
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.imprimirTicketPrueba() },
                enabled = estado.direccionMacImpresora != null && !estado.estaImprimiendoPrueba,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (estado.estaImprimiendoPrueba) "Imprimiendo..." else "Probar Impresión",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            BotonPos(
                texto = if (estado.estaGuardando) "Guardando..." else "Guardar Diseño",
                alHacerClic = { viewModel.guardarConfiguracion() },
                estaCargando = estado.estaGuardando,
                icono = Icons.Filled.Save,
                modifier = Modifier.weight(1.2f)
            )
        }
    }
}

// Selector segmentado elegante para posiciones (Encabezado / Pie de Página / Ocultar)
@Composable
private fun SelectorUbicacionElemento(
    etiqueta: String,
    posicionActual: Int,
    opciones: List<Pair<Int, String>>,
    alCambiar: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = etiqueta,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            opciones.forEach { (valor, texto) ->
                val seleccionado = posicionActual == valor
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (seleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(
                        width = if (seleccionado) 0.dp else 1.dp,
                        color = if (seleccionado) Color.Transparent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { alCambiar(valor) }
                ) {
                    Text(
                        text = texto,
                        fontSize = 11.sp,
                        fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium,
                        color = if (seleccionado) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                    )
                }
            }
        }
    }
}

// =========================================================================
// SUB-PANTALLA 3: IMPRESORA DE TICKETS
// =========================================================================
@Composable
private fun SubPantallaImpresora(
    viewModel: ConfiguracionViewModel,
    estado: ConfiguracionUiState,
    alIrADiseno: () -> Unit
) {
    val contexto = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dispositivo Vinculado Actual
        Card(
            shape = RoundedCornerShape(16.dp),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (estado.nombreImpresora != null) Icons.Filled.BluetoothConnected else Icons.Filled.BluetoothDisabled,
                            contentDescription = null,
                            tint = if (estado.nombreImpresora != null) AzulPrimario else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Impresora Actual",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (estado.nombreImpresora != null) {
                        OutlinedButton(
                            onClick = { viewModel.onDesvincularImpresora() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Desvincular", fontSize = 11.sp, color = RojoError)
                        }
                    }
                }

                if (estado.nombreImpresora != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = estado.nombreImpresora ?: "",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "MAC: ${estado.direccionMacImpresora ?: "N/A"}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Ninguna impresora vinculada aún. Selecciona un dispositivo de la lista inferior.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { viewModel.buscarDispositivosBluetooth() },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buscar Dispositivos Bluetooth", fontSize = 13.sp)
                }
            }
        }

        // Lista de dispositivos encontrados
        if (estado.dispositivosDisponibles.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Dispositivos Encontrados",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    estado.dispositivosDisponibles.forEach { disp ->
                        val esSeleccionada = disp.direccionMac == estado.direccionMacImpresora
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (esSeleccionada) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(
                                1.dp,
                                if (esSeleccionada) MaterialTheme.colorScheme.primary else Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onImpresoraSeleccionada(disp) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = disp.nombre,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = disp.direccionMac,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (esSeleccionada) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Configuración de Papel y Calibración
        Card(
            shape = RoundedCornerShape(16.dp),
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
                Text(
                    text = "Ancho del Rollo de Papel",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val presets = listOf(
                    Triple(58, "58 mm", "32 col"),
                    Triple(80, "80 mm", "48 col"),
                    Triple(57, "57 mm", "30 col"),
                    Triple(72, "72 mm", "42 col")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { (mm, label, cols) ->
                        val seleccionado = estado.tamanoPapel == mm
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (seleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.onTamanoPapelPresetSeleccionado(mm) }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (seleccionado) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = cols,
                                    fontSize = 10.sp,
                                    color = if (seleccionado) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Ajuste fino
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Calibración milimétrica:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.onIncrementarMilimetros(-1) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }

                        Text(
                            text = "${estado.anchoMilimetrosPersonalizado} mm (${estado.columnasPersonalizadas} cols)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        IconButton(
                            onClick = { viewModel.onIncrementarMilimetros(1) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // Acceso directo a personalizar diseño del ticket
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = alIrADiseno)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = AzulPrimario,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Personalizar Diseño del Ticket",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = AzulPrimario
                        )
                        Text(
                            text = "Ajustar tamaños, slogan, posiciones y vista previa",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = AzulPrimario,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Botón de Prueba y Guardar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.imprimirTicketPrueba() },
                enabled = estado.direccionMacImpresora != null && !estado.estaImprimiendoPrueba,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text(
                    text = if (estado.estaImprimiendoPrueba) "Imprimiendo..." else "Probar Ticket",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            BotonPos(
                texto = if (estado.estaGuardando) "Guardando..." else "Guardar Impresora",
                alHacerClic = { viewModel.guardarConfiguracion() },
                estaCargando = estado.estaGuardando,
                icono = Icons.Filled.Save,
                modifier = Modifier.weight(1.3f)
            )
        }
    }
}

// =========================================================================
// SUB-PANTALLA 4: INFORMACIÓN DEL SISTEMA, LICENCIA Y ACTUALIZACIONES
// =========================================================================
@Composable
private fun SubPantallaInfoSistema(viewModel: ConfiguracionViewModel, estado: ConfiguracionUiState) {
    val contexto = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- SECCIÓN LICENCIA ---
        Card(
            shape = RoundedCornerShape(16.dp),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = null,
                        tint = AzulPrimario,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Licencia y Activación",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AzulPrimario
                    )
                }

                // TARJETA SERIAL DEL DISPOSITIVO
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Fingerprint,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "SERIAL DE ESTE DISPOSITIVO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Badge del código serial con texto monospace
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = estado.serialDispositivo.ifEmpty { "Generando identificador..." },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 12.dp)
                            )
                        }

                        // Botones de acción
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val clipboard = contexto.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Serial TyPOS", estado.serialDispositivo)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(contexto, "Serial copiado al portapapeles", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AzulPrimario,
                                    contentColor = Blanco
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Copiar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Hola, este es mi Serial de TyPOS Móvil para generar la licencia:\n${estado.serialDispositivo}"
                                        )
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Enviar Serial TyPOS")
                                    contexto.startActivity(shareIntent)
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, AzulPrimario),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = AzulPrimario
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Compartir", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Estado visual de la licencia
                val sdfCompleto = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
                val sdfFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                when (val lic = estado.estadoLicencia) {
                    is EstadoLicencia.Activa -> {
                        val fechaVencTexto = if (lic.fechaVencimiento > 0) sdfFecha.format(Date(lic.fechaVencimiento)) else "Sin límite"
                        val fechaAplicadaTexto = if (lic.fechaActivacion > 0) sdfCompleto.format(Date(lic.fechaActivacion)) else "Registrada"

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = VerdeExito.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, VerdeExito.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "Estado:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Surface(
                                        color = VerdeExito,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "● LICENCIA ACTIVA",
                                            fontSize = 11.sp,
                                            color = Blanco,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(color = VerdeExito.copy(alpha = 0.2f))

                                FilaDetalleSistema(etiqueta = "Días Restantes:", valor = "${lic.diasRestantes} días de validez", colorValor = VerdeExito, esNegrita = true)
                                FilaDetalleSistema(etiqueta = "Fecha de Aplicación:", valor = fechaAplicadaTexto)
                                FilaDetalleSistema(etiqueta = "Fecha de Vencimiento:", valor = fechaVencTexto)
                                FilaDetalleSistema(etiqueta = "Clave Activa:", valor = lic.clave.chunked(4).joinToString("-"))
                            }
                        }
                    }
                    is EstadoLicencia.Vencida -> {
                        val fechaVencTexto = if (lic.fechaVencimiento > 0) sdfCompleto.format(Date(lic.fechaVencimiento)) else "Vencida"
                        val fechaAplicadaTexto = if (lic.fechaActivacion > 0) sdfCompleto.format(Date(lic.fechaActivacion)) else "Desconocida"

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = RojoError.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, RojoError.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "Estado:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Surface(
                                        color = RojoError,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "● LICENCIA VENCIDA",
                                            fontSize = 11.sp,
                                            color = Blanco,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(color = RojoError.copy(alpha = 0.2f))

                                FilaDetalleSistema(etiqueta = "Fecha de Aplicación:", valor = fechaAplicadaTexto)
                                FilaDetalleSistema(etiqueta = "Venció el:", valor = fechaVencTexto, colorValor = RojoError, esNegrita = true)
                            }
                        }
                    }
                    else -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AmarilloAdvertencia.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, AmarilloAdvertencia.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Sin licencia activa registrada. Ingresa una clave para activar el sistema.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                // Formulario para ingresar o renovar licencia
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampoTextoPos(
                        valor = estado.claveLicenciaNueva,
                        alCambiarValor = { viewModel.onClaveLicenciaNuevaCambiada(it) },
                        etiqueta = "Ingresar / Renovar Clave de Licencia",
                        iconoInicio = Icons.Filled.Key
                    )

                    BotonPos(
                        texto = "Activar / Renovar Licencia",
                        alHacerClic = { viewModel.activarLicenciaNueva() },
                        estaCargando = estado.estaActivandoLicencia,
                        icono = Icons.Filled.Verified,
                        variante = VarianteBoton.EXITO,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // --- SECCIÓN APLICACIÓN Y ACTUALIZACIONES ---
        Card(
            shape = RoundedCornerShape(16.dp),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Code,
                        contentDescription = null,
                        tint = AzulPrimario,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Acerca de TyPOS Móvil",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AzulPrimario
                    )
                }

                FilaDetalleSistema(etiqueta = "Versión Instalada:", valor = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})", esNegrita = true)
                FilaDetalleSistema(etiqueta = "Desarrollador:", valor = "TyPOS Software Studio")
                FilaDetalleSistema(etiqueta = "Contacto / Soporte:", valor = "soporte@typospos.com")

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                BotonPos(
                    texto = if (estado.estaBuscandoActualizaciones) "Consultando GitHub..." else "Buscar Actualizaciones Ahora",
                    alHacerClic = { viewModel.buscarActualizacionesManualmente() },
                    estaCargando = estado.estaBuscandoActualizaciones,
                    icono = Icons.Filled.SystemUpdate,
                    variante = VarianteBoton.PRIMARIO,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun FilaDetalleSistema(
    etiqueta: String,
    valor: String,
    colorValor: Color = Color.Unspecified,
    esNegrita: Boolean = false
) {
    val finalColorValor = if (colorValor != Color.Unspecified) colorValor else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = etiqueta,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = valor,
            fontSize = 12.sp,
            color = finalColorValor,
            fontWeight = if (esNegrita) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}
