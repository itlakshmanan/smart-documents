package com.smartdocument.ordermanagement.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import com.smartdocument.ordermanagement.exception.ErrorResponse
import com.smartdocument.ordermanagement.exception.OrderManagementServiceException

/**
 * Global exception handler for OrderManagementService.
 * Catches and handles common exceptions, returning a structured error response.
 */
@ControllerAdvice
class GlobalExceptionHandler {
    /**
     * Handles validation errors (e.g., @Valid failures).
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidationException(ex: MethodArgumentNotValidException): ErrorResponse {
        val errors = ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage ?: "Invalid value"}" }
        return ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            message = "Validation Failed",
            details = errors
        )
    }

    /**
     * Handles missing request parameters.
     */
    @ExceptionHandler(MissingServletRequestParameterException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleMissingParam(ex: MissingServletRequestParameterException): ErrorResponse =
        ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            message = ex.message ?: "Missing request parameter"
        )

    /**
     * Handles type mismatch errors (e.g., invalid enum values).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ErrorResponse =
        ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            message = ex.message ?: "Type mismatch"
        )

    /**
     * Handles not found errors (e.g., NoSuchElementException).
     */
    @ExceptionHandler(NoSuchElementException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: NoSuchElementException): ErrorResponse =
        ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            message = ex.message ?: "Resource not found"
        )

    /**
     * Handles illegal argument errors.
     */
    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgument(ex: IllegalArgumentException): ErrorResponse =
        ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            message = ex.message ?: "Bad request"
        )

    /**
     * Handles custom OrderManagementServiceException errors.
     */
    @ExceptionHandler(OrderManagementServiceException::class)
    fun handleOrderManagementServiceException(ex: OrderManagementServiceException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(ex.operation.httpStatus).body(
            ErrorResponse(
                status = ex.operation.httpStatus.value(),
                message = ex.operation.message
            )
        )

    /**
     * Handles all other uncaught exceptions.
     */
    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse(
                status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                message = ex.message ?: "Internal server error"
            )
        )
}
