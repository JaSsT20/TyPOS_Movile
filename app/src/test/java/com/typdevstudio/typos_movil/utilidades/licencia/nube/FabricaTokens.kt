package com.typdevstudio.typos_movil.utilidades.licencia.nube

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.time.Instant

/** Utilidades de prueba: genera llaves P-256 y firma tokens igual que la API (DER sobre el payload Base64URL). */
object FabricaTokens {

    const val DEVICE_HASH = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08"

    val parLlaves: KeyPair by lazy { generarParLlaves() }

    fun generarParLlaves(): KeyPair =
        KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()

    fun json(
        licencia: String = "K7QX-9M2D-PL4A",
        deviceHash: String = DEVICE_HASH,
        cliente: String = "Ferretería López",
        vence: String = "2026-12-31",
        diasCortesia: Int = 3,
        emitido: String = "2026-10-12T22:00:00Z",
        validoHasta: String = "2026-10-27T22:00:00Z",
    ): String =
        """{"licencia":"$licencia","device_hash":"$deviceHash","cliente":"$cliente","plan":"pro",""" +
            """"max_dispositivos":3,"max_usuarios":10,"modulos":["inventario","facturacion","cxc"],""" +
            """"vence":"$vence","dias_cortesia":$diasCortesia,"emitido":"$emitido","valido_hasta":"$validoHasta"}"""

    fun firmar(json: String, clavePrivada: PrivateKey = parLlaves.private, algoritmo: String = "SHA256withECDSA"): String {
        val payload = Base64Url.codificar(json.toByteArray(Charsets.UTF_8))
        val firma = Signature.getInstance(algoritmo).run {
            initSign(clavePrivada)
            update(payload.toByteArray(Charsets.US_ASCII))
            sign()
        }
        return payload + "." + Base64Url.codificar(firma)
    }

    fun verificador(): VerificadorToken = VerificadorToken(parLlaves.public)

    /** Payload ya leído, para probar el evaluador sin pasar por la firma. */
    fun payload(
        vence: String = "2026-12-31",
        diasCortesia: Int = 3,
        emitido: Instant = Instant.parse("2026-10-13T11:00:00Z"),
        validoHasta: Instant = Instant.parse("2026-10-28T12:00:00Z"),
        deviceHash: String = DEVICE_HASH,
    ) = PayloadLicencia(
        licencia = "K7QX-9M2D-PL4A",
        deviceHash = deviceHash,
        cliente = "Ferretería López",
        plan = "pro",
        maxDispositivos = 3,
        maxUsuarios = 10,
        modulos = listOf("inventario", "facturacion", "cxc"),
        vence = HoraRD.parsearFecha(vence),
        diasCortesia = diasCortesia,
        emitidoMs = emitido.toEpochMilli(),
        validoHastaMs = validoHasta.toEpochMilli(),
    )
}
