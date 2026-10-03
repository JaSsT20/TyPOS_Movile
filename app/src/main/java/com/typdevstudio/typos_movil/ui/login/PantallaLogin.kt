package com.typdevstudio.typos_movil.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.typdevstudio.typos_movil.ui.componentes.BotonPos
import com.typdevstudio.typos_movil.ui.componentes.CampoTextoPos
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.AzulPrimarioClaro
import com.typdevstudio.typos_movil.ui.theme.Blanco
import com.typdevstudio.typos_movil.ui.theme.FondoClaro
import com.typdevstudio.typos_movil.ui.theme.GrisSecundario
import com.typdevstudio.typos_movil.ui.theme.GrisTexto
import com.typdevstudio.typos_movil.ui.theme.RojoError
import com.typdevstudio.typos_movil.ui.theme.VerdeExito

@Composable
fun PantallaLogin(
    alIniciarSesionExitoso: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val estado by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    var mostrarDialogoActivacion by remember { mutableStateOf(false) }

    LaunchedEffect(estado.loginExitoso) {
        if (estado.loginExitoso) {
            alIniciarSesionExitoso()
            viewModel.reiniciarEstado()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoClaro)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logotipo circular e isotipo POS
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(AzulPrimario, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PointOfSale,
                    contentDescription = "Logo TyPOS Móvil",
                    tint = Blanco,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "TyPOS Móvil",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = AzulPrimario
            )

            Text(
                text = "Punto de Venta e Inventario",
                fontSize = 14.sp,
                color = GrisSecundario
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Tarjeta de formulario de acceso
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Blanco),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Iniciar Sesión",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = GrisTexto
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Mensaje de éxito si viene de activar un usuario
                    if (estado.mensajeExito != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(VerdeExito.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = VerdeExito, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = estado.mensajeExito!!,
                                    color = VerdeExito,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Campo de Usuario
                    CampoTextoPos(
                        valor = estado.usuario,
                        alCambiarValor = { viewModel.onUsuarioCambiado(it) },
                        etiqueta = "Usuario",
                        iconoInicio = Icons.Filled.Person,
                        mensajeError = estado.errorUsuario,
                        estaHabilitado = !estado.estaCargando,
                        opcionesTeclado = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        accionesTeclado = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Campo de Contraseña o PIN
                    CampoTextoPos(
                        valor = estado.clave,
                        alCambiarValor = { viewModel.onClaveCambiada(it) },
                        etiqueta = "Contraseña o PIN",
                        iconoInicio = Icons.Filled.Lock,
                        mensajeError = estado.errorClave,
                        esContrasena = true,
                        estaHabilitado = !estado.estaCargando,
                        opcionesTeclado = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        accionesTeclado = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.iniciarSesion()
                            }
                        )
                    )

                    if (estado.mensajeErrorGeneral != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(RojoError.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = estado.mensajeErrorGeneral!!,
                                color = RojoError,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Botón de Ingresar
                    BotonPos(
                        texto = "Ingresar al Sistema",
                        alHacerClic = {
                            focusManager.clearFocus()
                            viewModel.iniciarSesion()
                        },
                        estaCargando = estado.estaCargando
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón para Activar Usuario con Código de Invitación
            TextButton(
                onClick = { mostrarDialogoActivacion = true }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Key,
                        contentDescription = null,
                        tint = AzulPrimario,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "¿Tienes un código de usuario? Activar cuenta",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AzulPrimario
                    )
                }
            }
        }
    }

    // Modal de Activación de Usuario
    if (mostrarDialogoActivacion) {
        DialogoActivarUsuario(
            usuarioRepositorio = viewModel.repositorio,
            alCerrar = { mostrarDialogoActivacion = false },
            alUsuarioRegistrado = { nombreUsuario ->
                mostrarDialogoActivacion = false
                viewModel.onUsuarioRegistradoConExito(nombreUsuario)
            }
        )
    }
}
