package com.smartdocument.ordermanagement.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

/**
 * DTO for adding or updating a cart item.
 */
data class CartItemRequestDto(
    @field:NotNull(message = "Book ID must not be null")
    val bookId: Long,

    @field:NotNull(message = "Quantity must not be null")
    @field:Min(1, message = "Quantity must be at least 1")
    val quantity: Int,

    @field:NotNull(message = "Price must not be null")
    @field:Positive(message = "Price must be positive")
    val price: BigDecimal
)
