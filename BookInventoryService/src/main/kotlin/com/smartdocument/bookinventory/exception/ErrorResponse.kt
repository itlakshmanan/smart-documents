package com.smartdocument.bookinventory.exception

import java.time.LocalDateTime

data class ErrorResponse(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: Int,
    val message: String,
    val details: List<String>? = null,
    val error: String? = null,
    val path: String? = null
)
