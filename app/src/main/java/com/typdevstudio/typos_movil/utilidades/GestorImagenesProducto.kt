package com.typdevstudio.typos_movil.utilidades

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.AzulPrimarioClaro
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object GestorImagenesProducto {

    /**
     * Crea un archivo temporal en la carpeta interna para tomar una foto directamente con la cámara.
     */
    fun crearArchivoTemporalParaCamara(contexto: Context): Pair<File, Uri>? {
        return try {
            val carpetaProductos = File(contexto.filesDir, "fotos_productos").apply {
                if (!exists()) mkdirs()
            }
            val nombreArchivo = "prod_cam_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
            val archivo = File(carpetaProductos, nombreArchivo)
            val uri = FileProvider.getUriForFile(
                contexto,
                "${contexto.packageName}.fileprovider",
                archivo
            )
            Pair(archivo, uri)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Guarda una imagen seleccionada desde la galería en el almacenamiento interno privado.
     */
    fun guardarImagenProductoInterna(contexto: Context, uriOrigen: Uri): String? {
        return try {
            val carpetaProductos = File(contexto.filesDir, "fotos_productos").apply {
                if (!exists()) mkdirs()
            }
            val nombreArchivo = "prod_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
            val archivoDestino = File(carpetaProductos, nombreArchivo)

            contexto.contentResolver.openInputStream(uriOrigen)?.use { input ->
                FileOutputStream(archivoDestino).use { output ->
                    input.copyTo(output)
                }
            }
            archivoDestino.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Elimina un archivo de imagen si se encuentra en el directorio interno de fotos de productos.
     */
    fun eliminarImagen(ruta: String) {
        try {
            val archivo = File(ruta)
            if (archivo.exists() && archivo.absolutePath.contains("fotos_productos")) {
                archivo.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Decodifica un Bitmap de manera eficiente calculando el inSampleSize adecuado
     * para evitar consumir exceso de memoria RAM con fotos de alta resolución.
     */
    fun decodificarMuestreado(ruta: String, anchoDeseado: Int = 300, altoDeseado: Int = 300): Bitmap? {
        return try {
            val archivo = File(ruta)
            if (!archivo.exists()) return null

            val opciones = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(archivo.absolutePath, opciones)

            var factorMuestreo = 1
            if (opciones.outHeight > altoDeseado || opciones.outWidth > anchoDeseado) {
                val mitadAlto = opciones.outHeight / 2
                val mitadAncho = opciones.outWidth / 2
                while ((mitadAlto / factorMuestreo) >= altoDeseado && (mitadAncho / factorMuestreo) >= anchoDeseado) {
                    factorMuestreo *= 2
                }
            }

            val opcionesDecodificacion = BitmapFactory.Options().apply {
                inSampleSize = factorMuestreo
                inPreferredConfig = Bitmap.Config.RGB_565 // Optimizado para menor consumo de memoria
            }

            BitmapFactory.decodeFile(archivo.absolutePath, opcionesDecodificacion)
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Componente visual reutilizable para mostrar la foto principal o la inicial de un producto.
 */
@Composable
fun MiniaturaProducto(
    fotoUri: String?,
    nombre: String,
    modifier: Modifier = Modifier,
    tamano: Dp = 44.dp,
    forma: Shape = RoundedCornerShape(10.dp),
    colorFondoFallback: Color = AzulPrimarioClaro.copy(alpha = 0.35f),
    colorTextoFallback: Color = AzulPrimario
) {
    val bitmap = remember(fotoUri) {
        if (!fotoUri.isNullOrBlank()) {
            GestorImagenesProducto.decodificarMuestreado(fotoUri, 160, 160)?.asImageBitmap()
        } else null
    }

    Box(
        modifier = modifier
            .size(tamano)
            .clip(forma)
            .background(colorFondoFallback),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = nombre,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val inicial = nombre.trim().take(1).uppercase().ifEmpty { "P" }
            Text(
                text = inicial,
                fontSize = (tamano.value * 0.42f).sp,
                fontWeight = FontWeight.Bold,
                color = colorTextoFallback
            )
        }
    }
}
