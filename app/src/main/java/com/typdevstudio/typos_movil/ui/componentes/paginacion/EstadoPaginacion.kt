package com.typdevstudio.typos_movil.ui.componentes.paginacion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Gestor de estado ligero y reactivo para paginación de listas en Compose.
 * Diseñado para bajo consumo de memoria RAM en dispositivos gama baja,
 * garantizando que sólo un subconjunto acotado de elementos se mantenga
 * en memoria y composición activa.
 */
class EstadoPaginacion(
    tamanioPaginaInicial: Int = 16
) {
    var paginaActual by mutableIntStateOf(1)
        private set

    var tamanioPagina by mutableIntStateOf(tamanioPaginaInicial)
        private set

    /**
     * Calcula la cantidad total de páginas requeridas para los elementos dados.
     */
    fun calcularTotalPaginas(totalElementos: Int): Int {
        if (totalElementos <= 0) return 1
        return ((totalElementos + tamanioPagina - 1) / tamanioPagina).coerceAtLeast(1)
    }

    /**
     * Extrae de forma segura el subconjunto de datos para la página actual.
     * Si la lista se reduce debido a filtros o búsquedas, reajusta automáticamente
     * la página actual para evitar índices fuera de rango.
     */
    fun <T> obtenerPagina(listaTotal: List<T>): List<T> {
        if (listaTotal.isEmpty()) {
            if (paginaActual != 1) paginaActual = 1
            return emptyList()
        }
        val totalPaginas = calcularTotalPaginas(listaTotal.size)
        if (paginaActual > totalPaginas) {
            paginaActual = totalPaginas
        }
        if (paginaActual < 1) {
            paginaActual = 1
        }
        val inicio = (paginaActual - 1) * tamanioPagina
        if (inicio >= listaTotal.size) return emptyList()
        val fin = minOf(inicio + tamanioPagina, listaTotal.size)
        return listaTotal.subList(inicio, fin)
    }

    fun irAPagina(pagina: Int, totalPaginas: Int) {
        paginaActual = pagina.coerceIn(1, totalPaginas.coerceAtLeast(1))
    }

    fun paginaSiguiente(totalPaginas: Int) {
        if (paginaActual < totalPaginas) {
            paginaActual++
        }
    }

    fun paginaAnterior() {
        if (paginaActual > 1) {
            paginaActual--
        }
    }

    fun primeraPagina() {
        paginaActual = 1
    }

    fun ultimaPagina(totalPaginas: Int) {
        paginaActual = totalPaginas.coerceAtLeast(1)
    }

    fun cambiarTamanioPagina(nuevoTamanio: Int) {
        if (nuevoTamanio > 0 && nuevoTamanio != tamanioPagina) {
            tamanioPagina = nuevoTamanio
            paginaActual = 1
        }
    }
}

/**
 * Retorna y recuerda una instancia de [EstadoPaginacion].
 * Si [claveReinicio] cambia (por ejemplo, el texto de búsqueda o filtro seleccionado),
 * el estado regresa automáticamente a la primera página.
 */
@Composable
fun rememberEstadoPaginacion(
    tamanioPaginaInicial: Int = 16,
    claveReinicio: Any? = null
): EstadoPaginacion {
    val estado = remember { EstadoPaginacion(tamanioPaginaInicial) }
    LaunchedEffect(claveReinicio) {
        estado.primeraPagina()
    }
    return estado
}
