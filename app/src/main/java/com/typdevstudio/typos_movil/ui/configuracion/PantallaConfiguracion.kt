package com.typdevstudio.typos_movil.ui.configuracion

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaConfiguracion(
    viewModel: ConfiguracionViewModel,
    alVolver: () -> Unit
) {
    val estado by viewModel.uiState.collectAsState()
    val contexto = LocalContext.current

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
                        text = "Configuración del Sistema",
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
        containerColor = FondoClaro
    ) { paddingValores ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValores)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Alerta de feedback
            if (estado.mensajeAlerta != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (estado.esErrorAlerta) RojoError.copy(alpha = 0.12f) else VerdeExito.copy(alpha = 0.12f))
                        .border(
                            width = 1.dp,
                            color = if (estado.esErrorAlerta) RojoError else VerdeExito,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = estado.mensajeAlerta!!,
                            color = if (estado.esErrorAlerta) RojoError else VerdeExito,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.limpiarAlerta() }, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Cerrar",
                                tint = if (estado.esErrorAlerta) RojoError else VerdeExito,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // ==========================================
            // SECCIÓN 1: APARIENCIA Y TEMA VISUAL
            // ==========================================
            EncabezadoSeccion(
                icono = Icons.Filled.Palette,
                titulo = "Apariencia y Tema",
                subtitulo = "Personaliza los colores de la aplicación"
            )

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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Modo de Visualización",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GrisTexto
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple(0, "Sistema", Icons.Filled.BrightnessAuto),
                            Triple(1, "Claro", Icons.Filled.LightMode),
                            Triple(2, "Oscuro", Icons.Filled.DarkMode)
                        ).forEach { (modo, titulo, icono) ->
                            val seleccionado = estado.modoTema == modo
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (seleccionado) AzulPrimario else FondoClaro)
                                    .border(
                                        width = 1.dp,
                                        color = if (seleccionado) AzulPrimario else GrisClaro,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.onModoTemaCambiado(modo) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = icono,
                                        contentDescription = titulo,
                                        tint = if (seleccionado) Blanco else GrisSecundario,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = titulo,
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

            // ==========================================
            // SECCIÓN 2: DATOS DEL NEGOCIO (FACTURACIÓN)
            // ==========================================
            EncabezadoSeccion(
                icono = Icons.Filled.Store,
                titulo = "Datos del Negocio",
                subtitulo = "Información que aparecerá en tus comprobantes y tickets"
            )

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
                    CampoTextoPos(
                        valor = estado.nombreNegocio,
                        alCambiarValor = { viewModel.onNombreNegocioCambiado(it) },
                        etiqueta = "Nombre de la Tienda / Empresa *",
                        iconoInicio = Icons.Filled.Business
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CampoTextoPos(
                            valor = estado.rncCedula,
                            alCambiarValor = { viewModel.onRncCedulaCambiado(it) },
                            etiqueta = "RNC o Cédula",
                            iconoInicio = Icons.Filled.Receipt,
                            modifier = Modifier.weight(1f)
                        )
                        CampoTextoPos(
                            valor = estado.telefono,
                            alCambiarValor = { viewModel.onTelefonoCambiado(it) },
                            etiqueta = "Teléfono",
                            iconoInicio = Icons.Filled.Phone,
                            opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    CampoTextoPos(
                        valor = estado.direccion,
                        alCambiarValor = { viewModel.onDireccionCambiada(it) },
                        etiqueta = "Dirección Física (Opcional)",
                        iconoInicio = Icons.Filled.LocationOn
                    )

                    CampoTextoPos(
                        valor = estado.pieTicket,
                        alCambiarValor = { viewModel.onPieTicketCambiado(it) },
                        etiqueta = "Mensaje al final del Ticket",
                        iconoInicio = Icons.Filled.FormatAlignLeft
                    )
                }
            }

            // ==========================================
            // SECCIÓN 3: IMPRESORA TÉRMICA BLUETOOTH
            // ==========================================
            EncabezadoSeccion(
                icono = Icons.Filled.Print,
                titulo = "Impresora Térmica",
                subtitulo = "Configura tu impresora Bluetooth y calibración de papel"
            )

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
                    // Estado de Conexión y Botón Buscar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (estado.direccionMacImpresora != null) Icons.Filled.BluetoothConnected else Icons.Filled.BluetoothDisabled,
                                contentDescription = null,
                                tint = if (estado.direccionMacImpresora != null) VerdeExito else GrisSecundario,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (estado.direccionMacImpresora != null) (estado.nombreImpresora ?: "Impresora Conectada") else "Sin impresora vinculada",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GrisTexto
                                )
                                if (estado.direccionMacImpresora != null) {
                                    Text(
                                        text = "MAC: ${estado.direccionMacImpresora}",
                                        fontSize = 11.sp,
                                        color = GrisSecundario
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                                    ContextCompat.checkSelfPermission(contexto, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                    launcherPermisoBluetooth.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                } else {
                                    viewModel.buscarDispositivosBluetooth()
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Buscar dispositivos", tint = AzulPrimario)
                        }
                    }

                    // Lista de Dispositivos Disponibles
                    if (estado.dispositivosDisponibles.isNotEmpty()) {
                        Text(
                            text = "Dispositivos Bluetooth Vinculados:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrisSecundario
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            estado.dispositivosDisponibles.forEach { disp ->
                                val estaSeleccionado = disp.direccionMac == estado.direccionMacImpresora
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (estaSeleccionado) AzulPrimarioClaro.copy(alpha = 0.35f) else FondoClaro)
                                        .border(1.dp, if (estaSeleccionado) AzulPrimario else GrisClaro, RoundedCornerShape(10.dp))
                                        .clickable { viewModel.onImpresoraSeleccionada(disp) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.Bluetooth,
                                            contentDescription = null,
                                            tint = if (estaSeleccionado) AzulPrimario else GrisSecundario,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = disp.nombre,
                                                fontSize = 13.sp,
                                                fontWeight = if (estaSeleccionado) FontWeight.Bold else FontWeight.Medium,
                                                color = GrisTexto
                                            )
                                            Text(
                                                text = disp.direccionMac,
                                                fontSize = 10.sp,
                                                color = GrisSecundario
                                            )
                                        }
                                    }

                                    if (estaSeleccionado) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Seleccionada",
                                            tint = AzulPrimario,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = GrisClaro)

                    // Selector de Tamaño de Papel
                    Text(
                        text = "Tamaño del Papel:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GrisTexto
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple(58, "58 mm", "Portátil 2\""),
                            Triple(80, "80 mm", "Estándar 3\""),
                            Triple(57, "57 mm", "Mini"),
                            Triple(72, "72 mm", "Mediana")
                        ).forEach { (tamano, titulo, subtitulo) ->
                            val estaSeleccionado = estado.anchoMilimetrosPersonalizado == tamano.toString()
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (estaSeleccionado) AzulPrimario else FondoClaro)
                                    .border(1.dp, if (estaSeleccionado) AzulPrimario else GrisClaro, RoundedCornerShape(10.dp))
                                    .clickable { viewModel.onTamanoPapelPresetSeleccionado(tamano) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

                    // Calibración Fina de Milímetros
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

                    // Botón Imprimir Prueba
                    BotonPos(
                        texto = if (estado.estaImprimiendoPrueba) "Imprimiendo Ticket..." else "Imprimir Ticket de Prueba",
                        alHacerClic = { viewModel.imprimirTicketPrueba() },
                        estaCargando = estado.estaImprimiendoPrueba,
                        icono = Icons.Filled.Receipt,
                        variante = VarianteBoton.SECUNDARIO,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ==========================================
            // SECCIÓN 4: INFORMACIÓN DEL SISTEMA
            // ==========================================
            EncabezadoSeccion(
                icono = Icons.Filled.Info,
                titulo = "Información del Sistema",
                subtitulo = "Licencia, versión del software y datos de soporte"
            )

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
                    // --- SUB-SECCIÓN: LICENCIA COMERCIAL ---
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

                    // Serial de este Dispositivo
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(FondoClaro)
                            .border(1.dp, GrisClaro, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Serial de este Dispositivo",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GrisSecundario
                                )
                                Text(
                                    text = estado.serialDispositivo.ifEmpty { "Cargando..." },
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GrisTexto
                                )
                            }
                            androidx.compose.material3.OutlinedButton(
                                onClick = {
                                    val clipboard = contexto.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Serial TyPOS", estado.serialDispositivo)
                                    clipboard.setPrimaryClip(clip)
                                    android.widget.Toast.makeText(contexto, "Serial copiado", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(text = "Copiar", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
                                    Text(
                                        text = "Estado:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GrisSecundario
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(VerdeExito, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "● LICENCIA ACTIVA",
                                            fontSize = 11.sp,
                                            color = Blanco,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                HorizontalDivider(color = VerdeExito.copy(alpha = 0.2f))

                                FilaDetalleSistema(
                                    etiqueta = "Días Restantes:",
                                    valor = "${lic.diasRestantes} días de validez",
                                    colorValor = VerdeExito,
                                    esNegrita = true
                                )
                                FilaDetalleSistema(
                                    etiqueta = "Fecha de Aplicación:",
                                    valor = fechaAplicadaTexto
                                )
                                FilaDetalleSistema(
                                    etiqueta = "Fecha de Vencimiento:",
                                    valor = fechaVencTexto
                                )
                                FilaDetalleSistema(
                                    etiqueta = "Clave Activa:",
                                    valor = lic.clave.chunked(4).joinToString("-")
                                )
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

                    HorizontalDivider(color = GrisClaro)

                    // --- SUB-SECCIÓN: DATOS DE LA APLICACIÓN & DESARROLLADOR ---
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Code,
                            contentDescription = null,
                            tint = AzulPrimario,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Acerca de la Aplicación",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = AzulPrimario
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(FondoClaro)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilaDetalleSistema(etiqueta = "Aplicación:", valor = "TyPOS Móvil", esNegrita = true)
                        FilaDetalleSistema(etiqueta = "Versión del Software:", valor = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})")
                        FilaDetalleSistema(etiqueta = "Desarrollador:", valor = "TyPOS (TyP DevStudio)")
                        FilaDetalleSistema(etiqueta = "Correo de Contacto:", valor = "soporte@typdevstudio.com", colorValor = AzulPrimario)
                    }

                    // Módulo de Actualizaciones
                    BotonPos(
                        texto = if (estado.estaBuscandoActualizaciones) "Buscando Actualizaciones..." else "Buscar Actualizaciones de la App",
                        alHacerClic = { viewModel.buscarActualizacionesManualmente() },
                        estaCargando = estado.estaBuscandoActualizaciones,
                        icono = Icons.Filled.CloudDownload,
                        variante = VarianteBoton.SECUNDARIO,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ==========================================
            // BOTÓN FINAL: GUARDAR CONFIGURACIÓN
            // ==========================================
            BotonPos(
                texto = "Guardar Cambios de Configuración",
                alHacerClic = { viewModel.guardarConfiguracion() },
                estaCargando = estado.estaGuardando,
                icono = Icons.Filled.Save,
                variante = VarianteBoton.PRIMARIO,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Modal de Actualización si se encuentra una nueva versión
    val info = estado.infoActualizacion
    if (info != null && info.hayActualizacion) {
        DialogoActualizacion(
            info = info,
            alDescartar = { viewModel.descartarModalActualizacion() }
        )
    }
}

/**
 * Componente de Encabezado estilizado para cada sección de los Ajustes.
 */
@Composable
private fun EncabezadoSeccion(
    icono: ImageVector,
    titulo: String,
    subtitulo: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(AzulPrimarioClaro.copy(alpha = 0.35f), shape = RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = AzulPrimario,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = titulo,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = GrisTexto
            )
            Text(
                text = subtitulo,
                fontSize = 11.sp,
                color = GrisSecundario
            )
        }
    }
}

/**
 * Fila clave-valor estilizada para datos técnicos y del sistema.
 */
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
