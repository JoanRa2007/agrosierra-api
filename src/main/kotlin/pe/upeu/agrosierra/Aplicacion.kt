package pe.upeu.agrosierra

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import java.time.Clock

/**
 * AgroAPI: servicio REST de la cooperativa AgroSierra.
 * Autor: Joan Rodriguez - Examen Parcial Unidad 2, Desarrollo de Aplicaciones Moviles (UPeU).
 */
@SpringBootApplication
class Aplicacion {
    /** Reloj inyectable: permite probar la regla de 24 horas (RN-02). */
    @Bean
    fun reloj(): Clock = Clock.systemDefaultZone()
}

fun main(args: Array<String>) {
    runApplication<Aplicacion>(*args)
}
