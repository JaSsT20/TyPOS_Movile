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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.typdevstudio.typos_movil.BuildConfig
import com.typdevstudio.typos_movil.ui.componentes.BotonCancelarPos
import com.typdevstudio.typos_movil.ui.componentes.BotonPos
import com.typdevstudio.typos_movil.ui.componentes.CampoTextoPos
import com.typdevstudio.typos_movil.ui.componentes.DialogoPos
import com.typdevstudio.typos_movil.ui.componentes.VarianteBoton
import com.typdevstudio.typos_movil.ui.licencia.abrirWhatsApp
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.CelesteInformacion
import com.typdevstudio.typos_movil.ui.theme.RojoError
import com.typdevstudio.typos_movil.ui.theme.VerdeExito
import kotlinx.coroutines.launch

/**
 * Lo que se muestra en lugar del inicio de sesión cuando el teléfono no tiene usuarios: registrar el negocio (y su
 * administrador) para que el proveedor le asigne una licencia o, si el negocio ya tiene licencia, activarla para
 * traer sus usuarios.
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
                texto = "Registrar mi negocio",
                alHacerClic = { viewModel.abrirBienvenida(FormularioBienvenida.REGISTRAR_NEGOCIO) },
                icono = Icons.Filled.Storefront,
                variante = VarianteBoton.PRIMARIO,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Si es la primera vez: registra tu negocio y crea tu usuario. Tu proveedor activará tu licencia y la " +
                    "app se activará sola.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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
        }
    }

    when (estado.formulario) {
        FormularioBienvenida.REGISTRAR_NEGOCIO -> DialogoRegistrarNegocio(estado, viewModel)
        FormularioBienvenida.ACTIVAR_LICENCIA -> DialogoActivarLicencia(estado, viewModel)
        null -> Unit
    }
}

/**
 * Aviso tras enviar el registro: el negocio puede avisar por WhatsApp al proveedor para que le active la licencia y
 * luego entrar (en modo consulta hasta que la licencia llegue). Se muestra desde la pantalla de inicio de sesión
 * porque, al crearse el administrador, la tarjeta de bienvenida ya no aparece.
 */
@Composable
fun DialogoRegistroEnviado(estado: BienvenidaUiState, viewModel: LoginViewModel) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    DialogoPos(
        alDescartar = { viewModel.entrarTrasRegistro() },
        icono = Icons.Filled.MarkEmailRead,
        colorIcono = VerdeExito,
        titulo = "¡Registro enviado!",
        mensaje = (estado.mensajeRegistro?.takeIf { it.isNotBlank() } ?: "Recibimos tu registro.") +
            " Avísanos por WhatsApp para activar tu licencia más rápido.",
        acciones = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BotonPos(
                    texto = "Notificar por WhatsApp",
                    alHacerClic = {
                        alcance.launch {
                            abrirWhatsApp(contexto, BuildConfig.WHATSAPP_REGISTROS, viewModel.mensajeWhatsAppRegistro())
                        }
                    },
                    icono = Icons.AutoMirrored.Filled.Chat,
                    variante = VarianteBoton.EXITO,
                    modifier = Modifier.fillMaxWidth(),
                )
                BotonPos(
                    texto = "Entrar",
                    alHacerClic = { viewModel.entrarTrasRegistro() },
                    variante = VarianteBoton.SECUNDARIO,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}

@Composable
private fun DialogoRegistrarNegocio(estado: BienvenidaUiState, viewModel: LoginViewModel) {
    DialogoPos(
        alDescartar = { viewModel.cerrarBienvenida() },
        icono = Icons.Filled.Storefront,
        colorIcono = AzulPrimario,
        titulo = "Registrar mi negocio",
        mensaje = "Necesitas internet. Los datos del negocio son los que saldrán en tus facturas.",
        contenido = {
            SeccionFormulario("Tu negocio")
            CampoTextoPos(
                valor = estado.nombreNegocio,
                alCambiarValor = { texto -> viewModel.editarBienvenida { it.copy(nombreNegocio = texto.take(200)) } },
                etiqueta = "Nombre del negocio *",
                iconoInicio = Icons.Filled.Storefront,
                opcionesTeclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            CampoTextoPos(
                valor = estado.telefonoNegocio,
                alCambiarValor = { texto -> viewModel.editarBienvenida { it.copy(telefonoNegocio = texto.take(50)) } },
                etiqueta = "Teléfono del negocio *",
                iconoInicio = Icons.Filled.Phone,
                opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )
            CampoTextoPos(
                valor = estado.emailNegocio,
                alCambiarValor = { texto -> viewModel.editarBienvenida { it.copy(emailNegocio = texto.take(200)) } },
                etiqueta = "Correo (opcional)",
                iconoInicio = Icons.Filled.Email,
                opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
            CampoTextoPos(
                valor = estado.rncNegocio,
                alCambiarValor = { texto -> viewModel.editarBienvenida { it.copy(rncNegocio = texto.take(30)) } },
                etiqueta = "RNC o cédula (opcional)",
                iconoInicio = Icons.Filled.Badge,
            )
            CampoTextoPos(
                valor = estado.direccionNegocio,
                alCambiarValor = { texto -> viewModel.editarBienvenida { it.copy(direccionNegocio = texto.take(300)) } },
                etiqueta = "Dirección (opcional)",
                iconoInicio = Icons.Filled.Place,
            )
            CampoTextoPos(
                valor = estado.sloganNegocio,
                alCambiarValor = { texto -> viewModel.editarBienvenida { it.copy(sloganNegocio = texto.take(200)) } },
                etiqueta = "Slogan (opcional)",
                iconoInicio = Icons.Filled.FormatQuote,
            )

            SeccionFormulario("Persona de contacto")
            CampoTextoPos(
                valor = estado.contactoNombre,
                alCambiarValor = { texto -> viewModel.editarBienvenida { it.copy(contactoNombre = texto.take(200)) } },
                etiqueta = "Nombre (si es otra persona)",
                iconoInicio = Icons.Filled.Person,
                opcionesTeclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            CampoTextoPos(
                valor = estado.contactoTelefono,
                alCambiarValor = { texto -> viewModel.editarBienvenida { it.copy(contactoTelefono = texto.take(50)) } },
                etiqueta = "Teléfono del contacto (opcional)",
                iconoInicio = Icons.Filled.Phone,
                opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )

            SeccionFormulario("Tu usuario (administrador)")
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
                textoAceptar = "Registrar",
                estaGuardando = estado.estaGuardando,
                alAceptar = { viewModel.registrarNegocio() },
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

/** Título pequeño para separar las partes del formulario de registro. */
@Composable
private fun SeccionFormulario(titulo: String) {
    Text(
        titulo,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = AzulPrimario,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
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
