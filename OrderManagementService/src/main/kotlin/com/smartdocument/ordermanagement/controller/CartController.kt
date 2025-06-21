package com.smartdocument.ordermanagement.controller

import com.smartdocument.ordermanagement.model.Cart
import com.smartdocument.ordermanagement.service.CartService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import com.smartdocument.ordermanagement.dto.CartItemRequestDto
import com.smartdocument.ordermanagement.dto.CartResponseDto
import com.smartdocument.ordermanagement.dto.UpdateQuantityRequestDto
import jakarta.validation.Valid
import com.smartdocument.ordermanagement.mapper.CartMapper
import com.smartdocument.ordermanagement.dto.OrderResponseDto
import com.smartdocument.ordermanagement.mapper.OrderMapper
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@RestController
@RequestMapping("/api/v1/carts")
@Tag(name = "Cart Management", description = "APIs for managing customer shopping carts")
@SecurityRequirement(name = "basicAuth")
class CartController(
    private val cartService: CartService,
    private val cartMapper: CartMapper,
    private val orderMapper: OrderMapper
) {
    private val logger: Logger = LoggerFactory.getLogger(CartController::class.java)

    @GetMapping("/{customerId}")
    fun getCart(@PathVariable customerId: String): ResponseEntity<CartResponseDto> {
        logger.info("Getting cart for customer: {}", customerId)
        val cart = cartService.getCartByCustomerId(customerId)
        val cartResponse = cartMapper.toCartResponseDto(cart)
        return ResponseEntity.ok(cartResponse)
    }

    @PostMapping("/{customerId}/items")
    fun addItemToCart(
        @PathVariable customerId: String,
        @Valid @RequestBody cartItemRequest: CartItemRequestDto
    ): ResponseEntity<CartResponseDto> {
        logger.info("Adding item to cart for customer: {}, bookId: {}, quantity: {}",
                   customerId, cartItemRequest.bookId, cartItemRequest.quantity)
        val cart = cartService.addItemToCart(customerId, cartItemRequest)
        val cartResponse = cartMapper.toCartResponseDto(cart)
        return ResponseEntity.ok(cartResponse)
    }

    @PatchMapping("/{customerId}/items/{bookId}")
    fun updateItemQuantity(
        @PathVariable customerId: String,
        @PathVariable bookId: Long,
        @Valid @RequestBody request: UpdateQuantityRequestDto
    ): ResponseEntity<CartResponseDto> {
        logger.info("Updating item quantity for customer: {}, bookId: {}, new quantity: {}",
                   customerId, bookId, request.quantity)
        val cart = cartService.updateCartItemQuantity(customerId, bookId, request.quantity)
        val cartResponse = cartMapper.toCartResponseDto(cart)
        return ResponseEntity.ok(cartResponse)
    }

    @DeleteMapping("/{customerId}/items/{bookId}")
    fun removeItemFromCart(
        @PathVariable customerId: String,
        @PathVariable bookId: Long
    ): ResponseEntity<CartResponseDto> {
        logger.info("Removing item from cart for customer: {}, bookId: {}", customerId, bookId)
        val cart = cartService.removeItemFromCart(customerId, bookId)
        val cartResponse = cartMapper.toCartResponseDto(cart)
        return ResponseEntity.ok(cartResponse)
    }

    @DeleteMapping("/{customerId}")
    fun clearCart(@PathVariable customerId: String): ResponseEntity<CartResponseDto> {
        logger.info("Clearing cart for customer: {}", customerId)
        val cart = cartService.clearCart(customerId)
        val cartResponse = cartMapper.toCartResponseDto(cart)
        return ResponseEntity.ok(cartResponse)
    }

    @PostMapping("/{customerId}/checkout")
    fun checkoutCart(@PathVariable customerId: String): ResponseEntity<OrderResponseDto> {
        logger.info("Checking out cart for customer: {}", customerId)
        val order = cartService.checkoutCart(customerId)
        val orderResponse = orderMapper.toOrderResponseDto(order)
        return ResponseEntity.ok(orderResponse)
    }
}
