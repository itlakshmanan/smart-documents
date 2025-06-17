package com.smartdocument.ordermanagement.service

import com.smartdocument.ordermanagement.model.*
import com.smartdocument.ordermanagement.repository.OrderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class OrderService(private val orderRepository: OrderRepository) {

    fun getOrderById(id: Long): Order = orderRepository.findById(id)
        .orElseThrow { NoSuchElementException("Order not found with id: $id") }

    fun getOrdersByCustomerId(customerId: String): List<Order> =
        orderRepository.findByCustomerId(customerId)

    fun getOrdersByCustomerIdAndStatus(customerId: String, status: OrderStatus): List<Order> =
        orderRepository.findByCustomerIdAndStatus(customerId, status)

    @Transactional
    fun createOrder(order: Order): Order {
        order.status = OrderStatus.PENDING
        return orderRepository.save(order)
    }

    @Transactional
    fun updateOrderStatus(id: Long, status: OrderStatus): Order {
        val order = getOrderById(id)
        order.status = status
        order.updatedAt = LocalDateTime.now()
        return orderRepository.save(order)
    }

    @Transactional
    fun cancelOrder(id: Long): Order {
        val order = getOrderById(id)
        if (order.status == OrderStatus.DELIVERED) {
            throw IllegalStateException("Cannot cancel a delivered order")
        }
        order.status = OrderStatus.CANCELLED
        order.updatedAt = LocalDateTime.now()
        return orderRepository.save(order)
    }
} 