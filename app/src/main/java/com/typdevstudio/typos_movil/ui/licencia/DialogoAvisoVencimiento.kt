package com.typdevstudio.typos_movil.ui.licencia

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Event
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.typdevstudio.typos_movil.datos.local.GestorSesion
import com.typdevstudio.typos_movil.ui.componentes.BotonPos
import com.typdevstudio.typos_movil.ui.componentes.DialogoPos
import com.typdevstudio.typos_movil.ui.componentes.VarianteBoton
import com.typdevstudio.typos_movil.ui.theme.AmarilloAdvertencia
import com.typdevstudio.typos_movil.ui.theme.RojoError
import com.typdevstudio.typos_movil.utilidades.licencia.AvisoVencimiento
import com.typdevstudio.typos_movil.utilidades.licencia.CalculadoraAvisoVencimiento
import com.typdevstudio.typos_movil.utilidades.licencia.LicenciaGuard
import com.typdevstudio.typos_movil.utilidades.licencia.TipoAvisoVencimiento
import com.typdevstudio.typos_movil.utilidades.licencia.nube.HoraRD

/** Recuerda qué avisos de vencimiento ya se mostraron (no es información sensible). */
private class RegistroAvisosVencimiento(contexto: Context) {
    private val preferencias = contexto.applicationContext.getSharedPreferences("avisos_licencia", Context.MODE_PRIVATE)

    fun leer(): Set<String> = preferencias.getStringSet(CLAVE, emptySet()).orEmpty().toSet()

    fun marcar(aviso: AvisoVencimiento) {
        // Solo se conservan los avisos de la fecha actual; los de vencimientos anteriores ya no sirven.
        val vigentes = leer().filter { it.endsWith("_${aviso.venceDia}") }.toSet() + aviso.clave
        preferencias.edit().putStringSet(CLAVE, vigentes).apply()
    }

    private companion object {
        const val CLAVE = "mostrados"
    }
}

/**
 * Modal de vencimiento: aparece una vez cuando faltan 5 días o menos y otra vez el mismo día del vencimiento,
 * con el contacto para renovar. Solo con una sesión iniciada.
 */
@Composable
fun DialogoAvisoVencimiento() {
    val contexto = LocalContext.current
    val situacion by LicenciaGuard.situacion.collectAsState()
    val usuario by GestorSesion.usuarioActivo.collectAsState()
    val registro = remember { RegistroAvisosVencimiento(contexto) }
    var mostrados by remember { mutableStateOf(registro.leer()) }

    if (usuario == null) return
    // "Hoy" se recalcula en cada recomposición: al abrir la app, al iniciar sesión o cuando cambia la situación.
    val hoy = HoraRD.diaEnRD(System.currentTimeMillis())
    val aviso = CalculadoraAvisoVencimiento.calcular(situacion.venceDia, hoy, mostrados) ?: return

    val cerrar = {
        registro.marcar(aviso)
        mostrados = registro.leer()
    }
    val esHoy = aviso.tipo == TipoAvisoVencimiento.VENCE_HOY
    val fecha = TextosLicencia.fechaDesdeDia(aviso.venceDia)
    val diasCortesia = if (situacion.usaLicenciaAntigua) 0 else situacion.payload?.diasCortesia ?: 0

    DialogoPos(
        alDescartar = cerrar,
        icono = if (esHoy) Icons.Filled.EventBusy else Icons.Filled.Event,
        colorIcono = if (esHoy) RojoError else AmarilloAdvertencia,
        titulo = if (esHoy) "Su licencia vence hoy" else "Su licencia está por vencer",
        mensaje = if (esHoy) {
            "Su licencia de TyPOS Móvil vence hoy, $fecha. Comuníquese hoy mismo para renovarla." +
                if (diasCortesia > 0) " Después tendrá $diasCortesia días de cortesía antes de que la app quede en modo solo consulta." else ""
        } else {
            val dias = if (aviso.diasRestantes == 1) "1 día" else "${aviso.diasRestantes} días"
            "Su licencia de TyPOS Móvil vence el $fecha (en $dias). Renuévela a tiempo para seguir vendiendo sin interrupciones."
        },
        contenido = {
            BloqueContactoLicencia(
                mensajeWhatsApp = mensajeRenovacion(situacion.clave),
                alUsarContacto = cerrar
            )
        },
        acciones = {
            BotonPos(texto = "Entendido", alHacerClic = cerrar, variante = VarianteBoton.SECUNDARIO)
        }
    )
}

/** Mensaje inicial de WhatsApp; con la clave, el proveedor identifica la licencia sin preguntar. */
private fun mensajeRenovacion(clave: String?): String =
    if (clave != null) "Hola, quiero renovar mi licencia de TyPOS Móvil (clave $clave)."
    else "Hola, quiero renovar mi licencia de TyPOS Móvil."
