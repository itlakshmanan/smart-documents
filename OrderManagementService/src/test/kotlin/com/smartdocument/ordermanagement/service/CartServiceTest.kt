package com.smartdocument.ordermanagement.service

import com.smartdocument.ordermanagement.client.BookClient
import com.smartdocument.ordermanagement.dto.BookResponseDto
import com.smartdocument.ordermanagement.dto.CartItemRequestDto
import com.smartdocument.ordermanagement.exception.OrderManagementServiceException
import com.smartdocument.ordermanagement.model.Cart
import com.smartdocument.ordermanagement.model.CartItem
import com.smartdocument.ordermanagement.model.Order
import com.smartdocument.ordermanagement.model.OrderStatus
import com.smartdocument.ordermanagement.repository.CartRepository
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.*

@Transactional
class CartServiceTest {

    private lateinit var cartService: CartService

    private val cartRepository: CartRepository = mockk()
    private val bookClient: BookClient = mockk()
    private val orderService: OrderService = mockk()
    private val paymentService: PaymentService = mockk()

    @BeforeEach
    fun setUp() {
        cartService = CartService(cartRepository, bookClient, orderService, paymentService)
    }

    @Test
    fun `getCartByCustomerId should return existing cart`() {
        // Given
        val customerId = "testuser1"
        val existingCart = Cart(customerId = customerId)
        every { cartRepository.findByCustomerId(customerId) } returns existingCart

        // When
        val result = cartService.getCartByCustomerId(customerId)

        // Then
        assertEquals(existingCart, result)
        verify { cartRepository.findByCustomerId(customerId) }
    }

    @Test
    fun `getCartByCustomerId should create new cart when not exists`() {
        // Given
        val customerId = "testuser1"
        every { cartRepository.findByCustomerId(customerId) } returns null
        every { cartRepository.save(any()) } answers { firstArg() }

        // When
        val result = cartService.getCartByCustomerId(customerId)

        // Then
        assertEquals(customerId, result.customerId)
        assertTrue(result.cartItems.isEmpty())
        assertEquals(BigDecimal.ZERO, result.totalAmount)
        verify { cartRepository.findByCustomerId(customerId) }
        verify { cartRepository.save(any()) }
    }

    @Test
    fun `addItemToCart should add new item successfully`() {
        // Given
        val customerId = "testuser1"
        val bookId = 1L
        val quantity = 2
        val price = BigDecimal("19.99")
        val request = CartItemRequestDto(bookId, quantity)
        val book = createTestBook(bookId, price, 10)
        val cart = Cart(customerId = customerId)

        every { cartRepository.findByCustomerId(customerId) } returns cart
        every { bookClient.getBookById(bookId) } returns book
        every { cartRepository.save(any()) } answers { firstArg() }

        // When
        val result = cartService.addItemToCart(customerId, request)

        // Then
        assertEquals(1, result.cartItems.size)
        val cartItem = result.cartItems.first()
        assertEquals(bookId, cartItem.bookId)
        assertEquals(quantity, cartItem.quantity)
        assertEquals(price, cartItem.price)
        assertEquals(price.multiply(BigDecimal(quantity)), cartItem.subtotal)
        assertEquals(price.multiply(BigDecimal(quantity)), result.totalAmount)

        verify { bookClient.getBookById(bookId) }
        verify { cartRepository.save(any()) }
    }

    @Test
    fun `addItemToCart should update existing item quantity`() {
        // Given
        val customerId = "testuser1"
        val bookId = 1L
        val existingQuantity = 2
        val newQuantity = 3
        val price = BigDecimal("19.99")
        val request = CartItemRequestDto(bookId, newQuantity)
        val book = createTestBook(bookId, price, 10)
        val cart = Cart(customerId = customerId)
        val existingCartItem = CartItem(
            cart = cart,
            bookId = bookId,
            quantity = existingQuantity,
            price = price,
            subtotal = price.multiply(BigDecimal(existingQuantity))
        )
        cart.cartItems.add(existingCartItem)
        cart.totalAmount = existingCartItem.subtotal

        every { cartRepository.findByCustomerId(customerId) } returns cart
        every { bookClient.getBookById(bookId) } returns book
        every { cartRepository.save(any()) } answers { firstArg() }

        // When
        val result = cartService.addItemToCart(customerId, request)

        // Then
        assertEquals(1, result.cartItems.size)
        val cartItem = result.cartItems.first()
        assertEquals(existingQuantity + newQuantity, cartItem.quantity)
        assertEquals(price, cartItem.price)
        assertEquals(price.multiply(BigDecimal(existingQuantity + newQuantity)), cartItem.subtotal)

        verify { bookClient.getBookById(bookId) }
        verify { cartRepository.save(any()) }
    }

