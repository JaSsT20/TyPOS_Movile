package com.typdevstudio.typos_movil.datos.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.typdevstudio.typos_movil.datos.local.entidades.LicenciaEntidad
import kotlinx.coroutines.flow.Flow

@Dao
interface LicenciaDao {

    @Query("SELECT * FROM licencia WHERE id = 1 LIMIT 1")
    fun obtenerLicencia(): Flow<LicenciaEntidad?>

    @Query("SELECT * FROM licencia WHERE id = 1 LIMIT 1")
    suspend fun obtenerLicenciaDirecta(): LicenciaEntidad?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarLicencia(licencia: LicenciaEntidad)

    @Update
    suspend fun actualizarLicencia(licencia: LicenciaEntidad)

    @Query("DELETE FROM licencia")
    suspend fun eliminarLicencia()
}
