package com.typdevstudio.typos_movil.utilidades.backup

import java.util.UUID

/**
 * Catálogo de errores de las copias de seguridad. El mismo código aparece en la pantalla (con el código de soporte),
 * en los eventos que la app reporta a la API y en los logs del servidor. El mensaje es para el usuario: sin palabras
 * técnicas; el detalle técnico solo viaja en el evento.
 */
object CodigosBackup {
    const val ARCHIVO_DANADO = "BK_ARCHIVO_DANADO"
    const val ARCHIVO_NO_VALIDO = "BK_ARCHIVO_NO_VALIDO"
    const val VERSION_NUEVA = "BK_VERSION_NUEVA"
    const val VERSION_ANTIGUA = "BK_VERSION_ANTIGUA"
    const val ESPACIO_INSUFICIENTE = "BK_ESPACIO_INSUFICIENTE"
    const val SIN_INTERNET = "BK_SIN_INTERNET"
    const val OTRA_LICENCIA = "BK_OTRA_LICENCIA"
    const val SIN_LICENCIA_ORIGEN = "BK_SIN_LICENCIA_ORIGEN"
    const val RESTAURACION_FALLIDA = "BK_RESTAURACION_FALLIDA"
    const val CREACION_FALLIDA = "BK_CREACION_FALLIDA"
    const val IMPORTACION_FALLIDA = "BK_IMPORTACION_FALLIDA"

    fun mensaje(codigo: String): String = when (codigo) {
        ARCHIVO_DANADO -> "Este archivo de copia está dañado o incompleto. Pida que se lo envíen otra vez."
        ARCHIVO_NO_VALIDO -> "Este archivo no es una copia de seguridad de TyPOS."
        VERSION_NUEVA -> "Esta copia se hizo con una versión más nueva de TyPOS. Actualice la app e inténtelo de nuevo."
        VERSION_ANTIGUA -> "Esta copia es de una versión muy antigua de TyPOS y no se puede restaurar."
        ESPACIO_INSUFICIENTE -> "No hay espacio suficiente en el teléfono. Libere espacio e inténtelo de nuevo."
        SIN_INTERNET -> "Conéctese a internet para restaurar una copia de otro teléfono."
        OTRA_LICENCIA -> "Esta copia pertenece a otra cuenta. Comuníquese con soporte."
        SIN_LICENCIA_ORIGEN ->
            "Esta copia viene de un teléfono que no tenía la licencia nueva. Active primero este teléfono con su clave " +
                "de licencia y luego restaure la copia."
        RESTAURACION_FALLIDA -> "No se pudo restaurar la copia. Sus datos están como antes."
        CREACION_FALLIDA -> "No se pudo crear la copia de seguridad. Inténtelo de nuevo."
        IMPORTACION_FALLIDA -> "No se pudo recibir el archivo de copia. Inténtelo de nuevo."
        else -> "Ocurrió un problema con la copia de seguridad. Inténtelo de nuevo."
    }

    /**
     * Mensaje para un rechazo de la API al consultar o hacer la transferencia (restaurar una copia de otro teléfono).
     * Los códigos conocidos usan el texto del catálogo, pensado para este momento; los demás, el mensaje de la API.
     */
    fun mensajeDeTransferencia(codigo: String, mensajeApi: String): String = when (codigo) {
        "TOKEN_ORIGEN_INVALIDO" -> "No pudimos confirmar de dónde viene esta copia. Comuníquese con soporte."
        "LIMITE_TRANSFERENCIAS" -> "Ya cambió de teléfono varias veces este mes. Comuníquese con soporte para continuar."
        "DISPOSITIVO_EN_OTRA_LICENCIA" -> mensaje(OTRA_LICENCIA)
        "LIMITE_DISPOSITIVOS" -> "Su plan ya no tiene espacio para otro teléfono. Comuníquese con soporte."
        "LICENCIA_VENCIDA" -> "Su licencia está vencida. Renuévela para restaurar esta copia."
        "LICENCIA_SUSPENDIDA" -> "Su licencia está suspendida. Comuníquese con soporte."
        "LICENCIA_NO_ENCONTRADA" -> "La licencia de esta copia ya no existe. Comuníquese con soporte."
        "DISPOSITIVO_REVOCADO" -> "Este teléfono fue desvinculado de su cuenta. Comuníquese con soporte."
        "ERROR_INTERNO" -> "Ocurrió un problema de nuestro lado. Inténtelo en unos minutos."
        else -> mensajeApi.ifBlank { mensaje(RESTAURACION_FALLIDA) }
    }

    /** `Código de soporte: BK-4F2A`: las primeras 4 letras del id de la operación, para buscarla en los logs. */
    fun codigoSoporte(idOperacion: String, prefijo: String = "BK"): String =
        "$prefijo-${idOperacion.filter { it.isLetterOrDigit() }.take(4).uppercase()}"

    fun nuevaOperacion(): String = UUID.randomUUID().toString()
}

/**
 * Falla de una operación de copia con su código del catálogo. [detalleTecnico] es para el evento que se reporta a la
 * API (nunca se muestra al usuario ni lleva datos del negocio). [paso] dice dónde falló.
 */
class ErrorBackup(
    val codigo: String,
    val detalleTecnico: String? = null,
    causa: Throwable? = null,
    val paso: String? = null,
) : Exception("$codigo${detalleTecnico?.let { ": $it" } ?: ""}", causa) {
    val mensajeUsuario: String get() = CodigosBackup.mensaje(codigo)

    fun conPaso(paso: String) = if (this.paso != null) this else ErrorBackup(codigo, detalleTecnico, cause, paso)
}
