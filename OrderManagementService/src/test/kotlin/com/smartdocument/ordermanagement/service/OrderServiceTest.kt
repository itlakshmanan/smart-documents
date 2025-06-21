package com.smartdocument.ordermanagement.service

import com.smartdocument.ordermanagement.client.BookClient
import com.smartdocument.ordermanagement.dto.BookResponseDto
import com.smartdocument.ordermanagement.exception.OrderManagementServiceException
import com.smartdocument.ordermanagement.model.Order
import com.smartdocument.ordermanagement.model.OrderItem
import com.smartdocument.ordermanagement.model.OrderStatus
import com.smartdocument.ordermanagement.repository.OrderRepository
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.*

@Transactional
class OrderServiceTest {

    private lateinit var orderService: OrderService

    private val orderRepository: OrderRepository = mockk()
    private val bookClient: BookClient = mockk()

    @BeforeEach
    fun setUp() {
        orderService = OrderService(orderRepository, bookClient)
    }

    @Test
    fun `getOrderById should return existing order`() {
        // Given
        val orderId = 1L
        val order = createTestOrder("testuser1", listOf(), orderId)

        every { orderRepository.findById(orderId) } returns java.util.Optional.of(order)

        // When
        val result = orderService.getOrderById(orderId)

        // Then
        assertEquals(order, result)
        verify { orderRepository.findById(orderId) }
    }

    @Test
    fun `getOrderById should throw exception when order not found`() {
        // Given
        val orderId = 999L
        every { orderRepository.findById(orderId) } returns java.util.Optional.empty()

        // When & Then
        val exception = assertThrows<NoSuchElementException> {
            orderService.getOrderById(orderId)
        }
        assertEquals("Order not found with id: $orderId", exception.message)
        verify { orderRepository.findById(orderId) }
    }

    @Test
    fun `getOrdersByCustomerId should return customer orders`() {
        // Given
        val customerId = "testuser1"
        val order1 = createTestOrder(customerId, listOf(), 1L)
        val order2 = createTestOrder(customerId, listOf(), 2L)
        val orders = listOf(order1, order2)

        every { orderRepository.findByCustomerId(customerId) } returns orders

        // When
        val result = orderService.getOrdersByCustomerId(customerId)

        // Then
        assertEquals(orders, result)
        assertEquals(2, result.size)
        verify { orderRepository.findByCustomerId(customerId) }
    }

    @Test
    fun `createOrder should create order successfully`() {
        // Given
        val customerId = "testuser1"
        val bookId = 1L
        val quantity = 2
        val price = BigDecimal("19.99")
        val order = createTestOrder(customerId, listOf(), 0L, OrderStatus.PENDING)
        val orderItem = createTestOrderItem(order, bookId, quantity, price)
        order.orderItems.add(orderItem)
        val book = createTestBook(bookId, price, 10)

        every { bookClient.getBookById(bookId) } returns book
        every { orderRepository.save(any()) } answers { firstArg() }

        // When
        val result = orderService.createOrder(order)

        // Then
        assertEquals(OrderStatus.PENDING, result.status)
        assertEquals(1, result.orderItems.size)
        verify { bookClient.getBookById(bookId) }
        verify { orderRepository.save(any()) }
    }

    @Test
    fun `updateOrderStatus should update order status successfully`() {
        // Given
        val orderId = 1L
        val newStatus = OrderStatus.CONFIRMED
        val order = createTestOrder("testuser1", listOf(), orderId, OrderStatus.PENDING)

        every { orderRepository.findById(orderId) } returns java.util.Optional.of(order)
        every { orderRepository.save(any()) } answers { firstArg() }

        // When
        val result = orderService.updateOrderStatus(orderId, newStatus)

        // Then
        assertEquals(newStatus, result.status)
        assertNotNull(result.updatedAt)
        verify { orderRepository.findById(orderId) }
        verify { orderRepository.save(any()) }
    }

