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
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@RestController
@RequestMapping("/api/v1/carts")
@Tag(
    name = "Cart Management",
    description = "APIs for managing shopping cart operations including adding items, updating quantities, removing items, and checkout process."
)
class CartController(
    private val cartService: CartService,
    private val cartMapper: CartMapper,
    private val orderMapper: OrderMapper
) {

    private val logger: Logger = LoggerFactory.getLogger(CartController::class.java)

    @GetMapping("/{customerId}")
    @Operation(
        summary = "Get customer cart",
        description = "Retrieves the current cart for a specific customer. If no cart exists, a new empty cart is created.",
        operationId = "getCart"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Cart retrieved successfully",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CartResponseDto::class),
                        examples = [
                            ExampleObject(
                                name = "Cart with items",
                                value = """
                                {
                                    "customerId": "customer123",
                                    "totalAmount": 59.97,
                                    "items": [
                                        {
                                            "bookId": 1,
                                            "quantity": 2,
                                            "price": 19.99,
                                            "subtotal": 39.98
                                        },
                                        {
                                            "bookId": 2,
                                            "quantity": 1,
                                            "price": 19.99,
                                            "subtotal": 19.99
                                        }
                                    ]
                                }
                                """
                            ),
                            ExampleObject(
                                name = "Empty cart",
                                value = """
                                {
                                    "customerId": "customer123",
                                    "totalAmount": 0.00,
                                    "items": []
                                }
                                """
                            )
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Authentication required"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun getCart(
        @Parameter(
            description = "Unique identifier for the customer",
            example = "customer123",
            required = true
        )
        @PathVariable customerId: String
    ): ResponseEntity<CartResponseDto> {
        logger.info("GET /api/v1/carts/{} - Retrieving cart for customer", customerId)

        val cart = cartService.getCartByCustomerId(customerId)
        val response = cartMapper.toCartResponseDto(cart)

        logger.info("GET /api/v1/carts/{} - Successfully retrieved cart with {} items, total: {}",
                   customerId, response.items.size, response.totalAmount)

        return ResponseEntity.ok(response)
    }

    @PostMapping("/{customerId}/items")
    @Operation(
        summary = "Add item to cart",
        description = "Adds a new item to the customer's cart or updates the quantity if the item already exists. Validates inventory availability and retrieves current book pricing.",
        operationId = "addItemToCart"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Item added to cart successfully",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CartResponseDto::class),
                        examples = [
                            ExampleObject(
                                name = "Item added successfully",
                                value = """
                                {
                                    "customerId": "customer123",
                                    "totalAmount": 39.98,
                                    "items": [
                                        {
                                            "bookId": 1,
                                            "quantity": 2,
                                            "price": 19.99,
                                            "subtotal": 39.98
                                        }
                                    ]
                                }
                                """
                            )
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad request - Invalid input data",
                content = [
                    Content(
                        mediaType = "application/json",
                        examples = [
                            ExampleObject(
                                name = "Validation error",
                                value = """
                                {
                                    "status": 400,
                                    "error": "Bad Request",
                                    "message": "Validation failed",
                                    "details": ["quantity: Quantity must be at least 1"]
                                }
                                """
                            )
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Authentication required"
            ),
            ApiResponse(
                responseCode = "422",
                description = "Unprocessable Entity - Insufficient stock or invalid book",
                content = [
                    Content(
                        mediaType = "application/json",
                        examples = [
                            ExampleObject(
                                name = "Insufficient stock",
                                value = """
                                {
                                    "status": 422,
                                    "error": "Unprocessable Entity",
                                    "message": "Insufficient stock available"
                                }
                                """
                            )
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun addItemToCart(
        @Parameter(
            description = "Unique identifier for the customer",
            example = "customer123",
            required = true
        )
        @PathVariable customerId: String,
        @Parameter(
            description = "Cart item details to add",
            required = true
        )
        @Valid @RequestBody request: CartItemRequestDto
    ): ResponseEntity<CartResponseDto> {
        logger.info("POST /api/v1/carts/{}/items - Adding item to cart - bookId: {}, quantity: {}",
                   customerId, request.bookId, request.quantity)

        val cart = cartService.addItemToCart(customerId, request)
        val response = cartMapper.toCartResponseDto(cart)

        logger.info("POST /api/v1/carts/{}/items - Successfully added item to cart - bookId: {}, final total: {}",
                   customerId, request.bookId, response.totalAmount)

        return ResponseEntity.ok(response)
    }

    @PatchMapping("/{customerId}/items/{bookId}")
    @Operation(
        summary = "Update cart item quantity",
        description = "Updates the quantity of a specific item in the customer's cart. Validates inventory availability and updates pricing to current book price.",
        operationId = "updateCartItemQuantity"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Cart item quantity updated successfully",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CartResponseDto::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad request - Invalid quantity or book not found in cart"
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Authentication required"
            ),
            ApiResponse(
                responseCode = "422",
                description = "Unprocessable Entity - Insufficient stock"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun updateCartItemQuantity(
        @Parameter(
            description = "Unique identifier for the customer",
            example = "customer123",
            required = true
        )
        @PathVariable customerId: String,
        @Parameter(
            description = "Unique identifier for the book",
            example = "1",
            required = true
        )
        @PathVariable bookId: Long,
        @Parameter(
            description = "New quantity for the cart item (must be at least 1)",
            example = "3",
            required = true
        )
        @RequestParam quantity: Int
    ): ResponseEntity<CartResponseDto> {
        logger.info("PATCH /api/v1/carts/{}/items/{} - Updating cart item quantity - new quantity: {}",
                   customerId, bookId, quantity)

        val cart = cartService.updateCartItemQuantity(customerId, bookId, quantity)
        val response = cartMapper.toCartResponseDto(cart)

        logger.info("PATCH /api/v1/carts/{}/items/{} - Successfully updated cart item quantity - new total: {}",
                   customerId, bookId, response.totalAmount)

        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{customerId}/items/{bookId}")
    @Operation(
        summary = "Remove item from cart",
        description = "Removes a specific item from the customer's cart and recalculates the total amount.",
        operationId = "removeItemFromCart"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Item removed from cart successfully",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CartResponseDto::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Authentication required"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun removeItemFromCart(
        @Parameter(
            description = "Unique identifier for the customer",
            example = "customer123",
            required = true
        )
        @PathVariable customerId: String,
        @Parameter(
            description = "Unique identifier for the book to remove",
            example = "1",
            required = true
        )
        @PathVariable bookId: Long
    ): ResponseEntity<CartResponseDto> {
        logger.info("DELETE /api/v1/carts/{}/items/{} - Removing item from cart", customerId, bookId)

        val cart = cartService.removeItemFromCart(customerId, bookId)
        val response = cartMapper.toCartResponseDto(cart)

        logger.info("DELETE /api/v1/carts/{}/items/{} - Successfully removed item from cart - new total: {}",
                   customerId, bookId, response.totalAmount)

        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{customerId}")
    @Operation(
        summary = "Clear cart",
        description = "Removes all items from the customer's cart and resets the total amount to zero.",
        operationId = "clearCart"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Cart cleared successfully",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CartResponseDto::class),
                        examples = [
                            ExampleObject(
                                name = "Cleared cart",
                                value = """
                                {
                                    "customerId": "customer123",
                                    "totalAmount": 0.00,
                                    "items": []
                                }
                                """
                            )
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Authentication required"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun clearCart(
        @Parameter(
            description = "Unique identifier for the customer",
            example = "customer123",
            required = true
        )
        @PathVariable customerId: String
    ): ResponseEntity<CartResponseDto> {
        logger.info("DELETE /api/v1/carts/{} - Clearing cart", customerId)

        val cart = cartService.clearCart(customerId)
        val response = cartMapper.toCartResponseDto(cart)

        logger.info("DELETE /api/v1/carts/{} - Successfully cleared cart", customerId)

        return ResponseEntity.ok(response)
    }

    @PostMapping("/{customerId}/checkout")
    @Operation(
        summary = "Checkout cart",
        description = """
            Processes the checkout of a customer's cart. This operation:
            1. Validates inventory availability for all items
            2. Creates an order from the cart contents
            3. Updates book quantities in the inventory
            4. Processes payment (simulated)
            5. Updates order status based on payment result
            6. Clears the cart after successful processing

            **Important**: This is a transactional operation. If any step fails, the entire process is rolled back.
        """,
        operationId = "checkoutCart"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Checkout completed successfully",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = OrderResponseDto::class),
                        examples = [
                            ExampleObject(
                                name = "Successful checkout",
                                value = """
                                {
                                    "id": 1,
                                    "customerId": "customer123",
                                    "status": "CONFIRMED",
                                    "totalAmount": 59.97,
                                    "orderItems": [
                                        {
                                            "id": 1,
                                            "bookId": 1,
                                            "quantity": 2,
                                            "price": 19.99,
                                            "subtotal": 39.98
                                        },
                                        {
                                            "id": 2,
                                            "bookId": 2,
                                            "quantity": 1,
                                            "price": 19.99,
                                            "subtotal": 19.99
                                        }
                                    ],
                                    "createdAt": "2024-01-15T10:30:00",
                                    "updatedAt": "2024-01-15T10:30:05"
                                }
                                """
                            )
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad request - Empty cart or validation error"
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Authentication required"
            ),
            ApiResponse(
                responseCode = "422",
                description = "Unprocessable Entity - Insufficient stock or payment failure",
                content = [
                    Content(
                        mediaType = "application/json",
                        examples = [
                            ExampleObject(
                                name = "Payment failure",
                                value = """
                                {
                                    "status": 422,
                                    "error": "Unprocessable Entity",
                                    "message": "Payment processing failed"
                                }
                                """
                            )
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun checkoutCart(
        @Parameter(
            description = "Unique identifier for the customer",
            example = "customer123",
            required = true
        )
        @PathVariable customerId: String
    ): ResponseEntity<OrderResponseDto> {
        logger.info("POST /api/v1/carts/{}/checkout - Starting checkout process", customerId)

        val order = cartService.checkoutCart(customerId)
        val response = orderMapper.toOrderResponseDto(order)

        logger.info("POST /api/v1/carts/{}/checkout - Successfully completed checkout - orderId: {}, total: {}",
                   customerId, response.id, response.totalAmount)

        return ResponseEntity.ok(response)
    }
}
