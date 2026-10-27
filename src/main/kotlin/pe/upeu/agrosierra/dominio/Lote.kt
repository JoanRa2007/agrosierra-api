package pe.upeu.agrosierra.dominio

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "lote")
class Lote(
    @Column(nullable = false, unique = true, length = 20)
    var codigo: String,
    @Column(nullable = false, length = 100)
    var nombre: String,
    @Column(nullable = false)
    var hectareas: Double,
    @Column(nullable = false, length = 60)
    var cultivo: String,
    @Column(nullable = false, length = 60)
    var valle: String,
    @Column(nullable = false)
    var campaniaCerrada: Boolean = false,
    /** Dosis maxima de fumigacion por hectarea (RN-04). */
    @Column(nullable = false)
    var dosisMaximaPorHectarea: Double,
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)
