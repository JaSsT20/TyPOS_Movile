package com.typdevstudio.typos_movil.utilidades.importacion

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

object GestorPlantillaProductos {

    const val NOMBRE_ARCHIVO_PLANTILLA = "TyPOS_PlantillaProductos.csv"

    /**
     * Cabeceras estándar en español, legibles y compatibles con Excel / LibreOffice / Google Sheets.
     */
    val CABECERAS_ESTANDAR = listOf(
        "codigo_barras",
        "nombre",
        "descripcion",
        "precio_costo",
        "precio_venta",
        "stock",
        "controla_stock",
        "categoria",
        "exento_itbis",
        "itbis_incluido",
        "tasa_itbis"
    )

    /**
     * Genera el contenido del CSV estándar con BOM UTF-8 para apertura directa en Microsoft Excel.
     */
    fun generarContenidoCsvPlantilla(): String {
        val sb = StringBuilder()

        // Encabezados
        sb.append(CABECERAS_ESTANDAR.joinToString(","))
        sb.append("\r\n")

        // Filas de ejemplo realistas
        sb.append("7501031311309,Refresco Cola 500ml,Bebida carbonatada personal,25.00,45.00,48,SI,Bebidas,NO,SI,18\r\n")
        sb.append("7461234567890,Arroz Selecto 2lb,Arroz blanco calidad premium grano largo,75.00,95.00,30,SI,Abarrotes,SI,NO,0\r\n")
        sb.append("7891000245123,Aceite Vegetal 1L,Aceite comestible de soya 100% puro,120.00,160.00,24,SI,Abarrotes,NO,SI,18\r\n")
        sb.append("012345678912,Galletas de Chocolate 6pk,Paquete familiar de galletas 6 unidades,50.00,75.00,20,SI,Snacks,NO,SI,18\r\n")
        sb.append("SERV-001,Servicio de Delivery,Servicio de entrega a domicilio express,0.00,100.00,0,NO,Servicios,SI,NO,0\r\n")
        sb.append("7460011223344,Agua Purificada 16oz,Botella de agua purificada personal,10.00,25.00,60,SI,Bebidas,SI,NO,0\r\n")
        sb.append(",Manzana Roja Unidad,Manzana roja fresca importada por unidad,15.00,35.00,40,SI,Frutas,SI,NO,0\r\n")

        return sb.toString()
    }

    /**
     * Escribe la plantilla en la caché interna de la app y retorna su URI seguro mediante FileProvider.
     */
    fun obtenerUriPlantillaEnCache(contexto: Context): Uri {
        val archivoCache = File(contexto.cacheDir, NOMBRE_ARCHIVO_PLANTILLA)
        FileOutputStream(archivoCache).use { fos ->
            // Escribir marca BOM UTF-8 (\uFEFF) para que Excel reconozca tildes y caracteres en español
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            fos.write(generarContenidoCsvPlantilla().toByteArray(StandardCharsets.UTF_8))
        }

        return FileProvider.getUriForFile(
            contexto,
            "${contexto.packageName}.fileprovider",
            archivoCache
        )
    }

    /**
     * Abre el selector del sistema (Share Sheet) para enviar el CSV por WhatsApp, Correo, Drive, etc.
     */
    fun compartirPlantilla(contexto: Context) {
        try {
            val uri = obtenerUriPlantillaEnCache(contexto)

            val intentCompartir = Intent(Intent.ACTION_SEND).apply {
                type = "text/*"
                clipData = ClipData.newRawUri(NOMBRE_ARCHIVO_PLANTILLA, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Plantilla Estándar de Productos - TyPOS Móvil")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Adjunto la plantilla estándar en formato CSV para importar productos al catálogo de TyPOS Móvil."
                )
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intentCompartir, "Enviar plantilla por...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            contexto.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(
                contexto,
                "Error al compartir la plantilla: ${e.localizedMessage ?: "Error desconocido"}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Guarda la plantilla en el dispositivo y devuelve la URI para poder abrirla de inmediato.
     */
    fun guardarPlantillaEnDescargas(contexto: Context): Pair<Boolean, Uri?> {
        return try {
            val bytesContenido = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
                    generarContenidoCsvPlantilla().toByteArray(StandardCharsets.UTF_8)

            var uriGenerado: Uri? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ usando MediaStore Downloads
                val valores = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, NOMBRE_ARCHIVO_PLANTILLA)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val resolver = contexto.contentResolver
                val uriDescarga = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores)

                if (uriDescarga != null) {
                    resolver.openOutputStream(uriDescarga)?.use { os ->
                        os.write(bytesContenido)
                    }
                    uriGenerado = uriDescarga
                }
            } else {
                // Android 9 o inferior
                val carpetaDescargas = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!carpetaDescargas.exists()) {
                    carpetaDescargas.mkdirs()
                }
                val archivoDestino = File(carpetaDescargas, NOMBRE_ARCHIVO_PLANTILLA)
                FileOutputStream(archivoDestino).use { fos ->
                    fos.write(bytesContenido)
                }
                uriGenerado = FileProvider.getUriForFile(
                    contexto,
                    "${contexto.packageName}.fileprovider",
                    archivoDestino
                )
            }

            // Fallback a URI de caché si no se obtuvo URI de MediaStore
            if (uriGenerado == null) {
                uriGenerado = obtenerUriPlantillaEnCache(contexto)
            }

            Pair(true, uriGenerado)
        } catch (e: Exception) {
            // En caso de fallo de permisos de almacenamiento, usar caché interna
            val uriCache = obtenerUriPlantillaEnCache(contexto)
            Pair(true, uriCache)
        }
    }

    /**
     * Abre el archivo CSV con la aplicación predeterminada (Microsoft Excel, Google Sheets, WPS Office, etc.).
     */
    fun abrirArchivoPlantilla(contexto: Context, uri: Uri? = null) {
        try {
            val uriFinal = uri ?: obtenerUriPlantillaEnCache(contexto)

            val intentAbrir = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uriFinal, "text/*")
                clipData = ClipData.newRawUri(NOMBRE_ARCHIVO_PLANTILLA, uriFinal)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            contexto.startActivity(Intent.createChooser(intentAbrir, "Abrir plantilla con...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Toast.makeText(
                contexto,
                "No se encontró una aplicación compatible (como Excel o Sheets) para abrir el archivo.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