    @Test
    fun `cancelOrder should cancel order successfully`() {
        // Given
        val orderId = 1L
        val order = createTestOrder("testuser1", listOf(), orderId, OrderStatus.PENDING)

        every { orderRepository.findById(orderId) } returns java.util.Optional.of(order)
        every { orderRepository.save(any()) } answers { firstArg() }

        // When
        val result = orderService.cancelOrder(orderId)

        // Then
        assertEquals(OrderStatus.CANCELLED, result.status)
        assertNotNull(result.updatedAt)
        verify { orderRepository.findById(orderId) }
        verify { orderRepository.save(any()) }
    }

    @Test
    fun `cancelOrder should throw exception when order is delivered`() {
        // Given
        val orderId = 1L
        val order = createTestOrder("testuser1", listOf(), orderId, OrderStatus.DELIVERED)
        every { orderRepository.findById(orderId) } returns java.util.Optional.of(order)

        // When & Then
        val exception = assertThrows<OrderManagementServiceException> {
            orderService.cancelOrder(orderId)
        }
        assertEquals(OrderManagementServiceException.Operation.INVALID_ORDER_STATUS, exception.operation)
        verify { orderRepository.findById(orderId) }
    }

    @Test
    fun `createOrder should throw exception when one of multiple items has insufficient stock`() {
        // Given
        val customerId = "testuser1"
        val order = createTestOrder(customerId, listOf(), 0L, OrderStatus.PENDING)
        val book1 = createTestBook(1L, BigDecimal("19.99"), 10)
        val book2 = createTestBook(2L, BigDecimal("29.99"), 1)
        val orderItem1 = createTestOrderItem(order, 1L, 2, book1.price)
        val orderItem2 = createTestOrderItem(order, 2L, 5, book2.price) // Exceeds stock
        order.orderItems.addAll(listOf(orderItem1, orderItem2))

        every { bookClient.getBookById(1L) } returns book1
        every { bookClient.getBookById(2L) } returns book2

        // When & Then
        val exception = assertThrows<OrderManagementServiceException> {
            orderService.createOrder(order)
        }
        assertEquals(OrderManagementServiceException.Operation.INVALID_CART_QUANTITY, exception.operation)
        verify { bookClient.getBookById(1L) }
        verify { bookClient.getBookById(2L) }
    }

    @Test
    fun `createOrder should throw exception when item has negative quantity`() {
        // Given
        val customerId = "testuser1"
        val bookId = 1L
        val quantity = -5
        val price = BigDecimal("19.99")
        val order = createTestOrder(customerId, listOf(), 0L, OrderStatus.PENDING)
        val orderItem = createTestOrderItem(order, bookId, quantity, price)
        order.orderItems.add(orderItem)
        val book = createTestBook(bookId, price, 10)

        every { bookClient.getBookById(bookId) } returns book

        // When & Then
        val exception = assertThrows<OrderManagementServiceException> {
            orderService.createOrder(order)
        }
        assertEquals(OrderManagementServiceException.Operation.INVALID_CART_QUANTITY, exception.operation)
        verify { bookClient.getBookById(bookId) }
    }

    @Test
    fun `createOrder should handle item with zero price`() {
        // Given
        val customerId = "testuser1"
        val bookId = 1L
        val quantity = 2
        val price = BigDecimal.ZERO
        val order = createTestOrder(customerId, listOf(), 0L, OrderStatus.PENDING)
        val orderItem = createTestOrderItem(order, bookId, quantity, price)
        order.orderItems.add(orderItem)
        val book = createTestBook(bookId, price, 10)

        every { bookClient.getBookById(bookId) } returns book
        every { orderRepository.save(any()) } answers { firstArg() }

        // When
        val result = orderService.createOrder(order)

        // Then
        assertEquals(OrderStatus.PENDING, result.status)
        assertEquals(BigDecimal.ZERO, result.orderItems.first().price)
        verify { bookClient.getBookById(bookId) }
        verify { orderRepository.save(any()) }
    }

