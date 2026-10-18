package com.typdevstudio.typos_movil.utilidades.backup

import android.content.Context
import android.util.Log
import com.typdevstudio.typos_movil.BuildConfig
import com.typdevstudio.typos_movil.utilidades.licencia.LicenciaGuard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Avisa a la API de todo lo que pasa con las copias de seguridad (creadas, fallidas, compartidas, importadas,
 * restauradas). Cada evento se guarda primero en una cola local y se envía en lotes cuando hay internet, sin
 * molestar al usuario ni frenar la app.
 */
object ReportadorEventosBackup {

    private const val ETIQUETA = "EventosBackup"
    private const val ARCHIVO_COLA = "eventos_backup_pendientes.json"
    private const val TAMANO_LOTE = 50

    /** Pausa entre lotes para no chocar con el límite de solicitudes de la API (30 por minuto). */
    private const val PAUSA_ENTRE_LOTES_MS = 3_000L

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val enviando = Mutex()

    /** Guarda el evento y trata de enviarlo enseguida. Nunca lanza excepciones. */
    fun registrar(
        contexto: Context,
        tipo: String,
        idOperacion: String,
        codigo: String? = null,
        paso: String? = null,
        detalleTecnico: String? = null,
    ) {
        try {
            val identidad = LicenciaGuard.identidadParaBackup() ?: return
            val evento = EventoBackup(
                tipo = tipo,
                idOperacion = idOperacion,
                ocurridoEn = ahoraIso(),
                deviceHash = identidad.deviceHash,
                clave = identidad.clave,
                codigo = codigo,
                paso = paso,
                detalleTecnico = detalleTecnico?.replace(Regex("\\s+"), " ")?.take(EventoBackup.MAXIMO_DETALLE),
                versionApp = BuildConfig.VERSION_NAME,
            )
            cola(contexto).agregar(evento)
            Log.i(ETIQUETA, "$tipo codigo=${codigo ?: "-"} paso=${paso ?: "-"} operacion=$idOperacion")
            alcance.launch { enviarPendientes(contexto) }
        } catch (e: Exception) {
            Log.e(ETIQUETA, "No se pudo guardar el evento $tipo", e)
        }
    }

    /** Envía la cola en lotes de 50. Si no hay conexión, se queda para el siguiente intento. */
    suspend fun enviarPendientes(contexto: Context) {
        if (!enviando.tryLock()) return
        try {
            val cola = cola(contexto)
            while (true) {
                val lote = cola.primeros(TAMANO_LOTE)
                if (lote.isEmpty()) break
                val arreglo = JSONArray().apply { lote.forEach { put(it.aJson()) } }
                if (!LicenciaGuard.enviarEventosBackup(arreglo)) break
                cola.quitar(lote)
                if (cola.tamano() > 0) delay(PAUSA_ENTRE_LOTES_MS)
            }
        } catch (e: Exception) {
            Log.e(ETIQUETA, "No se pudieron enviar los eventos pendientes", e)
        } finally {
            enviando.unlock()
        }
    }

    /** Una sola instancia para toda la app: sus métodos sincronizados evitan que registrar y enviar pisen el archivo. */
    @Volatile
    private var colaCompartida: ColaEventosBackup? = null

    private fun cola(contexto: Context): ColaEventosBackup =
        colaCompartida ?: synchronized(this) {
            colaCompartida ?: ColaEventosBackup(File(contexto.applicationContext.filesDir, ARCHIVO_COLA))
                .also { colaCompartida = it }
        }

    fun ahoraIso(fecha: Date = Date()): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(fecha)
}
