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
    private val COMANDO_CODEPAGE_CP850 = CodificadorEscPos.COMANDO_SELECCIONAR_CP850
    private val COMANDO_ALINEAR_IZQ = byteArrayOf(0x1B, 0x61, 0x00)
    private val COMANDO_ALINEAR_CENTRO = byteArrayOf(0x1B, 0x61, 0x01)
    private val COMANDO_ALINEAR_DER = byteArrayOf(0x1B, 0x61, 0x02)
    private val COMANDO_NEGRITA_ON = byteArrayOf(0x1B, 0x45, 0x01)
    private val COMANDO_NEGRITA_OFF = byteArrayOf(0x1B, 0x45, 0x00)
    private val COMANDO_TAMANO_NORMAL = byteArrayOf(0x1D, 0x21, 0x00)
    private val COMANDO_TAMANO_DOBLE_ALTO = byteArrayOf(0x1D, 0x21, 0x01) // 1x ancho, 2x alto (Recomendado)
    private val COMANDO_TAMANO_DOBLE_ANCHO = byteArrayOf(0x1D, 0x21, 0x10) // 2x ancho, 1x alto
    private val COMANDO_TAMANO_DOBLE = byteArrayOf(0x1D, 0x21, 0x11) // 2x ancho, 2x alto
    private val COMANDO_FUENTE_B = byteArrayOf(0x1B, 0x4D, 0x01) // Fuente B (más pequeña para branding discreto)
    private val COMANDO_FUENTE_A = byteArrayOf(0x1B, 0x4D, 0x00) // Fuente A (estándar)

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

    private fun ByteArrayOutputStream.escribirTexto(texto: String) {
        this.write(CodificadorEscPos.aBytes(texto))
    }

    private fun generarBytesTicketPrueba(
        configuracion: ConfiguracionNegocioEntidad
    ): ByteArray {
        val ancho = calcularAnchoColumnas(configuracion)
        val lineaDivisoria = "-".repeat(ancho) + "\n"
        val buffer = ByteArrayOutputStream()

        buffer.write(COMANDO_INICIALIZAR)
        buffer.write(COMANDO_CODEPAGE_CP850)
        buffer.write(COMANDO_ALINEAR_CENTRO)

        // Nombre según tamaño configurado
        when (configuracion.tamanoNombreNegocio) {
            0 -> {
                buffer.write(COMANDO_TAMANO_NORMAL)
                buffer.write(COMANDO_NEGRITA_ON)
            }
            1 -> {
                buffer.write(COMANDO_TAMANO_DOBLE_ALTO)
                buffer.write(COMANDO_NEGRITA_ON)
            }
            2 -> {
                buffer.write(COMANDO_TAMANO_DOBLE)
                buffer.write(COMANDO_NEGRITA_ON)
            }
            else -> {
                buffer.write(COMANDO_TAMANO_DOBLE_ALTO)
                buffer.write(COMANDO_NEGRITA_ON)
            }
        }
        buffer.escribirTexto("${configuracion.nombreNegocio}\n")
        buffer.write(COMANDO_TAMANO_NORMAL)
        buffer.write(COMANDO_NEGRITA_OFF)

        if (configuracion.mostrarSlogan && !configuracion.slogan.isNullOrBlank()) {
            buffer.escribirTexto("${configuracion.slogan}\n")
        }

        buffer.escribirTexto("IMPRESIÓN DE PRUEBA\n")
        buffer.escribirTexto(lineaDivisoria)

        buffer.write(COMANDO_ALINEAR_IZQ)
        buffer.escribirTexto("Estado: Conectada con éxito\n")
        buffer.escribirTexto("Papel: ${if (configuracion.tamanoPapelImpresora == 0) "Personalizado" else "${configuracion.tamanoPapelImpresora}mm"} ($ancho cols)\n")
        buffer.escribirTexto("Impresora: ${configuracion.nombreImpresora ?: "Bluetooth"}\n")
        buffer.escribirTexto("MAC: ${configuracion.direccionMacImpresora ?: "N/A"}\n")

        buffer.write(COMANDO_ALINEAR_CENTRO)
        buffer.escribirTexto(lineaDivisoria)
        
        buffer.write(COMANDO_FUENTE_B)
        buffer.escribirTexto("Powered by TyPOS Móvil\n")
        buffer.write(COMANDO_FUENTE_A)
        buffer.escribirTexto("\n\n\n\n")

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
        buffer.write(COMANDO_CODEPAGE_CP850)

        // ==========================================
        // 1. ENCABEZADO DEL NEGOCIO (HEADER)
        // ==========================================
        buffer.write(COMANDO_ALINEAR_CENTRO)

        // Tamaño del Nombre del Negocio
        when (configuracion.tamanoNombreNegocio) {
            0 -> {
                buffer.write(COMANDO_TAMANO_NORMAL)
                buffer.write(COMANDO_NEGRITA_ON)
            }
            1 -> { // Mediano / Doble Alto (Recomendado - 32 columnas de texto sin desbordar)
                buffer.write(COMANDO_TAMANO_DOBLE_ALTO)
                buffer.write(COMANDO_NEGRITA_ON)
            }
            2 -> { // Grande / Doble Tamaño
                buffer.write(COMANDO_TAMANO_DOBLE)
                buffer.write(COMANDO_NEGRITA_ON)
            }
            else -> {
                buffer.write(COMANDO_TAMANO_DOBLE_ALTO)
                buffer.write(COMANDO_NEGRITA_ON)
            }
        }
        buffer.escribirTexto("${configuracion.nombreNegocio}\n")
        buffer.write(COMANDO_TAMANO_NORMAL)
        buffer.write(COMANDO_NEGRITA_OFF)

        // Slogan (opcional)
        if (configuracion.mostrarSlogan && !configuracion.slogan.isNullOrBlank()) {
            buffer.escribirTexto("${configuracion.slogan}\n")
        }

        // RNC / Cédula (si está en el Encabezado)
        if (configuracion.posicionRnc == 0 && !configuracion.rncCedula.isNullOrBlank()) {
            buffer.escribirTexto("RNC / Cédula: ${configuracion.rncCedula}\n")
        }

        // Dirección (si está en el Encabezado)
        if (configuracion.posicionDireccion == 0 && !configuracion.direccion.isNullOrBlank()) {
            buffer.escribirTexto("${configuracion.direccion}\n")
        }

        // Teléfono (si está en el Encabezado)
        if (configuracion.posicionTelefono == 0 && !configuracion.telefono.isNullOrBlank()) {
            buffer.escribirTexto("Tel: ${configuracion.telefono}\n")
        }

        buffer.escribirTexto(lineaDivisoria)

        // ==========================================
        // 2. DATOS DE LA FACTURA
        // ==========================================
        buffer.write(COMANDO_ALINEAR_IZQ)
        buffer.escribirTexto(formatearLineaDosColumnas("Factura: ${venta.numeroFactura}", "", ancho))
        buffer.escribirTexto("Fecha: $fechaTexto\n")

        if (configuracion.mostrarCajero && venta.usuario.isNotBlank()) {
            buffer.escribirTexto("Cajero: ${venta.usuario}\n")
        }

        if (configuracion.mostrarCliente && !venta.nombreCliente.isNullOrBlank()) {
            buffer.escribirTexto("Cliente: ${venta.nombreCliente}\n")
        }
        if (configuracion.mostrarCliente && !venta.rncCedulaCliente.isNullOrBlank()) {
            buffer.escribirTexto("RNC/Céd: ${venta.rncCedulaCliente}\n")
        }

        buffer.escribirTexto(lineaDivisoria)

        // ==========================================
        // 3. TABLA DE PRODUCTOS (CENTRAL)
        // ==========================================
        if (ancho <= 36) {
            buffer.escribirTexto(formatearLineaTresColumnas("Cant", "Descripción", "Total", ancho))
        } else {
            buffer.escribirTexto(formatearLineaCuatroColumnas("Cant", "Descripción", "Precio", "Total", ancho))
        }
        buffer.escribirTexto(lineaDivisoria)

        // Lista de Artículos
        for (item in detalles) {
            val cantTexto = if (item.cantidad % 1.0 == 0.0) item.cantidad.toInt().toString() else String.format(Locale.US, "%.2f", item.cantidad)
            val totalTexto = String.format(Locale.US, "%.2f", item.total)

            if (ancho <= 36) {
                buffer.escribirTexto(formatearLineaTresColumnas(cantTexto, item.nombre, totalTexto, ancho))
            } else {
                val precioTexto = String.format(Locale.US, "%.2f", item.precio)
                buffer.escribirTexto(formatearLineaCuatroColumnas(cantTexto, item.nombre, precioTexto, totalTexto, ancho))
            }
        }

        buffer.escribirTexto(lineaDivisoria)

        // ==========================================
        // 4. TOTALES Y DESGLOSE CONTABLE
        // ==========================================
        buffer.write(COMANDO_ALINEAR_DER)
        buffer.escribirTexto(formatearLineaDosColumnas("Subtotal:", "$${String.format(Locale.US, "%.2f", venta.subTotalNeto)}", ancho))

        if (venta.montoItbis > 0) {
            buffer.escribirTexto(formatearLineaDosColumnas("ITBIS (18%):", "$${String.format(Locale.US, "%.2f", venta.montoItbis)}", ancho))
        }

        if (venta.descuento > 0) {
            buffer.escribirTexto(formatearLineaDosColumnas("Descuento:", "-$${String.format(Locale.US, "%.2f", venta.descuento)}", ancho))
        }

        buffer.write(COMANDO_NEGRITA_ON)
        buffer.escribirTexto(formatearLineaDosColumnas("TOTAL A PAGAR:", "$${String.format(Locale.US, "%.2f", venta.total)}", ancho))
        buffer.write(COMANDO_NEGRITA_OFF)

        buffer.escribirTexto(lineaDivisoria)

        // ==========================================
        // 5. MÉTODO DE PAGO Y CAMBIO
        // ==========================================
        buffer.write(COMANDO_ALINEAR_IZQ)
        buffer.escribirTexto("Método de Pago: ${venta.metodoPago}\n")

        if (venta.metodoPago == "Efectivo" && venta.montoRecibido > 0) {
            buffer.escribirTexto(formatearLineaDosColumnas("Efectivo Recibido:", "$${String.format(Locale.US, "%.2f", venta.montoRecibido)}", ancho))
            buffer.escribirTexto(formatearLineaDosColumnas("Cambio / Devuelta:", "$${String.format(Locale.US, "%.2f", venta.montoDevuelto)}", ancho))
        }

        if (venta.metodoPago == "Transferencia" && !venta.referenciaTransferencia.isNullOrBlank()) {
            buffer.escribirTexto("Ref: ${venta.referenciaTransferencia}\n")
        }

        if (venta.metodoPago == "Cheque" && !venta.numeroCheque.isNullOrBlank()) {
            buffer.escribirTexto("Cheque No: ${venta.numeroCheque} (${venta.bancoCheque ?: ""})\n")
        }

        // ==========================================
        // 6. PIE DE TICKET (FOOTER)
        // ==========================================
        buffer.write(COMANDO_ALINEAR_CENTRO)

        // Dirección en Footer si fue configurada allí
        if (configuracion.posicionDireccion == 1 && !configuracion.direccion.isNullOrBlank()) {
            buffer.escribirTexto("\n${configuracion.direccion}\n")
        }

        // Teléfono en Footer si fue configurado allí
        if (configuracion.posicionTelefono == 1 && !configuracion.telefono.isNullOrBlank()) {
            buffer.escribirTexto("Tel: ${configuracion.telefono}\n")
        }

        // Mensaje de despedida
        if (configuracion.pieTicket.isNotBlank()) {
            buffer.escribirTexto("\n${configuracion.pieTicket}\n")
        }

        // Powered by TyPOS Móvil (Firma fija permanente)
        buffer.write(COMANDO_FUENTE_B)
        buffer.escribirTexto("Powered by TyPOS Móvil\n")
        buffer.write(COMANDO_FUENTE_A)

        // Avance de papel para corte
        buffer.escribirTexto("\n\n\n\n")

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
