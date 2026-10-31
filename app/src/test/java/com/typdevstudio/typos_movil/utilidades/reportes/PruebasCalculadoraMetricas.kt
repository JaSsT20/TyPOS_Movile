package com.typdevstudio.typos_movil.utilidades.reportes

import com.typdevstudio.typos_movil.datos.impresion.DatosReporteVentas
import com.typdevstudio.typos_movil.datos.impresion.OpcionesReporteVentas
import com.typdevstudio.typos_movil.datos.impresion.ServicioImpresoraBluetooth
import com.typdevstudio.typos_movil.datos.local.entidades.ConfiguracionNegocioEntidad
import com.typdevstudio.typos_movil.datos.local.entidades.DetalleVentaEntidad
import com.typdevstudio.typos_movil.datos.local.entidades.VentaEntidad
import com.typdevstudio.typos_movil.datos.local.relaciones.VentaConDetalles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PruebasCalculadoraMetricas {

    private val hoy = inicioDelDia(System.currentTimeMillis())

    private fun venta(
        total: Double,
        metodo: String = "Efectivo",
        fecha: Long = hoy + 3_600_000,
        estado: String = "Completada",
        usuario: String = "Administrador",
        efectivo: Double = 0.0,
        transferencia: Double = 0.0,
        devuelto: Double = 0.0,
        detalles: List<DetalleVentaEntidad> = emptyList()
    ) = VentaConDetalles(
        VentaEntidad(
            numeroFactura = "FAC-${(1000..9999).random()}",
            fecha = fecha,
            idUsuario = 1,
            usuario = usuario,
            subTotal = total / 1.18,
            subTotalNeto = total / 1.18,
            montoItbis = total - total / 1.18,
            total = total,
            montoRecibido = total,
            montoDevuelto = devuelto,
            estadoVenta = estado,
            metodoPago = metodo,
            montoEfectivo = efectivo,
            montoTransferencia = transferencia,
            motivoAnulacion = if (estado == "Cancelada") "Error en cobro" else null
        ),
        detalles
    )

    private fun detalle(nombre: String, cantidad: Double, total: Double, idProducto: Long? = null) = DetalleVentaEntidad(
        idVenta = 0, idProducto = idProducto, nombre = nombre, precio = total / cantidad, cantidad = cantidad,
        subTotal = total, subTotalNeto = total, total = total
    )

    @Test
    fun lasAnuladasNoSumanYSeListanAparte() {
        val resumen = CalculadoraMetricas.calcularResumen(
            listOf(venta(100.0, efectivo = 100.0), venta(50.0, estado = "Cancelada"))
        )
        assertEquals(1, resumen.cantidadFacturas)
        assertEquals(1, resumen.cantidadAnuladas)
        assertEquals(100.0, resumen.total, 0.001)
        assertEquals(50.0, resumen.montoAnulado, 0.001)
        assertEquals("Error en cobro", resumen.anuladas.single().motivo)
    }

    @Test
    fun laVentaMixtaSeRepartePorMetodoYDescuentaLaDevuelta() {
        // Pagó 300 en efectivo y 200 por transferencia una venta de 450: se devolvieron 50
        val resumen = CalculadoraMetricas.calcularResumen(
            listOf(venta(450.0, metodo = "Mixto", efectivo = 300.0, transferencia = 200.0, devuelto = 50.0))
        )
        val porMetodo = resumen.metodosPago.associateBy { it.metodo }
        assertEquals(250.0, porMetodo.getValue("Efectivo").monto, 0.001)
        assertEquals(200.0, porMetodo.getValue("Transferencia").monto, 0.001)
        assertEquals(250.0, resumen.efectivoEnCaja, 0.001)
        assertEquals(0.0, resumen.montoPendiente, 0.001)
    }

    @Test
    fun efectivoAntiguoSinMontoGuardadoUsaElTotal() {
        val resumen = CalculadoraMetricas.calcularResumen(listOf(venta(80.0, efectivo = 0.0)))
        assertEquals(80.0, resumen.efectivoEnCaja, 0.001)
    }

    @Test
    fun cobroMenorAlTotalQuedaComoPendiente() {
        val resumen = CalculadoraMetricas.calcularResumen(listOf(venta(100.0, efectivo = 70.0)))
        assertEquals(30.0, resumen.montoPendiente, 0.001)
    }

    @Test
    fun productosSeAgrupanYOrdenanPorMonto() {
        val resumen = CalculadoraMetricas.calcularResumen(
            listOf(
                venta(100.0, detalles = listOf(detalle("Refresco", 2.0, 90.0, idProducto = 1), detalle("Agua", 1.0, 10.0, idProducto = 2))),
                venta(45.0, detalles = listOf(detalle("Refresco", 1.0, 45.0, idProducto = 1)))
            )
        )
        assertEquals("Refresco", resumen.productos.first().nombre)
        assertEquals(3.0, resumen.productos.first().cantidad, 0.001)
        assertEquals(135.0, resumen.productos.first().monto, 0.001)
    }

    @Test
    fun totalesPorDiaUbicaCadaVentaEnSuDia() {
        val rango = RangoFechas.deDias(sumarDias(hoy, -6), hoy)
        assertEquals(7, rango.cantidadDias)
        val totales = CalculadoraMetricas.totalesPorDia(
            listOf(
                venta(10.0, fecha = sumarDias(hoy, -6) + 1000),
                venta(20.0, fecha = hoy + 1000),
                venta(5.0, fecha = hoy + 2000),
                venta(99.0, fecha = hoy + 3000, estado = "Cancelada")
            ),
            rango
        )
        assertEquals(listOf(10.0, 0.0, 0.0, 0.0, 0.0, 0.0, 25.0), totales)
    }

    @Test
    fun ticketDelReporteNoPasaDelAnchoDelPapel() {
        val config = ConfiguracionNegocioEntidad(nombreNegocio = "Mi Tienda", tamanoPapelImpresora = 58, columnasPersonalizadas = 32)
        val ventas = listOf(
            venta(15258.20, usuario = "Administrador del Colmado Principal", efectivo = 15258.20,
                detalles = listOf(detalle("Refresco Cola de 20 onzas botella retornable", 48.0, 2160.0))),
            venta(350.0, estado = "Cancelada")
        )
        val datos = DatosReporteVentas(
            desde = hoy,
            hasta = sumarDias(hoy, 1),
            esUnSoloDia = true,
            cajero = null,
            impresoPor = "Administrador",
            resumen = CalculadoraMetricas.calcularResumen(ventas),
            ventas = ventas.map { it.venta },
            opciones = OpcionesReporteVentas(detalleFacturas = true)
        )
        val texto = String(ServicioImpresoraBluetooth.generarBytesReporteVentas(config, datos), Charsets.ISO_8859_1)
            .replace(Regex("\u001B.."), "")
            .replace(Regex("\u001D.."), "")
            .replace(Regex("\u001C."), "")

        assertTrue(texto.contains("REPORTE DE VENTAS"))
        assertTrue(texto.contains("15,258.20"))
        texto.lines().forEach { linea ->
            assertTrue("Línea de ${linea.length} columnas: '$linea'", linea.length <= 32)
        }
    }
}
