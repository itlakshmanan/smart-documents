package com.smartdocument.bookinventory.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "books")
data class Book(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false)
    var title: String,

    @Column(nullable = false)
    var author: String,

    @Column(nullable = false)
    var isbn: String,

    @Column(nullable = false)
    var genre: String,

    @Column(nullable = false)
    var price: BigDecimal,

    @Column(nullable = false)
    var quantity: Int,

    @Column(length = 1000)
    var description: String? = null,

    @Column(nullable = false)
    var language: String,

    @Column(nullable = false)
    var publisher: String,

    @Column(nullable = false)
    var publishedDate: java.time.LocalDate,

    @Column(nullable = false, updatable = false, name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false, name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PrePersist
    fun onCreate() {
        createdAt = LocalDateTime.now()
        updatedAt = createdAt
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
