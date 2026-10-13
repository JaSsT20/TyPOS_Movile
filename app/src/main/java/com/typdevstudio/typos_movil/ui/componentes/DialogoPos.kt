package com.typdevstudio.typos_movil.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.typdevstudio.typos_movil.ui.theme.GrisTexto

/**
 * Modal con la estética de la app (la misma de "¡Venta Completada!" o "¿Restaurar Base de Datos?"):
 * tarjeta redondeada sobre `surface`, ícono en un círculo de color suave, título en negrita, mensaje
 * centrado y botones [BotonPos] a todo el ancho.
 */
@Composable
fun DialogoPos(
    alDescartar: () -> Unit,
    icono: ImageVector,
    colorIcono: Color,
    titulo: String,
    mensaje: String,
    contenido: @Composable ColumnScope.() -> Unit = {},
    acciones: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = alDescartar) {
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
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(colorIcono.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icono,
                        contentDescription = null,
                        tint = colorIcono,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Text(
                    text = titulo,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = mensaje,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                contenido()

                Spacer(modifier = Modifier.height(2.dp))

                acciones()
            }
        }
    }
}

/** Botón secundario con borde (Cancelar), como en "¿Restaurar Base de Datos?". */
@Composable
fun BotonCancelarPos(
    texto: String,
    alHacerClic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = alHacerClic,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(texto, color = GrisTexto, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
