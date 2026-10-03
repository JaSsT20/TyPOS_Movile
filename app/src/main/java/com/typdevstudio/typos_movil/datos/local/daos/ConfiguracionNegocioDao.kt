package com.typdevstudio.typos_movil.datos.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.typdevstudio.typos_movil.datos.local.entidades.ConfiguracionNegocioEntidad
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfiguracionNegocioDao {

    @Query("SELECT * FROM configuracion_negocio WHERE id = 1 LIMIT 1")
    fun obtenerConfiguracion(): Flow<ConfiguracionNegocioEntidad?>

    @Query("SELECT * FROM configuracion_negocio WHERE id = 1 LIMIT 1")
    suspend fun obtenerConfiguracionDirecta(): ConfiguracionNegocioEntidad?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarConfiguracion(configuracion: ConfiguracionNegocioEntidad)

    @Update
    suspend fun actualizarConfiguracion(configuracion: ConfiguracionNegocioEntidad)
}
