package pe.upeu.agrosierra.repositorio

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import pe.upeu.agrosierra.dominio.Labor
import pe.upeu.agrosierra.dominio.Lote
import pe.upeu.agrosierra.dominio.TipoLabor

interface LoteRepository : JpaRepository<Lote, Long> {
    @Query(
        """
        select l from Lote l
        where (:valle is null or lower(l.valle) = lower(cast(:valle as string)))
          and (:cerrada is null or l.campaniaCerrada = :cerrada)
        """,
    )
    fun buscar(@Param("valle") valle: String?, @Param("cerrada") cerrada: Boolean?, pageable: Pageable): Page<Lote>
}

interface LaborRepository : JpaRepository<Labor, Long> {
    fun findByLoteId(loteId: Long, pageable: Pageable): Page<Labor>
    fun existsByLoteIdAndTipo(loteId: Long, tipo: TipoLabor): Boolean
}