    @Test
    fun `addItemToCart should throw exception when quantity exceeds available stock`() {
        // Given
        val customerId = "testuser1"
        val bookId = 1L
        val quantity = 5
        val availableStock = 3
        val request = CartItemRequestDto(bookId, quantity)
        val book = createTestBook(bookId, BigDecimal("19.99"), availableStock)
        val cart = Cart(customerId = customerId)

        every { cartRepository.findByCustomerId(customerId) } returns cart
        every { bookClient.getBookById(bookId) } returns book

        // When & Then
        val exception = assertThrows<OrderManagementServiceException> {
            cartService.addItemToCart(customerId, request)
        }
        assertEquals(OrderManagementServiceException.Operation.INSUFFICIENT_STOCK, exception.operation)
    }

    @Test
    fun `addItemToCart should throw exception when book not found`() {
        // Given
        val customerId = "testuser1"
        val bookId = 999L
        val request = CartItemRequestDto(bookId, 2)

        every { bookClient.getBookById(bookId) } returns null

        // When & Then
        val exception = assertThrows<OrderManagementServiceException> {
            cartService.addItemToCart(customerId, request)
        }
        assertEquals(OrderManagementServiceException.Operation.INVALID_CART_ITEM, exception.operation)

        verify { bookClient.getBookById(bookId) }
    }

    @Test
    fun `addItemToCart should throw exception when quantity is less than 1`() {
        // Given
        val customerId = "testuser1"
        val request = CartItemRequestDto(1L, 0)

        // When & Then
        val exception = assertThrows<OrderManagementServiceException> {
            cartService.addItemToCart(customerId, request)
        }
        assertEquals(OrderManagementServiceException.Operation.INVALID_CART_QUANTITY, exception.operation)
    }

    @Test
    fun `updateCartItemQuantity should update quantity successfully`() {
        // Given
        val customerId = "testuser1"
        val bookId = 1L
        val newQuantity = 3
        val price = BigDecimal("19.99")
        val cart = Cart(customerId = customerId)
        val existingCartItem = CartItem(
            cart = cart,
            bookId = bookId,
            quantity = 2,
            price = price,
            subtotal = price.multiply(BigDecimal(2))
        )
        cart.cartItems.add(existingCartItem)
        cart.totalAmount = existingCartItem.subtotal
        val book = createTestBook(bookId, price, 10)

        every { cartRepository.findByCustomerId(customerId) } returns cart
        every { bookClient.getBookById(bookId) } returns book
        every { cartRepository.save(any()) } answers { firstArg() }

        // When
        val result = cartService.updateCartItemQuantity(customerId, bookId, newQuantity)

        // Then
        assertEquals(1, result.cartItems.size)
        val cartItem = result.cartItems.first()
        assertEquals(newQuantity, cartItem.quantity)
        assertEquals(price.multiply(BigDecimal(newQuantity)), cartItem.subtotal)
        assertEquals(price.multiply(BigDecimal(newQuantity)), result.totalAmount)
        verify { bookClient.getBookById(bookId) }
        verify { cartRepository.save(any()) }
    }

    @Test
    fun `updateCartItemQuantity should throw exception when cart item not found`() {
        // Given
        val customerId = "testuser1"
        val bookId = 999L
        val cart = Cart(customerId = customerId)
        val book = createTestBook(bookId, BigDecimal("19.99"), 10)
        every { cartRepository.findByCustomerId(customerId) } returns cart
        every { bookClient.getBookById(bookId) } returns book

        // When & Then
        val exception = assertThrows<OrderManagementServiceException> {
            cartService.updateCartItemQuantity(customerId, bookId, 2)
        }
        assertEquals(OrderManagementServiceException.Operation.ITEM_NOT_FOUND_IN_CART, exception.operation)
    }

