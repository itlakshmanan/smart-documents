package com.smartdocument.bookinventory.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureWebMvc
class BookControllerSecurityTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `should require authentication for protected endpoints`() {
        mockMvc.perform(get("/api/v1/books"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "bookadmin", password = "bookpass123", roles = ["ADMIN"])
    fun `should allow access with valid credentials`() {
        mockMvc.perform(get("/api/v1/books"))
            .andExpect(status().isOk)
    }

    @Test
    fun `should allow access to actuator endpoints without authentication`() {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk)
    }
}
