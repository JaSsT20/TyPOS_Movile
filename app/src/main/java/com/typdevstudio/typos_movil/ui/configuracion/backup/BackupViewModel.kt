package com.typdevstudio.typos_movil.ui.configuracion.backup

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.typdevstudio.typos_movil.utilidades.backup.ArchivoBackupUi
import com.typdevstudio.typos_movil.utilidades.backup.BackupUiState
import com.typdevstudio.typos_movil.utilidades.backup.CodigosBackup
import com.typdevstudio.typos_movil.utilidades.backup.ConfirmacionRestauracion
import com.typdevstudio.typos_movil.utilidades.backup.ErrorBackup
import com.typdevstudio.typos_movil.utilidades.backup.GestorBackup
import com.typdevstudio.typos_movil.utilidades.backup.PlanificadorBackupDiario
import com.typdevstudio.typos_movil.utilidades.backup.ReinicioApp
import com.typdevstudio.typos_movil.utilidades.backup.ReportadorEventosBackup
import com.typdevstudio.typos_movil.utilidades.backup.ResultadoRestauracion
import com.typdevstudio.typos_movil.utilidades.backup.TiposEventoBackup
import com.typdevstudio.typos_movil.utilidades.backup.VarianteRestauracion
import com.typdevstudio.typos_movil.utilidades.licencia.LicenciaGuard
import com.typdevstudio.typos_movil.utilidades.licencia.nube.ResultadoConsultaTransferencia
import com.typdevstudio.typos_movil.utilidades.licencia.nube.ResultadoTransferencia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BackupViewModel(application: Application) : AndroidViewModel(application) {

    private val contexto = application.applicationContext

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        cargarBackups()
        PlanificadorBackupDiario.programarBackupDiario(contexto)
    }

    fun cargarBackups() {
        viewModelScope.launch {
            val lista = withContext(Dispatchers.IO) {
                GestorBackup.obtenerListaBackups(contexto)
            }
            _uiState.update { it.copy(backups = lista) }
        }
    }

    fun crearBackupManual() {
        _uiState.update { it.copy(estaCreandoBackup = true, mensajeAlerta = null) }
        val idOperacion = CodigosBackup.nuevaOperacion()

        viewModelScope.launch {
            val resultado = withContext(Dispatchers.IO) {
                GestorBackup.crearBackup(contexto, esAutomatico = false, idOperacion = idOperacion)
            }
            val lista = withContext(Dispatchers.IO) { GestorBackup.obtenerListaBackups(contexto) }
            _uiState.update { it.copy(estaCreandoBackup = false, backups = lista) }
            resultado
                .onSuccess { mostrarExito(MENSAJE_COPIA_CREADA) }
                .onFailure { mostrarError(it, idOperacion) }
        }
    }

    // ------------------------------------------------------------------
    // Importar
    // ------------------------------------------------------------------

    /** El usuario eligió un archivo (Descargas, WhatsApp, Drive...). Se copia y se revisa; no se restaura. */
    fun importarBackup(uri: Uri) {
        _uiState.update { it.copy(estaImportando = true, mensajeAlerta = null) }
        val idOperacion = CodigosBackup.nuevaOperacion()
        viewModelScope.launch {
            val resultado = withContext(Dispatchers.IO) { GestorBackup.importarBackup(contexto, uri, idOperacion) }
            val lista = withContext(Dispatchers.IO) { GestorBackup.obtenerListaBackups(contexto) }
            _uiState.update { it.copy(estaImportando = false, backups = lista) }
            resultado
                .onSuccess { mostrarExito(MENSAJE_COPIA_IMPORTADA) }
                .onFailure { mostrarError(it, idOperacion) }
        }
    }

    // ------------------------------------------------------------------
    // Restaurar
    // ------------------------------------------------------------------

    /**
     * Decide qué aviso mostrar. Una copia de este teléfono se restaura sin internet. Una de otro teléfono consulta
     * primero la licencia: si este teléfono ya está, se restaura normal; si hay cupo, se agrega; si no, el otro
     * teléfono se desvincula (solo si el usuario acepta el aviso).
     */
    fun seleccionarParaRestaurar(backup: ArchivoBackupUi) {
        val idOperacion = CodigosBackup.nuevaOperacion()
        if (!backup.valida) {
            mostrarError(ErrorBackup(CodigosBackup.ARCHIVO_DANADO, "manifiesto ilegible"), idOperacion)
            return
        }
        if (backup.deEsteTelefono) {
            _uiState.update {
                it.copy(confirmacion = ConfirmacionRestauracion(backup, VarianteRestauracion.Normal, idOperacion, deOtroTelefono = false))
            }
            return
        }

        _uiState.update { it.copy(estaRestaurandoBackup = true, textoProgreso = "Consultando su licencia...", mensajeAlerta = null) }
        viewModelScope.launch {
            val confirmacion = prepararCopiaDeOtroTelefono(backup, idOperacion)
            _uiState.update { it.copy(estaRestaurandoBackup = false, textoProgreso = null, confirmacion = confirmacion) }
        }
    }

    /** La confirmación que corresponde a una copia de otro teléfono, o `null` si no se puede (ya avisó el error). */
    private suspend fun prepararCopiaDeOtroTelefono(backup: ArchivoBackupUi, idOperacion: String): ConfirmacionRestauracion? {
        val identidad = withContext(Dispatchers.IO) { LicenciaGuard.identidadParaBackup() }
        val tokenOrigen = backup.manifiesto?.tokenOrigen

        if (tokenOrigen == null) {
            // El otro teléfono no tenía la licencia nueva: no se puede desvincular desde la copia. Si este ya está
            // activado, solo se restauran los datos; si no, primero hay que activarlo con la clave.
            return if (identidad?.clave != null) {
                ConfirmacionRestauracion(backup, VarianteRestauracion.Normal, idOperacion, deOtroTelefono = true)
            } else {
                fallaAntesDeRestaurar(ErrorBackup(CodigosBackup.SIN_LICENCIA_ORIGEN, "copia sin token_origen"), idOperacion, "consultar")
                null
            }
        }

        val licenciaDeLaCopia = withContext(Dispatchers.IO) { LicenciaGuard.licenciaDeToken(tokenOrigen) }
        if (identidad?.clave != null && licenciaDeLaCopia != null && identidad.clave != licenciaDeLaCopia) {
            fallaAntesDeRestaurar(ErrorBackup(CodigosBackup.OTRA_LICENCIA, "este teléfono tiene otra licencia"), idOperacion, "consultar")
            return null
        }

        return when (val consulta = LicenciaGuard.consultarTransferencia(tokenOrigen, idOperacion)) {
            is ResultadoConsultaTransferencia.SinConexion -> {
                fallaAntesDeRestaurar(ErrorBackup(CodigosBackup.SIN_INTERNET, consulta.mensaje), idOperacion, "consultar")
                null
            }
            is ResultadoConsultaTransferencia.Rechazada -> {
                rechazoDeLaApi(consulta.codigo, consulta.mensaje, idOperacion, "consultar")
                null
            }
            is ResultadoConsultaTransferencia.Disponible -> {
                val c = consulta.consulta
                val nombre = c.nombreOrigen ?: backup.nombreOrigen
                val variante = VarianteRestauracion.desdeConsulta(c.yaRegistrado, c.hayCupo, c.enUso, c.maxDispositivos, nombre)
                ConfirmacionRestauracion(backup, variante, idOperacion, deOtroTelefono = true, requiereTransferencia = !c.yaRegistrado)
            }
        }
    }

    fun cancelarRestauracion() {
        _uiState.update { it.copy(confirmacion = null) }
    }

    fun confirmarRestauracion() {
        val confirmacion = _uiState.value.confirmacion ?: return
        val idOperacion = confirmacion.idOperacion
        _uiState.update {
            it.copy(confirmacion = null, estaRestaurandoBackup = true, textoProgreso = null, mensajeAlerta = null)
        }

        viewModelScope.launch {
            var desvinculado: String? = null
            if (confirmacion.requiereTransferencia) {
                _uiState.update { it.copy(textoProgreso = "Agregando este teléfono a su licencia...") }
                val tokenOrigen = confirmacion.backup.manifiesto?.tokenOrigen.orEmpty()
                val permitir = confirmacion.variante is VarianteRestauracion.Desvincular
                when (val resultado = LicenciaGuard.transferir(tokenOrigen, permitir, idOperacion)) {
                    is ResultadoTransferencia.Exito -> desvinculado = resultado.desvinculado
                    is ResultadoTransferencia.Rechazada -> {
                        rechazoDeLaApi(resultado.codigo, resultado.mensaje, idOperacion, "transferir")
                        terminarSinRestaurar()
                        return@launch
                    }
                    is ResultadoTransferencia.SinConexion -> {
                        fallaAntesDeRestaurar(ErrorBackup(CodigosBackup.SIN_INTERNET, resultado.mensaje), idOperacion, "transferir")
                        terminarSinRestaurar()
                        return@launch
                    }
                    is ResultadoTransferencia.TokenNoValido -> {
                        fallaAntesDeRestaurar(ErrorBackup(CodigosBackup.RESTAURACION_FALLIDA, resultado.mensaje), idOperacion, "transferir")
                        terminarSinRestaurar()
                        return@launch
                    }
                }
            }

            // Restaurar reemplaza los datos: en modo solo consulta no se permite (crear y compartir copias sí).
            // Con una copia de otro teléfono se revisa después de transferir, cuando este teléfono ya tiene licencia.
            if (!LicenciaGuard.puedeEmitir()) {
                terminarSinRestaurar()
                _uiState.update { it.copy(mensajeAlerta = LicenciaGuard.mensajeAccionBloqueada(), esError = true) }
                return@launch
            }

            _uiState.update { it.copy(textoProgreso = "Restaurando su copia de seguridad...") }
            val resultado = withContext(Dispatchers.IO) {
                GestorBackup.restaurarBackup(contexto, confirmacion.backup, confirmacion.deOtroTelefono, idOperacion)
            }
            when (resultado) {
                ResultadoRestauracion.Exito -> _uiState.update {
                    it.copy(
                        estaRestaurandoBackup = false,
                        textoProgreso = null,
                        mensajeReinicio = mensajeRestaurada(confirmacion.variante, desvinculado),
                        reinicioEsError = false,
                    )
                }
                is ResultadoRestauracion.Fallo -> if (resultado.requiereReinicio) {
                    _uiState.update {
                        it.copy(
                            estaRestaurandoBackup = false,
                            textoProgreso = null,
                            mensajeReinicio = conCodigo(resultado.error.mensajeUsuario, idOperacion) + "\n\nLa app se va a reiniciar.",
                            reinicioEsError = true,
                        )
                    }
                } else {
                    terminarSinRestaurar()
                    mostrarError(resultado.error, idOperacion)
                }
            }
        }
    }

    private fun terminarSinRestaurar() {
        _uiState.update { it.copy(estaRestaurandoBackup = false, textoProgreso = null) }
        cargarBackups()
    }

    /** El usuario cerró el aviso final de la restauración: la app se reinicia para cargar los datos nuevos. */
    fun reiniciarApp() {
        ReinicioApp.reiniciar(contexto)
    }

    private fun mensajeRestaurada(variante: VarianteRestauracion, desvinculado: String?): String = when {
        desvinculado != null -> "Listo, sus datos se recuperaron. $desvinculado ya no está en su cuenta. La app se va a reiniciar."
        variante is VarianteRestauracion.AgregarTelefono ->
            "Listo, sus datos se recuperaron y este teléfono ya está en su cuenta. La app se va a reiniciar."
        else -> "Listo, sus datos se recuperaron. La app se va a reiniciar."
    }

    // ------------------------------------------------------------------
    // Compartir y eliminar
    // ------------------------------------------------------------------

    /** Antes de compartir se advierte que el archivo lleva todos los datos del negocio. */
    fun compartirBackup(backup: ArchivoBackupUi) {
        _uiState.update { it.copy(backupParaCompartir = backup) }
    }

    fun cancelarCompartir() {
        _uiState.update { it.copy(backupParaCompartir = null) }
    }

    fun confirmarCompartir() {
        val backup = _uiState.value.backupParaCompartir ?: return
        _uiState.update { it.copy(backupParaCompartir = null) }
        GestorBackup.compartirBackup(contexto, backup.archivo)
    }

    fun seleccionarParaEliminar(backup: ArchivoBackupUi) {
        _uiState.update { it.copy(backupSeleccionadoParaEliminar = backup) }
    }

    fun cancelarEliminacion() {
        _uiState.update { it.copy(backupSeleccionadoParaEliminar = null) }
    }

    fun confirmarEliminacion() {
        val backup = _uiState.value.backupSeleccionadoParaEliminar ?: return

        _uiState.update { it.copy(backupSeleccionadoParaEliminar = null) }

        viewModelScope.launch {
            val exito = withContext(Dispatchers.IO) {
                GestorBackup.eliminarBackup(backup.archivo)
            }
            if (exito) mostrarExito("Copia eliminada.")
            cargarBackups()
        }
    }

    fun limpiarAlerta() {
        _uiState.update { it.copy(mensajeAlerta = null) }
    }

    // ------------------------------------------------------------------
    // Mensajes: sin palabras técnicas; en los errores, el código de soporte para encontrar la operación en los logs.
    // ------------------------------------------------------------------

    private fun mostrarExito(mensaje: String) {
        _uiState.update { it.copy(mensajeAlerta = mensaje, esError = false) }
    }

    private fun mostrarError(falla: Throwable, idOperacion: String) {
        val mensaje = (falla as? ErrorBackup)?.mensajeUsuario ?: CodigosBackup.mensaje("")
        _uiState.update { it.copy(mensajeAlerta = conCodigo(mensaje, idOperacion), esError = true) }
    }

    /** Falla antes de tocar los datos (sin internet, otra cuenta...): se avisa y se reporta a la API. */
    private fun fallaAntesDeRestaurar(falla: ErrorBackup, idOperacion: String, paso: String) {
        ReportadorEventosBackup.registrar(
            contexto, TiposEventoBackup.RESTAURACION_FALLIDA, idOperacion, falla.codigo, paso, falla.detalleTecnico,
        )
        mostrarError(falla, idOperacion)
    }

    /** La API no permitió la transferencia: mensaje del catálogo para ese código y código de soporte. */
    private fun rechazoDeLaApi(codigo: String, mensaje: String, idOperacion: String, paso: String) {
        ReportadorEventosBackup.registrar(contexto, TiposEventoBackup.RESTAURACION_FALLIDA, idOperacion, codigo, paso)
        val texto = CodigosBackup.mensajeDeTransferencia(codigo, mensaje)
        _uiState.update { it.copy(mensajeAlerta = conCodigo(texto, idOperacion, "TR"), esError = true) }
    }

    private fun conCodigo(mensaje: String, idOperacion: String, prefijo: String = "BK") =
        "$mensaje\nCódigo de soporte: ${CodigosBackup.codigoSoporte(idOperacion, prefijo)}"

    private companion object {
        const val MENSAJE_COPIA_CREADA = "Listo, su copia de seguridad se guardó."
        const val MENSAJE_COPIA_IMPORTADA = "Copia recibida. Revísela y toque Restaurar cuando quiera."
    }
}
