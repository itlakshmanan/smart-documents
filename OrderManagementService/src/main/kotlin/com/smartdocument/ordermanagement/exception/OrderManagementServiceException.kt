package com.smartdocument.ordermanagement.exception

import org.springframework.http.HttpStatus

/**
 * Base exception class for all order management service related exceptions.
 *
 * This exception is used to handle various order management related error scenarios
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
class OrderManagementServiceException(
    val operation: Operation,
    cause: Throwable? = null
) : Exception(operation.message, cause) {

    /**
     * Enumeration of all possible order management operations that can fail.
     *
     * Each operation includes:
     * - An appropriate HTTP status code for REST API responses
     * - A descriptive error message for logging and user feedback
     *
     * The operations cover all major failure scenarios in the order management system:
     * - Data not found scenarios (404)
     * - Validation failures (400)
     * - Business rule violations (409)
     * - Payment processing failures (422)
     */
    enum class Operation(val httpStatus: HttpStatus = HttpStatus.NOT_FOUND, val message: String) {
        /** Order not found in the system */
        ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Order not found"),

        /** Cart not found for the specified customer */
        CART_NOT_FOUND(HttpStatus.NOT_FOUND, "Cart not found"),

        /** Attempted to checkout an empty cart */
        EMPTY_CART(HttpStatus.BAD_REQUEST, "Cannot checkout empty cart"),

        /** Insufficient inventory for the requested quantity */
        INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "Insufficient stock for order"),

        /** Failed to reserve inventory during order processing */
        INVENTORY_RESERVATION_FAILED(HttpStatus.CONFLICT, "Failed to reserve inventory"),

        /** Invalid order status transition attempted */
        INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "Invalid order status"),

        /** Payment processing failed */
        PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "Payment failed"),

        /** Invalid cart item (e.g., book not found) */
        INVALID_CART_ITEM(HttpStatus.BAD_REQUEST, "Invalid cart item"),

        /** Item not found in the customer's cart */
        ITEM_NOT_FOUND_IN_CART(HttpStatus.NOT_FOUND, "Item not found in cart"),

        /** Invalid quantity specified (must be at least 1) */
        INVALID_CART_QUANTITY(HttpStatus.BAD_REQUEST, "Quantity must be at least 1"),

        /** Invalid price information in cart item */
        INVALID_CART_PRICE(HttpStatus.BAD_REQUEST, "Invalid cart price"),

        /** Invalid request data (missing required fields, malformed data) */
        INVALID_REQUEST_DATA(HttpStatus.BAD_REQUEST, "Invalid request data")
    }
}
