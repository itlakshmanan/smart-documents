package com.smartdocument.ordermanagement.dto

import java.math.BigDecimal

/**
 * DTO for returning cart item details in API responses.
 */
data class CartItemResponseDto(
    val bookId: Long,
    val quantity: Int,
    val price: BigDecimal,
    val subtotal: BigDecimal
)
