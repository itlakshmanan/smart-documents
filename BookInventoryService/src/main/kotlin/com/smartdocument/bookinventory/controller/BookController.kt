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

@RestController
@RequestMapping("/api/books")
class BookController(
    private val bookService: BookService,
    private val bookMapper: BookMapper
) {

    @GetMapping
    fun getAllBooks(): ResponseEntity<List<BookResponseDto>> =
        ResponseEntity.ok(bookService.getAllBooks().map { bookMapper.toResponseDto(it) })

    @GetMapping("/{id}")
    fun getBookById(@PathVariable id: Long): ResponseEntity<BookResponseDto> =
        ResponseEntity.ok(bookMapper.toResponseDto(bookService.getBookById(id)))

    @GetMapping("/genres")
    fun getAllGenres(): ResponseEntity<List<String>> =
        ResponseEntity.ok(bookService.getAllGenres())

    @PostMapping
    fun createBook(@Valid @RequestBody bookRequestDto: BookRequestDto): ResponseEntity<BookResponseDto> =
        ResponseEntity.ok(bookMapper.toResponseDto(bookService.createBook(bookMapper.toEntity(bookRequestDto))))

    @PutMapping("/{id}")
    fun updateBook(@PathVariable id: Long, @Valid @RequestBody bookRequestDto: BookRequestDto): ResponseEntity<BookResponseDto> =
        ResponseEntity.ok(bookMapper.toResponseDto(bookService.updateBook(id, bookMapper.toEntityWithId(bookRequestDto, id))))

    @PatchMapping("/{id}/inventory")
    fun updateInventory(
        @PathVariable id: Long,
        @RequestParam quantity: Int
    ): ResponseEntity<BookResponseDto> = ResponseEntity.ok(bookMapper.toResponseDto(bookService.updateInventory(id, quantity)))

    @DeleteMapping("/{id}")
    fun deleteBook(@PathVariable id: Long): ResponseEntity<Unit> {
        bookService.deleteBook(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/advanced-search")
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
    ): Page<BookResponseDto> =
        bookService.searchBooksAdvanced(title, author, genre, isbn, language, publisher, publishedDate, pageable)
            .map { bookMapper.toResponseDto(it) }
}
