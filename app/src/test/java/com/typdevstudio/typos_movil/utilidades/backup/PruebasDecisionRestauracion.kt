package com.typdevstudio.typos_movil.utilidades.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Qué aviso ve el usuario al restaurar una copia de otro teléfono, y qué mensaje si la API lo rechaza. */
class PruebasDecisionRestauracion {

    @Test
    fun siEsteTelefonoYaEstaEnLaLicenciaEsLaConfirmacionNormal() {
        val variante = VarianteRestauracion.desdeConsulta(
            yaRegistrado = true, hayCupo = false, enUso = 3, maximo = 3, nombreOrigen = "Samsung A54 - Caja 1",
        )
        assertEquals(VarianteRestauracion.Normal, variante)
    }

    @Test
    fun conCupoSeAgregaEsteTelefonoYDiceCuantosQuedarian() {
        val variante = VarianteRestauracion.desdeConsulta(
            yaRegistrado = false, hayCupo = true, enUso = 1, maximo = 3, nombreOrigen = "Samsung A54 - Caja 1",
        )
        assertEquals(VarianteRestauracion.AgregarTelefono("Samsung A54 - Caja 1", quedariaEn = 2, maximo = 3), variante)
    }

    @Test
    fun sinCupoSeDesvinculaElTelefonoDeOrigen() {
        val variante = VarianteRestauracion.desdeConsulta(
            yaRegistrado = false, hayCupo = false, enUso = 1, maximo = 1, nombreOrigen = "Samsung A54 - Caja 1",
        )
        assertEquals(VarianteRestauracion.Desvincular("Samsung A54 - Caja 1", maximo = 1), variante)
    }

    @Test
    fun losRechazosDeLaTransferenciaUsanElCatalogo() {
        assertEquals(
            "No pudimos confirmar de dónde viene esta copia. Comuníquese con soporte.",
            CodigosBackup.mensajeDeTransferencia("TOKEN_ORIGEN_INVALIDO", "texto de la API"),
        )
        assertEquals(
            "Ya cambió de teléfono varias veces este mes. Comuníquese con soporte para continuar.",
            CodigosBackup.mensajeDeTransferencia("LIMITE_TRANSFERENCIAS", "texto de la API"),
        )
        assertEquals(
            CodigosBackup.mensaje(CodigosBackup.OTRA_LICENCIA),
            CodigosBackup.mensajeDeTransferencia("DISPOSITIVO_EN_OTRA_LICENCIA", "texto de la API"),
        )
    }

    @Test
    fun unCodigoDesconocidoMuestraElMensajeDeLaApi() {
        assertEquals("Mensaje nuevo de la API.", CodigosBackup.mensajeDeTransferencia("CODIGO_NUEVO", "Mensaje nuevo de la API."))
        assertEquals(
            CodigosBackup.mensaje(CodigosBackup.RESTAURACION_FALLIDA),
            CodigosBackup.mensajeDeTransferencia("CODIGO_NUEVO", ""),
        )
    }

    @Test
    fun ningunMensajeDeTransferenciaTuteaNiUsaPalabrasTecnicas() {
        val codigos = listOf(
            "TOKEN_ORIGEN_INVALIDO", "LIMITE_TRANSFERENCIAS", "DISPOSITIVO_EN_OTRA_LICENCIA", "LIMITE_DISPOSITIVOS",
            "LICENCIA_VENCIDA", "LICENCIA_SUSPENDIDA", "LICENCIA_NO_ENCONTRADA", "DISPOSITIVO_REVOCADO", "ERROR_INTERNO",
        )
        val prohibidas = Regex("\\b(tu|tus|te|puedes|tienes|comunícate|inténtalo|backup|token|api)\\b", RegexOption.IGNORE_CASE)
        codigos.forEach { codigo ->
            val texto = CodigosBackup.mensajeDeTransferencia(codigo, "")
            assertTrue(texto.isNotBlank())
            assertFalse("$codigo: $texto", prohibidas.containsMatchIn(texto))
        }
    }

    @Test
    fun elCodigoDeSoporteUsaElPrefijoYLasPrimerasCuatroLetras() {
        assertEquals("TR-4F2A", CodigosBackup.codigoSoporte("4f2a9c10-0000-0000-0000-000000000000", "TR"))
        assertEquals("BK-4F2A", CodigosBackup.codigoSoporte("4f2a9c10-0000-0000-0000-000000000000"))
    }
}
