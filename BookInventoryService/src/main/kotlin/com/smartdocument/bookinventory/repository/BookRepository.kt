package com.smartdocument.bookinventory.repository

import com.smartdocument.bookinventory.model.Book
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * Repository interface for Book entities.
 * Extends JpaRepository to provide CRUD operations and custom queries for books.
 */
@Repository
interface BookRepository : JpaRepository<Book, Long> {
    /**
     * Retrieves all unique genres from the books table.
     * @return a list of unique genre names
     */
    @Query("SELECT DISTINCT b.genre FROM Book b")
    fun findAllGenres(): List<String>

    /**
     * Finds all books matching the given specification and pageable.
     * @param spec the specification for filtering books
     * @param pageable pagination and sorting information
     * @return a page of books matching the specification
     */
    fun findAll(spec: Specification<Book>?, pageable: Pageable): Page<Book>

    /**
     * Finds a book by its ISBN.
     * @param isbn the ISBN to search for
     * @return the Book entity if found, or null if not found
     */
    fun findByIsbn(isbn: String): Book?
}
