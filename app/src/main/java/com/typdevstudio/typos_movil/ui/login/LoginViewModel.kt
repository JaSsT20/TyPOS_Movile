package com.typdevstudio.typos_movil.ui.login

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.typdevstudio.typos_movil.datos.local.AppBaseDatos
import com.typdevstudio.typos_movil.datos.local.GestorSesion
import com.typdevstudio.typos_movil.datos.local.entidades.ConfiguracionNegocioEntidad
import com.typdevstudio.typos_movil.datos.local.entidades.UsuarioEntidad
import com.typdevstudio.typos_movil.datos.repositorio.ResultadoAutenticacion
import com.typdevstudio.typos_movil.datos.repositorio.ResultadoRegistroUsuario
import com.typdevstudio.typos_movil.datos.repositorio.UsuarioRepositorio
import com.typdevstudio.typos_movil.ui.licencia.LicenciaViewModel
import com.typdevstudio.typos_movil.utilidades.licencia.LicenciaGuard
import com.typdevstudio.typos_movil.utilidades.licencia.SincronizacionUsuarios
import com.typdevstudio.typos_movil.utilidades.licencia.nube.ArchivoSeguro
import com.typdevstudio.typos_movil.utilidades.licencia.nube.DatosRegistro
import com.typdevstudio.typos_movil.utilidades.licencia.nube.PreferenciasCifradas
import com.typdevstudio.typos_movil.utilidades.licencia.nube.ResultadoLicencia
import com.typdevstudio.typos_movil.utilidades.licencia.nube.ResultadoRegistro
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val usuario: String = "",
    val clave: String = "",
    val recordar: Boolean = false,
    val errorUsuario: String? = null,
    val errorClave: String? = null,
    val mensajeErrorGeneral: String? = null,
    val mensajeExito: String? = null,
    val estaCargando: Boolean = false,
    val loginExitoso: Boolean = false
)

/** Qué formulario de bienvenida está abierto cuando el teléfono no tiene usuarios. */
enum class FormularioBienvenida { REGISTRAR_NEGOCIO, ACTIVAR_LICENCIA }

