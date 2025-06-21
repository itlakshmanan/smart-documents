package com.smartdocument.ordermanagement.client

import com.smartdocument.ordermanagement.dto.BookResponseDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

/**
 * Client component for communicating with the BookInventoryService.
 *
 * This client provides a reactive interface to the BookInventoryService API,
 * handling HTTP communication, authentication, and response processing.
 *
 * The client supports:
 * - Retrieving book information by ID
 * - Updating book inventory quantities
 * - HTTP Basic Authentication
 * - Error handling for various HTTP status codes
 *
 * Configuration is externalized through application properties:
 * - `book.inventory.service.url`: Base URL of the BookInventoryService
 * - `book.inventory.service.username`: Username for Basic Authentication
 * - `book.inventory.service.password`: Password for Basic Authentication
 *
 * @property bookServiceUrl Base URL of the BookInventoryService
 * @property username Username for Basic Authentication
 * @property password Password for Basic Authentication
 * @property webClient Configured WebClient instance for HTTP communication
 */
@Component
class BookClient(
    @Value("\${book.inventory.service.url}") private val bookServiceUrl: String,
    @Value("\${book.inventory.service.username}") private val username: String,
    @Value("\${book.inventory.service.password}") private val password: String
) {

    /**
     * Configured WebClient instance for HTTP communication with BookInventoryService.
     *
     * The WebClient is configured with:
     * - Base URL from application properties
     * - HTTP Basic Authentication headers
     * - Reactive programming support
     */
    private val webClient: WebClient = WebClient.builder()
        .baseUrl(bookServiceUrl)
        .defaultHeaders { headers ->
            headers.setBasicAuth(username, password)
        }
        .build()

    /**
     * Retrieves book information by its unique identifier.
     *
     * This method makes a GET request to the BookInventoryService to retrieve
     * detailed information about a specific book, including:
     * - Book metadata (title, author, ISBN, etc.)
     * - Current pricing information
     * - Available inventory quantity
     * - Publication details
     *
     * The method handles various response scenarios:
     * - Returns book data if found
     * - Returns null if book is not found (404 response)
     * - Throws exception for other HTTP errors
     *
     * @param bookId The unique identifier of the book to retrieve
     * @return BookResponseDto containing book information, or null if not found
     * @throws WebClientResponseException for HTTP errors other than 404
     */
    fun getBookById(bookId: Long): BookResponseDto? =
        webClient.get()
            .uri("/api/v1/books/{id}", bookId)
            .exchangeToMono { response ->
                if (response.statusCode() == HttpStatus.NOT_FOUND) {
                    Mono.empty()
                } else {
                    response.bodyToMono(BookResponseDto::class.java)
                }
            }
            .block()

    /**
     * Updates the inventory quantity for a specific book.
     *
     * This method makes a PATCH request to the BookInventoryService to update
     * the available quantity of a book. The quantity change can be:
     * - Positive: Increase inventory (e.g., restocking)
     * - Negative: Decrease inventory (e.g., after order placement)
     * - Zero: No change
     *
     * The method is commonly used during order processing to:
     * - Reserve inventory when creating orders
     * - Restore inventory when orders are cancelled
     * - Update quantities after successful order completion
     *
     * @param bookId The unique identifier of the book to update
     * @param newQuantity The quantity change to apply (can be positive or negative)
     * @return true if the update was successful, false otherwise
     * @throws WebClientResponseException for HTTP communication errors
     */
    fun updateBookQuantity(bookId: Long, newQuantity: Int): Boolean =
        webClient.patch()
            .uri("/api/v1/books/{id}/inventory?quantity={quantity}", bookId, newQuantity)
            .exchangeToMono { response ->
                if (response.statusCode().is2xxSuccessful) {
                    Mono.just(true)
                } else {
                    Mono.just(false)
                }
            }
            .block() ?: false
}
