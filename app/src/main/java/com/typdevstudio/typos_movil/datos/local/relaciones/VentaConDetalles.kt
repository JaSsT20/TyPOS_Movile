package com.typdevstudio.typos_movil.datos.local.relaciones

import androidx.room.Embedded
import androidx.room.Relation
import com.typdevstudio.typos_movil.datos.local.entidades.DetalleVentaEntidad
import com.typdevstudio.typos_movil.datos.local.entidades.VentaEntidad

data class VentaConDetalles(
    @Embedded
    val venta: VentaEntidad,

    @Relation(
        parentColumn = "id",
        entityColumn = "id_venta"
    )
    val detalles: List<DetalleVentaEntidad>
)
