package com.typdevstudio.typos_movil.ui.navegacion

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.typdevstudio.typos_movil.datos.local.AppBaseDatos
import com.typdevstudio.typos_movil.datos.local.GestorSesion
import com.typdevstudio.typos_movil.ui.componentes.BotonPos
import com.typdevstudio.typos_movil.ui.componentes.DialogoActualizacion
import com.typdevstudio.typos_movil.ui.componentes.DialogoPos
import com.typdevstudio.typos_movil.ui.componentes.VarianteBoton
import com.typdevstudio.typos_movil.ui.licencia.BloqueContactoLicencia
import com.typdevstudio.typos_movil.ui.theme.AmarilloAdvertencia
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.RojoError
import com.typdevstudio.typos_movil.ui.configuracion.ConfiguracionViewModel
import com.typdevstudio.typos_movil.ui.configuracion.PantallaConfiguracion
import com.typdevstudio.typos_movil.ui.historial.HistorialViewModel
import com.typdevstudio.typos_movil.ui.historial.PantallaHistorialVentas
import com.typdevstudio.typos_movil.ui.licencia.DialogoAvisoVencimiento
import com.typdevstudio.typos_movil.ui.licencia.LicenciaViewModel
import com.typdevstudio.typos_movil.ui.licencia.PantallaLicencia
import com.typdevstudio.typos_movil.ui.login.PantallaLogin
import com.typdevstudio.typos_movil.ui.principal.PantallaPrincipal
import com.typdevstudio.typos_movil.ui.productos.PantallaFormularioProducto
import com.typdevstudio.typos_movil.ui.productos.PantallaProductos
import com.typdevstudio.typos_movil.ui.productos.ProductoViewModel
import com.typdevstudio.typos_movil.ui.ventas.PantallaVentas
import com.typdevstudio.typos_movil.ui.usuarios.PantallaUsuarios
import com.typdevstudio.typos_movil.ui.usuarios.UsuariosViewModel
import com.typdevstudio.typos_movil.ui.ventas.VentasViewModel
import com.typdevstudio.typos_movil.utilidades.actualizador.ActualizadorApp
import com.typdevstudio.typos_movil.utilidades.actualizador.InfoActualizacion
import com.typdevstudio.typos_movil.utilidades.licencia.LicenciaGuard
import com.typdevstudio.typos_movil.utilidades.licencia.ModulosLicencia
import kotlinx.coroutines.launch

sealed class Ruta(val ruta: String) {
    data object Login : Ruta("login")
    data object Principal : Ruta("principal")
    data object Productos : Ruta("productos")
    data object FormularioProducto : Ruta("formulario_producto")
    data object Ventas : Ruta("ventas")
    data object Historial : Ruta("historial")
    data object Configuracion : Ruta("configuracion")
    data object Licencia : Ruta("licencia")
    data object Usuarios : Ruta("usuarios")
}

