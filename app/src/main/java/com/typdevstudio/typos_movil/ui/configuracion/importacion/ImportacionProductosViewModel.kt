package com.typdevstudio.typos_movil.ui.configuracion.importacion

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.typdevstudio.typos_movil.datos.local.AppBaseDatos
import com.typdevstudio.typos_movil.datos.local.GestorSesion
import com.typdevstudio.typos_movil.datos.local.entidades.ProductoEntidad
import com.typdevstudio.typos_movil.datos.repositorio.ProductoRepositorio
import com.typdevstudio.typos_movil.utilidades.importacion.GestorPlantillaProductos
import com.typdevstudio.typos_movil.utilidades.importacion.ImportacionUiState
import com.typdevstudio.typos_movil.utilidades.importacion.LectorCsvProductos
import com.typdevstudio.typos_movil.utilidades.importacion.PasoImportacion
import com.typdevstudio.typos_movil.utilidades.importacion.ProductoImportadoUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImportacionProductosViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio: ProductoRepositorio
    private val contexto = application.applicationContext

    private val _uiState = MutableStateFlow(ImportacionUiState())
    val uiState: StateFlow<ImportacionUiState> = _uiState.asStateFlow()

    init {
        val bd = AppBaseDatos.obtenerBaseDatos(application)
        repositorio = ProductoRepositorio(bd.productoDao())
    }

    /**
     * Carga y procesa un archivo CSV desde la URI seleccionada por el usuario.
     */
    fun cargarArchivoCsv(uri: Uri, nombreArchivo: String?) {
        _uiState.update {
            it.copy(
                estaCargandoArchivo = true,
                mensajeError = null,
                nombreArchivo = nombreArchivo
            )
        }

        viewModelScope.launch {
            try {
                val productosExistentes = repositorio.obtenerTodosDirecto()
                val productosLeidos = withContext(Dispatchers.IO) {
                    LectorCsvProductos.leerDesdeUri(contexto, uri, productosExistentes)
                }

                if (productosLeidos.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            estaCargandoArchivo = false,
                            mensajeError = "No se encontraron filas con datos válidos en el archivo seleccionado."
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            estaCargandoArchivo = false,
                            pasoActual = PasoImportacion.VISTA_PREVIA,
                            totalLineasLeidas = productosLeidos.size,
                            productos = productosLeidos,
                            mensajeError = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        estaCargandoArchivo = false,
                        mensajeError = "Error al leer el archivo: ${e.localizedMessage ?: "Formato inválido"}"
                    )
                }
            }
        }
    }

    fun onFiltroTextoCambiado(texto: String) {
        _uiState.update { it.copy(filtroTexto = texto) }
    }

    fun onAlternarMostrarSoloErrores() {
        _uiState.update { it.copy(mostrarSoloErrores = !it.mostrarSoloErrores) }
    }

    fun onAlternarActualizarExistentes() {
        _uiState.update { it.copy(actualizarExistentes = !it.actualizarExistentes) }
    }

    fun iniciarEdicionProducto(producto: ProductoImportadoUi) {
        _uiState.update { it.copy(productoParaEditar = producto) }
    }

    fun cancelarEdicionProducto() {
        _uiState.update { it.copy(productoParaEditar = null) }
    }

    /**
     * Guarda los cambios de un producto editado en tiempo real en la vista previa.
     */
    fun guardarEdicionProducto(productoModificado: ProductoImportadoUi) {
        viewModelScope.launch {
            val productosExistentes = repositorio.obtenerTodosDirecto()
            val productoRevalidado = LectorCsvProductos.revalidarProducto(productoModificado, productosExistentes)

            _uiState.update { estado ->
                val nuevaLista = estado.productos.map { item ->
                    if (item.idTemporal == productoRevalidado.idTemporal) {
                        productoRevalidado
                    } else {
                        item
                    }
                }
                estado.copy(
                    productos = nuevaLista,
                    productoParaEditar = null
                )
            }
        }
    }

    /**
     * Elimina una fila individual de la lista previa.
     */
    fun eliminarProductoDeLista(idTemporal: String) {
        _uiState.update { estado ->
            val nuevaLista = estado.productos.filterNot { it.idTemporal == idTemporal }
            estado.copy(productos = nuevaLista)
        }
    }

    /**
     * Agrega un nuevo producto manual a la vista previa.
     */
    fun agregarProductoManual() {
        val nuevo = ProductoImportadoUi(
            nombre = "Nuevo Producto",
            precioVenta = 0.0,
            stock = 1.0,
            categoria = "General",
            esValido = false,
            errores = listOf("Ingresa un precio de venta mayor a 0.")
        )
        _uiState.update { estado ->
            estado.copy(
                productos = listOf(nuevo) + estado.productos,
                productoParaEditar = nuevo
            )
        }
    }

    /**
     * Ejecuta la inserción / actualización en lote de todos los productos válidos.
     */
    fun ejecutarImportacion() {
        val estadoActual = _uiState.value
        val productosValidos = estadoActual.productos.filter { it.esValido }

        if (productosValidos.isEmpty()) {
            _uiState.update { it.copy(mensajeError = "No hay productos válidos para importar.") }
            return
        }

        _uiState.update {
            it.copy(
                estaGuardandoEnBd = true,
                pasoActual = PasoImportacion.PROCESANDO,
                mensajeError = null
            )
        }

        viewModelScope.launch {
            try {
                val usuarioActivo = GestorSesion.usuarioActivo.value
                var contadorActualizados = 0
                var contadorCreados = 0

                val entidadesAGuardar = productosValidos.map { item ->
                    val esActualizar = item.esActualizacion && estadoActual.actualizarExistentes
                    val idFinal = if (esActualizar) (item.idExistente ?: 0L) else 0L

                    if (esActualizar) {
                        contadorActualizados++
                    } else {
                        contadorCreados++
                    }

                    ProductoEntidad(
                        id = idFinal,
                        idUsuario = usuarioActivo?.id,
                        codigoBarras = item.codigoBarras.trim().ifEmpty { null },
                        nombre = item.nombre.trim(),
                        descripcion = item.descripcion.trim().ifEmpty { null },
                        precioCosto = item.precioCosto,
                        precioVenta = item.precioVenta,
                        exentoItbis = item.exentoItbis,
                        itbisIncluido = item.itbisIncluido,
                        tasaItbis = if (item.exentoItbis) 0.0 else item.tasaItbis,
                        stock = item.stock,
                        controlaStock = item.controlaStock,
                        categoria = item.categoria.trim().ifEmpty { "General" },
                        estaActivo = true,
                        actualizadoEn = System.currentTimeMillis()
                    )
                }

                val totalGuardados = repositorio.guardarProductosEnLote(entidadesAGuardar)

                _uiState.update {
                    it.copy(
                        estaGuardandoEnBd = false,
                        pasoActual = PasoImportacion.RESULTADO,
                        totalImportados = totalGuardados,
                        totalActualizados = contadorActualizados,
                        totalCreados = contadorCreados,
                        mensajeExito = "¡$totalGuardados productos procesados exitosamente!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        estaGuardandoEnBd = false,
                        pasoActual = PasoImportacion.VISTA_PREVIA,
                        mensajeError = "Error al guardar en la base de datos: ${e.localizedMessage ?: "Error desconocido"}"
                    )
                }
            }
        }
    }

    /**
     * Reinicia el asistente de importación para cargar un nuevo archivo.
     */
    fun reiniciarImportacion() {
        _uiState.value = ImportacionUiState()
    }

    fun compartirPlantilla() {
        GestorPlantillaProductos.compartirPlantilla(contexto)
    }

    fun descargarPlantilla() {
        val (exito, uri) = GestorPlantillaProductos.guardarPlantillaEnDescargas(contexto)
        if (exito) {
            _uiState.update {
                it.copy(
                    uriPlantillaDescargada = uri,
                    mostrarNotificacionDescarga = true
                )
            }
        }
    }

    fun abrirPlantillaDescargada() {
        val uri = _uiState.value.uriPlantillaDescargada
        GestorPlantillaProductos.abrirArchivoPlantilla(contexto, uri)
    }

    fun descartarNotificacionDescarga() {
        _uiState.update { it.copy(mostrarNotificacionDescarga = false) }
    }
}
