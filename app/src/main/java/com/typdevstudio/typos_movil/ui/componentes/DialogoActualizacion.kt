package com.typdevstudio.typos_movil.ui.componentes

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.AzulPrimarioClaro
import com.typdevstudio.typos_movil.ui.theme.Blanco
import com.typdevstudio.typos_movil.ui.theme.FondoClaro
import com.typdevstudio.typos_movil.ui.theme.GrisMedio
import com.typdevstudio.typos_movil.ui.theme.GrisSecundario
import com.typdevstudio.typos_movil.ui.theme.GrisTexto
import com.typdevstudio.typos_movil.ui.theme.VerdeExito
import com.typdevstudio.typos_movil.utilidades.actualizador.InfoActualizacion

@Composable
fun DialogoActualizacion(
    info: InfoActualizacion,
    alDescartar: () -> Unit,
    alActualizar: () -> Unit
) {
    Dialog(
        onDismissRequest = alDescartar,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Blanco),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Ícono de Actualización
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(AzulPrimarioClaro.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SystemUpdate,
                        contentDescription = "Actualización",
                        tint = AzulPrimario,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "¡Nueva Versión Disponible!",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = GrisTexto
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Comparativa de versiones
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(GrisMedio.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Actual: v${info.versionActual}",
                            fontSize = 12.sp,
                            color = GrisSecundario,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text("➔", color = AzulPrimario, fontWeight = FontWeight.Bold)

                    Box(
                        modifier = Modifier
                            .background(VerdeExito.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Nueva: ${info.versionRemota}",
                            fontSize = 12.sp,
                            color = VerdeExito,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (info.tamanoMb > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tamaño aprox: ${String.format("%.1f", info.tamanoMb)} MB",
                        fontSize = 11.sp,
                        color = GrisSecundario
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Notas de la versión
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(FondoClaro)
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NewReleases,
                            contentDescription = null,
                            tint = AzulPrimario,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Novedades y mejoras:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrisTexto
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val notasScrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .verticalScroll(notasScrollState)
                    ) {
                        Text(
                            text = info.notasCambio.ifBlank { "Mejoras generales de rendimiento, corrección de errores y nuevas funcionalidades." },
                            fontSize = 12.sp,
                            color = GrisTexto,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Botones de acción
                BotonPos(
                    texto = "Descargar e Instalar",
                    alHacerClic = alActualizar,
                    variante = VarianteBoton.PRIMARIO,
                    icono = Icons.Filled.Download,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(
                    onClick = alDescartar,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Recordar más tarde",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GrisSecundario
                    )
                }
            }
        }
    }
}
