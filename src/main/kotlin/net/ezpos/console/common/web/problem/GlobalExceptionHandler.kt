package net.ezpos.console.common.web.problem

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.converter.HttpMessageNotReadableException

@RestControllerAdvice
class GlobalExceptionHandler {
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

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception, request: HttpServletRequest): ResponseEntity<ProblemDetail> {
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

