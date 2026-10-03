package com.typdevstudio.typos_movil.datos.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.typdevstudio.typos_movil.datos.local.entidades.UsuarioEntidad
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    @Query("SELECT * FROM usuarios WHERE nombre_usuario = :nombreUsuario AND esta_activo = 1 LIMIT 1")
    suspend fun obtenerPorNombreUsuario(nombreUsuario: String): UsuarioEntidad?

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: Long): UsuarioEntidad?

    @Query("SELECT * FROM usuarios WHERE esta_activo = 1 ORDER BY nombre_completo ASC")
    fun obtenerTodosActivos(): Flow<List<UsuarioEntidad>>

    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun contarUsuarios(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(usuario: UsuarioEntidad): Long

    @Update
    suspend fun actualizar(usuario: UsuarioEntidad)

    @Query("UPDATE usuarios SET esta_activo = 0 WHERE id = :id")
    suspend fun desactivarUsuario(id: Long)
}
