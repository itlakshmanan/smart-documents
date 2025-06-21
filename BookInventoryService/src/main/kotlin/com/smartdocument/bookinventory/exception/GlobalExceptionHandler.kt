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
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.context.request.WebRequest

/**
 * Global exception handler for BookInventoryService.
 *
 * This class provides centralized exception handling for all controllers
 * in the BookInventoryService. It catches and handles common exceptions,
 * returning structured error responses with appropriate HTTP status codes.
 *
 * The handler provides:
 * - Custom exception handling for BookInventoryServiceException
 * - Validation error handling for @Valid annotations
 * - Constraint violation handling for bean validation
 * - Standard exception handling for common scenarios
 * - Consistent error response format across all endpoints
 *
 * All exception handlers return ErrorResponse objects with:
 * - Appropriate HTTP status codes
 * - Descriptive error messages
 * - Request path information
 * - Timestamp of when the error occurred
 *
 * This ensures consistent error handling and provides meaningful
 * feedback to API consumers for debugging and user experience.
 */
@ControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {

    /**
     * Handles custom BookInventoryServiceException errors.
     *
     * Processes exceptions thrown by the service layer that contain
     * specific operation information and HTTP status codes. The handler
     * extracts the operation details and creates a standardized error response.
     *
     * @param ex The BookInventoryServiceException that was thrown
     * @param request The HTTP request that caused the exception
     * @return ResponseEntity with ErrorResponse and appropriate HTTP status
     */
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

    /**
     * Handles constraint violation exceptions from bean validation.
     *
     * Processes validation errors that occur when bean validation
     * constraints are violated. Extracts field-level validation messages
     * and creates a comprehensive error response with all validation failures.
     *
     * @param ex The ConstraintViolationException that was thrown
     * @param request The HTTP request that caused the exception
     * @return ResponseEntity with ErrorResponse and BAD_REQUEST status
     */
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

    /**
     * Handles not found errors (e.g., NoSuchElementException).
     *
     * Processes exceptions that occur when requested resources are not found
     * in the system. This typically happens when trying to retrieve, update,
     * or delete non-existent books.
     *
     * @param ex The NoSuchElementException that was thrown
     * @param request The HTTP request that caused the exception
     * @return ResponseEntity with ErrorResponse and NOT_FOUND status
     */
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

    /**
     * Handles illegal argument errors.
     *
     * Processes exceptions that occur when invalid arguments are passed
     * to methods. This includes cases where parameters don't meet
     * business logic requirements or are in an invalid format.
     *
     * @param ex The IllegalArgumentException that was thrown
     * @param request The HTTP request that caused the exception
     * @return ResponseEntity with ErrorResponse and BAD_REQUEST status
     */
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

    /**
     * Handles all other uncaught exceptions.
     *
     * Acts as a catch-all handler for any exceptions that weren't
     * handled by more specific exception handlers. This ensures that
     * no exceptions leak through to the client without proper handling.
     *
     * @param ex The Exception that was thrown
     * @param request The HTTP request that caused the exception
     * @return ResponseEntity with ErrorResponse and INTERNAL_SERVER_ERROR status
     */
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

    /**
     * Handles validation errors from @Valid annotations.
     *
     * Processes validation failures that occur when request bodies
     * don't meet the validation constraints defined in DTOs. Extracts
     * field-level validation messages and creates a detailed error response.
     *
     * @param ex The MethodArgumentNotValidException that was thrown
     * @param headers HTTP headers for the response
     * @param status HTTP status code for the response
     * @param request The web request that caused the exception
     * @return ResponseEntity with ErrorResponse and BAD_REQUEST status
     */
    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: org.springframework.http.HttpHeaders,
        status: org.springframework.http.HttpStatusCode,
        request: org.springframework.web.context.request.WebRequest
    ): ResponseEntity<Any> {
        val errors = ex.bindingResult.fieldErrors.map {
            "${it.field}: ${it.defaultMessage}"
        }
        val errorResponse = ErrorResponse(
            status = status.value(),
            message = "Validation failed",
            details = errors
        )
        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }

    /**
     * Handles malformed or missing request body.
     *
     * Processes exceptions that occur when the request body cannot be
     * parsed or is missing required fields. This typically happens with
     * malformed JSON or missing required request parameters.
     *
     * @param ex The HttpMessageNotReadableException that was thrown
     * @param headers HTTP headers for the response
     * @param status HTTP status code for the response
     * @param request The web request that caused the exception
     * @return ResponseEntity with ErrorResponse and BAD_REQUEST status
     */
    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: org.springframework.http.HttpHeaders,
        status: org.springframework.http.HttpStatusCode,
        request: org.springframework.web.context.request.WebRequest
    ): ResponseEntity<Any> {
        val errorResponse = ErrorResponse(
            status = status.value(),
            message = "Required fields are missing or invalid."
        )
        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }
}
