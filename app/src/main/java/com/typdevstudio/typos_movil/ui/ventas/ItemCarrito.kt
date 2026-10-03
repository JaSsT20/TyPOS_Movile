package com.typdevstudio.typos_movil.ui.ventas

import com.typdevstudio.typos_movil.datos.local.entidades.ProductoEntidad

data class ItemCarrito(
    val producto: ProductoEntidad,
    val cantidad: Double = 1.0,
    val descuento: Double = 0.0
) {
    val precioUnitario: Double
        get() = producto.precioVenta

    val tasaItbis: Double
        get() = if (producto.exentoItbis) 0.0 else producto.tasaItbis

    // Total del item sin descuento
    val totalBruto: Double
        get() = precioUnitario * cantidad

    // Total final a pagar por este item
    val total: Double
        get() = if (producto.itbisIncluido) {
            (totalBruto - descuento).coerceAtLeast(0.0)
        } else {
            subTotalNeto + montoItbis
        }

    // Subtotal base antes de impuestos y antes de descuento
    val subTotal: Double
        get() = if (producto.itbisIncluido) {
            if (tasaItbis > 0.0) totalBruto / (1.0 + tasaItbis / 100.0) else totalBruto
        } else {
            totalBruto
        }

    // Subtotal base después de descuento (Base Imponible)
    val subTotalNeto: Double
        get() = if (producto.itbisIncluido) {
            if (tasaItbis > 0.0) total / (1.0 + tasaItbis / 100.0) else total
        } else {
            (subTotal - descuento).coerceAtLeast(0.0)
        }

    // Monto de ITBIS calculado o extraído
    val montoItbis: Double
        get() = if (producto.itbisIncluido) {
            (total - subTotalNeto).coerceAtLeast(0.0)
        } else {
            subTotalNeto * (tasaItbis / 100.0)
        }
}
