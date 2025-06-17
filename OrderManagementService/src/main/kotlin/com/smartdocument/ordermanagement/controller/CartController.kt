package com.smartdocument.ordermanagement.controller

import com.smartdocument.ordermanagement.model.Cart
import com.smartdocument.ordermanagement.service.CartService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/api/carts")
class CartController(private val cartService: CartService) {

    @GetMapping("/{customerId}")
    fun getCart(@PathVariable customerId: String): ResponseEntity<Cart> =
        ResponseEntity.ok(cartService.getCartByCustomerId(customerId))

    @PostMapping("/{customerId}/items")
    fun addItemToCart(
        @PathVariable customerId: String,
        @RequestParam bookId: Long,
        @RequestParam quantity: Int,
        @RequestParam price: BigDecimal
    ): ResponseEntity<Cart> = ResponseEntity.ok(
        cartService.addItemToCart(customerId, bookId, quantity, price)
    )

    @PatchMapping("/{customerId}/items/{bookId}")
    fun updateCartItemQuantity(
        @PathVariable customerId: String,
        @PathVariable bookId: Long,
        @RequestParam quantity: Int
    ): ResponseEntity<Cart> = ResponseEntity.ok(
        cartService.updateCartItemQuantity(customerId, bookId, quantity)
    )

    @DeleteMapping("/{customerId}/items/{bookId}")
    fun removeItemFromCart(
        @PathVariable customerId: String,
        @PathVariable bookId: Long
    ): ResponseEntity<Cart> = ResponseEntity.ok(
        cartService.removeItemFromCart(customerId, bookId)
    )

    @DeleteMapping("/{customerId}")
    fun clearCart(@PathVariable customerId: String): ResponseEntity<Cart> =
        ResponseEntity.ok(cartService.clearCart(customerId))
} 