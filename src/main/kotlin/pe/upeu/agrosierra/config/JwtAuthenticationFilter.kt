package pe.upeu.agrosierra.config

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Lee "Authorization: Bearer <token>", verifica la firma y coloca los privilegios como authorities.
 * Si el token falta, esta alterado o vencido, no se autentica y el EntryPoint responde 401.
 */
@Component
class JwtAuthenticationFilter(private val jwt: JwtService) : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val cabecera = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (cabecera != null && cabecera.startsWith("Bearer ")) {
            try {
                val usuario = jwt.validar(cabecera.removePrefix("Bearer ").trim())
                val authorities = usuario.privilegios.map { SimpleGrantedAuthority(it) }
                SecurityContextHolder.getContext().authentication =
                    UsernamePasswordAuthenticationToken(usuario, null, authorities)
            } catch (e: ExpiredJwtException) {
                request.setAttribute(ATRIBUTO_ERROR, "El token vencio, inicia sesion nuevamente")
            } catch (e: JwtException) {
                request.setAttribute(ATRIBUTO_ERROR, "Token invalido o alterado")
            } catch (e: IllegalArgumentException) {
                request.setAttribute(ATRIBUTO_ERROR, "Token invalido o alterado")
            }
        }
        chain.doFilter(request, response)
    }

    companion object {
        const val ATRIBUTO_ERROR = "agro.jwt.error"
    }
}
