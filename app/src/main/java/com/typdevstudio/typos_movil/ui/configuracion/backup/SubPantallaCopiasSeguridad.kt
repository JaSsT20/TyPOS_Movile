package com.typdevstudio.typos_movil.ui.configuracion.backup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.typdevstudio.typos_movil.ui.componentes.BotonPos
import com.typdevstudio.typos_movil.ui.componentes.VarianteBoton
import com.typdevstudio.typos_movil.ui.theme.AmarilloAdvertencia
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.Blanco
import com.typdevstudio.typos_movil.ui.theme.GrisClaro
import com.typdevstudio.typos_movil.ui.theme.GrisMedio
import com.typdevstudio.typos_movil.ui.theme.GrisSecundario
import com.typdevstudio.typos_movil.ui.theme.GrisTexto
import com.typdevstudio.typos_movil.ui.theme.RojoError
import com.typdevstudio.typos_movil.ui.theme.VerdeExito
import com.typdevstudio.typos_movil.utilidades.backup.ArchivoBackupUi

@Composable
fun SubPantallaCopiasSeguridad(
    viewModel: BackupViewModel = viewModel()
) {
    val estado by viewModel.uiState.collectAsState()

    // Diálogo de confirmación para restaurar
    estado.backupSeleccionadoParaRestaurar?.let { backup ->
        DialogoConfirmarRestauracion(
            backup = backup,
            alConfirmar = { viewModel.confirmarRestauracion() },
            alDescartar = { viewModel.cancelarRestauracion() }
        )
    }

    // Diálogo de confirmación para eliminar
    estado.backupSeleccionadoParaEliminar?.let { backup ->
        DialogoConfirmarEliminacionBackup(
            backup = backup,
            alConfirmar = { viewModel.confirmarEliminacion() },
            alDescartar = { viewModel.cancelarEliminacion() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner de Alerta de resultado
        AnimatedVisibility(visible = estado.mensajeAlerta != null) {
            estado.mensajeAlerta?.let { mensaje ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (estado.esError) RojoError.copy(alpha = 0.12f) else VerdeExito.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, if (estado.esError) RojoError.copy(alpha = 0.5f) else VerdeExito.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (estado.esError) Icons.Filled.Error else Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = if (estado.esError) RojoError else VerdeExito,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = mensaje,
                            color = if (estado.esError) RojoError else VerdeExito,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.limpiarAlerta() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Cerrar",
                                tint = GrisTexto,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // 1. Tarjeta Informativa de Backup Automático Diario
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF673AB7).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF673AB7),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Copia Automática Diaria",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Programado a las 7:30 p.m. todos los días",
                            fontSize = 12.sp,
                            color = GrisTexto
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AzulPrimario.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${estado.totalBackups} / ${estado.maximoPermitido}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AzulPrimario,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "El sistema mantiene automáticamente las últimas 7 copias de seguridad (una por día). Al generarse una nueva, la más antigua se descarta automáticamente.",
                    fontSize = 12.sp,
                    color = GrisTexto,
                    lineHeight = 16.sp
                )

                BotonPos(
                    texto = "Crear Copia de Seguridad Ahora",
                    icono = Icons.Filled.AddCircle,
                    alHacerClic = { viewModel.crearBackupManual() },
                    estaHabilitado = !estado.estaCreandoBackup && !estado.estaRestaurandoBackup,
                    estaCargando = estado.estaCreandoBackup,
                    variante = VarianteBoton.PRIMARIO,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 2. Encabezado de la lista
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Copias Guardadas (${estado.totalBackups})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Máximo 7 copias",
                fontSize = 12.sp,
                color = GrisTexto
            )
        }

        // 3. Lista de Backups
        if (estado.backups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Storage,
                        contentDescription = null,
                        tint = GrisMedio,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Aún no hay copias de seguridad guardadas",
                        fontSize = 14.sp,
                        color = GrisTexto
                    )
                    Text(
                        text = "Pulsa el botón de arriba para crear tu primer respaldo.",
                        fontSize = 12.sp,
                        color = GrisMedio,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = estado.backups,
                    key = { it.nombreArchivo }
                ) { backup ->
                    TarjetaItemBackup(
                        backup = backup,
                        alRestaurar = { viewModel.seleccionarParaRestaurar(backup) },
                        alCompartir = { viewModel.compartirBackup(backup) },
                        alEliminar = { viewModel.seleccionarParaEliminar(backup) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaItemBackup(
    backup: ArchivoBackupUi,
    alRestaurar: () -> Unit,
    alCompartir: () -> Unit,
    alEliminar: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, GrisClaro),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                        imageVector = Icons.Filled.Storage,
                        contentDescription = null,
                        tint = AzulPrimario,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = backup.nombreVisual,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${backup.fechaFormateada} • ${backup.tamanoFormateado}",
                            fontSize = 11.sp,
                            color = GrisTexto
                        )
                    }
                }
            }

            HorizontalDivider(color = GrisClaro.copy(alpha = 0.6f))

            // Acciones: Compartir, Restaurar y Eliminar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = alCompartir,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Compartir por WhatsApp/Correo",
                        tint = AzulPrimario,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = alRestaurar,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Restore,
                        contentDescription = "Restaurar base de datos",
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = alEliminar,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Eliminar copia",
                        tint = RojoError.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
