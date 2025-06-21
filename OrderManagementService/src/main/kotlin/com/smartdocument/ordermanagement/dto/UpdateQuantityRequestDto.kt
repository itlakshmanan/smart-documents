package com.smartdocument.ordermanagement.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

@Schema(description = "Request DTO for updating cart item quantity")
data class UpdateQuantityRequestDto(
    @field:NotNull(message = "Quantity is required")
    @field:Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "New quantity for the cart item", example = "5", minimum = "1")
    val quantity: Int
)
