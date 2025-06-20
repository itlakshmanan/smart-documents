package com.smartdocument.ordermanagement.dto

import java.math.BigDecimal

/**
 * DTO for returning cart details in API responses.
 */
data class CartResponseDto(
    val customerId: String,
    val totalAmount: BigDecimal,
    val items: List<CartItemResponseDto>
)
