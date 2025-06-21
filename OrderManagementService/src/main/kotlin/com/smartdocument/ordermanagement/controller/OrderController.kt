package com.smartdocument.ordermanagement.controller

import com.smartdocument.ordermanagement.model.Order
import com.smartdocument.ordermanagement.model.OrderStatus
import com.smartdocument.ordermanagement.service.OrderService
import com.smartdocument.ordermanagement.dto.OrderResponseDto
import com.smartdocument.ordermanagement.mapper.OrderMapper
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@RestController
@RequestMapping("/api/v1/orders")
@Tag(
    name = "Order Management",
    description = "APIs for managing orders including retrieval, status updates, and order lifecycle management."
)
class OrderController(
    private val orderService: OrderService,
    private val orderMapper: OrderMapper
) {

    private val logger: Logger = LoggerFactory.getLogger(OrderController::class.java)

    @GetMapping("/{id}")
    @Operation(
        summary = "Get order by ID",
        description = "Retrieves a specific order by its unique identifier. Returns detailed order information including items, status, and timestamps.",
        operationId = "getOrderById"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Order retrieved successfully",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = OrderResponseDto::class),
                        examples = [
                            ExampleObject(
                                name = "Confirmed order",
                                value = """
                                {
                                    "id": 1,
                                    "customerId": "customer123",
                                    "status": "CONFIRMED",
                                    "totalAmount": 59.97,
                                    "orderItems": [
                                        {
                                            "id": 1,
                                            "bookId": 1,
                                            "quantity": 2,
                                            "price": 19.99,
                                            "subtotal": 39.98
                                        },
                                        {
                                            "id": 2,
                                            "bookId": 2,
                                            "quantity": 1,
                                            "price": 19.99,
                                            "subtotal": 19.99
                                        }
                                    ],
                                    "createdAt": "2024-01-15T10:30:00",
                                    "updatedAt": "2024-01-15T10:30:05"
                                }
                                """
                            )
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Authentication required"
            ),
            ApiResponse(
                responseCode = "404",
                description = "Order not found",
                content = [
                    Content(
                        mediaType = "application/json",
                        examples = [
                            ExampleObject(
                                name = "Order not found",
                                value = """
                                {
                                    "status": 404,
                                    "error": "Not Found",
                                    "message": "Order not found with id: 999"
                                }
                                """
                            )
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun getOrderById(
        @Parameter(
            description = "Unique identifier for the order",
            example = "1",
            required = true
        )
        @PathVariable id: Long
    ): ResponseEntity<OrderResponseDto> {
        logger.info("GET /api/v1/orders/{} - Retrieving order by ID", id)

        val order = orderService.getOrderById(id)
        val response = orderMapper.toOrderResponseDto(order)

        logger.info("GET /api/v1/orders/{} - Successfully retrieved order - customer: {}, status: {}, total: {}",
                   id, response.customerId, response.status, response.totalAmount)

        return ResponseEntity.ok(response)
    }

    @GetMapping("/customer/{customerId}")
    @Operation(
        summary = "Get orders by customer ID",
        description = "Retrieves all orders for a specific customer. Returns a list of orders with their current status and details.",
        operationId = "getOrdersByCustomerId"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Orders retrieved successfully",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = Array<OrderResponseDto>::class),
                        examples = [
                            ExampleObject(
                                name = "Multiple orders",
                                value = """
                                [
                                    {
                                        "id": 1,
                                        "customerId": "customer123",
                                        "status": "CONFIRMED",
                                        "totalAmount": 59.97,
                                        "orderItems": [
                                            {
                                                "id": 1,
                                                "bookId": 1,
                                                "quantity": 2,
                                                "price": 19.99,
                                                "subtotal": 39.98
                                            }
                                        ],
                                        "createdAt": "2024-01-15T10:30:00",
                                        "updatedAt": "2024-01-15T10:30:05"
                                    },
                                    {
                                        "id": 2,
                                        "customerId": "customer123",
                                        "status": "PENDING",
                                        "totalAmount": 29.99,
                                        "orderItems": [
                                            {
                                                "id": 3,
                                                "bookId": 3,
                                                "quantity": 1,
                                                "price": 29.99,
                                                "subtotal": 29.99
                                            }
                                        ],
                                        "createdAt": "2024-01-16T14:20:00",
                                        "updatedAt": "2024-01-16T14:20:00"
                                    }
                                ]
                                """
                            ),
                            ExampleObject(
                                name = "No orders",
                                value = "[]"
                            )
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Authentication required"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun getOrdersByCustomerId(
        @Parameter(
            description = "Unique identifier for the customer",
            example = "customer123",
            required = true
        )
        @PathVariable customerId: String
    ): ResponseEntity<List<OrderResponseDto>> {
        logger.info("GET /api/v1/orders/customer/{} - Retrieving all orders for customer", customerId)

        val orders = orderService.getOrdersByCustomerId(customerId)
        val response = orderMapper.toOrderResponseDtoList(orders)

        logger.info("GET /api/v1/orders/customer/{} - Successfully retrieved {} orders", customerId, response.size)

        return ResponseEntity.ok(response)
    }

    @GetMapping("/customer/{customerId}/status/{status}")
    @Operation(
        summary = "Get orders by customer ID and status",
        description = "Retrieves orders for a specific customer filtered by order status. Useful for tracking orders in different stages.",
        operationId = "getOrdersByCustomerIdAndStatus"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Orders retrieved successfully",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = Array<OrderResponseDto>::class),
                        examples = [
                            ExampleObject(
                                name = "Pending orders",
                                value = """
                                [
                                    {
                                        "id": 2,
                                        "customerId": "customer123",
                                        "status": "PENDING",
                                        "totalAmount": 29.99,
                                        "orderItems": [
                                            {
                                                "id": 3,
                                                "bookId": 3,
                                                "quantity": 1,
                                                "price": 29.99,
                                                "subtotal": 29.99
                                            }
                                        ],
                                        "createdAt": "2024-01-16T14:20:00",
                                        "updatedAt": "2024-01-16T14:20:00"
                                    }
                                ]
                                """
                            )
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad request - Invalid status value",
                content = [
                    Content(
                        mediaType = "application/json",
                        examples = [
                            ExampleObject(
                                name = "Invalid status",
                                value = """
                                {
                                    "status": 400,
                                    "error": "Bad Request",
                                    "message": "Invalid order status: INVALID_STATUS"
                                }
                                """
                            )
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Authentication required"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun getOrdersByCustomerIdAndStatus(
        @Parameter(
            description = "Unique identifier for the customer",
            example = "customer123",
            required = true
        )
        @PathVariable customerId: String,
        @Parameter(
            description = "Order status to filter by",
            example = "PENDING",
            schema = Schema(allowableValues = ["PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"]),
            required = true
        )
        @PathVariable status: OrderStatus
    ): ResponseEntity<List<OrderResponseDto>> {
        logger.info("GET /api/v1/orders/customer/{}/status/{} - Retrieving orders for customer with status",
                   customerId, status)

        val orders = orderService.getOrdersByCustomerIdAndStatus(customerId, status)
        val response = orderMapper.toOrderResponseDtoList(orders)

        logger.info("GET /api/v1/orders/customer/{}/status/{} - Successfully retrieved {} orders",
                   customerId, status, response.size)

        return ResponseEntity.ok(response)
    }

    @PatchMapping("/{id}/status")
    @Operation(
        summary = "Update order status",
        description = """
            Updates the status of a specific order. This operation allows tracking the order through its lifecycle:
            - PENDING → CONFIRMED (after payment)
            - CONFIRMED → SHIPPED (when shipped)
            - SHIPPED → DELIVERED (when delivered)
            - Any status → CANCELLED (if cancelled)

            **Note**: Once an order is DELIVERED, it cannot be cancelled.
        """,
        operationId = "updateOrderStatus"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Order status updated successfully",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = OrderResponseDto::class),
                        examples = [
                            ExampleObject(
                                name = "Status updated to CONFIRMED",
                                value = """
                                {
                                    "id": 1,
                                    "customerId": "customer123",
                                    "status": "CONFIRMED",
                                    "totalAmount": 59.97,
                                    "orderItems": [
                                        {
                                            "id": 1,
                                            "bookId": 1,
                                            "quantity": 2,
                                            "price": 19.99,
                                            "subtotal": 39.98
                                        }
                                    ],
                                    "createdAt": "2024-01-15T10:30:00",
                                    "updatedAt": "2024-01-15T10:35:00"
                                }
                                """
                            )
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad request - Invalid status transition or order already cancelled",
                content = [
                    Content(
                        mediaType = "application/json",
                        examples = [
                            ExampleObject(
                                name = "Invalid status transition",
                                value = """
                                {
                                    "status": 400,
                                    "error": "Bad Request",
                                    "message": "Cannot cancel a delivered order"
                                }
                                """
                            )
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Authentication required"
            ),
            ApiResponse(
                responseCode = "404",
                description = "Order not found"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun updateOrderStatus(
        @Parameter(
            description = "Unique identifier for the order",
            example = "1",
            required = true
        )
        @PathVariable id: Long,
        @Parameter(
            description = "New status for the order",
            example = "CONFIRMED",
            schema = Schema(allowableValues = ["PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"]),
            required = true
        )
        @RequestParam status: OrderStatus
    ): ResponseEntity<OrderResponseDto> {
        logger.info("PATCH /api/v1/orders/{}/status - Updating order status to {}", id, status)

        val order = orderService.updateOrderStatus(id, status)
        val response = orderMapper.toOrderResponseDto(order)

        logger.info("PATCH /api/v1/orders/{}/status - Successfully updated order status to {} for customer: {}",
                   id, status, response.customerId)

        return ResponseEntity.ok(response)
    }
}
