package com.typdevstudio.typos_movil.datos.local.entidades

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usuarios",
    indices = [
        Index(value = ["nombre_usuario"], unique = true)
    ]
)
data class UsuarioEntidad(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "nombre_completo")
    val nombreCompleto: String,

    @ColumnInfo(name = "nombre_usuario")
    val nombreUsuario: String,

    @ColumnInfo(name = "clave")
    val clave: String,

    @ColumnInfo(name = "rol")
    val rol: String = "CAJERO", // "ADMINISTRADOR" o "CAJERO"

    @ColumnInfo(name = "foto_uri")
    val fotoUri: String? = null,

    @ColumnInfo(name = "esta_activo")
    val estaActivo: Boolean = true,

    @ColumnInfo(name = "creado_en")
    val creadoEn: Long = System.currentTimeMillis()
)
