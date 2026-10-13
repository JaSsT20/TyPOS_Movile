package com.typdevstudio.typos_movil.utilidades.licencia.nube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class PruebasHoraRD {

    @Test
    fun convierteFechasIgualQueJavaTime() {
        var fecha = LocalDate.of(1899, 12, 25)
        while (fecha.isBefore(LocalDate.of(2101, 1, 10))) {
            val dia = HoraRD.parsearFecha(fecha.toString())
            assertEquals(fecha.toEpochDay(), dia)
            assertEquals(fecha.toString(), HoraRD.formatearFecha(dia))
            fecha = fecha.plusDays(13)
        }
        assertEquals(LocalDate.of(2024, 2, 29).toEpochDay(), HoraRD.parsearFecha("2024-02-29"))
        assertEquals(LocalDate.of(2000, 2, 29).toEpochDay(), HoraRD.parsearFecha("2000-02-29"))
    }

    @Test
    fun rechazaFechasInvalidas() {
        for (texto in listOf("2026-02-29", "1900-02-29", "2026-13-01", "2026-04-31", "2026-00-10", "2026-1-01", "2026-10-13T00:00:00Z", "")) {
            assertThrows(texto, IllegalArgumentException::class.java) { HoraRD.parsearFecha(texto) }
        }
    }

    @Test
    fun convierteInstantesUtcIgualQueJavaTime() {
        for (texto in listOf("1970-01-01T00:00:00Z", "2026-10-27T22:00:00Z", "2026-12-31T23:59:59Z", "1969-12-31T23:59:59Z")) {
            assertEquals(texto, Instant.parse(texto).toEpochMilli(), HoraRD.parsearInstanteUtc(texto))
        }
        assertEquals(Instant.parse("2026-10-27T22:00:00.500Z").toEpochMilli(), HoraRD.parsearInstanteUtc("2026-10-27T22:00:00.5Z"))
        assertEquals(Instant.parse("2026-10-27T22:00:00.123Z").toEpochMilli(), HoraRD.parsearInstanteUtc("2026-10-27T22:00:00.1234567Z"))
    }

    @Test
    fun rechazaInstantesInvalidos() {
        for (texto in listOf("2026-10-27T22:00:00", "2026-10-27T22:00:00+00:00", "2026-10-27T24:00:00Z", "2026-10-27T22:60:00Z", "2026-10-27 22:00:00Z", "2026-02-30T00:00:00Z")) {
            assertThrows(texto, IllegalArgumentException::class.java) { HoraRD.parsearInstanteUtc(texto) }
        }
    }

    @Test
    fun elDiaEnRDCambiaALas4DeLaMananaUtc() {
        val dia12 = HoraRD.parsearFecha("2026-10-12")
        val dia13 = HoraRD.parsearFecha("2026-10-13")
        assertEquals(dia12, HoraRD.diaEnRD(Instant.parse("2026-10-13T03:59:59.999Z").toEpochMilli()))
        assertEquals(dia13, HoraRD.diaEnRD(Instant.parse("2026-10-13T04:00:00Z").toEpochMilli()))
        assertEquals(LocalDate.of(1969, 12, 31).toEpochDay(), HoraRD.diaEnRD(Instant.parse("1970-01-01T03:00:00Z").toEpochMilli()))
    }

    @Test
    fun elFinDelDiaRDEsMedianocheDeRD() {
        assertEquals(
            Instant.parse("2027-01-01T04:00:00Z").toEpochMilli(),
            HoraRD.finDelDiaRD(HoraRD.parsearFecha("2026-12-31")),
        )
    }
}
