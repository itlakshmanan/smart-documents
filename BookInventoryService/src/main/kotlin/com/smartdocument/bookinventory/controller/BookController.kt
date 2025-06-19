package com.smartdocument.bookinventory.controller

import com.smartdocument.bookinventory.model.Book
import com.smartdocument.bookinventory.service.BookService
import com.smartdocument.bookinventory.dto.BookRequestDto
import com.smartdocument.bookinventory.dto.BookResponseDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import com.smartdocument.bookinventory.mapper.BookMapper

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

    @GetMapping("/isbn/{isbn}")
    fun getBookByIsbn(@PathVariable isbn: String): ResponseEntity<BookResponseDto> =
        ResponseEntity.ok(bookMapper.toResponseDto(bookService.getBookByIsbn(isbn)))

    @GetMapping("/search")
    fun searchBooks(@RequestParam query: String): ResponseEntity<List<BookResponseDto>> =
        ResponseEntity.ok(bookService.searchBooks(query).map { bookMapper.toResponseDto(it) })

    @GetMapping("/genre/{genre}")
    fun getBooksByGenre(@PathVariable genre: String): ResponseEntity<List<BookResponseDto>> =
        ResponseEntity.ok(bookService.getBooksByGenre(genre).map { bookMapper.toResponseDto(it) })

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
}
