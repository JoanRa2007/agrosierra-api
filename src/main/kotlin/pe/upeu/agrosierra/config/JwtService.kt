package pe.upeu.agrosierra.config

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pe.upeu.agrosierra.seguridad.Usuario
import java.util.Date
import javax.crypto.SecretKey

/** Datos del usuario que viajan dentro del token y que el filtro coloca en el contexto de seguridad. */
data class UsuarioAutenticado(
    val id: Long,
    val usuario: String,
    val nombre: String,
    val privilegios: Set<String>,
)

data class TokenGenerado(val valor: String, val expiraEn: Long)

@Service
class JwtService(
    @Value("\${agro.jwt.secreto}") secreto: String,
    @Value("\${agro.jwt.vigencia-minutos}") private val vigenciaMinutos: Long,
) {
    private val clave: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secreto))

    /** Firma HS256. El token transporta el id del usuario (sub) y sus privilegios. Sin renovacion. */
    fun generar(usuario: Usuario): TokenGenerado {
        val ahora = System.currentTimeMillis()
        val expira = ahora + vigenciaMinutos * 60_000
        val valor = Jwts.builder()
            .subject(usuario.id.toString())
            .claim("usuario", usuario.usuario)
            .claim("nombre", usuario.nombre)
            .claim("privilegios", usuario.nombresPrivilegios().toList())
            .issuedAt(Date(ahora))
            .expiration(Date(expira))
            .signWith(clave)
            .compact()
        return TokenGenerado(valor, expira)
    }

    /** Verifica firma y vigencia. Lanza JwtException si el token esta alterado o vencido. */
    fun validar(token: String): UsuarioAutenticado {
        val claims: Claims = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).payload
        @Suppress("UNCHECKED_CAST")
        val privilegios = (claims["privilegios"] as? List<String>).orEmpty().toSet()
        return UsuarioAutenticado(
            id = claims.subject.toLong(),
            usuario = claims["usuario"] as String,
            nombre = claims["nombre"] as String,
            privilegios = privilegios,
        )
    }
}
