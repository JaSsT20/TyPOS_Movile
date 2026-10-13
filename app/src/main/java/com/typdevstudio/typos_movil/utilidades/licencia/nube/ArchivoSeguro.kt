package com.typdevstudio.typos_movil.utilidades.licencia.nube

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Almacén clave/valor de textos. Se abstrae para probar [AlmacenLicencia] sin Android. */
interface ArchivoSeguro {
    fun leer(nombre: String): String?

    /** Escribe todos los valores de una vez; `null` borra el campo. */
    fun escribir(valores: Map<String, String?>)
}

/**
 * [ArchivoSeguro] sobre `SharedPreferences` con cada valor cifrado con AES-256-GCM. La llave vive en el
 * Android Keystore: nunca sale del teléfono ni viaja en respaldos.
 *
 * El nombre del campo va como dato autenticado (AAD), así que no se puede mover un valor cifrado a otro campo.
 * Si un valor no se puede descifrar (llave perdida, archivo restaurado en otro teléfono), se trata como
 * ausente: en el peor caso hay que activar de nuevo, lo que es idempotente en el servidor.
 *
 * El archivo está excluido del respaldo automático de Android (`backup_rules.xml` y `data_extraction_rules.xml`)
 * y no es parte del respaldo de la base de datos que hace la app.
 */
class PreferenciasCifradas(contexto: Context) : ArchivoSeguro {

    private val preferencias: SharedPreferences =
        contexto.applicationContext.getSharedPreferences(NOMBRE_ARCHIVO, Context.MODE_PRIVATE)

    override fun leer(nombre: String): String? {
        val cifrado = preferencias.getString(nombre, null) ?: return null
        return try {
            val datos = Base64Url.decodificar(cifrado)
            require(datos.size > LARGO_IV) { "Valor cifrado demasiado corto." }
            val cifrador = Cipher.getInstance(TRANSFORMACION)
            cifrador.init(Cipher.DECRYPT_MODE, obtenerLlave(), GCMParameterSpec(BITS_ETIQUETA, datos, 0, LARGO_IV))
            cifrador.updateAAD(nombre.toByteArray(Charsets.UTF_8))
            String(cifrador.doFinal(datos, LARGO_IV, datos.size - LARGO_IV), Charsets.UTF_8)
        } catch (e: GeneralSecurityException) {
            Log.w(ETIQUETA_LOG, "No se pudo descifrar el campo '$nombre' de la licencia; se ignora.")
            null
        } catch (e: IllegalArgumentException) {
            Log.w(ETIQUETA_LOG, "El campo '$nombre' de la licencia está dañado; se ignora.")
            null
        }
    }

    override fun escribir(valores: Map<String, String?>) {
        val editor = preferencias.edit()
        for ((nombre, valor) in valores) {
            if (valor == null) editor.remove(nombre) else editor.putString(nombre, cifrar(nombre, valor))
        }
        // commit (síncrono) para que un bloqueo o un token nuevo no se pierdan si el proceso muere.
        if (!editor.commit()) Log.w(ETIQUETA_LOG, "No se pudo guardar el estado de la licencia.")
    }

    private fun cifrar(nombre: String, valor: String): String {
        val cifrador = Cipher.getInstance(TRANSFORMACION)
        cifrador.init(Cipher.ENCRYPT_MODE, obtenerLlave())
        cifrador.updateAAD(nombre.toByteArray(Charsets.UTF_8))
        val textoCifrado = cifrador.doFinal(valor.toByteArray(Charsets.UTF_8))
        return Base64Url.codificar(cifrador.iv + textoCifrado)
    }

    private companion object {
        const val NOMBRE_ARCHIVO = "licencia_nube"
        const val ALIAS_LLAVE = "typos_licencia_nube"
        const val PROVEEDOR = "AndroidKeyStore"
        const val TRANSFORMACION = "AES/GCM/NoPadding"
        const val LARGO_IV = 12
        const val BITS_ETIQUETA = 128
        const val ETIQUETA_LOG = "LicenciaNube"

        @Synchronized
        fun obtenerLlave(): SecretKey {
            val almacen = KeyStore.getInstance(PROVEEDOR).apply { load(null) }
            (almacen.getKey(ALIAS_LLAVE, null) as? SecretKey)?.let { return it }

            val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVEEDOR)
            generador.init(
                KeyGenParameterSpec.Builder(ALIAS_LLAVE, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            return generador.generateKey()
        }
    }
}
