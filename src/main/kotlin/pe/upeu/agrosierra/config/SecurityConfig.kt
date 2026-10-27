package pe.upeu.agrosierra.config

import org.springframework.boot.autoconfigure.security.servlet.PathRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import pe.upeu.agrosierra.seguridad.Privilegio.Companion.CAMPANIA_CERRAR
import pe.upeu.agrosierra.seguridad.Privilegio.Companion.LABOR_CREAR
import pe.upeu.agrosierra.seguridad.Privilegio.Companion.LABOR_EDITAR
import pe.upeu.agrosierra.seguridad.Privilegio.Companion.LABOR_ELIMINAR
import pe.upeu.agrosierra.seguridad.Privilegio.Companion.LABOR_LEER
import pe.upeu.agrosierra.seguridad.Privilegio.Companion.LOTE_LEER

/**
 * Matriz de autorizacion del caso (seccion 4.3). Cada ruta exige un PRIVILEGIO, nunca un rol.
 * Que privilegios tiene cada rol se decide en la base de datos (tabla rol_privilegio).
 */
@Configuration
class SecurityConfig(
    private val jwtFilter: JwtAuthenticationFilter,
    private val entryPoint: EntryPoint401,
    private val accesoDenegado: AccesoDenegado403,
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .headers { h -> h.frameOptions { it.sameOrigin() } } // consola H2
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(PathRequest.toH2Console()).permitAll()
                    .requestMatchers("/error").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/usuarios/me").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/v1/lotes/*/labores").hasAuthority(LABOR_LEER)
                    .requestMatchers(HttpMethod.GET, "/api/v1/lotes", "/api/v1/lotes/*").hasAuthority(LOTE_LEER)
                    .requestMatchers(HttpMethod.POST, "/api/v1/lotes/*/cerrar-campania").hasAuthority(CAMPANIA_CERRAR)
                    .requestMatchers(HttpMethod.POST, "/api/v1/labores").hasAuthority(LABOR_CREAR)
                    .requestMatchers(HttpMethod.PUT, "/api/v1/labores/*").hasAuthority(LABOR_EDITAR)
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/labores/*").hasAuthority(LABOR_ELIMINAR)
                    .anyRequest().authenticated()
            }
            .exceptionHandling {
                it.authenticationEntryPoint(entryPoint)
                it.accessDeniedHandler(accesoDenegado)
            }
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}
