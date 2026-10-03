package com.typdevstudio.typos_movil.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.typdevstudio.typos_movil.datos.repositorio.UsuarioRepositorio
import com.typdevstudio.typos_movil.ui.componentes.BotonPos
import com.typdevstudio.typos_movil.ui.componentes.CampoTextoPos
import com.typdevstudio.typos_movil.ui.componentes.VarianteBoton
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
import com.typdevstudio.typos_movil.utilidades.licencia.CifradorTokensUsuario
import com.typdevstudio.typos_movil.utilidades.licencia.InfoTokenUsuario
import kotlinx.coroutines.launch

@Composable
fun DialogoActivarUsuario(
    usuarioRepositorio: UsuarioRepositorio,
    alCerrar: () -> Unit,
    alUsuarioRegistrado: (nombreUsuario: String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var codigoIngresado by remember { mutableStateOf("") }
    var infoToken by remember { mutableStateOf<InfoTokenUsuario?>(null) }
    var clave by remember { mutableStateOf("") }
    var confirmarClave by remember { mutableStateOf("") }
    var mensajeError by remember { mutableStateOf<String?>(null) }
    var mensajeExito by remember { mutableStateOf<String?>(null) }
    var estaGuardando by remember { mutableStateOf(false) }

    fun validarCodigo() {
        mensajeError = null
        val res = CifradorTokensUsuario.validarToken(codigoIngresado)
        if (!res.esValido) {
            infoToken = null
            mensajeError = res.mensajeError.ifBlank { "Código de activación inválido o expirado" }
            return
        }

        // Validar en la base de datos si el usuario ya existe
        coroutineScope.launch {
            if (usuarioRepositorio.existeUsuario(res.nombreUsuario)) {
                infoToken = null
                mensajeError = "El usuario '${res.nombreUsuario}' ya está registrado en este dispositivo."
            } else {
                infoToken = res
                mensajeError = null
            }
        }
    }

    fun registrarUsuario() {
        val token = infoToken ?: return
        if (clave.isBlank() || clave.length < 4) {
            mensajeError = "La contraseña debe tener al menos 4 caracteres"
            return
        }
        if (clave != confirmarClave) {
            mensajeError = "Las contraseñas no coinciden"
            return
        }

        estaGuardando = true
        mensajeError = null

        coroutineScope.launch {
            val exito = usuarioRepositorio.registrarNuevoUsuario(
                nombreUsuario = token.nombreUsuario,
                nombreCompleto = token.nombreUsuario.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() },
                clave = clave,
                rol = token.rol
            )
            estaGuardando = false

            if (exito) {
                mensajeExito = "¡Usuario '${token.nombreUsuario}' creado exitosamente!"
                alUsuarioRegistrado(token.nombreUsuario)
            } else {
                mensajeError = "No se pudo registrar el usuario. Es posible que ya exista."
            }
        }
    }

    Dialog(
        onDismissRequest = alCerrar,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Blanco),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Encabezado
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(AzulPrimarioClaro.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PersonAdd,
                        contentDescription = null,
                        tint = AzulPrimario,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Text(
                    text = "Activar Nuevo Usuario",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = GrisTexto
                )

                Text(
                    text = "Ingresa el código de invitación generado por el administrador para habilitar tu cuenta.",
                    fontSize = 12.sp,
                    color = GrisSecundario,
                    textAlign = TextAlign.Center
                )

                // Mensaje de Error
                if (mensajeError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(RojoError.copy(alpha = 0.1f))
                            .border(1.dp, RojoError.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = RojoError, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = mensajeError!!, fontSize = 11.sp, color = RojoError, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Paso 1: Ingreso de Código
                CampoTextoPos(
                    valor = codigoIngresado,
                    alCambiarValor = {
                        codigoIngresado = it
                        if (infoToken != null) infoToken = null // Resetear si cambia el código
                    },
                    etiqueta = "Código de Invitación (USR-...)",
                    iconoInicio = Icons.Filled.Key,
                    opcionesTeclado = KeyboardOptions(imeAction = ImeAction.Done)
                )

                if (infoToken == null) {
                    BotonPos(
                        texto = "Validar Código",
                        alHacerClic = { validarCodigo() },
                        variante = VarianteBoton.SECUNDARIO,
                        icono = Icons.Filled.VerifiedUser,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Paso 2: Código Válido - Mostrar Datos y Pedir Contraseña
                    val token = infoToken!!
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(VerdeExito.copy(alpha = 0.08f))
                            .border(1.dp, VerdeExito.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = VerdeExito, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Código Válido", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = VerdeExito)
                            }

                            Box(
                                modifier = Modifier
                                    .background(AzulPrimario, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(text = token.rol, fontSize = 10.sp, color = Blanco, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(color = VerdeExito.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            text = "Usuario asignado: @${token.nombreUsuario}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrisTexto
                        )
                    }

                    // Campos de Contraseña
                    CampoTextoPos(
                        valor = clave,
                        alCambiarValor = { clave = it },
                        etiqueta = "Crea tu Contraseña *",
                        iconoInicio = Icons.Filled.Lock,
                        esContrasena = true,
                        opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next)
                    )

                    CampoTextoPos(
                        valor = confirmarClave,
                        alCambiarValor = { confirmarClave = it },
                        etiqueta = "Confirmar Contraseña *",
                        iconoInicio = Icons.Filled.Lock,
                        esContrasena = true,
                        opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
                    )

                    BotonPos(
                        texto = "Crear y Activar Usuario",
                        alHacerClic = { registrarUsuario() },
                        estaCargando = estaGuardando,
                        variante = VarianteBoton.EXITO,
                        icono = Icons.Filled.CheckCircle,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                TextButton(
                    onClick = alCerrar,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cancelar",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GrisSecundario
                    )
                }
            }
        }
    }
}