/** Primer uso del teléfono (sin usuarios): registrar el negocio o traer los usuarios de una licencia. */
data class BienvenidaUiState(
    val formulario: FormularioBienvenida? = null,
    // Registro del negocio (los mismos datos que salen en su factura) y su contacto.
    val nombreNegocio: String = "",
    val telefonoNegocio: String = "",
    val emailNegocio: String = "",
    val rncNegocio: String = "",
    val direccionNegocio: String = "",
    val sloganNegocio: String = "",
    val contactoNombre: String = "",
    val contactoTelefono: String = "",
    // Administrador del negocio.
    val nombreUsuario: String = "",
    val nombreCompleto: String = "",
    val clave: String = "",
    val confirmarClave: String = "",
    val claveLicencia: String = "",
    val nombreTelefono: String = "",
    val estaGuardando: Boolean = false,
    val error: String? = null,
    /** Aviso tras activar la licencia (por ejemplo, que todavía no tiene usuarios). */
    val aviso: String? = null,
    /** El registro se envió: se muestra el aviso con "Notificar por WhatsApp" y "Entrar". */
    val registroEnviado: Boolean = false,
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    val repositorio: UsuarioRepositorio
    private val prefs = application.getSharedPreferences("typos_credenciales_pref", Context.MODE_PRIVATE)

    /** Contraseña recordada, cifrada con el Keystore (mismo archivo protegido que la licencia, fuera de los respaldos). */
    private val credencialesCifradas: ArchivoSeguro = PreferenciasCifradas(application)

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** `true` si el teléfono no tiene ningún usuario (primer uso); `null` mientras se consulta. */
    val sinUsuarios: StateFlow<Boolean?>

    private val _bienvenida = MutableStateFlow(BienvenidaUiState())
    val bienvenida: StateFlow<BienvenidaUiState> = _bienvenida.asStateFlow()

    init {
        val baseDatos = AppBaseDatos.obtenerBaseDatos(application)
        repositorio = UsuarioRepositorio(baseDatos.usuarioDao())
        sinUsuarios = baseDatos.usuarioDao().observarTodos()
            .map { it.isEmpty() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
        cargarCredencialesGuardadas()
        viewModelScope.launch { repositorio.convertirClavesAntiguas() }
    }

    /**
     * "Recordar" guarda el usuario y la contraseña para no escribirlos cada vez. La contraseña nunca queda en texto
     * plano: se cifra con una llave del chip de seguridad del teléfono (Keystore) y no viaja en los respaldos.
     * Las versiones anteriores la guardaban en texto plano; si quedó alguna, se borra aquí.
     */
    private fun cargarCredencialesGuardadas() {
        if (prefs.contains(CLAVE_ANTIGUA)) {
            prefs.edit().remove(CLAVE_ANTIGUA).commit()
        }
        _uiState.value = estadoInicial()
    }

    private fun estadoInicial(): LoginUiState =
        if (prefs.getBoolean("recordar", false)) {
            LoginUiState(
                usuario = prefs.getString("usuario", "") ?: "",
                clave = credencialesCifradas.leer(CLAVE_RECORDADA).orEmpty(),
                recordar = true,
            )
        } else {
            LoginUiState()
        }

    fun onUsuarioCambiado(nuevoUsuario: String) {
        _uiState.update {
            it.copy(
                usuario = nuevoUsuario,
                errorUsuario = null,
                mensajeErrorGeneral = null,
                mensajeExito = null
            )
        }
    }

    fun onClaveCambiada(nuevaClave: String) {
        _uiState.update {
            it.copy(
                clave = nuevaClave,
                errorClave = null,
                mensajeErrorGeneral = null,
                mensajeExito = null
            )
        }
    }

    fun onRecordarCambiado(nuevoRecordar: Boolean) {
        _uiState.update {
            it.copy(recordar = nuevoRecordar)
        }
        if (!nuevoRecordar) olvidarCredenciales()
    }

    private fun olvidarCredenciales() {
        prefs.edit()
            .putBoolean("recordar", false)
            .remove("usuario")
            .apply()
        credencialesCifradas.escribir(mapOf(CLAVE_RECORDADA to null))
    }

    fun onUsuarioRegistradoConExito(nombreUsuario: String) {
        _uiState.update {
            it.copy(
                usuario = nombreUsuario,
                clave = "",
                errorUsuario = null,
                errorClave = null,
                mensajeErrorGeneral = null,
                mensajeExito = "¡Usuario '$nombreUsuario' activado con éxito! Ingrese su contraseña para entrar."
            )
        }
    }

    fun iniciarSesion() {
        val estadoActual = _uiState.value
        val usuarioTrim = estadoActual.usuario.trim()
        val claveTrim = estadoActual.clave.trim()

        var hayError = false
        var errorUsuario: String? = null
        var errorClave: String? = null

        if (usuarioTrim.isEmpty()) {
            errorUsuario = "Ingrese su usuario"
            hayError = true
        }

        if (claveTrim.isEmpty()) {
            errorClave = "Ingrese su contraseña o PIN"
            hayError = true
        }

        if (hayError) {
            _uiState.update {
                it.copy(
                    errorUsuario = errorUsuario,
                    errorClave = errorClave
                )
            }
            return
        }

        _uiState.update { it.copy(estaCargando = true, mensajeErrorGeneral = null, mensajeExito = null) }

        viewModelScope.launch {
            // La licencia no bloquea el inicio de sesión: en modo restringido se entra para consultar
            // la información (LicenciaGuard decide qué acciones se permiten).
            val resultado = repositorio.autenticar(usuarioTrim, claveTrim)
            when (resultado) {
                is ResultadoAutenticacion.Exito -> {
                    if (estadoActual.recordar) {
                        prefs.edit()
                            .putBoolean("recordar", true)
                            .putString("usuario", usuarioTrim)
                            .apply()
                        credencialesCifradas.escribir(mapOf(CLAVE_RECORDADA to claveTrim))
                    } else {
                        olvidarCredenciales()
                    }

                    GestorSesion.iniciarSesion(resultado.usuario)
                    _uiState.update {
                        it.copy(
                            estaCargando = false,
                            loginExitoso = true
                        )
                    }
                }
                is ResultadoAutenticacion.Error -> {
                    _uiState.update {
                        it.copy(
                            estaCargando = false,
                            mensajeErrorGeneral = resultado.mensaje
                        )
                    }
                }
            }
        }
    }

    // ---------- Primer uso: teléfono sin usuarios ----------

    fun abrirBienvenida(formulario: FormularioBienvenida) = _bienvenida.update {
        BienvenidaUiState(
            formulario = formulario,
            nombreTelefono = LicenciaViewModel.nombreTelefonoPorDefecto(),
            aviso = it.aviso,
        )
    }

    fun cerrarBienvenida() = _bienvenida.update { if (it.estaGuardando) it else it.copy(formulario = null, error = null) }

    fun editarBienvenida(cambio: (BienvenidaUiState) -> BienvenidaUiState) =
        _bienvenida.update { cambio(it).copy(error = null) }

    /**
     * Registra el negocio para que el proveedor le asigne una licencia (requiere internet) y crea su administrador
     * en el teléfono. Mientras el proveedor no asigna la licencia, la app queda en modo consulta; cuando la asigna,
     * el teléfono la recibe solo y el administrador sube a la licencia.
     */
    fun registrarNegocio() {
        val f = _bienvenida.value
        val error = when {
            f.nombreNegocio.isBlank() -> "Escriba el nombre de su negocio."
            f.telefonoNegocio.isBlank() -> "Escriba el teléfono del negocio."
            f.emailNegocio.isNotBlank() && !FORMATO_CORREO.matches(f.emailNegocio.trim()) -> "El correo no es válido."
            !SincronizacionUsuarios.nombreValido(f.nombreUsuario) ->
                "El usuario debe tener de 1 a 50 letras, números o . _ @ - (sin espacios)."
            f.nombreCompleto.isBlank() -> "Escriba su nombre completo."
            f.clave.trim().length < 4 -> "La contraseña debe tener al menos 4 caracteres."
            f.clave != f.confirmarClave -> "Las contraseñas no coinciden."
            else -> null
        }
        if (error != null) {
            _bienvenida.update { it.copy(error = error) }
            return
        }
        _bienvenida.update { it.copy(estaGuardando = true) }
        viewModelScope.launch {
            val datos = DatosRegistro(
                nombreNegocio = f.nombreNegocio.trim(),
                telefono = f.telefonoNegocio.trim(),
                email = f.emailNegocio.trim().ifBlank { null },
                rnc = f.rncNegocio.trim().ifBlank { null },
                direccion = f.direccionNegocio.trim().ifBlank { null },
                slogan = f.sloganNegocio.trim().ifBlank { null },
                contactoNombre = f.contactoNombre.trim().ifBlank { f.nombreCompleto.trim() },
                contactoTelefono = f.contactoTelefono.trim().ifBlank { null },
                adminUsuario = SincronizacionUsuarios.normalizarNombre(f.nombreUsuario),
                adminNombre = f.nombreCompleto.trim(),
            )
            val falla = when (val resultado = LicenciaGuard.registrar(datos)) {
                is ResultadoRegistro.Pendiente -> {
                    guardarDatosDelNegocio(datos)
                    crearAdministradorLocal(f) ?: run {
                        _bienvenida.update {
                            it.copy(estaGuardando = false, formulario = null, registroEnviado = true)
                        }
                        null
                    }
                }
                // El teléfono ya tenía licencia asignada (por ejemplo, tras reinstalar): queda activado de una vez.
                is ResultadoRegistro.Asignado -> when (val licencia = resultado.licencia) {
                    is ResultadoLicencia.Exito -> {
                        guardarDatosDelNegocio(datos)
                        if (repositorio.contarUsuarios() > 0) {
                            _bienvenida.value = BienvenidaUiState()
                            _uiState.update { it.copy(mensajeExito = "¡Su licencia ya estaba activa! Inicie sesión con su usuario.") }
                            null
                        } else {
                            crearAdministradorLocal(f) ?: run { entrarComo(f.nombreUsuario, f.clave); null }
                        }
                    }
                    else -> LicenciaViewModel.mensajeDe(licencia, "")
                }
                is ResultadoRegistro.Rechazado -> resultado.mensaje
                is ResultadoRegistro.Rechazada -> resultado.mensaje
                is ResultadoRegistro.SinConexion -> "Para registrarse necesita internet. ${resultado.mensaje}"
                ResultadoRegistro.SinRegistro -> "No se pudo enviar el registro. Inténtelo de nuevo."
            }
            if (falla != null) _bienvenida.update { it.copy(estaGuardando = false, error = falla) }
        }
    }

    /** Tras el aviso de "registro enviado", entra con el administrador que se acaba de crear. */
    fun entrarTrasRegistro() {
        val f = _bienvenida.value
        viewModelScope.launch { entrarComo(f.nombreUsuario, f.clave) }
    }

    /** Mensaje para avisar por WhatsApp al proveedor que el negocio se registró. */
    suspend fun mensajeWhatsAppRegistro(): String {
        val f = _bienvenida.value
        return LicenciaViewModel.mensajeWhatsAppRegistro(
            f.nombreNegocio.trim(), f.telefonoNegocio.trim(), f.contactoNombre.trim().ifBlank { f.nombreCompleto.trim() },
            LicenciaGuard.identificadorDispositivo(),
        )
    }

    /** `null` si se creó; si no, el mensaje de error. Sin licencia, el administrador queda solo en el teléfono. */
    private suspend fun crearAdministradorLocal(f: BienvenidaUiState): String? =
        (repositorio.registrarNuevoUsuario(f.nombreUsuario, f.nombreCompleto, f.clave, "ADMINISTRADOR")
            as? ResultadoRegistroUsuario.Error)?.mensaje

    private suspend fun entrarComo(nombreUsuario: String, clave: String) {
        when (val acceso = repositorio.autenticar(nombreUsuario, clave)) {
            is ResultadoAutenticacion.Exito -> {
                _bienvenida.value = BienvenidaUiState()
                GestorSesion.iniciarSesion(acceso.usuario)
                _uiState.update { it.copy(loginExitoso = true) }
            }
            is ResultadoAutenticacion.Error -> {
                // No debería pasar: se deja en el inicio de sesión normal con el usuario escrito.
                _bienvenida.value = BienvenidaUiState()
                _uiState.update { it.copy(usuario = nombreUsuario.trim().lowercase(), mensajeErrorGeneral = acceso.mensaje) }
            }
        }
    }

    /** Los datos del registro son los de la factura: se guardan en la configuración del negocio del teléfono. */
    private suspend fun guardarDatosDelNegocio(datos: DatosRegistro) {
        val dao = AppBaseDatos.obtenerBaseDatos(getApplication()).configuracionNegocioDao()
        val actual = dao.obtenerConfiguracionDirecta() ?: ConfiguracionNegocioEntidad()
        dao.guardarConfiguracion(
            actual.copy(
                nombreNegocio = datos.nombreNegocio,
                telefono = datos.telefono,
                rncCedula = datos.rnc ?: actual.rncCedula,
                direccion = datos.direccion ?: actual.direccion,
                slogan = datos.slogan ?: actual.slogan,
            )
        )
    }

    /** Activa la licencia en este teléfono y trae sus usuarios para iniciar sesión con uno de ellos. */
    fun activarLicencia() {
        val f = _bienvenida.value
        val clave = LicenciaViewModel.claveNormalizada(f.claveLicencia)
        if (clave == null) {
            _bienvenida.update { it.copy(error = "Ingrese la clave completa: 12 letras o números (XXXX-XXXX-XXXX).") }
            return
        }
        _bienvenida.update { it.copy(estaGuardando = true, claveLicencia = clave) }
        viewModelScope.launch {
            val resultado = LicenciaGuard.activar(clave, f.nombreTelefono)
            if (resultado !is ResultadoLicencia.Exito) {
                _bienvenida.update { it.copy(estaGuardando = false, error = LicenciaViewModel.mensajeDe(resultado, "")) }
                return@launch
            }
            if (repositorio.contarUsuarios() > 0) {
                // Llegaron los usuarios de la licencia: la pantalla pasa sola al inicio de sesión.
                _bienvenida.value = BienvenidaUiState()
                _uiState.update { it.copy(mensajeExito = "¡Licencia activada! Inicie sesión con su usuario.") }
            } else {
                _bienvenida.value = BienvenidaUiState(
                    aviso = "¡Licencia activada! Su licencia todavía no tiene usuarios: cree el administrador. " +
                        "Si ya tenía usuarios, revise su conexión y vuelva a intentarlo."
                )
            }
        }
    }

    fun reiniciarEstado() {
        _uiState.value = estadoInicial()
    }

    private companion object {
        /** Validación sencilla de correo (opcional); la API vuelve a validarlo. */
        val FORMATO_CORREO = Regex("""^[^@\s]+@[^@\s]+\.[^@\s]+$""")

        /** Preferencia donde las versiones anteriores guardaban la contraseña en texto plano. */
        const val CLAVE_ANTIGUA = "clave"

        /** Campo cifrado con la contraseña recordada. */
        const val CLAVE_RECORDADA = "login_clave_recordada"
    }
}
