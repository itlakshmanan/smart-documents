package com.smartdocument.ordermanagement.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.smartdocument.ordermanagement.config.TestConfig
import com.smartdocument.ordermanagement.dto.CartItemRequestDto
import com.smartdocument.ordermanagement.dto.UpdateQuantityRequestDto
import com.smartdocument.ordermanagement.model.Cart
import com.smartdocument.ordermanagement.model.CartItem
import com.smartdocument.ordermanagement.repository.CartRepository
import com.smartdocument.ordermanagement.repository.OrderRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import java.math.BigDecimal
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.junit.jupiter.api.Assertions.*

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@Import(TestConfig::class)
class CartControllerIntegrationTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var cartRepository: CartRepository

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var mockMvc: MockMvc

    // Externalized configuration properties
    @Value("\${order.management.service.username}")
    private lateinit var username: String

    @Value("\${order.management.service.password}")
    private lateinit var password: String

    @Value("\${test.cart.base-url}")
    private lateinit var baseUrl: String

    @Value("\${test.cart.default.customer-id}")
    private lateinit var defaultCustomerId: String

    @Value("\${test.cart.default.book-id}")
    private lateinit var defaultBookId: String

    @Value("\${test.cart.default.quantity}")
    private lateinit var defaultQuantity: String

    @Value("\${test.cart.default.price}")
    private lateinit var defaultPrice: String

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .build()

        // Clear database before each test
        cartRepository.deleteAll()
        orderRepository.deleteAll()
    }

    // Helper method to add Basic Authentication headers
    private fun addBasicAuth(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder {
        return request.with { req ->
            req.addHeader("Authorization", "Basic " + java.util.Base64.getEncoder()
                .encodeToString("$username:$password".toByteArray()))
            req
        }
    }

    @Test
    fun `should get empty cart for new customer`() {
        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/$defaultCustomerId")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.customerId").value(defaultCustomerId))
            .andExpect(jsonPath("$.totalAmount").value(0))
            .andExpect(jsonPath("$.items").isArray)
            .andExpect(jsonPath("$.items").isEmpty)
    }

    @Test
    fun `should add item to cart successfully`() {
        // Given
        val cartItemRequest = CartItemRequestDto(
            bookId = defaultBookId.toLong(),
            quantity = defaultQuantity.toInt()
        )

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                post("$baseUrl/$defaultCustomerId/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(cartItemRequest))
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.customerId").value(defaultCustomerId))
            .andExpect(jsonPath("$.items").isArray)
            .andExpect(jsonPath("$.items.size()").value(1))
            .andExpect(jsonPath("$.items[0].bookId").value(defaultBookId.toLong()))
            .andExpect(jsonPath("$.items[0].quantity").value(defaultQuantity.toInt()))

        // Verify cart was saved in database
        val savedCart = cartRepository.findByCustomerId(defaultCustomerId)
        assertNotNull(savedCart)
        assertEquals(1, savedCart!!.cartItems.size)
        assertEquals(defaultBookId.toLong(), savedCart.cartItems.first().bookId)
    }

    @Test
    fun `should add multiple items to cart`() {
        // Given
        val cartItemRequest1 = CartItemRequestDto(bookId = 1L, quantity = 2)
        val cartItemRequest2 = CartItemRequestDto(bookId = 2L, quantity = 1)

        // When & Then - Add first item
        mockMvc.perform(
            addBasicAuth(
                post("$baseUrl/$defaultCustomerId/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(cartItemRequest1))
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.size()").value(1))

        // Add second item
        mockMvc.perform(
            addBasicAuth(
                post("$baseUrl/$defaultCustomerId/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(cartItemRequest2))
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.size()").value(2))
            .andExpect(jsonPath("$.items[0].bookId").value(1L))
            .andExpect(jsonPath("$.items[1].bookId").value(2L))

        // Verify database state
        val savedCart = cartRepository.findByCustomerId(defaultCustomerId)
        assertNotNull(savedCart)
        assertEquals(2, savedCart!!.cartItems.size)
    }

    @Test
    fun `should combine quantities when adding same book to cart`() {
        // Given
        val cartItemRequest = CartItemRequestDto(bookId = 1L, quantity = 2)

        // When & Then - Add item first time
        mockMvc.perform(
            addBasicAuth(
                post("$baseUrl/$defaultCustomerId/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(cartItemRequest))
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].quantity").value(2))

        // Add same item again
        mockMvc.perform(
            addBasicAuth(
                post("$baseUrl/$defaultCustomerId/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(cartItemRequest))
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.size()").value(1))
            .andExpect(jsonPath("$.items[0].quantity").value(4))

        // Verify database state
        val savedCart = cartRepository.findByCustomerId(defaultCustomerId)
        assertNotNull(savedCart)
        assertEquals(1, savedCart!!.cartItems.size)
        assertEquals(4, savedCart.cartItems.first().quantity)
    }

    @Test
    fun `should update item quantity successfully`() {
        // Given
        val cart = createTestCart(defaultCustomerId)
        val cartItem = createTestCartItem(cart, 1L, 2)
        cart.cartItems.add(cartItem)
        cartRepository.save(cart)

        val updateRequest = UpdateQuantityRequestDto(quantity = 5)

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/$defaultCustomerId/items/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest))
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].quantity").value(5))

        // Verify database was updated
        val updatedCart = cartRepository.findByCustomerId(defaultCustomerId)
        assertNotNull(updatedCart)
        assertEquals(5, updatedCart!!.cartItems.first().quantity)
    }

    @Test
    fun `should remove item from cart successfully`() {
        // Given
        val cart = createTestCart(defaultCustomerId)
        val cartItem1 = createTestCartItem(cart, 1L, 2)
        val cartItem2 = createTestCartItem(cart, 2L, 1)
        cart.cartItems.addAll(listOf(cartItem1, cartItem2))
        cartRepository.save(cart)

        // When & Then
        mockMvc.perform(addBasicAuth(delete("$baseUrl/$defaultCustomerId/items/1")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.size()").value(1))
            .andExpect(jsonPath("$.items[0].bookId").value(2L))

        // Verify item was removed from database
        val updatedCart = cartRepository.findByCustomerId(defaultCustomerId)
        assertNotNull(updatedCart)
        assertEquals(1, updatedCart!!.cartItems.size)
        assertEquals(2L, updatedCart.cartItems.first().bookId)
    }

    @Test
    fun `should clear cart successfully`() {
        // Given
        val cart = createTestCart(defaultCustomerId)
        val cartItem1 = createTestCartItem(cart, 1L, 2)
        val cartItem2 = createTestCartItem(cart, 2L, 1)
        cart.cartItems.addAll(listOf(cartItem1, cartItem2))
        cartRepository.save(cart)

        // When & Then
        mockMvc.perform(addBasicAuth(delete("$baseUrl/$defaultCustomerId")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items").isEmpty)
            .andExpect(jsonPath("$.totalAmount").value(0))

        // Verify cart was cleared in database
        val clearedCart = cartRepository.findByCustomerId(defaultCustomerId)
        assertNotNull(clearedCart)
        assertTrue(clearedCart!!.cartItems.isEmpty())
    }

    @Test
    fun `should return 400 when adding item with invalid quantity`() {
        // Given
        val invalidCartItemRequest = CartItemRequestDto(
            bookId = 1L,
            quantity = 0 // Invalid: quantity must be at least 1
        )

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                post("$baseUrl/$defaultCustomerId/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidCartItemRequest))
            )
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 400 when updating quantity with invalid value`() {
        // Given
        val cart = createTestCart(defaultCustomerId)
        val cartItem = createTestCartItem(cart, 1L, 2)
        cart.cartItems.add(cartItem)
        cartRepository.save(cart)

        val invalidUpdateRequest = UpdateQuantityRequestDto(quantity = -1) // Invalid: negative quantity

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/$defaultCustomerId/items/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidUpdateRequest))
            )
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 404 when updating quantity for non-existent item`() {
        // Given
        val updateRequest = UpdateQuantityRequestDto(quantity = 5)

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/$defaultCustomerId/items/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest))
            )
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 404 when removing non-existent item`() {
        // When & Then
        mockMvc.perform(addBasicAuth(delete("$baseUrl/$defaultCustomerId/items/999")))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should handle multiple customers with separate carts`() {
        // Given
        val customer1 = "customer1"
        val customer2 = "customer2"
        val cartItemRequest = CartItemRequestDto(bookId = 1L, quantity = 2)

        // When & Then - Add item to customer1's cart
        mockMvc.perform(
            addBasicAuth(
                post("$baseUrl/$customer1/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(cartItemRequest))
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.customerId").value(customer1))
            .andExpect(jsonPath("$.items.size()").value(1))

        // Add item to customer2's cart
        mockMvc.perform(
            addBasicAuth(
                post("$baseUrl/$customer2/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(cartItemRequest))
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.customerId").value(customer2))
            .andExpect(jsonPath("$.items.size()").value(1))

        // Verify separate carts exist in database
        val cart1 = cartRepository.findByCustomerId(customer1)
        val cart2 = cartRepository.findByCustomerId(customer2)
        assertNotNull(cart1)
        assertNotNull(cart2)
        assertNotEquals(cart1!!.id, cart2!!.id)
    }

    @Test
    fun `should calculate total amount correctly`() {
        // Given
        val cart = createTestCart(defaultCustomerId)
        val cartItem1 = createTestCartItem(cart, 1L, 2, BigDecimal("19.99"))
        val cartItem2 = createTestCartItem(cart, 2L, 1, BigDecimal("29.99"))
        cart.cartItems.addAll(listOf(cartItem1, cartItem2))
        cartRepository.save(cart)

        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/$defaultCustomerId")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalAmount").value(69.97)) // (19.99 * 2) + (29.99 * 1)
            .andExpect(jsonPath("$.items[0].subtotal").value(39.98)) // 19.99 * 2
            .andExpect(jsonPath("$.items[1].subtotal").value(29.99)) // 29.99 * 1
    }

    @Test
    fun `should handle empty cart after removing all items`() {
        // Given
        val cart = createTestCart(defaultCustomerId)
        val cartItem = createTestCartItem(cart, 1L, 2)
        cart.cartItems.add(cartItem)
        cartRepository.save(cart)

        // When & Then - Remove the item
        mockMvc.perform(addBasicAuth(delete("$baseUrl/$defaultCustomerId/items/1")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items").isEmpty)
            .andExpect(jsonPath("$.totalAmount").value(0))

        // Get cart again to verify it's empty
        mockMvc.perform(addBasicAuth(get("$baseUrl/$defaultCustomerId")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items").isEmpty)
            .andExpect(jsonPath("$.totalAmount").value(0))
    }

    @Test
    fun `should handle large quantities correctly`() {
        // Given
        val cartItemRequest = CartItemRequestDto(bookId = 1L, quantity = 1000)

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                post("$baseUrl/$defaultCustomerId/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(cartItemRequest))
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].quantity").value(1000))

        // Verify database state
        val savedCart = cartRepository.findByCustomerId(defaultCustomerId)
        assertNotNull(savedCart)
        assertEquals(1000, savedCart!!.cartItems.first().quantity)
    }

    @Test
    fun `should handle special characters in customer ID`() {
        // Given
        val specialCustomerId = "customer-123_test@example.com"
        val cartItemRequest = CartItemRequestDto(bookId = 1L, quantity = 1)

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                post("$baseUrl/$specialCustomerId/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(cartItemRequest))
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.customerId").value(specialCustomerId))

        // Verify cart was created
        val savedCart = cartRepository.findByCustomerId(specialCustomerId)
        assertNotNull(savedCart)
        assertEquals(specialCustomerId, savedCart!!.customerId)
    }

    @Test
    fun `should handle concurrent cart operations`() {
        // Given
        val cartItemRequest = CartItemRequestDto(bookId = 1L, quantity = 1)

        // When & Then - Add item multiple times concurrently (simulated)
        repeat(3) { _ ->
            mockMvc.perform(
                addBasicAuth(
                    post("$baseUrl/$defaultCustomerId/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartItemRequest))
                )
            )
                .andExpect(status().isOk)
        }

        // Verify final state
        val savedCart = cartRepository.findByCustomerId(defaultCustomerId)
        assertNotNull(savedCart)
        assertEquals(1, savedCart!!.cartItems.size)
        assertEquals(3, savedCart.cartItems.first().quantity) // Should be combined
    }

    // Helper method to create test carts
    private fun createTestCart(customerId: String): Cart {
        return Cart(customerId = customerId)
    }

    // Helper method to create test cart items
    private fun createTestCartItem(
        cart: Cart,
        bookId: Long,
        quantity: Int,
        price: BigDecimal = BigDecimal(defaultPrice)
    ): CartItem {
        return CartItem(
            cart = cart,
            bookId = bookId,
            quantity = quantity,
            price = price,
            subtotal = price.multiply(BigDecimal(quantity))
        )
    }
}
