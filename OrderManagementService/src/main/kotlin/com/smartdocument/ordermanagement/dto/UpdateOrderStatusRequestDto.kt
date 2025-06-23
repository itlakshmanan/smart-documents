package com.smartdocument.ordermanagement.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

/**
 * DTO for updating the status of an order via API.
 */
@Schema(description = "Request DTO for updating order status")
data class UpdateOrderStatusRequestDto(
    @field:NotNull(message = "Status is required")
    @field:NotBlank(message = "Status must not be blank")
    @Schema(description = "New status for the order", example = "CANCELLED")
    val status: String?
)
