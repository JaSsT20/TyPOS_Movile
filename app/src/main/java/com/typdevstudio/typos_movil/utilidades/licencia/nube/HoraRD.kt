package com.typdevstudio.typos_movil.utilidades.licencia.nube

/**
 * Fechas de licencia en hora de República Dominicana (UTC-4 fijo, sin horario de verano),
 * igual que el servidor. Aritmética pura sobre milisegundos epoch: `java.time` exige API 26
 * y no queremos depender de la zona horaria configurada en el teléfono.
 *
 * Un "día" se representa como días desde 1970-01-01 (época), en `Long`.
 */
object HoraRD {

    const val MS_POR_DIA = 86_400_000L
    const val DESFASE_MS = -4L * 60 * 60 * 1000

    private val PATRON_FECHA = Regex("""(\d{4})-(\d{2})-(\d{2})""")
    private val PATRON_INSTANTE = Regex("""(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})(?:\.(\d{1,7}))?Z""")

    /** Día civil en RD del instante dado. */
    fun diaEnRD(instanteMs: Long): Long = (instanteMs + DESFASE_MS).floorDiv(MS_POR_DIA)

    /**
     * Primer milisegundo del día siguiente en RD. Una fecha es válida "hasta el fin de ese día inclusive",
     * es decir, mientras `instante < finDelDiaRD(dia)`.
     */
    fun finDelDiaRD(dia: Long): Long = (dia + 1) * MS_POR_DIA - DESFASE_MS

    /**
     * Convierte `yyyy-MM-dd` en día epoch.
     * @throws IllegalArgumentException si el formato o la fecha no son válidos.
     */
    fun parsearFecha(texto: String): Long {
        val partes = requireNotNull(PATRON_FECHA.matchEntire(texto)) { "Fecha inválida: $texto" }.destructured
        val (anio, mes, dia) = partes
        return diasDesdeCivil(anio.toInt(), mes.toInt(), dia.toInt())
    }

    /**
     * Convierte `yyyy-MM-ddTHH:mm:ss[.fffffff]Z` (siempre UTC) en milisegundos epoch.
     * @throws IllegalArgumentException si el formato o la fecha no son válidos.
     */
    fun parsearInstanteUtc(texto: String): Long {
        val grupos = requireNotNull(PATRON_INSTANTE.matchEntire(texto)) { "Fecha y hora inválida: $texto" }.groupValues
        val dias = diasDesdeCivil(grupos[1].toInt(), grupos[2].toInt(), grupos[3].toInt())
        val hora = grupos[4].toInt()
        val minuto = grupos[5].toInt()
        val segundo = grupos[6].toInt()
        require(hora <= 23 && minuto <= 59 && segundo <= 59) { "Fecha y hora inválida: $texto" }
        val milisegundos = if (grupos[7].isEmpty()) 0 else grupos[7].padEnd(3, '0').take(3).toInt()
        return dias * MS_POR_DIA + ((hora * 60L + minuto) * 60 + segundo) * 1000 + milisegundos
    }

    /** Día epoch a `yyyy-MM-dd`. */
    fun formatearFecha(dia: Long): String {
        val (anio, mes, d) = civilDesdeDias(dia)
        return "%04d-%02d-%02d".format(java.util.Locale.ROOT, anio, mes, d)
    }

    /** Días desde la época para una fecha del calendario gregoriano (algoritmo de Howard Hinnant). */
    internal fun diasDesdeCivil(anio: Int, mes: Int, dia: Int): Long {
        require(mes in 1..12) { "Mes inválido: $mes" }
        require(dia in 1..diasDelMes(anio, mes)) { "Día inválido: $anio-$mes-$dia" }
        val a = (if (mes <= 2) anio - 1 else anio).toLong()
        val era = a.floorDiv(400L)
        val anioDeEra = a - era * 400
        val mesDesdeMarzo = (mes + 9) % 12
        val diaDelAnio = (153 * mesDesdeMarzo + 2) / 5 + dia - 1
        val diaDeEra = anioDeEra * 365 + anioDeEra / 4 - anioDeEra / 100 + diaDelAnio
        return era * 146_097 + diaDeEra - 719_468
    }

    internal fun civilDesdeDias(dias: Long): Triple<Int, Int, Int> {
        val z = dias + 719_468
        val era = z.floorDiv(146_097L)
        val diaDeEra = z - era * 146_097
        val anioDeEra = (diaDeEra - diaDeEra / 1460 + diaDeEra / 36_524 - diaDeEra / 146_096) / 365
        val diaDelAnio = diaDeEra - (365 * anioDeEra + anioDeEra / 4 - anioDeEra / 100)
        val mp = (5 * diaDelAnio + 2) / 153
        val dia = (diaDelAnio - (153 * mp + 2) / 5 + 1).toInt()
        val mes = (if (mp < 10) mp + 3 else mp - 9).toInt()
        val anio = (anioDeEra + era * 400 + if (mes <= 2) 1 else 0).toInt()
        return Triple(anio, mes, dia)
    }

    private fun diasDelMes(anio: Int, mes: Int): Int = when (mes) {
        2 -> if (anio % 4 == 0 && (anio % 100 != 0 || anio % 400 == 0)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }
}
