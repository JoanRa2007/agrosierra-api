package pe.upeu.agrosierra.dominio

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import pe.upeu.agrosierra.seguridad.Usuario
import java.time.LocalDate
import java.time.LocalDateTime

enum class TipoLabor { RIEGO, FUMIGACION, COSECHA }

@Entity
@Table(name = "labor")
class Labor(
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "lote_id")
    var lote: Lote,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var tipo: TipoLabor,
    @Column(nullable = false)
    var fecha: LocalDate,
    @Column(nullable = false, length = 100)
    var responsable: String,
    @Column(nullable = false)
    var cantidad: Double,
    @Column(nullable = false, length = 250)
    var observacion: String = "",
    /** Quien registro la labor y cuando: base de la regla RN-02. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "registrado_por_id")
    var registradoPor: Usuario,
    @Column(nullable = false)
    var fechaRegistro: LocalDateTime = LocalDateTime.now(),
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)
