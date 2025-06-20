package com.smartdocument.bookinventory.service

import com.smartdocument.bookinventory.exception.BookInventoryServiceException
import com.smartdocument.bookinventory.model.Book
import com.smartdocument.bookinventory.repository.BookRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class BookServiceTest {
    private val bookRepository: BookRepository = mock()
    private lateinit var bookService: BookService

    private lateinit var sampleBook: Book

    @BeforeEach
    fun setUp() {
        bookService = BookService(bookRepository)
        sampleBook = Book(
            id = 1L,
            title = "The Great Gatsby",
            author = "F. Scott Fitzgerald",
            genre = "Fiction",
            isbn = "978-0743273565",
            price = BigDecimal("12.99"),
            quantity = 50,
            language = "English",
            publisher = "Scribner",
            publishedDate = LocalDate.of(1925, 4, 10),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    @Test
    fun `getAllBooks should return all books`() {
        whenever(bookRepository.findAll()).thenReturn(listOf(sampleBook))
        val result = bookService.getAllBooks()
        assertEquals(1, result.size)
        assertEquals(sampleBook, result[0])
    }

    @Test
    fun `getBookById should return book if exists`() {
        whenever(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook))
        val result = bookService.getBookById(1L)
        assertEquals(sampleBook, result)
    }

    @Test
    fun `getBookById should throw if not found`() {
        whenever(bookRepository.findById(2L)).thenReturn(Optional.empty())
        assertThrows<NoSuchElementException> { bookService.getBookById(2L) }
    }

    @Test
    fun `createBook should save and return new book`() {
        val book = sampleBook.copy()
        book.onCreate() // Simulate JPA @PrePersist
        whenever(bookRepository.findByIsbn(book.isbn)).thenReturn(null)
        whenever(bookRepository.save(book)).thenReturn(book)
        val result = bookService.createBook(book)
        assertEquals(book.title, result.title)
        assertNotNull(result.createdAt)
        assertNotNull(result.updatedAt)
    }

    @Test
    fun `createBook should throw if ISBN exists`() {
        whenever(bookRepository.findByIsbn(sampleBook.isbn)).thenReturn(sampleBook)
        assertThrows<BookInventoryServiceException> { bookService.createBook(sampleBook) }
    }

    @Test
    fun `updateBook should update and return book if exists`() {
        val book = sampleBook.copy()
        book.onCreate()
        whenever(bookRepository.findById(1L)).thenReturn(Optional.of(book))
        book.title = "Updated"
        book.onUpdate() // Simulate JPA @PreUpdate
        whenever(bookRepository.save(book)).thenReturn(book)
        val result = bookService.updateBook(1L, book)
        assertEquals("Updated", result.title)
        assertNotNull(result.createdAt)
        assertNotNull(result.updatedAt)
    }

    @Test
    fun `updateBook should throw if not found`() {
        whenever(bookRepository.findById(2L)).thenReturn(Optional.empty())
        assertThrows<NoSuchElementException> { bookService.updateBook(2L, sampleBook) }
    }

    @Test
    fun `deleteBook should delete if exists`() {
        whenever(bookRepository.existsById(1L)).thenReturn(true)
        bookService.deleteBook(1L)
        verify(bookRepository).deleteById(1L)
    }

    @Test
    fun `deleteBook should throw if not found`() {
        whenever(bookRepository.existsById(2L)).thenReturn(false)
        assertThrows<NoSuchElementException> { bookService.deleteBook(2L) }
    }

    @Test
    fun `updateInventory should update quantity if book exists`() {
        val book = sampleBook.copy()
        book.onCreate()
        whenever(bookRepository.findById(1L)).thenReturn(Optional.of(book))
        whenever(bookRepository.save(book)).thenReturn(book)
        val result = bookService.updateInventory(1L, 100)
        assertEquals(100, result.quantity)
        assertNotNull(result.createdAt)
        assertNotNull(result.updatedAt)
    }

    @Test
    fun `updateInventory should throw if not found`() {
        whenever(bookRepository.findById(2L)).thenReturn(Optional.empty())
        assertThrows<NoSuchElementException> { bookService.updateInventory(2L, 100) }
    }

    @Test
    fun `getAllGenres should return unique genres`() {
        whenever(bookRepository.findAllGenres()).thenReturn(listOf("Fiction", "Science Fiction"))
        val result = bookService.getAllGenres()
        assertEquals(listOf("Fiction", "Science Fiction"), result)
    }

    @Test
    fun `searchBooksAdvanced should return paged results`() {
        val pageable = PageRequest.of(0, 10)
        val page: Page<Book> = PageImpl(listOf(sampleBook), pageable, 1)
        whenever(bookRepository.findAll(any<Specification<Book>>(), eq(pageable))).thenReturn(page)
        val result = bookService.searchBooksAdvanced("Gatsby", null, null, null, null, null, null, pageable)
        assertEquals(1, result.totalElements)
        assertEquals(sampleBook, result.content[0])
    }

    @Test
    fun `createBook should throw when required fields are blank`() {
        val invalidBook = sampleBook.copy(title = "", author = "", isbn = "", genre = "", language = "", publisher = "")
        invalidBook.onCreate()
        whenever(bookRepository.findByIsbn(any())).thenReturn(null)
        assertThrows<Exception> { bookService.createBook(invalidBook) }
    }

    @Test
    fun `createBook should throw when price is negative or zero`() {
        val zeroPriceBook = sampleBook.copy(price = BigDecimal.ZERO)
        zeroPriceBook.onCreate()
        whenever(bookRepository.findByIsbn(zeroPriceBook.isbn)).thenReturn(null)
        assertThrows<Exception> { bookService.createBook(zeroPriceBook) }

        val negativePriceBook = sampleBook.copy(price = BigDecimal(-1))
        negativePriceBook.onCreate()
        whenever(bookRepository.findByIsbn(negativePriceBook.isbn)).thenReturn(null)
        assertThrows<Exception> { bookService.createBook(negativePriceBook) }
    }

    @Test
    fun `createBook should throw when quantity is negative`() {
        val negativeQuantityBook = sampleBook.copy(quantity = -10)
        negativeQuantityBook.onCreate()
        whenever(bookRepository.findByIsbn(negativeQuantityBook.isbn)).thenReturn(null)
        assertThrows<Exception> { bookService.createBook(negativeQuantityBook) }
    }

    @Test
    fun `updateBook should throw when updating non-existent book`() {
        whenever(bookRepository.findById(999L)).thenReturn(Optional.empty())
        assertThrows<NoSuchElementException> { bookService.updateBook(999L, sampleBook) }
    }

    @Test
    fun `deleteBook should throw when deleting non-existent book`() {
        whenever(bookRepository.existsById(999L)).thenReturn(false)
        assertThrows<NoSuchElementException> { bookService.deleteBook(999L) }
    }

    @Test
    fun `createBook should allow special characters and max length`() {
        val specialBook = sampleBook.copy(
            title = "Tést!@# 600".repeat(50),
            author = "Äüößçñ!@# 600".repeat(50),
            genre = "Fiction",
            isbn = "978-0743273565",
            language = "日本語",
            publisher = "Pub!@# 600",
            description = null
        )
        specialBook.onCreate()
        whenever(bookRepository.findByIsbn(specialBook.isbn)).thenReturn(null)
        whenever(bookRepository.save(specialBook)).thenReturn(specialBook)
        val result = bookService.createBook(specialBook)
        assertEquals(specialBook.title, result.title)
        assertEquals(specialBook.author, result.author)
        assertNull(result.description)
    }

    @Test
    fun `createBook should allow null optional fields`() {
        val bookWithNullDescription = sampleBook.copy(description = null)
        bookWithNullDescription.onCreate()
        whenever(bookRepository.findByIsbn(bookWithNullDescription.isbn)).thenReturn(null)
        whenever(bookRepository.save(bookWithNullDescription)).thenReturn(bookWithNullDescription)
        val result = bookService.createBook(bookWithNullDescription)
        assertNull(result.description)
    }
}
