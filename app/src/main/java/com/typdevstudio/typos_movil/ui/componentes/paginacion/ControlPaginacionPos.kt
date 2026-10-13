package com.typdevstudio.typos_movil.ui.componentes.paginacion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.Blanco

/**
 * Componente visual reutilizable para la navegación de listas paginadas.
 * Brinda botones de navegación, saltos a primera/última página, información
 * de registros mostrados y un modal rápido para seleccionar páginas.
 */
@Composable
fun ControlPaginacionPos(
    estado: EstadoPaginacion,
    totalElementos: Int,
    modifier: Modifier = Modifier,
    mostrarSelectorTamanio: Boolean = false,
    opcionesTamanio: List<Int> = listOf(10, 20, 50)
) {
    val totalPaginas = estado.calcularTotalPaginas(totalElementos)
    if (totalElementos <= 0) return

    var mostrarSelectorPagina by remember { mutableStateOf(false) }

    val paginaActual = estado.paginaActual
    val inicioItem = ((paginaActual - 1) * estado.tamanioPagina) + 1
    val finItem = minOf(paginaActual * estado.tamanioPagina, totalElementos)

    // Modal para saltar directamente a cualquier página
    if (mostrarSelectorPagina) {
        DialogoSaltoPagina(
            paginaActual = paginaActual,
            totalPaginas = totalPaginas,
            totalElementos = totalElementos,
            alSeleccionarPagina = {
                estado.irAPagina(it, totalPaginas)
                mostrarSelectorPagina = false
            },
            alCerrar = { mostrarSelectorPagina = false }
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Fila de información resumida
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mostrando $inicioItem - $finItem de $totalElementos",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                if (mostrarSelectorTamanio) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Por pág:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        opcionesTamanio.forEach { tam ->
                            val seleccionado = tam == estado.tamanioPagina
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (seleccionado) AzulPrimario else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { estado.cambiarTamanioPagina(tam) }
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$tam",
                                    fontSize = 11.sp,
                                    fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal,
                                    color = if (seleccionado) Blanco else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Fila de controles de navegación
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ir a la primera página
                IconButton(
                    onClick = { estado.primeraPagina() },
                    enabled = paginaActual > 1,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FirstPage,
                        contentDescription = "Primera página",
                        tint = if (paginaActual > 1) AzulPrimario else MaterialTheme.colorScheme.outlineVariant
                    )
                }

                // Página anterior
                IconButton(
                    onClick = { estado.paginaAnterior() },
                    enabled = paginaActual > 1,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Página anterior",
                        tint = if (paginaActual > 1) AzulPrimario else MaterialTheme.colorScheme.outlineVariant
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Indicador central interactivo (Tocar para saltar a página)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, AzulPrimario.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .clickable { mostrarSelectorPagina = true }
                        .padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Página $paginaActual de $totalPaginas",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AzulPrimario
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Página siguiente
                IconButton(
                    onClick = { estado.paginaSiguiente(totalPaginas) },
                    enabled = paginaActual < totalPaginas,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Página siguiente",
                        tint = if (paginaActual < totalPaginas) AzulPrimario else MaterialTheme.colorScheme.outlineVariant
                    )
                }

                // Ir a la última página
                IconButton(
                    onClick = { estado.ultimaPagina(totalPaginas) },
                    enabled = paginaActual < totalPaginas,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LastPage,
                        contentDescription = "Última página",
                        tint = if (paginaActual < totalPaginas) AzulPrimario else MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogoSaltoPagina(
    paginaActual: Int,
    totalPaginas: Int,
    totalElementos: Int,
    alSeleccionarPagina: (Int) -> Unit,
    alCerrar: () -> Unit
) {
    Dialog(onDismissRequest = alCerrar) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Seleccionar Página",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$totalElementos elementos distribuidos en $totalPaginas páginas",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                val totalMostrar = minOf(totalPaginas, 100)
                val columnas = if (totalMostrar > 20) 5 else 4

                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnas),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(minOf(260.dp, ((totalMostrar / columnas) + 1) * 44.dp)),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(totalPaginas) { index ->
                        val numPagina = index + 1
                        val esActual = numPagina == paginaActual
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (esActual) AzulPrimario else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { alSeleccionarPagina(numPagina) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$numPagina",
                                fontSize = 13.sp,
                                fontWeight = if (esActual) FontWeight.Bold else FontWeight.Normal,
                                color = if (esActual) Blanco else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                TextButton(
                    onClick = alCerrar,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cerrar", fontWeight = FontWeight.Bold, color = AzulPrimario)
                }
            }
        }
    }
}
