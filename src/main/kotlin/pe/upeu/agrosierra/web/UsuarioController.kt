package pe.upeu.agrosierra.web

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pe.upeu.agrosierra.config.UsuarioAutenticado
import pe.upeu.agrosierra.dominio.NoEncontradoException
import pe.upeu.agrosierra.dto.UsuarioResponse
import pe.upeu.agrosierra.seguridad.UsuarioRepository

@RestController
@RequestMapping("/api/v1/usuarios")
class UsuarioController(private val usuarios: UsuarioRepository) {

    /** RF-B07: datos y privilegios del usuario del token. */
    @GetMapping("/me")
    fun yo(@AuthenticationPrincipal actor: UsuarioAutenticado): UsuarioResponse =
        UsuarioResponse.de(usuarios.findById(actor.id).orElseThrow { NoEncontradoException("Usuario no encontrado") })
}
