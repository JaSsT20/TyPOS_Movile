package com.typdevstudio.typos_movil.ui.navegacion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.typdevstudio.typos_movil.ui.componentes.DialogoActualizacion
import com.typdevstudio.typos_movil.ui.configuracion.ConfiguracionViewModel
import com.typdevstudio.typos_movil.ui.configuracion.PantallaConfiguracion
import com.typdevstudio.typos_movil.ui.historial.HistorialViewModel
import com.typdevstudio.typos_movil.ui.historial.PantallaHistorialVentas
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

    var infoActualizacion by remember { mutableStateOf<InfoActualizacion?>(null) }

    // Verificación automática y no intrusiva de actualizaciones al arrancar la app
    LaunchedEffect(Unit) {
        val resultado = ActualizadorApp.verificarActualizaciones(contexto)
        if (resultado != null && resultado.hayActualizacion) {
            infoActualizacion = resultado
        }
    }

    // Modal de nueva actualización encontrada al abrir
    infoActualizacion?.let { info ->
        if (info.hayActualizacion) {
            DialogoActualizacion(
                info = info,
                alDescartar = { infoActualizacion = null },
                alActualizar = {
                    ActualizadorApp.iniciarDescarga(contexto, info.urlDescarga)
                    infoActualizacion = null
                }
            )
        }
    }

    NavHost(
        navController = controladorNavegacion,
        startDestination = Ruta.Login.ruta
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
                }
            )
        }
    }
}
