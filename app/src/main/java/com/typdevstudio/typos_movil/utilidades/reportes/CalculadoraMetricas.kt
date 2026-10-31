package com.typdevstudio.typos_movil.utilidades.reportes

import com.typdevstudio.typos_movil.datos.local.entidades.VentaEntidad
import com.typdevstudio.typos_movil.datos.local.relaciones.VentaConDetalles
import java.util.Calendar

/** Rango de fechas [desde, hasta) en milisegundos. */
data class RangoFechas(val desde: Long, val hasta: Long) {
    /** Cantidad de días calendario que cubre el rango. */
    val cantidadDias: Int
        get() {
            var dias = 0
            var cursor = desde
            while (cursor < hasta) {
                dias++
                cursor = sumarDias(cursor, 1)
            }
            return dias
        }

    companion object {
        /** Desde el inicio del día de [desdeDia] hasta el final del día de [hastaDia] (ambos incluidos). */
        fun deDias(desdeDia: Long, hastaDia: Long): RangoFechas {
            val inicio = inicioDelDia(minOf(desdeDia, hastaDia))
            val fin = sumarDias(inicioDelDia(maxOf(desdeDia, hastaDia)), 1)
            return RangoFechas(inicio, fin)
        }

        fun hoy(): RangoFechas = deDias(System.currentTimeMillis(), System.currentTimeMillis())
    }
}

