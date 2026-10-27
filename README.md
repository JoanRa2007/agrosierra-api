# AgroAPI — Servicio REST de AgroSierra

**Autor:** Joan Rodriguez · Desarrollo de Aplicaciones Móviles · UPeU · Examen Parcial Unidad 2 (Versión A)

Servicio que custodia los lotes y las labores de campo de la cooperativa AgroSierra.
Spring Boot 3.5 + Kotlin, JWT (HS256, vigencia 1 hora), Spring Security por **privilegios**, JPA + H2.

## Cómo ejecutarlo (un solo comando)

Requisito: JDK 21 (IntelliJ IDEA lo trae o lo descarga).

```bash
./gradlew bootRun          # Linux / macOS / Git Bash
gradlew.bat bootRun        # Windows (CMD / PowerShell)
```

- Queda escuchando en `http://localhost:8080/api/v1` (desde el emulador: `http://10.0.2.2:8080/api/v1`).
- Al iniciar se crean el esquema y los **datos semilla** (usuarios, 8 lotes, 20 labores).
- Consola de la base: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:agrosierra`, usuario `sa`, sin contraseña).
- Pruebas de integración: `./gradlew test` (9 pruebas: 401, 403, 400, 409, RN-01..RN-04, paginación).
- En IntelliJ: abrir la carpeta, esperar la sincronización de Gradle y ejecutar `Aplicacion.kt` (▶).

### Usuarios de prueba

| Usuario  | Contraseña | Rol           |
|----------|------------|---------------|
| tecnico1 | Agro2026*  | TECNICO       |
| jefe1    | Agro2026*  | JEFE_CAMPO    |
| admin    | Agro2026*  | ADMINISTRADOR |

Las contraseñas se guardan cifradas con BCrypt.

## Estructura de paquetes

```
src/main/kotlin/pe/upeu/agrosierra/
├── config/       SecurityConfig.kt, JwtAuthenticationFilter.kt, JwtService.kt,
│                 ManejadorDeErrores.kt (+ EntryPoint401 y AccesoDenegado403), DatosSemilla.kt
├── seguridad/    Usuario.kt, Rol.kt, Privilegio.kt, UsuarioRepository.kt, AuthController.kt
├── dominio/      Lote.kt, Labor.kt (+ TipoLabor), Excepciones.kt
├── repositorio/  LoteRepository, LaborRepository (Spring Data JPA)
├── servicio/     LoteService (RN-03), LaborService (RN-01, RN-02, RN-04) — transaccionales
├── web/          LoteController, LaborController, UsuarioController (bajo /api/v1)
├── dto/          Pagina (envoltorio), ErrorRespuesta (formato común), peticiones y respuestas
└── Aplicacion.kt
```

## Contrato del API

| Verbo  | Ruta                                       | Privilegio      | Códigos                 |
|--------|--------------------------------------------|-----------------|-------------------------|
| POST   | /api/v1/auth/login                         | Público         | 200 · 401               |
| GET    | /api/v1/lotes?pagina&tamanio&valle&estado  | LOTE_LEER       | 200 · 401               |
| GET    | /api/v1/lotes/{id}                         | LOTE_LEER       | 200 · 401 · 404         |
| GET    | /api/v1/lotes/{id}/labores?pagina&tamanio  | LABOR_LEER      | 200 · 401 · 404         |
| POST   | /api/v1/labores                            | LABOR_CREAR     | 201 · 400 · 401 · 403 · 409 |
| PUT    | /api/v1/labores/{id}                       | LABOR_EDITAR    | 200 · 400 · 401 · 403 · 404 · 409 |
| DELETE | /api/v1/labores/{id}                       | LABOR_ELIMINAR  | 204 · 401 · 403 · 404   |
| POST   | /api/v1/lotes/{id}/cerrar-campania         | CAMPANIA_CERRAR | 200 · 401 · 403 · 404 · 409 |
| GET    | /api/v1/usuarios/me                        | Autenticado     | 200 · 401               |

`estado` admite `ABIERTA` o `CERRADA`. Listados con el envoltorio
`{contenido, pagina, tamanio, totalElementos, totalPaginas, ultima}`; errores con
`{timestamp, status, error, message, path, validationErrors}`.

Ejemplo de login:

```json
POST /api/v1/auth/login
{ "usuario": "tecnico1", "contrasena": "Agro2026*" }
→ 200 { "token": "eyJ...", "expiraEn": 1793100000000, "usuario": "tecnico1",
        "nombre": "Carlos Quispe", "rol": "TECNICO", "privilegios": ["LABOR_CREAR", ...] }
```

La colección de Postman está en `postman/AgroAPI.postman_collection.json` (incluye los ejemplos 401, 403 y 400).

## Roles y privilegios

| Privilegio          | TECNICO           | JEFE_CAMPO | ADMIN |
|---------------------|-------------------|------------|-------|
| LOTE_LEER           | Sí                | Sí         | Sí    |
| LABOR_LEER          | Sí                | Sí         | Sí    |
| LABOR_CREAR         | Sí                | Sí         | No    |
| LABOR_EDITAR        | Solo las propias  | Todas      | No    |
| LABOR_EDITAR_TODAS* | No                | Sí         | No    |
| LABOR_ELIMINAR      | No                | Sí         | No    |
| CAMPANIA_CERRAR     | No                | Sí         | No    |
| USUARIO_GESTIONAR   | No                | No         | Sí    |

\* `LABOR_EDITAR_TODAS` expresa el alcance «Todas» de la matriz como privilegio, para que la regla
RN-02 se decida por privilegio y **nunca** comparando el nombre del rol.

## Reglas de negocio (capa `servicio/`)

- **RN-01** Lote con campaña cerrada: no se registra ni corrige → 409.
- **RN-02** Sin `LABOR_EDITAR_TODAS`, solo se corrigen labores propias y dentro de 24 h → 403.
- **RN-03** Cerrar campaña exige al menos una labor COSECHA → 409.
- **RN-04** Fumigación: cantidad ≤ dosisMaximaPorHectarea × hectáreas → 400 con `validationErrors.cantidad`.

## Decisiones de arquitectura

1. **La seguridad vive en el servicio.** `SecurityConfig` exige un privilegio por ruta (`hasAuthority`);
   un usuario autenticado sin privilegio recibe 403 aunque la app oculte el botón.
2. **Rol = conjunto de privilegios en la base** (`rol_privilegio`). Agregar un rol nuevo es solo un dato
   semilla: ni el filtro ni los controladores tienen `if` con nombres de rol.
3. **JWT sin estado**: el token lleva `sub` (id), `usuario`, `nombre` y `privilegios`, firmado con HS256;
   `JwtAuthenticationFilter` verifica firma y vigencia; si falla, `EntryPoint401` responde 401.
4. **Controladores delgados**: las reglas RN-01..RN-04 están en `LoteService`/`LaborService` (transaccionales).
5. **Errores uniformes**: `ManejadorDeErrores` traduce cada excepción al formato común.
6. **Datos semilla** (`DatosSemilla`): 3 usuarios, 8 lotes en 3 valles (L-007 y L-008 cerrados),
   20 labores; L-001 tiene COSECHA (se puede cerrar) y L-002 no (demuestra el 409 de RN-03).
