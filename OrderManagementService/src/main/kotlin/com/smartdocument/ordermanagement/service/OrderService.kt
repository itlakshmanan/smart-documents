package com.smartdocument.ordermanagement.service

import com.smartdocument.ordermanagement.model.*
import com.smartdocument.ordermanagement.repository.OrderRepository
import com.smartdocument.ordermanagement.client.BookClient
import com.smartdocument.ordermanagement.exception.OrderManagementServiceException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val bookClient: BookClient
) {

    private val logger: Logger = LoggerFactory.getLogger(OrderService::class.java)

    fun getOrderById(id: Long): Order {
        logger.debug("Getting order by ID: {}", id)
        val order = orderRepository.findById(id)
            .orElseThrow {
                logger.warn("Order not found with ID: {}", id)
                NoSuchElementException("Order not found with id: $id")
            }
        logger.debug("Found order: {} for customer: {} with status: {}", id, order.customerId, order.status)
        return order
    }

    fun getOrdersByCustomerId(customerId: String): List<Order> {
        logger.debug("Getting orders for customer: {}", customerId)
        val orders = orderRepository.findByCustomerId(customerId)
        logger.info("Found {} orders for customer: {}", orders.size, customerId)
        return orders
    }

    fun getOrdersByCustomerIdAndStatus(customerId: String, status: OrderStatus): List<Order> {
        logger.debug("Getting orders for customer: {} with status: {}", customerId, status)
        val orders = orderRepository.findByCustomerIdAndStatus(customerId, status)
        logger.info("Found {} orders for customer: {} with status: {}", orders.size, customerId, status)
        return orders
    }

    @Transactional
    fun createOrder(order: Order): Order {
        logger.info("Creating new order for customer: {} with {} items, total: {}",
                   order.customerId, order.orderItems.size, order.totalAmount)

        // Validate each order item
        logger.debug("Validating {} order items", order.orderItems.size)
        order.orderItems.forEach { item ->
            logger.debug("Validating order item - bookId: {}, quantity: {}", item.bookId, item.quantity)

            val book = bookClient.getBookById(item.bookId)
                ?: throw OrderManagementServiceException(OrderManagementServiceException.Operation.INVALID_CART_ITEM)

            logger.debug("Retrieved book: {} with available quantity: {}", book.id, book.quantity)

            if (item.quantity < 1) {
                logger.warn("Invalid quantity in order item: {} for book: {}", item.quantity, item.bookId)
                throw OrderManagementServiceException(OrderManagementServiceException.Operation.INVALID_CART_QUANTITY)
            }

            if (item.quantity > book.quantity) {
                logger.warn("Insufficient stock for order item - book: {}, requested: {}, available: {}",
                           item.bookId, item.quantity, book.quantity)
                throw OrderManagementServiceException(OrderManagementServiceException.Operation.INVALID_CART_QUANTITY)
            }
        }

        order.status = OrderStatus.PENDING
        order.createdAt = LocalDateTime.now()
        order.updatedAt = LocalDateTime.now()

        val savedOrder = orderRepository.save(order)
        logger.info("Successfully created order: {} for customer: {} with status: {}",
                   savedOrder.id, savedOrder.customerId, savedOrder.status)
        return savedOrder
    }

    @Transactional
    fun updateOrderStatus(id: Long, status: OrderStatus): Order {
        logger.info("Updating order status - orderId: {}, new status: {}", id, status)

        val order = getOrderById(id)
        val oldStatus = order.status
        order.status = status
        order.updatedAt = LocalDateTime.now()

        val updatedOrder = orderRepository.save(order)
        logger.info("Successfully updated order status - orderId: {}, old status: {}, new status: {}",
                   id, oldStatus, status)
        return updatedOrder
    }

    @Transactional
    fun cancelOrder(id: Long): Order {
        logger.info("Cancelling order: {}", id)

        val order = getOrderById(id)
        logger.debug("Order current status: {}", order.status)

        if (order.status == OrderStatus.DELIVERED) {
            logger.warn("Cannot cancel delivered order: {}", id)
            throw IllegalStateException("Cannot cancel a delivered order")
        }

        if (order.status == OrderStatus.CANCELLED) {
            logger.info("Order is already cancelled: {}, updating timestamp and saving", id)
            order.updatedAt = LocalDateTime.now()
            val savedOrder = orderRepository.save(order)
            return savedOrder
        }

        order.status = OrderStatus.CANCELLED
        order.updatedAt = LocalDateTime.now()

        val cancelledOrder = orderRepository.save(order)
        logger.info("Successfully cancelled order: {} for customer: {}", id, order.customerId)
        return cancelledOrder
    }
}
