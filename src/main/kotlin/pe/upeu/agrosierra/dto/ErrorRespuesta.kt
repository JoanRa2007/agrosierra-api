package pe.upeu.agrosierra.dto

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.http.HttpStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Formato de error de todo el servicio (seccion 4.2). */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorRespuesta(
    val timestamp: String,
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val validationErrors: Map<String, String>? = null,
) {
    companion object {
        private val FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

        fun de(status: HttpStatus, mensaje: String, path: String, errores: Map<String, String>? = null) = ErrorRespuesta(
            timestamp = LocalDateTime.now().format(FORMATO),
            status = status.value(),
            error = status.reasonPhrase,
            message = mensaje,
            path = path,
            validationErrors = errores,
        )
    }
}
