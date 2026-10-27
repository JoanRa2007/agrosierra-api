package pe.upeu.agrosierra.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import pe.upeu.agrosierra.dominio.Labor
import pe.upeu.agrosierra.dominio.Lote
import pe.upeu.agrosierra.dominio.TipoLabor
import pe.upeu.agrosierra.seguridad.Usuario
import java.time.LocalDate

// ---------- Autenticacion ----------
data class LoginRequest(
    @field:NotBlank(message = "El usuario es obligatorio")
    val usuario: String?,
    @field:NotBlank(message = "La contrasena es obligatoria")
    val contrasena: String?,
)

data class LoginResponse(
    val token: String,
    /** Instante de vencimiento del token en milisegundos epoch. */
    val expiraEn: Long,
    val usuario: String,
    val nombre: String,
    val rol: String,
    val privilegios: Set<String>,
)

data class UsuarioResponse(
    val id: Long,
    val usuario: String,
    val nombre: String,
    val rol: String,
    val privilegios: Set<String>,
) {
    companion object {
        fun de(u: Usuario) = UsuarioResponse(u.id!!, u.usuario, u.nombre, u.rol.nombre, u.nombresPrivilegios())
    }
}

// ---------- Lotes ----------
data class LoteResponse(
    val id: Long,
    val codigo: String,
    val nombre: String,
    val hectareas: Double,
    val cultivo: String,
    val valle: String,
    val campaniaCerrada: Boolean,
    val dosisMaximaPorHectarea: Double,
) {
    companion object {
        fun de(l: Lote) = LoteResponse(
            l.id!!, l.codigo, l.nombre, l.hectareas, l.cultivo, l.valle, l.campaniaCerrada, l.dosisMaximaPorHectarea,
        )
    }
}

// ---------- Labores ----------
data class LaborRequest(
    @field:NotNull(message = "El lote es obligatorio")
    val loteId: Long?,
    @field:NotNull(message = "El tipo es obligatorio (RIEGO, FUMIGACION o COSECHA)")
    val tipo: TipoLabor?,
    @field:NotNull(message = "La fecha es obligatoria (formato AAAA-MM-DD)")
    val fecha: LocalDate?,
    @field:NotBlank(message = "El responsable es obligatorio")
    @field:Size(max = 100, message = "Maximo 100 caracteres")
    val responsable: String?,
    @field:NotNull(message = "La cantidad es obligatoria")
    @field:Positive(message = "La cantidad debe ser mayor que cero")
    val cantidad: Double?,
    @field:Size(max = 250, message = "Maximo 250 caracteres")
    val observacion: String? = "",
)

data class LaborResponse(
    val id: Long,
    val loteId: Long,
    val tipo: TipoLabor,
    val fecha: LocalDate,
    val responsable: String,
    val cantidad: Double,
    val observacion: String,
    val registradoPor: String,
    val fechaRegistro: String,
) {
    companion object {
        fun de(l: Labor) = LaborResponse(
            l.id!!, l.lote.id!!, l.tipo, l.fecha, l.responsable, l.cantidad, l.observacion,
            l.registradoPor.usuario, l.fechaRegistro.withNano(0).toString(),
        )
    }
}
