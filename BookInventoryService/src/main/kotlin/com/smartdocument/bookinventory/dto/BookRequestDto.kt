package com.smartdocument.bookinventory.dto

import jakarta.validation.constraints.*
import java.math.BigDecimal

/**
 * DTO for book create/update requests.
 */
data class BookRequestDto(
    @field:NotBlank(message = "Title must not be blank")
    @field:Size(max = 255, message = "Title must be at most 255 characters")
    val title: String,

    @field:NotBlank(message = "Author must not be blank")
    @field:Size(max = 255, message = "Author must be at most 255 characters")
    val author: String,

    /**
     * ISBN validation allows ISBN-10 and ISBN-13 formats with optional hyphens (e.g., 978-0547928241).
     */
    @field:NotBlank(message = "ISBN must not be blank")
    @field:Pattern(
        regexp = "^(97(8|9))?(-?\\d){9,12}(-?[\\dX])$",
        message = "ISBN must be valid (ISBN-10 or ISBN-13, hyphens allowed)"
    )
    val isbn: String,

    @field:NotBlank(message = "Genre must not be blank")
    @field:Size(max = 100, message = "Genre must be at most 100 characters")
    val genre: String,

    @field:NotNull(message = "Price must not be null")
    @field:DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
    val price: BigDecimal,

    @field:NotNull(message = "Quantity must not be null")
    @field:Min(value = 0, message = "Quantity must be zero or positive")
    val quantity: Int,

    @field:Size(max = 1000, message = "Description must be at most 1000 characters")
    val description: String? = null
)
