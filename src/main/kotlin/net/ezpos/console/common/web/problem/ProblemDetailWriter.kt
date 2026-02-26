package net.ezpos.console.common.web.problem

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.stereotype.Component

@Component
class ProblemDetailWriter(
    private val objectMapper: ObjectMapper,
) {
    fun write(response: HttpServletResponse, problem: ProblemDetail) {
        if (response.isCommitted) return

        response.status = problem.status ?: HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        response.characterEncoding = Charsets.UTF_8.name()
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        response.writer.use { it.write(objectMapper.writeValueAsString(problem)) }
    }
}

