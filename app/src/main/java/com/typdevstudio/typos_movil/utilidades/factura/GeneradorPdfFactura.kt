package com.typdevstudio.typos_movil.utilidades.factura

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.typdevstudio.typos_movil.datos.local.entidades.ConfiguracionNegocioEntidad
import com.typdevstudio.typos_movil.datos.local.entidades.DetalleVentaEntidad
import com.typdevstudio.typos_movil.datos.local.entidades.VentaEntidad
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GeneradorPdfFactura {

    // Tamaño de página estándar A4 (595 x 842 pt)
    private const val ANCHO_PAGINA_A4 = 595
    private const val ALTO_MINIMO_PAGINA_A4 = 842

    // Dimensiones fijas del ticket térmico estándar (80mm ≈ 280 pt) para que NUNCA se estire en pantallas grandes
    private const val ANCHO_TICKET = 280f
    private const val MARGEN_CONTENIDO_TICKET = 14f

    fun generarPdfFactura(
        contexto: Context,
        venta: VentaEntidad,
        detalles: List<DetalleVentaEntidad>,
        configuracion: ConfiguracionNegocioEntidad
    ): File {
        val formatoFecha = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val fechaFormateada = formatoFecha.format(Date(venta.fecha))

        val ticketInicioX = (ANCHO_PAGINA_A4 - ANCHO_TICKET) / 2f
        val ticketFinX = ticketInicioX + ANCHO_TICKET
        val centroTicketX = ticketInicioX + (ANCHO_TICKET / 2f)
        val contenidoInicioX = ticketInicioX + MARGEN_CONTENIDO_TICKET
        val contenidoFinX = ticketFinX - MARGEN_CONTENIDO_TICKET

        val margenSuperiorTicket = 36f

        // 1. Estimar altura requerida por el contenido del ticket
        var alturaEstimadaTicket = 60f // Márgenes superior e inferior dentro del ticket

        // Altura Encabezado
        val tamanoFuenteNegocio = when (configuracion.tamanoNombreNegocio) {
            0 -> 13f // Normal
            1 -> 15.5f // Mediano (Recomendado)
            2 -> 18f // Grande
            else -> 15.5f
        }
        alturaEstimadaTicket += tamanoFuenteNegocio + 8f
        if (configuracion.mostrarSlogan && !configuracion.slogan.isNullOrBlank()) alturaEstimadaTicket += 16f
        if (!configuracion.rncCedula.isNullOrBlank() && configuracion.posicionRnc == 0) alturaEstimadaTicket += 14f
        if (!configuracion.direccion.isNullOrBlank() && configuracion.posicionDireccion == 0) alturaEstimadaTicket += 14f
        if (!configuracion.telefono.isNullOrBlank() && configuracion.posicionTelefono == 0) alturaEstimadaTicket += 14f
        alturaEstimadaTicket += 20f // Línea divisoria

        // Altura Datos Factura
        alturaEstimadaTicket += 18f // Factura No. + Fecha
        if (configuracion.mostrarCajero && venta.usuario.isNotBlank()) alturaEstimadaTicket += 16f
        if (configuracion.mostrarCliente && !venta.nombreCliente.isNullOrBlank()) alturaEstimadaTicket += 16f
        if (configuracion.mostrarCliente && !venta.rncCedulaCliente.isNullOrBlank()) alturaEstimadaTicket += 14f
        alturaEstimadaTicket += 20f // Línea divisoria

        // Altura Artículos
        alturaEstimadaTicket += 20f // Encabezado columnas
        detalles.forEach { detalle ->
            val lineasNombre = if (detalle.nombre.length > 28) 2 else 1
            alturaEstimadaTicket += (lineasNombre * 14f) + 14f + 10f // Nombre + Cant x Precio + Separador
        }
        alturaEstimadaTicket += 14f // Espaciado tabla

        // Altura Totales
        alturaEstimadaTicket += 16f // Subtotal
        if (venta.montoItbis > 0.0) alturaEstimadaTicket += 16f
        if (venta.descuento > 0.0) alturaEstimadaTicket += 16f
        alturaEstimadaTicket += 36f // Caja Destacada TOTAL
        alturaEstimadaTicket += 20f // Línea divisoria

        // Altura Método de Pago
        alturaEstimadaTicket += 16f // Método de Pago
        if (venta.metodoPago.equals("Efectivo", ignoreCase = true)) {
            if (venta.montoRecibido > 0.0) alturaEstimadaTicket += 14f
            if (venta.montoDevuelto > 0.0) alturaEstimadaTicket += 16f
        } else if (venta.metodoPago.equals("Transferencia", ignoreCase = true) && !venta.referenciaTransferencia.isNullOrBlank()) {
            alturaEstimadaTicket += 14f
        } else if (venta.metodoPago.equals("Cheque", ignoreCase = true) && !venta.numeroCheque.isNullOrBlank()) {
            alturaEstimadaTicket += 14f
        } else if (venta.metodoPago.equals("Mixto", ignoreCase = true)) {
            if (venta.montoEfectivo > 0.0) alturaEstimadaTicket += 14f
            if (venta.montoTransferencia > 0.0) alturaEstimadaTicket += 14f
            if (venta.montoCheque > 0.0) alturaEstimadaTicket += 14f
            if (venta.montoDevuelto > 0.0) alturaEstimadaTicket += 16f
        }
        alturaEstimadaTicket += 20f // Línea divisoria

        // Altura Pie de Ticket
        if (configuracion.pieTicket.isNotBlank()) alturaEstimadaTicket += 18f
        if (!configuracion.direccion.isNullOrBlank() && configuracion.posicionDireccion == 1) alturaEstimadaTicket += 14f
        if (!configuracion.telefono.isNullOrBlank() && configuracion.posicionTelefono == 1) alturaEstimadaTicket += 14f
        if (configuracion.mostrarPoweredBy) alturaEstimadaTicket += 18f
        alturaEstimadaTicket += 20f

        val alturaTotalTicket = alturaEstimadaTicket
        // Altura de la página: Estándar A4 (842 pt) o expandible hacia abajo si el ticket es extremadamente largo
        val alturaPagina = maxOf(ALTO_MINIMO_PAGINA_A4, (margenSuperiorTicket + alturaTotalTicket + 40f).toInt())

        // Iniciar PDF
        val documentoPdf = PdfDocument()
        val infoPagina = PdfDocument.PageInfo.Builder(ANCHO_PAGINA_A4, alturaPagina, 1).create()
        val pagina = documentoPdf.startPage(infoPagina)
        val canvas: Canvas = pagina.canvas

        // Pinturas de diseño
        val paintFondoPagina = Paint().apply { color = Color.rgb(248, 250, 252) } // Slate 50
        val paintFondoTicket = Paint().apply { color = Color.WHITE }
        val paintBordeTicket = Paint().apply {
            color = Color.rgb(226, 232, 240) // Slate 200
            strokeWidth = 1.2f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val paintTextoNegrita = Paint().apply {
            color = Color.rgb(15, 23, 42) // Slate 900
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintTextoNormal = Paint().apply {
            color = Color.rgb(51, 65, 85) // Slate 700
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val paintTextoSecundario = Paint().apply {
            color = Color.rgb(100, 116, 139) // Slate 500
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val paintLineaDivisoria = Paint().apply {
            color = Color.rgb(203, 213, 225) // Slate 300
            strokeWidth = 1.0f
            style = Paint.Style.STROKE
            isAntiAlias = true
            pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
        }
        val paintLineaPunteadaFina = Paint().apply {
            color = Color.rgb(226, 232, 240) // Slate 200
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
            isAntiAlias = true
            pathEffect = DashPathEffect(floatArrayOf(2f, 2f), 0f)
        }

        // 1. Dibujar fondo de página
        canvas.drawRect(0f, 0f, ANCHO_PAGINA_A4.toFloat(), alturaPagina.toFloat(), paintFondoPagina)

        // 2. Dibujar tarjeta central del ticket (tamaño estándar siempre centrado, jamás se estira)
        val rectTicket = RectF(
            ticketInicioX,
            margenSuperiorTicket,
            ticketFinX,
            margenSuperiorTicket + alturaTotalTicket
        )
        canvas.drawRoundRect(rectTicket, 8f, 8f, paintFondoTicket)
        canvas.drawRoundRect(rectTicket, 8f, 8f, paintBordeTicket)

        var y = margenSuperiorTicket + 26f

        // ========================================================
        // 1. ENCABEZADO DEL NEGOCIO
        // ========================================================
        paintTextoNegrita.apply {
            textSize = tamanoFuenteNegocio
            textAlign = Paint.Align.CENTER
        }
        val nombreNegocio = configuracion.nombreNegocio.ifBlank { "MI TIENDA" }
        canvas.drawText(nombreNegocio, centroTicketX, y, paintTextoNegrita)
        y += tamanoFuenteNegocio + 2f

        if (configuracion.mostrarSlogan && !configuracion.slogan.isNullOrBlank()) {
            paintTextoSecundario.apply {
                textSize = 9.5f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            }
            canvas.drawText(configuracion.slogan!!, centroTicketX, y, paintTextoSecundario)
            paintTextoSecundario.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            y += 14f
        }

        paintTextoNormal.apply {
            textSize = 9.5f
            textAlign = Paint.Align.CENTER
        }

        if (!configuracion.rncCedula.isNullOrBlank() && configuracion.posicionRnc == 0) {
            canvas.drawText("RNC / Cédula: ${configuracion.rncCedula}", centroTicketX, y, paintTextoNormal)
            y += 13f
        }

        if (!configuracion.direccion.isNullOrBlank() && configuracion.posicionDireccion == 0) {
            canvas.drawText(configuracion.direccion!!, centroTicketX, y, paintTextoNormal)
            y += 13f
        }

        if (!configuracion.telefono.isNullOrBlank() && configuracion.posicionTelefono == 0) {
            canvas.drawText("Tel: ${configuracion.telefono}", centroTicketX, y, paintTextoNormal)
            y += 13f
        }

        y += 6f
        canvas.drawLine(contenidoInicioX, y, contenidoFinX, y, paintLineaDivisoria)
        y += 16f

        // ========================================================
        // 2. DATOS DE LA FACTURA
        // ========================================================
        paintTextoNegrita.apply {
            textSize = 10.5f
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("Factura: ${venta.numeroFactura}", contenidoInicioX, y, paintTextoNegrita)

        paintTextoSecundario.apply {
            textSize = 9.5f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(fechaFormateada, contenidoFinX, y, paintTextoSecundario)
        y += 15f

        if (configuracion.mostrarCajero && venta.usuario.isNotBlank()) {
            paintTextoNormal.apply {
                textSize = 9.5f
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("Cajero: ${venta.usuario}", contenidoInicioX, y, paintTextoNormal)
            y += 14f
        }

        if (configuracion.mostrarCliente && !venta.nombreCliente.isNullOrBlank()) {
            paintTextoNegrita.apply {
                textSize = 10f
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("Cliente: ${venta.nombreCliente}", contenidoInicioX, y, paintTextoNegrita)
            y += 14f

            if (!venta.rncCedulaCliente.isNullOrBlank()) {
                paintTextoNormal.apply {
                    textSize = 9f
                    textAlign = Paint.Align.LEFT
                }
                canvas.drawText("RNC/Céd: ${venta.rncCedulaCliente}", contenidoInicioX, y, paintTextoNormal)
                y += 14f
            }
        }

        y += 4f
        canvas.drawLine(contenidoInicioX, y, contenidoFinX, y, paintLineaDivisoria)
        y += 14f

        // ========================================================
        // 3. TABLA DE PRODUCTOS
        // ========================================================
        paintTextoNegrita.apply {
            textSize = 9.5f
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("DESCRIPCIÓN", contenidoInicioX, y, paintTextoNegrita)

        paintTextoNegrita.apply {
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("TOTAL", contenidoFinX, y, paintTextoNegrita)
        y += 8f
        canvas.drawLine(contenidoInicioX, y, contenidoFinX, y, paintLineaDivisoria)
        y += 14f

        // Lista de artículos
        detalles.forEach { detalle ->
            paintTextoNegrita.apply {
                textSize = 10f
                textAlign = Paint.Align.LEFT
            }

            // Manejo de nombres largos
            val nombreItem = detalle.nombre
            if (nombreItem.length > 28) {
                val linea1 = nombreItem.take(28)
                val linea2 = if (nombreItem.length > 56) nombreItem.substring(28, 54) + "..." else nombreItem.substring(28)
                canvas.drawText(linea1, contenidoInicioX, y, paintTextoNegrita)
                y += 13f
                canvas.drawText(linea2, contenidoInicioX, y, paintTextoNegrita)
            } else {
                canvas.drawText(nombreItem, contenidoInicioX, y, paintTextoNegrita)
            }

            // Precio total del ítem alineado a la derecha
            paintTextoNegrita.apply {
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("$${String.format(Locale.US, "%.2f", detalle.total)}", contenidoFinX, y, paintTextoNegrita)
            y += 12f

            // Detalle: Cantidad x Precio unitario + Nota de impuesto
            paintTextoSecundario.apply {
                textSize = 9f
                textAlign = Paint.Align.LEFT
            }
            val cantStr = if (detalle.cantidad % 1.0 == 0.0) "${detalle.cantidad.toInt()}" else String.format(Locale.US, "%.2f", detalle.cantidad)
            val notaImpuesto = if (detalle.montoItbis > 0) " (ITBIS incl.)" else " (Exento)"
            val lineaDetalle = "$cantStr x $${String.format(Locale.US, "%.2f", detalle.precio)}$notaImpuesto"
            canvas.drawText(lineaDetalle, contenidoInicioX, y, paintTextoSecundario)
            y += 10f

            canvas.drawLine(contenidoInicioX, y, contenidoFinX, y, paintLineaPunteadaFina)
            y += 10f
        }

        y += 4f

        // ========================================================
        // 4. TOTALES Y DESGLOSE CONTABLE
        // ========================================================
        paintTextoNormal.apply {
            textSize = 10f
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("Subtotal:", contenidoInicioX, y, paintTextoNormal)

        paintTextoNormal.apply {
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("$${String.format(Locale.US, "%.2f", venta.subTotalNeto)}", contenidoFinX, y, paintTextoNormal)
        y += 14f

        if (venta.montoItbis > 0.0) {
            paintTextoNormal.apply {
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("ITBIS (18%):", contenidoInicioX, y, paintTextoNormal)

            paintTextoNormal.apply {
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("$${String.format(Locale.US, "%.2f", venta.montoItbis)}", contenidoFinX, y, paintTextoNormal)
            y += 14f
        }

        if (venta.descuento > 0.0) {
            val paintDescuento = Paint().apply {
                color = Color.rgb(220, 38, 38)
                textSize = 10f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("Descuento:", contenidoInicioX, y, paintDescuento)

            paintDescuento.textAlign = Paint.Align.RIGHT
            canvas.drawText("-$${String.format(Locale.US, "%.2f", venta.descuento)}", contenidoFinX, y, paintDescuento)
            y += 14f
        }

        y += 6f
        // BLOQUE DESTACADO DEL TOTAL A PAGAR
        val paintCajaTotal = Paint().apply {
            color = Color.rgb(2, 136, 209) // Azul Primario TyPOS
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(contenidoInicioX - 2f, y - 12f, contenidoFinX + 2f, y + 18f), 6f, 6f, paintCajaTotal)

        val paintTextoTotal = Paint().apply {
            color = Color.WHITE
            textSize = 11.5f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("TOTAL A PAGAR:", contenidoInicioX + 6f, y + 5f, paintTextoTotal)

        paintTextoTotal.apply {
            textSize = 13.5f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("$${String.format(Locale.US, "%.2f", venta.total)}", contenidoFinX - 6f, y + 5f, paintTextoTotal)
        y += 30f

        // ========================================================
        // 5. MÉTODO DE PAGO Y CAMBIO / DEVUELTA
        // ========================================================
        paintTextoNormal.apply {
            textSize = 9.5f
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("Método de Pago: ${venta.metodoPago}", contenidoInicioX, y, paintTextoNormal)
        y += 13f

        if (venta.metodoPago.equals("Efectivo", ignoreCase = true)) {
            if (venta.montoRecibido > 0.0) {
                paintTextoSecundario.apply {
                    textSize = 9.5f
                    textAlign = Paint.Align.LEFT
                }
                canvas.drawText("Efectivo Recibido:", contenidoInicioX, y, paintTextoSecundario)
                paintTextoSecundario.textAlign = Paint.Align.RIGHT
                canvas.drawText("$${String.format(Locale.US, "%.2f", venta.montoRecibido)}", contenidoFinX, y, paintTextoSecundario)
                y += 13f
            }

            if (venta.montoDevuelto > 0.0) {
                val paintDevuelta = Paint().apply {
                    color = Color.rgb(22, 163, 74) // Verde Éxito
                    textSize = 10f
                    isAntiAlias = true
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.LEFT
                }
                canvas.drawText("Cambio / Devuelta:", contenidoInicioX, y, paintDevuelta)
                paintDevuelta.textAlign = Paint.Align.RIGHT
                canvas.drawText("$${String.format(Locale.US, "%.2f", venta.montoDevuelto)}", contenidoFinX, y, paintDevuelta)
                y += 14f
            }
        } else if (venta.metodoPago.equals("Transferencia", ignoreCase = true) && !venta.referenciaTransferencia.isNullOrBlank()) {
            paintTextoSecundario.apply {
                textSize = 9.5f
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("Ref: ${venta.referenciaTransferencia}", contenidoInicioX, y, paintTextoSecundario)
            y += 13f
        } else if (venta.metodoPago.equals("Cheque", ignoreCase = true) && !venta.numeroCheque.isNullOrBlank()) {
            paintTextoSecundario.apply {
                textSize = 9.5f
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("Cheque No: ${venta.numeroCheque} (${venta.bancoCheque ?: ""})", contenidoInicioX, y, paintTextoSecundario)
            y += 13f
        } else if (venta.metodoPago.equals("Mixto", ignoreCase = true)) {
            paintTextoSecundario.apply {
                textSize = 9f
                textAlign = Paint.Align.LEFT
            }
            if (venta.montoEfectivo > 0.0) {
                canvas.drawText("  Efectivo:", contenidoInicioX, y, paintTextoSecundario)
                paintTextoSecundario.textAlign = Paint.Align.RIGHT
                canvas.drawText("$${String.format(Locale.US, "%.2f", venta.montoEfectivo)}", contenidoFinX, y, paintTextoSecundario)
                paintTextoSecundario.textAlign = Paint.Align.LEFT
                y += 12f
            }
            if (venta.montoTransferencia > 0.0) {
                canvas.drawText("  Transferencia:", contenidoInicioX, y, paintTextoSecundario)
                paintTextoSecundario.textAlign = Paint.Align.RIGHT
                canvas.drawText("$${String.format(Locale.US, "%.2f", venta.montoTransferencia)}", contenidoFinX, y, paintTextoSecundario)
                paintTextoSecundario.textAlign = Paint.Align.LEFT
                y += 12f
            }
            if (venta.montoCheque > 0.0) {
                canvas.drawText("  Cheque:", contenidoInicioX, y, paintTextoSecundario)
                paintTextoSecundario.textAlign = Paint.Align.RIGHT
                canvas.drawText("$${String.format(Locale.US, "%.2f", venta.montoCheque)}", contenidoFinX, y, paintTextoSecundario)
                paintTextoSecundario.textAlign = Paint.Align.LEFT
                y += 12f
            }
            if (venta.montoDevuelto > 0.0) {
                val paintDevuelta = Paint().apply {
                    color = Color.rgb(22, 163, 74)
                    textSize = 10f
                    isAntiAlias = true
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.LEFT
                }
                canvas.drawText("Cambio / Devuelta:", contenidoInicioX, y, paintDevuelta)
                paintDevuelta.textAlign = Paint.Align.RIGHT
                canvas.drawText("$${String.format(Locale.US, "%.2f", venta.montoDevuelto)}", contenidoFinX, y, paintDevuelta)
                y += 14f
            }
        }

        y += 4f
        canvas.drawLine(contenidoInicioX, y, contenidoFinX, y, paintLineaDivisoria)
        y += 16f

        // ========================================================
        // 6. PIE DE TICKET (FOOTER)
        // ========================================================
        if (configuracion.pieTicket.isNotBlank()) {
            paintTextoNegrita.apply {
                textSize = 10.5f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(configuracion.pieTicket, centroTicketX, y, paintTextoNegrita)
            y += 15f
        }

        if (!configuracion.direccion.isNullOrBlank() && configuracion.posicionDireccion == 1) {
            paintTextoSecundario.apply {
                textSize = 9f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(configuracion.direccion!!, centroTicketX, y, paintTextoSecundario)
            y += 13f
        }

        if (!configuracion.telefono.isNullOrBlank() && configuracion.posicionTelefono == 1) {
            paintTextoSecundario.apply {
                textSize = 9f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Tel: ${configuracion.telefono}", centroTicketX, y, paintTextoSecundario)
            y += 13f
        }

        if (configuracion.mostrarPoweredBy) {
            y += 3f
            paintTextoSecundario.apply {
                textSize = 8.5f
                textAlign = Paint.Align.CENTER
            }
            val textoPowered = configuracion.textoPoweredBy.ifBlank { "Powered by TyPOS Móvil" }
            canvas.drawText(textoPowered, centroTicketX, y, paintTextoSecundario)
        }

        documentoPdf.finishPage(pagina)

        // Guardar archivo en cache/facturas
        val directorioFacturas = File(contexto.cacheDir, "facturas").apply { mkdirs() }
        val nombreArchivo = "Factura_${venta.numeroFactura.replace("[^a-zA-Z0-9_-]".toRegex(), "_")}.pdf"
        val archivoFinal = File(directorioFacturas, nombreArchivo)

        FileOutputStream(archivoFinal).use { fos ->
            documentoPdf.writeTo(fos)
        }
        documentoPdf.close()

        return archivoFinal
    }

    fun compartirPdf(
        contexto: Context,
        archivoPdf: File,
        venta: VentaEntidad
    ) {
        try {
            val uri = FileProvider.getUriForFile(
                contexto,
                "${contexto.packageName}.fileprovider",
                archivoPdf
            )

            val mensajeWhatsApp = "Hola, le compartimos el comprobante de su factura ${venta.numeroFactura} por un total de $${String.format(Locale.US, "%.2f", venta.total)}. ¡Gracias por su compra!"

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Factura ${venta.numeroFactura}")
                putExtra(Intent.EXTRA_TEXT, mensajeWhatsApp)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "Compartir factura por WhatsApp o correo").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            contexto.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(contexto, "No se pudo compartir la factura: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun abrirPdf(
        contexto: Context,
        archivoPdf: File
    ) {
        try {
            val uri = FileProvider.getUriForFile(
                contexto,
                "${contexto.packageName}.fileprovider",
                archivoPdf
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "Abrir Factura PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            contexto.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(contexto, "Instale un lector de PDF para ver el archivo", Toast.LENGTH_LONG).show()
        }
    }
}