    @Test
    fun `updateCartItemQuantity should throw exception when quantity is less than 1`() {
        // Given
        val customerId = "testuser1"
        val bookId = 1L

        // When & Then
        val exception = assertThrows<OrderManagementServiceException> {
            cartService.updateCartItemQuantity(customerId, bookId, 0)
        }
        assertEquals(OrderManagementServiceException.Operation.INVALID_CART_QUANTITY, exception.operation)
    }

    @Test
    fun `updateCartItemQuantity should throw exception when quantity exceeds available stock`() {
        // Given
        val customerId = "testuser1"
        val bookId = 1L
        val newQuantity = 5
        val availableStock = 3
        val book = createTestBook(bookId, BigDecimal("19.99"), availableStock)
        val cart = Cart(customerId = customerId)
        val cartItem = CartItem(
            cart = cart,
            bookId = bookId,
            quantity = 1,
            price = BigDecimal("19.99"),
            subtotal = BigDecimal("19.99")
        )
        cart.cartItems.add(cartItem)

        every { cartRepository.findByCustomerId(customerId) } returns cart
        every { bookClient.getBookById(bookId) } returns book

        // When & Then
        val exception = assertThrows<OrderManagementServiceException> {
            cartService.updateCartItemQuantity(customerId, bookId, newQuantity)
        }
        assertEquals(OrderManagementServiceException.Operation.INSUFFICIENT_STOCK, exception.operation)
    }

    @Test
    fun `removeItemFromCart should remove item successfully`() {
        // Given
        val customerId = "testuser1"
        val bookId = 1L
        val cart = Cart(customerId = customerId)
        val cartItem = CartItem(
            cart = cart,
            bookId = bookId,
            quantity = 2,
            price = BigDecimal("19.99"),
            subtotal = BigDecimal("19.99") * BigDecimal(2)
        )
        cart.cartItems.add(cartItem)
        cart.totalAmount = cartItem.subtotal

        every { cartRepository.findByCustomerId(customerId) } returns cart
        every { cartRepository.save(any()) } answers { firstArg() }

        // When
        val result = cartService.removeItemFromCart(customerId, bookId)

        // Then
        assertTrue(result.cartItems.isEmpty())
        assertEquals(BigDecimal.ZERO, result.totalAmount)
        verify { cartRepository.save(any()) }
    }

    @Test
    fun `clearCart should clear all items successfully`() {
        // Given
        val customerId = "testuser1"
        val cart = Cart(customerId = customerId)
        val cartItem1 = CartItem(
            cart = cart,
            bookId = 1L,
            quantity = 2,
            price = BigDecimal("19.99"),
            subtotal = BigDecimal("19.99") * BigDecimal(2)
        )
        val cartItem2 = CartItem(
            cart = cart,
            bookId = 2L,
            quantity = 1,
            price = BigDecimal("29.99"),
            subtotal = BigDecimal("29.99")
        )
        cart.cartItems.addAll(listOf(cartItem1, cartItem2))
        cart.totalAmount = cartItem1.subtotal.add(cartItem2.subtotal)

        every { cartRepository.findByCustomerId(customerId) } returns cart
        every { cartRepository.save(any()) } answers { firstArg() }

        // When
        val result = cartService.clearCart(customerId)

        // Then
        assertTrue(result.cartItems.isEmpty())
        assertEquals(BigDecimal.ZERO, result.totalAmount)
        verify { cartRepository.save(any()) }
    }

