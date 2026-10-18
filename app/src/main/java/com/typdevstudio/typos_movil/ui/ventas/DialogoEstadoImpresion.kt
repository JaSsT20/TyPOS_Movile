package com.typdevstudio.typos_movil.ui.ventas

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.typdevstudio.typos_movil.datos.impresion.EtapaImpresion
import com.typdevstudio.typos_movil.ui.componentes.BotonPos
import com.typdevstudio.typos_movil.ui.componentes.VarianteBoton
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.RojoError
import com.typdevstudio.typos_movil.ui.theme.VerdeExito

private enum class EstadoPaso { PENDIENTE, EN_CURSO, HECHO, FALLIDO }

/**
 * Muestra paso a paso la comunicación con la impresora Bluetooth: a cuál se conecta,
 * si la conexión se logró, el envío y el resultado final (o el error con su detalle).
 */
@Composable
fun DialogoEstadoImpresion(
    estado: EstadoImpresionUi?,
    alCerrar: () -> Unit,
    alReintentar: () -> Unit
) {
    if (estado == null) return

    val exito = estado.terminado && estado.mensajeError == null
    val fallo = estado.terminado && estado.mensajeError != null

    Dialog(
        onDismissRequest = { if (estado.terminado) alCerrar() },
        properties = DialogProperties(
            dismissOnBackPress = estado.terminado,
            dismissOnClickOutside = estado.terminado
        )
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = when {
                        exito -> "Impresión lista"
                        fallo -> "No se pudo imprimir"
                        else -> "Imprimiendo ticket…"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        exito -> VerdeExito
                        fallo -> RojoError
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )

                TarjetaImpresora(estado)

                if (estado.direccionMac != null) {
                    PasosImpresion(estado)
                }

                if (estado.mensajeError != null) {
                    Text(
                        text = estado.mensajeError,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(RojoError.copy(alpha = 0.10f))
                            .padding(12.dp)
                    )
                }

                if (estado.terminado) {
                    Spacer(modifier = Modifier.height(2.dp))
                    if (fallo && estado.direccionMac != null) {
                        BotonPos(
                            texto = "Reintentar",
                            alHacerClic = alReintentar,
                            icono = Icons.Filled.Refresh,
                            variante = VarianteBoton.PRIMARIO
                        )
                    }
                    BotonPos(
                        texto = if (exito) "Aceptar" else "Cerrar",
                        alHacerClic = alCerrar,
                        variante = if (exito) VarianteBoton.EXITO else VarianteBoton.SECUNDARIO
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaImpresora(estado: EstadoImpresionUi) {
    val sinImpresora = estado.direccionMac == null && estado.terminado
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (sinImpresora) Icons.Filled.BluetoothDisabled else Icons.Filled.Bluetooth,
            contentDescription = null,
            tint = if (sinImpresora) RojoError else AzulPrimario,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = when {
                    sinImpresora -> "Sin impresora configurada"
                    estado.direccionMac == null -> "Buscando la impresora guardada…"
                    else -> estado.nombreImpresora ?: "Impresora Bluetooth"
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (estado.direccionMac != null) {
                Text(
                    text = estado.direccionMac,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PasosImpresion(estado: EstadoImpresionUi) {
    val nombre = estado.nombreImpresora ?: "la impresora"
    // Índice del paso que está ocurriendo (o en el que falló).
    val pasoActual = when (estado.etapa) {
        EtapaImpresion.CONECTANDO -> 0
        EtapaImpresion.CONECTADO, EtapaImpresion.ENVIANDO -> 1
        EtapaImpresion.FINALIZANDO -> 2
    }
    val pasos = listOf(
        if (pasoActual > 0) "Conectado con $nombre" else "Conectando con $nombre…",
        if (pasoActual > 1) "Impresión enviada" else "Enviando impresión…",
        if (estado.terminado && estado.mensajeError == null) "Impresión lista" else "Terminando de imprimir…"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        pasos.forEachIndexed { indice, texto ->
            val estadoPaso = when {
                estado.terminado && estado.mensajeError == null -> EstadoPaso.HECHO
                indice < pasoActual -> EstadoPaso.HECHO
                indice == pasoActual && estado.terminado -> EstadoPaso.FALLIDO
                indice == pasoActual -> EstadoPaso.EN_CURSO
                else -> EstadoPaso.PENDIENTE
            }
            FilaPaso(texto, estadoPaso)
        }
    }
}

@Composable
private fun FilaPaso(texto: String, estado: EstadoPaso) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            when (estado) {
                EstadoPaso.EN_CURSO -> CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = AzulPrimario,
                    strokeWidth = 2.dp
                )
                EstadoPaso.HECHO -> Icon(Icons.Filled.CheckCircle, null, tint = VerdeExito)
                EstadoPaso.FALLIDO -> Icon(Icons.Filled.ErrorOutline, null, tint = RojoError)
                EstadoPaso.PENDIENTE -> Icon(
                    Icons.Filled.RadioButtonUnchecked,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = texto,
            fontSize = 14.sp,
            fontWeight = if (estado == EstadoPaso.EN_CURSO) FontWeight.SemiBold else FontWeight.Normal,
            color = when (estado) {
                EstadoPaso.PENDIENTE -> MaterialTheme.colorScheme.onSurfaceVariant
                EstadoPaso.FALLIDO -> RojoError
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