    @Test
    fun `createOrder should handle very large total amount`() {
        // Given
        val customerId = "testuser1"
        val bookId = 1L
        val quantity = 1_000_000
        val price = BigDecimal("999999.99")
        val order = createTestOrder(customerId, listOf(), 0L, OrderStatus.PENDING)
        val orderItem = createTestOrderItem(order, bookId, quantity, price)
        order.orderItems.add(orderItem)
        val book = createTestBook(bookId, price, quantity)

        every { bookClient.getBookById(bookId) } returns book
        every { orderRepository.save(any()) } answers { firstArg() }

        // When
        val result = orderService.createOrder(order)

        // Then
        assertEquals(OrderStatus.PENDING, result.status)
        assertEquals(quantity, result.orderItems.first().quantity)
        assertEquals(price, result.orderItems.first().price)
        verify { bookClient.getBookById(bookId) }
        verify { orderRepository.save(any()) }
    }

    @Test
    fun `createOrder should handle empty orderItems list`() {
        // Given
        val customerId = "testuser1"
        val order = createTestOrder(customerId, listOf(), 0L, OrderStatus.PENDING)
        every { orderRepository.save(any()) } answers { firstArg() }

        // When
        val result = orderService.createOrder(order)

        // Then
        assertEquals(OrderStatus.PENDING, result.status)
        assertTrue(result.orderItems.isEmpty())
        verify { orderRepository.save(any()) }
    }

    @Test
    fun `cancelOrder should allow cancelling already cancelled order`() {
        // Given
        val orderId = 1L
        val order = createTestOrder("testuser1", listOf(), orderId, OrderStatus.CANCELLED)
        every { orderRepository.findById(orderId) } returns java.util.Optional.of(order)
        every { orderRepository.save(any()) } answers { firstArg() }

        // When
        val result = orderService.cancelOrder(orderId)

        // Then
        assertEquals(OrderStatus.CANCELLED, result.status)
        verify { orderRepository.findById(orderId) }
        verify { orderRepository.save(any()) }
    }

    @Test
    fun `getOrdersByCustomerId should handle large number of orders`() {
        // Given
        val customerId = "testuser1"
        val orders = (1..1000).map { createTestOrder(customerId, listOf(), it.toLong()) }
        every { orderRepository.findByCustomerId(customerId) } returns orders

        // When
        val result = orderService.getOrdersByCustomerId(customerId)

        // Then
        assertEquals(1000, result.size)
        result.forEach { assertEquals(customerId, it.customerId) }
        verify { orderRepository.findByCustomerId(customerId) }
    }

    // Helper methods
    private fun createTestOrder(
        customerId: String,
        orderItems: List<OrderItem>,
        id: Long = 0L,
        status: OrderStatus = OrderStatus.PENDING
    ): Order {
        val order = Order(
            id = id,
            customerId = customerId,
            status = status,
            totalAmount = orderItems.sumOf { it.subtotal }
        )
        order.orderItems.addAll(orderItems)
        return order
    }

    private fun createTestOrderItem(
        order: Order,
        bookId: Long,
        quantity: Int,
        price: BigDecimal
    ): OrderItem {
        return OrderItem(
            order = order,
            bookId = bookId,
            quantity = quantity,
            price = price,
            subtotal = price.multiply(BigDecimal(quantity))
        )
    }

    private fun createTestBook(id: Long, price: BigDecimal, quantity: Int): BookResponseDto {
        return BookResponseDto(
            id = id,
            title = "Test Book $id",
            author = "Test Author",
            isbn = "1234567890",
            genre = "Fiction",
            price = price,
            quantity = quantity,
            description = "Test description",
            language = "English",
            publisher = "Test Publisher",
            publishedDate = "2023-01-01"
        )
    }
}
