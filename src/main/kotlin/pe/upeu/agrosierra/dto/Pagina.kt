package pe.upeu.agrosierra.dto

import org.springframework.data.domain.Page

/** Envoltorio de paginacion comun a todos los listados (seccion 4.2). */
data class Pagina<T>(
    val contenido: List<T>,
    val pagina: Int,
    val tamanio: Int,
    val totalElementos: Long,
    val totalPaginas: Int,
    val ultima: Boolean,
) {
    companion object {
        fun <E, T> de(page: Page<E>, convertir: (E) -> T) = Pagina(
            contenido = page.content.map(convertir),
            pagina = page.number,
            tamanio = page.size,
            totalElementos = page.totalElements,
            totalPaginas = page.totalPages,
            ultima = page.isLast,
        )
    }
}
