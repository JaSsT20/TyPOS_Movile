package com.typdevstudio.typos_movil.ui.configuracion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.AzulPrimarioClaro
import com.typdevstudio.typos_movil.ui.theme.Blanco
import com.typdevstudio.typos_movil.ui.theme.GrisClaro
import com.typdevstudio.typos_movil.ui.theme.GrisMedio
import com.typdevstudio.typos_movil.ui.theme.GrisSecundario
import com.typdevstudio.typos_movil.ui.theme.GrisTexto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PrevisualizacionTicket(
    nombreNegocio: String,
    rncCedula: String,
    direccion: String,
    telefono: String,
    pieTicket: String,
    tamanoPapel: Int,
    columnas: Int,
    modifier: Modifier = Modifier
) {
    val fechaActual = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date())
    val anchoCols = if (columnas in 16..80) columnas else if (tamanoPapel == 80) 48 else if (tamanoPapel == 72) 42 else if (tamanoPapel == 57) 30 else 32

    val lineaSeparador = "-".repeat(anchoCols.coerceAtMost(48))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Blanco),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
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
                            text = "Vista Previa de la Factura",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AzulPrimario
                        )
                        Text(
                            text = "Formato simulado: $anchoCols columnas (${if (tamanoPapel == 0) "Personalizado" else "${tamanoPapel}mm"})",
                            fontSize = 12.sp,
                            color = GrisSecundario
                        )
                    }
                }
            }

            // Hoja de Papel Térmico Simulada
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 3.dp, shape = RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFCFDFE))
                    .border(1.dp, GrisClaro, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Nombre del Negocio
                    Text(
                        text = nombreNegocio.ifBlank { "MI TIENDA" }.uppercase(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )

                    if (rncCedula.isNotBlank()) {
                        Text(
                            text = "RNC / Céd: $rncCedula",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (direccion.isNotBlank()) {
                        Text(
                            text = direccion,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (telefono.isNotBlank()) {
                        Text(
                            text = "Tel: $telefono",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center
                        )
                    }

                    Text(
                        text = lineaSeparador,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray,
                        maxLines = 1
                    )

                    // Datos de la Factura
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Factura: FAC-000001", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                        }
                        Text("Fecha: $fechaActual", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                        Text("Cajero: Administrador", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                        Text("Cliente: Consumidor Final", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                    }

                    Text(
                        text = lineaSeparador,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray,
                        maxLines = 1
                    )

                    // Encabezado de Productos
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cant", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.Black)
                        Text("Descripción", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.Black)
                        Text("Total", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.Black)
                    }

                    Text(
                        text = lineaSeparador,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray,
                        maxLines = 1
                    )

                    // Artículos de Ejemplo
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("2.00", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                        Text("Papel de Baño", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                        Text("$120.00", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("1.00", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                        Text("Refresco 500ml", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                        Text("$50.00", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                    }

                    Text(
                        text = lineaSeparador,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray,
                        maxLines = 1
                    )

                    // Totales e ITBIS
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text("Subtotal:  $144.07", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                        Text("ITBIS (18%):  $25.93", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                        Text("TOTAL: $170.00", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.Black)
                    }

                    Text(
                        text = lineaSeparador,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray,
                        maxLines = 1
                    )

                    // Pago y Cambio
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Método de Pago: Efectivo", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Efectivo Recibido:", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                            Text("$200.00", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Cambio / Devuelta:", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.Black)
                            Text("$30.00", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Pie de Ticket
                    Text(
                        text = pieTicket.ifBlank { "¡Gracias por su compra!" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Software: TyPOS Móvil",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