fun inicioDelDia(milis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = milis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

// Se suma por calendario (no 24 h fijas) para respetar los cambios de horario.
fun sumarDias(milis: Long, dias: Int): Long = Calendar.getInstance().apply {
    timeInMillis = milis
    add(Calendar.DAY_OF_YEAR, dias)
}.timeInMillis

data class TotalMetodoPago(val metodo: String, val cantidadVentas: Int, val monto: Double)
data class TotalCajero(val nombre: String, val cantidadVentas: Int, val monto: Double)
data class ProductoVendido(val nombre: String, val cantidad: Double, val monto: Double, val esServicio: Boolean)
data class VentaAnulada(val numeroFactura: String, val motivo: String?, val monto: Double)

data class ResumenVentas(
    val cantidadFacturas: Int = 0,
    val cantidadAnuladas: Int = 0,
    val montoAnulado: Double = 0.0,
    val subtotal: Double = 0.0,
    val descuentos: Double = 0.0,
    val itbis: Double = 0.0,
    val total: Double = 0.0,
    val metodosPago: List<TotalMetodoPago> = emptyList(),
    val cajeros: List<TotalCajero> = emptyList(),
    val productos: List<ProductoVendido> = emptyList(), // Ordenados de mayor a menor monto
    val anuladas: List<VentaAnulada> = emptyList(),
    val efectivoEnCaja: Double = 0.0,
    val montoPendiente: Double = 0.0, // Ventas cobradas por menos del total
    val facturasPorHora: List<Int> = List(24) { 0 }
) {
    val ticketPromedio: Double get() = if (cantidadFacturas > 0) total / cantidadFacturas else 0.0
}

/**
 * Cálculos de Métricas y del Reporte de Ventas a partir de las ventas guardadas.
 * Solo las ventas "Completada" suman; las anuladas se listan aparte.
 */
object CalculadoraMetricas {

    // Orden fijo para que los gráficos y el ticket muestren siempre los métodos en el mismo orden
    val ORDEN_METODOS = listOf("Efectivo", "Transferencia", "Cheque", "Crédito")

    fun esCompletada(venta: VentaEntidad) = venta.estadoVenta.equals("Completada", ignoreCase = true)

    fun calcularResumen(ventas: List<VentaConDetalles>): ResumenVentas {
        val completadas = ventas.filter { esCompletada(it.venta) }
        val canceladas = ventas.filterNot { esCompletada(it.venta) }

        // Métodos de pago: una venta Mixta reparte su monto entre los métodos que usó
        val montosPorMetodo = mutableMapOf<String, Double>()
        val ventasPorMetodo = mutableMapOf<String, Int>()
        var pendiente = 0.0
        for (item in completadas) {
            val desglose = desglosePago(item.venta)
            desglose.forEach { (metodo, monto) ->
                montosPorMetodo[metodo] = (montosPorMetodo[metodo] ?: 0.0) + monto
                ventasPorMetodo[metodo] = (ventasPorMetodo[metodo] ?: 0) + 1
            }
            pendiente += (item.venta.total - desglose.values.sum()).coerceAtLeast(0.0)
        }
        val metodosPago = ORDEN_METODOS
            .filter { (montosPorMetodo[it] ?: 0.0) > 0.0 }
            .map { TotalMetodoPago(it, ventasPorMetodo[it] ?: 0, montosPorMetodo[it] ?: 0.0) }

        val cajeros = completadas
            .groupBy { it.venta.usuario.ifBlank { "Sin usuario" } }
            .map { (nombre, lista) -> TotalCajero(nombre, lista.size, lista.sumOf { it.venta.total }) }
            .sortedByDescending { it.monto }

        val productos = completadas
            .flatMap { it.detalles }
            .groupBy { it.idProducto?.toString() ?: it.nombre.trim().lowercase() }
            .map { (_, detalles) ->
                ProductoVendido(
                    nombre = detalles.last().nombre,
                    cantidad = detalles.sumOf { it.cantidad },
                    monto = detalles.sumOf { it.total },
                    esServicio = detalles.last().tipo.equals("Servicio", ignoreCase = true)
                )
            }
            .sortedByDescending { it.monto }

        val porHora = MutableList(24) { 0 }
        val calendario = Calendar.getInstance()
        completadas.forEach {
            calendario.timeInMillis = it.venta.fecha
            porHora[calendario.get(Calendar.HOUR_OF_DAY)]++
        }

        return ResumenVentas(
            cantidadFacturas = completadas.size,
            cantidadAnuladas = canceladas.size,
            montoAnulado = canceladas.sumOf { it.venta.total },
            subtotal = completadas.sumOf { it.venta.subTotal },
            descuentos = completadas.sumOf { it.venta.descuento },
            itbis = completadas.sumOf { it.venta.montoItbis },
            total = completadas.sumOf { it.venta.total },
            metodosPago = metodosPago,
            cajeros = cajeros,
            productos = productos,
            anuladas = canceladas.map { VentaAnulada(it.venta.numeroFactura, it.venta.motivoAnulacion, it.venta.total) },
            efectivoEnCaja = montosPorMetodo["Efectivo"] ?: 0.0,
            montoPendiente = pendiente,
            facturasPorHora = porHora
        )
    }

    /** Lo que entró por cada método en una venta. En efectivo se descuenta la devuelta. */
    fun desglosePago(venta: VentaEntidad): Map<String, Double> {
        val desglose = when (venta.metodoPago) {
            "Mixto" -> mapOf(
                "Efectivo" to (venta.montoEfectivo - venta.montoDevuelto).coerceAtLeast(0.0),
                "Transferencia" to venta.montoTransferencia,
                "Cheque" to venta.montoCheque,
                "Crédito" to venta.montoCredito
            )
            // Ventas en efectivo antiguas pueden no tener monto_efectivo guardado
            "Efectivo" -> mapOf("Efectivo" to if (venta.montoEfectivo > 0) venta.montoEfectivo else venta.total)
            "Credito", "Crédito" -> mapOf("Crédito" to venta.total)
            else -> mapOf(venta.metodoPago to venta.total)
        }
        return desglose.filterValues { it > 0.0 }
    }

    /** Total vendido (completadas) en cada día del rango, empezando por [rango].desde. */
    fun totalesPorDia(ventas: List<VentaConDetalles>, rango: RangoFechas): List<Double> {
        val dias = rango.cantidadDias
        val inicios = (0..dias).map { sumarDias(rango.desde, it) }
        val totales = MutableList(dias) { 0.0 }
        ventas.filter { esCompletada(it.venta) }.forEach { item ->
            val indice = inicios.indexOfLast { it <= item.venta.fecha }
            if (indice in 0 until dias) totales[indice] += item.venta.total
        }
        return totales
    }
}
