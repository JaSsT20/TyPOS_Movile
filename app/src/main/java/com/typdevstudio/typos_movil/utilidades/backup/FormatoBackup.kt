package com.typdevstudio.typos_movil.utilidades.backup

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Formato `.typosbk`: un ZIP con la base de datos, las fotos de productos y `manifiesto.json`. Sin Android: se prueba
 * en la JVM.
 *
 * Dentro del ZIP:
 * - `manifiesto.json` (primero, para leerlo rápido)
 * - `typos_movil_bd.db`
 * - `fotos_productos/<nombre>` (una entrada por foto, sin subcarpetas)
 */
object FormatoBackup {
    const val EXTENSION = ".typosbk"
    const val ENTRADA_MANIFIESTO = "manifiesto.json"
    const val ENTRADA_BD = "typos_movil_bd.db"
    const val CARPETA_FOTOS = "fotos_productos"

    /** Límite de seguridad contra archivos manipulados (ZIP bomba): 4 GB descomprimidos en total. */
    private const val MAXIMO_TOTAL_BYTES = 4L * 1024 * 1024 * 1024
    private const val MAXIMO_MANIFIESTO_BYTES = 256 * 1024

    private val NOMBRE_FOTO_VALIDO = Regex("[A-Za-z0-9._-]{1,200}")

