package com.smartdocument.bookinventory.controller

import com.smartdocument.bookinventory.model.Book
import com.smartdocument.bookinventory.service.BookService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/books")
class BookController(private val bookService: BookService) {

    @GetMapping
    fun getAllBooks(): ResponseEntity<List<Book>> = 
        ResponseEntity.ok(bookService.getAllBooks())

    @GetMapping("/{id}")
    fun getBookById(@PathVariable id: Long): ResponseEntity<Book> =
        ResponseEntity.ok(bookService.getBookById(id))

    @GetMapping("/isbn/{isbn}")
    fun getBookByIsbn(@PathVariable isbn: String): ResponseEntity<Book> =
        ResponseEntity.ok(bookService.getBookByIsbn(isbn))

    @GetMapping("/search")
    fun searchBooks(@RequestParam query: String): ResponseEntity<List<Book>> =
        ResponseEntity.ok(bookService.searchBooks(query))

    @GetMapping("/genre/{genre}")
    fun getBooksByGenre(@PathVariable genre: String): ResponseEntity<List<Book>> =
        ResponseEntity.ok(bookService.getBooksByGenre(genre))

    @GetMapping("/genres")
    fun getAllGenres(): ResponseEntity<List<String>> =
        ResponseEntity.ok(bookService.getAllGenres())

    @PostMapping
    fun createBook(@RequestBody book: Book): ResponseEntity<Book> =
        ResponseEntity.ok(bookService.createBook(book))

    @PutMapping("/{id}")
    fun updateBook(@PathVariable id: Long, @RequestBody book: Book): ResponseEntity<Book> =
        ResponseEntity.ok(bookService.updateBook(id, book))

    @PatchMapping("/{id}/inventory")
    fun updateInventory(
        @PathVariable id: Long,
        @RequestParam quantity: Int
    ): ResponseEntity<Book> = ResponseEntity.ok(bookService.updateInventory(id, quantity))

    @DeleteMapping("/{id}")
    fun deleteBook(@PathVariable id: Long): ResponseEntity<Unit> {
        bookService.deleteBook(id)
        return ResponseEntity.noContent().build()
    }
} 