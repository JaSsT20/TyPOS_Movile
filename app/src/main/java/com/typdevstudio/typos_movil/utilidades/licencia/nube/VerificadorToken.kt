package com.typdevstudio.typos_movil.utilidades.licencia.nube

import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.X509EncodedKeySpec

/** Resultado de verificar un token: firma correcta y payload legible, o el motivo del rechazo. */
sealed interface ResultadoVerificacion {
    data class Valido(val payload: PayloadLicencia) : ResultadoVerificacion
    data class Invalido(val motivo: String) : ResultadoVerificacion
}

/**
 * Verifica tokens `PAYLOAD_B64URL.FIRMA_B64URL` emitidos por la API de licencias.
 *
 * - Firma ECDSA P-256 / SHA-256 en formato DER, que es el nativo de `SHA256withECDSA`.
 * - Lo firmado son los bytes ASCII de `PAYLOAD_B64URL` tal como llegó: no se vuelve a serializar el JSON.
 *
 * Solo comprueba firma y formato. Las reglas de negocio (dispositivo, fechas, reloj)
 * las aplica [EvaluadorEstadoLicencia].
 */
class VerificadorToken(private val clavePublica: PublicKey) {

    fun verificar(token: String): ResultadoVerificacion {
        if (token.length > LONGITUD_MAXIMA_TOKEN) return ResultadoVerificacion.Invalido("Token demasiado largo.")
        val partes = token.split('.')
        if (partes.size != 2 || partes[0].isEmpty() || partes[1].isEmpty()) {
            return ResultadoVerificacion.Invalido("El token no tiene el formato PAYLOAD.FIRMA.")
        }
        val (payloadBase64Url, firmaBase64Url) = partes

        val firmaValida = try {
            Signature.getInstance(ALGORITMO_FIRMA).run {
                initVerify(clavePublica)
                update(payloadBase64Url.toByteArray(Charsets.US_ASCII))
                verify(Base64Url.decodificar(firmaBase64Url))
            }
        } catch (e: IllegalArgumentException) {
            false // Base64 inválido
        } catch (e: GeneralSecurityException) {
            false // DER malformado
        }
        if (!firmaValida) return ResultadoVerificacion.Invalido("La firma del token no es válida.")

        return try {
            val json = String(Base64Url.decodificar(payloadBase64Url), Charsets.UTF_8)
            ResultadoVerificacion.Valido(PayloadLicencia.desdeJson(json))
        } catch (e: IllegalArgumentException) {
            ResultadoVerificacion.Invalido(e.message ?: "El contenido del token no es válido.")
        }
    }

    companion object {
        const val ALGORITMO_FIRMA = "SHA256withECDSA"

        /** Un token real ronda los 700 caracteres; esto solo evita procesar basura enorme. */
        private const val LONGITUD_MAXIMA_TOKEN = 8 * 1024

        /** Crea el verificador a partir de la clave pública en Base64 de SubjectPublicKeyInfo (X.509 DER). */
        fun desdeClavePublicaBase64(clavePublicaBase64: String): VerificadorToken =
            VerificadorToken(cargarClavePublica(clavePublicaBase64))

        /**
         * Carga una clave pública EC P-256 en Base64 de SubjectPublicKeyInfo, como la entrega
         * `LicenciasApi generar-llaves` en `clave_publica_base64.txt`.
         * @throws IllegalArgumentException si no es una clave EC de 256 bits.
         */
        fun cargarClavePublica(clavePublicaBase64: String): PublicKey {
            val clave = try {
                KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(Base64Url.decodificar(clavePublicaBase64.trim())))
            } catch (e: GeneralSecurityException) {
                throw IllegalArgumentException("La clave pública no es una clave EC válida.", e)
            }
            require(clave is ECPublicKey && clave.params.curve.field.fieldSize == 256) {
                "La clave pública debe ser ECDSA P-256."
            }
            return clave
        }
    }
}
