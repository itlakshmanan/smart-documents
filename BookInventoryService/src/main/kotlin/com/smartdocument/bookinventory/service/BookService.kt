package com.smartdocument.bookinventory.service

import com.smartdocument.bookinventory.exception.BookInventoryServiceException
import com.smartdocument.bookinventory.model.Book
import com.smartdocument.bookinventory.repository.BookRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import com.smartdocument.bookinventory.mapper.BookMapper
import org.slf4j.LoggerFactory

@Service
class BookService(private val bookRepository: BookRepository) {

    private val logger = LoggerFactory.getLogger(BookService::class.java)

    fun getAllBooks(): List<Book> {
        logger.info("Fetching all books from inventory")
        val books = bookRepository.findAll()
        logger.info("Retrieved {} books from inventory", books.size)
        return books
    }

    fun getBookById(id: Long): Book {
        logger.info("Fetching book with id: {}", id)
        return bookRepository.findById(id)
            .orElseThrow {
                logger.error("Book not found with id: {}", id)
                NoSuchElementException("Book not found with id: $id")
            }
            .also { logger.info("Successfully retrieved book: {} (id: {})", it.title, id) }
    }

    fun getAllGenres(): List<String> {
        logger.info("Fetching all available genres")
        val genres = bookRepository.findAllGenres()
        logger.info("Retrieved {} unique genres: {}", genres.size, genres)
        return genres
    }

    @Transactional
    fun createBook(book: Book): Book {
        logger.info("Creating new book: {} (ISBN: {})", book.title, book.isbn)

        if (bookRepository.findByIsbn(book.isbn) != null) {
            logger.error("Failed to create book: ISBN {} already exists", book.isbn)
            throw BookInventoryServiceException(BookInventoryServiceException.Operation.ISBN_ALREADY_EXISTS)
        }

        val savedBook = bookRepository.save(book)
        logger.info("Successfully created book: {} (id: {}, ISBN: {})", savedBook.title, savedBook.id, savedBook.isbn)
        return savedBook
    }

    @Transactional
    fun updateBook(id: Long, updatedBook: Book): Book {
        logger.info("Updating book with id: {}", id)

        val existingBook = getBookById(id)
        logger.debug("Found existing book: {} (id: {})", existingBook.title, id)

        val bookToSave = updatedBook.copy(createdAt = existingBook.createdAt)
        val savedBook = bookRepository.save(bookToSave)

        logger.info("Successfully updated book: {} (id: {})", savedBook.title, id)
        return savedBook
    }

    @Transactional
    fun updateInventory(id: Long, quantity: Int): Book {
        logger.info("Updating inventory for book id: {} to quantity: {}", id, quantity)

        val book = getBookById(id)
        val oldQuantity = book.quantity
        book.quantity = quantity

        val updatedBook = bookRepository.save(book)
        logger.info("Successfully updated inventory for book: {} (id: {}): {} -> {}",
                   updatedBook.title, id, oldQuantity, quantity)
        return updatedBook
    }

    @Transactional
    fun deleteBook(id: Long) {
        logger.info("Attempting to delete book with id: {}", id)

        if (!bookRepository.existsById(id)) {
            logger.error("Failed to delete book: Book not found with id: {}", id)
            throw NoSuchElementException("Book not found with id: $id")
        }

        bookRepository.deleteById(id)
        logger.info("Successfully deleted book with id: {}", id)
    }

    fun searchBooksAdvanced(
        title: String?,
        author: String?,
        genre: String?,
        isbn: String?,
        language: String?,
        publisher: String?,
        publishedDate: String?,
        pageable: Pageable
    ): Page<Book> {
        logger.info("Performing advanced book search with filters - title: {}, author: {}, genre: {}, isbn: {}, page: {}, size: {}",
                   title, author, genre, isbn, pageable.pageNumber, pageable.pageSize)

        val spec = Specification.where<Book>(null)
            .and(title?.let { titleLike(it) })
            .and(author?.let { authorLike(it) })
            .and(genre?.let { genreEquals(it) })
            .and(isbn?.let { isbnEquals(it) })
            // Add more filters as needed

        val result = bookRepository.findAll(spec, pageable)
        logger.info("Advanced search completed: found {} books out of {} total",
                   result.content.size, result.totalElements)
        return result
    }

    private fun titleLike(title: String) = Specification<Book> { root, _, cb ->
        cb.like(cb.lower(root.get("title")), "%" + title.lowercase() + "%")
    }
    private fun authorLike(author: String) = Specification<Book> { root, _, cb ->
        cb.like(cb.lower(root.get("author")), "%" + author.lowercase() + "%")
    }
    private fun genreEquals(genre: String) = Specification<Book> { root, _, cb ->
        cb.equal(cb.lower(root.get("genre")), genre.lowercase())
    }
    private fun isbnEquals(isbn: String) = Specification<Book> { root, _, cb ->
        cb.equal(root.get<String>("isbn"), isbn)
    }
}
