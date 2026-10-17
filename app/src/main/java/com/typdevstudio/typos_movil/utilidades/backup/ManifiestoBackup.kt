package com.typdevstudio.typos_movil.utilidades.backup

import org.json.JSONException
import org.json.JSONObject

/** Cuántos registros trae la copia, para mostrar un resumen antes de restaurar. */
data class ConteosBackup(val productos: Int, val ventas: Int, val usuarios: Int)

/**
 * Describe una copia `.typosbk` y de qué teléfono salió. Va dentro del ZIP como `manifiesto.json`.
 *
 * [tokenOrigen] es el último token firmado por el servidor que tenía el teléfono de origen (o `null` si no tenía la
 * licencia nueva). Prueba a qué licencia pertenecía ese teléfono y permite restaurar la copia en otro.
 * [archivos] guarda el SHA-256 (hex) de cada archivo del ZIP, para detectar uno dañado o modificado.
 */
data class ManifiestoBackup(
    val versionFormato: Int,
    val versionBd: Int,
    val versionApp: String,
    val creadoEn: String,
    val automatico: Boolean,
    val deviceHashOrigen: String,
    val nombreDispositivoOrigen: String?,
    val tokenOrigen: String?,
    val conteos: ConteosBackup,
    val archivos: Map<String, String>,
) {
    fun aJson(): String {
        val hashes = JSONObject()
        archivos.toSortedMap().forEach { (ruta, sha) -> hashes.put(ruta, sha) }
        return JSONObject()
            .put("version_formato", versionFormato)
            .put("version_bd", versionBd)
            .put("version_app", versionApp)
            .put("creado_en", creadoEn)
            .put("automatico", automatico)
            .put("device_hash_origen", deviceHashOrigen)
            .put("nombre_dispositivo_origen", nombreDispositivoOrigen ?: JSONObject.NULL)
            .put("token_origen", tokenOrigen ?: JSONObject.NULL)
            .put(
                "conteos",
                JSONObject()
                    .put("productos", conteos.productos)
                    .put("ventas", conteos.ventas)
                    .put("usuarios", conteos.usuarios)
            )
            .put("sha256", hashes)
            .toString(2)
    }

    companion object {
        const val VERSION_FORMATO_ACTUAL = 1

        /** @throws ErrorBackup [CodigosBackup.ARCHIVO_NO_VALIDO] si el JSON no tiene la forma esperada. */
        fun desdeJson(json: String): ManifiestoBackup = try {
            val o = JSONObject(json)
            val conteos = o.optJSONObject("conteos")
            val hashes = o.getJSONObject("sha256")
            ManifiestoBackup(
                versionFormato = o.getInt("version_formato"),
                versionBd = o.getInt("version_bd"),
                versionApp = o.optString("version_app"),
                creadoEn = o.getString("creado_en"),
                automatico = o.optBoolean("automatico", false),
                deviceHashOrigen = o.getString("device_hash_origen"),
                nombreDispositivoOrigen = textoOpcional(o, "nombre_dispositivo_origen"),
                tokenOrigen = textoOpcional(o, "token_origen"),
                conteos = ConteosBackup(
                    productos = conteos?.optInt("productos") ?: 0,
                    ventas = conteos?.optInt("ventas") ?: 0,
                    usuarios = conteos?.optInt("usuarios") ?: 0,
                ),
                archivos = hashes.keys().asSequence().associateWith { hashes.getString(it) },
            )
        } catch (e: JSONException) {
            throw ErrorBackup(CodigosBackup.ARCHIVO_NO_VALIDO, "manifiesto ilegible: ${e.message}", e)
        }

        private fun textoOpcional(o: JSONObject, campo: String): String? =
            if (o.isNull(campo)) null else o.optString(campo).takeIf { it.isNotBlank() }
    }
}
