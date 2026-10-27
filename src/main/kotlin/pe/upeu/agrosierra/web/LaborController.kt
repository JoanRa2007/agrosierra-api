package pe.upeu.agrosierra.web

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import pe.upeu.agrosierra.config.UsuarioAutenticado
import pe.upeu.agrosierra.dto.LaborRequest
import pe.upeu.agrosierra.dto.LaborResponse
import pe.upeu.agrosierra.servicio.LaborService

@RestController
@RequestMapping("/api/v1/labores")
class LaborController(private val servicio: LaborService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun registrar(
        @Valid @RequestBody req: LaborRequest,
        @AuthenticationPrincipal actor: UsuarioAutenticado,
    ): LaborResponse = LaborResponse.de(servicio.registrar(req, actor))

    @PutMapping("/{id}")
    fun corregir(
        @PathVariable id: Long,
        @Valid @RequestBody req: LaborRequest,
        @AuthenticationPrincipal actor: UsuarioAutenticado,
    ): LaborResponse = LaborResponse.de(servicio.corregir(id, req, actor))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun eliminar(@PathVariable id: Long) = servicio.eliminar(id)
}
