package com.typdevstudio.typos_movil.datos.local.entidades

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "configuracion_negocio")
data class ConfiguracionNegocioEntidad(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 1,

    @ColumnInfo(name = "nombre_negocio")
    val nombreNegocio: String = "Mi Tienda",

    @ColumnInfo(name = "rnc_cedula")
    val rncCedula: String? = null,

    @ColumnInfo(name = "direccion")
    val direccion: String? = null,

    @ColumnInfo(name = "telefono")
    val telefono: String? = null,

    @ColumnInfo(name = "pie_ticket")
    val pieTicket: String = "¡Gracias por su compra!",

    @ColumnInfo(name = "nombre_impresora")
    val nombreImpresora: String? = null,

    @ColumnInfo(name = "direccion_mac_impresora")
    val direccionMacImpresora: String? = null,

    @ColumnInfo(name = "tamano_papel_impresora")
    val tamanoPapelImpresora: Int = 58, // 58, 80, 57, 72 o 0 (Personalizado)

    @ColumnInfo(name = "columnas_personalizadas")
    val columnasPersonalizadas: Int = 32, // Número de caracteres por línea

    @ColumnInfo(name = "modo_tema")
    val modoTema: Int = 0 // 0: Seguir el Sistema, 1: Modo Claro, 2: Modo Oscuro
)
