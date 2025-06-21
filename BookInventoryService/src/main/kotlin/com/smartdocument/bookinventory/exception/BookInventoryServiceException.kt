package com.smartdocument.bookinventory.exception

import org.springframework.http.HttpStatus

/**
 * Base exception class for all book inventory service related exceptions.
 * This exception can be used to handle various book inventory related error scenarios.
 *
 * @property operation The operation that caused the error
 */
class BookInventoryServiceException(
    val operation: Operation,
    cause: Throwable? = null
) : Exception(operation.message, cause) {

    enum class Operation(val httpStatus: HttpStatus = HttpStatus.NOT_FOUND, val message: String) {
        ISBN_ALREADY_EXISTS(HttpStatus.CONFLICT, "ISBN already exists"),
        NEGATIVE_QUANTITY(HttpStatus.BAD_REQUEST, "Quantity must be zero or positive")
    }
}
