package com.smartdocument.ordermanagement.dto

import java.math.BigDecimal

/**
 * DTO for returning order item details in API responses.
 */
data class OrderItemResponseDto(
    val id: Long?,
    val bookId: Long,
    val quantity: Int,
    val price: BigDecimal,
    val subtotal: BigDecimal
)
