package com.smartdocument.bookinventory.service

import com.smartdocument.bookinventory.exception.BookInventoryServiceException
import com.smartdocument.bookinventory.model.Book
import com.smartdocument.bookinventory.repository.BookRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class BookService(private val bookRepository: BookRepository) {

    fun getAllBooks(): List<Book> = bookRepository.findAll()

    fun getBookById(id: Long): Book = bookRepository.findById(id)
        .orElseThrow { NoSuchElementException("Book not found with id: $id") }

    fun getBookByIsbn(isbn: String): Book = bookRepository.findByIsbn(isbn)
        ?: throw NoSuchElementException("Book not found with ISBN: $isbn")

    fun searchBooks(query: String): List<Book> = bookRepository.searchBooks(query)

    fun getBooksByGenre(genre: String): List<Book> = bookRepository.findByGenre(genre)

    fun getAllGenres(): List<String> = bookRepository.findAllGenres()

    @Transactional
    fun createBook(book: Book): Book {
        if (bookRepository.findByIsbn(book.isbn) != null) {
            throw BookInventoryServiceException(BookInventoryServiceException.Operation.ISBN_ALREADY_EXISTS)
        }
        return bookRepository.save(book)
    }

    @Transactional
    fun updateBook(id: Long, updatedBook: Book): Book {
        val existingBook = getBookById(id)
        existingBook.apply {
            title = updatedBook.title
            author = updatedBook.author
            isbn = updatedBook.isbn
            genre = updatedBook.genre
            price = updatedBook.price
            quantity = updatedBook.quantity
            description = updatedBook.description
        }
        return bookRepository.save(existingBook)
    }

    @Transactional
    fun updateInventory(id: Long, quantity: Int): Book {
        val book = getBookById(id)
        book.quantity = quantity
        return bookRepository.save(book)
    }

    @Transactional
    fun deleteBook(id: Long) {
        if (!bookRepository.existsById(id)) {
            throw NoSuchElementException("Book not found with id: $id")
        }
        bookRepository.deleteById(id)
    }
}
