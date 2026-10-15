package com.typdevstudio.typos_movil.ui.licencia

import org.junit.Assert.assertEquals
import org.junit.Test

class PruebasMensajeWhatsAppRegistro {

    @Test
    fun llevaLosDatosDelNegocioYElIdDelTelefono() {
        val mensaje = LicenciaViewModel.mensajeWhatsAppRegistro("Colmado Juan", "809-555-0101", "Juan Guzmán", "B6AF-1AC3-0FB1-1B59")

        assertEquals(
            "Hola, registré mi negocio en TyPOS Móvil y quiero activar mi licencia.\n" +
                "Negocio: Colmado Juan\nTeléfono: 809-555-0101\nContacto: Juan Guzmán\nID del teléfono: B6AF-1AC3-0FB1-1B59",
            mensaje,
        )
    }

    @Test
    fun omiteLosDatosQueFaltan() {
        assertEquals(
            "Hola, registré mi negocio en TyPOS Móvil y quiero activar mi licencia.\nID del teléfono: B6AF",
            LicenciaViewModel.mensajeWhatsAppRegistro(null, " ", null, "B6AF"),
        )
    }
}
