package com.typdevstudio.typos_movil.utilidades.licencia

import com.typdevstudio.typos_movil.ui.licencia.LicenciaViewModel
import com.typdevstudio.typos_movil.ui.licencia.TextosLicencia
import com.typdevstudio.typos_movil.utilidades.licencia.nube.EstadoLicencia
import com.typdevstudio.typos_movil.utilidades.licencia.nube.FabricaTokens
import com.typdevstudio.typos_movil.utilidades.licencia.nube.HoraRD
import com.typdevstudio.typos_movil.utilidades.licencia.nube.MotivoRestriccion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PruebasSituacionLicencia {

    private val payload = FabricaTokens.payload()
    private val antiguaVigente = LicenciaAntigua(vigente = true, venceMs = 1_800_000_000_000L)
    private val antiguaVencida = LicenciaAntigua(vigente = false, venceMs = 1_700_000_000_000L)

    @Test
    fun sinLicenciaNuevaYConLaAntiguaVigenteSigueVendiendo() {
        val situacion = SituacionLicencia.combinar(EstadoLicencia.SinActivar, antiguaVigente, usarAntigua = true)
        assertTrue(situacion.usaLicenciaAntigua)
        assertTrue(situacion.permiteEmitir)
        assertNull(situacion.motivoRestriccion)
    }

    @Test
    fun conLaBanderaApagadaLaAntiguaNoCuenta() {
        val situacion = SituacionLicencia.combinar(EstadoLicencia.SinActivar, antiguaVigente, usarAntigua = false)
        assertFalse(situacion.permiteEmitir)
        assertEquals(MotivoRestriccion.SIN_ACTIVAR, situacion.motivoRestriccion)
    }

    @Test
    fun laAntiguaVencidaOAusenteNoPermiteEmitir() {
        for (antigua in listOf(antiguaVencida, null)) {
            val situacion = SituacionLicencia.combinar(EstadoLicencia.SinActivar, antigua, usarAntigua = true)
            assertFalse(situacion.permiteEmitir)
            assertEquals(MotivoRestriccion.SIN_ACTIVAR, situacion.motivoRestriccion)
        }
        assertTrue(SituacionLicencia.combinar(EstadoLicencia.SinActivar, antiguaVencida, true).mensajeRestriccion().contains("anterior venció"))
        assertTrue(SituacionLicencia.combinar(EstadoLicencia.SinActivar, null, true).mensajeRestriccion().contains("no tiene una licencia"))
    }

    @Test
    fun laLicenciaNuevaMandaSobreLaAntigua() {
        // Un bloqueo del servidor o un token expirado no se salvan con la licencia antigua.
        for (motivo in MotivoRestriccion.entries) {
            val situacion = SituacionLicencia.combinar(EstadoLicencia.Restringida(motivo, payload), antiguaVigente, usarAntigua = true)
            assertFalse(motivo.name, situacion.usaLicenciaAntigua)
            assertFalse(motivo.name, situacion.permiteEmitir)
            assertEquals(motivo, situacion.motivoRestriccion)
            assertTrue(situacion.mensajeRestriccion().isNotBlank())
            assertTrue(situacion.mensajeAccionBloqueada().startsWith("Modo solo consulta"))
        }
        val activa = SituacionLicencia.combinar(EstadoLicencia.Activa(payload), antiguaVencida, usarAntigua = true)
        assertTrue(activa.permiteEmitir)
        assertFalse(activa.usaLicenciaAntigua)
        assertEquals(payload, activa.payload)
    }

    @Test
    fun cortesiaYPorValidarPermitenEmitir() {
        assertTrue(SituacionLicencia.combinar(EstadoLicencia.EnCortesia(payload, 2), null, true).permiteEmitir)
        assertTrue(SituacionLicencia.combinar(EstadoLicencia.ActivaPorValidar(payload), null, true).permiteEmitir)
    }

    @Test
    fun laSituacionInicialNoEstaVerificada() {
        assertFalse(SituacionLicencia.INICIAL.verificada)
    }

    @Test
    fun elMensajeDeLaApiTienePrioridad() {
        val situacion = SituacionLicencia.combinar(EstadoLicencia.Restringida(MotivoRestriccion.SUSPENDIDA, payload), null, true)
            .copy(mensajeServidor = "La licencia está suspendida. Comuníquese con su proveedor.")
        assertEquals("La licencia está suspendida. Comuníquese con su proveedor.", situacion.mensajeRestriccion())
        assertTrue(situacion.mensajeAccionBloqueada().contains("Comuníquese con su proveedor"))
    }

    @Test
    fun avisaCincoDiasAntesYElMismoDia() {
        val vence = HoraRD.parsearFecha("2026-12-31")
        fun aviso(diasAntes: Long, mostrados: Set<String> = emptySet()) =
            CalculadoraAvisoVencimiento.calcular(vence, vence - diasAntes, mostrados)

        assertNull(aviso(6))
        assertEquals(TipoAvisoVencimiento.PROXIMO_A_VENCER, aviso(5)!!.tipo)
        assertEquals(5, aviso(5)!!.diasRestantes)
        assertEquals(TipoAvisoVencimiento.PROXIMO_A_VENCER, aviso(1)!!.tipo)
        assertEquals(TipoAvisoVencimiento.VENCE_HOY, aviso(0)!!.tipo)
        assertNull(aviso(-1)) // ya venció: lo cubre el aviso de cortesía
        assertNull(CalculadoraAvisoVencimiento.calcular(null, vence, emptySet()))

        // Cada aviso se muestra una sola vez por fecha de vencimiento.
        val proximo = aviso(5)!!
        assertNull(aviso(3, setOf(proximo.clave)))
        assertEquals(TipoAvisoVencimiento.VENCE_HOY, aviso(0, setOf(proximo.clave))!!.tipo)
        assertNull(aviso(0, setOf(proximo.clave, aviso(0)!!.clave)))
        // Si renueva, la fecha cambia y se vuelve a avisar.
        assertEquals(
            TipoAvisoVencimiento.PROXIMO_A_VENCER,
            CalculadoraAvisoVencimiento.calcular(vence + 365, vence + 362, setOf(proximo.clave))!!.tipo
        )
    }

    @Test
    fun laFechaParaAvisarEsLaDeLaLicenciaQueAutoriza() {
        assertEquals(payload.vence, SituacionLicencia.combinar(EstadoLicencia.Activa(payload), null, true).venceDia)
        assertEquals(payload.vence, SituacionLicencia.combinar(EstadoLicencia.ActivaPorValidar(payload), null, true).venceDia)
        // Licencia antigua: vence a medianoche del 2026-12-31 en RD, así que el último día es el 31.
        val finDia31 = HoraRD.finDelDiaRD(HoraRD.parsearFecha("2026-12-31"))
        val transicion = SituacionLicencia.combinar(EstadoLicencia.SinActivar, LicenciaAntigua(true, finDia31), true)
        assertEquals(HoraRD.parsearFecha("2026-12-31"), transicion.venceDia)
        // En modo restringido no hay aviso de vencimiento (ya se muestra el bloqueo).
        assertNull(SituacionLicencia.combinar(EstadoLicencia.Restringida(MotivoRestriccion.VENCIDA, payload), null, true).venceDia)
        assertNull(SituacionLicencia.INICIAL.venceDia)
    }

    @Test
    fun losModulosSalenDelPlanSalvoConLaLicenciaAntigua() {
        val soloFacturacion = payload.copy(modulos = listOf(ModulosLicencia.FACTURACION))
        val conPlan = SituacionLicencia.combinar(EstadoLicencia.Activa(soloFacturacion), null, true)
        assertTrue(conPlan.moduloIncluido(ModulosLicencia.FACTURACION))
        assertFalse(conPlan.moduloIncluido(ModulosLicencia.INVENTARIO))
        assertFalse(conPlan.moduloIncluido(ModulosLicencia.CXC))

        // Transición: con la licencia antigua todo sigue como hoy.
        val transicion = SituacionLicencia.combinar(EstadoLicencia.SinActivar, antiguaVigente, true)
        assertTrue(transicion.moduloIncluido(ModulosLicencia.INVENTARIO))
        // Sin token no hay plan que consultar: lo que no se puede hacer lo decide el modo restringido.
        assertTrue(SituacionLicencia.combinar(EstadoLicencia.SinActivar, null, true).moduloIncluido(ModulosLicencia.INVENTARIO))

        val mensaje = ModulosLicencia.mensajeNoIncluido("Inventario")
        assertTrue(mensaje.startsWith("El módulo Inventario no está incluido en su plan."))
        assertTrue(mensaje.contains("+1 (849) 359-4927"))
    }

    @Test
    fun elLimiteDeUsuariosSoloBloqueaCrearNuevos() {
        val plan = SituacionLicencia.combinar(EstadoLicencia.Activa(payload.copy(maxUsuarios = 2)), null, true)
        assertEquals(2, plan.maxUsuarios)
        assertNull(plan.motivoParaNoCrearUsuario(0))
        assertNull(plan.motivoParaNoCrearUsuario(1))
        assertTrue(plan.motivoParaNoCrearUsuario(2)!!.startsWith("Su plan permite hasta 2 usuarios y ya tiene 2."))
        // Ya pasado del límite (bajó de plan): tampoco se crean más, pero nada se borra.
        assertTrue(plan.motivoParaNoCrearUsuario(5)!!.contains("ya tiene 5"))

        val unUsuario = SituacionLicencia.combinar(EstadoLicencia.Activa(payload.copy(maxUsuarios = 1)), null, true)
        assertTrue(unUsuario.motivoParaNoCrearUsuario(1)!!.startsWith("Su plan permite hasta 1 usuario y"))

        // Licencia antigua: sin límite, como hoy.
        val transicion = SituacionLicencia.combinar(EstadoLicencia.SinActivar, antiguaVigente, true)
        assertNull(transicion.maxUsuarios)
        assertNull(transicion.motivoParaNoCrearUsuario(50))

        // Modo restringido: no se crean usuarios, salvo el primero.
        val restringida = SituacionLicencia.combinar(EstadoLicencia.SinActivar, null, true)
        assertNull(restringida.motivoParaNoCrearUsuario(0))
        assertTrue(restringida.motivoParaNoCrearUsuario(1)!!.startsWith("Modo solo consulta"))

        assertEquals("3 de 10 permitidos", TextosLicencia.usoUsuarios(3, 10))
        assertEquals("10 permitidos", TextosLicencia.usoUsuarios(null, 10))
    }

    @Test
    fun laClaveSeNormalizaConOSinGuiones() {
        assertEquals("6F3M-DRBX-ANRU", LicenciaViewModel.claveNormalizada("6f3mdrbxanru"))
        assertEquals("6F3M-DRBX-ANRU", LicenciaViewModel.claveNormalizada(" 6F3M-drbx anru "))
        assertNull(LicenciaViewModel.claveNormalizada("6F3M-DRBX-ANR"))
        assertNull(LicenciaViewModel.claveNormalizada("6F3M-DRBX-ANRUX"))
    }

    @Test
    fun textosDeLaPantallaDeLicencia() {
        assertEquals("31/12/2026", TextosLicencia.fechaDesdeDia(HoraRD.parsearFecha("2026-12-31")))
        assertEquals("—", TextosLicencia.fechaDesdeMs(null))
        assertTrue(TextosLicencia.textoCortesia(0).startsWith("Hoy es el último día"))
        assertTrue(TextosLicencia.textoCortesia(1).startsWith("Tiene 1 día "))
        assertTrue(TextosLicencia.textoCortesia(3).startsWith("Tiene 3 días"))
        assertEquals("Facturación", TextosLicencia.nombreModulo("facturacion"))
        assertEquals("Cuentas por cobrar", TextosLicencia.nombreModulo("cxc"))
        assertEquals("Reportes", TextosLicencia.nombreModulo("reportes"))
        assertEquals("Básico", TextosLicencia.nombrePlan("basico"))
        assertEquals("Pro", TextosLicencia.nombrePlan("pro"))
        assertEquals("Empresarial plus", TextosLicencia.nombrePlan("empresarial_plus"))
    }
}
