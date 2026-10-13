package com.typdevstudio.typos_movil.utilidades.licencia.nube

/** Motivos por los que la app entra en modo restringido. */
enum class MotivoRestriccion {
    SIN_ACTIVAR,
    TOKEN_EXPIRADO,
    SUSPENDIDA,
    VENCIDA,
    REVOCADO,
    FIRMA_INVALIDA,
    DISPOSITIVO_DISTINTO,
    RELOJ_ALTERADO;

    companion object {
        /**
         * Traduce un código de error de la API al bloqueo que comunica, o `null` si ese código
         * no debe restringir (errores técnicos, de red, 5xx o 429: nunca castigan al cliente).
         */
        fun desdeCodigoServidor(codigo: String?): MotivoRestriccion? = when (codigo) {
            "LICENCIA_SUSPENDIDA" -> SUSPENDIDA
            "LICENCIA_VENCIDA" -> VENCIDA
            "DISPOSITIVO_REVOCADO" -> REVOCADO
            // La clave ya no existe o el teléfono ya no está en la licencia: hay que activar de nuevo.
            "LICENCIA_NO_ENCONTRADA", "DISPOSITIVO_NO_REGISTRADO" -> SIN_ACTIVAR
            else -> null
        }
    }
}

/** Estado de la licencia nueva. Es la única fuente que consultan el resto de la app. */
sealed class EstadoLicencia {

    /** `true` si se pueden emitir ventas y documentos nuevos. */
    abstract val permiteEmitir: Boolean

    /** No hay token guardado. Mientras dure la transición, manda la licencia antigua. */
    data object SinActivar : EstadoLicencia() {
        override val permiteEmitir = false
    }

    data class Activa(val payload: PayloadLicencia) : EstadoLicencia() {
        override val permiteEmitir = true
    }

    /** Activa, pero faltan 3 días o menos para `valido_hasta`: hay que conectarse para validar. */
    data class ActivaPorValidar(val payload: PayloadLicencia) : EstadoLicencia() {
        override val permiteEmitir = true
    }

    /**
     * Ya pasó `vence`, pero sigue dentro de la cortesía.
     * [diasRestantes] = `vence` + `dias_cortesia` − hoy (0 = último día).
     */
    data class EnCortesia(val payload: PayloadLicencia, val diasRestantes: Int) : EstadoLicencia() {
        override val permiteEmitir = true
    }

    /** Modo restringido: se consulta y se respalda, pero no se emite. */
    data class Restringida(val motivo: MotivoRestriccion, val payload: PayloadLicencia? = null) : EstadoLicencia() {
        override val permiteEmitir = false
    }
}
