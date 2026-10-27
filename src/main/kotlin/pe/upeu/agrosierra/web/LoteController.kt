package pe.upeu.agrosierra.web

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pe.upeu.agrosierra.dto.LaborResponse
import pe.upeu.agrosierra.dto.LoteResponse
import pe.upeu.agrosierra.dto.Pagina
import pe.upeu.agrosierra.servicio.LoteService

/** Controlador delgado: solo traduce HTTP <-> servicio. Los privilegios se exigen en SecurityConfig. */
@RestController
@RequestMapping("/api/v1/lotes")
class LoteController(private val servicio: LoteService) {

    @GetMapping
    fun listar(
        @RequestParam(defaultValue = "0") pagina: Int,
        @RequestParam(defaultValue = "20") tamanio: Int,
        @RequestParam(required = false) valle: String?,
        @RequestParam(required = false) estado: String?,
    ): Pagina<LoteResponse> = Pagina.de(servicio.listar(valle, estado, pagina, tamanio), LoteResponse::de)

    @GetMapping("/{id}")
    fun detalle(@PathVariable id: Long): LoteResponse = LoteResponse.de(servicio.obtener(id))

    @GetMapping("/{id}/labores")
    fun labores(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") pagina: Int,
        @RequestParam(defaultValue = "20") tamanio: Int,
    ): Pagina<LaborResponse> = Pagina.de(servicio.labores(id, pagina, tamanio), LaborResponse::de)

    @PostMapping("/{id}/cerrar-campania")
    fun cerrarCampania(@PathVariable id: Long): LoteResponse = LoteResponse.de(servicio.cerrarCampania(id))
}
