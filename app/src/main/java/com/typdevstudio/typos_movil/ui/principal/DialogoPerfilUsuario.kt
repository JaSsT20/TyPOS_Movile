package com.typdevstudio.typos_movil.ui.principal

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.typdevstudio.typos_movil.datos.local.entidades.UsuarioEntidad
import com.typdevstudio.typos_movil.datos.repositorio.ResultadoActualizacionPerfil
import com.typdevstudio.typos_movil.datos.repositorio.UsuarioRepositorio
import com.typdevstudio.typos_movil.ui.componentes.BotonPos
import com.typdevstudio.typos_movil.ui.componentes.CampoTextoPos
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.AzulPrimarioClaro
import com.typdevstudio.typos_movil.ui.theme.Blanco
import com.typdevstudio.typos_movil.ui.theme.GrisSecundario
import com.typdevstudio.typos_movil.ui.theme.RojoError
import com.typdevstudio.typos_movil.ui.theme.VerdeExito
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * Componente reutilizable para renderizar el Avatar o Foto de Perfil del Usuario.
 */
@Composable
fun AvatarUsuario(
    fotoUri: String?,
    tamano: Dp = 50.dp,
    iconoTamano: Dp = 30.dp,
    colorFondo: Color = AzulPrimarioClaro.copy(alpha = 0.4f),
    colorIcono: Color = AzulPrimario,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(fotoUri) {
        if (!fotoUri.isNullOrBlank()) {
            val archivo = File(fotoUri)
            if (archivo.exists()) {
                BitmapFactory.decodeFile(archivo.absolutePath)?.asImageBitmap()
            } else null
        } else null
    }

    Box(
        modifier = modifier
            .size(tamano)
            .clip(CircleShape)
            .background(colorFondo),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Foto de perfil",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = colorIcono,
                modifier = Modifier.size(iconoTamano)
            )
        }
    }
}

/**
 * Guarda una foto seleccionada en el almacenamiento interno privado de la aplicación.
 */
fun guardarFotoPerfilInterna(contexto: Context, usuarioId: Long, uriOrigen: Uri): String? {
    return try {
        val carpetaPerfiles = File(contexto.filesDir, "fotos_perfil").apply {
            if (!exists()) mkdirs()
        }
        val archivoDestino = File(carpetaPerfiles, "avatar_${usuarioId}_${System.currentTimeMillis()}.jpg")
        contexto.contentResolver.openInputStream(uriOrigen)?.use { input ->
            FileOutputStream(archivoDestino).use { output ->
                input.copyTo(output)
            }
        }
        archivoDestino.absolutePath
    } catch (e: Exception) {
        null
    }
}

/**
 * Modal para configurar y editar el perfil del usuario activo (Nombre, Foto, Contraseña).
 */
