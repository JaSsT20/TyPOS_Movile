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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.ImeAction
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

enum class SeccionConfiguracion(val titulo: String) {
    MENU_PRINCIPAL("Configuración"),
    APARIENCIA("Apariencia y Tema"),
    NEGOCIO("Datos del Negocio"),
    IMPRESORA("Impresora de Tickets"),
    INFO_SISTEMA("Información del Sistema")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaConfiguracion(
    viewModel: ConfiguracionViewModel,
    alVolver: () -> Unit
) {
    val estado by viewModel.uiState.collectAsState()
    val contexto = LocalContext.current
    var seccionActual by remember { mutableStateOf(SeccionConfiguracion.MENU_PRINCIPAL) }

    // Manejo del botón físico/gesto de atrás
    BackHandler(enabled = seccionActual != SeccionConfiguracion.MENU_PRINCIPAL) {
        seccionActual = SeccionConfiguracion.MENU_PRINCIPAL
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
        },
        containerColor = FondoClaro
    ) { paddingValores ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValores)
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
                        SubPantallaNegocio(viewModel = viewModel, estado = estado)
                    }
                    SeccionConfiguracion.IMPRESORA -> {
                        SubPantallaImpresora(viewModel = viewModel, estado = estado)
                    }
                    SeccionConfiguracion.INFO_SISTEMA -> {
                        SubPantallaInfoSistema(viewModel = viewModel, estado = estado)
                    }
                }
            }

            // Modal de actualización si se encuentra una nueva
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

// =========================================================================
// PANTALLA PRINCIPAL: MENÚ ESTILO AJUSTES DE TELÉFONO (SAMSUNG ONE UI)
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
        // Alerta de feedback general si existe
        if (estado.mensajeAlerta != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (estado.esErrorAlerta) RojoError.copy(alpha = 0.12f) else VerdeExito.copy(alpha = 0.12f))
                    .border(1.dp, if (estado.esErrorAlerta) RojoError else VerdeExito, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = estado.mensajeAlerta!!,
                    color = if (estado.esErrorAlerta) RojoError else VerdeExito,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // --- GRUPO 1: PREFERENCIAS Y NEGOCIO ---
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Blanco),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                ItemMenuAjustes(
                    icono = Icons.Filled.Palette,
                    colorIcono = Color(0xFF7C4DFF), // Violeta vibrante
                    titulo = "Apariencia y Tema",
                    subtitulo = "Modo de tema • Claro • Oscuro",
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

        // --- GRUPO 2: HARDWARE Y PERIFÉRICOS ---
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Blanco),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
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

        // --- GRUPO 3: SISTEMA Y SOPORTE ---
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Blanco),
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

