package com.smartdocument.bookinventory.exception

import java.time.LocalDateTime

/**
 * Standard error response structure for API errors in BookInventoryService.
 *
 * This data class provides a consistent error response format across all
 * API endpoints. It includes comprehensive error information to help
 * developers and users understand what went wrong and how to resolve it.
 *
 * The error response includes:
 * - Timestamp of when the error occurred
 * - HTTP status code for the error
 * - Human-readable error message
 * - Optional detailed error information
 * - Error type classification
 * - Request path that caused the error
 *
 * This standardized format ensures consistent error handling across
 * all endpoints and provides sufficient information for debugging
 * and user feedback.
 *
 * @property timestamp When the error occurred (auto-generated)
 * @property status HTTP status code of the error
 * @property message Human-readable error message
 * @property details Optional list of detailed error messages for validation failures
 * @property error Optional error type classification
 * @property path Optional request path that caused the error
 */
data class ErrorResponse(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: Int,
    val message: String,
    val details: List<String>? = null,
    val error: String? = null,
    val path: String? = null
)
