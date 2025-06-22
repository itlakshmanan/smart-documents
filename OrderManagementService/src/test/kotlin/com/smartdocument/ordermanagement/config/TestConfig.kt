package com.smartdocument.ordermanagement.config

import com.smartdocument.ordermanagement.client.BookClient
import com.smartdocument.ordermanagement.dto.BookResponseDto
import io.mockk.every
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.math.BigDecimal

/**
 * Test configuration for integration tests.
 *
 * This configuration provides mock implementations of external dependencies
 * to avoid requiring external services during integration tests.
 */
@TestConfiguration
class TestConfig {

    /**
     * Provides a mocked BookClient for integration tests.
     *
     * This mock returns predefined book responses to avoid making
     * actual HTTP calls to the BookInventoryService during tests.
     *
     * @return Mocked BookClient instance
     */
    @Bean
    @Primary
    fun bookClient(): BookClient {
        val mockBookClient = mockk<BookClient>()

        // Mock responses for common test scenarios
        every { mockBookClient.getBookById(1L) } returns createTestBook(1L, "Test Book 1", BigDecimal("19.99"), 1000)
        every { mockBookClient.getBookById(2L) } returns createTestBook(2L, "Test Book 2", BigDecimal("29.99"), 1000)
        every { mockBookClient.getBookById(999L) } returns null // Non-existent book

        // Mock updateBookQuantity to always return true for successful updates
        every { mockBookClient.updateBookQuantity(any(), any()) } returns true

        return mockBookClient
    }

    /**
     * Creates a test book response for mocking.
     *
     * @param id Book ID
     * @param title Book title
     * @param price Book price
     * @param quantity Available quantity
     * @return BookResponseDto instance
     */
    private fun createTestBook(id: Long, title: String, price: BigDecimal, quantity: Int): BookResponseDto {
        return BookResponseDto(
            id = id,
            title = title,
            author = "Test Author",
            isbn = "978-test-$id",
            genre = "Test Genre",
            price = price,
            quantity = quantity,
            description = "Test book description",
            language = "English",
            publisher = "Test Publisher",
            publishedDate = "2023-01-01"
        )
    }
}
