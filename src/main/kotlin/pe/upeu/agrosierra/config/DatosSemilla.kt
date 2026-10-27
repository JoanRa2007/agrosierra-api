package pe.upeu.agrosierra.config

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pe.upeu.agrosierra.dominio.Labor
import pe.upeu.agrosierra.dominio.Lote
import pe.upeu.agrosierra.dominio.TipoLabor
import pe.upeu.agrosierra.dominio.TipoLabor.COSECHA
import pe.upeu.agrosierra.dominio.TipoLabor.FUMIGACION
import pe.upeu.agrosierra.dominio.TipoLabor.RIEGO
import pe.upeu.agrosierra.repositorio.LaborRepository
import pe.upeu.agrosierra.repositorio.LoteRepository
import pe.upeu.agrosierra.seguridad.Privilegio
import pe.upeu.agrosierra.seguridad.PrivilegioRepository
import pe.upeu.agrosierra.seguridad.Rol
import pe.upeu.agrosierra.seguridad.RolRepository
import pe.upeu.agrosierra.seguridad.Usuario
import pe.upeu.agrosierra.seguridad.UsuarioRepository
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Datos semilla del anexo (seccion 10): privilegios, roles con su matriz (3.3), 3 usuarios,
 * 8 lotes en 3 valles (2 cerrados) y 20 labores.
 */
