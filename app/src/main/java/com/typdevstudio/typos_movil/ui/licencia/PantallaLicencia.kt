package com.typdevstudio.typos_movil.ui.licencia

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.typdevstudio.typos_movil.ui.componentes.BotonCancelarPos
import com.typdevstudio.typos_movil.ui.componentes.BotonPos
import com.typdevstudio.typos_movil.ui.componentes.DialogoPos
import com.typdevstudio.typos_movil.ui.componentes.CampoTextoPos
import com.typdevstudio.typos_movil.ui.componentes.VarianteBoton
import com.typdevstudio.typos_movil.ui.theme.AmarilloAdvertencia
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.Blanco
import com.typdevstudio.typos_movil.ui.theme.CelesteInformacion
import com.typdevstudio.typos_movil.ui.theme.RojoError
import com.typdevstudio.typos_movil.ui.theme.VerdeExito
import com.typdevstudio.typos_movil.utilidades.licencia.SituacionLicencia
import com.typdevstudio.typos_movil.utilidades.licencia.nube.EstadoLicencia

/** Pantalla de licencia (solo administrador): estado, datos del plan, activar, validar ahora y desactivar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaLicencia(
    viewModel: LicenciaViewModel,
    alVolver: () -> Unit,
) {
    val situacion by viewModel.situacion.collectAsState()
    val estado by viewModel.uiState.collectAsState()

    if (estado.confirmarDesactivacion) {
        DialogoPos(
            alDescartar = { viewModel.cancelarDesactivacion() },
            icono = Icons.Filled.LinkOff,
            colorIcono = RojoError,
            titulo = "¿Desactivar este teléfono?",
            mensaje = "Se liberará su lugar en la licencia y este teléfono quedará en modo solo consulta hasta que " +
                "vuelvas a ingresar la clave. Tus ventas y productos no se borran.",
            acciones = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BotonCancelarPos(
                        texto = "Cancelar",
                        alHacerClic = { viewModel.cancelarDesactivacion() },
                        modifier = Modifier.weight(1f),
                    )
                    BotonPos(
                        texto = "Desactivar",
                        alHacerClic = { viewModel.confirmarDesactivacion() },
                        icono = Icons.Filled.LinkOff,
                        variante = VarianteBoton.PELIGRO,
                        modifier = Modifier.weight(1.1f),
                    )
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Licencia", fontWeight = FontWeight.Bold, color = Blanco) },
                navigationIcon = {
                    IconButton(onClick = alVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Blanco)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AzulPrimario),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValores ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValores)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            estado.mensaje?.let { mensaje ->
                MensajeResultado(mensaje, estado.esError, alCerrar = { viewModel.limpiarMensaje() })
            }

            TarjetaEstado(situacion)

            situacion.payload?.let { payload ->
                Tarjeta {
                    Titulo("Datos de la licencia")
                    Fila("Cliente", payload.cliente)
                    Fila("Plan", TextosLicencia.nombrePlan(payload.plan))
                    Fila("Módulos", payload.modulos.joinToString(", ") { TextosLicencia.nombreModulo(it) }.ifEmpty { "—" })
                    Fila("Usuarios", TextosLicencia.usoUsuarios(estado.usuariosActivos, payload.maxUsuarios))
                    val activos = estado.usuariosActivos
                    if (activos != null && activos > payload.maxUsuarios) {
                        Text(
                            "Tienes más usuarios de los que permite tu plan. Puedes seguir usándolos, pero no crear nuevos.",
                            fontSize = 12.sp,
                            color = AmarilloAdvertencia,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Fila("Vence", TextosLicencia.fechaDesdeDia(payload.vence))
                    Fila("Última validación", TextosLicencia.fechaHoraDesdeMs(situacion.ultimaValidacionMs))
                    Fila("Válida sin conexión hasta", TextosLicencia.fechaHoraDesdeMs(payload.validoHastaMs))
                    situacion.clave?.let { Fila("Clave", it) }
                }
            }

            if (situacion.clave != null) {
                Tarjeta {
                    BotonPos(
                        texto = "Validar ahora",
                        alHacerClic = { viewModel.validarAhora() },
                        estaCargando = estado.estaValidando,
                        icono = Icons.Filled.Sync,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    BotonPos(
                        texto = "Desactivar este teléfono",
                        alHacerClic = { viewModel.pedirDesactivacion() },
                        estaCargando = estado.estaDesactivando,
                        icono = Icons.Filled.LinkOff,
                        variante = VarianteBoton.PELIGRO,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Se puede ingresar una clave si no hay licencia nueva o si la actual no permite trabajar.
            if (situacion.clave == null || !situacion.permiteEmitir) {
                Tarjeta {
                    Titulo(if (situacion.clave == null) "Activar licencia" else "Ingresar otra clave")
                    CampoTextoPos(
                        valor = estado.claveIngresada,
                        alCambiarValor = { viewModel.onClaveCambiada(it) },
                        etiqueta = "Clave de licencia (XXXX-XXXX-XXXX)",
                        iconoInicio = Icons.Filled.Key,
                        opcionesTeclado = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    )
                    CampoTextoPos(
                        valor = estado.nombreDispositivo,
                        alCambiarValor = { viewModel.onNombreDispositivoCambiado(it) },
                        etiqueta = "Nombre de este teléfono (ej. Caja 1)",
                        iconoInicio = Icons.Filled.PhoneAndroid,
                    )
                    BotonPos(
                        texto = "Activar licencia",
                        alHacerClic = { viewModel.activar() },
                        estaCargando = estado.estaActivando,
                        icono = Icons.Filled.Verified,
                        variante = VarianteBoton.EXITO,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Necesitas conexión a internet para activar. Después la app funciona sin conexión.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Tarjeta {
                Fila("Identificador del teléfono", estado.identificadorDispositivo.ifEmpty { "…" }, monoespaciado = true)
                Text(
                    "Compártelo con soporte si te lo piden.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TarjetaEstado(situacion: SituacionLicencia) {
    val (etiqueta, color, descripcion) = when {
        !situacion.verificada -> Triple("VERIFICANDO…", CelesteInformacion, "Revisando la licencia de este teléfono.")
        !situacion.permiteEmitir -> Triple("SOLO CONSULTA", RojoError, situacion.mensajeRestriccion())
        situacion.usaLicenciaAntigua -> Triple(
            "LICENCIA ANTERIOR",
            CelesteInformacion,
            "Tu licencia anterior sigue vigente hasta ${TextosLicencia.fechaDesdeMs(situacion.antigua?.venceMs)}. " +
                "Activa tu nueva licencia con la clave que te entregó tu proveedor.",
        )
        else -> when (val nube = situacion.nube) {
            is EstadoLicencia.EnCortesia -> Triple("VENCIDA · EN CORTESÍA", AmarilloAdvertencia, "Tu licencia venció. " + TextosLicencia.textoCortesia(nube.diasRestantes))
            is EstadoLicencia.ActivaPorValidar -> Triple("ACTIVA · POR VALIDAR", AmarilloAdvertencia, "Conéctate a internet para validar tu licencia.")
            else -> Triple("ACTIVA", VerdeExito, "Todo en orden.")
        }
    }
    Tarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Estado:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
            Surface(color = color, shape = RoundedCornerShape(6.dp)) {
                Text(
                    "● $etiqueta",
                    fontSize = 11.sp,
                    color = Blanco,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
        Text(descripcion, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun MensajeResultado(mensaje: String, esError: Boolean, alCerrar: () -> Unit) {
    val color = if (esError) RojoError else VerdeExito
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(mensaje, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            IconButton(onClick = alCerrar) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = color, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun Tarjeta(contenido: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            contenido()
        }
    }
}

@Composable
private fun Titulo(texto: String) {
    Text(texto, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AzulPrimario)
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun Fila(etiqueta: String, valor: String, monoespaciado: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            valor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = if (monoespaciado) FontFamily.Monospace else null,
        )
    }
}
