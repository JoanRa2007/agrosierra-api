package pe.upeu.agrosierra.servicio

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pe.upeu.agrosierra.config.UsuarioAutenticado
import pe.upeu.agrosierra.dominio.ConflictoException
import pe.upeu.agrosierra.dominio.Labor
import pe.upeu.agrosierra.dominio.Lote
import pe.upeu.agrosierra.dominio.NoEncontradoException
import pe.upeu.agrosierra.dominio.SinPrivilegioException
import pe.upeu.agrosierra.dominio.TipoLabor
import pe.upeu.agrosierra.dominio.ValidacionException
import pe.upeu.agrosierra.dto.LaborRequest
import pe.upeu.agrosierra.repositorio.LaborRepository
import pe.upeu.agrosierra.repositorio.LoteRepository
import pe.upeu.agrosierra.seguridad.Privilegio
import pe.upeu.agrosierra.seguridad.UsuarioRepository
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime

/**
 * Reglas de negocio RN-01, RN-02 y RN-04 sobre las labores.
 * Viven aqui (capa de servicio, transaccional), no en el controlador ni en la aplicacion.
 */
@Service
class LaborService(
    private val labores: LaborRepository,
    private val lotes: LoteRepository,
    private val usuarios: UsuarioRepository,
    private val reloj: Clock,
) {
    @Transactional
    fun registrar(req: LaborRequest, actor: UsuarioAutenticado): Labor {
        val lote = lotes.findById(req.loteId!!).orElseThrow { NoEncontradoException("El lote ${req.loteId} no existe") }
        validarCampaniaAbierta(lote)                         // RN-01
        validarDosis(lote, req.tipo!!, req.cantidad!!)        // RN-04
        val labor = Labor(
            lote = lote,
            tipo = req.tipo,
            fecha = req.fecha!!,
            responsable = req.responsable!!.trim(),
            cantidad = req.cantidad,
            observacion = req.observacion.orEmpty().trim(),
            registradoPor = usuarios.findById(actor.id).orElseThrow { NoEncontradoException("Usuario no encontrado") },
            fechaRegistro = LocalDateTime.now(reloj),
        )
        return labores.save(labor)
    }

    @Transactional
    fun corregir(id: Long, req: LaborRequest, actor: UsuarioAutenticado): Labor {
        val labor = buscar(id)
        validarAlcanceEdicion(labor, actor)                    // RN-02
        validarCampaniaAbierta(labor.lote)                     // RN-01
        validarDosis(labor.lote, req.tipo!!, req.cantidad!!)   // RN-04
        labor.tipo = req.tipo
        labor.fecha = req.fecha!!
        labor.responsable = req.responsable!!.trim()
        labor.cantidad = req.cantidad
        labor.observacion = req.observacion.orEmpty().trim()
        return labor
    }

    @Transactional
    fun eliminar(id: Long) {
        labores.delete(buscar(id))
    }

    private fun buscar(id: Long): Labor =
        labores.findById(id).orElseThrow { NoEncontradoException("La labor $id no existe") }

    /** RN-01: sobre un lote con campania cerrada no se registra ni corrige nada. */
    private fun validarCampaniaAbierta(lote: Lote) {
        if (lote.campaniaCerrada) {
            throw ConflictoException("La campania del lote ${lote.codigo} esta cerrada; no admite registros ni correcciones")
        }
    }

    /**
     * RN-02: con LABOR_EDITAR_TODAS (jefe de campo) se corrige cualquier labor.
     * Sin ese alcance, solo las labores propias y dentro de las 24 horas siguientes al registro.
     * Se decide por privilegio, no por nombre de rol.
     */
    private fun validarAlcanceEdicion(labor: Labor, actor: UsuarioAutenticado) {
        if (Privilegio.LABOR_EDITAR_TODAS in actor.privilegios) return
        if (labor.registradoPor.id != actor.id) {
            throw SinPrivilegioException("Solo puede corregir las labores que usted registro")
        }
        val transcurrido = Duration.between(labor.fechaRegistro, LocalDateTime.now(reloj))
        if (transcurrido > Duration.ofHours(24)) {
            throw SinPrivilegioException("El plazo de 24 horas para corregir esta labor ya vencio")
        }
    }

    /** RN-04: en una fumigacion la cantidad no puede superar dosisMaxima x hectareas. */
    private fun validarDosis(lote: Lote, tipo: TipoLabor, cantidad: Double) {
        if (tipo != TipoLabor.FUMIGACION) return
        val maximo = lote.dosisMaximaPorHectarea * lote.hectareas
        if (cantidad > maximo) {
            throw ValidacionException(
                "cantidad",
                "La cantidad supera la dosis maxima permitida para el lote (%.2f)".format(java.util.Locale.US, maximo),
            )
        }
    }
}
