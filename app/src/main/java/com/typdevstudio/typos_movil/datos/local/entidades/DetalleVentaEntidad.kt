package com.typdevstudio.typos_movil.datos.local.entidades

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "detalles_venta",
    foreignKeys = [
        ForeignKey(
            entity = VentaEntidad::class,
            parentColumns = ["id"],
            childColumns = ["id_venta"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductoEntidad::class,
            parentColumns = ["id"],
            childColumns = ["id_producto"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["id_venta"]),
        Index(value = ["id_producto"])
    ]
)
data class DetalleVentaEntidad(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "id_venta")
    val idVenta: Long,

    @ColumnInfo(name = "id_producto")
    val idProducto: Long?,

    @ColumnInfo(name = "codigo_producto")
    val codigoProducto: String? = null,

    @ColumnInfo(name = "nombre")
    val nombre: String,

    @ColumnInfo(name = "precio")
    val precio: Double,

    @ColumnInfo(name = "cantidad")
    val cantidad: Double = 1.0,

    @ColumnInfo(name = "subtotal")
    val subTotal: Double,

    @ColumnInfo(name = "descuento")
    val descuento: Double = 0.0,

    @ColumnInfo(name = "subtotal_neto")
    val subTotalNeto: Double,

    @ColumnInfo(name = "tasa_itbis")
    val tasaItbis: Double = 18.0,

    @ColumnInfo(name = "monto_itbis")
    val montoItbis: Double = 0.0,

    @ColumnInfo(name = "total")
    val total: Double,

    @ColumnInfo(name = "tipo")
    val tipo: String = "Producto" // "Producto" o "Servicio"
)
