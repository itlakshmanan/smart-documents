package com.smartdocument.bookinventory.model

import jakarta.persistence.*
import java.math.BigDecimal

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
    var description: String? = null
)
