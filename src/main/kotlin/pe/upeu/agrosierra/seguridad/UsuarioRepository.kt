package pe.upeu.agrosierra.seguridad

import org.springframework.data.jpa.repository.JpaRepository

interface UsuarioRepository : JpaRepository<Usuario, Long> {
    fun findByUsuario(usuario: String): Usuario?
}

interface RolRepository : JpaRepository<Rol, Long> {
    fun findByNombre(nombre: String): Rol?
}

interface PrivilegioRepository : JpaRepository<Privilegio, Long> {
    fun findByNombre(nombre: String): Privilegio?
}
