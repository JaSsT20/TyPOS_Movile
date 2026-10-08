package com.typdevstudio.typos_movil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.typdevstudio.typos_movil.datos.local.AppBaseDatos
import com.typdevstudio.typos_movil.ui.navegacion.NavegacionApp
import com.typdevstudio.typos_movil.ui.theme.TyPOS_MovilTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val bd = AppBaseDatos.obtenerBaseDatos(this)
        val configuracionFlow = bd.configuracionNegocioDao().obtenerConfiguracion()

        // Asegurar que la alarma para el backup diario de las 7:30 PM esté programada
        com.typdevstudio.typos_movil.utilidades.backup.PlanificadorBackupDiario.programarBackupDiario(this)

        setContent {
            val config by configuracionFlow.collectAsState(initial = null)
            val modoTema = config?.modoTema ?: 0

            TyPOS_MovilTheme(modoTema = modoTema) {
                NavegacionApp()
            }
        }
    }
}