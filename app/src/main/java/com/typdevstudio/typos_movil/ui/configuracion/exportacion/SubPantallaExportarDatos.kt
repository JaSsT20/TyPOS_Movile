package com.typdevstudio.typos_movil.ui.configuracion.exportacion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.typdevstudio.typos_movil.ui.componentes.BotonPos
import com.typdevstudio.typos_movil.ui.componentes.VarianteBoton
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.GrisTexto
import com.typdevstudio.typos_movil.ui.theme.RojoError
import com.typdevstudio.typos_movil.ui.theme.VerdeExito

@Composable
fun SubPantallaExportarDatos(
    viewModel: ExportacionViewModel = viewModel()
) {
    val estado by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabecera descriptiva
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = AzulPrimario.copy(alpha = 0.08f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(AzulPrimario.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudUpload,
                        contentDescription = null,
                        tint = AzulPrimario,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Exportar Productos",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Cree un archivo con todos sus productos y sus fotos para guardarlo o enviarlo.",
                        fontSize = 12.sp,
                        color = GrisTexto,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // Banner de resultado
        AnimatedVisibility(visible = estado.mensajeAlerta != null) {
            estado.mensajeAlerta?.let { mensaje ->
                val color = if (estado.esError) RojoError else VerdeExito
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = color.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (estado.esError) Icons.Filled.Error else Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = mensaje,
                            color = color,
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

        // Opciones
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                FilaOpcion(
                    titulo = "Incluir productos inactivos",
                    subtitulo = "Los productos que desactivó también se agregan al archivo.",
                    marcado = estado.opciones.incluirInactivos,
                    habilitado = !estado.estaExportando,
                    alCambiar = viewModel::cambiarIncluirInactivos
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                FilaOpcion(
                    titulo = "Incluir fotos",
                    subtitulo = "Agrega una copia de las fotos de cada producto. El archivo será más pesado.",
                    marcado = estado.opciones.incluirFotos,
                    habilitado = !estado.estaExportando,
                    alCambiar = viewModel::cambiarIncluirFotos
                )
            }
        }

        BotonPos(
            texto = if (estado.estaExportando) "Exportando..." else "Exportar productos",
            alHacerClic = { viewModel.exportar() },
            modifier = Modifier.fillMaxWidth(),
            estaHabilitado = !estado.estaExportando,
            estaCargando = estado.estaExportando,
            icono = Icons.Filled.CloudUpload
        )

        // Progreso
        if (estado.estaExportando) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AzulPrimario.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = estado.progreso?.texto ?: "Un momento...",
                        color = AzulPrimario,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    val progreso = estado.progreso
                    if (progreso != null && progreso.total > 1) {
                        LinearProgressIndicator(
                            progress = { progreso.fraccion },
                            modifier = Modifier.fillMaxWidth(),
                            color = AzulPrimario
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = AzulPrimario)
                    }
                }
            }
        }

        // Archivo listo
        estado.resultado?.let { resultado ->
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
                    Text(
                        text = "Archivo listo",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${resultado.archivo.name}\n${tamanoLegible(resultado.archivo.length())}",
                        fontSize = 12.sp,
                        color = GrisTexto,
                        lineHeight = 17.sp
                    )
                    BotonPos(
                        texto = "Compartir",
                        alHacerClic = { viewModel.compartir() },
                        modifier = Modifier.fillMaxWidth(),
                        variante = VarianteBoton.EXITO,
                        icono = Icons.Filled.Share
                    )
                    BotonPos(
                        texto = "Guardar en Descargas",
                        alHacerClic = { viewModel.guardarEnDescargas() },
                        modifier = Modifier.fillMaxWidth(),
                        variante = VarianteBoton.SECUNDARIO,
                        estaHabilitado = !estado.estaGuardando,
                        estaCargando = estado.estaGuardando,
                        icono = Icons.Filled.Download
                    )
                }
            }
        }

        // Qué trae el archivo
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = AzulPrimario,
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "¿Qué contiene el archivo?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Una hoja que se abre en Excel con todos sus productos (y que puede volver a importar " +
                            "en TyPOS), la misma lista en un formato para otros sistemas y una carpeta con las fotos. " +
                            "No incluye ventas, usuarios ni la configuración de la app; para eso use las copias de seguridad.",
                        fontSize = 12.sp,
                        color = GrisTexto,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaOpcion(
    titulo: String,
    subtitulo: String,
    marcado: Boolean,
    habilitado: Boolean,
    alCambiar: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitulo,
                fontSize = 12.sp,
                color = GrisTexto,
                lineHeight = 16.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = marcado, onCheckedChange = alCambiar, enabled = habilitado)
    }
}

private fun tamanoLegible(bytes: Long): String = when {
    bytes >= 1024L * 1024 -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024))
    bytes >= 1024 -> "${bytes / 1024} KB"
    else -> "$bytes bytes"
}
