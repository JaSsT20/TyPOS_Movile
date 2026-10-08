package com.typdevstudio.typos_movil.utilidades.backup

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object PlanificadorBackupDiario {

    private const val CODIGO_SOLICITUD_BACKUP = 730
    private const val PREFS_BACKUP = "typos_backup_pref"
    private const val KEY_ULTIMO_BACKUP_AUTO = "ultimo_backup_automatico_fecha"

    /**
     * Programa la ejecución del backup automático diario exactamente para las 7:30 p.m. (19:30 hrs).
     */
    fun programarBackupDiario(contexto: Context) {
        try {
            val alarmManager = contexto.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            val intent = Intent(contexto, ReceptorBackupDiario::class.java).apply {
                action = ReceptorBackupDiario.ACCION_BACKUP_AUTOMATICO
            }

            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getBroadcast(
                contexto,
                CODIGO_SOLICITUD_BACKUP,
                intent,
                flags
            )

            val calendario = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, 19)
                set(Calendar.MINUTE, 30)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Si ya pasaron las 7:30 p.m. del día de hoy, programar para las 7:30 p.m. de mañana
            if (calendario.timeInMillis <= System.currentTimeMillis()) {
                calendario.add(Calendar.DAY_OF_YEAR, 1)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendario.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendario.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            // Ignorar excepciones de permisos de alarma exacta en Android 12+ si no están concedidos
        }
    }

    /**
     * Registra que el backup automático de hoy ya fue completado.
     */
    fun registrarBackupAutomaticoCompletado(contexto: Context) {
        val hoyStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val prefs = contexto.getSharedPreferences(PREFS_BACKUP, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ULTIMO_BACKUP_AUTO, hoyStr).apply()
    }

    /**
     * Verifica si hoy ya pasaron las 7:30 p.m. y no se ha generado la copia automática del día
     * (por ejemplo, si el teléfono estaba apagado a las 7:30 p.m.).
     */
    fun verificarSiFaltaBackupHoy(contexto: Context): Boolean {
        val cal = Calendar.getInstance()
        val horaActual = cal.get(Calendar.HOUR_OF_DAY)
        val minutoActual = cal.get(Calendar.MINUTE)

        val esDespuesDeLas730Pm = (horaActual > 19) || (horaActual == 19 && minutoActual >= 30)
        if (!esDespuesDeLas730Pm) return false

        val hoyStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val prefs = contexto.getSharedPreferences(PREFS_BACKUP, Context.MODE_PRIVATE)
        val ultimoRealizado = prefs.getString(KEY_ULTIMO_BACKUP_AUTO, null)

        return ultimoRealizado != hoyStr
    }
}
