package com.smartdocument.bookinventory.controller

import com.smartdocument.bookinventory.model.Book
import com.smartdocument.bookinventory.service.BookService
import com.smartdocument.bookinventory.dto.BookRequestDto
import com.smartdocument.bookinventory.dto.BookResponseDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import com.smartdocument.bookinventory.mapper.BookMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.SortDefault
import org.springframework.data.domain.Sort
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import io.swagger.v3.oas.annotations.Operation

/**
 * REST controller for managing book inventory operations.
 * Exposes endpoints for CRUD operations, genre listing, inventory updates, and advanced search.
 * Uses BookService for business logic and BookMapper for DTO mapping.
 */
@RestController
@RequestMapping("/api/v1/books")
@Tag(name = "Book Management", description = "APIs for managing book inventory")
@SecurityRequirement(name = "basicAuth")
class BookController(
    private val bookService: BookService,
    private val bookMapper: BookMapper
) {
    private val logger: Logger = LoggerFactory.getLogger(BookController::class.java)

    /**
     * Retrieves all books in the inventory.
     *
     * @return a list of BookResponseDto objects representing all books
     */
    @GetMapping
    @Operation(
        summary = "Get all books",
        description = "Retrieves a list of all books available in the inventory. Returns a list of BookResponseDto objects."
    )
    fun getAllBooks(): ResponseEntity<List<BookResponseDto>> {
        logger.info("Fetching all books from inventory")
        val books = bookService.getAllBooks().map { bookMapper.toResponseDto(it) }
        return ResponseEntity.ok(books)
    }

    /**
     * Retrieves a specific book by its unique identifier.
     *
     * @param id the unique identifier of the book
     * @return the BookResponseDto for the requested book
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get book by ID",
        description = "Retrieves a specific book by its unique identifier. Returns the BookResponseDto for the requested book."
    )
    fun getBookById(@PathVariable id: Long): ResponseEntity<BookResponseDto> {
        logger.info("Fetching book with id: {}", id)
        val book = bookService.getBookById(id)
        return ResponseEntity.ok(bookMapper.toResponseDto(book))
    }

    /**
     * Retrieves all unique genres from the books in the inventory.
     *
     * @return a list of genre names
     */
    @GetMapping("/genres")
    @Operation(
        summary = "Get all genres",
        description = "Retrieves all unique genres from the books in the inventory. Returns a list of genre names."
    )
    fun getAllGenres(): ResponseEntity<List<String>> {
        logger.info("Fetching all available genres")
        val genres = bookService.getAllGenres()
        return ResponseEntity.ok(genres)
    }

    /**
     * Creates a new book in the inventory.
     *
     * @param bookRequestDto the DTO containing book information
     * @return the created BookResponseDto
     */
    @PostMapping
    @Operation(
        summary = "Create a new book",
        description = "Creates a new book in the inventory using the provided BookRequestDto. Returns the created BookResponseDto."
    )
    fun createBook(@Valid @RequestBody bookRequestDto: BookRequestDto): ResponseEntity<BookResponseDto> {
        logger.info("Creating new book: {} (ISBN: {})", bookRequestDto.title, bookRequestDto.isbn)
        val book = bookService.createBook(bookMapper.toEntity(bookRequestDto))
        return ResponseEntity.ok(bookMapper.toResponseDto(book))
    }

    /**
     * Updates an existing book's information.
     *
     * @param id the unique identifier of the book to update
     * @param bookRequestDto the DTO containing updated book information
     * @return the updated BookResponseDto
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "Update a book",
        description = "Updates an existing book's information by its unique identifier. Accepts a BookRequestDto and returns the updated BookResponseDto."
    )
    fun updateBook(
        @PathVariable id: Long,
        @Valid @RequestBody bookRequestDto: BookRequestDto
    ): ResponseEntity<BookResponseDto> {
        logger.info("Updating book with id: {}", id)
        val book = bookService.updateBook(id, bookMapper.toEntityWithId(bookRequestDto, id))
        return ResponseEntity.ok(bookMapper.toResponseDto(book))
    }

    /**
     * Updates the inventory quantity for a specific book.
     *
     * @param id the unique identifier of the book
     * @param quantity the new quantity to set
     * @return the updated BookResponseDto
     */
    @PatchMapping("/{id}/inventory")
    @Operation(
        summary = "Update book inventory",
        description = "Updates the inventory quantity for a specific book by its unique identifier. Accepts a quantity parameter and returns the updated BookResponseDto."
    )
    fun updateInventory(
        @PathVariable id: Long,
        @RequestParam quantity: Int
    ): ResponseEntity<BookResponseDto> {
        logger.info("Updating inventory for book id: {} to quantity: {}", id, quantity)
        val book = bookService.updateInventory(id, quantity)
        return ResponseEntity.ok(bookMapper.toResponseDto(book))
    }

    /**
     * Deletes a book from the inventory by its unique identifier.
     *
     * @param id the unique identifier of the book to delete
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete a book",
        description = "Deletes a book from the inventory by its unique identifier. Returns no content if successful."
    )
    fun deleteBook(@PathVariable id: Long): ResponseEntity<Unit> {
        logger.info("Attempting to delete book with id: {}", id)
        bookService.deleteBook(id)
        return ResponseEntity.noContent().build()
    }

    /**
     * Performs an advanced search for books with multiple optional filters and pagination.
     *
     * @param title optional title filter
     * @param author optional author filter
     * @param genre optional genre filter
     * @param isbn optional ISBN filter
     * @param language optional language filter
     * @param publisher optional publisher filter
     * @param publishedDate optional published date filter
     * @param pageable pagination and sorting information
     * @return a page of BookResponseDto objects matching the filters
     */
    @GetMapping("/advanced-search")
    @Operation(
        summary = "Advanced book search",
        description = "Performs an advanced search for books with multiple optional filters (title, author, genre, ISBN, language, publisher, published date) and supports pagination and sorting. Returns a page of BookResponseDto objects matching the filters."
    )
    fun searchBooksAdvanced(
        @RequestParam(required = false) title: String?,
        @RequestParam(required = false) author: String?,
        @RequestParam(required = false) genre: String?,
        @RequestParam(required = false) isbn: String?,
        @RequestParam(required = false) language: String?,
        @RequestParam(required = false) publisher: String?,
        @RequestParam(required = false) publishedDate: String?,
        @PageableDefault(size = 20) @SortDefault.SortDefaults(
            SortDefault(sort = ["title"], direction = Sort.Direction.ASC)
        ) pageable: Pageable
    ): Page<BookResponseDto> {
        logger.info("Performing advanced search with filters - title: {}, author: {}, genre: {}, isbn: {}, page: {}, size: {}",
                   title, author, genre, isbn, pageable.pageNumber, pageable.pageSize)
        val result = bookService.searchBooksAdvanced(title, author, genre, isbn, language, publisher, publishedDate, pageable)
            .map { bookMapper.toResponseDto(it) }
        logger.info("Advanced search completed: found {} books out of {} total", result.content.size, result.totalElements)
        return result
    }
}
