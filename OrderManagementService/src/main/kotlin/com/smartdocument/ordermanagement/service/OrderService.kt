package com.smartdocument.ordermanagement.service

import com.smartdocument.ordermanagement.model.*
import com.smartdocument.ordermanagement.repository.OrderRepository
import com.smartdocument.ordermanagement.client.BookClient
import com.smartdocument.ordermanagement.exception.OrderManagementServiceException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val bookClient: BookClient
) {

    fun getOrderById(id: Long): Order = orderRepository.findById(id)
        .orElseThrow { NoSuchElementException("Order not found with id: $id") }

    fun getOrdersByCustomerId(customerId: String): List<Order> =
        orderRepository.findByCustomerId(customerId)

    fun getOrdersByCustomerIdAndStatus(customerId: String, status: OrderStatus): List<Order> =
        orderRepository.findByCustomerIdAndStatus(customerId, status)

    @Transactional
    fun createOrder(order: Order): Order {
        // Validate each order item
        order.orderItems.forEach { item ->
            val book = bookClient.getBookById(item.bookId)
                ?: throw OrderManagementServiceException(OrderManagementServiceException.Operation.INVALID_CART_ITEM)
            if (item.quantity > book.quantity) {
                throw OrderManagementServiceException(OrderManagementServiceException.Operation.INVALID_CART_QUANTITY)
            }
        }
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
