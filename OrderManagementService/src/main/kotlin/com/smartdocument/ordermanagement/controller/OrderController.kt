package com.smartdocument.ordermanagement.controller

import com.smartdocument.ordermanagement.model.Order
import com.smartdocument.ordermanagement.model.OrderStatus
import com.smartdocument.ordermanagement.service.OrderService
import com.smartdocument.ordermanagement.dto.OrderResponseDto
import com.smartdocument.ordermanagement.mapper.OrderMapper
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val orderService: OrderService,
    private val orderMapper: OrderMapper
) {

    @GetMapping("/{id}")
    fun getOrderById(@PathVariable id: Long): ResponseEntity<OrderResponseDto> =
        ResponseEntity.ok(orderMapper.toOrderResponseDto(orderService.getOrderById(id)))

    @GetMapping("/customer/{customerId}")
    fun getOrdersByCustomerId(@PathVariable customerId: String): ResponseEntity<List<OrderResponseDto>> =
        ResponseEntity.ok(orderMapper.toOrderResponseDtoList(orderService.getOrdersByCustomerId(customerId)))

    @GetMapping("/customer/{customerId}/status/{status}")
    fun getOrdersByCustomerIdAndStatus(
        @PathVariable customerId: String,
        @PathVariable status: OrderStatus
    ): ResponseEntity<List<OrderResponseDto>> =
        ResponseEntity.ok(orderMapper.toOrderResponseDtoList(orderService.getOrdersByCustomerIdAndStatus(customerId, status)))

    @PatchMapping("/{id}/status")
    fun updateOrderStatus(
        @PathVariable id: Long,
        @RequestParam status: OrderStatus
    ): ResponseEntity<OrderResponseDto> = ResponseEntity.ok(orderMapper.toOrderResponseDto(orderService.updateOrderStatus(id, status)))

    @PostMapping("/{id}/cancel")
    fun cancelOrder(@PathVariable id: Long): ResponseEntity<OrderResponseDto> =
        ResponseEntity.ok(orderMapper.toOrderResponseDto(orderService.cancelOrder(id)))
}
