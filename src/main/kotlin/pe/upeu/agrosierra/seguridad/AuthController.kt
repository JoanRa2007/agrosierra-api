package pe.upeu.agrosierra.seguridad

import jakarta.validation.Valid
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pe.upeu.agrosierra.config.JwtService
import pe.upeu.agrosierra.dominio.CredencialesInvalidasException
import pe.upeu.agrosierra.dto.LoginRequest
import pe.upeu.agrosierra.dto.LoginResponse

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val usuarios: UsuarioRepository,
    private val encoder: PasswordEncoder,
    private val jwt: JwtService,
) {
    /** RF-B01: valida contra la contrasena cifrada y devuelve un token firmado con vigencia de una hora. */
    @PostMapping("/login")
    fun login(@Valid @RequestBody req: LoginRequest): LoginResponse {
        val usuario = usuarios.findByUsuario(req.usuario!!.trim())
            ?.takeIf { encoder.matches(req.contrasena, it.contrasena) }
            ?: throw CredencialesInvalidasException()
        val token = jwt.generar(usuario)
        return LoginResponse(
            token = token.valor,
            expiraEn = token.expiraEn,
            usuario = usuario.usuario,
            nombre = usuario.nombre,
            rol = usuario.rol.nombre,
            privilegios = usuario.nombresPrivilegios(),
        )
    }
}
