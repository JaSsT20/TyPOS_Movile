package com.typdevstudio.typos_movil.datos.local.entidades

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "productos",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntidad::class,
            parentColumns = ["id"],
            childColumns = ["id_usuario"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["codigo_barras"]),
        Index(value = ["nombre"]),
        Index(value = ["id_usuario"])
    ]
)
data class ProductoEntidad(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "id_usuario")
    val idUsuario: Long? = null,

    @ColumnInfo(name = "codigo_barras")
    val codigoBarras: String? = null,

    @ColumnInfo(name = "nombre")
    val nombre: String,

    @ColumnInfo(name = "descripcion")
    val descripcion: String? = null,

    @ColumnInfo(name = "precio_costo")
    val precioCosto: Double = 0.0,

    @ColumnInfo(name = "precio_venta")
    val precioVenta: Double,

    @ColumnInfo(name = "exento_itbis")
    val exentoItbis: Boolean = false,

    @ColumnInfo(name = "itbis_incluido")
    val itbisIncluido: Boolean = true,

    @ColumnInfo(name = "tasa_itbis")
    val tasaItbis: Double = 18.0,

    @ColumnInfo(name = "stock")
    val stock: Double = 0.0,

    @ColumnInfo(name = "controla_stock")
    val controlaStock: Boolean = true,

    @ColumnInfo(name = "categoria")
    val categoria: String = "General",

    @ColumnInfo(name = "esta_activo")
    val estaActivo: Boolean = true,

    @ColumnInfo(name = "imagenes_uri")
    val imagenesUri: String? = null,

    @ColumnInfo(name = "creado_en")
    val creadoEn: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "actualizado_en")
    val actualizadoEn: Long = System.currentTimeMillis()
) {
    val listaImagenes: List<String>
        get() = imagenesUri?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    val imagenPrincipal: String?
        get() = listaImagenes.firstOrNull()
}
