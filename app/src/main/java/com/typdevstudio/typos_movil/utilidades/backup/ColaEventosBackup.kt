package com.typdevstudio.typos_movil.utilidades.backup

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File

/** Tipos de evento que la API acepta en `/eventos/backup`. */
object TiposEventoBackup {
    const val BACKUP_CREADO = "backup_creado"
    const val BACKUP_FALLIDO = "backup_fallido"
    const val BACKUP_COMPARTIDO = "backup_compartido"
    const val BACKUP_IMPORTADO = "backup_importado"
    const val RESTAURACION_EXITOSA = "restauracion_exitosa"
    const val RESTAURACION_FALLIDA = "restauracion_fallida"
}

/**
 * Lo que pasó con una copia de seguridad, tal como se envía a la API. [ocurridoEn] es la hora real (ISO-8601), así
 * un evento que llega tarde no confunde el orden.
 */
data class EventoBackup(
    val tipo: String,
    val idOperacion: String,
    val ocurridoEn: String,
    val deviceHash: String,
    val clave: String? = null,
    val codigo: String? = null,
    val paso: String? = null,
    val detalleTecnico: String? = null,
    val versionApp: String? = null,
) {
    fun aJson(): JSONObject = JSONObject()
        .put("tipo", tipo)
        .put("id_operacion", idOperacion)
        .put("ocurrido_en", ocurridoEn)
        .put("device_hash", deviceHash)
        .put("clave", clave ?: JSONObject.NULL)
        .put("codigo", codigo ?: JSONObject.NULL)
        .put("paso", paso ?: JSONObject.NULL)
        .put("detalle_tecnico", detalleTecnico?.take(MAXIMO_DETALLE) ?: JSONObject.NULL)
        .put("version_app", versionApp ?: JSONObject.NULL)

    companion object {
        const val MAXIMO_DETALLE = 500

        fun desdeJson(o: JSONObject) = EventoBackup(
            tipo = o.getString("tipo"),
            idOperacion = o.getString("id_operacion"),
            ocurridoEn = o.getString("ocurrido_en"),
            deviceHash = o.getString("device_hash"),
            clave = texto(o, "clave"),
            codigo = texto(o, "codigo"),
            paso = texto(o, "paso"),
            detalleTecnico = texto(o, "detalle_tecnico"),
            versionApp = texto(o, "version_app"),
        )

        private fun texto(o: JSONObject, campo: String) = if (o.isNull(campo)) null else o.optString(campo)
    }
}

/**
 * Cola local de eventos pendientes de enviar (un archivo JSON). Sin internet nada se pierde: se envían en lotes
 * cuando vuelve la conexión. Si se acumulan demasiados, se descartan los más viejos.
 */
class ColaEventosBackup(private val archivo: File, private val maximo: Int = 500) {

    @Synchronized
    fun agregar(evento: EventoBackup) {
        guardar((leer() + evento).takeLast(maximo))
    }

    /** Los primeros [cantidad] eventos, en el orden en que ocurrieron. */
    @Synchronized
    fun primeros(cantidad: Int): List<EventoBackup> = leer().take(cantidad)

    /**
     * Quita los eventos ya enviados. Se buscan por contenido y no por posición: si mientras se enviaba el lote se
     * descartaron los más viejos por exceso, no se borra por error un evento que todavía no se envió.
     */
    @Synchronized
    fun quitar(enviados: List<EventoBackup>) {
        val restantes = leer().toMutableList()
        enviados.forEach { restantes.remove(it) }
        guardar(restantes)
    }

    @Synchronized
    fun tamano(): Int = leer().size

    private fun leer(): List<EventoBackup> {
        if (!archivo.exists()) return emptyList()
        return try {
            val arreglo = JSONArray(archivo.readText(Charsets.UTF_8))
            (0 until arreglo.length()).mapNotNull { i ->
                try {
                    EventoBackup.desdeJson(arreglo.getJSONObject(i))
                } catch (e: JSONException) {
                    null
                }
            }
        } catch (e: Exception) {
            // Un archivo dañado no debe trabar las copias: se empieza de nuevo.
            emptyList()
        }
    }

    private fun guardar(eventos: List<EventoBackup>) {
        if (eventos.isEmpty()) {
            archivo.delete()
            return
        }
        val arreglo = JSONArray()
        eventos.forEach { arreglo.put(it.aJson()) }
        val temporal = File(archivo.parentFile, archivo.name + ".tmp")
        temporal.writeText(arreglo.toString(), Charsets.UTF_8)
        if (!temporal.renameTo(archivo)) {
            archivo.delete()
            temporal.renameTo(archivo)
        }
    }
}
