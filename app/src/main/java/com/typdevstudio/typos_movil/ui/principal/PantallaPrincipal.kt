package com.typdevstudio.typos_movil.ui.principal

import android.app.Activity
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.typdevstudio.typos_movil.datos.local.GestorSesion
import com.typdevstudio.typos_movil.ui.componentes.DialogoConfirmacionSalida
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.AzulPrimarioClaro
import com.typdevstudio.typos_movil.ui.theme.Blanco
import com.typdevstudio.typos_movil.ui.theme.CelesteInformacion
import com.typdevstudio.typos_movil.ui.theme.GrisSecundario
import com.typdevstudio.typos_movil.ui.theme.VerdeExito

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipal(
    alNavegarAVentas: () -> Unit,
    alNavegarAProductos: () -> Unit,
    alNavegarAHistorial: () -> Unit,
    alNavegarAConfiguracion: () -> Unit,
    alCerrarSesion: () -> Unit
) {
    val contexto = LocalContext.current
    val usuarioActivo by GestorSesion.usuarioActivo.collectAsState()
    var mostrarDialogoSalida by remember { mutableStateOf(false) }

    // Intercepta el botón Atrás de Android para mostrar el diálogo de confirmación
    BackHandler(enabled = true) {
        mostrarDialogoSalida = true
    }

    DialogoConfirmacionSalida(
        mostrar = mostrarDialogoSalida,
        alCerrarSesion = {
            GestorSesion.cerrarSesion()
            alCerrarSesion()
        },
        alSalirApp = {
            (contexto as? Activity)?.finishAffinity()
        },
        alDescartar = {
            mostrarDialogoSalida = false
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "TyPOS Móvil",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Blanco
                        )
                        Text(
                            text = "Usuario: ${usuarioActivo?.nombreCompleto ?: "Desconocido"} (${usuarioActivo?.rol ?: "CAJERO"})",
                            fontSize = 12.sp,
                            color = AzulPrimarioClaro
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { mostrarDialogoSalida = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Cerrar sesión o salir",
                            tint = Blanco
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AzulPrimario
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValores ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValores)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tarjeta de Bienvenida
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(AzulPrimarioClaro.copy(alpha = 0.4f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = AzulPrimario,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "¡Hola, ${usuarioActivo?.nombreCompleto ?: "Usuario"}!",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Selecciona una opción para comenzar a operar.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Opciones Principales del POS
            Text(
                text = "Módulos de Trabajo",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )

            TarjetaModuloPos(
                titulo = "Caja y Ventas",
                descripcion = "Registrar nueva venta, cobrar e imprimir ticket",
                icono = Icons.Filled.ShoppingCart,
                colorIcono = VerdeExito,
                alHacerClic = alNavegarAVentas
            )

            TarjetaModuloPos(
                titulo = "Catálogo de Productos",
                descripcion = "Registrar productos, gestionar precios y stock",
                icono = Icons.Filled.Inventory,
                colorIcono = AzulPrimario,
                alHacerClic = alNavegarAProductos
            )

            TarjetaModuloPos(
                titulo = "Historial y Reportes",
                descripcion = "Consultar ventas realizadas, reimprimir y anular tickets",
                icono = Icons.Filled.Assessment,
                colorIcono = CelesteInformacion,
                alHacerClic = alNavegarAHistorial
            )

            TarjetaModuloPos(
                titulo = "Configuración e Impresora",
                descripcion = "Vincular impresora Bluetooth térmica y datos del negocio",
                icono = Icons.Filled.Print,
                colorIcono = GrisSecundario,
                alHacerClic = alNavegarAConfiguracion
            )
        }
    }
}

@Composable
fun TarjetaModuloPos(
    titulo: String,
    descripcion: String,
    icono: ImageVector,
    colorIcono: Color,
    alHacerClic: () -> Unit
) {
    Card(
        onClick = alHacerClic,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(colorIcono.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = colorIcono,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = descripcion,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
