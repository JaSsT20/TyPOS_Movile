package com.typdevstudio.typos_movil.datos.local.entidades

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "licencia")
data class LicenciaEntidad(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 1,

    @ColumnInfo(name = "clave_licencia")
    val claveLicencia: String,

    @ColumnInfo(name = "fecha_emision")
    val fechaEmision: Long,

    @ColumnInfo(name = "fecha_activacion")
    val fechaActivacion: Long,

    @ColumnInfo(name = "fecha_vencimiento")
    val fechaVencimiento: Long,

    @ColumnInfo(name = "dias_totales")
    val diasTotales: Int,

    @ColumnInfo(name = "estado")
    val estado: String = "ACTIVA", // "ACTIVA", "VENCIDA", "MANIPULADA"

    @ColumnInfo(name = "ultima_fecha_uso")
    val ultimaFechaUso: Long = System.currentTimeMillis()
)
