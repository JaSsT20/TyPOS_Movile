package com.typdevstudio.typos_movil.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EsquemaColorOscuro = darkColorScheme(
    primary = AzulPrimario,
    onPrimary = Blanco,
    primaryContainer = Color(0xFF1565C0),
    onPrimaryContainer = AzulPrimarioClaro,
    secondary = GrisSecundarioDark,
    onSecondary = Blanco,
    tertiary = CelesteInformacionDark,
    onTertiary = Blanco,
    background = Color(0xFF121212),
    onBackground = Blanco,
    surface = Color(0xFF1E1E1E),
    onSurface = Blanco,
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFB0BEC5),
    outline = Color(0xFF546E7A),
    outlineVariant = Color(0xFF333333),
    error = RojoErrorDark,
    onError = Blanco
)

private val EsquemaColorClaro = lightColorScheme(
    primary = AzulPrimario,
    onPrimary = Blanco,
    primaryContainer = AzulPrimarioClaro,
    onPrimaryContainer = AzulPrimarioOscuro,
    secondary = GrisSecundario,
    onSecondary = Blanco,
    tertiary = CelesteInformacion,
    onTertiary = Blanco,
    background = FondoClaro,
    onBackground = GrisTexto,
    surface = SuperficieClara,
    onSurface = GrisTexto,
    surfaceVariant = Color(0xFFECEFF1),
    onSurfaceVariant = GrisSecundario,
    outline = GrisMedio,
    outlineVariant = GrisClaro,
    error = RojoError,
    onError = Blanco
)

@Composable
fun TyPOS_MovilTheme(
    modoTema: Int = 0, // 0: Seguir el Sistema, 1: Modo Claro, 2: Modo Oscuro
    contenido: @Composable () -> Unit
) {
    val sistemaEsOscuro = isSystemInDarkTheme()
    val usarOscuro = when (modoTema) {
        1 -> false // Forzar modo Claro
        2 -> true  // Forzar modo Oscuro
        else -> sistemaEsOscuro // Seguir el sistema operativo
    }

    val esquemaColor = if (usarOscuro) EsquemaColorOscuro else EsquemaColorClaro

    MaterialTheme(
        colorScheme = esquemaColor,
        typography = Typography,
        content = contenido
    )
}