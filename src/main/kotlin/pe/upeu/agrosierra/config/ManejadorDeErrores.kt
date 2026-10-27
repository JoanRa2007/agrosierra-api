package pe.upeu.agrosierra.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException
import pe.upeu.agrosierra.dominio.ConflictoException
import pe.upeu.agrosierra.dominio.CredencialesInvalidasException
import pe.upeu.agrosierra.dominio.NoEncontradoException
import pe.upeu.agrosierra.dominio.SinPrivilegioException
import pe.upeu.agrosierra.dominio.ValidacionException
import pe.upeu.agrosierra.dto.ErrorRespuesta

/** Traduce cada excepcion al formato de error comun de la seccion 4.2. */
@RestControllerAdvice
class ManejadorDeErrores {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validacion(e: MethodArgumentNotValidException, req: HttpServletRequest) =
        responder(
            HttpStatus.BAD_REQUEST, "Datos invalidos", req,
            e.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Valor invalido") },
        )

    @ExceptionHandler(ValidacionException::class)
    fun validacionNegocio(e: ValidacionException, req: HttpServletRequest) =
        responder(HttpStatus.BAD_REQUEST, e.message!!, req, mapOf(e.campo to e.message!!))

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun cuerpoIlegible(e: HttpMessageNotReadableException, req: HttpServletRequest): ResponseEntity<ErrorRespuesta> {
        val causa = e.cause
        val errores = if (causa is MismatchedInputException && causa.path.isNotEmpty()) {
            val campo = causa.path.joinToString(".") { it.fieldName ?: "[${it.index}]" }
            val msg = if (causa is InvalidFormatException) "Valor no permitido: ${causa.value}" else "Formato invalido"
            mapOf(campo to msg)
        } else null
        return responder(HttpStatus.BAD_REQUEST, "Cuerpo de la peticion invalido", req, errores)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun parametroInvalido(e: MethodArgumentTypeMismatchException, req: HttpServletRequest) =
        responder(HttpStatus.BAD_REQUEST, "Parametro invalido", req, mapOf(e.name to "Valor no permitido: ${e.value}"))

    @ExceptionHandler(CredencialesInvalidasException::class)
    fun credenciales(e: CredencialesInvalidasException, req: HttpServletRequest) =
        responder(HttpStatus.UNAUTHORIZED, e.message!!, req)

    @ExceptionHandler(SinPrivilegioException::class, AccessDeniedException::class)
    fun sinPrivilegio(e: Exception, req: HttpServletRequest) =
        responder(HttpStatus.FORBIDDEN, e.message ?: "No tiene el privilegio requerido", req)

    @ExceptionHandler(NoEncontradoException::class, NoResourceFoundException::class)
    fun noEncontrado(e: Exception, req: HttpServletRequest) =
        responder(HttpStatus.NOT_FOUND, (e as? NoEncontradoException)?.message ?: "Recurso no encontrado", req)

    @ExceptionHandler(ConflictoException::class)
    fun conflicto(e: ConflictoException, req: HttpServletRequest) =
        responder(HttpStatus.CONFLICT, e.message!!, req)

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun metodo(e: HttpRequestMethodNotSupportedException, req: HttpServletRequest) =
        responder(HttpStatus.METHOD_NOT_ALLOWED, "Metodo no permitido", req)

    @ExceptionHandler(Exception::class)
    fun servidor(e: Exception, req: HttpServletRequest): ResponseEntity<ErrorRespuesta> {
        log.error("Error no controlado en {}", req.requestURI, e)
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", req)
    }

    private fun responder(
        status: HttpStatus, mensaje: String, req: HttpServletRequest, errores: Map<String, String>? = null,
    ) = ResponseEntity.status(status).body(ErrorRespuesta.de(status, mensaje, req.requestURI, errores))
}

/** 401: token ausente, alterado o vencido. */
@Component
class EntryPoint401(private val mapper: ObjectMapper) : AuthenticationEntryPoint {
    override fun commence(req: HttpServletRequest, res: HttpServletResponse, e: AuthenticationException) {
        val mensaje = req.getAttribute(JwtAuthenticationFilter.ATRIBUTO_ERROR) as? String ?: "Se requiere autenticacion"
        escribir(mapper, res, ErrorRespuesta.de(HttpStatus.UNAUTHORIZED, mensaje, req.requestURI))
    }
}

/** 403: autenticado, pero sin el privilegio requerido. La sesion no se cierra. */
@Component
class AccesoDenegado403(private val mapper: ObjectMapper) : AccessDeniedHandler {
    override fun handle(req: HttpServletRequest, res: HttpServletResponse, e: AccessDeniedException) {
        escribir(mapper, res, ErrorRespuesta.de(HttpStatus.FORBIDDEN, "No tiene el privilegio requerido para esta operacion", req.requestURI))
    }
}

private fun escribir(mapper: ObjectMapper, res: HttpServletResponse, cuerpo: ErrorRespuesta) {
    res.status = cuerpo.status
    res.contentType = MediaType.APPLICATION_JSON_VALUE
    res.characterEncoding = "UTF-8"
    mapper.writeValue(res.outputStream, cuerpo)
}
