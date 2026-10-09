package com.typdevstudio.typos_movil.ui.configuracion

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.AzulPrimarioClaro
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PrevisualizacionTicket(
    nombreNegocio: String,
    slogan: String? = null,
    tamanoNombreNegocio: Int = 1,
    mostrarSlogan: Boolean = true,
    rncCedula: String? = null,
    posicionRnc: Int = 0,
    direccion: String? = null,
    posicionDireccion: Int = 0,
    telefono: String? = null,
    posicionTelefono: Int = 0,
    mostrarCajero: Boolean = true,
    mostrarCliente: Boolean = true,
    pieTicket: String = "¡Gracias por su compra!",
    tamanoPapel: Int = 58,
    columnas: Int = 32,
    alCambiarTamanoPapel: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Permite previsualizar alternando tamaños de papel interactivamente
    var tamanoPapelSeleccionado by remember(tamanoPapel) { mutableIntStateOf(if (tamanoPapel > 0) tamanoPapel else 58) }

    val anchoCols = when (tamanoPapelSeleccionado) {
        58 -> 32
        80 -> 48
        57 -> 30
        72 -> 42
        else -> if (columnas in 16..80) columnas else 32
    }

    val fechaActual = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date())
    val lineaSeparador = "-".repeat(anchoCols.coerceAtMost(48))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header del Card con Título y Selector de Ancho
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
                        Icon(imageVector = Icons.Filled.ReceiptLong, contentDescription = null, tint = AzulPrimario, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Vista Previa en Vivo",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Simulación térmica real • $anchoCols columnas",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // SELECTOR INTERACTIVO DE TAMAÑO DE PAPEL (58mm, 80mm, 57mm, 72mm)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Probar apariencia en ancho de rollo:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val opcionesPapel = listOf(
                    Pair(58, "58 mm (32 col)"),
                    Pair(80, "80 mm (48 col)"),
                    Pair(57, "57 mm (30 col)"),
                    Pair(72, "72 mm (42 col)")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    opcionesPapel.forEach { (mm, label) ->
                        val seleccionado = tamanoPapelSeleccionado == mm
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (seleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(
                                width = if (seleccionado) 0.dp else 1.dp,
                                color = if (seleccionado) Color.Transparent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    tamanoPapelSeleccionado = mm
                                    alCambiarTamanoPapel?.invoke(mm)
                                }
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium,
                                color = if (seleccionado) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                            )
                        }
                    }
                }
            }

            // HOJA DE PAPEL TÉRMICO SIMULADA (Ancho adaptable realista)
            val factorAnchoHoja = when (tamanoPapelSeleccionado) {
                58, 57 -> 0.90f // Formato compacto 58mm
                else -> 1f // Formato ancho 80mm
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(factorAnchoHoja)
                        .shadow(elevation = 4.dp, shape = RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFFDF8)) // Marfil papel térmico auténtico
                        .border(1.dp, Color(0xFFE2DFD8), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // ========================================================
                        // 1. HEADER (ENCABEZADO CONFIGURABLE)
                        // ========================================================

                        // Cálculo exacto de cómo la impresora envuelve el nombre según el tamaño y ancho de columnas
                        val maxCaracteresPorLineaNombre = when (tamanoNombreNegocio) {
                            2 -> anchoCols / 2 // Doble ancho: mitad de caracteres por línea (16 en 58mm, 24 en 80mm)
                            else -> anchoCols // 1x ancho: ancho completo (32 en 58mm, 48 en 80mm)
                        }

                        val nombreNegocioTexto = nombreNegocio.ifBlank { "MI TIENDA" }
                        val nombreFormateado = if (nombreNegocioTexto.length > maxCaracteresPorLineaNombre) {
                            nombreNegocioTexto.chunked(maxCaracteresPorLineaNombre).joinToString("\n")
                        } else {
                            nombreNegocioTexto
                        }

                        val (fontTamano, fontPeso) = when (tamanoNombreNegocio) {
                            0 -> Pair(13.sp, FontWeight.Bold) // Normal
                            1 -> Pair(16.sp, FontWeight.ExtraBold) // Mediano / Doble Alto (Recomendado)
                            2 -> Pair(18.sp, FontWeight.Black) // Grande / Doble Tamaño
                            else -> Pair(16.sp, FontWeight.ExtraBold)
                        }

                        Text(
                            text = nombreFormateado,
                            fontSize = fontTamano,
                            fontWeight = fontPeso,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF1A1A1A),
                            textAlign = TextAlign.Center,
                            lineHeight = fontTamano * 1.15f
                        )

                        // Slogan
                        if (mostrarSlogan && !slogan.isNullOrBlank()) {
                            Text(
                                text = slogan,
                                fontSize = 11.sp,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF333333),
                                textAlign = TextAlign.Center
                            )
                        }

                        // RNC / Cédula (si está en el Encabezado)
                        if (posicionRnc == 0 && !rncCedula.isNullOrBlank()) {
                            Text(
                                text = "RNC / Céd: $rncCedula",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF333333),
                                textAlign = TextAlign.Center
                            )
                        }

                        // Dirección (si está en el Encabezado)
                        if (posicionDireccion == 0 && !direccion.isNullOrBlank()) {
                            Text(
                                text = direccion,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF333333),
                                textAlign = TextAlign.Center
                            )
                        }

                        // Teléfono (si está en el Encabezado)
                        if (posicionTelefono == 0 && !telefono.isNullOrBlank()) {
                            Text(
                                text = "Tel: $telefono",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF333333),
                                textAlign = TextAlign.Center
                            )
                        }

                        Text(
                            text = lineaSeparador,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF888888),
                            maxLines = 1
                        )

                        // ========================================================
                        // 2. DATOS DE LA FACTURA
                        // ========================================================
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Factura: FAC-000001", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF1A1A1A))
                            Text("Fecha: $fechaActual", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF1A1A1A))
                            if (mostrarCajero) {
                                Text("Cajero: Administrador", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF1A1A1A))
                            }
                            if (mostrarCliente) {
                                Text("Cliente: Consumidor Final", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF1A1A1A))
                            }
                        }

                        Text(
                            text = lineaSeparador,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF888888),
                            maxLines = 1
                        )

                        // ========================================================
                        // 3. TABLA DE PRODUCTOS (3 O 4 COLUMNAS SEGÚN ANCHO)
                        // ========================================================
                        if (anchoCols <= 36) {
                            // Formato 58mm / 57mm (3 Columnas)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Cant", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFF1A1A1A))
                                Text("Descripción", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFF1A1A1A))
                                Text("Total", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFF1A1A1A))
                            }
                        } else {
                            // Formato 80mm / 72mm (4 Columnas)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Cant", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFF1A1A1A))
                                Text("Descripción", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFF1A1A1A))
                                Text("Precio", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFF1A1A1A))
                                Text("Total", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFF1A1A1A))
                            }
                        }

                        Text(
                            text = lineaSeparador,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF888888),
                            maxLines = 1
                        )

                        // Artículos de Ejemplo
                        if (anchoCols <= 36) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("2.00", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF1A1A1A))
                                Text("Baby Wipes", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF1A1A1A))
                                Text("$2,800.00", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF1A1A1A))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("1.00", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF1A1A1A))
                                Text("Canada Dry", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF1A1A1A))
                                Text("$500.00", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF1A1A1A))
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("2.00", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF1A1A1A))
                                Text("Baby Wipes", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF1A1A1A))
                                Text("$1,400.00", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF1A1A1A))
                                Text("$2,800.00", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF1A1A1A))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("1.00", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF1A1A1A))
                                Text("Canada Dry", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF1A1A1A))
                                Text("$500.00", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF1A1A1A))
                                Text("$500.00", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF1A1A1A))
                            }
                        }

                        Text(
                            text = lineaSeparador,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF888888),
                            maxLines = 1
                        )

                        // ========================================================
                        // 4. TOTALES
                        // ========================================================
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.End
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Subtotal:", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF1A1A1A))
                                Text("$3,300.00", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF1A1A1A))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("TOTAL A PAGAR:", fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.Black)
                                Text("$3,300.00", fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.Black)
                            }
                        }

                        Text(
                            text = lineaSeparador,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF888888),
                            maxLines = 1
                        )

                        // ========================================================
                        // 5. MÉTODO DE PAGO Y CAMBIO
                        // ========================================================
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Método de Pago: Efectivo", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF1A1A1A))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Efectivo Recibido:", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF1A1A1A))
                                Text("$4,000.00", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF1A1A1A))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Cambio / Devuelta:", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.Black)
                                Text("$700.00", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.Black)
                            }
                        }

                        // ========================================================
                        // 6. FOOTER (PIE DE TICKET CONFIGURABLE)
                        // ========================================================

                        // Dirección en Footer si fue configurada allí
                        if (posicionDireccion == 1 && !direccion.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = direccion,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF333333),
                                textAlign = TextAlign.Center
                            )
                        }

                        // Teléfono en Footer si fue configurado allí
                        if (posicionTelefono == 1 && !telefono.isNullOrBlank()) {
                            Text(
                                text = "Tel: $telefono",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF333333),
                                textAlign = TextAlign.Center
                            )
                        }

                        // Mensaje de despedida
                        if (pieTicket.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = pieTicket,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF333333),
                                textAlign = TextAlign.Center
                            )
                        }

                        // Powered By (Firma de software permanente)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Powered by TyPOS Móvil",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF777777),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
