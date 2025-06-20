package com.smartdocument.bookinventory.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.context.ActiveProfiles
import com.smartdocument.bookinventory.service.BookService
import com.smartdocument.bookinventory.mapper.BookMapper

@WebMvcTest(BookController::class)
@ActiveProfiles("test")
class BookControllerSecurityTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var bookService: BookService

    @MockBean
    private lateinit var bookMapper: BookMapper

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
}
