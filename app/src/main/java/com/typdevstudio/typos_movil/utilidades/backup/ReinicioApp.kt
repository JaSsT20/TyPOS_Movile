package com.typdevstudio.typos_movil.utilidades.backup

import android.content.Context
import android.content.Intent
import kotlin.system.exitProcess

/** Reinicia la app desde cero (tras restaurar una copia, para que todo se cargue con la base nueva). */
object ReinicioApp {
    fun reiniciar(contexto: Context) {
        val aplicacion = contexto.applicationContext
        val lanzador = aplicacion.packageManager.getLaunchIntentForPackage(aplicacion.packageName)
        val componente = lanzador?.component
        if (componente != null) {
            aplicacion.startActivity(Intent.makeRestartActivityTask(componente))
        }
        Runtime.getRuntime().exit(0)
        exitProcess(0)
    }
}
