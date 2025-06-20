package com.smartdocument.ordermanagement.dto

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * DTO for returning order details in API responses.
 */
data class OrderResponseDto(
    val id: Long?,
    val customerId: String,
    val status: String,
    val totalAmount: BigDecimal,
    val orderItems: List<OrderItemResponseDto>,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
)