@Component
class DatosSemilla(
    private val privilegios: PrivilegioRepository,
    private val roles: RolRepository,
    private val usuarios: UsuarioRepository,
    private val lotes: LoteRepository,
    private val labores: LaborRepository,
    private val encoder: PasswordEncoder,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (usuarios.count() > 0) return

        // ---- Privilegios ----
        val p = listOf(
            Privilegio.LOTE_LEER to "Consultar el padron y el detalle de lotes",
            Privilegio.LABOR_LEER to "Consultar las labores de un lote",
            Privilegio.LABOR_CREAR to "Registrar una labor nueva",
            Privilegio.LABOR_EDITAR to "Corregir una labor existente (propias, 24 h)",
            Privilegio.LABOR_EDITAR_TODAS to "Corregir cualquier labor sin limite de plazo",
            Privilegio.LABOR_ELIMINAR to "Eliminar una labor",
            Privilegio.CAMPANIA_CERRAR to "Cerrar la campania de un lote",
            Privilegio.USUARIO_GESTIONAR to "Crear usuarios y asignar roles",
        ).associate { (nombre, desc) -> nombre to privilegios.save(Privilegio(nombre, desc)) }

        // ---- Roles = conjuntos de privilegios (matriz 3.3) ----
        fun rol(nombre: String, vararg nombres: String) =
            roles.save(Rol(nombre, nombres.map { p.getValue(it) }.toMutableSet()))

        val tecnico = rol(
            "TECNICO",
            Privilegio.LOTE_LEER, Privilegio.LABOR_LEER, Privilegio.LABOR_CREAR, Privilegio.LABOR_EDITAR,
        )
        val jefe = rol(
            "JEFE_CAMPO",
            Privilegio.LOTE_LEER, Privilegio.LABOR_LEER, Privilegio.LABOR_CREAR, Privilegio.LABOR_EDITAR,
            Privilegio.LABOR_EDITAR_TODAS, Privilegio.LABOR_ELIMINAR, Privilegio.CAMPANIA_CERRAR,
        )
        val admin = rol(
            "ADMINISTRADOR",
            Privilegio.LOTE_LEER, Privilegio.LABOR_LEER, Privilegio.USUARIO_GESTIONAR,
        )

        // ---- Usuarios de prueba (contrasena cifrada con BCrypt) ----
        val clave = encoder.encode("Agro2026*")
        val uTecnico = usuarios.save(Usuario("tecnico1", "Carlos Quispe", clave, tecnico))
        val uJefe = usuarios.save(Usuario("jefe1", "Rosa Huaman", clave, jefe))
        usuarios.save(Usuario("admin", "Administrador AgroSierra", clave, admin))

        // ---- 8 lotes en 3 valles; L-007 y L-008 con campania cerrada ----
        fun lote(codigo: String, nombre: String, ha: Double, cultivo: String, valle: String, dosis: Double, cerrada: Boolean = false) =
            lotes.save(Lote(codigo, nombre, ha, cultivo, valle, cerrada, dosis))

        val l1 = lote("L-001", "Chacra Santa Rosa", 2.5, "Papa", "Mantaro", 4.0)
        val l2 = lote("L-002", "Chacra El Molino", 1.8, "Maiz", "Mantaro", 3.0)
        val l3 = lote("L-003", "Chacra Huayao", 3.2, "Alcachofa", "Mantaro", 5.0)
        val l4 = lote("L-004", "Chacra San Juan", 1.2, "Haba", "Yanamarca", 2.5)
        val l5 = lote("L-005", "Chacra Pucara", 2.0, "Quinua", "Yanamarca", 3.5)
        val l6 = lote("L-006", "Chacra La Esperanza", 4.0, "Cafe", "Chanchamayo", 6.0)
        val l7 = lote("L-007", "Chacra Los Andes", 2.8, "Papa", "Yanamarca", 4.0, cerrada = true)
        val l8 = lote("L-008", "Chacra Pichanaki", 3.5, "Cafe", "Chanchamayo", 6.0, cerrada = true)

        // ---- 20 labores: cada lote abierto con >= 2; L-001 tiene COSECHA y L-002 no (RN-03) ----
        val hace3Dias = LocalDateTime.now().minusDays(3)
        fun labor(lote: Lote, tipo: TipoLabor, fecha: String, cantidad: Double, obs: String, jefeRegistra: Boolean = false) {
            val autor = if (jefeRegistra) uJefe else uTecnico
            labores.save(
                Labor(lote, tipo, LocalDate.parse(fecha), autor.nombre, cantidad, obs, autor, hace3Dias),
            )
        }
        labor(l1, RIEGO, "2026-09-02", 120.0, "Riego por gravedad")
        labor(l1, FUMIGACION, "2026-09-20", 8.0, "Control de rancha")
        labor(l1, COSECHA, "2026-10-15", 4500.0, "Cosecha de papa amarilla", jefeRegistra = true)
        labor(l2, RIEGO, "2026-09-05", 90.0, "Riego de inicio")
        labor(l2, FUMIGACION, "2026-09-28", 4.5, "Control de cogollero")
        labor(l3, RIEGO, "2026-09-10", 150.0, "Riego por goteo")
        labor(l3, FUMIGACION, "2026-10-01", 12.0, "Preventivo de oidio")
        labor(l4, RIEGO, "2026-09-12", 60.0, "Riego ligero")
        labor(l4, FUMIGACION, "2026-10-03", 2.0, "Control de pulgon")
        labor(l5, RIEGO, "2026-09-15", 80.0, "Riego de macollamiento")
        labor(l5, FUMIGACION, "2026-10-05", 5.0, "Control de mildiu")
        labor(l6, RIEGO, "2026-09-08", 200.0, "Riego por aspersion")
        labor(l6, FUMIGACION, "2026-09-30", 18.0, "Control de roya")
        labor(l7, RIEGO, "2026-06-10", 110.0, "Riego de campania anterior")
        labor(l7, FUMIGACION, "2026-07-02", 9.0, "Control de rancha")
        labor(l7, COSECHA, "2026-08-20", 5200.0, "Cosecha cerrada", jefeRegistra = true)
        labor(l8, RIEGO, "2026-05-15", 180.0, "Riego de campania anterior")
        labor(l8, FUMIGACION, "2026-06-01", 15.0, "Control de broca")
        labor(l8, COSECHA, "2026-07-25", 3100.0, "Cosecha de cafe", jefeRegistra = true)
        labor(l6, COSECHA, "2026-10-20", 2800.0, "Primera cosecha de cafe", jefeRegistra = true)

        log.info("Datos semilla cargados: {} usuarios, {} lotes, {} labores", usuarios.count(), lotes.count(), labores.count())
    }
}
