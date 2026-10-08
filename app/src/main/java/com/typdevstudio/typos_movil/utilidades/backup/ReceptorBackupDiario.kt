package com.typdevstudio.typos_movil.utilidades.backup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver que atiende el disparo de la alarma a las 7:30 p.m. o el reinicio del sistema (BOOT_COMPLETED).
 */
class ReceptorBackupDiario : BroadcastReceiver() {

    companion object {
        const val ACCION_BACKUP_AUTOMATICO = "com.typdevstudio.typos_movil.ACCION_BACKUP_AUTOMATICO"
    }

    override fun onReceive(contexto: Context, intent: Intent) {
        val accion = intent.action

        if (accion == Intent.ACTION_BOOT_COMPLETED || accion == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Re-programar la alarma tras reinicio o actualización
            PlanificadorBackupDiario.programarBackupDiario(contexto)
            return
        }

        if (accion == ACCION_BACKUP_AUTOMATICO) {
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val resultado = GestorBackup.crearBackup(contexto, esAutomatico = true)
                    if (resultado.isSuccess) {
                        PlanificadorBackupDiario.registrarBackupAutomaticoCompletado(contexto)
                    }
                } finally {
                    // Reprogramar para el día siguiente a las 7:30 p.m.
                    PlanificadorBackupDiario.programarBackupDiario(contexto)
                    pendingResult.finish()
                }
            }
        }
    }
}
