package com.typdevstudio.typos_movil.ui.configuracion.importacion

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.typdevstudio.typos_movil.ui.componentes.BotonPos
import com.typdevstudio.typos_movil.ui.componentes.CampoTextoPos
import com.typdevstudio.typos_movil.ui.componentes.VarianteBoton
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.Blanco
import com.typdevstudio.typos_movil.ui.theme.GrisClaro
import com.typdevstudio.typos_movil.ui.theme.GrisTexto
import com.typdevstudio.typos_movil.ui.theme.RojoError
import com.typdevstudio.typos_movil.utilidades.importacion.ProductoImportadoUi

@Composable
fun DialogoEditarProductoImportado(
    producto: ProductoImportadoUi,
    alGuardar: (ProductoImportadoUi) -> Unit,
    alDescartar: () -> Unit
) {
    var nombre by remember { mutableStateOf(producto.nombre) }
    var codigoBarras by remember { mutableStateOf(producto.codigoBarras) }
    var descripcion by remember { mutableStateOf(producto.descripcion) }
    var precioCosto by remember {
        mutableStateOf(if (producto.precioCosto > 0) producto.precioCosto.toString() else "")
    }
    var precioVenta by remember {
        mutableStateOf(if (producto.precioVenta > 0) producto.precioVenta.toString() else "")
    }
    var stock by remember {
        mutableStateOf(if (producto.stock > 0) producto.stock.toString() else "0")
    }
    var controlaStock by remember { mutableStateOf(producto.controlaStock) }
    var categoria by remember { mutableStateOf(producto.categoria) }
    var exentoItbis by remember { mutableStateOf(producto.exentoItbis) }
    var itbisIncluido by remember { mutableStateOf(producto.itbisIncluido) }
    var tasaItbis by remember { mutableStateOf(producto.tasaItbis.toString()) }

    var errorNombre by remember { mutableStateOf<String?>(null) }
    var errorPrecioVenta by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = alDescartar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Cabecera del diálogo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(AzulPrimario.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            tint = AzulPrimario,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Editar Producto",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Ajusta los datos antes de importar",
                            fontSize = 12.sp,
                            color = GrisTexto
                        )
                    }

                    IconButton(
                        onClick = alDescartar,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cerrar",
                            tint = GrisTexto
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = GrisClaro
                )

                // Formulario con scroll
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Nombre (Obligatorio)
                    CampoTextoPos(
                        valor = nombre,
                        alCambiarValor = {
                            nombre = it
                            if (it.isNotBlank()) errorNombre = null
                        },
                        etiqueta = "Nombre del producto *",
                        iconoInicio = Icons.Filled.ShoppingBag,
                        mensajeError = errorNombre
                    )

                    // Código de barras
                    CampoTextoPos(
                        valor = codigoBarras,
                        alCambiarValor = { codigoBarras = it },
                        etiqueta = "Código de barras / SKU",
                        iconoInicio = Icons.Filled.QrCode,
                        opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                    )

                    // Precios: Venta y Costo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            CampoTextoPos(
                                valor = precioVenta,
                                alCambiarValor = {
                                    precioVenta = it
                                    if (it.toDoubleOrNull() ?: 0.0 > 0.0) errorPrecioVenta = null
                                },
                                etiqueta = "Precio Venta *",
                                iconoInicio = Icons.Filled.AttachMoney,
                                mensajeError = errorPrecioVenta,
                                opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            CampoTextoPos(
                                valor = precioCosto,
                                alCambiarValor = { precioCosto = it },
                                etiqueta = "Precio Costo",
                                iconoInicio = Icons.Filled.AttachMoney,
                                opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }
                    }

                    // Stock y Categoría
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            CampoTextoPos(
                                valor = stock,
                                alCambiarValor = { stock = it },
                                etiqueta = "Stock Inicial",
                                iconoInicio = Icons.Filled.Inventory,
                                opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            CampoTextoPos(
                                valor = categoria,
                                alCambiarValor = { categoria = it },
                                etiqueta = "Categoría",
                                iconoInicio = Icons.Filled.Category
                            )
                        }
                    }

                    // Descripción
                    CampoTextoPos(
                        valor = descripcion,
                        alCambiarValor = { descripcion = it },
                        etiqueta = "Descripción (Opcional)",
                        iconoInicio = Icons.Filled.Description
                    )

                    // Interruptores de control
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = GrisClaro.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Controlar Inventario / Stock", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Switch(
                                    checked = controlaStock,
                                    onCheckedChange = { controlaStock = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Blanco, checkedTrackColor = AzulPrimario)
                                )
                            }

                            HorizontalDivider(color = GrisClaro)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Exento de ITBIS (0% Impuesto)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Switch(
                                    checked = exentoItbis,
                                    onCheckedChange = { exentoItbis = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Blanco, checkedTrackColor = AzulPrimario)
                                )
                            }

                            if (!exentoItbis) {
                                HorizontalDivider(color = GrisClaro)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Precio ya incluye ITBIS", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Switch(
                                        checked = itbisIncluido,
                                        onCheckedChange = { itbisIncluido = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Blanco, checkedTrackColor = AzulPrimario)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = alDescartar,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar", color = GrisTexto, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    BotonPos(
                        texto = "Guardar",
                        icono = Icons.Filled.Check,
                        alHacerClic = {
                            var valido = true
                            if (nombre.trim().isBlank()) {
                                errorNombre = "El nombre es obligatorio"
                                valido = false
                            }
                            val pVenta = precioVenta.toDoubleOrNull() ?: 0.0
                            if (pVenta <= 0.0) {
                                errorPrecioVenta = "Precio de venta requerido (> 0)"
                                valido = false
                            }

                            if (valido) {
                                val pCosto = precioCosto.toDoubleOrNull() ?: 0.0
                                val cantStock = stock.toDoubleOrNull() ?: 0.0
                                val tasa = if (exentoItbis) 0.0 else (tasaItbis.toDoubleOrNull() ?: 18.0)

                                alGuardar(
                                    producto.copy(
                                        nombre = nombre.trim(),
                                        codigoBarras = codigoBarras.trim(),
                                        descripcion = descripcion.trim(),
                                        precioVenta = pVenta,
                                        precioCosto = pCosto,
                                        stock = cantStock,
                                        controlaStock = controlaStock,
                                        categoria = categoria.trim().ifBlank { "General" },
                                        exentoItbis = exentoItbis,
                                        itbisIncluido = itbisIncluido,
                                        tasaItbis = tasa
                                    )
                                )
                            }
                        },
                        variante = VarianteBoton.PRIMARIO,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
