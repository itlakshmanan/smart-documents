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

/**
 * Service class for managing book inventory operations.
 * Handles business logic for CRUD operations, inventory updates, and advanced search.
 * Uses BookRepository for persistence and supports transactional operations where needed.
 */
@Service
class BookService(private val bookRepository: BookRepository) {

    private val logger = LoggerFactory.getLogger(BookService::class.java)

    /**
     * Retrieves all books from the inventory.
     *
     * @return a list of all Book entities in the database.
     */
    fun getAllBooks(): List<Book> {
        logger.info("Fetching all books from inventory")
        val books = bookRepository.findAll()
        logger.info("Retrieved {} books from inventory", books.size)
        return books
    }

    /**
     * Retrieves a book by its unique identifier.
     *
     * @param id the unique identifier of the book
     * @return the Book entity if found
     * @throws BookInventoryServiceException if the book is not found
     */
    fun getBookById(id: Long): Book {
        logger.info("Fetching book with id: {}", id)
        return bookRepository.findById(id)
            .orElseThrow {
                logger.error("Book not found with id: {}", id)
                BookInventoryServiceException(BookInventoryServiceException.Operation.BOOK_NOT_FOUND)
            }
            .also { logger.info("Successfully retrieved book: {} (id: {})", it.title, id) }
    }

    /**
     * Retrieves all unique genres from the books in the inventory.
     *
     * @return a list of unique genre names
     */
    fun getAllGenres(): List<String> {
        logger.info("Fetching all available genres")
        val genres = bookRepository.findAllGenres()
        logger.info("Retrieved {} unique genres: {}", genres.size, genres)
        return genres
    }

    /**
     * Creates a new book in the inventory.
     *
     * @param book the Book entity to create
     * @return the created Book entity
     * @throws BookInventoryServiceException if a book with the same ISBN already exists
     */
    @Transactional
    fun createBook(book: Book): Book {
        logger.info("Creating new book: {} (ISBN: {})", book.title, book.isbn)

        // Check for duplicate ISBN before creating the book
        if (bookRepository.findByIsbn(book.isbn) != null) {
            logger.error("Failed to create book: ISBN {} already exists", book.isbn)
            throw BookInventoryServiceException(BookInventoryServiceException.Operation.ISBN_ALREADY_EXISTS)
        }

        val savedBook = bookRepository.save(book)
        logger.info("Successfully created book: {} (id: {}, ISBN: {})", savedBook.title, savedBook.id, savedBook.isbn)
        return savedBook
    }

    /**
     * Updates an existing book's information.
     *
     * @param id the unique identifier of the book to update
     * @param updatedBook the Book entity with updated information
     * @return the updated Book entity
     * @throws BookInventoryServiceException if the book is not found
     */
    @Transactional
    fun updateBook(id: Long, updatedBook: Book): Book {
        logger.info("Updating book with id: {}", id)

        val existingBook = getBookById(id)
        logger.debug("Found existing book: {} (id: {})", existingBook.title, id)

        // Preserve the original creation timestamp
        val bookToSave = updatedBook.copy(createdAt = existingBook.createdAt)
        val savedBook = bookRepository.save(bookToSave)

        logger.info("Successfully updated book: {} (id: {})", savedBook.title, id)
        return savedBook
    }

    /**
     * Updates the inventory quantity for a specific book.
     *
     * @param id the unique identifier of the book
     * @param quantity the new quantity to set
     * @return the updated Book entity
     * @throws BookInventoryServiceException if the book is not found or quantity is negative
     */
    @Transactional
    fun updateInventory(id: Long, quantity: Int): Book {
        logger.info("Updating inventory for book id: {} to quantity: {}", id, quantity)

        // Validate quantity is not negative
        if (quantity < 0) {
            logger.error("Failed to update inventory: Quantity cannot be negative. Book id: {}, requested quantity: {}", id, quantity)
            throw BookInventoryServiceException(BookInventoryServiceException.Operation.NEGATIVE_QUANTITY)
        }

        val book = getBookById(id)
        val oldQuantity = book.quantity
        book.quantity = quantity

        val updatedBook = bookRepository.save(book)
        logger.info("Successfully updated inventory for book: {} (id: {}): {} -> {}",
                   updatedBook.title, id, oldQuantity, quantity)
        return updatedBook
    }

