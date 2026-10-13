package com.typdevstudio.typos_movil.utilidades.licencia.nube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PruebasAlmacenLicencia {

    private val archivo = ArchivoEnMemoria()
    private val almacen = AlmacenLicencia(archivo)

    @Test
    fun vacioDevuelveTodoNulo() {
        assertEquals(DatosLicenciaGuardados(null, null, null, null, null), almacen.leer())
        assertNull(almacen.leerDeviceHash())
    }

    @Test
    fun laValidacionExitosaQuitaElBloqueoYReiniciaElReloj() {
        almacen.registrarFechaVista(9_000L)
        almacen.guardarBloqueoServidor(MotivoRestriccion.SUSPENDIDA)

        almacen.guardarValidacionExitosa("K7QX-9M2D-PL4A", "PAYLOAD.FIRMA", 5_000L)

        assertEquals(DatosLicenciaGuardados("K7QX-9M2D-PL4A", "PAYLOAD.FIRMA", 5_000L, null, 5_000L), almacen.leer())
    }

    @Test
    fun guardaElBloqueoDelServidorSinTocarElToken() {
        almacen.guardarValidacionExitosa("K7QX-9M2D-PL4A", "PAYLOAD.FIRMA", 5_000L)
        almacen.guardarBloqueoServidor(MotivoRestriccion.REVOCADO)

        val datos = almacen.leer()
        assertEquals(MotivoRestriccion.REVOCADO, datos.bloqueoServidor)
        assertEquals("PAYLOAD.FIRMA", datos.token)
    }

    @Test
    fun laFechaVistaNuncaRetrocede() {
        almacen.registrarFechaVista(2_000L)
        almacen.registrarFechaVista(1_000L)
        assertEquals(2_000L, almacen.leer().ultimaFechaVistaMs)
        almacen.registrarFechaVista(3_000L)
        assertEquals(3_000L, almacen.leer().ultimaFechaVistaMs)
    }

    @Test
    fun borrarLicenciaConservaLoQueEsDelTelefono() {
        almacen.guardarDeviceHash("ab".repeat(32))
        almacen.guardarValidacionExitosa("K7QX-9M2D-PL4A", "PAYLOAD.FIRMA", 5_000L)
        almacen.guardarBloqueoServidor(MotivoRestriccion.VENCIDA)

        almacen.borrarLicencia()

        assertEquals(DatosLicenciaGuardados(null, null, null, null, 5_000L), almacen.leer())
        assertEquals("ab".repeat(32), almacen.leerDeviceHash())
    }

    @Test
    fun valoresIlegiblesSeTratanComoAusentes() {
        archivo.valores["ultima_validacion"] = "no-es-numero"
        archivo.valores["bloqueo_servidor"] = "MOTIVO_DESCONOCIDO"
        val datos = almacen.leer()
        assertNull(datos.ultimaValidacionMs)
        assertNull(datos.bloqueoServidor)
    }
}
