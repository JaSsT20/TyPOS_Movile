package com.typdevstudio.typos_movil.ui.configuracion

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.CloudDownload
import com.typdevstudio.typos_movil.datos.repositorio.EstadoLicencia
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.typdevstudio.typos_movil.BuildConfig
import com.typdevstudio.typos_movil.ui.componentes.DialogoActualizacion
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
                        text = "Configuración e Impresora",
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Cerrar", tint = if (estado.esErrorAlerta) RojoError else VerdeExito, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Sección 1: Datos del Negocio
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
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(AzulPrimarioClaro.copy(alpha = 0.35f), shape = RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Filled.Store, contentDescription = null, tint = AzulPrimario, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Datos del Negocio (Ticket)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AzulPrimario
                        )
                    }

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

            // Sección 2: Impresora Térmica Bluetooth y Dimensiones en mm
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(AzulPrimarioClaro.copy(alpha = 0.35f), shape = RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Filled.Print, contentDescription = null, tint = AzulPrimario, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Impresora Térmica",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = AzulPrimario
                            )
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

                    // Título y Selector de Tamaño de Papel
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Tamaño del Papel (Milímetros)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GrisTexto
                        )
                        Text(
                            text = "Selecciona un estándar o ingresa libremente la medida en mm",
                            fontSize = 12.sp,
                            color = GrisSecundario
                        )
                    }

                    // Chips de Acceso Rápido (Presets)
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

                    // Tarjeta de Medida Libre en mm y Calibración Fina
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = FondoClaro),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GrisClaro),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Medida Exacta del Rollo",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AzulPrimario
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Blanco)
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
                                        etiqueta = "Ancho en mm (ej: 58)",
                                        iconoInicio = Icons.Filled.AspectRatio,
                                        opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Blanco)
                                        .border(1.dp, GrisClaro, RoundedCornerShape(10.dp))
                                        .clickable { viewModel.onIncrementarMilimetros(1) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Filled.Add, contentDescription = "+1 mm", tint = GrisTexto, modifier = Modifier.size(20.dp))
                                }
                            }

                            // Calibración fina de caracteres por línea
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AzulPrimarioClaro.copy(alpha = 0.25f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = "Caracteres por línea",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AzulPrimario
                                    )
                                    Text(
                                        text = "Calculado: ${estado.columnasPersonalizadas} columnas",
                                        fontSize = 11.sp,
                                        color = GrisSecundario
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Blanco)
                                            .clickable { viewModel.onColumnasPersonalizadasCambiadas(estado.columnasPersonalizadas - 1) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = Icons.Filled.Remove, contentDescription = "Menos columnas", tint = GrisTexto, modifier = Modifier.size(16.dp))
                                    }

                                    Box(
                                        modifier = Modifier.width(36.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${estado.columnasPersonalizadas}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = AzulPrimario,
                                            maxLines = 1,
                                            softWrap = false,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Blanco)
                                            .clickable { viewModel.onColumnasPersonalizadasCambiadas(estado.columnasPersonalizadas + 1) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = Icons.Filled.Add, contentDescription = "Más columnas", tint = GrisTexto, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Impresora Vinculada Actualmente
                    Text(
                        text = "Impresora Vinculada",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GrisTexto
                    )

                    if (!estado.direccionMacImpresora.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(VerdeExito.copy(alpha = 0.1f))
                                .border(1.dp, VerdeExito.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Filled.BluetoothConnected, contentDescription = null, tint = VerdeExito, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = estado.nombreImpresora ?: "Impresora Bluetooth",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GrisTexto
                                    )
                                    Text(
                                        text = "MAC: ${estado.direccionMacImpresora}",
                                        fontSize = 12.sp,
                                        color = GrisSecundario
                                    )
                                }
                                TextButton(onClick = { viewModel.onDesvincularImpresora() }) {
                                    Text("Cambiar", color = RojoError, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Botón de Prueba de Impresión
                        BotonPos(
                            texto = "Imprimir Ticket de Prueba",
                            alHacerClic = { viewModel.imprimirTicketPrueba() },
                            estaCargando = estado.estaImprimiendoPrueba,
                            variante = VarianteBoton.SECUNDARIO,
                            icono = Icons.Filled.Print
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AmarilloAdvertencia.copy(alpha = 0.12f))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Filled.BluetoothDisabled, contentDescription = null, tint = AmarilloAdvertencia, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "No hay impresora seleccionada. Elige una de la lista de abajo.",
                                    fontSize = 13.sp,
                                    color = GrisTexto
                                )
                            }
                        }
                    }

                    // Lista de Dispositivos Bluetooth Vinculados
                    Text(
                        text = "Dispositivos Bluetooth Disponibles:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GrisSecundario
                    )

                    if (estado.dispositivosDisponibles.isEmpty()) {
                        Text(
                            text = "No se encontraron dispositivos emparejados. Vincula tu impresora en los Ajustes de Bluetooth del teléfono y presiona el botón de actualizar (↻).",
                            fontSize = 12.sp,
                            color = GrisMedio
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            estado.dispositivosDisponibles.forEach { disp ->
                                val esActiva = disp.direccionMac == estado.direccionMacImpresora
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (esActiva) AzulPrimarioClaro.copy(alpha = 0.3f) else FondoClaro)
                                        .border(1.dp, if (esActiva) AzulPrimario else GrisClaro, RoundedCornerShape(10.dp))
                                        .clickable { viewModel.onImpresoraSeleccionada(disp) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Filled.Bluetooth, contentDescription = null, tint = if (esActiva) AzulPrimario else GrisSecundario, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(text = disp.nombre, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = GrisTexto)
                                                Text(text = disp.direccionMac, fontSize = 11.sp, color = GrisMedio)
                                            }
                                        }
                                        if (esActiva) {
                                            Icon(imageVector = Icons.Filled.Check, contentDescription = "Seleccionada", tint = AzulPrimario, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Sección 3: Vista Previa en Vivo de la Factura
            val mmNumero = estado.anchoMilimetrosPersonalizado.toIntOrNull() ?: 58
            PrevisualizacionTicket(
                nombreNegocio = estado.nombreNegocio,
                rncCedula = estado.rncCedula,
                direccion = estado.direccion,
                telefono = estado.telefono,
                pieTicket = estado.pieTicket,
                tamanoPapel = mmNumero,
                columnas = estado.columnasPersonalizadas
            )

            // Sección 4: Tema y Apariencia Visual
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(AzulPrimarioClaro.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Palette,
                                contentDescription = null,
                                tint = AzulPrimario,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Tema y Apariencia",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = GrisTexto
                            )
                            Text(
                                text = "Elige la combinación de colores que prefieras",
                                fontSize = 12.sp,
                                color = GrisSecundario
                            )
                        }
                    }

                    // Opciones de tema: Sistema (0), Claro (1), Oscuro (2)
                    val opcionesTema = listOf(
                        Triple(0, "Seguir el Sistema", Icons.Filled.BrightnessAuto),
                        Triple(1, "Modo Claro", Icons.Filled.LightMode),
                        Triple(2, "Modo Oscuro", Icons.Filled.DarkMode)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        opcionesTema.forEach { (codigo, titulo, icono) ->
                            val esSeleccionado = estado.modoTema == codigo
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (esSeleccionado) AzulPrimarioClaro.copy(alpha = 0.25f) else FondoClaro)
                                    .border(1.dp, if (esSeleccionado) AzulPrimario else GrisClaro, RoundedCornerShape(10.dp))
                                    .clickable { viewModel.onModoTemaCambiado(codigo) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = icono,
                                            contentDescription = null,
                                            tint = if (esSeleccionado) AzulPrimario else GrisSecundario,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = titulo,
                                            fontSize = 14.sp,
                                            fontWeight = if (esSeleccionado) FontWeight.Bold else FontWeight.Medium,
                                            color = GrisTexto
                                        )
                                    }

                                    if (esSeleccionado) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Seleccionado",
                                            tint = AzulPrimario,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Sección 5: Licencia y Suscripción
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(AzulPrimarioClaro.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Key,
                                contentDescription = null,
                                tint = AzulPrimario,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Licencia del Sistema",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = GrisTexto
                            )
                            Text(
                                text = "Estado de tu suscripción y activación",
                                fontSize = 12.sp,
                                color = GrisSecundario
                            )
                        }
                    }

                    // Estado actual de la licencia
                    when (val lic = estado.estadoLicencia) {
                        is EstadoLicencia.Activa -> {
                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            val fechaVenc = sdf.format(Date(lic.fechaVencimiento))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(VerdeExito.copy(alpha = 0.12f))
                                    .border(1.dp, VerdeExito.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "ESTADO:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GrisSecundario)
                                        Box(
                                            modifier = Modifier
                                                .background(VerdeExito, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("LICENCIA ACTIVA", fontSize = 10.sp, color = Blanco, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text(
                                        text = "Vence: $fechaVenc (${lic.diasRestantes} días restantes)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GrisTexto
                                    )
                                    Text(
                                        text = "Clave: ${lic.clave.chunked(4).joinToString("-")}",
                                        fontSize = 11.sp,
                                        color = GrisSecundario
                                    )
                                }
                            }
                        }
                        is EstadoLicencia.Vencida -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(RojoError.copy(alpha = 0.12f))
                                    .border(1.dp, RojoError.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Tu licencia ha vencido. Ingresa una nueva clave para reactivar.",
                                    fontSize = 12.sp,
                                    color = RojoError,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AmarilloAdvertencia.copy(alpha = 0.12f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Sin licencia activa registrada.",
                                    fontSize = 12.sp,
                                    color = GrisTexto,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Formulario para extender o ingresar nueva clave
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

            // Sección 6: Actualizaciones y Versión
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(AzulPrimarioClaro.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SystemUpdate,
                                contentDescription = null,
                                tint = AzulPrimario,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Actualizaciones de la App",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = GrisTexto
                            )
                            Text(
                                text = "Versión instalada: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                                fontSize = 12.sp,
                                color = GrisSecundario
                            )
                        }
                    }

                    Text(
                        text = "Consulta directamente en GitHub Releases si existe una versión más reciente con nuevas funciones y mejoras.",
                        fontSize = 12.sp,
                        color = GrisMedio,
                        lineHeight = 16.sp
                    )

                    BotonPos(
                        texto = if (estado.estaBuscandoActualizaciones) "Verificando..." else "Buscar Actualizaciones Ahora",
                        alHacerClic = { viewModel.buscarActualizacionesManualmente() },
                        estaCargando = estado.estaBuscandoActualizaciones,
                        icono = Icons.Filled.CloudDownload,
                        variante = VarianteBoton.SECUNDARIO,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Sección 5: Botón Guardar Cambios
            BotonPos(
                texto = "Guardar Configuración",
                alHacerClic = { viewModel.guardarConfiguracion() },
                estaCargando = estado.estaGuardando,
                icono = Icons.Filled.Save,
                variante = VarianteBoton.PRIMARIO
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Modal si hay una actualización encontrada
    val info = estado.infoActualizacion
    if (info != null && info.hayActualizacion) {
        DialogoActualizacion(
            info = info,
            alDescartar = { viewModel.descartarModalActualizacion() }
        )
    }
}
