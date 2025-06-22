package com.smartdocument.bookinventory.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.smartdocument.bookinventory.dto.BookRequestDto
import com.smartdocument.bookinventory.model.Book
import com.smartdocument.bookinventory.repository.BookRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import java.math.BigDecimal
import java.time.LocalDate
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.junit.jupiter.api.Assertions.*

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class BookControllerIntegrationTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var bookRepository: BookRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var mockMvc: MockMvc

    // Externalized configuration properties
    @Value("\${book.inventory.service.username}")
    private lateinit var username: String

    @Value("\${book.inventory.service.password}")
    private lateinit var password: String

    @Value("\${book.inventory.service.base-url}")
    private lateinit var baseUrl: String

    @Value("\${test.book.default.price}")
    private lateinit var defaultPrice: String

    @Value("\${test.book.default.quantity}")
    private lateinit var defaultQuantity: String

    @Value("\${test.book.default.language}")
    private lateinit var defaultLanguage: String

    @Value("\${test.book.default.publisher}")
    private lateinit var defaultPublisher: String

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .build()

        // Clear database before each test
        bookRepository.deleteAll()
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
    fun `should create book successfully`() {
        // Given
        val bookRequest = BookRequestDto(
            title = "Test Book",
            author = "Test Author",
            isbn = "978-1234567890",
            genre = "Fiction",
            price = BigDecimal(defaultPrice),
            quantity = defaultQuantity.toInt(),
            description = "A test book",
            language = defaultLanguage,
            publisher = defaultPublisher,
            publishedDate = "2023-01-01"
        )

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                post(baseUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(bookRequest))
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.title").value("Test Book"))
            .andExpect(jsonPath("$.author").value("Test Author"))
            .andExpect(jsonPath("$.isbn").value("978-1234567890"))
            .andExpect(jsonPath("$.price").value(defaultPrice.toDouble()))
            .andExpect(jsonPath("$.quantity").value(defaultQuantity.toInt()))

        // Verify book was saved in database
        val savedBook = bookRepository.findByIsbn("978-1234567890")
        assertNotNull(savedBook)
        assertEquals("Test Book", savedBook!!.title)
    }

    @Test
    fun `should get all books`() {
        // Given
        val book1 = createTestBook("Book 1", "Author 1", "978-1111111111")
        val book2 = createTestBook("Book 2", "Author 2", "978-2222222222")
        bookRepository.saveAll(listOf(book1, book2))

        // When & Then
        mockMvc.perform(addBasicAuth(get(baseUrl)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.size()").value(2))
            .andExpect(jsonPath("$[0].title").value("Book 1"))
            .andExpect(jsonPath("$[1].title").value("Book 2"))
    }

    @Test
    fun `should get book by id`() {
        // Given
        val book = createTestBook("Test Book", "Test Author", "978-1234567890")
        val savedBook = bookRepository.save(book)

        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/${savedBook.id}")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(savedBook.id))
            .andExpect(jsonPath("$.title").value("Test Book"))
            .andExpect(jsonPath("$.author").value("Test Author"))
    }

    @Test
    fun `should update book successfully`() {
        // Given
        val book = createTestBook("Original Title", "Original Author", "978-1234567890")
        val savedBook = bookRepository.save(book)

        val updateRequest = BookRequestDto(
            title = "Updated Title",
            author = "Updated Author",
            isbn = "978-1234567890",
            genre = "Non-Fiction",
            price = BigDecimal("29.99"),
            quantity = 20,
            description = "Updated description",
            language = defaultLanguage,
            publisher = defaultPublisher,
            publishedDate = "2023-02-01"
        )

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                put("$baseUrl/${savedBook.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest))
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("Updated Title"))
            .andExpect(jsonPath("$.author").value("Updated Author"))
            .andExpect(jsonPath("$.price").value(29.99))
            .andExpect(jsonPath("$.quantity").value(20))

        // Verify database was updated
        val updatedBook = bookRepository.findById(savedBook.id).orElse(null)
        assertNotNull(updatedBook)
        assertEquals("Updated Title", updatedBook!!.title)
        assertEquals("Updated Author", updatedBook.author)
    }

    @Test
    fun `should delete book successfully`() {
        // Given
        val book = createTestBook("Test Book", "Test Author", "978-1234567890")
        val savedBook = bookRepository.save(book)

        // When & Then
        mockMvc.perform(addBasicAuth(delete("$baseUrl/${savedBook.id}")))
            .andExpect(status().isNoContent)

        // Verify book was deleted from database
        val deletedBook = bookRepository.findById(savedBook.id)
        assertTrue(deletedBook.isEmpty)
    }

    @Test
    fun `should get all genres`() {
        // Given
        val book1 = createTestBook("Book 1", "Author 1", "978-1111111111", "Fiction")
        val book2 = createTestBook("Book 2", "Author 2", "978-2222222222", "Non-Fiction")
        val book3 = createTestBook("Book 3", "Author 3", "978-3333333333", "Fiction")
        bookRepository.saveAll(listOf(book1, book2, book3))

        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/genres")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.size()").value(2))
            .andExpect(jsonPath("$").value(org.hamcrest.Matchers.hasItems("Fiction", "Non-Fiction")))
    }

    @Test
    fun `should update inventory successfully`() {
        // Given
        val book = createTestBook("Test Book", "Test Author", "978-1234567890", quantity = 10)
        val savedBook = bookRepository.save(book)

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/${savedBook.id}/inventory")
                    .param("quantity", "25")
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.quantity").value(25))

        // Verify database was updated
        val updatedBook = bookRepository.findById(savedBook.id).orElse(null)
        assertNotNull(updatedBook)
        assertEquals(25, updatedBook!!.quantity)
    }

    @Test
    fun `should return 400 when creating book with invalid data`() {
        // Given
        val invalidBookRequest = BookRequestDto(
            title = "", // Invalid: empty title
            author = "Test Author",
            isbn = "invalid-isbn", // Invalid: wrong format
            genre = "Fiction",
            price = BigDecimal("-10.00"), // Invalid: negative price
            quantity = -5, // Invalid: negative quantity
            description = "A test book",
            language = defaultLanguage,
            publisher = defaultPublisher,
            publishedDate = "invalid-date" // Invalid: wrong format
        )

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                post(baseUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidBookRequest))
            )
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 409 when creating book with duplicate ISBN`() {
        // Given
        val existingBook = createTestBook("Existing Book", "Author 1", "978-1234567890")
        bookRepository.save(existingBook)

        val duplicateBookRequest = BookRequestDto(
            title = "New Book",
            author = "Author 2",
            isbn = "978-1234567890", // Same ISBN as existing book
            genre = "Fiction",
            price = BigDecimal(defaultPrice),
            quantity = defaultQuantity.toInt(),
            description = "A new book",
            language = defaultLanguage,
            publisher = defaultPublisher,
            publishedDate = "2023-01-01"
        )

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                post(baseUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(duplicateBookRequest))
            )
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `should return 404 when getting book by non-existent id`() {
        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/99999")))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 404 when updating book with non-existent id`() {
        // Given
        val updateRequest = BookRequestDto(
            title = "Updated Title",
            author = "Updated Author",
            isbn = "978-1234567890",
            genre = "Fiction",
            price = BigDecimal(defaultPrice),
            quantity = defaultQuantity.toInt(),
            description = "Updated description",
            language = defaultLanguage,
            publisher = defaultPublisher,
            publishedDate = "2023-01-01"
        )

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                put("$baseUrl/99999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest))
            )
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 404 when deleting book with non-existent id`() {
        // When & Then
        mockMvc.perform(addBasicAuth(delete("$baseUrl/99999")))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 400 when updating inventory with negative quantity`() {
        // Given
        val book = createTestBook("Test Book", "Test Author", "978-1234567890")
        val savedBook = bookRepository.save(book)

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/${savedBook.id}/inventory")
                    .param("quantity", "-5")
            )
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 400 when creating book with missing required fields`() {
        // Given - Missing title and author
        val invalidBookRequest = BookRequestDto(
            title = "", // Missing title
            author = "", // Missing author
            isbn = "978-1234567890",
            genre = "Fiction",
            price = BigDecimal(defaultPrice),
            quantity = defaultQuantity.toInt(),
            description = "A test book",
            language = defaultLanguage,
            publisher = defaultPublisher,
            publishedDate = "2023-01-01"
        )

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                post(baseUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidBookRequest))
            )
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should return empty list when getting all genres with no books`() {
        // Given - Database is empty (cleared in setUp)

        // When & Then
        mockMvc.perform(addBasicAuth(get("$baseUrl/genres")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.size()").value(0))
    }

    @Test
    fun `should handle advanced search with no filters`() {
        // Given
        val book1 = createTestBook("Book 1", "Author 1", "978-1111111111", "Fiction")
        val book2 = createTestBook("Book 2", "Author 2", "978-2222222222", "Non-Fiction")
        bookRepository.saveAll(listOf(book1, book2))

        // When & Then - No filters should return all books
        mockMvc.perform(addBasicAuth(get("$baseUrl/advanced-search")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.size()").value(2))
            .andExpect(jsonPath("$.totalElements").value(2))
    }

    @Test
    fun `should handle advanced search with title filter`() {
        // Given
        val book1 = createTestBook("Java Programming", "Author 1", "978-1111111111", "Technical")
        val book2 = createTestBook("Python Basics", "Author 2", "978-2222222222", "Technical")
        val book3 = createTestBook("Fiction Novel", "Author 3", "978-3333333333", "Fiction")
        bookRepository.saveAll(listOf(book1, book2, book3))

        // When & Then - Search for "Java"
        mockMvc.perform(
            addBasicAuth(
                get("$baseUrl/advanced-search")
                    .param("title", "Java")
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.size()").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Java Programming"))
    }

    @Test
    fun `should handle advanced search with no results`() {
        // Given
        val book1 = createTestBook("Java Programming", "Author 1", "978-1111111111", "Technical")
        bookRepository.save(book1)

        // When & Then - Search for non-existent title
        mockMvc.perform(
            addBasicAuth(
                get("$baseUrl/advanced-search")
                    .param("title", "NonExistentBook")
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.size()").value(0))
            .andExpect(jsonPath("$.totalElements").value(0))
    }

    @Test
    fun `should handle advanced search with invalid date format`() {
        // Given
        val book1 = createTestBook("Test Book", "Author 1", "978-1111111111", "Fiction")
        bookRepository.save(book1)

        // When & Then - Invalid date format should return empty results
        mockMvc.perform(
            addBasicAuth(
                get("$baseUrl/advanced-search")
                    .param("publishedDate", "invalid-date-format")
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.size()").value(0))
    }

    @Test
    fun `should handle pagination in advanced search`() {
        // Given - Create 25 books
        val books = (1..25).map { i ->
            createTestBook("Book $i", "Author $i", "978-$i$i$i$i$i$i$i$i$i$i", "Fiction")
        }
        bookRepository.saveAll(books)

        // When & Then - First page with size 10
        mockMvc.perform(
            addBasicAuth(
                get("$baseUrl/advanced-search")
                    .param("page", "0")
                    .param("size", "10")
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.size()").value(10))
            .andExpect(jsonPath("$.totalElements").value(25))
            .andExpect(jsonPath("$.totalPages").value(3))
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(false))
    }

    @Test
    fun `should handle last page in pagination`() {
        // Given - Create 25 books
        val books = (1..25).map { i ->
            createTestBook("Book $i", "Author $i", "978-$i$i$i$i$i$i$i$i$i$i", "Fiction")
        }
        bookRepository.saveAll(books)

        // When & Then - Last page (page 2 with size 10)
        mockMvc.perform(
            addBasicAuth(
                get("$baseUrl/advanced-search")
                    .param("page", "2")
                    .param("size", "10")
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.size()").value(5))
            .andExpect(jsonPath("$.totalElements").value(25))
            .andExpect(jsonPath("$.last").value(true))
    }

    @Test
    fun `should handle boundary values for inventory update`() {
        // Given
        val book = createTestBook("Test Book", "Test Author", "978-1234567890")
        val savedBook = bookRepository.save(book)

        // When & Then - Zero quantity (boundary case)
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/${savedBook.id}/inventory")
                    .param("quantity", "0")
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.quantity").value(0))

        // Verify database was updated
        val updatedBook = bookRepository.findById(savedBook.id).orElse(null)
        assertNotNull(updatedBook)
        assertEquals(0, updatedBook!!.quantity)
    }

    @Test
    fun `should handle large quantity values`() {
        // Given
        val book = createTestBook("Test Book", "Test Author", "978-1234567890")
        val savedBook = bookRepository.save(book)

        // When & Then - Large quantity
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/${savedBook.id}/inventory")
                    .param("quantity", "999999")
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.quantity").value(999999))
    }

    @Test
    fun `should handle case-insensitive search`() {
        // Given
        val book1 = createTestBook("JAVA PROGRAMMING", "JOHN DOE", "978-1111111111", "Technical")
        val book2 = createTestBook("java basics", "jane smith", "978-2222222222", "Technical")
        bookRepository.saveAll(listOf(book1, book2))

        // When & Then - Case-insensitive search
        mockMvc.perform(
            addBasicAuth(
                get("$baseUrl/advanced-search")
                    .param("title", "java")
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.size()").value(2))
    }

    @Test
    fun `should handle special characters in search`() {
        // Given
        val book1 = createTestBook("C++ Programming", "Author 1", "978-1111111111", "Technical")
        val book2 = createTestBook("C# Basics", "Author 2", "978-2222222222", "Technical")
        bookRepository.saveAll(listOf(book1, book2))

        // When & Then - Search with special characters
        mockMvc.perform(
            addBasicAuth(
                get("$baseUrl/advanced-search")
                    .param("title", "C++")
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.size()").value(1))
            .andExpect(jsonPath("$.content[0].title").value("C++ Programming"))
    }

    @Test
    fun `should handle empty request body`() {
        // When & Then - Empty JSON body
        mockMvc.perform(
            addBasicAuth(
                post(baseUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
            )
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should handle malformed JSON`() {
        // When & Then - Malformed JSON
        mockMvc.perform(
            addBasicAuth(
                post(baseUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{ invalid json }")
            )
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should handle very long input values`() {
        // Given
        val longTitle = "A".repeat(255) // Reasonable long title (within typical DB limits)
        val bookRequest = BookRequestDto(
            title = longTitle,
            author = "Test Author",
            isbn = "978-1234567890",
            genre = "Fiction",
            price = BigDecimal(defaultPrice),
            quantity = defaultQuantity.toInt(),
            description = "A test book",
            language = defaultLanguage,
            publisher = defaultPublisher,
            publishedDate = "2023-01-01"
        )

        // When & Then - Should handle long input gracefully
        mockMvc.perform(
            addBasicAuth(
                post(baseUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(bookRequest))
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value(longTitle))
    }

    // ========== ADDITIONAL BUSINESS LOGIC EDGE CASES ==========

    @Test
    fun `should handle multiple genre search`() {
        // Given
        val book1 = createTestBook("Book 1", "Author 1", "978-1111111111", "Fiction")
        val book2 = createTestBook("Book 2", "Author 2", "978-2222222222", "Non-Fiction")
        val book3 = createTestBook("Book 3", "Author 3", "978-3333333333", "Fiction")
        bookRepository.saveAll(listOf(book1, book2, book3))

        // When & Then - Search by genre
        mockMvc.perform(
            addBasicAuth(
                get("$baseUrl/advanced-search")
                    .param("genre", "Fiction")
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.size()").value(2))
            .andExpect(jsonPath("$.content[0].genre").value("Fiction"))
            .andExpect(jsonPath("$.content[1].genre").value("Fiction"))
    }

    @Test
    fun `should handle author search with partial match`() {
        // Given
        val book1 = createTestBook("Book 1", "John Smith", "978-1111111111", "Fiction")
        val book2 = createTestBook("Book 2", "Jane Smith", "978-2222222222", "Fiction")
        val book3 = createTestBook("Book 3", "Bob Johnson", "978-3333333333", "Fiction")
        bookRepository.saveAll(listOf(book1, book2, book3))

        // When & Then - Partial author search
        mockMvc.perform(
            addBasicAuth(
                get("$baseUrl/advanced-search")
                    .param("author", "Smith")
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.size()").value(2))
    }

    @Test
    fun `should handle ISBN exact match search`() {
        // Given
        val book1 = createTestBook("Book 1", "Author 1", "978-1111111111", "Fiction")
        val book2 = createTestBook("Book 2", "Author 2", "978-2222222222", "Fiction")
        bookRepository.saveAll(listOf(book1, book2))

        // When & Then - Exact ISBN search
        mockMvc.perform(
            addBasicAuth(
                get("$baseUrl/advanced-search")
                    .param("isbn", "978-1111111111")
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.size()").value(1))
            .andExpect(jsonPath("$.content[0].isbn").value("978-1111111111"))
    }

    @Test
    fun `should handle publisher search`() {
        // Given
        val book1 = createTestBook("Book 1", "Author 1", "978-1111111111", "Fiction")
        val book2 = createTestBook("Book 2", "Author 2", "978-2222222222", "Fiction")
        bookRepository.saveAll(listOf(book1, book2))

        // When & Then - Publisher search
        mockMvc.perform(
            addBasicAuth(
                get("$baseUrl/advanced-search")
                    .param("publisher", defaultPublisher)
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.size()").value(2))
    }

    @Test
    fun `should handle language search`() {
        // Given
        val book1 = createTestBook("Book 1", "Author 1", "978-1111111111", "Fiction")
        val book2 = createTestBook("Book 2", "Author 2", "978-2222222222", "Fiction")
        bookRepository.saveAll(listOf(book1, book2))

        // When & Then - Language search
        mockMvc.perform(
            addBasicAuth(
                get("$baseUrl/advanced-search")
                    .param("language", defaultLanguage)
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.size()").value(2))
    }

    @Test
    fun `should handle multiple filter combination`() {
        // Given
        val book1 = createTestBook("Java Programming", "John Smith", "978-1111111111", "Technical")
        val book2 = createTestBook("Python Basics", "Jane Smith", "978-2222222222", "Technical")
        val book3 = createTestBook("Fiction Novel", "Bob Johnson", "978-3333333333", "Fiction")
        bookRepository.saveAll(listOf(book1, book2, book3))

        // When & Then - Multiple filters
        mockMvc.perform(
            addBasicAuth(
                get("$baseUrl/advanced-search")
                    .param("title", "Java")
                    .param("author", "Smith")
                    .param("genre", "Technical")
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.size()").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Java Programming"))
    }

    @Test
    fun `should handle inventory update with zero and then positive`() {
        // Given
        val book = createTestBook("Test Book", "Test Author", "978-1234567890", quantity = 10)
        val savedBook = bookRepository.save(book)

        // When & Then - Set to zero
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/${savedBook.id}/inventory")
                    .param("quantity", "0")
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.quantity").value(0))

        // Then set back to positive
        mockMvc.perform(
            addBasicAuth(
                patch("$baseUrl/${savedBook.id}/inventory")
                    .param("quantity", "5")
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.quantity").value(5))
    }

    @Test
    fun `should handle book update preserving creation timestamp`() {
        // Given
        val book = createTestBook("Original Title", "Original Author", "978-1234567890")
        val savedBook = bookRepository.save(book)
        val originalCreatedAt = savedBook.createdAt

        val updateRequest = BookRequestDto(
            title = "Updated Title",
            author = "Updated Author",
            isbn = "978-1234567890",
            genre = "Non-Fiction",
            price = BigDecimal("29.99"),
            quantity = 20,
            description = "Updated description",
            language = defaultLanguage,
            publisher = defaultPublisher,
            publishedDate = "2023-02-01"
        )

        // When & Then
        mockMvc.perform(
            addBasicAuth(
                put("$baseUrl/${savedBook.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest))
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("Updated Title"))

        // Verify creation timestamp is preserved
        val updatedBook = bookRepository.findById(savedBook.id).orElse(null)
        assertNotNull(updatedBook)
        assertEquals(originalCreatedAt, updatedBook!!.createdAt)
    }

    @Test
    fun `should handle sorting in advanced search`() {
        // Given
        val book1 = createTestBook("Zebra Book", "Author 1", "978-1111111111", "Fiction")
        val book2 = createTestBook("Alpha Book", "Author 2", "978-2222222222", "Fiction")
        val book3 = createTestBook("Beta Book", "Author 3", "978-3333333333", "Fiction")
        bookRepository.saveAll(listOf(book1, book2, book3))

        // When & Then - Sort by title ascending
        mockMvc.perform(
            addBasicAuth(
                get("$baseUrl/advanced-search")
                    .param("sort", "title,asc")
            )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.size()").value(3))
            .andExpect(jsonPath("$.content[0].title").value("Alpha Book"))
            .andExpect(jsonPath("$.content[1].title").value("Beta Book"))
            .andExpect(jsonPath("$.content[2].title").value("Zebra Book"))
    }

    // Helper method to create test books using externalized properties
    private fun createTestBook(
        title: String,
        author: String,
        isbn: String,
        genre: String = "Fiction",
        quantity: Int = defaultQuantity.toInt()
    ): Book {
        return Book(
            title = title,
            author = author,
            isbn = isbn,
            genre = genre,
            price = BigDecimal(defaultPrice),
            quantity = quantity,
            description = "A test book",
            language = defaultLanguage,
            publisher = defaultPublisher,
            publishedDate = LocalDate.of(2023, 1, 1)
        )
    }
}
