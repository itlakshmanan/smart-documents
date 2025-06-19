package com.smartdocument.bookinventory.exception

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(BookInventoryServiceException::class)
    fun handleBookInventoryServiceException(
            ex: BookInventoryServiceException,
            request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse =
                ErrorResponse(
                        status = ex.operation.httpStatus.value(),
                        error = ex.operation.httpStatus.reasonPhrase,
                        message = ex.operation.message,
                        path = request.requestURI
                )
        return ResponseEntity.status(ex.operation.httpStatus).body(errorResponse)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleConstraintViolationException(
            ex: ConstraintViolationException,
            request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val errors =
                ex.constraintViolations
                        .map { violation -> "${violation.propertyPath}: ${violation.message}" }
                        .joinToString(", ")

        val errorResponse =
                ErrorResponse(
                        status = HttpStatus.BAD_REQUEST.value(),
                        error = "Validation Error",
                        message = errors,
                        path = request.requestURI
                )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(NoSuchElementException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNoSuchElementException(
            ex: NoSuchElementException,
            request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse =
                ErrorResponse(
                        status = HttpStatus.NOT_FOUND.value(),
                        error = "Not Found",
                        message = ex.message ?: "Resource not found",
                        path = request.requestURI
                )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgumentException(
            ex: IllegalArgumentException,
            request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse =
                ErrorResponse(
                        status = HttpStatus.BAD_REQUEST.value(),
                        error = "Bad Request",
                        message = ex.message ?: "Invalid input",
                        path = request.requestURI
                )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGlobalException(
            ex: Exception,
            request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse =
                ErrorResponse(
                        status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        error = "Internal Server Error",
                        message = "An unexpected error occurred",
                        path = request.requestURI
                )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }

    protected override fun handleMethodArgumentNotValid(
            ex: MethodArgumentNotValidException,
            headers: org.springframework.http.HttpHeaders,
            status: org.springframework.http.HttpStatusCode,
            request: org.springframework.web.context.request.WebRequest
    ): ResponseEntity<Any> {
        val errors =
                ex.bindingResult
                        .fieldErrors
                        .map { error: FieldError -> "${error.field}: ${error.defaultMessage}" }
                        .joinToString(", ")

        val errorResponse =
                ErrorResponse(
                        status = status.value(),
                        error = "Validation Error",
                        message = errors,
                        path =
                                (request as?
                                                org.springframework.web.context.request.ServletWebRequest)
                                        ?.request
                                        ?.requestURI
                                        ?: ""
                )
        return ResponseEntity.status(status.value()).body(errorResponse)
    }
}
