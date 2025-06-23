package com.smartdocument.bookinventory.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.beans.factory.annotation.Value

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Value("\${book.inventory.service.base-url}")
    private lateinit var baseUrl: String

    @Value("\${book.inventory.service.username}")
    private lateinit var username: String

    @Value("\${book.inventory.service.password}")
    private lateinit var password: String

    private fun basicAuthHeader(user: String, pass: String): String =
        "Basic " + java.util.Base64.getEncoder().encodeToString("$user:$pass".toByteArray())

    @Test
    fun `should return 401 Unauthorized for unauthenticated request`() {
        mockMvc.perform(get(baseUrl))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return 401 Unauthorized for wrong password`() {
        mockMvc.perform(get(baseUrl).header("Authorization", basicAuthHeader(username, "wrongpass")))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return 401 Unauthorized for wrong username`() {
        mockMvc.perform(get(baseUrl).header("Authorization", basicAuthHeader("wronguser", password)))
            .andExpect(status().isUnauthorized)
    }
}
