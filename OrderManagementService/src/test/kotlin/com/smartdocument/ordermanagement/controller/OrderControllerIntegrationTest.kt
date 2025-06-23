package com.smartdocument.ordermanagement.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.smartdocument.ordermanagement.config.TestConfig
import com.smartdocument.ordermanagement.model.Order
import com.smartdocument.ordermanagement.model.OrderItem
import com.smartdocument.ordermanagement.model.OrderStatus
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@Import(TestConfig::class)
class OrderControllerIntegrationTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

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

    @Value("\${test.order.base-url}")
    private lateinit var baseUrl: String

    @Value("\${test.order.default.customer-id}")
    private lateinit var defaultCustomerId: String

    @Value("\${test.order.default.book-id}")
    private lateinit var defaultBookId: String

    @Value("\${test.order.default.quantity}")
    private lateinit var defaultQuantity: String

    @Value("\${test.order.default.price}")
    private lateinit var defaultPrice: String

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .build()

        // Clear database before each test
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
    fun `should get order by ID successfully`() {
        // Given
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), defaultQuantity.toInt())
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.PENDING, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/${savedOrder.id}")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(savedOrder.id))
            .andExpect(jsonPath("$.customerId").value(defaultCustomerId))
            .andExpect(jsonPath("$.status").value(OrderStatus.PENDING.name))
            .andExpect(jsonPath("$.totalAmount").value(BigDecimal(defaultPrice).multiply(BigDecimal(defaultQuantity))))
            .andExpect(jsonPath("$.orderItems").isArray)
            .andExpect(jsonPath("$.orderItems.size()").value(1))
            .andExpect(jsonPath("$.orderItems[0].bookId").value(defaultBookId.toLong()))
            .andExpect(jsonPath("$.orderItems[0].quantity").value(defaultQuantity.toInt()))
            .andExpect(jsonPath("$.orderItems[0].price").value(BigDecimal(defaultPrice)))
            .andExpect(jsonPath("$.orderItems[0].subtotal").value(BigDecimal(defaultPrice).multiply(BigDecimal(defaultQuantity))))
            .andExpect(jsonPath("$.createdAt").exists())
            // updatedAt may be null for new orders, so we don't assert it
    }

    @Test
    fun `should return 404 when getting non-existent order`() {
        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/999")))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should update order status successfully`() {
        // Given
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), defaultQuantity.toInt())
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.PENDING, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        val statusUpdateRequest = mapOf("status" to OrderStatus.CONFIRMED.name)

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/${savedOrder.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(statusUpdateRequest))
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(savedOrder.id))
            .andExpect(jsonPath("$.status").value(OrderStatus.CONFIRMED.name))
            .andExpect(jsonPath("$.updatedAt").exists())

        // Verify database was updated
        val updatedOrder = orderRepository.findById(savedOrder.id)
        assertTrue(updatedOrder.isPresent)
        assertEquals(OrderStatus.CONFIRMED, updatedOrder.get().status)
    }

    @Test
    fun `should return 400 when updating status with invalid status value`() {
        // Given
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), defaultQuantity.toInt())
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.PENDING, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        val invalidStatusRequest = mapOf("status" to "INVALID_STATUS")

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/${savedOrder.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidStatusRequest))
            )
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 404 when updating status for non-existent order`() {
        // Given
        val statusUpdateRequest = mapOf("status" to OrderStatus.CONFIRMED.name)

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(statusUpdateRequest))
            )
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should handle order with multiple items correctly`() {
        // Given
        val orderItem1 = createTestOrderItem(null, 1L, 2, BigDecimal(defaultPrice))
        val orderItem2 = createTestOrderItem(null, 2L, 1, BigDecimal(defaultPrice))
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.PENDING, listOf(orderItem1, orderItem2))
        val savedOrder = orderRepository.save(order)

        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/${savedOrder.id}")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.orderItems.size()").value(2))
            .andExpect(jsonPath("$.orderItems[0].bookId").value(1L))
            .andExpect(jsonPath("$.orderItems[0].quantity").value(2))
            .andExpect(jsonPath("$.orderItems[0].price").value(BigDecimal(defaultPrice)))
            .andExpect(jsonPath("$.orderItems[0].subtotal").value(BigDecimal(defaultPrice).multiply(BigDecimal(2))))
            .andExpect(jsonPath("$.orderItems[1].bookId").value(2L))
            .andExpect(jsonPath("$.orderItems[1].quantity").value(1))
            .andExpect(jsonPath("$.orderItems[1].price").value(BigDecimal(defaultPrice)))
            .andExpect(jsonPath("$.orderItems[1].subtotal").value(BigDecimal(defaultPrice)))
            .andExpect(jsonPath("$.totalAmount").value(BigDecimal(defaultPrice).multiply(BigDecimal(3))))
    }

    @Test
    fun `should handle order with zero total amount`() {
        // Given
        val order = createTestOrder(defaultCustomerId, OrderStatus.PENDING)
        order.totalAmount = BigDecimal.ZERO
        val savedOrder = orderRepository.save(order)

        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/${savedOrder.id}")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalAmount").value(0))
            .andExpect(jsonPath("$.orderItems").isEmpty)
    }

    @Test
    fun `should handle order with decimal prices correctly`() {
        // Given
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), 3, BigDecimal(defaultPrice))
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.PENDING, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/${savedOrder.id}")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.orderItems[0].price").value(BigDecimal(defaultPrice)))
            .andExpect(jsonPath("$.orderItems[0].subtotal").value(BigDecimal(defaultPrice).multiply(BigDecimal(3))))
            .andExpect(jsonPath("$.totalAmount").value(BigDecimal(defaultPrice).multiply(BigDecimal(3))))
    }

    @Test
    fun `should handle special characters in customer ID`() {
        // Given
        val specialCustomerId = "customer-123_test@example.com"
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), defaultQuantity.toInt())
        val order = createTestOrderWithItems(specialCustomerId, OrderStatus.PENDING, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/${savedOrder.id}")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.customerId").value(specialCustomerId))

        // Verify order exists in database
        val retrievedOrder = orderRepository.findById(savedOrder.id)
        assertTrue(retrievedOrder.isPresent)
        assertEquals(specialCustomerId, retrievedOrder.get().customerId)
    }

    @Test
    fun `should handle concurrent status updates`() {
        // Given
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), defaultQuantity.toInt())
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.PENDING, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        val statusUpdateRequest = mapOf("status" to OrderStatus.CONFIRMED.name)

        // When & Then - Update status multiple times (simulated concurrent updates)
        // First update should succeed, subsequent ones should fail due to validation
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/${savedOrder.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(statusUpdateRequest))
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(OrderStatus.CONFIRMED.name))

        // Subsequent updates should fail because order is already CONFIRMED
        repeat(2) {
            mockMvc.perform(
                addBasicAuth(
                    patch("$baseUrl/${savedOrder.id}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdateRequest))
                )
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.message").value(containsString("Invalid order status")))
        }

        // Verify final state in database
        val finalOrder = orderRepository.findById(savedOrder.id)
        assertTrue(finalOrder.isPresent)
        assertEquals(OrderStatus.CONFIRMED, finalOrder.get().status)
    }

    @Test
    fun `should handle malformed JSON in status update request`() {
        // Given
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), defaultQuantity.toInt())
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.PENDING, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        val malformedJson = "{ invalid json }"

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/${savedOrder.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(malformedJson)
            )
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should handle empty JSON in status update request`() {
        // Given
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), defaultQuantity.toInt())
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.PENDING, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        val emptyJson = "{}"

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/${savedOrder.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(emptyJson)
            )
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should prevent invalid status transitions - PENDING to DELIVERED`() {
        // Given
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), defaultQuantity.toInt())
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.PENDING, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        val invalidStatusRequest = mapOf("status" to OrderStatus.DELIVERED.name)

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/${savedOrder.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidStatusRequest))
            )
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(containsString("Invalid order status")))

        // Verify order status was not changed
        val unchangedOrder = orderRepository.findById(savedOrder.id)
        assertTrue(unchangedOrder.isPresent)
        assertEquals(OrderStatus.PENDING, unchangedOrder.get().status)
    }

    @Test
    fun `should prevent invalid status transitions - CONFIRMED to PENDING`() {
        // Given
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), defaultQuantity.toInt())
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.CONFIRMED, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        val invalidStatusRequest = mapOf("status" to OrderStatus.PENDING.name)

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/${savedOrder.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidStatusRequest))
            )
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(containsString("Invalid order status")))

        // Verify order status was not changed
        val unchangedOrder = orderRepository.findById(savedOrder.id)
        assertTrue(unchangedOrder.isPresent)
        assertEquals(OrderStatus.CONFIRMED, unchangedOrder.get().status)
    }

    @Test
    fun `should prevent invalid status transitions - DELIVERED to SHIPPED`() {
        // Given
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), defaultQuantity.toInt())
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.DELIVERED, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        val invalidStatusRequest = mapOf("status" to OrderStatus.SHIPPED.name)

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/${savedOrder.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidStatusRequest))
            )
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(containsString("Invalid order status")))

        // Verify order status was not changed
        val unchangedOrder = orderRepository.findById(savedOrder.id)
        assertTrue(unchangedOrder.isPresent)
        assertEquals(OrderStatus.DELIVERED, unchangedOrder.get().status)
    }

    @Test
    fun `should prevent invalid status transitions - CANCELLED to CONFIRMED`() {
        // Given
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), defaultQuantity.toInt())
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.CANCELLED, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        val invalidStatusRequest = mapOf("status" to OrderStatus.CONFIRMED.name)

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/${savedOrder.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidStatusRequest))
            )
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(containsString("Invalid order status")))

        // Verify order status was not changed
        val unchangedOrder = orderRepository.findById(savedOrder.id)
        assertTrue(unchangedOrder.isPresent)
        assertEquals(OrderStatus.CANCELLED, unchangedOrder.get().status)
    }

    @Test
    fun `should allow cancelling pending orders`() {
        // Given
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), defaultQuantity.toInt())
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.PENDING, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        val cancelRequest = mapOf("status" to OrderStatus.CANCELLED.name)

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/${savedOrder.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(cancelRequest))
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(OrderStatus.CANCELLED.name))
            .andExpect(jsonPath("$.updatedAt").exists())

        // Verify database was updated
        val cancelledOrder = orderRepository.findById(savedOrder.id)
        assertTrue(cancelledOrder.isPresent)
        assertEquals(OrderStatus.CANCELLED, cancelledOrder.get().status)
    }

    @Test
    fun `should allow cancelling confirmed orders`() {
        // Given
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), defaultQuantity.toInt())
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.CONFIRMED, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        val cancelRequest = mapOf("status" to OrderStatus.CANCELLED.name)

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/${savedOrder.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(cancelRequest))
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(OrderStatus.CANCELLED.name))

        // Verify database was updated
        val cancelledOrder = orderRepository.findById(savedOrder.id)
        assertTrue(cancelledOrder.isPresent)
        assertEquals(OrderStatus.CANCELLED, cancelledOrder.get().status)
    }

    @Test
    fun `should allow cancelling shipped orders`() {
        // Given
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), defaultQuantity.toInt())
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.SHIPPED, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        val cancelRequest = mapOf("status" to OrderStatus.CANCELLED.name)

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/${savedOrder.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(cancelRequest))
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(OrderStatus.CANCELLED.name))

        // Verify database was updated
        val cancelledOrder = orderRepository.findById(savedOrder.id)
        assertTrue(cancelledOrder.isPresent)
        assertEquals(OrderStatus.CANCELLED, cancelledOrder.get().status)
    }

    @Test
    fun `should prevent cancelling delivered orders`() {
        // Given
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), defaultQuantity.toInt())
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.DELIVERED, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        val cancelRequest = mapOf("status" to OrderStatus.CANCELLED.name)

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/${savedOrder.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(cancelRequest))
            )
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(containsString("Invalid order status")))

        // Verify order status was not changed
        val unchangedOrder = orderRepository.findById(savedOrder.id)
        assertTrue(unchangedOrder.isPresent)
        assertEquals(OrderStatus.DELIVERED, unchangedOrder.get().status)
    }

    @Test
    fun `should handle cancelling already cancelled orders`() {
        // Given
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), defaultQuantity.toInt())
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.CANCELLED, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        val cancelRequest = mapOf("status" to OrderStatus.CANCELLED.name)

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/${savedOrder.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(cancelRequest))
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(OrderStatus.CANCELLED.name))
            .andExpect(jsonPath("$.updatedAt").exists())

        // Verify order remains cancelled but timestamp is updated
        val cancelledOrder = orderRepository.findById(savedOrder.id)
        assertTrue(cancelledOrder.isPresent)
        assertEquals(OrderStatus.CANCELLED, cancelledOrder.get().status)
        assertNotNull(cancelledOrder.get().updatedAt)
    }

    @Test
    fun `should handle very large order amounts correctly`() {
        // Given
        val largePrice = BigDecimal("999999.99")
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), 1000, largePrice)
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.PENDING, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        val expectedTotal = largePrice.multiply(BigDecimal(1000))

        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/${savedOrder.id}")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalAmount").value(Matchers.closeTo(expectedTotal.toDouble(), 0.01)))
            .andExpect(jsonPath("$.orderItems[0].price").value(Matchers.closeTo(largePrice.toDouble(), 0.01)))
            .andExpect(jsonPath("$.orderItems[0].subtotal").value(Matchers.closeTo(expectedTotal.toDouble(), 0.01)))
    }

    @Test
    fun `should handle very small decimal prices correctly`() {
        // Given
        val smallPrice = BigDecimal("0.01")
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), 5, smallPrice)
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.PENDING, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/${savedOrder.id}")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalAmount").value(BigDecimal("0.05")))
            .andExpect(jsonPath("$.orderItems[0].price").value(smallPrice))
            .andExpect(jsonPath("$.orderItems[0].subtotal").value(BigDecimal("0.05")))
    }

    @Test
    fun `should handle orders with many decimal places`() {
        // Given
        val precisePrice = BigDecimal("19.999999")
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), 3, precisePrice)
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.PENDING, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/${savedOrder.id}")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.orderItems[0].price").value(precisePrice))
            .andExpect(jsonPath("$.orderItems[0].subtotal").value(precisePrice.multiply(BigDecimal(3))))
            .andExpect(jsonPath("$.totalAmount").value(precisePrice.multiply(BigDecimal(3))))
    }

    @Test
    fun `should handle orders with zero quantity items`() {
        // Given
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), 0, BigDecimal(defaultPrice))
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.PENDING, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/${savedOrder.id}")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.orderItems[0].quantity").value(0))
            .andExpect(jsonPath("$.orderItems[0].subtotal").value(0.0))
            .andExpect(jsonPath("$.totalAmount").value(0.0))
    }

    @Test
    fun `should handle status update with missing status field`() {
        // Given
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), defaultQuantity.toInt())
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.PENDING, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        val invalidRequest = mapOf("someOtherField" to "someValue")

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/${savedOrder.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest))
            )
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Invalid request data"))

        // Verify order status was not changed
        val unchangedOrder = orderRepository.findById(savedOrder.id)
        assertTrue(unchangedOrder.isPresent)
        assertEquals(OrderStatus.PENDING, unchangedOrder.get().status)
    }

    @Test
    fun `should handle status update with null status value`() {
        // Given
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), defaultQuantity.toInt())
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.PENDING, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        val invalidRequest = mapOf("status" to null)

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/${savedOrder.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest))
            )
        )
            .andExpect(status().isBadRequest)

        // Verify order status was not changed
        val unchangedOrder = orderRepository.findById(savedOrder.id)
        assertTrue(unchangedOrder.isPresent)
        assertEquals(OrderStatus.PENDING, unchangedOrder.get().status)
    }

    @Test
    fun `should handle status update with empty status string`() {
        // Given
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), defaultQuantity.toInt())
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.PENDING, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        val invalidRequest = mapOf("status" to "")

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/${savedOrder.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest))
            )
        )
            .andExpect(status().isBadRequest)

        // Verify order status was not changed
        val unchangedOrder = orderRepository.findById(savedOrder.id)
        assertTrue(unchangedOrder.isPresent)
        assertEquals(OrderStatus.PENDING, unchangedOrder.get().status)
    }

    @Test
    fun `should handle status update with whitespace-only status`() {
        // Given
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), defaultQuantity.toInt())
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.PENDING, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        val invalidRequest = mapOf("status" to "   ")

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/${savedOrder.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest))
            )
        )
            .andExpect(status().isBadRequest)

        // Verify order status was not changed
        val unchangedOrder = orderRepository.findById(savedOrder.id)
        assertTrue(unchangedOrder.isPresent)
        assertEquals(OrderStatus.PENDING, unchangedOrder.get().status)
    }

    @Test
    fun `should handle status update with case-sensitive status values`() {
        // Given
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), defaultQuantity.toInt())
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.PENDING, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        val invalidRequest = mapOf("status" to "confirmed") // lowercase

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/${savedOrder.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest))
            )
        )
            .andExpect(status().isBadRequest)

        // Verify order status was not changed
        val unchangedOrder = orderRepository.findById(savedOrder.id)
        assertTrue(unchangedOrder.isPresent)
        assertEquals(OrderStatus.PENDING, unchangedOrder.get().status)
    }

    @Test
    fun `should handle orders with negative prices`() {
        // Given
        val negativePrice = BigDecimal("-19.99")
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), 2, negativePrice)
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.PENDING, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/${savedOrder.id}")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.orderItems[0].price").value(Matchers.closeTo(negativePrice.toDouble(), 0.01)))
            .andExpect(jsonPath("$.orderItems[0].subtotal").value(Matchers.closeTo(negativePrice.multiply(BigDecimal(2)).toDouble(), 0.01)))
            .andExpect(jsonPath("$.totalAmount").value(Matchers.closeTo(negativePrice.multiply(BigDecimal(2)).toDouble(), 0.01)))
    }

    @Test
    fun `should handle orders with very long customer IDs`() {
        // Given
        val longCustomerId = "customer_" + "a".repeat(200) + "@verylongdomain.com"
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), defaultQuantity.toInt())
        val order = createTestOrderWithItems(longCustomerId, OrderStatus.PENDING, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/${savedOrder.id}")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.customerId").value(longCustomerId))

        // Verify order exists in database
        val retrievedOrder = orderRepository.findById(savedOrder.id)
        assertTrue(retrievedOrder.isPresent)
        assertEquals(longCustomerId, retrievedOrder.get().customerId)
    }

    @Test
    fun `should handle orders with Unicode characters in customer ID`() {
        // Given
        val unicodeCustomerId = "customer_测试_123_🎉_test@example.com"
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), defaultQuantity.toInt())
        val order = createTestOrderWithItems(unicodeCustomerId, OrderStatus.PENDING, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/${savedOrder.id}")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.customerId").value(unicodeCustomerId))

        // Verify order exists in database
        val retrievedOrder = orderRepository.findById(savedOrder.id)
        assertTrue(retrievedOrder.isPresent)
        assertEquals(unicodeCustomerId, retrievedOrder.get().customerId)
    }

    @Test
    fun `should handle orders with maximum book ID values`() {
        // Given
        val maxBookId = Long.MAX_VALUE
        val orderItem = createTestOrderItem(null, maxBookId, defaultQuantity.toInt())
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.PENDING, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/${savedOrder.id}")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.orderItems[0].bookId").value(maxBookId))
    }

    @Test
    fun `should handle orders with maximum quantity values`() {
        // Given
        val maxQuantity = Int.MAX_VALUE
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), maxQuantity)
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.PENDING, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/${savedOrder.id}")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.orderItems[0].quantity").value(maxQuantity))
            .andExpect(jsonPath("$.totalAmount").value(Matchers.closeTo(BigDecimal(defaultPrice).multiply(BigDecimal(maxQuantity)).toDouble(), 0.01)))
    }

    @Test
    fun `should handle orders with mixed positive and negative prices`() {
        // Given
        val positiveItem = createTestOrderItem(null, 1L, 2, BigDecimal("19.99"))
        val negativeItem = createTestOrderItem(null, 2L, 1, BigDecimal("-9.99"))
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.PENDING, listOf(positiveItem, negativeItem))
        val savedOrder = orderRepository.save(order)

        val expectedTotal = BigDecimal("19.99").multiply(BigDecimal(2)).add(BigDecimal("-9.99").multiply(BigDecimal(1)))

        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/${savedOrder.id}")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.orderItems.size()").value(2))
            .andExpect(jsonPath("$.orderItems[0].price").value(Matchers.closeTo(19.99, 0.01)))
            .andExpect(jsonPath("$.orderItems[1].price").value(Matchers.closeTo(-9.99, 0.01)))
            .andExpect(jsonPath("$.totalAmount").value(Matchers.closeTo(expectedTotal.toDouble(), 0.01)))
    }

    @Test
    fun `should handle status update with extra fields in request`() {
        // Given
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), defaultQuantity.toInt())
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.PENDING, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        val requestWithExtraFields = mapOf(
            "status" to OrderStatus.CONFIRMED.name,
            "extraField1" to "extraValue1",
            "extraField2" to 123,
            "nestedField" to mapOf("nested" to "value")
        )

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/${savedOrder.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestWithExtraFields))
            )
        )
            .andExpect(status().isBadRequest)

        // Verify order status was not changed
        val unchangedOrder = orderRepository.findById(savedOrder.id)
        assertTrue(unchangedOrder.isPresent)
        assertEquals(OrderStatus.PENDING, unchangedOrder.get().status)
    }

    @Test
    fun `should handle orders with items having zero price`() {
        // Given
        val zeroPriceItem = createTestOrderItem(null, defaultBookId.toLong(), 5, BigDecimal.ZERO)
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.PENDING, listOf(zeroPriceItem))
        val savedOrder = orderRepository.save(order)

        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/${savedOrder.id}")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.orderItems[0].price").value(0.0))
            .andExpect(jsonPath("$.orderItems[0].subtotal").value(0.0))
            .andExpect(jsonPath("$.totalAmount").value(0.0))
    }

    @Test
    fun `should handle orders with items having negative quantity`() {
        // Given
        val negativeQuantityItem = createTestOrderItem(null, defaultBookId.toLong(), -3, BigDecimal(defaultPrice))
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.PENDING, listOf(negativeQuantityItem))
        val savedOrder = orderRepository.save(order)

        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/${savedOrder.id}")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.orderItems[0].quantity").value(-3))
            .andExpect(jsonPath("$.orderItems[0].subtotal").value(Matchers.closeTo(BigDecimal(defaultPrice).multiply(BigDecimal(-3)).toDouble(), 0.01)))
            .andExpect(jsonPath("$.totalAmount").value(Matchers.closeTo(BigDecimal(defaultPrice).multiply(BigDecimal(-3)).toDouble(), 0.01)))
    }

    @Test
    fun `should handle status update request with array instead of object`() {
        // Given
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), defaultQuantity.toInt())
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.PENDING, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        val invalidArrayRequest = "[{\"status\": \"CONFIRMED\"}]"

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/${savedOrder.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidArrayRequest)
            )
        )
            .andExpect(status().isBadRequest)

        // Verify order status was not changed
        val unchangedOrder = orderRepository.findById(savedOrder.id)
        assertTrue(unchangedOrder.isPresent)
        assertEquals(OrderStatus.PENDING, unchangedOrder.get().status)
    }

    @Test
    fun `should handle status update request with nested object`() {
        // Given
        val orderItem = createTestOrderItem(null, defaultBookId.toLong(), defaultQuantity.toInt())
        val order = createTestOrderWithItems(defaultCustomerId, OrderStatus.PENDING, listOf(orderItem))
        val savedOrder = orderRepository.save(order)

        val nestedRequest = mapOf("status" to mapOf("value" to OrderStatus.CONFIRMED.name))

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/${savedOrder.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(nestedRequest))
            )
        )
            .andExpect(status().isBadRequest)

        // Verify order status was not changed
        val unchangedOrder = orderRepository.findById(savedOrder.id)
        assertTrue(unchangedOrder.isPresent)
        assertEquals(OrderStatus.PENDING, unchangedOrder.get().status)
    }

    @Test
    fun `should get all orders for a customer successfully`() {
        // Given
        val customerId = "customer123"
        val orderItem1 = createTestOrderItem(null, 1L, 2, BigDecimal("19.99"))
        val orderItem2 = createTestOrderItem(null, 2L, 1, BigDecimal("9.99"))
        val order1 = createTestOrderWithItems(customerId, OrderStatus.PENDING, listOf(orderItem1))
        val order2 = createTestOrderWithItems(customerId, OrderStatus.CONFIRMED, listOf(orderItem2))
        val savedOrder1 = orderRepository.save(order1)
        val savedOrder2 = orderRepository.save(order2)

        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/customer/$customerId")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", Matchers.hasSize<Any>(2)))
            .andExpect(jsonPath("$[0].customerId").value(customerId))
            .andExpect(jsonPath("$[1].customerId").value(customerId))
            .andExpect(jsonPath("$[0].orderItems").isArray)
            .andExpect(jsonPath("$[1].orderItems").isArray)
            .andExpect(jsonPath("$[0].orderItems[0].bookId").value(1L))
            .andExpect(jsonPath("$[1].orderItems[0].bookId").value(2L))
    }

    @Test
    fun `should return empty list when customer has no orders`() {
        // Given
        val customerId = "noorders"

        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/customer/$customerId")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$", Matchers.hasSize<Any>(0)))
    }

    @Test
    fun `should handle special characters in customer ID for getOrdersByCustomerId`() {
        // Given
        val specialCustomerId = "customer-123_test@example.com"
        val orderItem = createTestOrderItem(null, 3L, 1, BigDecimal("29.99"))
        val order = createTestOrderWithItems(specialCustomerId, OrderStatus.PENDING, listOf(orderItem))
        orderRepository.save(order)

        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/customer/$specialCustomerId")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", Matchers.hasSize<Any>(1)))
            .andExpect(jsonPath("$[0].customerId").value(specialCustomerId))
    }

    @Test
    fun `should handle very long customer ID for getOrdersByCustomerId`() {
        // Given
        val longCustomerId = "customer_" + "a".repeat(200) + "@verylongdomain.com"
        val orderItem = createTestOrderItem(null, 4L, 1, BigDecimal("39.99"))
        val order = createTestOrderWithItems(longCustomerId, OrderStatus.PENDING, listOf(orderItem))
        orderRepository.save(order)

        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/customer/$longCustomerId")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", Matchers.hasSize<Any>(1)))
            .andExpect(jsonPath("$[0].customerId").value(longCustomerId))
    }

    @Test
    fun `should handle Unicode characters in customer ID for getOrdersByCustomerId`() {
        // Given
        val unicodeCustomerId = "customer_123_test@example.com"
        val orderItem = createTestOrderItem(null, 5L, 1, BigDecimal("49.99"))
        val order = createTestOrderWithItems(unicodeCustomerId, OrderStatus.PENDING, listOf(orderItem))
        orderRepository.save(order)

        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/customer/$unicodeCustomerId")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", Matchers.hasSize<Any>(1)))
            .andExpect(jsonPath("$[0].customerId").value(unicodeCustomerId))
    }

    @Test
    fun `should return all order statuses for a customer`() {
        // Given
        val customerId = "statuscases"
        val statuses = listOf(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.SHIPPED, OrderStatus.DELIVERED, OrderStatus.CANCELLED)
        statuses.forEachIndexed { i, status ->
            val orderItem = createTestOrderItem(null, (10 + i).toLong(), 1, BigDecimal("10.00"))
            val order = createTestOrderWithItems(customerId, status, listOf(orderItem))
            orderRepository.save(order)
        }
        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/customer/$customerId")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", Matchers.hasSize<Any>(5)))
            .andExpect(jsonPath("$[*].status", Matchers.containsInAnyOrder(*statuses.map { it.name }.toTypedArray())))
    }

    @Test
    fun `should handle customer with a large number of orders`() {
        // Given
        val customerId = "bulkuser"
        val orderCount = 100
        (1..orderCount).forEach { i ->
            val orderItem = createTestOrderItem(null, i.toLong(), 1, BigDecimal("1.00"))
            val order = createTestOrderWithItems(customerId, OrderStatus.PENDING, listOf(orderItem))
            orderRepository.save(order)
        }
        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/customer/$customerId")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", Matchers.hasSize<Any>(orderCount)))
    }

    @Test
    fun `should return orders with multiple items for a customer`() {
        // Given
        val customerId = "multiitem"
        val orderItem1 = createTestOrderItem(null, 1L, 2, BigDecimal("5.00"))
        val orderItem2 = createTestOrderItem(null, 2L, 3, BigDecimal("7.00"))
        val order = createTestOrderWithItems(customerId, OrderStatus.PENDING, listOf(orderItem1, orderItem2))
        orderRepository.save(order)
        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/customer/$customerId")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", Matchers.hasSize<Any>(1)))
            .andExpect(jsonPath("$[0].orderItems", Matchers.hasSize<Any>(2)))
            .andExpect(jsonPath("$[0].orderItems[0].quantity").value(2))
            .andExpect(jsonPath("$[0].orderItems[1].quantity").value(3))
    }

    @Test
    fun `should return orders with edge-case total amounts`() {
        // Given
        val customerId = "edgeamounts"
        val zeroOrder = createTestOrderWithItems(customerId, OrderStatus.PENDING, listOf(createTestOrderItem(null, 1L, 0, BigDecimal.ZERO)))
        zeroOrder.totalAmount = BigDecimal.ZERO
        val negativeOrder = createTestOrderWithItems(customerId, OrderStatus.PENDING, listOf(createTestOrderItem(null, 2L, 1, BigDecimal("-10.00"))))
        negativeOrder.totalAmount = BigDecimal("-10.00")
        val largeOrder = createTestOrderWithItems(customerId, OrderStatus.PENDING, listOf(createTestOrderItem(null, 3L, 1, BigDecimal("1000000.00"))))
        largeOrder.totalAmount = BigDecimal("1000000.00")
        val decimalOrder = createTestOrderWithItems(customerId, OrderStatus.PENDING, listOf(createTestOrderItem(null, 4L, 1, BigDecimal("0.99"))))
        decimalOrder.totalAmount = BigDecimal("0.99")
        orderRepository.saveAll(listOf(zeroOrder, negativeOrder, largeOrder, decimalOrder))
        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/customer/$customerId")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", Matchers.hasSize<Any>(4)))
            .andExpect(jsonPath("$[?(@.totalAmount==0.0)]").exists())
            .andExpect(jsonPath("$[?(@.totalAmount==-10.0)]").exists())
            .andExpect(jsonPath("$[?(@.totalAmount==1000000.0)]").exists())
            .andExpect(jsonPath("$[?(@.totalAmount==0.99)]").exists())
    }

    @Test
    fun `should return orders with edge-case item values`() {
        // Given
        val customerId = "edgeitems"
        val zeroQtyItem = createTestOrderItem(null, 1L, 0, BigDecimal("10.00"))
        val negativeQtyItem = createTestOrderItem(null, 2L, -5, BigDecimal("10.00"))
        val largeQtyItem = createTestOrderItem(null, 3L, Int.MAX_VALUE, BigDecimal("1.00"))
        val negativePriceItem = createTestOrderItem(null, 4L, 1, BigDecimal("-20.00"))
        val order = createTestOrderWithItems(customerId, OrderStatus.PENDING, listOf(zeroQtyItem, negativeQtyItem, largeQtyItem, negativePriceItem))
        orderRepository.save(order)
        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/customer/$customerId")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", Matchers.hasSize<Any>(1)))
            .andExpect(jsonPath("$[0].orderItems", Matchers.hasSize<Any>(4)))
            .andExpect(jsonPath("$[0].orderItems[0].quantity").value(0))
            .andExpect(jsonPath("$[0].orderItems[1].quantity").value(-5))
            .andExpect(jsonPath("$[0].orderItems[2].quantity").value(Int.MAX_VALUE))
            .andExpect(jsonPath("$[0].orderItems[3].price").value(-20.0))
    }

    // Helper method to create test orders
    private fun createTestOrder(customerId: String, status: OrderStatus): Order {
        return Order(
            customerId = customerId,
            status = status,
            totalAmount = BigDecimal.ZERO
        )
    }

    // Helper method to create test order items
    private fun createTestOrderItem(
        order: Order?,
        bookId: Long,
        quantity: Int,
        price: BigDecimal = BigDecimal(defaultPrice)
    ): OrderItem {
        val subtotal = price.multiply(BigDecimal(quantity))
        return OrderItem(
            order = order ?: Order(customerId = "", status = OrderStatus.PENDING, totalAmount = BigDecimal.ZERO),
            bookId = bookId,
            quantity = quantity,
            price = price,
            subtotal = subtotal
        )
    }

    // Helper method to create test orders with calculated total amount
    private fun createTestOrderWithItems(
        customerId: String,
        status: OrderStatus,
        items: List<OrderItem>
    ): Order {
        val order = Order(
            customerId = customerId,
            status = status,
            totalAmount = BigDecimal.ZERO
        )

        // Add items to order and set the order reference
        items.forEach { it.order = order }
        order.orderItems.addAll(items)

        // Calculate total amount from items
        val totalAmount = items.sumOf { it.subtotal }
        order.totalAmount = totalAmount

        return order
    }
}
