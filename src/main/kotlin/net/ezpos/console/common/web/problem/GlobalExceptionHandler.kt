package net.ezpos.console.common.web.problem

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import net.ezpos.console.common.exception.AuthenticationFailedException
import net.ezpos.console.common.exception.BusinessRuleException
import net.ezpos.console.common.exception.DataIntegrityException
import net.ezpos.console.common.exception.EntityAlreadyExistsException
import net.ezpos.console.common.exception.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    // ── 业务异常 ──────────────────────────────────────────

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFound(ex: EntityNotFoundException, request: HttpServletRequest): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetailSupport.basic(HttpStatus.NOT_FOUND, ex.message, request)
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(pd)
    }

    @ExceptionHandler(EntityAlreadyExistsException::class)
    fun handleEntityAlreadyExists(ex: EntityAlreadyExistsException, request: HttpServletRequest): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetailSupport.basic(HttpStatus.CONFLICT, ex.message, request)
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(pd)
    }

    @ExceptionHandler(BusinessRuleException::class)
    fun handleBusinessRule(ex: BusinessRuleException, request: HttpServletRequest): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetailSupport.basic(HttpStatus.BAD_REQUEST, ex.message, request)
        return ResponseEntity.badRequest()
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(pd)
    }

    @ExceptionHandler(AuthenticationFailedException::class)
    fun handleAuthenticationFailed(ex: AuthenticationFailedException, request: HttpServletRequest): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetailSupport.basic(HttpStatus.UNAUTHORIZED, ex.message, request)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(pd)
    }

    @ExceptionHandler(DataIntegrityException::class)
    fun handleDataIntegrity(ex: DataIntegrityException, request: HttpServletRequest): ResponseEntity<ProblemDetail> {
        log.error("Data integrity error on {}: {}", request.requestURI, ex.message)
        val pd = ProblemDetailSupport.basic(HttpStatus.INTERNAL_SERVER_ERROR, ex.message, request)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(pd)
    }

    // ── Spring 框架异常 ──────────────────────────────────

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(ex: ResponseStatusException, request: HttpServletRequest): ResponseEntity<ProblemDetail> {
        val status = ex.statusCode
        val pd = ProblemDetailSupport.basic(status, ex.reason, request)
        return ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(pd)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetailSupport.basic(HttpStatus.BAD_REQUEST, "Validation failed", request)
        val errors = ex.bindingResult.fieldErrors.map {
            mapOf(
                "field" to it.field,
                "message" to (it.defaultMessage ?: "Invalid value"),
            )
        }
        pd.setProperty("errors", errors)
        return ResponseEntity.badRequest()
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(pd)
    }

    @ExceptionHandler(BindException::class)
    fun handleBindException(ex: BindException, request: HttpServletRequest): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetailSupport.basic(HttpStatus.BAD_REQUEST, "Validation failed", request)
        val errors = ex.bindingResult.fieldErrors.map {
            mapOf(
                "field" to it.field,
                "message" to (it.defaultMessage ?: "Invalid value"),
            )
        }
        pd.setProperty("errors", errors)
        return ResponseEntity.badRequest()
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(pd)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(ex: ConstraintViolationException, request: HttpServletRequest): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetailSupport.basic(HttpStatus.BAD_REQUEST, "Validation failed", request)
        val errors = ex.constraintViolations.map {
            mapOf(
                "path" to it.propertyPath.toString(),
                "message" to it.message,
            )
        }
        pd.setProperty("errors", errors)
        return ResponseEntity.badRequest()
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(pd)
    }

    @ExceptionHandler(
        MethodArgumentTypeMismatchException::class,
        HttpMessageNotReadableException::class,
        IllegalArgumentException::class,
    )
    fun handleBadRequest(ex: Exception, request: HttpServletRequest): ResponseEntity<ProblemDetail> {
        val detail = when (ex) {
            is HttpMessageNotReadableException -> "Malformed request body"
            is MethodArgumentTypeMismatchException -> "Invalid parameter: ${ex.name}"
            else -> ex.message ?: "Bad request"
        }
        val pd = ProblemDetailSupport.basic(HttpStatus.BAD_REQUEST, detail, request)
        return ResponseEntity.badRequest()
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(pd)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException, request: HttpServletRequest): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetailSupport.basic(HttpStatus.FORBIDDEN, ex.message ?: "Access is denied", request)
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(pd)
    }

    // ── 兜底 ─────────────────────────────────────────────

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception, request: HttpServletRequest): ResponseEntity<ProblemDetail> {
        log.error("Unexpected error on {}", request.requestURI, ex)
        val pd = ProblemDetailSupport.basic(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Unexpected error",
            request,
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(pd)
    }
}
