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

/**
 * Service class responsible for managing order operations in the Order Management Service.
 *
 * This service handles all order-related business logic including:
 * - Order creation with inventory validation
 * - Order retrieval and filtering
 * - Order status updates and lifecycle management
 * - Order cancellation with business rule enforcement
 *
 * The service integrates with:
 * - [BookClient] for inventory validation during order creation
 * - [OrderRepository] for data persistence
 *
 * All operations are transactional to ensure data consistency.
 *
 * @property orderRepository Repository for order data persistence
 * @property bookClient Client for communicating with BookInventoryService
 */
@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val bookClient: BookClient
) {

    private val logger: Logger = LoggerFactory.getLogger(OrderService::class.java)

    /**
     * Retrieves an order by its unique identifier.
     *
     * @param id The unique identifier of the order
     * @return The order with the specified ID
     * @throws NoSuchElementException if the order is not found
     */
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

    /**
     * Retrieves all orders for a specific customer.
     *
     * @param customerId The unique identifier of the customer
     * @return List of orders for the customer, ordered by creation date (newest first)
     */
    fun getOrdersByCustomerId(customerId: String): List<Order> {
        logger.debug("Getting orders for customer: {}", customerId)
        val orders = orderRepository.findByCustomerId(customerId)
        logger.info("Found {} orders for customer: {}", orders.size, customerId)
        return orders
    }

    /**
     * Retrieves orders for a specific customer filtered by order status.
     *
     * This method is useful for tracking orders in different stages of the lifecycle.
     *
     * @param customerId The unique identifier of the customer
     * @param status The order status to filter by
     * @return List of orders matching the customer and status criteria
     */
    fun getOrdersByCustomerIdAndStatus(customerId: String, status: OrderStatus): List<Order> {
        logger.debug("Getting orders for customer: {} with status: {}", customerId, status)
        val orders = orderRepository.findByCustomerIdAndStatus(customerId, status)
        logger.info("Found {} orders for customer: {} with status: {}", orders.size, customerId, status)
        return orders
    }

    /**
     * Creates a new order with comprehensive validation.
     *
     * This method performs the following validations:
     * - Ensures all order items reference valid books
     * - Validates that quantities are at least 1
     * - Checks that requested quantities don't exceed available inventory
     * - Sets initial order status to PENDING
     * - Sets creation and update timestamps
     *
     * @param order The order to create
     * @return The created order with generated ID and timestamps
     * @throws OrderManagementServiceException if validation fails
     */
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

    /**
     * Updates the status of an existing order.
     *
     * This method allows tracking the order through its lifecycle:
     * - PENDING → CONFIRMED (after payment)
     * - CONFIRMED → SHIPPED (when shipped)
     * - SHIPPED → DELIVERED (when delivered)
     * - Any status → CANCELLED (if cancelled)
     *
     * @param id The unique identifier of the order
     * @param status The new status for the order
     * @return The updated order
     * @throws NoSuchElementException if the order is not found
     */
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

    /**
     * Cancels an existing order with business rule enforcement.
     *
     * Business rules:
     * - Delivered orders cannot be cancelled
     * - Already cancelled orders can be "cancelled" again (updates timestamp)
     * - All other orders can be cancelled
     *
     * @param id The unique identifier of the order to cancel
     * @return The cancelled order
     * @throws IllegalStateException if the order is delivered and cannot be cancelled
     * @throws NoSuchElementException if the order is not found
     */
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
