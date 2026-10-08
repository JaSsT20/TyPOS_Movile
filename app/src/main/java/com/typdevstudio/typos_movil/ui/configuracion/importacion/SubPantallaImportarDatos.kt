package com.typdevstudio.typos_movil.ui.configuracion.importacion

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.GrisClaro
import com.typdevstudio.typos_movil.ui.theme.GrisMedio
import com.typdevstudio.typos_movil.ui.theme.GrisTexto
import com.typdevstudio.typos_movil.ui.theme.VerdeExito

@Composable
fun SubPantallaImportarDatos(
    alSeleccionarImportarProductos: () -> Unit
) {
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
                        imageVector = Icons.Filled.CloudDownload,
                        contentDescription = null,
                        tint = AzulPrimario,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Centro de Importación",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Carga datos masivos a la app mediante plantillas estándar de Excel o CSV.",
                        fontSize = 12.sp,
                        color = GrisTexto,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // Grupo de Módulos de Importación
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                ItemOpcionImportacion(
                    icono = Icons.Filled.Inventory,
                    colorIcono = Color(0xFF00ACC1), // Cyan moderno
                    titulo = "Importar Productos",
                    subtitulo = "Plantilla estándar de artículos, códigos de barra, precios, stock y categorías.",
                    etiquetaEstado = "Disponible",
                    alHacerClic = alSeleccionarImportarProductos,
                    mostrarDivisor = false
                )
            }
        }

        // Tarjeta Informativa de Seguridad
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
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = VerdeExito,
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Importación 100% segura y editable",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Antes de guardar cualquier dato en la app, tendrás una vista previa completa donde podrás revisar, corregir o editar cada producto directamente en pantalla.",
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
private fun ItemOpcionImportacion(
    icono: ImageVector,
    colorIcono: Color,
    titulo: String,
    subtitulo: String,
    etiquetaEstado: String? = null,
    habilitado: Boolean = true,
    alHacerClic: () -> Unit,
    mostrarDivisor: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = habilitado, onClick = alHacerClic)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(if (habilitado) colorIcono else GrisMedio, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = titulo,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (habilitado) MaterialTheme.colorScheme.onSurface else GrisMedio
                )

                if (etiquetaEstado != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (habilitado) VerdeExito.copy(alpha = 0.15f) else GrisClaro
                    ) {
                        Text(
                            text = etiquetaEstado,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (habilitado) VerdeExito else GrisTexto,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitulo,
                fontSize = 12.sp,
                color = if (habilitado) GrisTexto else GrisMedio,
                lineHeight = 16.sp
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = if (habilitado) GrisTexto.copy(alpha = 0.5f) else GrisMedio.copy(alpha = 0.3f),
            modifier = Modifier.size(16.dp)
        )
    }

    if (mostrarDivisor) {
        HorizontalDivider(
            modifier = Modifier.padding(start = 74.dp),
            color = GrisClaro
        )
    }
}
