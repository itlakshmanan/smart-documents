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

@Service
class BookService(private val bookRepository: BookRepository) {

    fun getAllBooks(): List<Book> = bookRepository.findAll()

    fun getBookById(id: Long): Book = bookRepository.findById(id)
        .orElseThrow { NoSuchElementException("Book not found with id: $id") }

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
        val bookToSave = updatedBook.copy(createdAt = existingBook.createdAt)
        return bookRepository.save(bookToSave)
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
        val spec = Specification.where<Book>(null)
            .and(title?.let { titleLike(it) })
            .and(author?.let { authorLike(it) })
            .and(genre?.let { genreEquals(it) })
            .and(isbn?.let { isbnEquals(it) })
            // Add more filters as needed
        return bookRepository.findAll(spec, pageable)
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
