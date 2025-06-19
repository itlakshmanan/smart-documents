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
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.security.SecurityRequirement

@RestController
@RequestMapping("/api/v1/books")
@Tag(name = "Book Management", description = "APIs for managing book inventory")
@SecurityRequirement(name = "basicAuth")
class BookController(
    private val bookService: BookService,
    private val bookMapper: BookMapper
) {

    @GetMapping
    @Operation(
        summary = "Get all books",
        description = "Retrieves a list of all books in the inventory"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved books",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = BookResponseDto::class))]
            ),
            ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required")
        ]
    )
    fun getAllBooks(): ResponseEntity<List<BookResponseDto>> =
        ResponseEntity.ok(bookService.getAllBooks().map { bookMapper.toResponseDto(it) })

    @GetMapping("/{id}")
    @Operation(
        summary = "Get book by ID",
        description = "Retrieves a specific book by its unique identifier"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved book",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = BookResponseDto::class))]
            ),
            ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            ApiResponse(responseCode = "404", description = "Book not found")
        ]
    )
    fun getBookById(
        @Parameter(description = "Unique identifier of the book", example = "1")
        @PathVariable id: Long
    ): ResponseEntity<BookResponseDto> =
        ResponseEntity.ok(bookMapper.toResponseDto(bookService.getBookById(id)))

    @GetMapping("/genres")
    @Operation(
        summary = "Get all genres",
        description = "Retrieves a list of all available book genres"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved genres",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = String::class))]
            ),
            ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required")
        ]
    )
    fun getAllGenres(): ResponseEntity<List<String>> =
        ResponseEntity.ok(bookService.getAllGenres())

    @PostMapping
    @Operation(
        summary = "Create a new book",
        description = "Creates a new book in the inventory"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully created book",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = BookResponseDto::class))]
            ),
            ApiResponse(responseCode = "400", description = "Bad request - Invalid input data"),
            ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            ApiResponse(responseCode = "409", description = "Conflict - ISBN already exists")
        ]
    )
    fun createBook(
        @Parameter(
            description = "Book information to create",
            required = true,
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = BookRequestDto::class),
                examples = [
                    ExampleObject(
                        name = "Sample Book",
                        value = """
                        {
                            "title": "The Great Gatsby",
                            "author": "F. Scott Fitzgerald",
                            "genre": "Fiction",
                            "isbn": "978-0743273565",
                            "price": 12.99,
                            "quantity": 50,
                            "language": "English",
                            "publisher": "Scribner",
                            "publishedDate": "1925-04-10"
                        }
                        """
                    )
                ]
            )]
        )
        @Valid @RequestBody bookRequestDto: BookRequestDto
    ): ResponseEntity<BookResponseDto> =
        ResponseEntity.ok(bookMapper.toResponseDto(bookService.createBook(bookMapper.toEntity(bookRequestDto))))

    @PutMapping("/{id}")
    @Operation(
        summary = "Update a book",
        description = "Updates an existing book's information"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully updated book",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = BookResponseDto::class))]
            ),
            ApiResponse(responseCode = "400", description = "Bad request - Invalid input data"),
            ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            ApiResponse(responseCode = "404", description = "Book not found")
        ]
    )
    fun updateBook(
        @Parameter(description = "Unique identifier of the book to update", example = "1")
        @PathVariable id: Long,
        @Valid @RequestBody bookRequestDto: BookRequestDto
    ): ResponseEntity<BookResponseDto> =
        ResponseEntity.ok(bookMapper.toResponseDto(bookService.updateBook(id, bookMapper.toEntityWithId(bookRequestDto, id))))

    @PatchMapping("/{id}/inventory")
    @Operation(
        summary = "Update book inventory",
        description = "Updates the quantity of a specific book in inventory"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully updated inventory",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = BookResponseDto::class))]
            ),
            ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            ApiResponse(responseCode = "404", description = "Book not found")
        ]
    )
    fun updateInventory(
        @Parameter(description = "Unique identifier of the book", example = "1")
        @PathVariable id: Long,
        @Parameter(description = "New quantity for the book", example = "25")
        @RequestParam quantity: Int
    ): ResponseEntity<BookResponseDto> = ResponseEntity.ok(bookMapper.toResponseDto(bookService.updateInventory(id, quantity)))

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete a book",
        description = "Removes a book from the inventory"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Successfully deleted book"),
            ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            ApiResponse(responseCode = "404", description = "Book not found")
        ]
    )
    fun deleteBook(
        @Parameter(description = "Unique identifier of the book to delete", example = "1")
        @PathVariable id: Long
    ): ResponseEntity<Unit> {
        bookService.deleteBook(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/advanced-search")
    @Operation(
        summary = "Advanced book search",
        description = "Search books with multiple criteria including pagination and sorting"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved search results",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = BookResponseDto::class))]
            ),
            ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required")
        ]
    )
    fun searchBooksAdvanced(
        @Parameter(description = "Book title to search for", example = "Gatsby")
        @RequestParam(required = false) title: String?,
        @Parameter(description = "Author name to search for", example = "Fitzgerald")
        @RequestParam(required = false) author: String?,
        @Parameter(description = "Genre to filter by", example = "Fiction")
        @RequestParam(required = false) genre: String?,
        @Parameter(description = "ISBN to search for", example = "978-0743273565")
        @RequestParam(required = false) isbn: String?,
        @Parameter(description = "Language to filter by", example = "English")
        @RequestParam(required = false) language: String?,
        @Parameter(description = "Publisher to filter by", example = "Scribner")
        @RequestParam(required = false) publisher: String?,
        @Parameter(description = "Published date to filter by", example = "1925-04-10")
        @RequestParam(required = false) publishedDate: String?,
        @PageableDefault(size = 20) @SortDefault.SortDefaults(
            SortDefault(sort = ["title"], direction = Sort.Direction.ASC)
        ) pageable: Pageable
    ): Page<BookResponseDto> =
        bookService.searchBooksAdvanced(title, author, genre, isbn, language, publisher, publishedDate, pageable)
            .map { bookMapper.toResponseDto(it) }
}