    @Test
    fun `checkoutCart should create order and clear cart successfully`() {
        // Given
        val customerId = "testuser1"
        val bookId = 1L
        val quantity = 2
        val price = BigDecimal("19.99")
        val cart = Cart(customerId = customerId)
        val cartItem = CartItem(
            cart = cart,
            bookId = bookId,
            quantity = quantity,
            price = price,
            subtotal = price.multiply(BigDecimal(quantity))
        )
        cart.cartItems.add(cartItem)
        cart.totalAmount = cartItem.subtotal
        val book = createTestBook(bookId, price, 10)
        val order = createTestOrder(customerId, listOf(cartItem), 1L)
        order.status = OrderStatus.CONFIRMED
        order.updatedAt = java.time.LocalDateTime.now()
        order.createdAt = java.time.LocalDateTime.now()

        every { cartRepository.findByCustomerId(customerId) } returns cart
        every { cartRepository.save(any()) } answers { firstArg() }
        every { bookClient.getBookById(bookId) } returns book
        every { orderService.createOrder(any()) } returns order
        every { paymentService.processPayment(order.id, order.totalAmount) } returns true
        every { orderService.updateOrderStatus(order.id, OrderStatus.CONFIRMED) } returns order
        every { orderService.getOrderById(order.id) } returns order

        // When
        val result = cartService.checkoutCart(customerId)

        // Then
        assertEquals(order.id, result.id)
        assertEquals(order.customerId, result.customerId)
        assertEquals(order.status, result.status)
        assertEquals(order.totalAmount, result.totalAmount)
        assertEquals(order.orderItems.size, result.orderItems.size)
        for (i in order.orderItems.indices) {
            val expectedItem = order.orderItems[i]
            val actualItem = result.orderItems[i]
            assertEquals(expectedItem.bookId, actualItem.bookId)
            assertEquals(expectedItem.quantity, actualItem.quantity)
            assertEquals(expectedItem.price, actualItem.price)
            assertEquals(expectedItem.subtotal, actualItem.subtotal)
        }
        assertNotNull(result.createdAt)
        assertNotNull(result.updatedAt)
        verify { bookClient.getBookById(bookId) }
        verify { orderService.createOrder(any()) }
        verify { paymentService.processPayment(order.id, order.totalAmount) }
        verify { orderService.updateOrderStatus(order.id, OrderStatus.CONFIRMED) }
        verify { orderService.getOrderById(order.id) }
    }

    @Test
    fun `checkoutCart should throw exception when cart is empty`() {
        // Given
        val customerId = "testuser1"
        val cart = Cart(customerId = customerId)
        every { cartRepository.findByCustomerId(customerId) } returns cart

        // When & Then
        val exception = assertThrows<OrderManagementServiceException> {
            cartService.checkoutCart(customerId)
        }
        assertEquals(OrderManagementServiceException.Operation.EMPTY_CART, exception.operation)
    }

    @Test
    fun `checkoutCart should throw exception when payment fails`() {
        // Given
        val customerId = "testuser1"
        val bookId = 1L
        val quantity = 2
        val price = BigDecimal("19.99")
        val cart = Cart(customerId = customerId)
        val cartItem = CartItem(
            cart = cart,
            bookId = bookId,
            quantity = quantity,
            price = price,
            subtotal = price.multiply(BigDecimal(quantity))
        )
        cart.cartItems.add(cartItem)
        cart.totalAmount = cartItem.subtotal
        val book = createTestBook(bookId, price, 10)
        val order = createTestOrder(customerId, listOf(cartItem), 1L)
        order.status = OrderStatus.CANCELLED
        order.updatedAt = java.time.LocalDateTime.now()
        order.createdAt = java.time.LocalDateTime.now()

        every { cartRepository.findByCustomerId(customerId) } returns cart
        every { cartRepository.save(any()) } answers { firstArg() }
        every { bookClient.getBookById(bookId) } returns book
        every { orderService.createOrder(any()) } returns order
        every { paymentService.processPayment(order.id, order.totalAmount) } throws OrderManagementServiceException(OrderManagementServiceException.Operation.PAYMENT_FAILED)
        every { orderService.cancelOrder(any()) } returns order

        // When & Then
        val exception = assertThrows<OrderManagementServiceException> {
            cartService.checkoutCart(customerId)
        }
        assertEquals(OrderManagementServiceException.Operation.PAYMENT_FAILED, exception.operation)
        verify { bookClient.getBookById(bookId) }
        verify { orderService.createOrder(any()) }
        verify { paymentService.processPayment(order.id, order.totalAmount) }
        verify { orderService.cancelOrder(any()) }
    }