@Composable
fun DialogoPerfilUsuario(
    usuario: UsuarioEntidad,
    usuarioRepositorio: UsuarioRepositorio,
    alCerrar: () -> Unit,
    alPerfilActualizado: (UsuarioEntidad) -> Unit
) {
    val contexto = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var nombreCompleto by remember { mutableStateOf(usuario.nombreCompleto) }
    var fotoUriTemporal by remember { mutableStateOf(usuario.fotoUri) }

    var cambiarClave by remember { mutableStateOf(false) }
    var claveActual by remember { mutableStateOf("") }
    var nuevaClave by remember { mutableStateOf("") }
    var confirmarNuevaClave by remember { mutableStateOf("") }

    var errorNombre by remember { mutableStateOf<String?>(null) }
    var errorClaveActual by remember { mutableStateOf<String?>(null) }
    var errorNuevaClave by remember { mutableStateOf<String?>(null) }
    var errorConfirmacion by remember { mutableStateOf<String?>(null) }
    var mensajeErrorGeneral by remember { mutableStateOf<String?>(null) }
    var estaGuardando by remember { mutableStateOf(false) }

    // Selector de imagen de galería
    val launcherGaleria = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val rutaGuardada = guardarFotoPerfilInterna(contexto, usuario.id, uri)
            if (rutaGuardada != null) {
                fotoUriTemporal = rutaGuardada
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!estaGuardando) alCerrar() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Cabecera con título y botón de cierre
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Mi Perfil",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Personaliza tus datos de acceso",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = alCerrar,
                        enabled = !estaGuardando
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Sección Foto de Perfil
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        AvatarUsuario(
                            fotoUri = fotoUriTemporal,
                            tamano = 96.dp,
                            iconoTamano = 54.dp,
                            modifier = Modifier
                                .border(3.dp, AzulPrimario.copy(alpha = 0.6f), CircleShape)
                        )

                        // Botón flotante para cambiar foto
                        Surface(
                            shape = CircleShape,
                            color = AzulPrimario,
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .size(34.dp)
                                .clickable {
                                    if (!estaGuardando) {
                                        launcherGaleria.launch("image/*")
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = "Cambiar Foto",
                                    tint = Blanco,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = { launcherGaleria.launch("image/*") },
                        enabled = !estaGuardando
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = AzulPrimario)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cambiar Foto", fontSize = 13.sp, color = AzulPrimario)
                    }

                    if (fotoUriTemporal != null) {
                        TextButton(
                            onClick = { fotoUriTemporal = null },
                            enabled = !estaGuardando
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = RojoError)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Quitar Foto", fontSize = 13.sp, color = RojoError)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))

                // Rol y Usuario (Solo lectura / informativo)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Usuario del Sistema",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "@${usuario.nombreUsuario}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AzulPrimario.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = usuario.rol,
                            color = AzulPrimario,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Campo Nombre Completo
                CampoTextoPos(
                    valor = nombreCompleto,
                    alCambiarValor = {
                        nombreCompleto = it
                        errorNombre = null
                        mensajeErrorGeneral = null
                    },
                    etiqueta = "Nombre Completo",
                    iconoInicio = Icons.Filled.Person,
                    mensajeError = errorNombre,
                    estaHabilitado = !estaGuardando,
                    opcionesTeclado = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = if (cambiarClave) ImeAction.Next else ImeAction.Done
                    ),
                    accionesTeclado = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        onDone = { focusManager.clearFocus() }
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Checkbox para activar cambio de contraseña
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = !estaGuardando) {
                            cambiarClave = !cambiarClave
                            if (!cambiarClave) {
                                claveActual = ""
                                nuevaClave = ""
                                confirmarNuevaClave = ""
                                errorClaveActual = null
                                errorNuevaClave = null
                                errorConfirmacion = null
                            }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = cambiarClave,
                        onCheckedChange = { activo ->
                            cambiarClave = activo
                            if (!activo) {
                                claveActual = ""
                                nuevaClave = ""
                                confirmarNuevaClave = ""
                                errorClaveActual = null
                                errorNuevaClave = null
                                errorConfirmacion = null
                            }
                        },
                        enabled = !estaGuardando,
                        colors = CheckboxDefaults.colors(
                            checkedColor = AzulPrimario,
                            checkmarkColor = Blanco
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Cambiar mi contraseña o PIN",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Campos desplegables para Cambio de Contraseña
                AnimatedVisibility(visible = cambiarClave) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CampoTextoPos(
                            valor = claveActual,
                            alCambiarValor = {
                                claveActual = it
                                errorClaveActual = null
                                mensajeErrorGeneral = null
                            },
                            etiqueta = "Contraseña Actual",
                            iconoInicio = Icons.Filled.Lock,
                            mensajeError = errorClaveActual,
                            esContrasena = true,
                            estaHabilitado = !estaGuardando,
                            opcionesTeclado = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            accionesTeclado = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            )
                        )

                        CampoTextoPos(
                            valor = nuevaClave,
                            alCambiarValor = {
                                nuevaClave = it
                                errorNuevaClave = null
                                mensajeErrorGeneral = null
                            },
                            etiqueta = "Nueva Contraseña o PIN",
                            iconoInicio = Icons.Filled.Key,
                            mensajeError = errorNuevaClave,
                            esContrasena = true,
                            estaHabilitado = !estaGuardando,
                            opcionesTeclado = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            accionesTeclado = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            )
                        )

                        CampoTextoPos(
                            valor = confirmarNuevaClave,
                            alCambiarValor = {
                                confirmarNuevaClave = it
                                errorConfirmacion = null
                                mensajeErrorGeneral = null
                            },
                            etiqueta = "Confirmar Nueva Contraseña",
                            iconoInicio = Icons.Filled.Key,
                            mensajeError = errorConfirmacion,
                            esContrasena = true,
                            estaHabilitado = !estaGuardando,
                            opcionesTeclado = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            accionesTeclado = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            )
                        )
                    }
                }

                // Mensaje de Error General si ocurre
                if (mensajeErrorGeneral != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RojoError.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = mensajeErrorGeneral!!,
                            color = RojoError,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botón Guardar Cambios
                BotonPos(
                    texto = "Guardar Cambios",
                    alHacerClic = {
                        focusManager.clearFocus()
                        var hayError = false

                        if (nombreCompleto.trim().isBlank()) {
                            errorNombre = "El nombre no puede estar vacío"
                            hayError = true
                        }

                        if (cambiarClave) {
                            if (claveActual.trim().isBlank()) {
                                errorClaveActual = "Ingresa tu contraseña actual"
                                hayError = true
                            }
                            if (nuevaClave.trim().isBlank()) {
                                errorNuevaClave = "Ingresa la nueva contraseña"
                                hayError = true
                            } else if (nuevaClave.trim().length < 4) {
                                errorNuevaClave = "Mínimo 4 caracteres"
                                hayError = true
                            }
                            if (confirmarNuevaClave.trim() != nuevaClave.trim()) {
                                errorConfirmacion = "Las contraseñas no coinciden"
                                hayError = true
                            }
                        }

                        if (hayError) return@BotonPos

                        estaGuardando = true
                        mensajeErrorGeneral = null

                        coroutineScope.launch {
                            val resultado = usuarioRepositorio.actualizarPerfil(
                                usuarioId = usuario.id,
                                nuevoNombreCompleto = nombreCompleto.trim(),
                                nuevaFotoUri = fotoUriTemporal,
                                claveActual = if (cambiarClave) claveActual.trim() else null,
                                nuevaClave = if (cambiarClave) nuevaClave.trim() else null
                            )

                            estaGuardando = false
                            when (resultado) {
                                is ResultadoActualizacionPerfil.Exito -> {
                                    alPerfilActualizado(resultado.usuario)
                                }
                                is ResultadoActualizacionPerfil.Error -> {
                                    mensajeErrorGeneral = resultado.mensaje
                                }
                            }
                        }
                    },
                    estaCargando = estaGuardando
                )
            }
        }
    }
}
