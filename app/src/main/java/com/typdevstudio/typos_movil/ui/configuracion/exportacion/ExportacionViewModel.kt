package com.typdevstudio.typos_movil.ui.configuracion.exportacion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.typdevstudio.typos_movil.BuildConfig
import com.typdevstudio.typos_movil.datos.local.AppBaseDatos
import com.typdevstudio.typos_movil.utilidades.exportacion.CodigosExportacion
import com.typdevstudio.typos_movil.utilidades.exportacion.ErrorExportacion
import com.typdevstudio.typos_movil.utilidades.exportacion.ExportacionUiState
import com.typdevstudio.typos_movil.utilidades.exportacion.GestorArchivoExportacion
import com.typdevstudio.typos_movil.utilidades.exportacion.GestorExportacionProductos
import com.typdevstudio.typos_movil.utilidades.exportacion.ReportadorEventosExportacion
import com.typdevstudio.typos_movil.utilidades.exportacion.TiposEventoExportacion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExportacionViewModel(application: Application) : AndroidViewModel(application) {

    private val contexto = application.applicationContext
    private val bd = AppBaseDatos.obtenerBaseDatos(application)

    private val _uiState = MutableStateFlow(ExportacionUiState())
    val uiState: StateFlow<ExportacionUiState> = _uiState.asStateFlow()

    /** Id de la última exportación, para unir los eventos de compartir y guardar con ella. */
    private var idOperacionActual: String? = null

    fun cambiarIncluirInactivos(valor: Boolean) {
        _uiState.update { it.copy(opciones = it.opciones.copy(incluirInactivos = valor), resultado = null) }
    }

    fun cambiarIncluirFotos(valor: Boolean) {
        _uiState.update { it.copy(opciones = it.opciones.copy(incluirFotos = valor), resultado = null) }
    }

    fun exportar() {
        if (_uiState.value.estaExportando) return
        val opciones = _uiState.value.opciones
        val idOperacion = CodigosExportacion.nuevaOperacion()
        idOperacionActual = idOperacion
        _uiState.update { it.copy(estaExportando = true, progreso = null, resultado = null, mensajeAlerta = null) }
        ReportadorEventosExportacion.registrar(
            TiposEventoExportacion.INICIADA, idOperacion,
            detalleTecnico = "incluir_inactivos=${opciones.incluirInactivos} incluir_fotos=${opciones.incluirFotos}",
        )

        viewModelScope.launch {
            try {
                val resultado = withContext(Dispatchers.IO) {
                    val productos = bd.productoDao().obtenerTodos().first()
                    val negocio = bd.configuracionNegocioDao().obtenerConfiguracionDirecta()?.nombreNegocio
                    val destino = GestorArchivoExportacion.prepararDestino(
                        contexto, GestorExportacionProductos.nombreArchivo(),
                    )
                    GestorExportacionProductos.exportar(
                        productos = productos,
                        opciones = opciones,
                        destino = destino,
                        versionApp = BuildConfig.VERSION_NAME,
                        nombreNegocio = negocio,
                    ) { progreso -> _uiState.update { it.copy(progreso = progreso) } }
                }
                if (resultado.fotosFaltantes.isNotEmpty()) {
                    ReportadorEventosExportacion.registrar(
                        TiposEventoExportacion.COMPLETADA, idOperacion, CodigosExportacion.FOTO_FALTANTE, "fotos",
                        "fotos_faltantes=${resultado.fotosFaltantes.size}",
                    )
                } else {
                    ReportadorEventosExportacion.registrar(
                        TiposEventoExportacion.COMPLETADA, idOperacion,
                        detalleTecnico = "productos=${resultado.cantidadProductos} fotos=${resultado.cantidadFotos} " +
                            "bytes=${resultado.archivo.length()}",
                    )
                }
                _uiState.update {
                    it.copy(estaExportando = false, progreso = null, resultado = resultado)
                }
                mostrarAlerta(resultado.mensajeExito, esError = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val falla = e as? ErrorExportacion
                    ?: ErrorExportacion(CodigosExportacion.ERROR_ESCRITURA, "${e.javaClass.simpleName}: ${e.message}", e, "leer_datos")
                ReportadorEventosExportacion.registrar(
                    TiposEventoExportacion.FALLIDA, idOperacion, falla.codigo, falla.paso, falla.detalleTecnico,
                )
                _uiState.update { it.copy(estaExportando = false, progreso = null) }
                mostrarAlerta(falla.mensajeUsuario, esError = true)
            }
        }
    }

    fun compartir() {
        val resultado = _uiState.value.resultado ?: return
        val idOperacion = idOperacionActual ?: CodigosExportacion.nuevaOperacion()
        try {
            GestorArchivoExportacion.compartir(contexto, resultado.archivo)
            ReportadorEventosExportacion.registrar(TiposEventoExportacion.COMPARTIDA, idOperacion)
        } catch (e: ErrorExportacion) {
            ReportadorEventosExportacion.registrar(
                TiposEventoExportacion.FALLIDA, idOperacion, e.codigo, e.paso, e.detalleTecnico,
            )
            mostrarAlerta(e.mensajeUsuario, esError = true)
        }
    }

    fun guardarEnDescargas() {
        val resultado = _uiState.value.resultado ?: return
        if (_uiState.value.estaGuardando) return
        val idOperacion = idOperacionActual ?: CodigosExportacion.nuevaOperacion()
        _uiState.update { it.copy(estaGuardando = true, mensajeAlerta = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { GestorArchivoExportacion.guardarEnDescargas(contexto, resultado.archivo) }
                ReportadorEventosExportacion.registrar(TiposEventoExportacion.GUARDADA, idOperacion)
                mostrarAlerta("Listo. El archivo ${resultado.archivo.name} quedó guardado en Descargas.", esError = false)
            } catch (e: ErrorExportacion) {
                ReportadorEventosExportacion.registrar(
                    TiposEventoExportacion.FALLIDA, idOperacion, e.codigo, e.paso, e.detalleTecnico,
                )
                mostrarAlerta(e.mensajeUsuario, esError = true)
            } finally {
                _uiState.update { it.copy(estaGuardando = false) }
            }
        }
    }

    fun limpiarAlerta() {
        _uiState.update { it.copy(mensajeAlerta = null) }
    }

    private fun mostrarAlerta(mensaje: String, esError: Boolean) {
        _uiState.update { it.copy(mensajeAlerta = mensaje, esError = esError) }
    }
}
