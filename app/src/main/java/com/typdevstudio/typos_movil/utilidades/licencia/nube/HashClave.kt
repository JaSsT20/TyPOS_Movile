package com.typdevstudio.typos_movil.utilidades.licencia.nube

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Hash de contraseñas de usuarios: PBKDF2-HMAC-SHA256 con sal aleatoria, en el formato
 * `pbkdf2_sha256$iteraciones$sal$hash` (sal y hash en Base64URL sin relleno).
 *
 * Se calcula en el teléfono: la API recibe y reparte solo este texto, nunca la contraseña real.
 * Se implementa a mano sobre `HmacSHA256` porque `PBKDF2WithHmacSHA256` no existe antes de Android 8 (minSdk 24).
 */
object HashClave {

    const val PREFIJO = "pbkdf2_sha256"
    const val ITERACIONES = 60_000
    private const val LARGO_SAL = 16
    private const val LARGO_HASH = 32
    private val aleatorio = SecureRandom()

    /** `true` si el valor guardado ya es un hash (y no una contraseña antigua en texto plano). */
    fun esHash(valor: String): Boolean = valor.startsWith("$PREFIJO$")

    fun generar(clave: String, iteraciones: Int = ITERACIONES): String {
        val sal = ByteArray(LARGO_SAL).also(aleatorio::nextBytes)
        val hash = pbkdf2Sha256(clave.toByteArray(Charsets.UTF_8), sal, iteraciones, LARGO_HASH)
        return "$PREFIJO$$iteraciones$${Base64Url.codificar(sal)}$${Base64Url.codificar(hash)}"
    }

    /** Compara en tiempo constante. Un valor que no es hash válido nunca coincide. */
    fun verificar(clave: String, almacenado: String): Boolean {
        val partes = almacenado.split('$')
        if (partes.size != 4 || partes[0] != PREFIJO) return false
        val iteraciones = partes[1].toIntOrNull()?.takeIf { it in 1..10_000_000 } ?: return false
        return try {
            val sal = Base64Url.decodificar(partes[2])
            val esperado = Base64Url.decodificar(partes[3])
            val calculado = pbkdf2Sha256(clave.toByteArray(Charsets.UTF_8), sal, iteraciones, esperado.size)
            MessageDigest.isEqual(esperado, calculado)
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    /** PBKDF2 (RFC 8018) con HMAC-SHA256. */
    internal fun pbkdf2Sha256(clave: ByteArray, sal: ByteArray, iteraciones: Int, largo: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        // HMAC admite claves vacías, pero SecretKeySpec no: se usa un byte 0 equivalente para HMAC.
        mac.init(SecretKeySpec(if (clave.isEmpty()) ByteArray(1) else clave, "HmacSHA256"))
        val salida = ByteArray(largo)
        var bloque = 1
        var escritos = 0
        while (escritos < largo) {
            mac.update(sal)
            mac.update(byteArrayOf((bloque ushr 24).toByte(), (bloque ushr 16).toByte(), (bloque ushr 8).toByte(), bloque.toByte()))
            var u = mac.doFinal()
            val t = u.copyOf()
            for (i in 1 until iteraciones) {
                u = mac.doFinal(u)
                for (j in t.indices) t[j] = (t[j].toInt() xor u[j].toInt()).toByte()
            }
            val copiar = minOf(t.size, largo - escritos)
            System.arraycopy(t, 0, salida, escritos, copiar)
            escritos += copiar
            bloque++
        }
        return salida
    }
}
