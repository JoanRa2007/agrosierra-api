package pe.upeu.agrosierra.seguridad

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * Un privilegio es un permiso concreto. La autorizacion SIEMPRE se verifica contra
 * el privilegio, nunca contra el nombre del rol.
 */
@Entity
@Table(name = "privilegio")
class Privilegio(
    @Column(nullable = false, unique = true, length = 40)
    var nombre: String,
    @Column(nullable = false, length = 120)
    var descripcion: String = "",
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
) {
    companion object {
        const val LOTE_LEER = "LOTE_LEER"
        const val LABOR_LEER = "LABOR_LEER"
        const val LABOR_CREAR = "LABOR_CREAR"
        const val LABOR_EDITAR = "LABOR_EDITAR"
        /** Alcance de LABOR_EDITAR: corregir labores de cualquier usuario y sin plazo (RN-02). */
        const val LABOR_EDITAR_TODAS = "LABOR_EDITAR_TODAS"
        const val LABOR_ELIMINAR = "LABOR_ELIMINAR"
        const val CAMPANIA_CERRAR = "CAMPANIA_CERRAR"
        const val USUARIO_GESTIONAR = "USUARIO_GESTIONAR"
    }
}
