package com.typdevstudio.typos_movil.datos.impresion

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.typdevstudio.typos_movil.datos.local.entidades.ConfiguracionNegocioEntidad
import com.typdevstudio.typos_movil.datos.local.entidades.DetalleVentaEntidad
import com.typdevstudio.typos_movil.datos.local.entidades.VentaEntidad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed interface ResultadoImpresion {
    data object Exito : ResultadoImpresion
    data class Error(val mensaje: String) : ResultadoImpresion
}

data class DispositivoBluetoothPos(
    val nombre: String,
    val direccionMac: String
)

object ServicioImpresoraBluetooth {

    // UUID estándar para Bluetooth SPP (Serial Port Profile)
    private val UUID_SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Comandos ESC/POS estándar
    private val COMANDO_INICIALIZAR = byteArrayOf(0x1B, 0x40)
    private val COMANDO_ALINEAR_IZQ = byteArrayOf(0x1B, 0x61, 0x00)
    private val COMANDO_ALINEAR_CENTRO = byteArrayOf(0x1B, 0x61, 0x01)
    private val COMANDO_ALINEAR_DER = byteArrayOf(0x1B, 0x61, 0x02)
    private val COMANDO_NEGRITA_ON = byteArrayOf(0x1B, 0x45, 0x01)
    private val COMANDO_NEGRITA_OFF = byteArrayOf(0x1B, 0x45, 0x00)
    private val COMANDO_TAMANO_DOBLE = byteArrayOf(0x1D, 0x21, 0x11)
    private val COMANDO_TAMANO_NORMAL = byteArrayOf(0x1D, 0x21, 0x00)

