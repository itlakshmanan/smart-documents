package com.smartdocument.ordermanagement.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

/**
 * DTO for adding or updating a cart item.
 */
data class CartItemRequestDto(
    @field:NotNull(message = "Book ID must not be null")
    val bookId: Long,

    @field:NotNull(message = "Quantity must not be null")
    @field:Min(1, message = "Quantity must be at least 1")
    val quantity: Int
)
