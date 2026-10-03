package com.typdevstudio.typos_movil.datos.local.entidades

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ventas",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntidad::class,
            parentColumns = ["id"],
            childColumns = ["id_usuario"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["fecha"]),
        Index(value = ["numero_factura"]),
        Index(value = ["id_usuario"])
    ]
)
data class VentaEntidad(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "numero_factura")
    val numeroFactura: String,

    @ColumnInfo(name = "fecha")
    val fecha: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "id_usuario")
    val idUsuario: Long,

    @ColumnInfo(name = "usuario")
    val usuario: String,

    // Cliente (Opcional)
    @ColumnInfo(name = "nombre_cliente")
    val nombreCliente: String? = null,

    @ColumnInfo(name = "rnc_cedula_cliente")
    val rncCedulaCliente: String? = null,

    // Totales y Desglose Financiero con ITBIS
    @ColumnInfo(name = "subtotal")
    val subTotal: Double,

    @ColumnInfo(name = "descuento")
    val descuento: Double = 0.0,

    @ColumnInfo(name = "subtotal_neto")
    val subTotalNeto: Double,

    @ColumnInfo(name = "monto_itbis")
    val montoItbis: Double,

    @ColumnInfo(name = "total")
    val total: Double,

    @ColumnInfo(name = "monto_recibido")
    val montoRecibido: Double,

    @ColumnInfo(name = "monto_devuelto")
    val montoDevuelto: Double,

    @ColumnInfo(name = "estado_venta")
    val estadoVenta: String = "Completada", // "Completada", "Cancelada"

    // Métodos de Pago
    @ColumnInfo(name = "metodo_pago")
    val metodoPago: String = "Efectivo", // "Efectivo", "Transferencia", "Cheque", "Credito", "Mixto"

    @ColumnInfo(name = "monto_efectivo")
    val montoEfectivo: Double = 0.0,

    @ColumnInfo(name = "monto_transferencia")
    val montoTransferencia: Double = 0.0,

    @ColumnInfo(name = "referencia_transferencia")
    val referenciaTransferencia: String? = null,

    @ColumnInfo(name = "monto_cheque")
    val montoCheque: Double = 0.0,

    @ColumnInfo(name = "numero_cheque")
    val numeroCheque: String? = null,

    @ColumnInfo(name = "banco_cheque")
    val bancoCheque: String? = null,

    @ColumnInfo(name = "monto_credito")
    val montoCredito: Double = 0.0,

    @ColumnInfo(name = "motivo_anulacion")
    val motivoAnulacion: String? = null
)
