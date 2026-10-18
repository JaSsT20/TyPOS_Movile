package com.typdevstudio.typos_movil.ui.usuarios

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.typdevstudio.typos_movil.datos.local.GestorSesion
import com.typdevstudio.typos_movil.datos.local.entidades.UsuarioEntidad
import com.typdevstudio.typos_movil.ui.componentes.BotonCancelarPos
import com.typdevstudio.typos_movil.ui.componentes.BotonPos
import com.typdevstudio.typos_movil.ui.componentes.CampoTextoPos
import com.typdevstudio.typos_movil.ui.componentes.DialogoPos
import com.typdevstudio.typos_movil.ui.componentes.VarianteBoton
import com.typdevstudio.typos_movil.ui.licencia.MensajeResultado
import com.typdevstudio.typos_movil.ui.licencia.Tarjeta
import com.typdevstudio.typos_movil.ui.licencia.TextosLicencia
import com.typdevstudio.typos_movil.ui.theme.AmarilloAdvertencia
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.Blanco
import com.typdevstudio.typos_movil.ui.theme.RojoError
import com.typdevstudio.typos_movil.ui.theme.VerdeExito

/**
 * Usuarios de la licencia (solo administrador): lista, crear, cambiar rol, activar/desactivar y restablecer
 * la contraseña. Cualquier usuario puede iniciar sesión en cualquier teléfono de la licencia.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaUsuarios(
    viewModel: UsuariosViewModel,
    alVolver: () -> Unit,
) {
    val estado by viewModel.uiState.collectAsState()
    val usuarios by viewModel.usuarios.collectAsState()
    val resumen by viewModel.resumen.collectAsState()
    val sesion by GestorSesion.usuarioActivo.collectAsState()

    estado.seleccionado?.let { usuario ->
        DialogoAccionesUsuario(
            usuario = usuario,
            esSesionActual = usuario.id == sesion?.id,
            alCerrar = { viewModel.seleccionar(null) },
            alCambiarRol = { viewModel.cambiarRol(usuario) },
            alRestablecerClave = { viewModel.abrirRestablecerClave(usuario) },
            alCambiarActivo = { viewModel.cambiarActivo(usuario) },
        )
    }

    estado.formulario?.let { formulario ->
        DialogoFormularioUsuario(
            formulario = formulario,
            estaGuardando = estado.estaGuardando,
            alEditar = viewModel::editarFormulario,
            alGuardar = viewModel::guardarFormulario,
            alCerrar = viewModel::cerrarFormulario,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Usuarios", fontWeight = FontWeight.Bold, color = Blanco) },
                navigationIcon = {
                    IconButton(onClick = alVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Blanco)
                    }
                },
                actions = {
                    if (estado.conLicencia) {
                        if (estado.estaSincronizando) {
                            CircularProgressIndicator(color = Blanco, strokeWidth = 2.dp, modifier = Modifier.padding(end = 16.dp).size(20.dp))
                        } else {
                            IconButton(onClick = { viewModel.sincronizar() }) {
                                Icon(Icons.Filled.Sync, contentDescription = "Actualizar", tint = Blanco)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AzulPrimario),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValores ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValores),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            estado.mensaje?.let { mensaje ->
                item { MensajeResultado(mensaje, estado.esError, alCerrar = { viewModel.limpiarMensaje() }) }
            }

            item {
                Tarjeta {
                    val activos = usuarios.count { it.estaActivo }
                    val maximo = resumen?.maxUsuarios
                    if (estado.conLicencia && maximo != null) {
                        Text(
                            "Usuarios activos: ${TextosLicencia.usoUsuarios(activos, maximo)}",
                            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AzulPrimario,
                        )
                        Text(
                            "Los usuarios son de su licencia: pueden iniciar sesión en cualquier teléfono de ella. " +
                                "Crear o cambiar usuarios necesita internet.",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (activos > maximo) {
                            Text(
                                "Tiene más usuarios de los que permite su plan. Puede seguir usándolos, pero no crear nuevos.",
                                fontSize = 12.sp, color = AmarilloAdvertencia, fontWeight = FontWeight.SemiBold,
                            )
                        }
                    } else {
                        Text("Usuarios activos: $activos", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AzulPrimario)
                        Text(
                            if (estado.conLicencia) "Conéctese a internet para ver el límite de su plan."
                            else "Estos usuarios están solo en este teléfono. Cuando active su licencia se compartirán con sus demás teléfonos.",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    BotonPos(
                        texto = "Nuevo usuario",
                        alHacerClic = { viewModel.abrirNuevo() },
                        icono = Icons.Filled.PersonAdd,
                        variante = VarianteBoton.EXITO,
                        estaHabilitado = !estado.estaGuardando,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            items(usuarios, key = { it.id }) { usuario ->
                FilaUsuario(
                    usuario = usuario,
                    esSesionActual = usuario.id == sesion?.id,
                    habilitada = !estado.estaGuardando,
                    alTocar = { viewModel.seleccionar(usuario) },
                )
            }
        }
    }
}

@Composable
private fun FilaUsuario(usuario: UsuarioEntidad, esSesionActual: Boolean, habilitada: Boolean, alTocar: () -> Unit) {
    val esAdmin = usuario.rol == "ADMINISTRADOR"
    val colorRol = if (esAdmin) AzulPrimario else VerdeExito
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().clickable(enabled = habilitada, onClick = alTocar),
    ) {
        Row(
            modifier = Modifier.padding(14.dp).alpha(if (usuario.estaActivo) 1f else 0.55f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(colorRol.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    usuario.nombreCompleto.trim().firstOrNull()?.uppercase() ?: "?",
                    color = colorRol, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    usuario.nombreCompleto + if (esSesionActual) " (usted)" else "",
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text("@${usuario.nombreUsuario}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Etiqueta(if (esAdmin) "ADMIN" else "CAJERO", colorRol)
                if (!usuario.estaActivo) Etiqueta("INACTIVO", RojoError)
            }
        }
    }
}

@Composable
private fun Etiqueta(texto: String, color: Color) {
    Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
        Text(
            texto, fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun DialogoAccionesUsuario(
    usuario: UsuarioEntidad,
    esSesionActual: Boolean,
    alCerrar: () -> Unit,
    alCambiarRol: () -> Unit,
    alRestablecerClave: () -> Unit,
    alCambiarActivo: () -> Unit,
) {
    val esAdmin = usuario.rol == "ADMINISTRADOR"
    DialogoPos(
        alDescartar = alCerrar,
        icono = Icons.Filled.ManageAccounts,
        colorIcono = AzulPrimario,
        titulo = usuario.nombreCompleto,
        mensaje = "@${usuario.nombreUsuario} · ${if (esAdmin) "Administrador" else "Cajero"}" +
            if (usuario.estaActivo) "" else " · Inactivo",
        acciones = {
            if (usuario.estaActivo) {
                BotonPos(
                    texto = if (esAdmin) "Cambiar a cajero" else "Hacer administrador",
                    alHacerClic = alCambiarRol,
                    icono = if (esAdmin) Icons.Filled.PointOfSale else Icons.Filled.AdminPanelSettings,
                    variante = VarianteBoton.SECUNDARIO,
                    modifier = Modifier.fillMaxWidth(),
                )
                BotonPos(
                    texto = "Restablecer contraseña",
                    alHacerClic = alRestablecerClave,
                    icono = Icons.Filled.LockReset,
                    variante = VarianteBoton.SECUNDARIO,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (!esSesionActual) {
                BotonPos(
                    texto = if (usuario.estaActivo) "Desactivar" else "Activar",
                    alHacerClic = alCambiarActivo,
                    icono = if (usuario.estaActivo) Icons.Filled.PersonOff else Icons.Filled.VerifiedUser,
                    variante = if (usuario.estaActivo) VarianteBoton.PELIGRO else VarianteBoton.EXITO,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            BotonCancelarPos(texto = "Cerrar", alHacerClic = alCerrar)
        },
    )
}

@Composable
private fun DialogoFormularioUsuario(
    formulario: FormularioUsuario,
    estaGuardando: Boolean,
    alEditar: ((FormularioUsuario) -> FormularioUsuario) -> Unit,
    alGuardar: () -> Unit,
    alCerrar: () -> Unit,
) {
    val esNuevo = formulario.usuario == null
    DialogoPos(
        alDescartar = { if (!estaGuardando) alCerrar() },
        icono = if (esNuevo) Icons.Filled.PersonAdd else Icons.Filled.LockReset,
        colorIcono = if (esNuevo) VerdeExito else AzulPrimario,
        titulo = if (esNuevo) "Nuevo usuario" else "Restablecer contraseña",
        mensaje = if (esNuevo) "Podrá iniciar sesión en cualquier teléfono de su licencia."
        else "Nueva contraseña para @${formulario.usuario?.nombreUsuario}. Désela al usuario; podrá cambiarla desde su perfil.",
        contenido = {
            if (esNuevo) {
                CampoTextoPos(
                    valor = formulario.nombreUsuario,
                    alCambiarValor = { texto -> alEditar { it.copy(nombreUsuario = texto.take(50)) } },
                    etiqueta = "Usuario (para iniciar sesión)",
                    iconoInicio = Icons.Filled.Person,
                )
                CampoTextoPos(
                    valor = formulario.nombreCompleto,
                    alCambiarValor = { texto -> alEditar { it.copy(nombreCompleto = texto.take(100)) } },
                    etiqueta = "Nombre completo",
                    iconoInicio = Icons.Filled.Badge,
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("CAJERO" to "Cajero", "ADMINISTRADOR" to "Administrador").forEach { (valor, texto) ->
                        BotonPos(
                            texto = texto,
                            alHacerClic = { alEditar { it.copy(rol = valor) } },
                            variante = if (formulario.rol == valor) VarianteBoton.PRIMARIO else VarianteBoton.SECUNDARIO,
                            tamanoTexto = 13.sp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            CampoTextoPos(
                valor = formulario.clave,
                alCambiarValor = { texto -> alEditar { it.copy(clave = texto.take(64)) } },
                etiqueta = "Contraseña o PIN",
                iconoInicio = Icons.Filled.Lock,
                esContrasena = true,
            )
            CampoTextoPos(
                valor = formulario.confirmarClave,
                alCambiarValor = { texto -> alEditar { it.copy(confirmarClave = texto.take(64)) } },
                etiqueta = "Confirmar contraseña",
                iconoInicio = Icons.Filled.Lock,
                esContrasena = true,
            )
            formulario.error?.let {
                Text(it, color = RojoError, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        },
        acciones = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BotonCancelarPos(texto = "Cancelar", alHacerClic = alCerrar, modifier = Modifier.weight(1f))
                BotonPos(
                    texto = "Guardar",
                    alHacerClic = alGuardar,
                    estaCargando = estaGuardando,
                    variante = VarianteBoton.EXITO,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    )
}
