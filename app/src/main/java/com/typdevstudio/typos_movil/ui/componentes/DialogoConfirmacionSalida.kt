package com.typdevstudio.typos_movil.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.Blanco
import com.typdevstudio.typos_movil.ui.theme.GrisTexto

@Composable
fun DialogoConfirmacionSalida(
    mostrar: Boolean,
    alCerrarSesion: () -> Unit,
    alSalirApp: () -> Unit,
    alDescartar: () -> Unit
) {
    if (!mostrar) return

    Dialog(onDismissRequest = alDescartar) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "¿Qué deseas hacer?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Elige si deseas cerrar tu sesión actual, salir de la aplicación o continuar trabajando.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BotonPos(
                        texto = "Cerrar sesión",
                        alHacerClic = {
                            alDescartar()
                            alCerrarSesion()
                        },
                        variante = VarianteBoton.SECUNDARIO,
                        icono = Icons.Filled.Logout
                    )

                    BotonPos(
                        texto = "Salir de la app",
                        alHacerClic = {
                            alDescartar()
                            alSalirApp()
                        },
                        variante = VarianteBoton.PELIGRO,
                        icono = Icons.Filled.ExitToApp
                    )

                    BotonPos(
                        texto = "Quedarme en la app",
                        alHacerClic = alDescartar,
                        variante = VarianteBoton.PRIMARIO
                    )
                }
            }
        }
    }
}
