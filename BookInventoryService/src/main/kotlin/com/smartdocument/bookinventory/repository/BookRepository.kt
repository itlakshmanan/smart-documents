package com.smartdocument.bookinventory.repository

import com.smartdocument.bookinventory.model.Book
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

@Repository
interface BookRepository : JpaRepository<Book, Long> {
    @Query("SELECT DISTINCT b.genre FROM Book b")
    fun findAllGenres(): List<String>

    fun findAll(spec: Specification<Book>?, pageable: Pageable): Page<Book>

    fun findByIsbn(isbn: String): Book?
}
