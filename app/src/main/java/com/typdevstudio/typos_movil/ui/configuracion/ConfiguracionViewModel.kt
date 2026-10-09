package com.typdevstudio.typos_movil.ui.configuracion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.typdevstudio.typos_movil.datos.impresion.DispositivoBluetoothPos
import com.typdevstudio.typos_movil.datos.impresion.ResultadoImpresion
import com.typdevstudio.typos_movil.datos.impresion.ServicioImpresoraBluetooth
import com.typdevstudio.typos_movil.datos.local.AppBaseDatos
import com.typdevstudio.typos_movil.datos.local.entidades.ConfiguracionNegocioEntidad
import com.typdevstudio.typos_movil.datos.repositorio.ConfiguracionRepositorio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

import com.typdevstudio.typos_movil.datos.repositorio.EstadoLicencia
import com.typdevstudio.typos_movil.datos.repositorio.LicenciaRepositorio

data class ConfiguracionUiState(
    val nombreNegocio: String = "Mi Tienda",
    val slogan: String = "",
    val tamanoNombreNegocio: Int = 1, // 0: Normal, 1: Mediano/Doble Alto (Recomendado), 2: Grande
    val mostrarSlogan: Boolean = true,
    val rncCedula: String = "",
    val posicionRnc: Int = 0, // 0: Encabezado, 1: Ocultar
    val direccion: String = "",
    val posicionDireccion: Int = 0, // 0: Encabezado, 1: Pie de Página, 2: Ocultar
    val telefono: String = "",
    val posicionTelefono: Int = 0, // 0: Encabezado, 1: Pie de Página, 2: Ocultar
    val mostrarCajero: Boolean = true,
    val mostrarCliente: Boolean = true,
    val pieTicket: String = "¡Gracias por su compra!",
    val mostrarPoweredBy: Boolean = true,
    val textoPoweredBy: String = "Powered by TyPOS Móvil",
    val nombreImpresora: String? = null,
    val direccionMacImpresora: String? = null,
    val tamanoPapel: Int = 58, // 58, 80, 57, 72 o 0 (Personalizado)
    val anchoMilimetrosPersonalizado: String = "58",
    val columnasPersonalizadas: Int = 32,
    val modoTema: Int = 0, // 0: Sistema, 1: Claro, 2: Oscuro
    val dispositivosDisponibles: List<DispositivoBluetoothPos> = emptyList(),
    val estadoLicencia: EstadoLicencia = EstadoLicencia.SinLicencia,
    val serialDispositivo: String = "",
    val claveLicenciaNueva: String = "",
    val estaActivandoLicencia: Boolean = false,
    val estaGuardando: Boolean = false,
    val estaImprimiendoPrueba: Boolean = false,
    val estaBuscandoActualizaciones: Boolean = false,
    val infoActualizacion: com.typdevstudio.typos_movil.utilidades.actualizador.InfoActualizacion? = null,
    val guardadoExitoso: Boolean = false,
    val mensajeAlerta: String? = null,
    val esErrorAlerta: Boolean = false
)

class ConfiguracionViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio: ConfiguracionRepositorio
    private val licenciaRepositorio: LicenciaRepositorio
    val serialDispositivo: String = com.typdevstudio.typos_movil.utilidades.licencia.IdDispositivo.obtenerSerialDispositivo(application)

    private val _uiState = MutableStateFlow(ConfiguracionUiState(serialDispositivo = serialDispositivo))
    val uiState: StateFlow<ConfiguracionUiState> = _uiState.asStateFlow()

    init {
        val bd = AppBaseDatos.obtenerBaseDatos(application)
        repositorio = ConfiguracionRepositorio(bd.configuracionNegocioDao())
        licenciaRepositorio = LicenciaRepositorio(bd.licenciaDao(), serialDispositivo)
        cargarConfiguracion()
    }

    fun cargarConfiguracion() {
        viewModelScope.launch {
            val config = repositorio.obtenerConfiguracionDirecta()
            val estadoLic = licenciaRepositorio.verificarEstadoLicencia()
            val mmTexto = if (config.tamanoPapelImpresora > 0) config.tamanoPapelImpresora.toString() else "58"
            _uiState.update {
                it.copy(
                    nombreNegocio = config.nombreNegocio,
                    slogan = config.slogan ?: "",
                    tamanoNombreNegocio = config.tamanoNombreNegocio,
                    mostrarSlogan = config.mostrarSlogan,
                    rncCedula = config.rncCedula ?: "",
                    posicionRnc = config.posicionRnc,
                    direccion = config.direccion ?: "",
                    posicionDireccion = config.posicionDireccion,
                    telefono = config.telefono ?: "",
                    posicionTelefono = config.posicionTelefono,
                    mostrarCajero = config.mostrarCajero,
                    mostrarCliente = config.mostrarCliente,
                    pieTicket = config.pieTicket,
                    mostrarPoweredBy = config.mostrarPoweredBy,
                    textoPoweredBy = config.textoPoweredBy,
                    nombreImpresora = config.nombreImpresora,
                    direccionMacImpresora = config.direccionMacImpresora,
                    tamanoPapel = config.tamanoPapelImpresora,
                    anchoMilimetrosPersonalizado = mmTexto,
                    columnasPersonalizadas = config.columnasPersonalizadas,
                    modoTema = config.modoTema,
                    estadoLicencia = estadoLic
                )
            }
            buscarDispositivosBluetooth()
        }
    }

    fun buscarDispositivosBluetooth() {
        val dispositivos = ServicioImpresoraBluetooth.obtenerDispositivosVinculados()
        _uiState.update { it.copy(dispositivosDisponibles = dispositivos) }
    }

    fun onNombreNegocioCambiado(nombre: String) {
        _uiState.update { it.copy(nombreNegocio = nombre) }
    }

    fun onSloganCambiado(slogan: String) {
        _uiState.update { it.copy(slogan = slogan) }
    }

    fun onTamanoNombreNegocioCambiado(tamano: Int) {
        _uiState.update { it.copy(tamanoNombreNegocio = tamano) }
    }

    fun onMostrarSloganCambiado(mostrar: Boolean) {
        _uiState.update { it.copy(mostrarSlogan = mostrar) }
    }

    fun onRncCedulaCambiado(rnc: String) {
        _uiState.update { it.copy(rncCedula = rnc) }
    }

    fun onPosicionRncCambiada(posicion: Int) {
        _uiState.update { it.copy(posicionRnc = posicion) }
    }

    fun onDireccionCambiada(dir: String) {
        _uiState.update { it.copy(direccion = dir) }
    }

    fun onPosicionDireccionCambiada(posicion: Int) {
        _uiState.update { it.copy(posicionDireccion = posicion) }
    }

    fun onTelefonoCambiado(tel: String) {
        _uiState.update { it.copy(telefono = tel) }
    }

    fun onPosicionTelefonoCambiada(posicion: Int) {
        _uiState.update { it.copy(posicionTelefono = posicion) }
    }

    fun onMostrarCajeroCambiado(mostrar: Boolean) {
        _uiState.update { it.copy(mostrarCajero = mostrar) }
    }

    fun onMostrarClienteCambiado(mostrar: Boolean) {
        _uiState.update { it.copy(mostrarCliente = mostrar) }
    }

    fun onPieTicketCambiado(pie: String) {
        _uiState.update { it.copy(pieTicket = pie) }
    }

    fun onMostrarPoweredByCambiado(mostrar: Boolean) {
        _uiState.update { it.copy(mostrarPoweredBy = mostrar) }
    }

    fun onTextoPoweredByCambiado(texto: String) {
        _uiState.update { it.copy(textoPoweredBy = texto) }
    }

    fun onTamanoPapelPresetSeleccionado(tamanoMm: Int) {
        val columnas = when (tamanoMm) {
            58 -> 32
            80 -> 48
            57 -> 30
            72 -> 42
            else -> 32
        }
        _uiState.update {
            it.copy(
                tamanoPapel = tamanoMm,
                anchoMilimetrosPersonalizado = tamanoMm.toString(),
                columnasPersonalizadas = columnas
            )
        }
    }

    fun onAnchoMilimetrosManualCambiado(mmTexto: String) {
        val soloDigitos = mmTexto.filter { it.isDigit() }.take(3)
        val mmDouble = soloDigitos.toDoubleOrNull()
        val columnasCalculadas = if (mmDouble != null && mmDouble > 0) {
            when (mmDouble.toInt()) {
                58 -> 32
                80 -> 48
                57 -> 30
                72 -> 42
                else -> (mmDouble * 0.58).roundToInt().coerceIn(16, 80)
            }
        } else {
            32
        }

        val esPreset = mmDouble != null && mmDouble.toInt() in listOf(58, 80, 57, 72)
        val tamanoPapel = if (esPreset) mmDouble!!.toInt() else (mmDouble?.toInt() ?: 0)

        _uiState.update {
            it.copy(
                tamanoPapel = tamanoPapel,
                anchoMilimetrosPersonalizado = soloDigitos,
                columnasPersonalizadas = columnasCalculadas
            )
        }
    }

    fun onIncrementarMilimetros(incremento: Int) {
        val mmActual = _uiState.value.anchoMilimetrosPersonalizado.toIntOrNull() ?: 58
        val nuevoMm = (mmActual + incremento).coerceIn(20, 150)
        onAnchoMilimetrosManualCambiado(nuevoMm.toString())
    }

    fun onColumnasPersonalizadasCambiadas(columnas: Int) {
        _uiState.update { it.copy(columnasPersonalizadas = columnas.coerceIn(16, 80)) }
    }

    fun onImpresoraSeleccionada(dispositivo: DispositivoBluetoothPos) {
        _uiState.update {
            it.copy(
                nombreImpresora = dispositivo.nombre,
                direccionMacImpresora = dispositivo.direccionMac
            )
        }
    }

    fun onDesvincularImpresora() {
        _uiState.update {
            it.copy(
                nombreImpresora = null,
                direccionMacImpresora = null
            )
        }
    }

    fun onModoTemaCambiado(modo: Int) {
        _uiState.update { it.copy(modoTema = modo) }
        guardarConfiguracion()
    }

    fun guardarConfiguracion() {
        val estado = _uiState.value
        val nombreNegocioTrim = estado.nombreNegocio.trim().ifEmpty { "Mi Tienda" }
        val mmInt = estado.anchoMilimetrosPersonalizado.toIntOrNull() ?: 58

        _uiState.update { it.copy(estaGuardando = true, mensajeAlerta = null) }

        viewModelScope.launch {
            try {
                val entidad = ConfiguracionNegocioEntidad(
                    id = 1,
                    nombreNegocio = nombreNegocioTrim,
                    slogan = estado.slogan.trim().ifEmpty { null },
                    tamanoNombreNegocio = estado.tamanoNombreNegocio,
                    mostrarSlogan = estado.mostrarSlogan,
                    rncCedula = estado.rncCedula.trim().ifEmpty { null },
                    posicionRnc = estado.posicionRnc,
                    direccion = estado.direccion.trim().ifEmpty { null },
                    posicionDireccion = estado.posicionDireccion,
                    telefono = estado.telefono.trim().ifEmpty { null },
                    posicionTelefono = estado.posicionTelefono,
                    mostrarCajero = estado.mostrarCajero,
                    mostrarCliente = estado.mostrarCliente,
                    pieTicket = estado.pieTicket.trim().ifEmpty { "¡Gracias por su compra!" },
                    mostrarPoweredBy = true,
                    textoPoweredBy = "Powered by TyPOS Móvil",
                    nombreImpresora = estado.nombreImpresora,
                    direccionMacImpresora = estado.direccionMacImpresora,
                    tamanoPapelImpresora = mmInt,
                    columnasPersonalizadas = estado.columnasPersonalizadas,
                    modoTema = estado.modoTema
                )

                repositorio.guardarConfiguracion(entidad)
                _uiState.update {
                    it.copy(
                        estaGuardando = false,
                        guardadoExitoso = true,
                        mensajeAlerta = "Configuración guardada correctamente",
                        esErrorAlerta = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        estaGuardando = false,
                        mensajeAlerta = "Error al guardar: ${e.localizedMessage ?: "Error desconocido"}",
                        esErrorAlerta = true
                    )
                }
            }
        }
    }

    fun imprimirTicketPrueba() {
        val estado = _uiState.value
        val mac = estado.direccionMacImpresora
        if (mac.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    mensajeAlerta = "Selecciona primero una impresora Bluetooth vinculada",
                    esErrorAlerta = true
                )
            }
            return
        }

        _uiState.update { it.copy(estaImprimiendoPrueba = true, mensajeAlerta = null) }

        viewModelScope.launch {
            val mmInt = estado.anchoMilimetrosPersonalizado.toIntOrNull() ?: 58
            val entidad = ConfiguracionNegocioEntidad(
                id = 1,
                nombreNegocio = estado.nombreNegocio,
                slogan = estado.slogan.trim().ifEmpty { null },
                tamanoNombreNegocio = estado.tamanoNombreNegocio,
                mostrarSlogan = estado.mostrarSlogan,
                rncCedula = estado.rncCedula.trim().ifEmpty { null },
                posicionRnc = estado.posicionRnc,
                direccion = estado.direccion.trim().ifEmpty { null },
                posicionDireccion = estado.posicionDireccion,
                telefono = estado.telefono.trim().ifEmpty { null },
                posicionTelefono = estado.posicionTelefono,
                mostrarCajero = estado.mostrarCajero,
                mostrarCliente = estado.mostrarCliente,
                pieTicket = estado.pieTicket.trim().ifEmpty { "¡Gracias por su compra!" },
                mostrarPoweredBy = true,
                textoPoweredBy = "Powered by TyPOS Móvil",
                nombreImpresora = estado.nombreImpresora,
                direccionMacImpresora = mac,
                tamanoPapelImpresora = mmInt,
                columnasPersonalizadas = estado.columnasPersonalizadas
            )

            val resultado = ServicioImpresoraBluetooth.imprimirTicketPrueba(
                direccionMac = mac,
                tamanoPapel = mmInt,
                configuracion = entidad
            )

            when (resultado) {
                is ResultadoImpresion.Exito -> {
                    _uiState.update {
                        it.copy(
                            estaImprimiendoPrueba = false,
                            mensajeAlerta = "¡Ticket de prueba impreso correctamente!",
                            esErrorAlerta = false
                        )
                    }
                }
                is ResultadoImpresion.Error -> {
                    _uiState.update {
                        it.copy(
                            estaImprimiendoPrueba = false,
                            mensajeAlerta = resultado.mensaje,
                            esErrorAlerta = true
                        )
                    }
                }
            }
        }
    }

    fun buscarActualizacionesManualmente() {
        _uiState.update { it.copy(estaBuscandoActualizaciones = true, mensajeAlerta = null) }
        viewModelScope.launch {
            val contexto = getApplication<Application>()
            if (!com.typdevstudio.typos_movil.utilidades.actualizador.ActualizadorApp.hayConexionInternet(contexto)) {
                _uiState.update {
                    it.copy(
                        estaBuscandoActualizaciones = false,
                        mensajeAlerta = "Sin conexión a Internet. Conéctate a una red Wi-Fi o datos móviles para buscar actualizaciones.",
                        esErrorAlerta = true
                    )
                }
                return@launch
            }

            val resultado = com.typdevstudio.typos_movil.utilidades.actualizador.ActualizadorApp.verificarActualizaciones(contexto)
            if (resultado != null && resultado.hayActualizacion) {
                _uiState.update {
                    it.copy(
                        estaBuscandoActualizaciones = false,
                        infoActualizacion = resultado
                    )
                }
            } else if (resultado != null) {
                _uiState.update {
                    it.copy(
                        estaBuscandoActualizaciones = false,
                        mensajeAlerta = "¡TyPOS Móvil ya está en la última versión (${resultado.versionActual})!",
                        esErrorAlerta = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        estaBuscandoActualizaciones = false,
                        mensajeAlerta = "No se pudo consultar el servidor de actualizaciones en este momento.",
                        esErrorAlerta = true
                    )
                }
            }
        }
    }

    fun descartarModalActualizacion() {
        _uiState.update { it.copy(infoActualizacion = null) }
    }

    fun iniciarDescargaActualizacion(url: String) {
        val contexto = getApplication<Application>()
        com.typdevstudio.typos_movil.utilidades.actualizador.ActualizadorApp.abrirEnNavegador(contexto, url)
        _uiState.update { it.copy(infoActualizacion = null) }
    }

    fun onClaveLicenciaNuevaCambiada(clave: String) {
        val soloAlfanumerico = clave.replace("-", "").replace(" ", "").trim().uppercase().take(16)
        val formateada = soloAlfanumerico.chunked(4).joinToString("-")
        _uiState.update { it.copy(claveLicenciaNueva = formateada) }
    }

    fun activarLicenciaNueva() {
        val clave = _uiState.value.claveLicenciaNueva.trim()
        if (clave.isBlank()) {
            _uiState.update {
                it.copy(
                    mensajeAlerta = "Por favor ingresa la clave de licencia de 16 caracteres",
                    esErrorAlerta = true
                )
            }
            return
        }

        _uiState.update { it.copy(estaActivandoLicencia = true, mensajeAlerta = null) }

        viewModelScope.launch {
            val resultado = licenciaRepositorio.activarLicencia(clave)
            if (resultado.esValida && !resultado.estaVencida) {
                val nuevoEstado = licenciaRepositorio.verificarEstadoLicencia()
                _uiState.update {
                    it.copy(
                        estaActivandoLicencia = false,
                        estadoLicencia = nuevoEstado,
                        claveLicenciaNueva = "",
                        mensajeAlerta = "¡Licencia activada con éxito! Válida por ${resultado.diasValidez} días.",
                        esErrorAlerta = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        estaActivandoLicencia = false,
                        mensajeAlerta = resultado.mensajeError.ifBlank { "Clave de licencia incorrecta o inválida" },
                        esErrorAlerta = true
                    )
                }
            }
        }
    }

    fun limpiarAlerta() {
        _uiState.update { it.copy(mensajeAlerta = null) }
    }
}
