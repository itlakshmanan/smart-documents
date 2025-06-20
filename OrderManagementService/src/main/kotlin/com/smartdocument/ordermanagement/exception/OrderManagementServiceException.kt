package com.smartdocument.ordermanagement.exception

import org.springframework.http.HttpStatus

/**
 * Base exception class for all order management service related exceptions.
 * This exception can be used to handle various order management related error scenarios.
 *
 * @property operation The operation that caused the error
 */
class OrderManagementServiceException(
    val operation: Operation,
    cause: Throwable? = null
) : Exception(operation.message, cause) {
    enum class Operation(val httpStatus: HttpStatus = HttpStatus.NOT_FOUND, val message: String) {
        ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Order not found"),
        CART_NOT_FOUND(HttpStatus.NOT_FOUND, "Cart not found"),
        EMPTY_CART(HttpStatus.BAD_REQUEST, "Cannot checkout empty cart"),
        INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "Insufficient stock for order"),
        INVENTORY_RESERVATION_FAILED(HttpStatus.CONFLICT, "Failed to reserve inventory"),
        INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "Invalid order status"),
        PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "Payment failed"),
        INVALID_CART_ITEM(HttpStatus.BAD_REQUEST, "Invalid cart item"),
        ITEM_NOT_FOUND_IN_CART(HttpStatus.NOT_FOUND, "Item not found in cart"),
        INVALID_CART_QUANTITY(HttpStatus.BAD_REQUEST, "Quantity must be at least 1"),
        INVALID_CART_PRICE(HttpStatus.BAD_REQUEST, "Invalid cart price")
    }
}
