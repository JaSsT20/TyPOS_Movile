package com.typdevstudio.typos_movil.ui.licencia

import com.typdevstudio.typos_movil.utilidades.licencia.nube.HoraRD
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Textos y formatos de la licencia para la interfaz. */
object TextosLicencia {

    private val LOCALE = Locale("es", "DO")

    fun fechaDesdeMs(ms: Long?): String =
        if (ms == null || ms <= 0) "—" else SimpleDateFormat("dd/MM/yyyy", LOCALE).format(Date(ms))

    fun fechaHoraDesdeMs(ms: Long?): String =
        if (ms == null || ms <= 0) "—" else SimpleDateFormat("dd/MM/yyyy hh:mm a", LOCALE).format(Date(ms))

    /** Día epoch (por ejemplo `vence`) a `dd/MM/yyyy`. */
    fun fechaDesdeDia(dia: Long): String {
        val (anio, mes, d) = HoraRD.formatearFecha(dia).split('-')
        return "$d/$mes/$anio"
    }

    /** "Tu licencia venció, tienes X días". */
    fun textoCortesia(diasRestantes: Int): String = when {
        diasRestantes <= 0 -> "Hoy es el último día para renovarla antes de que la app quede en modo solo consulta."
        diasRestantes == 1 -> "Tienes 1 día para renovarla antes de que la app quede en modo solo consulta."
        else -> "Tienes $diasRestantes días para renovarla antes de que la app quede en modo solo consulta."
    }

    /** "3 de 10 permitidos" (o solo el límite si todavía no se contaron). */
    fun usoUsuarios(activos: Int?, maximo: Int): String =
        if (activos == null) "$maximo permitidos" else "$activos de $maximo permitidos"

    /** El token trae el código del plan; aquí su nombre para mostrar. Un plan nuevo sin mapear sale capitalizado. */
    fun nombrePlan(codigo: String): String = when (codigo) {
        "basico" -> "Básico"
        "pro" -> "Pro"
        else -> codigo.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

    fun nombreModulo(codigo: String): String = when (codigo) {
        "facturacion" -> "Facturación"
        "inventario" -> "Inventario"
        "cxc" -> "Cuentas por cobrar"
        else -> codigo.replaceFirstChar { it.uppercase() }
    }
}
