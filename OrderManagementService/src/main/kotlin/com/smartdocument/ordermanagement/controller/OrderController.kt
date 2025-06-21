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
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order Management", description = "APIs for managing orders")
@SecurityRequirement(name = "basicAuth")
class OrderController(
    private val orderService: OrderService,
    private val orderMapper: OrderMapper
) {

    private val logger: Logger = LoggerFactory.getLogger(OrderController::class.java)

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order details")
    fun getOrder(
        @Parameter(description = "Unique identifier for the order")
        @PathVariable orderId: String
    ): ResponseEntity<OrderResponseDto> {
        logger.info("Getting order: {}", orderId)
        val order = orderService.getOrderById(orderId.toLong())
        val orderResponse = orderMapper.toOrderResponseDto(order)
        return ResponseEntity.ok(orderResponse)
    }

    @PatchMapping("/{orderId}")
    @Operation(summary = "Update order status")
    fun updateOrderStatus(
        @Parameter(description = "Unique identifier for the order")
        @PathVariable orderId: String,
        @RequestBody request: Map<String, String>
    ): ResponseEntity<OrderResponseDto> {
        val status = request["status"] ?: throw IllegalArgumentException("Status is required")
        logger.info("Updating order status: {}, new status: {}", orderId, status)
        val order = orderService.updateOrderStatus(orderId.toLong(), OrderStatus.valueOf(status))
        val orderResponse = orderMapper.toOrderResponseDto(order)
        return ResponseEntity.ok(orderResponse)
    }
}
