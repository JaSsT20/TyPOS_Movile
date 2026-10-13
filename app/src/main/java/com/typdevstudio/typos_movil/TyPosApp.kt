package com.typdevstudio.typos_movil

import android.app.Application
import android.util.Log
import com.typdevstudio.typos_movil.ui.error.PantallaErrorActivity
import com.typdevstudio.typos_movil.utilidades.licencia.LicenciaGuard
import com.typdevstudio.typos_movil.utilidades.logs.GestorLogsError
import kotlin.system.exitProcess

class TyPosApp : Application() {

    override fun onCreate() {
        super.onCreate()
        configurarCapturadorGlobalErrores()
        LicenciaGuard.inicializar(this)
    }

    private fun configurarCapturadorGlobalErrores() {
        val manejadorPorDefecto = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { hilo, throwable ->
            try {
                Log.e("TyPosApp", "Excepción no controlada capturada en el hilo ${hilo.name}", throwable)

                // 1. Guardar log completo en archivo de texto en el dispositivo
                val archivoLog = GestorLogsError.guardarError(this, throwable, hilo)
                val reporte = GestorLogsError.construirReporteError(this, throwable, hilo)

                // 2. Abrir la pantalla de error informativa para el usuario
                PantallaErrorActivity.iniciar(
                    contexto = this,
                    detallesError = reporte,
                    rutaLog = archivoLog.absolutePath
                )
            } catch (e: Exception) {
                Log.e("TyPosApp", "Error al procesar la excepción no controlada", e)
                manejadorPorDefecto?.uncaughtException(hilo, throwable)
            } finally {
                // 3. Matar el proceso actual defectuoso para evitar estados inconsistentes
                android.os.Process.killProcess(android.os.Process.myPid())
                exitProcess(10)
            }
        }
    }
}
