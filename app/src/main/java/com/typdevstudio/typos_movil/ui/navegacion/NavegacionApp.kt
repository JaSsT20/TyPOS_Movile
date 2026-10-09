package com.typdevstudio.typos_movil.ui.navegacion

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.typdevstudio.typos_movil.datos.local.GestorSesion
import com.typdevstudio.typos_movil.datos.repositorio.EstadoLicencia
import com.typdevstudio.typos_movil.ui.componentes.DialogoActualizacion
import com.typdevstudio.typos_movil.ui.configuracion.ConfiguracionViewModel
import com.typdevstudio.typos_movil.ui.configuracion.PantallaConfiguracion
import com.typdevstudio.typos_movil.ui.historial.HistorialViewModel
import com.typdevstudio.typos_movil.ui.historial.PantallaHistorialVentas
import com.typdevstudio.typos_movil.ui.licencia.DialogoLicenciaBloqueada
import com.typdevstudio.typos_movil.ui.licencia.LicenciaViewModel
import com.typdevstudio.typos_movil.ui.login.PantallaLogin
import com.typdevstudio.typos_movil.ui.principal.PantallaPrincipal
import com.typdevstudio.typos_movil.ui.productos.PantallaFormularioProducto
import com.typdevstudio.typos_movil.ui.productos.PantallaProductos
import com.typdevstudio.typos_movil.ui.productos.ProductoViewModel
import com.typdevstudio.typos_movil.ui.ventas.PantallaVentas
import com.typdevstudio.typos_movil.ui.ventas.VentasViewModel
import com.typdevstudio.typos_movil.utilidades.actualizador.ActualizadorApp
import com.typdevstudio.typos_movil.utilidades.actualizador.InfoActualizacion

sealed class Ruta(val ruta: String) {
    data object Login : Ruta("login")
    data object Principal : Ruta("principal")
    data object Productos : Ruta("productos")
    data object FormularioProducto : Ruta("formulario_producto")
    data object Ventas : Ruta("ventas")
    data object Historial : Ruta("historial")
    data object Configuracion : Ruta("configuracion")
}

@Composable
fun NavegacionApp() {
    val contexto = LocalContext.current
    val controladorNavegacion = rememberNavController()
    val productoViewModel: ProductoViewModel = viewModel()
    val ventasViewModel: VentasViewModel = viewModel()
    val configuracionViewModel: ConfiguracionViewModel = viewModel()
    val historialViewModel: HistorialViewModel = viewModel()
    val licenciaViewModel: LicenciaViewModel = viewModel()

    val estadoLicenciaUi by licenciaViewModel.uiState.collectAsState()
    var infoActualizacion by remember { mutableStateOf<InfoActualizacion?>(null) }

    // 1. Verificación automática y no intrusiva de actualizaciones al arrancar la app
    LaunchedEffect(Unit) {
        val resultado = ActualizadorApp.verificarActualizaciones(contexto)
        if (resultado != null && resultado.hayActualizacion) {
            infoActualizacion = resultado
        }
    }

    // 2. Control reactivo de caducidad: Si la licencia expira, forzar salida inmediata al Login
    LaunchedEffect(estadoLicenciaUi.estado, estadoLicenciaUi.estaVerificandoInicial) {
        if (!estadoLicenciaUi.estaVerificandoInicial && estadoLicenciaUi.estado !is EstadoLicencia.Activa) {
            if (GestorSesion.usuarioActivo.value != null) {
                GestorSesion.cerrarSesion()
                controladorNavegacion.navigate(Ruta.Login.ruta) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    // 3. Control estricto de Licenciamiento (Modal no descartable de bloqueo si no hay licencia o está vencida)
    DialogoLicenciaBloqueada(viewModel = licenciaViewModel)

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
                    controladorNavegacion.navigate(Ruta.Ventas.ruta)
                },
                alNavegarAProductos = {
                    controladorNavegacion.navigate(Ruta.Productos.ruta)
                },
                alNavegarAHistorial = {
                    controladorNavegacion.navigate(Ruta.Historial.ruta)
                },
                alNavegarAConfiguracion = {
                    controladorNavegacion.navigate(Ruta.Configuracion.ruta)
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
                }
            )
        }
    }
}
