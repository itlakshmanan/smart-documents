package com.smartdocument.ordermanagement.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Integration tests for authentication boundaries.
 * Only tests authentication/authorization, not business logic.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc
) {
    private val baseUrl = "/api/v1/orders"

    @Test
    fun `should return 401 Unauthorized for unauthenticated request to get orders by customer`() {
        // Given
        val customerId = "customer123"
        val request = get("$baseUrl/customer/$customerId")
        // When & Then
        mockMvc.perform(request)
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return 401 Unauthorized for wrong password`() {
        val customerId = "customer123"
        mockMvc.perform(
            get("$baseUrl/customer/$customerId")
                .header("Authorization", basicAuth("orderadmin", "wrongpass"))
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return 401 Unauthorized for wrong username`() {
        val customerId = "customer123"
        mockMvc.perform(
            get("$baseUrl/customer/$customerId")
                .header("Authorization", basicAuth("wronguser", "orderpass123"))
        ).andExpect(status().isUnauthorized)
    }

    // Helper for HTTP Basic Auth header
    private fun basicAuth(username: String, password: String): String {
        val creds = "$username:$password"
        val encoded = java.util.Base64.getEncoder().encodeToString(creds.toByteArray())
        return "Basic $encoded"
    }
}
