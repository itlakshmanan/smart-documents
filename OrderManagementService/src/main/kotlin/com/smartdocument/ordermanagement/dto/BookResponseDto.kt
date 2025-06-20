package com.smartdocument.ordermanagement.dto

import java.math.BigDecimal

/**
 * DTO for book API responses (for BookClient validation).
 */
data class BookResponseDto(
    val id: Long,
    val title: String,
    val author: String,
    val isbn: String,
    val genre: String,
    val price: BigDecimal,
    val quantity: Int,
    val description: String? = null,
    val language: String,
    val publisher: String,
    val publishedDate: String
)