    // Helper methods
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

    private fun createTestOrder(customerId: String, cartItems: List<CartItem>, id: Long = 0): Order {
        val order = Order(
            id = id,
            customerId = customerId,
            status = OrderStatus.PENDING,
            totalAmount = cartItems.sumOf { it.subtotal }
        )
        return order
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    fun `addItemToCart should throw exception when quantity is negative`() {
        // Given
        val customerId = "testuser1"
        val request = CartItemRequestDto(1L, -1)

        // When & Then
        val exception = assertThrows<OrderManagementServiceException> {
            cartService.addItemToCart(customerId, request)
        }
        assertEquals(OrderManagementServiceException.Operation.INVALID_CART_QUANTITY, exception.operation)
    }

    @Test
    fun `addItemToCart should throw exception when quantity is maximum integer`() {
        // Given
        val customerId = "testuser1"
        val request = CartItemRequestDto(1L, Int.MAX_VALUE)
        val book = createTestBook(1L, BigDecimal("19.99"), 10)
        val cart = Cart(customerId = customerId)

        every { cartRepository.findByCustomerId(customerId) } returns cart
        every { bookClient.getBookById(1L) } returns book

        // When & Then
        val exception = assertThrows<OrderManagementServiceException> {
            cartService.addItemToCart(customerId, request)
        }
        assertEquals(OrderManagementServiceException.Operation.INSUFFICIENT_STOCK, exception.operation)
    }

    @Test
    fun `addItemToCart should handle book with zero stock`() {
        // Given
        val customerId = "testuser1"
        val bookId = 1L
        val request = CartItemRequestDto(bookId, 1)
        val book = createTestBook(bookId, BigDecimal("19.99"), 0)
        val cart = Cart(customerId = customerId)

        every { cartRepository.findByCustomerId(customerId) } returns cart
        every { bookClient.getBookById(bookId) } returns book

        // When & Then
        val exception = assertThrows<OrderManagementServiceException> {
            cartService.addItemToCart(customerId, request)
        }
        assertEquals(OrderManagementServiceException.Operation.INSUFFICIENT_STOCK, exception.operation)
    }

    @Test
    fun `addItemToCart should handle book with exactly matching stock`() {
        // Given
        val customerId = "testuser1"
        val bookId = 1L
        val quantity = 5
        val availableStock = 5
        val request = CartItemRequestDto(bookId, quantity)
        val book = createTestBook(bookId, BigDecimal("19.99"), availableStock)
        val cart = Cart(customerId = customerId)

        every { cartRepository.findByCustomerId(customerId) } returns cart
        every { bookClient.getBookById(bookId) } returns book
        every { cartRepository.save(any()) } answers { firstArg() }

        // When
        val result = cartService.addItemToCart(customerId, request)

        // Then
        assertEquals(1, result.cartItems.size)
        val cartItem = result.cartItems.first()
        assertEquals(quantity, cartItem.quantity)
        assertEquals(availableStock, cartItem.quantity) // Should match exactly
    }

    @Test
    fun `addItemToCart should handle zero price book`() {
        // Given
        val customerId = "testuser1"
        val bookId = 1L
        val quantity = 2
        val request = CartItemRequestDto(bookId, quantity)
        val book = createTestBook(bookId, BigDecimal.ZERO, 10)
        val cart = Cart(customerId = customerId)

        every { cartRepository.findByCustomerId(customerId) } returns cart
        every { bookClient.getBookById(bookId) } returns book
        every { cartRepository.save(any()) } answers { firstArg() }

        // When
        val result = cartService.addItemToCart(customerId, request)

        // Then
        assertEquals(1, result.cartItems.size)
        val cartItem = result.cartItems.first()
        assertEquals(BigDecimal.ZERO, cartItem.price)
        assertEquals(BigDecimal.ZERO, cartItem.subtotal)
        assertEquals(BigDecimal.ZERO, result.totalAmount)
    }

    @Test
    fun `updateCartItemQuantity should throw exception when quantity is negative`() {
        // Given
        val customerId = "testuser1"
        val bookId = 1L

        // When & Then
        val exception = assertThrows<OrderManagementServiceException> {
            cartService.updateCartItemQuantity(customerId, bookId, -1)
        }
        assertEquals(OrderManagementServiceException.Operation.INVALID_CART_QUANTITY, exception.operation)
    }

    @Test
    fun `updateCartItemQuantity should throw exception when quantity is maximum integer`() {
        // Given
        val customerId = "testuser1"
        val bookId = 1L
        val book = createTestBook(bookId, BigDecimal("19.99"), 10)
        val cart = Cart(customerId = customerId)
        val cartItem = CartItem(
            cart = cart,
            bookId = bookId,
            quantity = 1,
            price = BigDecimal("19.99"),
            subtotal = BigDecimal("19.99")
        )
        cart.cartItems.add(cartItem)

        every { cartRepository.findByCustomerId(customerId) } returns cart
        every { bookClient.getBookById(bookId) } returns book

        // When & Then
        val exception = assertThrows<OrderManagementServiceException> {
            cartService.updateCartItemQuantity(customerId, bookId, Int.MAX_VALUE)
        }
        assertEquals(OrderManagementServiceException.Operation.INSUFFICIENT_STOCK, exception.operation)
    }

    @Test
    fun `removeItemFromCart should handle removing non-existent item`() {
        // Given
        val customerId = "testuser1"
        val bookId = 999L
        val cart = Cart(customerId = customerId)

        every { cartRepository.findByCustomerId(customerId) } returns cart

        // When & Then
        val exception = assertThrows<OrderManagementServiceException> {
            cartService.removeItemFromCart(customerId, bookId)
        }
        assertEquals(OrderManagementServiceException.Operation.ITEM_NOT_FOUND_IN_CART, exception.operation)
    }

    @Test
    fun `cart total calculation should handle multiple items with different prices`() {
        // Given
        val customerId = "testuser1"
        val cart = Cart(customerId = customerId)

        val item1 = CartItem(cart = cart, bookId = 1L, quantity = 2, price = BigDecimal("10.00"), subtotal = BigDecimal("20.00"))
        val item2 = CartItem(cart = cart, bookId = 2L, quantity = 1, price = BigDecimal("15.50"), subtotal = BigDecimal("15.50"))
        val item3 = CartItem(cart = cart, bookId = 3L, quantity = 3, price = BigDecimal("5.25"), subtotal = BigDecimal("15.75"))

        cart.cartItems.addAll(listOf(item1, item2, item3))
        cart.totalAmount = item1.subtotal.add(item2.subtotal).add(item3.subtotal)

        every { cartRepository.findByCustomerId(customerId) } returns cart
        every { cartRepository.save(any()) } answers { firstArg() }

        // When
        val result = cartService.clearCart(customerId)

        // Then
        assertTrue(result.cartItems.isEmpty())
        assertEquals(BigDecimal.ZERO, result.totalAmount)
        assertEquals(BigDecimal("51.25"), item1.subtotal.add(item2.subtotal).add(item3.subtotal)) // Verify original total
    }

    @Test
    fun `addItemToCart should handle price precision edge cases`() {
        // Given
        val customerId = "testuser1"
        val bookId = 1L
        val quantity = 3
        val price = BigDecimal("19.999999") // High precision price
        val request = CartItemRequestDto(bookId, quantity)
        val book = createTestBook(bookId, price, 10)
        val cart = Cart(customerId = customerId)

        every { cartRepository.findByCustomerId(customerId) } returns cart
        every { bookClient.getBookById(bookId) } returns book
        every { cartRepository.save(any()) } answers { firstArg() }

        // When
        val result = cartService.addItemToCart(customerId, request)

        // Then
        assertEquals(1, result.cartItems.size)
        val cartItem = result.cartItems.first()
        assertEquals(price, cartItem.price)
        assertEquals(price.multiply(BigDecimal(quantity)), cartItem.subtotal)
        assertEquals(price.multiply(BigDecimal(quantity)), result.totalAmount)
    }
}
