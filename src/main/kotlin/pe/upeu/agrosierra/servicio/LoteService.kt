package pe.upeu.agrosierra.servicio

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pe.upeu.agrosierra.dominio.ConflictoException
import pe.upeu.agrosierra.dominio.Labor
import pe.upeu.agrosierra.dominio.Lote
import pe.upeu.agrosierra.dominio.NoEncontradoException
import pe.upeu.agrosierra.dominio.TipoLabor
import pe.upeu.agrosierra.dominio.ValidacionException
import pe.upeu.agrosierra.repositorio.LaborRepository
import pe.upeu.agrosierra.repositorio.LoteRepository

@Service
class LoteService(
    private val lotes: LoteRepository,
    private val labores: LaborRepository,
) {
    @Transactional(readOnly = true)
    fun listar(valle: String?, estado: String?, pagina: Int, tamanio: Int): Page<Lote> {
        val cerrada = when (estado?.uppercase()) {
            null, "" -> null
            "ABIERTA" -> false
            "CERRADA" -> true
            else -> throw ValidacionException("estado", "Use ABIERTA o CERRADA")
        }
        return lotes.buscar(valle?.takeIf { it.isNotBlank() }, cerrada, paginacion(pagina, tamanio, Sort.by("codigo")))
    }

    @Transactional(readOnly = true)
    fun obtener(id: Long): Lote = lotes.findById(id).orElseThrow { NoEncontradoException("El lote $id no existe") }

    @Transactional(readOnly = true)
    fun labores(loteId: Long, pagina: Int, tamanio: Int): Page<Labor> {
        obtener(loteId)
        val orden = Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("id"))
        return labores.findByLoteId(loteId, paginacion(pagina, tamanio, orden))
    }

    /** RF-B06 + RN-03: no se cierra si el lote no tiene al menos una labor de COSECHA. */
    @Transactional
    fun cerrarCampania(id: Long): Lote {
        val lote = obtener(id)
        if (lote.campaniaCerrada) throw ConflictoException("La campania del lote ${lote.codigo} ya esta cerrada")
        if (!labores.existsByLoteIdAndTipo(id, TipoLabor.COSECHA)) {
            throw ConflictoException("No se puede cerrar la campania: el lote ${lote.codigo} no tiene ninguna labor de COSECHA")
        }
        lote.campaniaCerrada = true
        return lote
    }
}

internal fun paginacion(pagina: Int, tamanio: Int, orden: Sort): PageRequest {
    if (pagina < 0) throw ValidacionException("pagina", "Debe ser mayor o igual a 0")
    if (tamanio !in 1..100) throw ValidacionException("tamanio", "Debe estar entre 1 y 100")
    return PageRequest.of(pagina, tamanio, orden)
}
