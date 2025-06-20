package com.smartdocument.ordermanagement.exception

import java.time.LocalDateTime

/**
 * Standard error response structure for API errors in OrderManagementService.
 */
data class ErrorResponse(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: Int,
    val message: String,
    val details: List<String>? = null,
    val error: String? = null,
    val path: String? = null
)