    /**
     * Escribe el `.typosbk` en [destino]. Calcula el SHA-256 de cada archivo y lo pone en el manifiesto.
     * [manifiestoBase] trae todo menos los hashes.
     */
    fun escribir(destino: File, bd: File, fotos: List<File>, manifiestoBase: ManifiestoBackup): ManifiestoBackup {
        val fotosValidas = fotos.filter { it.isFile && NOMBRE_FOTO_VALIDO.matches(it.name) }.distinctBy { it.name }
        val hashes = linkedMapOf(ENTRADA_BD to sha256(bd))
        fotosValidas.forEach { hashes["$CARPETA_FOTOS/${it.name}"] = sha256(it) }
        val manifiesto = manifiestoBase.copy(archivos = hashes)

        ZipOutputStream(FileOutputStream(destino).buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(ENTRADA_MANIFIESTO))
            zip.write(manifiesto.aJson().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry(ENTRADA_BD))
            FileInputStream(bd).use { it.copyTo(zip) }
            zip.closeEntry()

            fotosValidas.forEach { foto ->
                zip.putNextEntry(ZipEntry("$CARPETA_FOTOS/${foto.name}"))
                FileInputStream(foto).use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return manifiesto
    }

    /** Lee solo el manifiesto. No verifica los hashes (ver [verificar]). */
    fun leerManifiesto(archivo: File): ManifiestoBackup = abrir(archivo) { zip ->
        val entrada = zip.getEntry(ENTRADA_MANIFIESTO)
            ?: throw ErrorBackup(CodigosBackup.ARCHIVO_NO_VALIDO, "sin manifiesto.json")
        val bytes = zip.getInputStream(entrada).use { leerLimitado(it, MAXIMO_MANIFIESTO_BYTES) }
        ManifiestoBackup.desdeJson(String(bytes, Charsets.UTF_8)).also { validarManifiesto(it) }
    }

    /** Recorre el ZIP y comprueba el SHA-256 de cada archivo del manifiesto, sin extraer nada. */
    fun verificar(archivo: File): ManifiestoBackup {
        val manifiesto = leerManifiesto(archivo)
        abrir(archivo) { zip ->
            var total = 0L
            manifiesto.archivos.forEach { (ruta, esperado) ->
                val entrada = zip.getEntry(ruta) ?: throw ErrorBackup(CodigosBackup.ARCHIVO_DANADO, "falta $ruta")
                val (hash, bytes) = zip.getInputStream(entrada).use { hashearFlujo(it, null, MAXIMO_TOTAL_BYTES - total) }
                total += bytes
                if (!hash.equals(esperado, ignoreCase = true)) {
                    throw ErrorBackup(CodigosBackup.ARCHIVO_DANADO, "sha256 distinto en $ruta")
                }
            }
        }
        return manifiesto
    }

    /**
     * Extrae la base y las fotos en [carpeta] (que debe estar vacía) comprobando cada SHA-256. Solo se extraen las
     * rutas del manifiesto, nunca fuera de [carpeta]. Antes revisa que haya espacio.
     */
    fun extraer(archivo: File, carpeta: File, espacioLibre: (File) -> Long = { it.usableSpace }): ManifiestoBackup {
        val manifiesto = leerManifiesto(archivo)
        carpeta.mkdirs()
        abrir(archivo) { zip ->
            val entradas = manifiesto.archivos.keys.associateWith { ruta ->
                zip.getEntry(ruta) ?: throw ErrorBackup(CodigosBackup.ARCHIVO_DANADO, "falta $ruta")
            }
            val necesario = entradas.values.sumOf { it.size.coerceAtLeast(0) }
            if (necesario > MAXIMO_TOTAL_BYTES) throw ErrorBackup(CodigosBackup.ARCHIVO_NO_VALIDO, "contenido demasiado grande")
            // Margen de 20 MB para que el teléfono no se quede sin espacio al terminar.
            if (necesario + 20L * 1024 * 1024 > espacioLibre(carpeta)) {
                throw ErrorBackup(CodigosBackup.ESPACIO_INSUFICIENTE, "se necesitan $necesario bytes")
            }

            var total = 0L
            entradas.forEach { (ruta, entrada) ->
                val destino = File(carpeta, ruta)
                if (!destino.canonicalPath.startsWith(carpeta.canonicalPath + File.separator)) {
                    throw ErrorBackup(CodigosBackup.ARCHIVO_NO_VALIDO, "ruta no permitida")
                }
                destino.parentFile?.mkdirs()
                val (hash, bytes) = try {
                    zip.getInputStream(entrada).use { entradaZip ->
                        FileOutputStream(destino).use { salida -> hashearFlujo(entradaZip, salida, MAXIMO_TOTAL_BYTES - total) }
                    }
                } catch (e: IOException) {
                    throw if (esFaltaDeEspacio(e)) ErrorBackup(CodigosBackup.ESPACIO_INSUFICIENTE, e.message, e)
                    else ErrorBackup(CodigosBackup.ARCHIVO_DANADO, "no se pudo extraer $ruta: ${e.message}", e)
                }
                total += bytes
                if (!hash.equals(manifiesto.archivos[ruta], ignoreCase = true)) {
                    throw ErrorBackup(CodigosBackup.ARCHIVO_DANADO, "sha256 distinto en $ruta")
                }
            }
        }
        return manifiesto
    }

    /**
     * Rutas de las fotos de un producto (separadas por coma) apuntando a la carpeta de fotos de este teléfono.
     * Solo cambia las que están en `fotos_productos`; las demás quedan igual.
     */
    fun reescribirRutasImagenes(valor: String?, carpetaFotos: String): String? {
        if (valor.isNullOrBlank()) return valor
        return valor.split(",").map { it.trim() }.filter { it.isNotEmpty() }.joinToString(",") { ruta ->
            val nombre = ruta.substringAfterLast('/')
            if (ruta.contains("/$CARPETA_FOTOS/") && NOMBRE_FOTO_VALIDO.matches(nombre)) "$carpetaFotos/$nombre" else ruta
        }
    }

    fun sha256(archivo: File): String = FileInputStream(archivo).use { hashearFlujo(it, null, Long.MAX_VALUE).first }

    fun esFaltaDeEspacio(e: Throwable): Boolean =
        generateSequence(e) { it.cause }.any { it.message?.contains("ENOSPC") == true || it.message?.contains("No space") == true }

    private fun validarManifiesto(manifiesto: ManifiestoBackup) {
        if (manifiesto.versionFormato > ManifiestoBackup.VERSION_FORMATO_ACTUAL) {
            throw ErrorBackup(CodigosBackup.VERSION_NUEVA, "version_formato ${manifiesto.versionFormato}")
        }
        if (ENTRADA_BD !in manifiesto.archivos) throw ErrorBackup(CodigosBackup.ARCHIVO_NO_VALIDO, "sin base de datos")
        val rutaInvalida = manifiesto.archivos.keys.firstOrNull { ruta ->
            ruta != ENTRADA_BD &&
                !(ruta.startsWith("$CARPETA_FOTOS/") && NOMBRE_FOTO_VALIDO.matches(ruta.removePrefix("$CARPETA_FOTOS/")))
        }
        if (rutaInvalida != null) throw ErrorBackup(CodigosBackup.ARCHIVO_NO_VALIDO, "ruta no permitida en el manifiesto")
    }

    private fun <T> abrir(archivo: File, bloque: (ZipFile) -> T): T {
        val zip = try {
            ZipFile(archivo)
        } catch (e: ZipException) {
            throw ErrorBackup(CodigosBackup.ARCHIVO_NO_VALIDO, "no es un ZIP: ${e.message}", e)
        } catch (e: IOException) {
            throw ErrorBackup(CodigosBackup.ARCHIVO_DANADO, "no se pudo abrir: ${e.message}", e)
        }
        return try {
            zip.use(bloque)
        } catch (e: ErrorBackup) {
            throw e
        } catch (e: IOException) {
            throw ErrorBackup(CodigosBackup.ARCHIVO_DANADO, "lectura fallida: ${e.message}", e)
        }
    }

    /** Copia (si hay [salida]) y calcula el SHA-256. Devuelve el hash y los bytes leídos. */
    private fun hashearFlujo(entrada: InputStream, salida: OutputStream?, maximo: Long): Pair<String, Long> {
        val digest = MessageDigest.getInstance("SHA-256")
        val bufer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val leidos = entrada.read(bufer)
            if (leidos < 0) break
            total += leidos
            if (total > maximo) throw ErrorBackup(CodigosBackup.ARCHIVO_NO_VALIDO, "contenido demasiado grande")
            digest.update(bufer, 0, leidos)
            salida?.write(bufer, 0, leidos)
        }
        return digest.digest().joinToString("") { "%02x".format(it) } to total
    }

    private fun leerLimitado(entrada: InputStream, maximo: Int): ByteArray {
        val bytes = entrada.readBytes()
        if (bytes.size > maximo) throw ErrorBackup(CodigosBackup.ARCHIVO_NO_VALIDO, "manifiesto demasiado grande")
        return bytes
    }
}
