package pe.upeu.agrosierra

import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AgroApiTest @Autowired constructor(
    private val mvc: MockMvc,
    private val mapper: ObjectMapper,
) {
    private fun token(usuario: String): String {
        val res = mvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"usuario":"$usuario","contrasena":"Agro2026*"}"""
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return mapper.readTree(res)["token"].asText()
    }

    private fun labor(loteId: Long, tipo: String = "RIEGO", cantidad: Double = 10.0) =
        """{"loteId":$loteId,"tipo":"$tipo","fecha":"2026-10-27","responsable":"Joan Rodriguez","cantidad":$cantidad,"observacion":"prueba"}"""

    @Test
    fun `login correcto devuelve token, nombre y privilegios`() {
        mvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"usuario":"jefe1","contrasena":"Agro2026*"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.token") { exists() }
            jsonPath("$.nombre") { value("Rosa Huaman") }
            jsonPath("$.privilegios") { value(hasItem("CAMPANIA_CERRAR")) }
        }
    }

    @Test
    fun `credenciales incorrectas responden 401`() {
        mvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"usuario":"jefe1","contrasena":"otra"}"""
        }.andExpect { status { isUnauthorized() }; jsonPath("$.status") { value(401) } }
    }

    @Test
    fun `sin token o con token alterado responde 401`() {
        mvc.get("/api/v1/lotes").andExpect { status { isUnauthorized() } }
        val alterado = token("tecnico1").dropLast(3) + "abc"
        mvc.get("/api/v1/lotes") { header("Authorization", "Bearer $alterado") }
            .andExpect { status { isUnauthorized() }; jsonPath("$.path") { value("/api/v1/lotes") } }
    }

    @Test
    fun `listado paginado con filtro por valle y estado`() {
        val t = token("tecnico1")
        mvc.get("/api/v1/lotes?pagina=0&tamanio=5") { header("Authorization", "Bearer $t") }.andExpect {
            status { isOk() }
            jsonPath("$.contenido.length()") { value(5) }
            jsonPath("$.totalElementos") { value(8) }
            jsonPath("$.totalPaginas") { value(2) }
            jsonPath("$.ultima") { value(false) }
        }
        mvc.get("/api/v1/lotes?valle=mantaro&estado=ABIERTA") { header("Authorization", "Bearer $t") }
            .andExpect { jsonPath("$.totalElementos") { value(3) } }
        mvc.get("/api/v1/lotes/1/labores") { header("Authorization", "Bearer $t") }
            .andExpect { jsonPath("$.contenido[0].tipo") { value("COSECHA") } } // mas reciente primero
    }

    @Test
    fun `tecnico sin privilegio recibe 403 al eliminar o cerrar campania`() {
        val t = token("tecnico1")
        mvc.delete("/api/v1/labores/1") { header("Authorization", "Bearer $t") }.andExpect { status { isForbidden() } }
        mvc.post("/api/v1/lotes/1/cerrar-campania") { header("Authorization", "Bearer $t") }.andExpect { status { isForbidden() } }
        val admin = token("admin")
        mvc.post("/api/v1/labores") {
            header("Authorization", "Bearer $admin"); contentType = MediaType.APPLICATION_JSON; content = labor(1)
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `registro 201, validacion 400, RN-01 409 y RN-04 400`() {
        val t = token("tecnico1")
        mvc.post("/api/v1/labores") {
            header("Authorization", "Bearer $t"); contentType = MediaType.APPLICATION_JSON; content = labor(1)
        }.andExpect { status { isCreated() }; jsonPath("$.id") { exists() } }

        mvc.post("/api/v1/labores") {
            header("Authorization", "Bearer $t"); contentType = MediaType.APPLICATION_JSON
            content = """{"loteId":1,"tipo":"RIEGO","fecha":"2026-10-27","responsable":"","cantidad":-1}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.validationErrors.responsable") { exists() }
            jsonPath("$.validationErrors.cantidad") { exists() }
        }
        // RN-01: L-007 (id 7) tiene la campania cerrada
        mvc.post("/api/v1/labores") {
            header("Authorization", "Bearer $t"); contentType = MediaType.APPLICATION_JSON; content = labor(7)
        }.andExpect { status { isConflict() } }
        // RN-04: L-001 2.5 ha x 4.0 = 10.0 maximo
        mvc.post("/api/v1/labores") {
            header("Authorization", "Bearer $t"); contentType = MediaType.APPLICATION_JSON; content = labor(1, "FUMIGACION", 10.5)
        }.andExpect { status { isBadRequest() }; jsonPath("$.validationErrors.cantidad") { exists() } }
    }

    @Test
    fun `RN-02 tecnico corrige solo lo propio dentro de 24 horas, jefe corrige todo`() {
        val t = token("tecnico1")
        // labor semilla registrada hace 3 dias -> 403
        mvc.put("/api/v1/labores/1") {
            header("Authorization", "Bearer $t"); contentType = MediaType.APPLICATION_JSON; content = labor(1)
        }.andExpect { status { isForbidden() } }
        // labor recien creada por el mismo tecnico -> 200
        val creada = mvc.post("/api/v1/labores") {
            header("Authorization", "Bearer $t"); contentType = MediaType.APPLICATION_JSON; content = labor(2)
        }.andReturn().response.contentAsString
        val id = mapper.readTree(creada)["id"].asLong()
        mvc.put("/api/v1/labores/$id") {
            header("Authorization", "Bearer $t"); contentType = MediaType.APPLICATION_JSON; content = labor(2, cantidad = 20.0)
        }.andExpect { status { isOk() }; jsonPath("$.cantidad") { value(20.0) } }
        // jefe corrige la labor semilla -> 200; eliminar -> 204; de nuevo -> 404
        val j = token("jefe1")
        mvc.put("/api/v1/labores/1") {
            header("Authorization", "Bearer $j"); contentType = MediaType.APPLICATION_JSON; content = labor(1)
        }.andExpect { status { isOk() } }
        mvc.delete("/api/v1/labores/$id") { header("Authorization", "Bearer $j") }
            .andExpect { status { isNoContent() }; content { string("") } }
        mvc.delete("/api/v1/labores/$id") { header("Authorization", "Bearer $j") }.andExpect { status { isNotFound() } }
    }

    @Test
    fun `RN-03 cerrar campania exige una cosecha`() {
        val j = token("jefe1")
        mvc.post("/api/v1/lotes/2/cerrar-campania") { header("Authorization", "Bearer $j") }
            .andExpect { status { isConflict() } }
        mvc.post("/api/v1/lotes/1/cerrar-campania") { header("Authorization", "Bearer $j") }
            .andExpect { status { isOk() }; jsonPath("$.campaniaCerrada") { value(true) } }
        mvc.get("/api/v1/lotes/999") { header("Authorization", "Bearer $j") }.andExpect { status { isNotFound() } }
    }

    @Test
    fun `usuarios me devuelve los privilegios del token`() {
        val t = token("tecnico1")
        mvc.get("/api/v1/usuarios/me") { header("Authorization", "Bearer $t") }.andExpect {
            status { isOk() }
            jsonPath("$.rol") { value("TECNICO") }
            jsonPath("$.privilegios") { value(not(hasItem("LABOR_ELIMINAR"))) }
        }
    }
}
