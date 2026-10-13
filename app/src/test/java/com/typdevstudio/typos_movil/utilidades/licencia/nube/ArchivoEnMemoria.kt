package com.typdevstudio.typos_movil.utilidades.licencia.nube

/** [ArchivoSeguro] en memoria para pruebas: el cifrado real con Keystore solo existe en el teléfono. */
class ArchivoEnMemoria : ArchivoSeguro {
    val valores = mutableMapOf<String, String>()

    override fun leer(nombre: String) = valores[nombre]

    override fun escribir(valores: Map<String, String?>) {
        for ((nombre, valor) in valores) if (valor == null) this.valores.remove(nombre) else this.valores[nombre] = valor
    }
}

/** [RegistroLicencia] que guarda los mensajes, para comprobar que nunca contienen claves completas. */
class RegistroEnMemoria : RegistroLicencia {
    val mensajes = mutableListOf<String>()
    override fun info(mensaje: String) { mensajes += mensaje }
    override fun advertencia(mensaje: String) { mensajes += mensaje }
}
