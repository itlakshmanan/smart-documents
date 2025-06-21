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

/**
 * REST controller for managing customer shopping carts.
 *
 * This controller provides endpoints for all cart-related operations including:
 * - Retrieving cart contents
 * - Adding items to cart
 * - Updating item quantities
 * - Removing items from cart
 * - Clearing entire cart
 * - Checking out cart to create an order
 *
 * All operations are performed in the context of a specific customer identified
 * by their customer ID. The cart is automatically created if it doesn't exist
 * when the first item is added.
 *
 * Business rules enforced:
 * - Cart items must have valid book IDs that exist in the inventory
 * - Quantities must be positive integers
 * - Cart totals are calculated automatically
 * - Checkout validates inventory availability before creating orders
 *
 * Authentication is required for all endpoints using HTTP Basic Authentication.
 *
 * @property cartService Service layer for cart business logic
 * @property cartMapper Mapper for converting between cart entities and DTOs
 * @property orderMapper Mapper for converting between order entities and DTOs
 */
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

    /**
     * Retrieves the current cart for a specific customer.
     *
     * Returns the complete cart contents including all items, quantities,
     * prices, and calculated totals. If no cart exists for the customer,
     * an empty cart will be returned.
     *
     * @param customerId Unique identifier for the customer
     * @return CartResponseDto containing cart details and items
     * @throws com.smartdocument.ordermanagement.exception.OrderManagementServiceException if customer ID is invalid
     */
    @GetMapping("/{customerId}")
    fun getCart(@PathVariable customerId: String): ResponseEntity<CartResponseDto> {
        logger.info("Getting cart for customer: {}", customerId)
        val cart = cartService.getCartByCustomerId(customerId)
        val cartResponse = cartMapper.toCartResponseDto(cart)
        return ResponseEntity.ok(cartResponse)
    }

    /**
     * Adds a new item to the customer's cart.
     *
     * Validates the book exists in inventory and adds the specified quantity
     * to the cart. If the book is already in the cart, the quantities are
     * combined. The cart is automatically created if it doesn't exist.
     *
     * @param customerId Unique identifier for the customer
     * @param cartItemRequest Request containing book ID and quantity to add
     * @return CartResponseDto with updated cart contents
     * @throws com.smartdocument.ordermanagement.exception.OrderManagementServiceException if book doesn't exist or quantity is invalid
     */
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

    /**
     * Updates the quantity of a specific item in the customer's cart.
     *
     * Changes the quantity of the specified book in the cart. If the new
     * quantity is 0, the item is removed from the cart. If the quantity
     * is negative, an error is returned.
     *
     * @param customerId Unique identifier for the customer
     * @param bookId Unique identifier of the book to update
     * @param request Request containing the new quantity
     * @return CartResponseDto with updated cart contents
     * @throws com.smartdocument.ordermanagement.exception.OrderManagementServiceException if book is not in cart or quantity is invalid
     */
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

    /**
     * Removes a specific item from the customer's cart.
     *
     * Completely removes the specified book from the cart regardless of
     * its current quantity. The cart total is recalculated after removal.
     *
     * @param customerId Unique identifier for the customer
     * @param bookId Unique identifier of the book to remove
     * @return CartResponseDto with updated cart contents
     * @throws com.smartdocument.ordermanagement.exception.OrderManagementServiceException if book is not in cart
     */
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

    /**
     * Clears all items from the customer's cart.
     *
     * Removes all items from the cart, effectively resetting it to an
     * empty state. The cart entity itself is preserved but contains no items.
     *
     * @param customerId Unique identifier for the customer
     * @return CartResponseDto with empty cart contents
     * @throws com.smartdocument.ordermanagement.exception.OrderManagementServiceException if customer ID is invalid
     */
    @DeleteMapping("/{customerId}")
    fun clearCart(@PathVariable customerId: String): ResponseEntity<CartResponseDto> {
        logger.info("Clearing cart for customer: {}", customerId)
        val cart = cartService.clearCart(customerId)
        val cartResponse = cartMapper.toCartResponseDto(cart)
        return ResponseEntity.ok(cartResponse)
    }

    /**
     * Processes the checkout of the customer's cart to create an order.
     *
     * Validates that all items in the cart are available in sufficient
     * quantities in the inventory. If validation passes, creates a new
     * order with all cart items and clears the cart. If validation fails,
     * returns an error with details about insufficient inventory.
     *
     * The checkout process includes:
     * - Inventory availability validation
     * - Order creation with all cart items
     * - Payment simulation
     * - Cart clearing after successful order creation
     *
     * @param customerId Unique identifier for the customer
     * @return OrderResponseDto containing the created order details
     * @throws com.smartdocument.ordermanagement.exception.OrderManagementServiceException if cart is empty or inventory is insufficient
     */
    @PostMapping("/{customerId}/checkout")
    fun checkoutCart(@PathVariable customerId: String): ResponseEntity<OrderResponseDto> {
        logger.info("Checking out cart for customer: {}", customerId)
        val order = cartService.checkoutCart(customerId)
        val orderResponse = orderMapper.toOrderResponseDto(order)
        return ResponseEntity.ok(orderResponse)
    }
}
