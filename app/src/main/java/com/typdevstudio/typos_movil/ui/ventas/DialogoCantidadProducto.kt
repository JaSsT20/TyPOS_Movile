package com.typdevstudio.typos_movil.ui.ventas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ProductionQuantityLimits
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.typdevstudio.typos_movil.datos.local.entidades.ProductoEntidad
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
import com.typdevstudio.typos_movil.ui.theme.RojoError
import com.typdevstudio.typos_movil.ui.theme.VerdeExito

@Composable
fun DialogoCantidadProducto(
    mostrar: Boolean,
    producto: ProductoEntidad?,
    cantidadInicial: Double = 1.0,
    alConfirmar: (ProductoEntidad, Double) -> Unit,
    alDescartar: () -> Unit
) {
    if (!mostrar || producto == null) return

    val textoInicial = if (cantidadInicial % 1.0 == 0.0) {
        "${cantidadInicial.toInt()}"
    } else {
        String.format(java.util.Locale.US, "%.2f", cantidadInicial)
    }

    var textoValor by remember(producto.id, cantidadInicial) {
        mutableStateOf(
            TextFieldValue(
                text = textoInicial,
                selection = TextRange(0, textoInicial.length)
            )
        )
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    val cantidadDouble = textoValor.text.toDoubleOrNull() ?: 0.0
    val stockDisponible = producto.stock
    val controlaStock = producto.controlaStock
    val superaStock = controlaStock && cantidadDouble > stockDisponible

    // Calcular total bruto del producto con esta cantidad
    val totalEstimado = cantidadDouble * producto.precioVenta

    fun actualizarCantidad(nuevaCantidad: Double) {
        val clamp = nuevaCantidad.coerceAtLeast(0.0)
        val nuevoTexto = if (clamp % 1.0 == 0.0) "${clamp.toInt()}" else String.format(java.util.Locale.US, "%.2f", clamp)
        textoValor = TextFieldValue(
            text = nuevoTexto,
            selection = TextRange(nuevoTexto.length)
        )
    }

    Dialog(onDismissRequest = alDescartar) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Cabecera: Título y botón cerrar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AzulPrimarioClaro.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ShoppingBag,
                                contentDescription = null,
                                tint = AzulPrimario,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Especificar Cantidad",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = producto.nombre,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    IconButton(
                        onClick = alDescartar,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Info de Precio unitario y Stock
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Precio Unitario",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$${String.format(java.util.Locale.US, "%.2f", producto.precioVenta)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AzulPrimario
                        )
                    }

                    if (controlaStock) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Stock Disponible",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val colorStock = if (stockDisponible <= 0) RojoError else if (stockDisponible <= 5) AmarilloAdvertencia else VerdeExito
                            Text(
                                text = "${stockDisponible.toInt()} unidades",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorStock
                            )
                        }
                    }
                }

                // Control central de cantidad: [-] Campo de Texto [+]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Botón decrementar (-)
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                val actual = textoValor.text.toDoubleOrNull() ?: 1.0
                                actualizarCantidad(actual - 1.0)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = "Restar 1",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Campo de entrada de cantidad grande
                    OutlinedTextField(
                        value = textoValor,
                        onValueChange = { nuevo ->
                            // Filtrar para permitir números y como máximo un punto decimal
                            val textoLimpio = nuevo.text.filter { it.isDigit() || it == '.' }
                            if (textoLimpio.count { it == '.' } <= 1) {
                                textoValor = nuevo.copy(text = textoLimpio)
                            }
                        },
                        modifier = Modifier
                            .width(130.dp)
                            .focusRequester(focusRequester),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            color = if (superaStock) RojoError else MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (!superaStock) {
                                    alConfirmar(producto, cantidadDouble)
                                }
                            }
                        ),
                        isError = superaStock,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (superaStock) RojoError else AzulPrimario,
                            unfocusedBorderColor = if (superaStock) RojoError else MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Botón incrementar (+)
                    val puedeIncrementar = !controlaStock || cantidadDouble < stockDisponible
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (puedeIncrementar) AzulPrimario else MaterialTheme.colorScheme.outlineVariant)
                            .clickable(enabled = puedeIncrementar) {
                                val actual = textoValor.text.toDoubleOrNull() ?: 0.0
                                actualizarCantidad(actual + 1.0)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Sumar 1",
                            tint = if (puedeIncrementar) Blanco else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Advertencias de validación
                if (superaStock) {
                    Text(
                        text = "⚠️ Supera el stock disponible (${stockDisponible.toInt()} unid.)",
                        color = RojoError,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                } else if (cantidadDouble <= 0.0) {
                    Text(
                        text = "ℹ️ Con 0 unidades se quitará del carrito",
                        color = AmarilloAdvertencia,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                // Chips de cantidades rápidas (+5, +10, +20, +50, +100, Max)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Cantidades rápidas:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val incrementos = listOf(5, 10, 20, 50, 100)
                        incrementos.forEach { cant ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val actual = textoValor.text.toDoubleOrNull() ?: 0.0
                                    actualizarCantidad(actual + cant)
                                },
                                label = { Text("+$cant", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = AzulPrimario
                                )
                            )
                        }

                        if (controlaStock && stockDisponible > 0) {
                            FilterChip(
                                selected = cantidadDouble == stockDisponible,
                                onClick = {
                                    actualizarCantidad(stockDisponible)
                                },
                                label = { Text("Máx (${stockDisponible.toInt()})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VerdeExito,
                                    selectedLabelColor = Blanco,
                                    containerColor = VerdeExito.copy(alpha = 0.15f),
                                    labelColor = VerdeExito
                                )
                            )
                        }
                    }
                }

                // Resumen del total calculado para este producto
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AzulPrimarioClaro.copy(alpha = 0.2f))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total por este ítem:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$${String.format(java.util.Locale.US, "%.2f", totalEstimado)}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AzulPrimario
                        )
                    }
                }

                // Botones de Acción (Cancelar / Confirmar)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BotonPos(
                        texto = "Cancelar",
                        alHacerClic = alDescartar,
                        variante = VarianteBoton.SECUNDARIO,
                        modifier = Modifier.weight(1f)
                    )

                    BotonPos(
                        texto = if (cantidadDouble <= 0.0) "Quitar" else "Aplicar",
                        alHacerClic = {
                            if (!superaStock) {
                                alConfirmar(producto, cantidadDouble)
                            }
                        },
                        estaHabilitado = !superaStock,
                        variante = if (cantidadDouble <= 0.0) VarianteBoton.PELIGRO else VarianteBoton.PRIMARIO,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
