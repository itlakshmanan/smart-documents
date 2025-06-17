package com.smartdocument.bookinventory.repository

import com.smartdocument.bookinventory.model.Book
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface BookRepository : JpaRepository<Book, Long> {
    fun findByIsbn(isbn: String): Book?
    
    @Query("SELECT b FROM Book b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(b.author) LIKE LOWER(CONCAT('%', :query, '%'))")
    fun searchBooks(query: String): List<Book>
    
    fun findByGenre(genre: String): List<Book>
    
    @Query("SELECT DISTINCT b.genre FROM Book b")
    fun findAllGenres(): List<String>
} 