    /**
     * Deletes a book from the inventory by its unique identifier.
     *
     * @param id the unique identifier of the book to delete
     * @throws BookInventoryServiceException if the book is not found
     */
    @Transactional
    fun deleteBook(id: Long) {
        logger.info("Attempting to delete book with id: {}", id)

        // Check if the book exists before attempting deletion
        if (!bookRepository.existsById(id)) {
            logger.error("Failed to delete book: Book not found with id: {}", id)
            throw BookInventoryServiceException(BookInventoryServiceException.Operation.BOOK_NOT_FOUND)
        }

        bookRepository.deleteById(id)
        logger.info("Successfully deleted book with id: {}", id)
    }

    /**
     * Performs an advanced search for books with multiple optional filters and pagination.
     *
     * @param title optional title filter (partial match, case-insensitive)
     * @param author optional author filter (partial match, case-insensitive)
     * @param genre optional genre filter (exact match, case-insensitive)
     * @param isbn optional ISBN filter (exact match)
     * @param language optional language filter (exact match, case-insensitive)
     * @param publisher optional publisher filter (exact match, case-insensitive)
     * @param publishedDate optional published date filter (exact match)
     * @param pageable pagination and sorting information
     * @return a page of Book entities matching the filters
     */
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
        logger.info("Performing advanced book search with filters - title: {}, author: {}, genre: {}, isbn: {}, language: {}, publisher: {}, publishedDate: {}, page: {}, size: {}",
                   title, author, genre, isbn, language, publisher, publishedDate, pageable.pageNumber, pageable.pageSize)

        // Build dynamic specification based on provided filters
        val spec = Specification.where<Book>(null)
            .and(title?.let { titleLike(it) })
            .and(author?.let { authorLike(it) })
            .and(genre?.let { genreEquals(it) })
            .and(isbn?.let { isbnEquals(it) })
            .and(language?.let { languageEquals(it) })
            .and(publisher?.let { publisherEquals(it) })
            .and(publishedDate?.let { publishedDateEquals(it) })

        val result = bookRepository.findAll(spec, pageable)
        logger.info("Advanced search completed: found {} books out of {} total",
                   result.content.size, result.totalElements)
        return result
    }

    // Specification helper for case-insensitive title search
    private fun titleLike(title: String) = Specification<Book> { root, _, cb ->
        cb.like(cb.lower(root.get("title")), "%" + title.lowercase() + "%")
    }
    // Specification helper for case-insensitive author search
    private fun authorLike(author: String) = Specification<Book> { root, _, cb ->
        cb.like(cb.lower(root.get("author")), "%" + author.lowercase() + "%")
    }
    // Specification helper for case-insensitive genre search
    private fun genreEquals(genre: String) = Specification<Book> { root, _, cb ->
        cb.equal(cb.lower(root.get("genre")), genre.lowercase())
    }
    // Specification helper for exact ISBN match
    private fun isbnEquals(isbn: String) = Specification<Book> { root, _, cb ->
        cb.equal(root.get<String>("isbn"), isbn)
    }
    // Specification helper for case-insensitive language search
    private fun languageEquals(language: String) = Specification<Book> { root, _, cb ->
        cb.equal(cb.lower(root.get("language")), language.lowercase())
    }
    // Specification helper for case-insensitive publisher search
    private fun publisherEquals(publisher: String) = Specification<Book> { root, _, cb ->
        cb.equal(cb.lower(root.get("publisher")), publisher.lowercase())
    }
    // Specification helper for exact published date match
    private fun publishedDateEquals(publishedDate: String) = Specification<Book> { root, _, cb ->
        try {
            val localDate = java.time.LocalDate.parse(publishedDate)
            cb.equal(root.get<java.time.LocalDate>("publishedDate"), localDate)
        } catch (e: Exception) {
            // If parsing fails, return a specification that matches nothing
            cb.equal(cb.literal(1), cb.literal(0))
        }
    }
}
