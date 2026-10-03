package com.typdevstudio.typos_movil.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EsquemaColorOscuro = darkColorScheme(
    primary = AzulPrimarioDark,
    onPrimary = Color.Black,
    primaryContainer = AzulPrimarioOscuro,
    onPrimaryContainer = AzulPrimarioClaro,
    secondary = GrisSecundarioDark,
    onSecondary = Color.Black,
    tertiary = CelesteInformacionDark,
    onTertiary = Color.Black,
    background = FondoOscuro,
    onBackground = Blanco,
    surface = SuperficieOscura,
    onSurface = Blanco,
    error = RojoErrorDark,
    onError = Color.Black
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