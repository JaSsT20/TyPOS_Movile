package com.typdevstudio.typos_movil.ui.configuracion.backup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.typdevstudio.typos_movil.utilidades.backup.ArchivoBackupUi
import com.typdevstudio.typos_movil.utilidades.backup.BackupUiState
import com.typdevstudio.typos_movil.utilidades.backup.GestorBackup
import com.typdevstudio.typos_movil.utilidades.backup.PlanificadorBackupDiario
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

        viewModelScope.launch {
            val resultado = withContext(Dispatchers.IO) {
                GestorBackup.crearBackup(contexto, esAutomatico = false)
            }

            if (resultado.isSuccess) {
                val listaActualizada = withContext(Dispatchers.IO) {
                    GestorBackup.obtenerListaBackups(contexto)
                }
                _uiState.update {
                    it.copy(
                        estaCreandoBackup = false,
                        backups = listaActualizada,
                        mensajeAlerta = "¡Copia de seguridad creada con éxito! (Guardando los últimos 7 días)",
                        esError = false
                    )
                }
            } else {
                val errorMsg = resultado.exceptionOrNull()?.localizedMessage ?: "Error desconocido al crear copia"
                _uiState.update {
                    it.copy(
                        estaCreandoBackup = false,
                        mensajeAlerta = "Error: $errorMsg",
                        esError = true
                    )
                }
            }
        }
    }

    fun seleccionarParaRestaurar(backup: ArchivoBackupUi) {
        _uiState.update { it.copy(backupSeleccionadoParaRestaurar = backup) }
    }

    fun cancelarRestauracion() {
        _uiState.update { it.copy(backupSeleccionadoParaRestaurar = null) }
    }

    fun confirmarRestauracion() {
        val backup = _uiState.value.backupSeleccionadoParaRestaurar ?: return

        _uiState.update {
            it.copy(
                estaRestaurandoBackup = true,
                backupSeleccionadoParaRestaurar = null,
                mensajeAlerta = null
            )
        }

        viewModelScope.launch {
            val exito = withContext(Dispatchers.IO) {
                GestorBackup.restaurarBackup(contexto, backup.archivo)
            }

            if (exito) {
                _uiState.update {
                    it.copy(
                        estaRestaurandoBackup = false,
                        mensajeAlerta = "¡Base de datos restaurada con éxito desde ${backup.nombreVisual}!",
                        esError = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        estaRestaurandoBackup = false,
                        mensajeAlerta = "No se pudo restaurar la copia de seguridad seleccionada.",
                        esError = true
                    )
                }
            }
        }
    }

    fun compartirBackup(backup: ArchivoBackupUi) {
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
            if (exito) {
                _uiState.update {
                    it.copy(
                        mensajeAlerta = "Copia de seguridad eliminada con éxito.",
                        esError = false
                    )
                }
            }
            cargarBackups()
        }
    }

    fun limpiarAlerta() {
        _uiState.update { it.copy(mensajeAlerta = null) }
    }
}
