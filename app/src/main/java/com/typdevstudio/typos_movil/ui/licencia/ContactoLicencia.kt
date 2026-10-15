package com.typdevstudio.typos_movil.ui.licencia

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.typdevstudio.typos_movil.BuildConfig
import com.typdevstudio.typos_movil.ui.componentes.BotonPos
import com.typdevstudio.typos_movil.ui.componentes.VarianteBoton
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.GrisTexto

/**
 * Recuadro de contacto de T&P DevStudio (nombre y teléfono) para los modales de licencia,
 * con los botones "Llamar" y "WhatsApp".
 *
 * @param mensajeWhatsApp texto que queda escrito en el chat.
 * @param alUsarContacto se llama antes de abrir el marcador o WhatsApp (por ejemplo, para cerrar el modal).
 */
@Composable
fun BloqueContactoLicencia(
    mensajeWhatsApp: String,
    alUsarContacto: () -> Unit = {},
) {
    val contexto = LocalContext.current
    val telefono = BuildConfig.CONTACTO_LICENCIA_TELEFONO

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = AzulPrimario.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text("Comunícate con", fontSize = 12.sp, color = GrisTexto)
            Text(BuildConfig.CONTACTO_LICENCIA_NOMBRE, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AzulPrimario)
            if (telefono.isNotBlank()) {
                Text(telefono, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AzulPrimario)
            }
        }
    }

    if (telefono.isNotBlank()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BotonPos(
                texto = "Llamar",
                alHacerClic = {
                    alUsarContacto()
                    llamar(contexto, telefono)
                },
                icono = Icons.Filled.Call,
                variante = VarianteBoton.PRIMARIO,
                modifier = Modifier.weight(1f)
            )
            BotonPos(
                texto = "WhatsApp",
                alHacerClic = {
                    alUsarContacto()
                    abrirWhatsApp(contexto, telefono, mensajeWhatsApp)
                },
                icono = Icons.AutoMirrored.Filled.Chat,
                variante = VarianteBoton.EXITO,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Abre el marcador con el número (no llama solo ni necesita permisos). */
private fun llamar(contexto: Context, telefono: String) {
    val numero = telefono.filter { it.isDigit() || it == '+' }
    try {
        contexto.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$numero")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        Toast.makeText(contexto, "No se pudo abrir el teléfono. Llámanos al $telefono.", Toast.LENGTH_LONG).show()
    }
}

/** Abre el chat de WhatsApp con ese número y el mensaje escrito (wa.me abre la app o, si no está, el navegador). */
internal fun abrirWhatsApp(contexto: Context, telefono: String, mensaje: String) {
    val numero = telefono.filter { it.isDigit() }
    val uri = Uri.parse("https://wa.me/$numero?text=${Uri.encode(mensaje)}")
    try {
        contexto.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        Toast.makeText(contexto, "No se pudo abrir WhatsApp. Escríbenos al $telefono.", Toast.LENGTH_LONG).show()
    }
}
