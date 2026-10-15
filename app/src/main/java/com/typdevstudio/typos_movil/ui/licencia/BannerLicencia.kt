package com.typdevstudio.typos_movil.ui.licencia

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.typdevstudio.typos_movil.BuildConfig
import com.typdevstudio.typos_movil.datos.local.AppBaseDatos
import com.typdevstudio.typos_movil.datos.local.GestorSesion
import com.typdevstudio.typos_movil.ui.theme.AmarilloAdvertencia
import com.typdevstudio.typos_movil.ui.theme.CelesteInformacion
import com.typdevstudio.typos_movil.ui.theme.RojoError
import com.typdevstudio.typos_movil.utilidades.licencia.LicenciaGuard
import com.typdevstudio.typos_movil.utilidades.licencia.SituacionLicencia
import com.typdevstudio.typos_movil.utilidades.licencia.nube.EstadoLicencia
import com.typdevstudio.typos_movil.utilidades.licencia.nube.ResultadoLicencia
import com.typdevstudio.typos_movil.utilidades.licencia.nube.ResultadoRegistro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Aviso de licencia para las pantallas principales. No se muestra si la licencia está activa y al día. */
private data class Aviso(
    val titulo: String,
    val texto: String,
    val color: Color,
    val icono: ImageVector,
    val textoBotonLicencia: String,
    val mostrarValidar: Boolean,
    /** Registro del negocio en revisión: revisar si ya llegó la licencia y avisar por WhatsApp al proveedor. */
    val esRegistroPendiente: Boolean = false,
)

private fun avisoPara(situacion: SituacionLicencia): Aviso? {
    if (!situacion.verificada) return null
    val tieneClave = situacion.clave != null
    if (situacion.registroPendiente) {
        return Aviso(
            titulo = "Registro en revisión",
            texto = situacion.mensajeRestriccion(),
            color = CelesteInformacion,
            icono = Icons.Filled.HourglassTop,
            textoBotonLicencia = "Ver licencia",
            mostrarValidar = false,
            esRegistroPendiente = true,
        )
    }
    if (!situacion.permiteEmitir) {
        return Aviso(
            titulo = "Modo solo consulta",
            texto = situacion.mensajeRestriccion(),
            color = RojoError,
            icono = Icons.Filled.Lock,
            textoBotonLicencia = if (tieneClave) "Ver licencia" else "Ingresar licencia",
            mostrarValidar = tieneClave,
        )
    }
    if (situacion.usaLicenciaAntigua) {
        return Aviso(
            titulo = "Activa tu nueva licencia",
            texto = "Tu licencia actual sigue funcionando hasta ${TextosLicencia.fechaDesdeMs(situacion.antigua?.venceMs)}. " +
                "Ingresa la clave de tu nueva licencia cuando la recibas.",
            color = CelesteInformacion,
            icono = Icons.Filled.Info,
            textoBotonLicencia = "Activar",
            mostrarValidar = false,
        )
    }
    return when (val estado = situacion.nube) {
        is EstadoLicencia.EnCortesia -> Aviso(
            titulo = "Tu licencia venció",
            texto = TextosLicencia.textoCortesia(estado.diasRestantes),
            color = AmarilloAdvertencia,
            icono = Icons.Filled.Warning,
            textoBotonLicencia = "Ver licencia",
            mostrarValidar = true,
        )
        is EstadoLicencia.ActivaPorValidar -> Aviso(
            titulo = "Valida tu licencia",
            texto = "Conéctate a internet para validar tu licencia.",
            color = AmarilloAdvertencia,
            icono = Icons.Filled.Warning,
            textoBotonLicencia = "Ver licencia",
            mostrarValidar = true,
        )
        else -> null
    }
}

@Composable
fun BannerLicencia(
    alAbrirLicencia: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val situacion by LicenciaGuard.situacion.collectAsState()
    val aviso = avisoPara(situacion) ?: return
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    var estaValidando by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = aviso.color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, aviso.color.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(aviso.icono, contentDescription = null, tint = aviso.color, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(aviso.titulo, color = aviso.color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Text(
                text = aviso.texto,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp, end = 6.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (aviso.mostrarValidar) {
                    TextButton(
                        enabled = !estaValidando,
                        onClick = {
                            estaValidando = true
                            alcance.launch {
                                val resultado = LicenciaGuard.validarAhora()
                                estaValidando = false
                                Toast.makeText(
                                    contexto,
                                    LicenciaViewModel.mensajeDe(resultado, "Licencia validada correctamente."),
                                    if (resultado is ResultadoLicencia.Exito) Toast.LENGTH_SHORT else Toast.LENGTH_LONG,
                                ).show()
                            }
                        },
                    ) {
                        if (estaValidando) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = aviso.color)
                        } else {
                            Text("Validar ahora", color = aviso.color, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (aviso.esRegistroPendiente) {
                    TextButton(
                        enabled = !estaValidando,
                        onClick = {
                            estaValidando = true
                            alcance.launch {
                                val resultado = LicenciaGuard.consultarRegistro()
                                estaValidando = false
                                Toast.makeText(contexto, mensajeDeRegistro(resultado), Toast.LENGTH_LONG).show()
                            }
                        },
                    ) {
                        if (estaValidando) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = aviso.color)
                        } else {
                            Text("Revisar ahora", color = aviso.color, fontWeight = FontWeight.Bold)
                        }
                    }
                    TextButton(onClick = { alcance.launch { avisarRegistroPorWhatsApp(contexto) } }) {
                        Text("Avisar por WhatsApp", color = aviso.color, fontWeight = FontWeight.Bold)
                    }
                } else {
                    TextButton(onClick = alAbrirLicencia) {
                        Text(aviso.textoBotonLicencia, color = aviso.color, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun mensajeDeRegistro(resultado: ResultadoRegistro): String = when (resultado) {
    is ResultadoRegistro.Asignado ->
        if (resultado.licencia is ResultadoLicencia.Exito) "¡Tu licencia está lista! Ya puedes vender."
        else LicenciaViewModel.mensajeDe(resultado.licencia, "")
    is ResultadoRegistro.Pendiente -> "Tu registro sigue en revisión. Te avisaremos cuando tu licencia esté lista."
    is ResultadoRegistro.Rechazado -> resultado.mensaje
    is ResultadoRegistro.Rechazada -> resultado.mensaje
    is ResultadoRegistro.SinConexion -> resultado.mensaje
    ResultadoRegistro.SinRegistro -> "Este teléfono no tiene un registro pendiente."
}

/** Abre WhatsApp con el aviso al proveedor (datos del negocio guardados en la configuración e ID del teléfono). */
private suspend fun avisarRegistroPorWhatsApp(contexto: Context) {
    val negocio = withContext(Dispatchers.IO) {
        AppBaseDatos.obtenerBaseDatos(contexto).configuracionNegocioDao().obtenerConfiguracionDirecta()
    }
    val mensaje = LicenciaViewModel.mensajeWhatsAppRegistro(
        negocio?.nombreNegocio, negocio?.telefono, GestorSesion.usuarioActivo.value?.nombreCompleto,
        LicenciaGuard.identificadorDispositivo(),
    )
    abrirWhatsApp(contexto, BuildConfig.WHATSAPP_REGISTROS, mensaje)
}
