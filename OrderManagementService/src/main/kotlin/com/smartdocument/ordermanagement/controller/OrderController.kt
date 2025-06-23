package com.smartdocument.ordermanagement.controller

import com.smartdocument.ordermanagement.model.Order
import com.smartdocument.ordermanagement.model.OrderStatus
import com.smartdocument.ordermanagement.service.OrderService
import com.smartdocument.ordermanagement.dto.OrderResponseDto
import com.smartdocument.ordermanagement.mapper.OrderMapper
import com.smartdocument.ordermanagement.exception.OrderManagementServiceException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * REST controller for managing customer orders.
 *
 * This controller provides endpoints for order-related operations including:
 * - Retrieving order details by order ID
 * - Updating order status (e.g., PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)
 *
 * Orders are created through the cart checkout process and represent completed
 * purchases. Each order contains order items with book details, quantities,
 * and pricing information captured at the time of purchase.
 *
 * Business rules enforced:
 * - Orders are immutable after creation (except status updates)
 * - Order status transitions follow predefined workflow
 * - Order details include historical pricing information
 * - All order operations require valid order ID
 *
 * Authentication is required for all endpoints using HTTP Basic Authentication.
 *
 * @property orderService Service layer for order business logic
 * @property orderMapper Mapper for converting between order entities and DTOs
 */
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order Management", description = "APIs for managing orders")
@SecurityRequirement(name = "basicAuth")
class OrderController(
    private val orderService: OrderService,
    private val orderMapper: OrderMapper
) {

    private val logger: Logger = LoggerFactory.getLogger(OrderController::class.java)

    /**
     * Retrieves order details by order ID.
     *
     * Returns complete order information including all order items,
     * customer details, order status, timestamps, and total amounts.
     * The response includes historical pricing information captured
     * at the time of order creation.
     *
     * @param orderId Unique identifier for the order
     * @return OrderResponseDto containing complete order details
     * @throws com.smartdocument.ordermanagement.exception.OrderManagementServiceException if order ID is invalid or order not found
     */
    @GetMapping("/{orderId}")
    fun getOrder(@PathVariable orderId: String): ResponseEntity<OrderResponseDto> {
        logger.info("Getting order: {}", orderId)
        val order = orderService.getOrderById(orderId.toLong())
        val orderResponse = orderMapper.toOrderResponseDto(order)
        return ResponseEntity.ok(orderResponse)
    }

    /**
     * Retrieves all orders for a specific customer.
     *
     * Returns a list of all orders associated with the specified customer ID,
     * including order details, status, timestamps, and total amounts. Orders
     * are returned in the order they were created (typically newest first).
     * This endpoint is useful for displaying a customer's complete order history.
     *
     * The response includes:
     * - All orders for the customer regardless of status
     * - Complete order details with historical pricing information
     * - Order items and their associated data
     * - Creation and update timestamps
     *
     * @param customerId Unique identifier for the customer
     * @return List of OrderResponseDto objects containing all customer orders
     */
    @GetMapping("/customer/{customerId}")
    fun getOrdersByCustomerId(@PathVariable customerId: String): ResponseEntity<List<OrderResponseDto>> {
        logger.info("Getting all orders for customer: {}", customerId)
        val orders = orderService.getOrdersByCustomerId(customerId)
        val orderResponses = orderMapper.toOrderResponseDtoList(orders)
        logger.debug("Found {} orders for customer: {}", orderResponses.size, customerId)
        return ResponseEntity.ok(orderResponses)
    }

    /**
     * Updates the status of an existing order.
     *
     * Allows updating the order status to reflect the current state
     * of the order processing workflow. Valid status values include:
     * - PENDING: Order created but not yet confirmed
     * - CONFIRMED: Order confirmed and being processed
     * - SHIPPED: Order has been shipped to customer
     * - DELIVERED: Order has been delivered to customer
     * - CANCELLED: Order has been cancelled
     *
     * Status transitions are validated to ensure they follow the
     * proper workflow sequence.
     *
     * @param orderId Unique identifier for the order
     * @param request Request containing the new status value
     * @return OrderResponseDto with updated order details
     * @throws com.smartdocument.ordermanagement.exception.OrderManagementServiceException if order ID is invalid, order not found, invalid status transition, or request data is invalid
     */
    @PatchMapping("/{orderId}")
    fun updateOrderStatus(
        @PathVariable orderId: String,
        @RequestBody request: Map<String, String>
    ): ResponseEntity<OrderResponseDto> {
        val status = request["status"]
        if (status == null) {
            throw OrderManagementServiceException(
                OrderManagementServiceException.Operation.INVALID_REQUEST_DATA,
                Throwable("Missing required field: status")
            )
        }
        logger.info("Updating order status: {}, new status: {}", orderId, status)
        val order = orderService.updateOrderStatus(orderId.toLong(), OrderStatus.valueOf(status))
        val orderResponse = orderMapper.toOrderResponseDto(order)
        return ResponseEntity.ok(orderResponse)
    }
}
