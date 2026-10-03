package com.typdevstudio.typos_movil.ui.licencia

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.typdevstudio.typos_movil.datos.repositorio.EstadoLicencia
import com.typdevstudio.typos_movil.ui.componentes.BotonPos
import com.typdevstudio.typos_movil.ui.componentes.CampoTextoPos
import com.typdevstudio.typos_movil.ui.componentes.VarianteBoton
import com.typdevstudio.typos_movil.ui.theme.AmarilloAdvertencia
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.AzulPrimarioClaro
import com.typdevstudio.typos_movil.ui.theme.Blanco
import com.typdevstudio.typos_movil.ui.theme.FondoClaro
import com.typdevstudio.typos_movil.ui.theme.GrisClaro
import com.typdevstudio.typos_movil.ui.theme.GrisMedio
import com.typdevstudio.typos_movil.ui.theme.GrisSecundario
import com.typdevstudio.typos_movil.ui.theme.GrisTexto
import com.typdevstudio.typos_movil.ui.theme.RojoError
import com.typdevstudio.typos_movil.ui.theme.VerdeExito
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DialogoLicenciaBloqueada(
    viewModel: LicenciaViewModel
) {
    val estadoUi by viewModel.uiState.collectAsState()

    val (titulo, subtitulo, icono, colorIcono) = when (val est = estadoUi.estado) {
        is EstadoLicencia.Vencida -> {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fechaVenc = sdf.format(Date(est.fechaVencimiento))
            Quadruple(
                "Licencia Vencida",
                "Tu licencia de TyPOS Móvil expiró el $fechaVenc. Para continuar facturando y usando el sistema, ingresa una nueva clave de activación.",
                Icons.Filled.Lock,
                RojoError
            )
        }
        is EstadoLicencia.RelojAlterado -> {
            Quadruple(
                "Fecha del Sistema Alterada",
                "Se detectó que la fecha u hora de tu teléfono fue cambiada hacia atrás. Por favor sincroniza la hora correcta de red para continuar.",
                Icons.Filled.Schedule,
                AmarilloAdvertencia
            )
        }
        is EstadoLicencia.SinLicencia -> {
            Quadruple(
                "Activación Requerida",
                "Bienvenido a TyPOS Móvil. Por favor ingresa tu clave de licencia de 16 caracteres para activar el sistema.",
                Icons.Filled.Key,
                AzulPrimario
            )
        }
        is EstadoLicencia.Activa -> return // No mostrar diálogo si está activa
    }

    Dialog(
        onDismissRequest = { /* Bloqueo estricto: no se cierra hasta ingresar licencia válida */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Blanco),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Ícono central de estado
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(colorIcono.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icono,
                        contentDescription = null,
                        tint = colorIcono,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = titulo,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = GrisTexto,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = subtitulo,
                    fontSize = 13.sp,
                    color = GrisSecundario,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Campo para ingresar la clave
                CampoTextoPos(
                    valor = estadoUi.claveIngresada,
                    alCambiarValor = { viewModel.onClaveIngresadaCambiada(it) },
                    etiqueta = "Clave de Licencia (16 caracteres)",
                    iconoInicio = Icons.Filled.Key,
                    mensajeError = estadoUi.mensajeError
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Botón de activación
                BotonPos(
                    texto = "Activar Licencia Ahora",
                    alHacerClic = { viewModel.activarLicencia() },
                    estaCargando = estadoUi.estaActivando,
                    variante = VarianteBoton.EXITO,
                    icono = Icons.Filled.Key,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Nota informativa de contacto de soporte
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(FondoClaro)
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SupportAgent,
                            contentDescription = null,
                            tint = AzulPrimario,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Solicita tu nueva clave al equipo de soporte de TyPOS para renovar tu suscripción.",
                            fontSize = 11.sp,
                            color = GrisSecundario,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