    fun calcularAnchoColumnas(configuracion: ConfiguracionNegocioEntidad): Int {
        return if (configuracion.columnasPersonalizadas in 16..80) {
            configuracion.columnasPersonalizadas
        } else {
            when (configuracion.tamanoPapelImpresora) {
                58 -> 32
                80 -> 48
                57 -> 30
                72 -> 42
                else -> 32
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun obtenerDispositivosVinculados(): List<DispositivoBluetoothPos> {
        val adaptadorBluetooth = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        if (!adaptadorBluetooth.isEnabled) return emptyList()

        return try {
            val dispositivos: Set<BluetoothDevice>? = adaptadorBluetooth.bondedDevices
            dispositivos?.map {
                DispositivoBluetoothPos(
                    nombre = it.name ?: "Dispositivo Desconocido",
                    direccionMac = it.address
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun imprimirTicketVenta(
        direccionMac: String,
        tamanoPapel: Int,
        configuracion: ConfiguracionNegocioEntidad,
        venta: VentaEntidad,
        detalles: List<DetalleVentaEntidad>
    ): ResultadoImpresion = withContext(Dispatchers.IO) {
        val adaptador = BluetoothAdapter.getDefaultAdapter()
            ?: return@withContext ResultadoImpresion.Error("El dispositivo no cuenta con Bluetooth")

        if (!adaptador.isEnabled) {
            return@withContext ResultadoImpresion.Error("El Bluetooth se encuentra apagado. Por favor enciéndelo.")
        }

        var socket: BluetoothSocket? = null
        try {
            val dispositivo = adaptador.getRemoteDevice(direccionMac)
            socket = dispositivo.createRfcommSocketToServiceRecord(UUID_SPP)
            socket.connect()

            val flujoSalida: OutputStream = socket.outputStream
            val bytesTicket = generarBytesTicketVenta(configuracion, venta, detalles)

            flujoSalida.write(bytesTicket)
            flujoSalida.flush()
            Thread.sleep(500)
            socket.close()

            ResultadoImpresion.Exito
        } catch (e: Exception) {
            try { socket?.close() } catch (_: Exception) {}
            ResultadoImpresion.Error("Error al conectar con la impresora: ${e.localizedMessage ?: "Verifica que esté encendida y vinculada"}")
        }
    }

    suspend fun imprimirTicketPrueba(
        direccionMac: String,
        tamanoPapel: Int,
        configuracion: ConfiguracionNegocioEntidad
    ): ResultadoImpresion = withContext(Dispatchers.IO) {
        val adaptador = BluetoothAdapter.getDefaultAdapter()
            ?: return@withContext ResultadoImpresion.Error("El dispositivo no tiene Bluetooth")

        if (!adaptador.isEnabled) {
            return@withContext ResultadoImpresion.Error("El Bluetooth está apagado")
        }

        var socket: BluetoothSocket? = null
        try {
            val dispositivo = adaptador.getRemoteDevice(direccionMac)
            socket = dispositivo.createRfcommSocketToServiceRecord(UUID_SPP)
            socket.connect()

            val flujoSalida: OutputStream = socket.outputStream
            val bytesPrueba = generarBytesTicketPrueba(configuracion)

            flujoSalida.write(bytesPrueba)
            flujoSalida.flush()
            Thread.sleep(500)
            socket.close()

            ResultadoImpresion.Exito
        } catch (e: Exception) {
            try { socket?.close() } catch (_: Exception) {}
            ResultadoImpresion.Error("No se pudo imprimir: ${e.localizedMessage ?: "Error de conexión"}")
        }
    }

    private fun generarBytesTicketPrueba(
        configuracion: ConfiguracionNegocioEntidad
    ): ByteArray {
        val ancho = calcularAnchoColumnas(configuracion)
        val lineaDivisoria = "-".repeat(ancho) + "\n"
        val buffer = ByteArrayOutputStream()

        buffer.write(COMANDO_INICIALIZAR)
        buffer.write(COMANDO_ALINEAR_CENTRO)
        buffer.write(COMANDO_TAMANO_DOBLE)
        buffer.write("${configuracion.nombreNegocio}\n".toByteArray(Charsets.ISO_8859_1))
        buffer.write(COMANDO_TAMANO_NORMAL)

        buffer.write("IMPRESION DE PRUEBA\n".toByteArray(Charsets.ISO_8859_1))
        buffer.write(lineaDivisoria.toByteArray(Charsets.ISO_8859_1))

        buffer.write(COMANDO_ALINEAR_IZQ)
        buffer.write("Estado: Conectada con exito\n".toByteArray(Charsets.ISO_8859_1))
        buffer.write("Papel: ${if (configuracion.tamanoPapelImpresora == 0) "Personalizado" else "${configuracion.tamanoPapelImpresora}mm"} ($ancho cols)\n".toByteArray(Charsets.ISO_8859_1))
        buffer.write("Impresora: ${configuracion.nombreImpresora ?: "Bluetooth"}\n".toByteArray(Charsets.ISO_8859_1))
        buffer.write("MAC: ${configuracion.direccionMacImpresora ?: "N/A"}\n".toByteArray(Charsets.ISO_8859_1))

        buffer.write(COMANDO_ALINEAR_CENTRO)
        buffer.write(lineaDivisoria.toByteArray(Charsets.ISO_8859_1))
        buffer.write("TyPOS Movil - Listo para vender\n\n\n\n".toByteArray(Charsets.ISO_8859_1))

        return buffer.toByteArray()
    }

    fun generarBytesTicketVenta(
        configuracion: ConfiguracionNegocioEntidad,
        venta: VentaEntidad,
        detalles: List<DetalleVentaEntidad>
    ): ByteArray {
        val ancho = calcularAnchoColumnas(configuracion)
        val lineaDivisoria = "-".repeat(ancho) + "\n"
        val buffer = ByteArrayOutputStream()

        val formatoFecha = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val fechaTexto = formatoFecha.format(Date(venta.fecha))

        buffer.write(COMANDO_INICIALIZAR)

        // Encabezado del Negocio
        buffer.write(COMANDO_ALINEAR_CENTRO)
        buffer.write(COMANDO_TAMANO_DOBLE)
        buffer.write(COMANDO_NEGRITA_ON)
        buffer.write("${configuracion.nombreNegocio}\n".toByteArray(Charsets.ISO_8859_1))
        buffer.write(COMANDO_TAMANO_NORMAL)
        buffer.write(COMANDO_NEGRITA_OFF)

        if (!configuracion.rncCedula.isNullOrBlank()) {
            buffer.write("RNC / Cedula: ${configuracion.rncCedula}\n".toByteArray(Charsets.ISO_8859_1))
        }
        if (!configuracion.direccion.isNullOrBlank()) {
            buffer.write("${configuracion.direccion}\n".toByteArray(Charsets.ISO_8859_1))
        }
        if (!configuracion.telefono.isNullOrBlank()) {
            buffer.write("Tel: ${configuracion.telefono}\n".toByteArray(Charsets.ISO_8859_1))
        }

        buffer.write(lineaDivisoria.toByteArray(Charsets.ISO_8859_1))

        // Datos de la Factura
        buffer.write(COMANDO_ALINEAR_IZQ)
        buffer.write(formatearLineaDosColumnas("Factura: ${venta.numeroFactura}", "", ancho).toByteArray(Charsets.ISO_8859_1))
        buffer.write("Fecha: $fechaTexto\n".toByteArray(Charsets.ISO_8859_1))
        buffer.write("Cajero: ${venta.usuario}\n".toByteArray(Charsets.ISO_8859_1))

        if (!venta.nombreCliente.isNullOrBlank()) {
            buffer.write("Cliente: ${venta.nombreCliente}\n".toByteArray(Charsets.ISO_8859_1))
        }
        if (!venta.rncCedulaCliente.isNullOrBlank()) {
            buffer.write("RNC/Ced: ${venta.rncCedulaCliente}\n".toByteArray(Charsets.ISO_8859_1))
        }

        buffer.write(lineaDivisoria.toByteArray(Charsets.ISO_8859_1))

        // Encabezado de Productos
        if (ancho <= 36) {
            buffer.write(formatearLineaTresColumnas("Cant", "Descripcion", "Total", ancho).toByteArray(Charsets.ISO_8859_1))
        } else {
            buffer.write(formatearLineaCuatroColumnas("Cant", "Descripcion", "Precio", "Total", ancho).toByteArray(Charsets.ISO_8859_1))
        }
        buffer.write(lineaDivisoria.toByteArray(Charsets.ISO_8859_1))

        // Lista de Artículos
        for (item in detalles) {
            val cantTexto = if (item.cantidad % 1.0 == 0.0) item.cantidad.toInt().toString() else String.format("%.2f", item.cantidad)
            val totalTexto = String.format("%.2f", item.total)

            if (ancho <= 36) {
                buffer.write(formatearLineaTresColumnas(cantTexto, item.nombre, totalTexto, ancho).toByteArray(Charsets.ISO_8859_1))
            } else {
                val precioTexto = String.format("%.2f", item.precio)
                buffer.write(formatearLineaCuatroColumnas(cantTexto, item.nombre, precioTexto, totalTexto, ancho).toByteArray(Charsets.ISO_8859_1))
            }
        }

        buffer.write(lineaDivisoria.toByteArray(Charsets.ISO_8859_1))

        // Totales y Desglose Contable con ITBIS
        buffer.write(COMANDO_ALINEAR_DER)
        buffer.write(formatearLineaDosColumnas("Subtotal:", "$${String.format("%.2f", venta.subTotalNeto)}", ancho).toByteArray(Charsets.ISO_8859_1))

        if (venta.montoItbis > 0) {
            buffer.write(formatearLineaDosColumnas("ITBIS (18%):", "$${String.format("%.2f", venta.montoItbis)}", ancho).toByteArray(Charsets.ISO_8859_1))
        }

        if (venta.descuento > 0) {
            buffer.write(formatearLineaDosColumnas("Descuento:", "-$${String.format("%.2f", venta.descuento)}", ancho).toByteArray(Charsets.ISO_8859_1))
        }

        buffer.write(COMANDO_NEGRITA_ON)
        buffer.write(formatearLineaDosColumnas("TOTAL A PAGAR:", "$${String.format("%.2f", venta.total)}", ancho).toByteArray(Charsets.ISO_8859_1))
        buffer.write(COMANDO_NEGRITA_OFF)

        buffer.write(lineaDivisoria.toByteArray(Charsets.ISO_8859_1))

        // Método de Pago y Cambio
        buffer.write(COMANDO_ALINEAR_IZQ)
        buffer.write("Metodo de Pago: ${venta.metodoPago}\n".toByteArray(Charsets.ISO_8859_1))

        if (venta.metodoPago == "Efectivo" && venta.montoRecibido > 0) {
            buffer.write(formatearLineaDosColumnas("Efectivo Recibido:", "$${String.format("%.2f", venta.montoRecibido)}", ancho).toByteArray(Charsets.ISO_8859_1))
            buffer.write(formatearLineaDosColumnas("Cambio / Devuelta:", "$${String.format("%.2f", venta.montoDevuelto)}", ancho).toByteArray(Charsets.ISO_8859_1))
        }

        if (venta.metodoPago == "Transferencia" && !venta.referenciaTransferencia.isNullOrBlank()) {
            buffer.write("Ref: ${venta.referenciaTransferencia}\n".toByteArray(Charsets.ISO_8859_1))
        }

        if (venta.metodoPago == "Cheque" && !venta.numeroCheque.isNullOrBlank()) {
            buffer.write("Cheque No: ${venta.numeroCheque} (${venta.bancoCheque ?: ""})\n".toByteArray(Charsets.ISO_8859_1))
        }

        // Pie de Ticket
        buffer.write(COMANDO_ALINEAR_CENTRO)
        buffer.write("\n${configuracion.pieTicket}\n".toByteArray(Charsets.ISO_8859_1))
        buffer.write("Software: TyPOS Movil\n\n\n\n".toByteArray(Charsets.ISO_8859_1))

        return buffer.toByteArray()
    }

    fun formatearLineaDosColumnas(col1: String, col2: String, anchoTotal: Int): String {
        val espaciado = (anchoTotal - col1.length - col2.length).coerceAtLeast(1)
        return col1 + " ".repeat(espaciado) + col2 + "\n"
    }

    fun formatearLineaTresColumnas(col1: String, col2: String, col3: String, anchoTotal: Int): String {
        val anchoCant = 5
        val anchoTotalCol = 8
        val anchoDesc = (anchoTotal - anchoCant - anchoTotalCol).coerceAtLeast(8)

        val cant = col1.padEnd(anchoCant).take(anchoCant)
        val desc = col2.padEnd(anchoDesc).take(anchoDesc)
        val tot = col3.padStart(anchoTotalCol).take(anchoTotalCol)

        return "$cant$desc$tot\n"
    }

    fun formatearLineaCuatroColumnas(col1: String, col2: String, col3: String, col4: String, anchoTotal: Int): String {
        val anchoCant = 5
        val anchoPrecio = 7
        val anchoTotalCol = 8
        val anchoDesc = (anchoTotal - anchoCant - anchoPrecio - anchoTotalCol).coerceAtLeast(12)

        val cant = col1.padEnd(anchoCant).take(anchoCant)
        val desc = col2.padEnd(anchoDesc).take(anchoDesc)
        val prec = col3.padStart(anchoPrecio).take(anchoPrecio)
        val tot = col4.padStart(anchoTotalCol).take(anchoTotalCol)

        return "$cant$desc$prec$tot\n"
    }
}
