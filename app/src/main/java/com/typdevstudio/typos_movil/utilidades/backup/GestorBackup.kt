package com.typdevstudio.typos_movil.utilidades.backup

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.sqlite.db.SupportSQLiteDatabase
import com.typdevstudio.typos_movil.BuildConfig
import com.typdevstudio.typos_movil.datos.local.AppBaseDatos
import com.typdevstudio.typos_movil.ui.licencia.LicenciaViewModel
import com.typdevstudio.typos_movil.utilidades.licencia.LicenciaGuard
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Cómo terminó una restauración. Si la base ya se había cerrado, la app debe reiniciarse aunque haya fallado. */
sealed interface ResultadoRestauracion {
    data object Exito : ResultadoRestauracion
    data class Fallo(val error: ErrorBackup, val requiereReinicio: Boolean) : ResultadoRestauracion
}

/**
 * Copias de seguridad de TyPOS: crear (`.typosbk` con base, fotos y manifiesto), listar, importar desde otra app,
 * restaurar de forma segura (con copia de emergencia) y compartir. Cada paso, bueno o malo, se reporta a la API con
 * [ReportadorEventosBackup].
 */
object GestorBackup {

    const val MAXIMO_BACKUPS = 7
    const val PREFIJO_BACKUP = "TyPOS_BK_"
    private const val EXTENSION_ANTIGUA = ".db"
    private const val ETIQUETA = "GestorBackup"
    private const val CARPETA_EMERGENCIA = "restauracion_emergencia"

    /** Mientras exista, la restauración no terminó: si la app se cierra a la mitad, al abrir se vuelve a la copia. */
    private const val MARCA_EN_CURSO = ".en_curso"
    private const val MARGEN_ESPACIO_BYTES = 20L * 1024 * 1024

