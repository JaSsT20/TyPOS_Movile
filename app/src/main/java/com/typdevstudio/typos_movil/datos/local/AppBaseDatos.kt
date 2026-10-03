package com.typdevstudio.typos_movil.datos.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.typdevstudio.typos_movil.datos.local.daos.ConfiguracionNegocioDao
import com.typdevstudio.typos_movil.datos.local.daos.ProductoDao
import com.typdevstudio.typos_movil.datos.local.daos.UsuarioDao
import com.typdevstudio.typos_movil.datos.local.daos.VentaDao
import com.typdevstudio.typos_movil.datos.local.entidades.ConfiguracionNegocioEntidad
import com.typdevstudio.typos_movil.datos.local.entidades.DetalleVentaEntidad
import com.typdevstudio.typos_movil.datos.local.entidades.ProductoEntidad
import com.typdevstudio.typos_movil.datos.local.entidades.UsuarioEntidad
import com.typdevstudio.typos_movil.datos.local.entidades.VentaEntidad
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val MIGRACION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE configuracion_negocio ADD COLUMN modo_tema INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(
    entities = [
        UsuarioEntidad::class,
        ProductoEntidad::class,
        VentaEntidad::class,
        DetalleVentaEntidad::class,
        ConfiguracionNegocioEntidad::class
    ],
    version = 6, // Versión 6: Campo modo_tema en ConfiguracionNegocioEntidad
    exportSchema = false
)
abstract class AppBaseDatos : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun productoDao(): ProductoDao
    abstract fun ventaDao(): VentaDao
    abstract fun configuracionNegocioDao(): ConfiguracionNegocioDao

    companion object {
        @Volatile
        private var INSTANCIA: AppBaseDatos? = null

        fun obtenerBaseDatos(contexto: Context): AppBaseDatos {
            return INSTANCIA ?: synchronized(this) {
                val instancia = Room.databaseBuilder(
                    contexto.applicationContext,
                    AppBaseDatos::class.java,
                    "typos_movil_bd.db"
                )
                    .addMigrations(MIGRACION_5_6)
                    .addCallback(CallbackInicial(contexto))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCIA = instancia
                instancia
            }
        }
    }

    private class CallbackInicial(private val contexto: Context) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            sembrarDatosIniciales()
        }

        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            super.onDestructiveMigration(db)
            sembrarDatosIniciales()
        }

        private fun sembrarDatosIniciales() {
            CoroutineScope(Dispatchers.IO).launch {
                val baseDatos = obtenerBaseDatos(contexto)
                // Usuario administrador inicial de fábrica
                baseDatos.usuarioDao().insertar(
                    UsuarioEntidad(
                        nombreCompleto = "Administrador",
                        nombreUsuario = "admin",
                        clave = "admin",
                        rol = "ADMINISTRADOR",
                        estaActivo = true
                    )
                )

                // Configuración inicial del negocio
                baseDatos.configuracionNegocioDao().guardarConfiguracion(
                    ConfiguracionNegocioEntidad(
                        id = 1,
                        nombreNegocio = "TyPOS Móvil",
                        pieTicket = "¡Gracias por su compra!",
                        tamanoPapelImpresora = 58,
                        columnasPersonalizadas = 32
                    )
                )
            }
        }
    }
}
