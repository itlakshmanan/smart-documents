package com.smartdocument.ordermanagement.controller

import com.smartdocument.ordermanagement.model.Cart
import com.smartdocument.ordermanagement.service.CartService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import com.smartdocument.ordermanagement.dto.CartItemRequestDto
import com.smartdocument.ordermanagement.dto.CartResponseDto
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/v1/carts")
class CartController(private val cartService: CartService) {

    @GetMapping("/{customerId}")
    fun getCart(@PathVariable customerId: String): ResponseEntity<CartResponseDto> =
        ResponseEntity.ok(cartService.toCartResponseDto(cartService.getCartByCustomerId(customerId)))

    @PostMapping("/{customerId}/items")
    fun addItemToCart(
        @PathVariable customerId: String,
        @Valid @RequestBody request: CartItemRequestDto
    ): ResponseEntity<CartResponseDto> = ResponseEntity.ok(
        cartService.toCartResponseDto(cartService.addItemToCart(customerId, request))
    )

    @PatchMapping("/{customerId}/items/{bookId}")
    fun updateCartItemQuantity(
        @PathVariable customerId: String,
        @PathVariable bookId: Long,
        @RequestParam quantity: Int
    ): ResponseEntity<CartResponseDto> = ResponseEntity.ok(
        cartService.toCartResponseDto(cartService.updateCartItemQuantity(customerId, bookId, quantity))
    )

    @DeleteMapping("/{customerId}/items/{bookId}")
    fun removeItemFromCart(
        @PathVariable customerId: String,
        @PathVariable bookId: Long
    ): ResponseEntity<CartResponseDto> = ResponseEntity.ok(
        cartService.toCartResponseDto(cartService.removeItemFromCart(customerId, bookId))
    )

    @DeleteMapping("/{customerId}")
    fun clearCart(@PathVariable customerId: String): ResponseEntity<CartResponseDto> =
        ResponseEntity.ok(cartService.toCartResponseDto(cartService.clearCart(customerId)))
}
