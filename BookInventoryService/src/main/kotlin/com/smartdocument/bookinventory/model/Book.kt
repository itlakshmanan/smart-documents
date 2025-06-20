package com.smartdocument.bookinventory.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Entity representing a book in the inventory.
 * Maps to the 'books' table in the database.
 */
@Entity
@Table(name = "books")
data class Book(
    /** Unique identifier for the book (auto-generated). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    /** Title of the book. */
    @Column(nullable = false)
    var title: String,

    /** Author of the book. */
    @Column(nullable = false)
    var author: String,

    /** ISBN of the book (should be unique). */
    @Column(nullable = false)
    var isbn: String,

    /** Genre of the book. */
    @Column(nullable = false)
    var genre: String,

    /** Price of the book. */
    @Column(nullable = false)
    var price: BigDecimal,

    /** Quantity in stock. */
    @Column(nullable = false)
    var quantity: Int,

    /** Optional description of the book. */
    @Column(length = 1000)
    var description: String? = null,

    /** Language of the book. */
    @Column(nullable = false)
    var language: String,

    /** Publisher of the book. */
    @Column(nullable = false)
    var publisher: String,

    /** Published date of the book. */
    @Column(nullable = false)
    var publishedDate: java.time.LocalDate,

    /** Timestamp when the book was created. */
    @Column(nullable = false, updatable = false, name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(),

    /** Timestamp when the book was last updated. */
    @Column(nullable = false, name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Sets the creation and update timestamps before persisting.
     */
    @PrePersist
    fun onCreate() {
        createdAt = LocalDateTime.now()
        updatedAt = createdAt
    }

    /**
     * Updates the update timestamp before updating the entity.
     */
    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
