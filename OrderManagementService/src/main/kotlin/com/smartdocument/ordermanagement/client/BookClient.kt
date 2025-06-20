package com.smartdocument.ordermanagement.client

import com.smartdocument.ordermanagement.dto.BookResponseDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

@Component
class BookClient(
    @Value("\${book.inventory.service.url}") private val bookServiceUrl: String,
    @Value("\${book.inventory.service.username}") private val username: String,
    @Value("\${book.inventory.service.password}") private val password: String
) {
    private val webClient: WebClient = WebClient.builder()
        .baseUrl(bookServiceUrl)
        .defaultHeaders { headers ->
            headers.setBasicAuth(username, password)
        }
        .build()

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
