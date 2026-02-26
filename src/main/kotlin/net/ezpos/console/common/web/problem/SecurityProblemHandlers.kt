package net.ezpos.console.common.web.problem

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

@Component
class ProblemDetailAuthenticationEntryPoint(
    private val writer: ProblemDetailWriter,
) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        val pd = ProblemDetailSupport.basic(
            HttpStatus.UNAUTHORIZED,
            "Unauthorized",
            request,
        )
        writer.write(response, pd)
    }
}

@Component
class ProblemDetailAccessDeniedHandler(
    private val writer: ProblemDetailWriter,
) : AccessDeniedHandler {
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        val pd = ProblemDetailSupport.basic(
            HttpStatus.FORBIDDEN,
            "Forbidden",
            request,
        )
        writer.write(response, pd)
    }
}

