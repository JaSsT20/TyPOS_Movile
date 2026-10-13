package com.typdevstudio.typos_movil.utilidades.licencia.nube

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Contenido firmado del token de licencia, ya convertido a tipos útiles.
 * Los nombres JSON (snake_case) son el contrato con la API.
 */
data class PayloadLicencia(
    val licencia: String,
    val deviceHash: String,
    val cliente: String,
    val plan: String,
    val maxDispositivos: Int,
    val maxUsuarios: Int,
    val modulos: List<String>,
    /** Día epoch de `vence`: la licencia es válida hasta el fin de ese día en hora RD. */
    val vence: Long,
    val diasCortesia: Int,
    val emitidoMs: Long,
    val validoHastaMs: Long,
) {
    companion object {

        /**
         * Lee el JSON del payload. Exige los campos del contrato con su tipo correcto;
         * `dias_cortesia` es opcional (0 si falta, es decir, sin cortesía).
         * @throws IllegalArgumentException si el JSON no cumple el contrato.
         */
        fun desdeJson(json: String): PayloadLicencia {
            try {
                val o = JSONObject(json)
                return PayloadLicencia(
                    licencia = o.texto("licencia"),
                    deviceHash = o.texto("device_hash"),
                    cliente = o.texto("cliente"),
                    plan = o.texto("plan"),
                    maxDispositivos = o.entero("max_dispositivos"),
                    maxUsuarios = o.entero("max_usuarios"),
                    modulos = o.listaDeTextos("modulos"),
                    vence = HoraRD.parsearFecha(o.texto("vence")),
                    diasCortesia = if (o.has("dias_cortesia")) o.entero("dias_cortesia") else 0,
                    emitidoMs = HoraRD.parsearInstanteUtc(o.texto("emitido")),
                    validoHastaMs = HoraRD.parsearInstanteUtc(o.texto("valido_hasta")),
                ).also {
                    require(it.maxDispositivos > 0 && it.maxUsuarios > 0 && it.diasCortesia >= 0) {
                        "Límites del token fuera de rango."
                    }
                }
            } catch (e: JSONException) {
                throw IllegalArgumentException("El payload del token no es JSON válido.", e)
            }
        }

        // Se valida el tipo exacto: optString/getInt de org.json convierten tipos en silencio.
        private fun JSONObject.valor(nombre: String): Any {
            require(has(nombre) && !isNull(nombre)) { "Falta el campo '$nombre' en el token." }
            return get(nombre)
        }

        private fun JSONObject.texto(nombre: String): String =
            valor(nombre) as? String ?: throw IllegalArgumentException("El campo '$nombre' debe ser texto.")

        private fun JSONObject.entero(nombre: String): Int {
            val numero = valor(nombre)
            require(numero is Int || (numero is Long && numero in Int.MIN_VALUE..Int.MAX_VALUE)) {
                "El campo '$nombre' debe ser un número entero."
            }
            return (numero as Number).toInt()
        }

        private fun JSONObject.listaDeTextos(nombre: String): List<String> {
            val arreglo = valor(nombre) as? JSONArray
                ?: throw IllegalArgumentException("El campo '$nombre' debe ser una lista.")
            return (0 until arreglo.length()).map { i ->
                arreglo.get(i) as? String ?: throw IllegalArgumentException("El campo '$nombre' solo admite textos.")
            }
        }
    }
}