// Fila interactiva estilo ajustes One UI
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
                color = GrisTexto
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitulo,
                fontSize = 12.sp,
                color = GrisSecundario,
                lineHeight = 16.sp
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = GrisMedio,
            modifier = Modifier.size(15.dp)
        )
    }

    if (mostrarDivisor) {
        HorizontalDivider(
            color = GrisClaro.copy(alpha = 0.6f),
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
            colors = CardDefaults.cardColors(containerColor = Blanco),
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
                    text = "Selecciona el tema visual de la aplicación:",
                    fontSize = 13.sp,
                    color = GrisSecundario
                )

                val opcionesTema = listOf(
                    Triple(0, "Sistema", Icons.Filled.BrightnessAuto),
                    Triple(1, "Claro", Icons.Filled.LightMode),
                    Triple(2, "Oscuro", Icons.Filled.DarkMode)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    opcionesTema.forEach { (modo, nombre, icono) ->
                        val seleccionado = estado.modoTema == modo
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (seleccionado) AzulPrimario else FondoClaro)
                                .border(
                                    width = if (seleccionado) 0.dp else 1.dp,
                                    color = if (seleccionado) Color.Transparent else GrisClaro,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.onModoTemaCambiado(modo) }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = icono,
                                    contentDescription = null,
                                    tint = if (seleccionado) Blanco else GrisSecundario,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = nombre,
                                    fontSize = 12.sp,
                                    fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium,
                                    color = if (seleccionado) Blanco else GrisTexto
                                )
                            }
                        }
                    }
                }
            }
        }

        BotonPos(
            texto = if (estado.estaGuardando) "Guardando..." else "Guardar Preferencia",
            alHacerClic = { viewModel.guardarConfiguracion() },
            estaCargando = estado.estaGuardando,
            icono = Icons.Filled.Save,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// =========================================================================
// SUB-PANTALLA 2: DATOS DEL NEGOCIO
// =========================================================================
@Composable
private fun SubPantallaNegocio(viewModel: ConfiguracionViewModel, estado: ConfiguracionUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Blanco),
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
// SUB-PANTALLA 3: IMPRESORA DE TICKETS
// =========================================================================
@Composable
private fun SubPantallaImpresora(viewModel: ConfiguracionViewModel, estado: ConfiguracionUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tarjeta de Selección Bluetooth
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Blanco),
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Impresora Bluetooth Vinculada",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AzulPrimario
                    )
                    IconButton(onClick = { viewModel.buscarDispositivosBluetooth() }) {
                        Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Refrescar", tint = AzulPrimario)
                    }
                }

                if (estado.dispositivosDisponibles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(FondoClaro)
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontraron impresoras vinculadas en los ajustes de Bluetooth de tu teléfono.",
                            fontSize = 12.sp,
                            color = GrisSecundario,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    estado.dispositivosDisponibles.forEach { dispositivo ->
                        val estaSeleccionada = estado.direccionMacImpresora == dispositivo.direccionMac
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (estaSeleccionada) AzulPrimarioClaro else FondoClaro)
                                .border(
                                    width = if (estaSeleccionada) 1.5.dp else 1.dp,
                                    color = if (estaSeleccionada) AzulPrimario else GrisClaro,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.onImpresoraSeleccionada(dispositivo) }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (estaSeleccionada) Icons.Filled.BluetoothConnected else Icons.Filled.Bluetooth,
                                        contentDescription = null,
                                        tint = if (estaSeleccionada) AzulPrimario else GrisSecundario
                                    )
                                    Column {
                                        Text(
                                            text = dispositivo.nombre,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GrisTexto
                                        )
                                        Text(
                                            text = dispositivo.direccionMac,
                                            fontSize = 11.sp,
                                            color = GrisSecundario
                                        )
                                    }
                                }

                                if (estaSeleccionada) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Seleccionada",
                                        tint = AzulPrimario
                                    )
                                }
                            }
                        }
                    }
                }

                if (!estado.direccionMacImpresora.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = { viewModel.onDesvincularImpresora() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.BluetoothDisabled, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Desvincular Impresora", fontSize = 12.sp)
                    }
                }
            }
        }

        // Calibración de Ancho de Papel
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Blanco),
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
                    text = "Tamaño y Ancho del Rollo Térmico",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AzulPrimario
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(
                        Triple(58, "58 mm", "Estándar Móvil"),
                        Triple(80, "80 mm", "Estándar Caja"),
                        Triple(57, "57 mm", "Compacto"),
                        Triple(72, "72 mm", "Mediano")
                    )

                    presets.forEach { (mm, titulo, subtitulo) ->
                        val estaSeleccionado = estado.tamanoPapel == mm
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (estaSeleccionado) AzulPrimario else FondoClaro)
                                .border(
                                    width = if (estaSeleccionado) 0.dp else 1.dp,
                                    color = if (estaSeleccionado) Color.Transparent else GrisClaro,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.onTamanoPapelPresetSeleccionado(mm) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column {
                                Text(
                                    text = titulo,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (estaSeleccionado) Blanco else GrisTexto
                                )
                                Text(
                                    text = subtitulo,
                                    fontSize = 10.sp,
                                    color = if (estaSeleccionado) Blanco.copy(alpha = 0.85f) else GrisSecundario
                                )
                            }
                        }
                    }
                }

                // Ajuste fino en milímetros
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(FondoClaro)
                            .border(1.dp, GrisClaro, RoundedCornerShape(10.dp))
                            .clickable { viewModel.onIncrementarMilimetros(-1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Filled.Remove, contentDescription = "-1 mm", tint = GrisTexto, modifier = Modifier.size(20.dp))
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        CampoTextoPos(
                            valor = estado.anchoMilimetrosPersonalizado,
                            alCambiarValor = { viewModel.onAnchoMilimetrosManualCambiado(it) },
                            etiqueta = "Ancho en mm",
                            iconoInicio = Icons.Filled.AspectRatio,
                            opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(FondoClaro)
                            .border(1.dp, GrisClaro, RoundedCornerShape(10.dp))
                            .clickable { viewModel.onIncrementarMilimetros(1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "+1 mm", tint = GrisTexto, modifier = Modifier.size(20.dp))
                    }
                }

                // Botón Imprimir Ticket de Prueba
                BotonPos(
                    texto = if (estado.estaImprimiendoPrueba) "Imprimiendo..." else "Imprimir Ticket de Prueba",
                    alHacerClic = { viewModel.imprimirTicketPrueba() },
                    estaCargando = estado.estaImprimiendoPrueba,
                    icono = Icons.Filled.Receipt,
                    variante = VarianteBoton.SECUNDARIO,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        BotonPos(
            texto = if (estado.estaGuardando) "Guardando..." else "Guardar Configuración de Impresora",
            alHacerClic = { viewModel.guardarConfiguracion() },
            estaCargando = estado.estaGuardando,
            icono = Icons.Filled.Save,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// =========================================================================
// SUB-PANTALLA 4: INFORMACIÓN DEL SISTEMA
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
            colors = CardDefaults.cardColors(containerColor = Blanco),
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
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = AzulPrimario,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Licencia y Activación",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AzulPrimario
                    )
                }

                // Tarjeta de Serial del Dispositivo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AzulPrimarioClaro)
                        .border(1.dp, AzulPrimario.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SERIAL DE ESTE DISPOSITIVO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AzulPrimario
                            )
                            Text(
                                text = estado.serialDispositivo.ifEmpty { "Cargando..." },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = GrisTexto
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = contexto.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Serial TyPOS", estado.serialDispositivo)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(contexto, "Serial copiado", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Copiar", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(14.dp))
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

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(VerdeExito.copy(alpha = 0.08f))
                                .border(1.dp, VerdeExito.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Estado:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GrisSecundario)
                                Box(
                                    modifier = Modifier
                                        .background(VerdeExito, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(text = "● LICENCIA ACTIVA", fontSize = 11.sp, color = Blanco, fontWeight = FontWeight.Bold)
                                }
                            }

                            HorizontalDivider(color = VerdeExito.copy(alpha = 0.2f))

                            FilaDetalleSistema(etiqueta = "Días Restantes:", valor = "${lic.diasRestantes} días de validez", colorValor = VerdeExito, esNegrita = true)
                            FilaDetalleSistema(etiqueta = "Fecha de Aplicación:", valor = fechaAplicadaTexto)
                            FilaDetalleSistema(etiqueta = "Fecha de Vencimiento:", valor = fechaVencTexto)
                            FilaDetalleSistema(etiqueta = "Clave Activa:", valor = lic.clave.chunked(4).joinToString("-"))
                        }
                    }
                    is EstadoLicencia.Vencida -> {
                        val fechaVencTexto = if (lic.fechaVencimiento > 0) sdfCompleto.format(Date(lic.fechaVencimiento)) else "Vencida"
                        val fechaAplicadaTexto = if (lic.fechaActivacion > 0) sdfCompleto.format(Date(lic.fechaActivacion)) else "Desconocida"

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(RojoError.copy(alpha = 0.08f))
                                .border(1.dp, RojoError.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Estado:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GrisSecundario)
                                Box(
                                    modifier = Modifier
                                        .background(RojoError, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(text = "● LICENCIA VENCIDA", fontSize = 11.sp, color = Blanco, fontWeight = FontWeight.Bold)
                                }
                            }

                            HorizontalDivider(color = RojoError.copy(alpha = 0.2f))

                            FilaDetalleSistema(etiqueta = "Fecha de Aplicación:", valor = fechaAplicadaTexto)
                            FilaDetalleSistema(etiqueta = "Venció el:", valor = fechaVencTexto, colorValor = RojoError, esNegrita = true)
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AmarilloAdvertencia.copy(alpha = 0.12f))
                                .border(1.dp, AmarilloAdvertencia.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Sin licencia activa registrada. Ingresa una clave para activar el sistema.",
                                fontSize = 12.sp,
                                color = GrisTexto,
                                fontWeight = FontWeight.Medium
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
            colors = CardDefaults.cardColors(containerColor = Blanco),
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

                HorizontalDivider(color = GrisClaro)

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
    colorValor: Color = GrisTexto,
    esNegrita: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = etiqueta,
            fontSize = 12.sp,
            color = GrisSecundario,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = valor,
            fontSize = 12.sp,
            color = colorValor,
            fontWeight = if (esNegrita) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}
