package pe.upeu.agrosierra.dominio

/** 404 */
class NoEncontradoException(mensaje: String) : RuntimeException(mensaje)

/** 409: regla de negocio incumplida (RN-01, RN-03). */
class ConflictoException(mensaje: String) : RuntimeException(mensaje)

/** 403: autenticado, pero la regla de alcance no le permite la operacion (RN-02). */
class SinPrivilegioException(mensaje: String) : RuntimeException(mensaje)

/** 400 con detalle por campo (RN-04). */
class ValidacionException(val campo: String, mensaje: String) : RuntimeException(mensaje)

/** 401 en el inicio de sesion. */
class CredencialesInvalidasException : RuntimeException("Usuario o contrasena incorrectos")