@Composable
fun NavegacionApp() {
    val contexto = LocalContext.current
    val controladorNavegacion = rememberNavController()
    val productoViewModel: ProductoViewModel = viewModel()
    val ventasViewModel: VentasViewModel = viewModel()
    val configuracionViewModel: ConfiguracionViewModel = viewModel()
    val historialViewModel: HistorialViewModel = viewModel()
    var infoActualizacion by remember { mutableStateOf<InfoActualizacion?>(null) }
    var mostrarAvisoSoloAdministrador by remember { mutableStateOf(false) }
    var moduloNoIncluido by remember { mutableStateOf<String?>(null) }

    // Módulos del plan: si el plan no incluye el módulo, se explica en vez de abrir la pantalla.
    val navegarAModulo: (String, String, String) -> Unit = { ruta, modulo, nombreModulo ->
        if (LicenciaGuard.situacion.value.moduloIncluido(modulo)) {
            controladorNavegacion.navigate(ruta)
        } else {
            moduloNoIncluido = nombreModulo
        }
    }
    val alcance = rememberCoroutineScope()
    val usuarioDao = remember { AppBaseDatos.obtenerBaseDatos(contexto).usuarioDao() }

    // Licencia, Configuración y Respaldos son solo para administradores. Si el teléfono no tiene
    // ningún administrador se deja entrar a cualquiera (avisando), para que nadie quede sin acceso.
    val navegarComoAdministrador: (String) -> Unit = { ruta ->
        alcance.launch {
            if (GestorSesion.tieneAccesoAdministrador(usuarioDao)) {
                if (!GestorSesion.esAdministrador()) {
                    Toast.makeText(
                        contexto,
                        "No hay ningún usuario administrador en este teléfono. Crea uno para proteger esta sección.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                controladorNavegacion.navigate(ruta) { launchSingleTop = true }
            } else {
                mostrarAvisoSoloAdministrador = true
            }
        }
    }

    // 1. Verificación automática y no intrusiva de actualizaciones al arrancar la app
    LaunchedEffect(Unit) {
        val resultado = ActualizadorApp.verificarActualizaciones(contexto)
        if (resultado != null && resultado.hayActualizacion) {
            infoActualizacion = resultado
        }
    }

    // 2. La licencia ya no bloquea la app con un modal ni cierra la sesión: LicenciaGuard pasa a modo
    // solo consulta y las pantallas muestran BannerLicencia con el motivo.
    if (mostrarAvisoSoloAdministrador) {
        DialogoPos(
            alDescartar = { mostrarAvisoSoloAdministrador = false },
            icono = Icons.Filled.AdminPanelSettings,
            colorIcono = AzulPrimario,
            titulo = "Solo para administradores",
            mensaje = "Esta sección solo la puede abrir un usuario administrador. Pídele que inicie sesión.",
            acciones = {
                BotonPos(texto = "Entendido", alHacerClic = { mostrarAvisoSoloAdministrador = false })
            }
        )
    }

    moduloNoIncluido?.let { nombreModulo ->
        DialogoPos(
            alDescartar = { moduloNoIncluido = null },
            icono = Icons.Filled.Extension,
            colorIcono = AmarilloAdvertencia,
            titulo = "Módulo no incluido",
            mensaje = "El módulo $nombreModulo no está incluido en tu plan. Para agregarlo a tu licencia:",
            contenido = {
                BloqueContactoLicencia(
                    mensajeWhatsApp = "Hola, quiero agregar el módulo $nombreModulo a mi licencia de TyPOS Móvil.",
                    alUsarContacto = { moduloNoIncluido = null }
                )
            },
            acciones = {
                BotonPos(texto = "Entendido", alHacerClic = { moduloNoIncluido = null }, variante = VarianteBoton.SECUNDARIO)
            }
        )
    }

    // Aviso de vencimiento de la licencia (5 días antes y el mismo día), con el contacto para renovar
    DialogoAvisoVencimiento()

    // Sesión cerrada desde la licencia: el usuario fue desactivado en el portal o en otro teléfono
    val avisoSesionCerrada by GestorSesion.avisoSesionCerrada.collectAsState()
    avisoSesionCerrada?.let { motivo ->
        val volverAlInicioDeSesion = {
            GestorSesion.descartarAvisoSesionCerrada()
            controladorNavegacion.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } }
        }
        DialogoPos(
            alDescartar = volverAlInicioDeSesion,
            icono = Icons.Filled.Lock,
            colorIcono = RojoError,
            titulo = "Sesión cerrada",
            mensaje = motivo,
            acciones = { BotonPos(texto = "Entendido", alHacerClic = volverAlInicioDeSesion) }
        )
    }

    // Modal de nueva actualización encontrada al abrir
    infoActualizacion?.let { info ->
        if (info.hayActualizacion) {
            DialogoActualizacion(
                info = info,
                alDescartar = { infoActualizacion = null }
            )
        }
    }

    // Transición profesional sutil y fluida (Fade suave con desplazamiento mínimo de 4% sin movimientos bruscos)
    NavHost(
        navController = controladorNavegacion,
        startDestination = Ruta.Login.ruta,
        enterTransition = {
            fadeIn(animationSpec = tween(150, easing = LinearOutSlowInEasing)) +
            slideInHorizontally(
                initialOffsetX = { (it * 0.04f).toInt() },
                animationSpec = tween(160, easing = FastOutSlowInEasing)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(100, easing = FastOutLinearInEasing))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(150, easing = LinearOutSlowInEasing)) +
            slideInHorizontally(
                initialOffsetX = { -(it * 0.04f).toInt() },
                animationSpec = tween(160, easing = FastOutSlowInEasing)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(100, easing = FastOutLinearInEasing))
        }
    ) {
        composable(Ruta.Login.ruta) {
            PantallaLogin(
                alIniciarSesionExitoso = {
                    controladorNavegacion.navigate(Ruta.Principal.ruta) {
                        popUpTo(Ruta.Login.ruta) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Ruta.Principal.ruta) {
            PantallaPrincipal(
                alNavegarAVentas = {
                    navegarAModulo(Ruta.Ventas.ruta, ModulosLicencia.FACTURACION, "Facturación")
                },
                alNavegarAProductos = {
                    controladorNavegacion.navigate(Ruta.Productos.ruta)
                },
                alNavegarAHistorial = {
                    controladorNavegacion.navigate(Ruta.Historial.ruta)
                },
                alNavegarAConfiguracion = {
                    navegarComoAdministrador(Ruta.Configuracion.ruta)
                },
                alNavegarALicencia = {
                    navegarComoAdministrador(Ruta.Licencia.ruta)
                },
                alCerrarSesion = {
                    controladorNavegacion.navigate(Ruta.Login.ruta) {
                        popUpTo(0) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Ruta.Ventas.ruta) {
            PantallaVentas(
                viewModel = ventasViewModel,
                alVolver = {
                    controladorNavegacion.popBackStack()
                },
                alNavegarALicencia = {
                    navegarComoAdministrador(Ruta.Licencia.ruta)
                }
            )
        }

        composable(Ruta.Historial.ruta) {
            PantallaHistorialVentas(
                viewModel = historialViewModel,
                alVolver = {
                    controladorNavegacion.popBackStack()
                }
            )
        }

        composable(Ruta.Productos.ruta) {
            PantallaProductos(
                viewModel = productoViewModel,
                alCrearProducto = {
                    controladorNavegacion.navigate(Ruta.FormularioProducto.ruta)
                },
                alEditarProducto = {
                    controladorNavegacion.navigate(Ruta.FormularioProducto.ruta)
                },
                alVolver = {
                    controladorNavegacion.popBackStack()
                }
            )
        }

        composable(Ruta.FormularioProducto.ruta) {
            PantallaFormularioProducto(
                viewModel = productoViewModel,
                alVolver = {
                    controladorNavegacion.popBackStack()
                }
            )
        }

        composable(Ruta.Configuracion.ruta) {
            PantallaConfiguracion(
                viewModel = configuracionViewModel,
                alVolver = {
                    controladorNavegacion.popBackStack()
                },
                alNavegarAProductos = {
                    controladorNavegacion.navigate(Ruta.Productos.ruta)
                },
                alNavegarALicencia = {
                    // Configuración ya exige administrador para entrar.
                    controladorNavegacion.navigate(Ruta.Licencia.ruta) { launchSingleTop = true }
                },
                alNavegarAUsuarios = {
                    controladorNavegacion.navigate(Ruta.Usuarios.ruta) { launchSingleTop = true }
                }
            )
        }

        composable(Ruta.Usuarios.ruta) {
            val usuariosViewModel: UsuariosViewModel = viewModel()
            PantallaUsuarios(
                viewModel = usuariosViewModel,
                alVolver = {
                    controladorNavegacion.popBackStack()
                }
            )
        }

        composable(Ruta.Licencia.ruta) {
            val licenciaViewModel: LicenciaViewModel = viewModel()
            PantallaLicencia(
                viewModel = licenciaViewModel,
                alVolver = {
                    controladorNavegacion.popBackStack()
                }
            )
        }
    }
}
