package com.typdevstudio.typos_movil.ui.componentes

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.Blanco
import com.typdevstudio.typos_movil.ui.theme.GrisSecundario
import com.typdevstudio.typos_movil.ui.theme.RojoError
import com.typdevstudio.typos_movil.ui.theme.VerdeExito

enum class VarianteBoton {
    PRIMARIO,
    SECUNDARIO,
    PELIGRO,
    EXITO
}

@Composable
fun BotonPos(
    texto: String,
    alHacerClic: () -> Unit,
    modifier: Modifier = Modifier,
    variante: VarianteBoton = VarianteBoton.PRIMARIO,
    estaHabilitado: Boolean = true,
    estaCargando: Boolean = false,
    icono: ImageVector? = null
) {
    val colorFondo = when (variante) {
        VarianteBoton.PRIMARIO -> AzulPrimario
        VarianteBoton.SECUNDARIO -> GrisSecundario
        VarianteBoton.PELIGRO -> RojoError
        VarianteBoton.EXITO -> VerdeExito
    }

    Button(
        onClick = alHacerClic,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = estaHabilitado && !estaCargando,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colorFondo,
            contentColor = Blanco,
            disabledContainerColor = colorFondo.copy(alpha = 0.5f),
            disabledContentColor = Blanco.copy(alpha = 0.7f)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (estaCargando) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Blanco,
                strokeWidth = 2.5.dp
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icono != null) {
                    androidx.compose.material3.Icon(
                        imageVector = icono,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = texto,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
