package com.typdevstudio.typos_movil.ui.configuracion.backup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.typdevstudio.typos_movil.ui.componentes.BotonPos
import com.typdevstudio.typos_movil.ui.componentes.VarianteBoton
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.GrisTexto
import com.typdevstudio.typos_movil.ui.theme.RojoError
import com.typdevstudio.typos_movil.utilidades.backup.ArchivoBackupUi

@Composable
fun DialogoConfirmarEliminacionBackup(
    backup: ArchivoBackupUi,
    alConfirmar: () -> Unit,
    alDescartar: () -> Unit
) {
    Dialog(onDismissRequest = alDescartar) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(RojoError.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        tint = RojoError,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Text(
                    text = "¿Eliminar Copia de Seguridad?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Estás a punto de eliminar permanentemente la siguiente copia de seguridad:",
                    fontSize = 13.sp,
                    color = GrisTexto,
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = RojoError.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = backup.nombreVisual,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = RojoError
                        )
                        Text(
                            text = "${backup.fechaFormateada} • ${backup.tamanoFormateado}",
                            fontSize = 11.sp,
                            color = GrisTexto
                        )
                    }
                }

                Text(
                    text = "Esta acción no se puede deshacer.",
                    fontSize = 12.sp,
                    color = RojoError,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = alDescartar,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar", color = GrisTexto, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    BotonPos(
                        texto = "Eliminar",
                        icono = Icons.Filled.Delete,
                        alHacerClic = alConfirmar,
                        variante = VarianteBoton.PELIGRO,
                        modifier = Modifier.weight(1.1f)
                    )
                }
            }
        }
    }
}
