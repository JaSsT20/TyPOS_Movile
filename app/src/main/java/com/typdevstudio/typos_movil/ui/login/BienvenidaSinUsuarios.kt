package com.typdevstudio.typos_movil.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.typdevstudio.typos_movil.ui.componentes.BotonCancelarPos
import com.typdevstudio.typos_movil.ui.componentes.BotonPos
import com.typdevstudio.typos_movil.ui.componentes.CampoTextoPos
import com.typdevstudio.typos_movil.ui.componentes.DialogoPos
import com.typdevstudio.typos_movil.ui.componentes.VarianteBoton
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.CelesteInformacion
import com.typdevstudio.typos_movil.ui.theme.RojoError
import com.typdevstudio.typos_movil.ui.theme.VerdeExito

/**
 * Lo que se muestra en lugar del inicio de sesión cuando el teléfono no tiene usuarios: crear el primer
 * administrador o, si el negocio ya tiene licencia, activarla para traer sus usuarios.
 */
@Composable
fun TarjetaBienvenidaSinUsuarios(estado: BienvenidaUiState, viewModel: LoginViewModel) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("¡Bienvenido!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "Este teléfono todavía no tiene usuarios. ¿Cómo quieres empezar?",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            estado.aviso?.let { aviso ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CelesteInformacion.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(aviso, color = CelesteInformacion, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            BotonPos(
                texto = "Ya tengo licencia",
                alHacerClic = { viewModel.abrirBienvenida(FormularioBienvenida.ACTIVAR_LICENCIA) },
                icono = Icons.Filled.Verified,
                variante = VarianteBoton.EXITO,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Si tu negocio ya usa TyPOS en otro teléfono: activa la licencia y entra con tu usuario de siempre.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            BotonPos(
                texto = "Crear administrador",
                alHacerClic = { viewModel.abrirBienvenida(FormularioBienvenida.CREAR_ADMINISTRADOR) },
                icono = Icons.Filled.AdminPanelSettings,
                variante = VarianteBoton.PRIMARIO,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Si es la primera vez: crea el usuario dueño del negocio. Después podrás agregar a tus cajeros.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    when (estado.formulario) {
        FormularioBienvenida.CREAR_ADMINISTRADOR -> DialogoCrearAdministrador(estado, viewModel)
        FormularioBienvenida.ACTIVAR_LICENCIA -> DialogoActivarLicencia(estado, viewModel)
        null -> Unit
    }
}

@Composable
private fun DialogoCrearAdministrador(estado: BienvenidaUiState, viewModel: LoginViewModel) {
    DialogoPos(
        alDescartar = { viewModel.cerrarBienvenida() },
        icono = Icons.Filled.AdminPanelSettings,
        colorIcono = AzulPrimario,
        titulo = "Crear administrador",
        mensaje = "Este usuario podrá entrar a Configuración, Licencia y Usuarios.",
        contenido = {
            CampoTextoPos(
                valor = estado.nombreUsuario,
                alCambiarValor = { texto -> viewModel.editarBienvenida { it.copy(nombreUsuario = texto.take(50)) } },
                etiqueta = "Usuario (para iniciar sesión)",
                iconoInicio = Icons.Filled.Person,
            )
            CampoTextoPos(
                valor = estado.nombreCompleto,
                alCambiarValor = { texto -> viewModel.editarBienvenida { it.copy(nombreCompleto = texto.take(100)) } },
                etiqueta = "Nombre completo",
                iconoInicio = Icons.Filled.Badge,
            )
            CampoTextoPos(
                valor = estado.clave,
                alCambiarValor = { texto -> viewModel.editarBienvenida { it.copy(clave = texto.take(64)) } },
                etiqueta = "Contraseña o PIN",
                iconoInicio = Icons.Filled.Lock,
                esContrasena = true,
            )
            CampoTextoPos(
                valor = estado.confirmarClave,
                alCambiarValor = { texto -> viewModel.editarBienvenida { it.copy(confirmarClave = texto.take(64)) } },
                etiqueta = "Confirmar contraseña",
                iconoInicio = Icons.Filled.Lock,
                esContrasena = true,
            )
            ErrorFormulario(estado.error)
        },
        acciones = {
            BotonesFormulario(
                textoAceptar = "Crear y entrar",
                estaGuardando = estado.estaGuardando,
                alAceptar = { viewModel.crearAdministrador() },
                alCancelar = { viewModel.cerrarBienvenida() },
            )
        },
    )
}

@Composable
private fun DialogoActivarLicencia(estado: BienvenidaUiState, viewModel: LoginViewModel) {
    DialogoPos(
        alDescartar = { viewModel.cerrarBienvenida() },
        icono = Icons.Filled.Verified,
        colorIcono = VerdeExito,
        titulo = "Activar licencia",
        mensaje = "Escribe la clave que te entregó tu proveedor. Se descargarán los usuarios de tu negocio. Necesitas internet.",
        contenido = {
            CampoTextoPos(
                valor = estado.claveLicencia,
                // Se guarda tal cual: transformarla mientras se escribe hace que el teclado pierda caracteres.
                alCambiarValor = { texto -> viewModel.editarBienvenida { it.copy(claveLicencia = texto.take(30)) } },
                etiqueta = "Clave de licencia (XXXX-XXXX-XXXX)",
                iconoInicio = Icons.Filled.Key,
                opcionesTeclado = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            )
            CampoTextoPos(
                valor = estado.nombreTelefono,
                alCambiarValor = { texto -> viewModel.editarBienvenida { it.copy(nombreTelefono = texto.take(100)) } },
                etiqueta = "Nombre de este teléfono (ej. Caja 1)",
                iconoInicio = Icons.Filled.PhoneAndroid,
            )
            ErrorFormulario(estado.error)
        },
        acciones = {
            BotonesFormulario(
                textoAceptar = "Activar",
                estaGuardando = estado.estaGuardando,
                alAceptar = { viewModel.activarLicencia() },
                alCancelar = { viewModel.cerrarBienvenida() },
            )
        },
    )
}

@Composable
private fun ErrorFormulario(error: String?) {
    error?.let {
        Text(it, color = RojoError, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun BotonesFormulario(textoAceptar: String, estaGuardando: Boolean, alAceptar: () -> Unit, alCancelar: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BotonCancelarPos(texto = "Cancelar", alHacerClic = alCancelar, modifier = Modifier.weight(1f))
        BotonPos(
            texto = textoAceptar,
            alHacerClic = alAceptar,
            estaCargando = estaGuardando,
            variante = VarianteBoton.EXITO,
            modifier = Modifier.weight(1.2f),
        )
    }
}
