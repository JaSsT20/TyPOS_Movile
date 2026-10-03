package com.typdevstudio.typos_movil.ui.componentes

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.AzulPrimarioClaro
import com.typdevstudio.typos_movil.ui.theme.Blanco
import com.typdevstudio.typos_movil.ui.theme.GrisMedio
import com.typdevstudio.typos_movil.ui.theme.GrisSecundario
import com.typdevstudio.typos_movil.ui.theme.GrisTexto
import com.typdevstudio.typos_movil.ui.theme.RojoError
import com.typdevstudio.typos_movil.ui.theme.VerdeExito
import com.typdevstudio.typos_movil.utilidades.actualizador.ActualizadorApp
import com.typdevstudio.typos_movil.utilidades.actualizador.InfoActualizacion
import com.typdevstudio.typos_movil.utilidades.actualizador.ProgresoDescarga
import kotlinx.coroutines.launch

@Composable
fun DialogoActualizacion(
    info: InfoActualizacion,
    alDescartar: () -> Unit,
    alActualizar: (() -> Unit)? = null
) {
    val contexto = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var progreso by remember { mutableStateOf(ProgresoDescarga()) }

    fun iniciarDescargaDirecta() {
        coroutineScope.launch {
            ActualizadorApp.descargarEInstalarApk(contexto, info.urlDescarga) { nuevoProgreso ->
                progreso = nuevoProgreso
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (!progreso.estaDescargando) {
                alDescartar()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !progreso.estaDescargando,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                        imageVector = if (progreso.estaDescargando) Icons.Filled.CloudDownload else Icons.Filled.SystemUpdate,
                        contentDescription = "Actualización",
                        tint = AzulPrimario,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (progreso.estaDescargando) "Descargando Actualización..." else "¡Nueva Versión Disponible!",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

                if (info.tamanoMb > 0 && !progreso.estaDescargando) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tamaño aprox: ${String.format("%.1f", info.tamanoMb)} MB",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SECCIÓN DE PROGRESO DE DESCARGA
                if (progreso.estaDescargando || progreso.completado) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (progreso.totalBytes > 0) {
                            LinearProgressIndicator(
                                progress = { progreso.porcentaje },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = AzulPrimario,
                                trackColor = GrisMedio.copy(alpha = 0.25f)
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = AzulPrimario,
                                trackColor = GrisMedio.copy(alpha = 0.25f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val textoProgreso = if (progreso.completado) {
                            "¡Descarga completada! Abriendo instalador..."
                        } else if (progreso.mbTotales > 0) {
                            "${String.format("%.1f", progreso.mbDescargados)} MB / ${String.format("%.1f", progreso.mbTotales)} MB (${(progreso.porcentaje * 100).toInt()}%)"
                        } else {
                            "${String.format("%.1f", progreso.mbDescargados)} MB descargados..."
                        }

                        Text(
                            text = textoProgreso,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (progreso.completado) VerdeExito else MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else if (progreso.error != null) {
                    // Mensaje de Error si falla la descarga
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(RojoError.copy(alpha = 0.1f))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = RojoError,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Hubo un problema al descargar",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = RojoError
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = progreso.error ?: "Error desconocido",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Notas de la versión
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
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
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val notasScrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .verticalScroll(notasScrollState)
                        ) {
                            Text(
                                text = info.notasCambio.ifBlank { "Mejoras generales de rendimiento, corrección de errores y nuevas funcionalidades." },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // BOTONES DE ACCIÓN
                if (progreso.estaDescargando) {
                    Text(
                        text = "Por favor espera mientras se descarga el paquete de actualización...",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                } else if (progreso.error != null) {
                    BotonPos(
                        texto = "Reintentar Descarga",
                        alHacerClic = { iniciarDescargaDirecta() },
                        variante = VarianteBoton.PRIMARIO,
                        icono = Icons.Filled.Refresh,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    BotonPos(
                        texto = "Descargar desde Navegador",
                        alHacerClic = { ActualizadorApp.abrirEnNavegador(contexto, info.urlDescarga) },
                        variante = VarianteBoton.SECUNDARIO,
                        icono = Icons.Filled.OpenInBrowser,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    TextButton(
                        onClick = alDescartar,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Cancelar",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    BotonPos(
                        texto = "Descargar e Instalar",
                        alHacerClic = {
                            if (alActualizar != null) {
                                alActualizar()
                            } else {
                                iniciarDescargaDirecta()
                            }
                        },
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
