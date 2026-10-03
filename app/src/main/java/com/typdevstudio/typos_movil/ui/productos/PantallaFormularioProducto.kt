package com.typdevstudio.typos_movil.ui.productos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.typdevstudio.typos_movil.ui.componentes.BotonPos
import com.typdevstudio.typos_movil.ui.componentes.CampoTextoPos
import com.typdevstudio.typos_movil.ui.componentes.DialogoEscanerCodigoBarras
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.AzulPrimarioClaro
import com.typdevstudio.typos_movil.ui.theme.Blanco
import com.typdevstudio.typos_movil.ui.theme.FondoClaro
import com.typdevstudio.typos_movil.ui.theme.GrisClaro
import com.typdevstudio.typos_movil.ui.theme.GrisMedio
import com.typdevstudio.typos_movil.ui.theme.GrisSecundario
import com.typdevstudio.typos_movil.ui.theme.GrisTexto
import com.typdevstudio.typos_movil.ui.theme.RojoError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaFormularioProducto(
    viewModel: ProductoViewModel,
    alVolver: () -> Unit
) {
    val estado by viewModel.formularioState.collectAsState()
    var mostrarEscaner by remember { mutableStateOf(false) }

    LaunchedEffect(estado.guardadoExitoso) {
        if (estado.guardadoExitoso) {
            viewModel.limpiarFormulario()
            alVolver()
        }
    }

    // Modal de Escaneo con Cámara
    DialogoEscanerCodigoBarras(
        mostrar = mostrarEscaner,
        alDetectarCodigo = { codigoEscaneado ->
            viewModel.onCodigoBarrasCambiado(codigoEscaneado)
            mostrarEscaner = false
        },
        alCerrar = { mostrarEscaner = false }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (estado.id == 0L) "Nuevo Producto" else "Editar Producto",
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
            // Tarjeta de Datos del Producto
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
                        text = "Información General",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AzulPrimario
                    )

                    // Campo Código de Barras con Botón de Escáner Integrado
                    OutlinedTextField(
                        value = estado.codigoBarras,
                        onValueChange = { viewModel.onCodigoBarrasCambiado(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Código de barras / SKU (Opcional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(
                                onClick = { mostrarEscaner = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.QrCodeScanner,
                                    contentDescription = "Escanear con la cámara",
                                    tint = AzulPrimario,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = GrisTexto,
                            unfocusedTextColor = GrisTexto,
                            focusedContainerColor = Blanco,
                            unfocusedContainerColor = Blanco,
                            focusedBorderColor = AzulPrimario,
                            unfocusedBorderColor = GrisMedio,
                            focusedLabelColor = AzulPrimario,
                            unfocusedLabelColor = GrisSecundario,
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

            // Tarjeta de Precios e Inventario
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
                        text = "Precios e Inventario",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AzulPrimario
                    )

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

                    // Interruptor: ¿El precio de venta ya incluye ITBIS?
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (estado.itbisIncluido && !estado.exentoItbis) AzulPrimarioClaro.copy(alpha = 0.25f) else FondoClaro)
                            .border(
                                width = 1.dp,
                                color = if (estado.itbisIncluido && !estado.exentoItbis) AzulPrimario.copy(alpha = 0.4f) else GrisClaro,
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
                                    color = if (estado.itbisIncluido && !estado.exentoItbis) AzulPrimario else GrisTexto
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
                                    color = GrisSecundario
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
                            .background(if (estado.exentoItbis) AzulPrimarioClaro.copy(alpha = 0.25f) else FondoClaro)
                            .border(
                                width = 1.dp,
                                color = if (estado.exentoItbis) AzulPrimario.copy(alpha = 0.4f) else GrisClaro,
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
                                    color = if (estado.exentoItbis) AzulPrimario else GrisTexto
                                )
                                Text(
                                    text = "Activar para alimentos básicos o medicamentos sin impuesto",
                                    fontSize = 11.sp,
                                    color = GrisSecundario
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
                                .clip(RoundedCornerShape(10.dp))
                                .background(AzulPrimarioClaro.copy(alpha = 0.15f))
                                .border(1.dp, AzulPrimario.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
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
                                    Text(text = "Base: $${String.format("%.2f", baseImp)}", fontSize = 12.sp, color = GrisTexto)
                                    Text(text = "ITBIS: $${String.format("%.2f", itbisMonto)}", fontSize = 12.sp, color = if (itbisMonto > 0) AzulPrimario else GrisSecundario)
                                    Text(text = "Total Venta: $${String.format("%.2f", totalCobro)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AzulPrimario)
                                }
                            }
                        }
                    }

                    // Interruptor Switch Moderno con Alto Contraste
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (estado.controlaStock) AzulPrimarioClaro.copy(alpha = 0.25f) else FondoClaro)
                            .border(
                                width = 1.dp,
                                color = if (estado.controlaStock) AzulPrimario.copy(alpha = 0.4f) else GrisClaro,
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
                                    color = if (estado.controlaStock) AzulPrimario else GrisTexto
                                )
                                Text(
                                    text = if (estado.controlaStock) "Descuenta unidades automáticamente al vender" else "Desactivado (Producto o Servicio sin límite)",
                                    fontSize = 12.sp,
                                    color = GrisSecundario
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
                        // Campo de Stock inicial
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
                        .background(RojoError.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = estado.mensajeErrorGeneral!!,
                        color = RojoError,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botón Guardar
            BotonPos(
                texto = if (estado.id == 0L) "Registrar Producto" else "Actualizar Producto",
                alHacerClic = { viewModel.guardarProducto() },
                estaCargando = estado.estaGuardando,
                icono = Icons.Filled.Save
            )
        }
    }
}
