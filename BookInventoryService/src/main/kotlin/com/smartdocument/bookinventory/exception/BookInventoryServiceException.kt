package com.smartdocument.bookinventory.exception

import org.springframework.http.HttpStatus

/**
 * Base exception class for all book inventory service related exceptions.
 *
 * This exception is used to handle various book inventory related error scenarios
 * with standardized error codes, HTTP status codes, and descriptive messages.
 *
 * The exception includes:
 * - An operation enum that categorizes the type of error
 * - Associated HTTP status codes for proper REST API responses
 * - Descriptive error messages for debugging and user feedback
 * - Support for chaining with underlying exceptions
 *
 * @property operation The operation that caused the error, providing context about the failure
 * @property cause The underlying exception that caused this error (optional)
 */
class BookInventoryServiceException(
    val operation: Operation,
    cause: Throwable? = null
) : Exception(operation.message, cause) {

    /**
     * Enumeration of all possible book inventory operations that can fail.
     *
     * Each operation includes:
     * - An appropriate HTTP status code for REST API responses
     * - A descriptive error message for logging and user feedback
     *
     * The operations cover all major failure scenarios in the book inventory system:
     * - Data conflicts (409) for duplicate ISBNs
     * - Validation failures (400) for invalid quantities
     */
    enum class Operation(val httpStatus: HttpStatus = HttpStatus.NOT_FOUND, val message: String) {
        /** Book not found in the system */
        BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "Book not found"),

        /** Attempted to create a book with an ISBN that already exists */
        ISBN_ALREADY_EXISTS(HttpStatus.CONFLICT, "ISBN already exists"),

        /** Attempted to set a negative quantity for book inventory */
        NEGATIVE_QUANTITY(HttpStatus.BAD_REQUEST, "Quantity must be zero or positive"),

        /** Not enough stock to fulfill the request */
        INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "Insufficient stock for order")
    }
}
