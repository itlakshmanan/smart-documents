package com.smartdocument.ordermanagement.controller

import com.smartdocument.ordermanagement.model.Order
import com.smartdocument.ordermanagement.model.OrderStatus
import com.smartdocument.ordermanagement.service.OrderService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/orders")
class OrderController(private val orderService: OrderService) {

    @GetMapping("/{id}")
    fun getOrderById(@PathVariable id: Long): ResponseEntity<Order> =
        ResponseEntity.ok(orderService.getOrderById(id))

    @GetMapping("/customer/{customerId}")
    fun getOrdersByCustomerId(@PathVariable customerId: String): ResponseEntity<List<Order>> =
        ResponseEntity.ok(orderService.getOrdersByCustomerId(customerId))

    @GetMapping("/customer/{customerId}/status/{status}")
    fun getOrdersByCustomerIdAndStatus(
        @PathVariable customerId: String,
        @PathVariable status: OrderStatus
    ): ResponseEntity<List<Order>> =
        ResponseEntity.ok(orderService.getOrdersByCustomerIdAndStatus(customerId, status))

    @PostMapping
    fun createOrder(@RequestBody order: Order): ResponseEntity<Order> =
        ResponseEntity.ok(orderService.createOrder(order))

    @PatchMapping("/{id}/status")
    fun updateOrderStatus(
        @PathVariable id: Long,
        @RequestParam status: OrderStatus
    ): ResponseEntity<Order> = ResponseEntity.ok(orderService.updateOrderStatus(id, status))

    @PostMapping("/{id}/cancel")
    fun cancelOrder(@PathVariable id: Long): ResponseEntity<Order> =
        ResponseEntity.ok(orderService.cancelOrder(id))
} 