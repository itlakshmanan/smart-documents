package com.smartdocument.ordermanagement.controller

import com.smartdocument.ordermanagement.model.Cart
import com.smartdocument.ordermanagement.service.CartService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import com.smartdocument.ordermanagement.dto.CartItemRequestDto
import com.smartdocument.ordermanagement.dto.CartResponseDto
import jakarta.validation.Valid
import com.smartdocument.ordermanagement.mapper.CartMapper
import com.smartdocument.ordermanagement.dto.OrderResponseDto
import com.smartdocument.ordermanagement.mapper.OrderMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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
    @Operation(summary = "Get customer cart")
    fun getCart(
        @Parameter(description = "Unique identifier for the customer")
        @PathVariable customerId: String
    ): ResponseEntity<CartResponseDto> {
        logger.info("Getting cart for customer: {}", customerId)
        val cart = cartService.getCartByCustomerId(customerId)
        val cartResponse = cartMapper.toCartResponseDto(cart)
        return ResponseEntity.ok(cartResponse)
    }

    @PostMapping("/{customerId}/items")
    @Operation(summary = "Add item to cart")
    fun addItemToCart(
        @Parameter(description = "Unique identifier for the customer")
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
    @Operation(summary = "Update item quantity")
    fun updateItemQuantity(
        @Parameter(description = "Unique identifier for the customer")
        @PathVariable customerId: String,
        @Parameter(description = "Unique identifier for the book")
        @PathVariable bookId: Long,
        @RequestBody request: Map<String, Int>
    ): ResponseEntity<CartResponseDto> {
        val quantity = request["quantity"] ?: throw IllegalArgumentException("Quantity is required")
        logger.info("Updating item quantity for customer: {}, bookId: {}, new quantity: {}",
                   customerId, bookId, quantity)
        val cart = cartService.updateCartItemQuantity(customerId, bookId, quantity)
        val cartResponse = cartMapper.toCartResponseDto(cart)
        return ResponseEntity.ok(cartResponse)
    }

    @DeleteMapping("/{customerId}/items/{bookId}")
    @Operation(summary = "Remove item from cart")
    fun removeItemFromCart(
        @Parameter(description = "Unique identifier for the customer")
        @PathVariable customerId: String,
        @Parameter(description = "Unique identifier for the book")
        @PathVariable bookId: Long
    ): ResponseEntity<CartResponseDto> {
        logger.info("Removing item from cart for customer: {}, bookId: {}", customerId, bookId)
        val cart = cartService.removeItemFromCart(customerId, bookId)
        val cartResponse = cartMapper.toCartResponseDto(cart)
        return ResponseEntity.ok(cartResponse)
    }

    @DeleteMapping("/{customerId}")
    @Operation(summary = "Clear cart")
    fun clearCart(
        @Parameter(description = "Unique identifier for the customer")
        @PathVariable customerId: String
    ): ResponseEntity<CartResponseDto> {
        logger.info("Clearing cart for customer: {}", customerId)
        val cart = cartService.clearCart(customerId)
        val cartResponse = cartMapper.toCartResponseDto(cart)
        return ResponseEntity.ok(cartResponse)
    }

    @PostMapping("/{customerId}/checkout")
    @Operation(summary = "Checkout cart")
    fun checkoutCart(
        @Parameter(description = "Unique identifier for the customer")
        @PathVariable customerId: String
    ): ResponseEntity<OrderResponseDto> {
        logger.info("Checking out cart for customer: {}", customerId)
        val order = cartService.checkoutCart(customerId)
        val orderResponse = orderMapper.toOrderResponseDto(order)
        return ResponseEntity.ok(orderResponse)
    }
}