    /** Directorio donde se almacenan las copias de seguridad. */
    fun obtenerDirectorioBackups(contexto: Context): File {
        val dir = File(contexto.getExternalFilesDir(null), "backups")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun carpetaFotos(contexto: Context) = File(contexto.filesDir, FormatoBackup.CARPETA_FOTOS)

    private fun esArchivoDeCopia(archivo: File) = archivo.isFile && archivo.name.startsWith(PREFIJO_BACKUP) &&
        (archivo.name.endsWith(FormatoBackup.EXTENSION) || archivo.name.endsWith(EXTENSION_ANTIGUA))

    // ------------------------------------------------------------------
    // Crear
    // ------------------------------------------------------------------

    /**
     * Crea la copia `.typosbk`: base de datos (tras vaciar el WAL), fotos de productos y manifiesto con el teléfono
     * de origen y su token. Nombre en disco: `TyPOS_BK_ddMMyyyy_HH-mm-ss.typosbk`.
     */
    fun crearBackup(
        contexto: Context,
        esAutomatico: Boolean = false,
        idOperacion: String = CodigosBackup.nuevaOperacion(),
    ): Result<ArchivoBackupUi> {
        var paso = "leer_base"
        val temporal = File(contexto.cacheDir, "copia_en_curso.db")
        var parcial: File? = null
        return try {
            val db = AppBaseDatos.obtenerBaseDatos(contexto).openHelper.writableDatabase
            try {
                db.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
            } catch (e: Exception) {
                // Si el WAL ya estaba vacío se sigue con la copia.
            }
            val archivoBd = contexto.getDatabasePath(AppBaseDatos.NOMBRE_BD)
            if (!archivoBd.exists()) throw ErrorBackup(CodigosBackup.CREACION_FALLIDA, "no existe la base de datos")
            val conteos = ConteosBackup(
                productos = contar(db, "productos"),
                ventas = contar(db, "ventas"),
                usuarios = contar(db, "usuarios"),
            )

            paso = "copiar_base"
            // Se copia primero: así la base no cambia mientras se comprime y su SHA-256 coincide.
            copiar(archivoBd, temporal)

            paso = "comprimir"
            val ahora = Date()
            val identidad = LicenciaGuard.identidadParaBackup()
            val nombreArchivo = "$PREFIJO_BACKUP${SimpleDateFormat("ddMMyyyy_HH-mm-ss", Locale.US).format(ahora)}${FormatoBackup.EXTENSION}"
            val dir = obtenerDirectorioBackups(contexto)
            val destino = File(dir, nombreArchivo)
            val enCurso = File(dir, ".$nombreArchivo.parcial").also { parcial = it }
            val manifiesto = FormatoBackup.escribir(
                destino = enCurso,
                bd = temporal,
                fotos = carpetaFotos(contexto).listFiles()?.toList().orEmpty(),
                manifiestoBase = ManifiestoBackup(
                    versionFormato = ManifiestoBackup.VERSION_FORMATO_ACTUAL,
                    versionBd = db.version,
                    versionApp = BuildConfig.VERSION_NAME,
                    creadoEn = ReportadorEventosBackup.ahoraIso(ahora),
                    automatico = esAutomatico,
                    deviceHashOrigen = identidad?.deviceHash.orEmpty(),
                    nombreDispositivoOrigen = LicenciaViewModel.nombreTelefonoPorDefecto(),
                    tokenOrigen = identidad?.token,
                    conteos = conteos,
                    archivos = emptyMap(),
                ),
            )
            if (!enCurso.renameTo(destino)) throw IOException("no se pudo renombrar la copia")

            aplicarPoliticaRetencion(contexto)
            ReportadorEventosBackup.registrar(
                contexto, TiposEventoBackup.BACKUP_CREADO, idOperacion,
                detalleTecnico = "automatico=$esAutomatico; productos=${conteos.productos}; ventas=${conteos.ventas}; " +
                    "fotos=${manifiesto.archivos.size - 1}; bytes=${destino.length()}",
            )
            Result.success(aArchivoUi(destino, manifiesto, identidad?.deviceHash))
        } catch (e: Exception) {
            parcial?.delete()
            val error = comoErrorBackup(e, CodigosBackup.CREACION_FALLIDA, paso)
            Log.e(ETIQUETA, "No se pudo crear la copia (${error.codigo}, paso $paso)", e)
            ReportadorEventosBackup.registrar(
                contexto, TiposEventoBackup.BACKUP_FALLIDO, idOperacion, error.codigo, error.paso, error.detalleTecnico,
            )
            Result.failure(error)
        } finally {
            temporal.delete()
        }
    }

    private fun contar(db: SupportSQLiteDatabase, tabla: String): Int = try {
        db.query("SELECT COUNT(*) FROM $tabla").use { if (it.moveToFirst()) it.getInt(0) else 0 }
    } catch (e: Exception) {
        0
    }

    /** Conserva solo las últimas [MAXIMO_BACKUPS] copias (de los dos formatos), borrando las más antiguas. */
    fun aplicarPoliticaRetencion(contexto: Context) {
        try {
            val archivos = obtenerDirectorioBackups(contexto).listFiles { file -> esArchivoDeCopia(file) }
                ?.sortedByDescending { it.lastModified() } ?: return
            archivos.drop(MAXIMO_BACKUPS).forEach { it.delete() }
        } catch (e: Exception) {
            // Ignorar errores menores al purgar archivos antiguos
        }
    }

    fun formatearNombreVisual(nombreArchivo: String): String {
        val sinExtension = nombreArchivo.removeSuffix(FormatoBackup.EXTENSION).removeSuffix(EXTENSION_ANTIGUA)
        val partes = sinExtension.split("_")
        return if (partes.size >= 4 && partes[0] == "TyPOS" && partes[1] == "BK") {
            "TyPOS_BK_${partes[2]}_${partes[3].replace("-", ":")}"
        } else {
            sinExtension
        }
    }

    // ------------------------------------------------------------------
    // Listar
    // ------------------------------------------------------------------

    /** Las últimas copias disponibles (máximo 7), de los dos formatos. */
    fun obtenerListaBackups(contexto: Context): List<ArchivoBackupUi> {
        val archivos = obtenerDirectorioBackups(contexto).listFiles { file -> esArchivoDeCopia(file) }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
        val deviceHash = LicenciaGuard.identidadParaBackup()?.deviceHash
        return archivos.take(MAXIMO_BACKUPS).map { archivo ->
            val manifiesto = if (archivo.name.endsWith(FormatoBackup.EXTENSION)) {
                try {
                    FormatoBackup.leerManifiesto(archivo)
                } catch (e: ErrorBackup) {
                    null
                }
            } else null
            aArchivoUi(archivo, manifiesto, deviceHash)
        }
    }

    private fun aArchivoUi(archivo: File, manifiesto: ManifiestoBackup?, deviceHashLocal: String?): ArchivoBackupUi {
        val esAntiguo = archivo.name.endsWith(EXTENSION_ANTIGUA)
        return ArchivoBackupUi(
            nombreArchivo = archivo.name,
            nombreVisual = formatearNombreVisual(archivo.name),
            fechaCreacion = manifiesto?.let { fechaDe(it.creadoEn) } ?: archivo.lastModified(),
            tamanoBytes = archivo.length(),
            archivo = archivo,
            esAutomatico = manifiesto?.automatico ?: (archivo.name.contains("19-30") || archivo.name.contains("1930")),
            manifiesto = manifiesto,
            esFormatoAntiguo = esAntiguo,
            // Una copia antigua (.db) o sin huella de origen se trata como de este teléfono, igual que antes.
            deEsteTelefono = manifiesto == null || manifiesto.deviceHashOrigen.isBlank() ||
                deviceHashLocal == null || manifiesto.deviceHashOrigen == deviceHashLocal,
            valida = esAntiguo || manifiesto != null,
        )
    }

    private fun fechaDe(iso: String): Long? = try {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(iso)?.time
    } catch (e: Exception) {
        null
    }

    // ------------------------------------------------------------------
    // Importar (desde Descargas, WhatsApp, Drive...)
    // ------------------------------------------------------------------

    /** Copia el archivo elegido a la carpeta de copias, comprueba sus SHA-256 y lee el manifiesto. No restaura nada. */
    fun importarBackup(contexto: Context, uri: Uri, idOperacion: String): Result<ArchivoBackupUi> {
        val dir = obtenerDirectorioBackups(contexto)
        val temporal = File(dir, ".importando_${System.currentTimeMillis()}.tmp")
        var paso = "copiar_archivo"
        return try {
            val entrada = contexto.contentResolver.openInputStream(uri)
                ?: throw ErrorBackup(CodigosBackup.IMPORTACION_FALLIDA, "no se pudo abrir el archivo elegido")
            entrada.use { flujo -> FileOutputStream(temporal).use { flujo.copyTo(it) } }

            paso = "verificar_archivo"
            val manifiesto = FormatoBackup.verificar(temporal)
            if (manifiesto.versionBd > AppBaseDatos.VERSION_BD) {
                throw ErrorBackup(CodigosBackup.VERSION_NUEVA, "version_bd ${manifiesto.versionBd}")
            }

            paso = "guardar_archivo"
            val fecha = fechaDe(manifiesto.creadoEn)?.let { Date(it) } ?: Date()
            val base = "$PREFIJO_BACKUP${SimpleDateFormat("ddMMyyyy_HH-mm-ss", Locale.US).format(fecha)}"
            var destino = File(dir, base + FormatoBackup.EXTENSION)
            if (destino.exists() && FormatoBackup.sha256(destino) == FormatoBackup.sha256(temporal)) {
                temporal.delete() // Ya estaba importada.
            } else {
                var n = 2
                while (destino.exists()) destino = File(dir, "${base}_${n++}${FormatoBackup.EXTENSION}")
                if (!temporal.renameTo(destino)) throw IOException("no se pudo guardar la copia importada")
            }
            // Recién recibida: arriba en la lista y a salvo de la retención.
            destino.setLastModified(System.currentTimeMillis())
            aplicarPoliticaRetencion(contexto)

            val deviceHash = LicenciaGuard.identidadParaBackup()?.deviceHash
            ReportadorEventosBackup.registrar(
                contexto, TiposEventoBackup.BACKUP_IMPORTADO, idOperacion,
                detalleTecnico = "de_otro_telefono=${manifiesto.deviceHashOrigen != deviceHash}; " +
                    "version_bd=${manifiesto.versionBd}; con_token=${manifiesto.tokenOrigen != null}; bytes=${destino.length()}",
            )
            Result.success(aArchivoUi(destino, manifiesto, deviceHash))
        } catch (e: Exception) {
            temporal.delete()
            val error = comoErrorBackup(e, CodigosBackup.IMPORTACION_FALLIDA, paso)
            Log.e(ETIQUETA, "No se pudo importar la copia (${error.codigo}, paso $paso)", e)
            // La importación es el primer paso de una restauración.
            ReportadorEventosBackup.registrar(
                contexto, TiposEventoBackup.RESTAURACION_FALLIDA, idOperacion, error.codigo, "importar:${error.paso}",
                error.detalleTecnico,
            )
            Result.failure(error)
        }
    }

    // ------------------------------------------------------------------
    // Restaurar
    // ------------------------------------------------------------------

    /**
     * Restauración segura:
     * 1. Extrae la copia en una carpeta temporal y comprueba los SHA-256.
     * 2. Revisa la base (integridad y versión) y la prepara: fotos con las rutas de este teléfono y, si la copia vino
     *    de otro teléfono, sin la licencia antigua (en este teléfono nunca validaría).
     * 3. Cierra Room, guarda la base y las fotos actuales como copia de emergencia y pone las nuevas en su lugar.
     * 4. Si algo falla en el paso 3, vuelve a la copia de emergencia: los datos quedan como antes.
     * Al terminar (bien o mal, si ya se cerró la base) la app debe reiniciarse.
     */
    fun restaurarBackup(
        contexto: Context,
        backup: ArchivoBackupUi,
        deOtroTelefono: Boolean,
        idOperacion: String,
    ): ResultadoRestauracion {
        val trabajo = File(contexto.cacheDir, "restauracion")
        val emergencia = File(contexto.filesDir, CARPETA_EMERGENCIA)
        val archivoBd = contexto.getDatabasePath(AppBaseDatos.NOMBRE_BD)
        val fotosActuales = carpetaFotos(contexto)
        var paso = "extraer"
        var baseCerrada = false
        var baseMovida = false
        var fotosMovidas = false

        try {
            trabajo.deleteRecursively()
            trabajo.mkdirs()

            // 1. Extraer y verificar.
            val bdNueva = File(trabajo, FormatoBackup.ENTRADA_BD)
            val conFotos = !backup.esFormatoAntiguo
            if (conFotos) {
                FormatoBackup.extraer(backup.archivo, trabajo)
            } else {
                if (!backup.archivo.exists()) throw ErrorBackup(CodigosBackup.ARCHIVO_DANADO, "el archivo ya no existe")
                if (backup.archivo.length() + MARGEN_ESPACIO_BYTES > trabajo.usableSpace) {
                    throw ErrorBackup(CodigosBackup.ESPACIO_INSUFICIENTE, "se necesitan ${backup.archivo.length()} bytes")
                }
                copiar(backup.archivo, bdNueva)
            }

            // 2. Revisar y preparar la base nueva (todavía no se toca nada del teléfono).
            paso = "verificar_base"
            prepararBase(bdNueva, if (conFotos) fotosActuales.absolutePath else null, borrarLicenciaAntigua = deOtroTelefono)

            // 3. Reemplazar.
            paso = "cerrar_base"
            AppBaseDatos.cerrarInstancia()
            baseCerrada = true

            paso = "copia_emergencia"
            emergencia.deleteRecursively()
            emergencia.mkdirs()
            File(emergencia, MARCA_EN_CURSO).writeText(idOperacion)
            baseMovida = true
            archivosDeBase(archivoBd).filter { it.exists() }.forEach { mover(it, File(emergencia, it.name)) }
            if (conFotos && fotosActuales.exists()) {
                fotosMovidas = true
                mover(fotosActuales, File(emergencia, FormatoBackup.CARPETA_FOTOS))
            }

            paso = "reemplazar_base"
            archivoBd.parentFile?.mkdirs()
            mover(bdNueva, archivoBd)
            // Si algo reabrió la base durante el cambio, sus -wal/-shm no son de la copia: aplicarlos la dañaría.
            archivosDeBase(archivoBd).drop(1).forEach { it.delete() }

            if (conFotos) {
                paso = "reemplazar_fotos"
                fotosMovidas = true
                val fotosNuevas = File(trabajo, FormatoBackup.CARPETA_FOTOS)
                if (fotosNuevas.exists()) mover(fotosNuevas, fotosActuales) else fotosActuales.mkdirs()
            }

            File(emergencia, MARCA_EN_CURSO).delete()
            emergencia.deleteRecursively()
            LicenciaGuard.olvidarVersionUsuarios()
            ReportadorEventosBackup.registrar(
                contexto, TiposEventoBackup.RESTAURACION_EXITOSA, idOperacion,
                detalleTecnico = "de_otro_telefono=$deOtroTelefono; formato=${if (conFotos) "typosbk" else "db"}; " +
                    "version_bd=${backup.manifiesto?.versionBd ?: "-"}",
            )
            return ResultadoRestauracion.Exito
        } catch (e: Exception) {
            val original = comoErrorBackup(e, CodigosBackup.RESTAURACION_FALLIDA, paso)
            Log.e(ETIQUETA, "Falló la restauración (${original.codigo}, paso $paso)", e)
            val error = if (baseCerrada) {
                val rescate = volverACopiaDeEmergencia(archivoBd, fotosActuales, emergencia, baseMovida, fotosMovidas)
                // Ya se tocó el teléfono: el usuario solo necesita saber que sus datos están como antes.
                ErrorBackup(
                    CodigosBackup.RESTAURACION_FALLIDA,
                    "${original.codigo}: ${original.detalleTecnico ?: e.message}${rescate?.let { "; rescate: $it" } ?: ""}",
                    e, paso,
                )
            } else original
            ReportadorEventosBackup.registrar(
                contexto, TiposEventoBackup.RESTAURACION_FALLIDA, idOperacion, error.codigo, error.paso, error.detalleTecnico,
            )
            return ResultadoRestauracion.Fallo(error, requiereReinicio = baseCerrada)
        } finally {
            trabajo.deleteRecursively()
        }
    }

    /**
     * Revisa la base extraída (integridad, versión) y la deja lista: rutas de fotos de este teléfono y, si se pide,
     * sin la licencia antigua. Trabaja sobre la copia temporal: si falla, el teléfono no cambió.
     */
    private fun prepararBase(bd: File, carpetaFotos: String?, borrarLicenciaAntigua: Boolean) {
        val db = try {
            SQLiteDatabase.openDatabase(bd.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
        } catch (e: Exception) {
            throw ErrorBackup(CodigosBackup.ARCHIVO_DANADO, "no se pudo abrir la base: ${e.message}", e)
        }
        try {
            val integridad = db.rawQuery("PRAGMA integrity_check", null).use { if (it.moveToFirst()) it.getString(0) else null }
            if (!integridad.equals("ok", ignoreCase = true)) {
                throw ErrorBackup(CodigosBackup.ARCHIVO_DANADO, "integrity_check: ${integridad?.take(200)}")
            }
            val version = db.version
            if (version > AppBaseDatos.VERSION_BD) throw ErrorBackup(CodigosBackup.VERSION_NUEVA, "user_version $version")
            if (version < AppBaseDatos.VERSION_MINIMA_RESTAURABLE) {
                throw ErrorBackup(CodigosBackup.VERSION_ANTIGUA, "user_version $version")
            }
            if (!tieneTabla(db, "productos")) throw ErrorBackup(CodigosBackup.ARCHIVO_NO_VALIDO, "sin tabla productos")

            db.beginTransaction()
            try {
                if (carpetaFotos != null && tieneColumna(db, "productos", "imagenes_uri")) {
                    db.rawQuery("SELECT id, imagenes_uri FROM productos WHERE imagenes_uri IS NOT NULL", null).use { cursor ->
                        while (cursor.moveToNext()) {
                            val actual = cursor.getString(1)
                            val nueva = FormatoBackup.reescribirRutasImagenes(actual, carpetaFotos)
                            if (nueva != actual) {
                                db.execSQL("UPDATE productos SET imagenes_uri = ? WHERE id = ?", arrayOf<Any?>(nueva, cursor.getLong(0)))
                            }
                        }
                    }
                }
                if (borrarLicenciaAntigua && tieneTabla(db, "licencia")) db.execSQL("DELETE FROM licencia")
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            // Todo dentro del archivo principal: así basta con mover un solo archivo.
            try {
                db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { it.moveToFirst() }
            } catch (e: Exception) {
                // Sin WAL no hay nada que vaciar.
            }
        } finally {
            db.close()
        }
        File(bd.path + "-wal").delete()
        File(bd.path + "-shm").delete()
        File(bd.path + "-journal").delete()
    }

    private fun tieneTabla(db: SQLiteDatabase, tabla: String): Boolean =
        db.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?", arrayOf(tabla))
            .use { it.moveToFirst() && it.getInt(0) == 1 }

    private fun tieneColumna(db: SQLiteDatabase, tabla: String, columna: String): Boolean =
        db.rawQuery("PRAGMA table_info($tabla)", null).use { cursor ->
            val indice = cursor.getColumnIndex("name")
            var encontrada = false
            while (!encontrada && cursor.moveToNext()) encontrada = cursor.getString(indice) == columna
            encontrada
        }

    /** Deja la base y las fotos como estaban. Devuelve un texto si algo no se pudo devolver (para el evento). */
    private fun volverACopiaDeEmergencia(
        archivoBd: File, fotosActuales: File, emergencia: File, baseMovida: Boolean, fotosMovidas: Boolean,
    ): String? = try {
        if (baseMovida) {
            archivosDeBase(archivoBd).forEach { original ->
                val respaldo = File(emergencia, original.name)
                if (respaldo.exists()) {
                    original.delete()
                    mover(respaldo, original)
                } else if (original.name != archivoBd.name) {
                    original.delete() // -wal/-shm de la base nueva
                }
            }
        }
        val fotosRespaldo = File(emergencia, FormatoBackup.CARPETA_FOTOS)
        if (fotosMovidas && fotosRespaldo.exists()) {
            fotosActuales.deleteRecursively()
            mover(fotosRespaldo, fotosActuales)
        }
        emergencia.deleteRecursively()
        null
    } catch (e: Exception) {
        // La copia de emergencia se deja en su carpeta para recuperarla a mano.
        Log.e(ETIQUETA, "No se pudo volver a la copia de emergencia", e)
        e.message ?: e.javaClass.simpleName
    }

    /**
     * Se llama al abrir la app, antes de usar la base. Si una restauración quedó a la mitad (la app se cerró o se
     * apagó el teléfono), devuelve la base y las fotos a como estaban. Devuelve el id de esa operación para reportarla.
     */
    fun recuperarRestauracionInterrumpida(contexto: Context): String? {
        val emergencia = File(contexto.filesDir, CARPETA_EMERGENCIA)
        if (!emergencia.exists()) return null
        val marca = File(emergencia, MARCA_EN_CURSO)
        if (!marca.exists()) {
            // La restauración terminó y solo faltaba borrar la carpeta.
            emergencia.deleteRecursively()
            return null
        }
        val idOperacion = try {
            marca.readText().trim().ifEmpty { CodigosBackup.nuevaOperacion() }
        } catch (e: Exception) {
            CodigosBackup.nuevaOperacion()
        }
        val archivoBd = contexto.getDatabasePath(AppBaseDatos.NOMBRE_BD)
        volverACopiaDeEmergencia(
            archivoBd, carpetaFotos(contexto), emergencia,
            baseMovida = File(emergencia, archivoBd.name).exists(),
            fotosMovidas = true,
        )
        Log.w(ETIQUETA, "Se deshizo una restauración interrumpida (operacion=$idOperacion)")
        return idOperacion
    }

    /** Reporta a la API la restauración interrumpida que [recuperarRestauracionInterrumpida] deshizo. */
    fun reportarRestauracionInterrumpida(contexto: Context, idOperacion: String) {
        ReportadorEventosBackup.registrar(
            contexto, TiposEventoBackup.RESTAURACION_FALLIDA, idOperacion, CodigosBackup.RESTAURACION_FALLIDA,
            "reinicio_interrumpido", "la app se cerró durante el cambio de base; se volvió a la copia de emergencia",
        )
    }

    private fun archivosDeBase(archivoBd: File) =
        listOf(archivoBd, File(archivoBd.path + "-wal"), File(archivoBd.path + "-shm"), File(archivoBd.path + "-journal"))

    // ------------------------------------------------------------------
    // Compartir y eliminar
    // ------------------------------------------------------------------

    /** Comparte el archivo de la copia por el menú del sistema (WhatsApp, Drive, correo...). */
    fun compartirBackup(contexto: Context, archivo: File, idOperacion: String = CodigosBackup.nuevaOperacion()) {
        try {
            if (!archivo.exists()) {
                Toast.makeText(contexto, "El archivo de la copia ya no existe.", Toast.LENGTH_SHORT).show()
                return
            }

            val uri: Uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", archivo)
            val intentCompartir = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                clipData = ClipData.newRawUri(archivo.name, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Copia de seguridad de TyPOS Móvil (${archivo.name})")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Copia de seguridad de TyPOS Móvil: ${archivo.name}. Para usarla, ábrala en TyPOS desde " +
                        "Copias de seguridad > Importar copia.",
                )
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intentCompartir, "Enviar copia de seguridad por...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            contexto.startActivity(chooser)
            ReportadorEventosBackup.registrar(
                contexto, TiposEventoBackup.BACKUP_COMPARTIDO, idOperacion,
                detalleTecnico = "formato=${if (archivo.name.endsWith(FormatoBackup.EXTENSION)) "typosbk" else "db"}; bytes=${archivo.length()}",
            )
        } catch (e: Exception) {
            Log.e(ETIQUETA, "No se pudo compartir la copia", e)
            Toast.makeText(contexto, "No se pudo compartir la copia. Inténtelo de nuevo.", Toast.LENGTH_LONG).show()
        }
    }

    /** Elimina un archivo de copia de seguridad. */
    fun eliminarBackup(archivo: File): Boolean {
        return try {
            if (archivo.exists()) archivo.delete() else true
        } catch (e: Exception) {
            false
        }
    }

    // ------------------------------------------------------------------

    private fun copiar(origen: File, destino: File) {
        try {
            FileInputStream(origen).use { entrada -> FileOutputStream(destino).use { entrada.copyTo(it) } }
        } catch (e: IOException) {
            destino.delete()
            throw e
        }
    }

    /** Mueve (renombra, o copia y borra si no se puede renombrar). */
    private fun mover(origen: File, destino: File) {
        if (origen.renameTo(destino)) return
        if (origen.isDirectory) {
            if (!origen.copyRecursively(destino, overwrite = true)) throw IOException("no se pudo copiar ${origen.name}")
            origen.deleteRecursively()
        } else {
            copiar(origen, destino)
            origen.delete()
        }
    }

    private fun comoErrorBackup(e: Exception, codigoPorDefecto: String, paso: String): ErrorBackup = when {
        e is ErrorBackup -> e.conPaso(paso)
        FormatoBackup.esFaltaDeEspacio(e) -> ErrorBackup(CodigosBackup.ESPACIO_INSUFICIENTE, e.message, e, paso)
        else -> ErrorBackup(codigoPorDefecto, "${e.javaClass.simpleName}: ${e.message}", e, paso)
    }
}
