package com.typdevstudio.typos_movil.ui.configuracion.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PhonelinkErase
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
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
import com.typdevstudio.typos_movil.ui.componentes.BotonCancelarPos
import com.typdevstudio.typos_movil.ui.componentes.BotonPos
import com.typdevstudio.typos_movil.ui.componentes.DialogoPos
import com.typdevstudio.typos_movil.ui.componentes.VarianteBoton
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.GrisTexto
import com.typdevstudio.typos_movil.ui.theme.RojoError
import com.typdevstudio.typos_movil.ui.theme.VerdeExito
import com.typdevstudio.typos_movil.utilidades.backup.ArchivoBackupUi
import com.typdevstudio.typos_movil.utilidades.backup.ConfirmacionRestauracion
import com.typdevstudio.typos_movil.utilidades.backup.VarianteRestauracion

private val NaranjaAviso = Color(0xFFE65100)

/**
 * Confirmación antes de restaurar. Tres variantes: normal, "este teléfono se agregará a su licencia" (con cupo) y
 * desvinculación (sin cupo: el otro teléfono queda en modo consulta).
 */
@Composable
fun DialogoConfirmarRestauracion(
    confirmacion: ConfirmacionRestauracion,
    alConfirmar: () -> Unit,
    alDescartar: () -> Unit
) {
    val backup = confirmacion.backup
    val reemplazo = "Los datos actuales de este teléfono se reemplazarán por los de la copia."
    val (icono, titulo, mensaje, textoBoton) = when (val variante = confirmacion.variante) {
        VarianteRestauracion.Normal -> Cuarteto(
            Icons.Filled.Warning,
            "¿Restaurar esta copia?",
            if (confirmacion.deOtroTelefono) "Esta copia viene de ${backup.nombreOrigen}. $reemplazo" else reemplazo,
            "Restaurar",
        )
        is VarianteRestauracion.AgregarTelefono -> Cuarteto(
            Icons.Filled.Restore,
            "¿Restaurar en este teléfono?",
            "Esta copia viene de ${variante.nombreOrigen}. Este teléfono se agregará a su licencia " +
                "(${variante.quedariaEn} de ${variante.maximo} teléfonos) y el otro seguirá funcionando. $reemplazo",
            "Restaurar",
        )
        is VarianteRestauracion.Desvincular -> Cuarteto(
            Icons.Filled.PhonelinkErase,
            "¿Restaurar en este teléfono?",
            "Esta copia viene de ${variante.nombreOrigen}. Su licencia permite ${variante.maximo} " +
                "${if (variante.maximo == 1) "teléfono" else "teléfonos"} y ya está en uso. Al restaurar, " +
                "${variante.nombreOrigen} se desvinculará de su licencia y quedará en modo consulta: podrá ver sus datos, " +
                "pero no vender. $reemplazo",
            "Desvincular y restaurar",
        )
    }

    DialogoPos(
        alDescartar = alDescartar,
        icono = icono,
        colorIcono = NaranjaAviso,
        titulo = titulo,
        mensaje = mensaje,
        contenido = {
            ResumenCopia(backup)
            Text(
                text = "Las ventas hechas después de la fecha de la copia no se recuperan. Si tiene datos recientes " +
                    "en este teléfono que no quiere perder, cree una copia antes de continuar.",
                fontSize = 11.sp,
                color = NaranjaAviso,
                lineHeight = 15.sp,
                textAlign = TextAlign.Center
            )
        },
        acciones = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BotonPos(
                    texto = textoBoton,
                    icono = Icons.Filled.Restore,
                    alHacerClic = alConfirmar,
                    variante = VarianteBoton.PELIGRO,
                    modifier = Modifier.fillMaxWidth()
                )
                BotonCancelarPos(texto = "Cancelar", alHacerClic = alDescartar)
            }
        }
    )
}

@Composable
private fun ResumenCopia(backup: ArchivoBackupUi) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = AzulPrimario.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Copia del ${backup.fechaFormateada}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AzulPrimario,
                textAlign = TextAlign.Center
            )
            Text(text = backup.origenTexto, fontSize = 11.sp, color = GrisTexto, textAlign = TextAlign.Center)
            backup.resumen?.let { Text(text = it, fontSize = 11.sp, color = GrisTexto, textAlign = TextAlign.Center) }
        }
    }
}

/** Advertencia antes de compartir: el archivo lleva todos los datos del negocio. */
@Composable
fun DialogoAdvertenciaCompartir(
    alConfirmar: () -> Unit,
    alDescartar: () -> Unit
) {
    DialogoPos(
        alDescartar = alDescartar,
        icono = Icons.Filled.Share,
        colorIcono = AzulPrimario,
        titulo = "Compartir copia de seguridad",
        mensaje = "Esta copia contiene todos los datos de su negocio: ventas, productos, fotos y usuarios. " +
            "Envíela solo a usted mismo o a personas de confianza.",
        acciones = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BotonPos(
                    texto = "Compartir",
                    icono = Icons.Filled.Share,
                    alHacerClic = alConfirmar,
                    variante = VarianteBoton.PRIMARIO,
                    modifier = Modifier.fillMaxWidth()
                )
                BotonCancelarPos(texto = "Cancelar", alHacerClic = alDescartar)
            }
        }
    )
}

/** Aviso final de la restauración. Al cerrarlo, la app se reinicia para cargar los datos. */
@Composable
fun DialogoReinicio(
    mensaje: String,
    esError: Boolean,
    alReiniciar: () -> Unit
) {
    DialogoPos(
        alDescartar = alReiniciar,
        icono = if (esError) Icons.Filled.Error else Icons.Filled.CheckCircle,
        colorIcono = if (esError) RojoError else VerdeExito,
        titulo = if (esError) "No se pudo restaurar" else "¡Copia restaurada!",
        mensaje = mensaje,
        acciones = {
            BotonPos(
                texto = "Aceptar",
                alHacerClic = alReiniciar,
                variante = if (esError) VarianteBoton.PRIMARIO else VarianteBoton.EXITO,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

private data class Cuarteto<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
