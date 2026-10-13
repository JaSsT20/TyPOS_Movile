package com.typdevstudio.typos_movil.utilidades.licencia.nube

/**
 * Función pura que decide el estado de la licencia. No lee el reloj ni el almacenamiento:
 * todo llega por parámetro para poder probarla.
 */
object EvaluadorEstadoLicencia {

    /** Margen para diferencias normales de reloj entre el teléfono y el servidor. */
    const val TOLERANCIA_RELOJ_MS = 10L * 60 * 1000

    /** Con 3 días o menos para `valido_hasta` se pide validar en línea. */
    const val ANTICIPACION_POR_VALIDAR_MS = 3L * HoraRD.MS_POR_DIA

    /**
     * @param token resultado de verificar el token guardado, o `null` si no hay ninguno.
     * @param deviceHashLocal `device_hash` de este teléfono.
     * @param ahoraMs hora actual del teléfono, en milisegundos epoch.
     * @param bloqueoServidor bloqueo comunicado por la API. Se respeta aunque el token siga vigente.
     * @param ultimaFechaVistaMs la hora más alta vista antes por la app, o `null` si no hay registro.
     */
    fun evaluar(
        token: ResultadoVerificacion?,
        deviceHashLocal: String,
        ahoraMs: Long,
        bloqueoServidor: MotivoRestriccion?,
        ultimaFechaVistaMs: Long?,
    ): EstadoLicencia {
        val payload = (token as? ResultadoVerificacion.Valido)?.payload

        if (bloqueoServidor != null) return EstadoLicencia.Restringida(bloqueoServidor, payload)
        if (token == null) return EstadoLicencia.SinActivar
        if (payload == null) return EstadoLicencia.Restringida(MotivoRestriccion.FIRMA_INVALIDA)

        if (!payload.deviceHash.equals(deviceHashLocal, ignoreCase = true)) {
            return EstadoLicencia.Restringida(MotivoRestriccion.DISPOSITIVO_DISTINTO, payload)
        }

        val relojAtrasado = (ultimaFechaVistaMs != null && ahoraMs < ultimaFechaVistaMs - TOLERANCIA_RELOJ_MS) ||
            ahoraMs < payload.emitidoMs - TOLERANCIA_RELOJ_MS
        if (relojAtrasado) return EstadoLicencia.Restringida(MotivoRestriccion.RELOJ_ALTERADO, payload)

        if (ahoraMs >= payload.validoHastaMs) {
            return EstadoLicencia.Restringida(MotivoRestriccion.TOKEN_EXPIRADO, payload)
        }

        val hoy = HoraRD.diaEnRD(ahoraMs)
        if (hoy > payload.vence) {
            val diasRestantes = payload.vence + payload.diasCortesia - hoy
            // El servidor ya limita valido_hasta al fin de la cortesía; esto es solo defensa adicional.
            return if (diasRestantes >= 0) {
                EstadoLicencia.EnCortesia(payload, diasRestantes.toInt())
            } else {
                EstadoLicencia.Restringida(MotivoRestriccion.VENCIDA, payload)
            }
        }

        return if (payload.validoHastaMs - ahoraMs <= ANTICIPACION_POR_VALIDAR_MS) {
            EstadoLicencia.ActivaPorValidar(payload)
        } else {
            EstadoLicencia.Activa(payload)
        }
    }

    /** Nueva "hora más alta vista": nunca retrocede, aunque el reloj del teléfono sí lo haga. */
    fun nuevaUltimaFechaVista(anteriorMs: Long?, ahoraMs: Long): Long =
        if (anteriorMs == null) ahoraMs else maxOf(anteriorMs, ahoraMs)
}
