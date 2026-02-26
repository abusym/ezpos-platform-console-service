package net.ezpos.console.common.web.problem

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import java.net.URI

internal object ProblemDetailSupport {
    fun titleOf(status: HttpStatusCode): String =
        (status as? HttpStatus)?.reasonPhrase ?: "HTTP ${status.value()}"

    fun instanceOf(request: HttpServletRequest): URI =
        URI.create(request.requestURI ?: "/")

    fun basic(
        status: HttpStatusCode,
        detail: String?,
        request: HttpServletRequest,
    ): ProblemDetail {
        val pd = ProblemDetail.forStatusAndDetail(status, detail ?: titleOf(status))
        pd.title = titleOf(status)
        pd.type = URI.create("about:blank")
        pd.instance = instanceOf(request)
        return pd
    }
}

