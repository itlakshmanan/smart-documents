package com.smartdocument.ordermanagement.service

import com.smartdocument.ordermanagement.model.*
import com.smartdocument.ordermanagement.repository.CartRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import com.smartdocument.ordermanagement.dto.CartItemRequestDto
import com.smartdocument.ordermanagement.dto.CartItemResponseDto
import com.smartdocument.ordermanagement.dto.CartResponseDto
import com.smartdocument.ordermanagement.exception.OrderManagementServiceException
import com.smartdocument.ordermanagement.client.BookClient
import com.smartdocument.ordermanagement.service.OrderService
import com.smartdocument.ordermanagement.service.PaymentService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Service class responsible for managing shopping cart operations in the Order Management Service.
 *
 * This service handles all cart-related business logic including:
 * - Adding items to cart with inventory validation
 * - Updating cart item quantities
 * - Removing items from cart
 * - Clearing entire carts
 * - Processing checkout with order creation and payment
 *
 * The service integrates with:
 * - [BookClient] for real-time inventory validation and pricing
 * - [OrderService] for order creation and management
 * - [PaymentService] for payment processing
 * - [CartRepository] for data persistence
 *
 * All operations are transactional to ensure data consistency.
 *
 * @property cartRepository Repository for cart data persistence
 * @property bookClient Client for communicating with BookInventoryService
 * @property orderService Service for order management operations
 * @property paymentService Service for payment processing
 */
@Service
class CartService(
    private val cartRepository: CartRepository,
    private val bookClient: BookClient,
    private val orderService: OrderService,
    private val paymentService: PaymentService
) {

    private val logger: Logger = LoggerFactory.getLogger(CartService::class.java)

    /**
     * Retrieves or creates a cart for a specific customer.
     *
     * If a cart doesn't exist for the customer, a new empty cart is created.
     * If a cart already exists, it is returned with updated total amount.
     *
     * @param customerId The unique identifier of the customer
     * @return The customer's cart (existing or newly created)
     */
    fun getCartByCustomerId(customerId: String): Cart {
        logger.debug("Getting cart for customer: {}", customerId)
        val cart = cartRepository.findByCustomerId(customerId)
        return if (cart != null) {
            logger.debug("Found existing cart for customer: {} with {} items", customerId, cart.cartItems.size)
            // Ensure cart total is up-to-date with current items
            updateCartTotal(cart)
            cart
        } else {
            logger.info("No cart found for customer: {}, creating new cart", customerId)
            createCart(customerId)
        }
    }

    /**
     * Creates a new cart for a specific customer.
     *
     * @param customerId The unique identifier of the customer
     * @return The newly created cart
     */
    @Transactional
    fun createCart(customerId: String): Cart {
        logger.info("Creating new cart for customer: {}", customerId)
        val cart = Cart(customerId = customerId)
        val savedCart = cartRepository.save(cart)
        logger.info("Successfully created cart with ID: {} for customer: {}", savedCart.id, customerId)
        return savedCart
    }

    /**
     * Adds an item to a customer's cart or updates the quantity if the item already exists.
     *
     * This operation:
     * 1. Validates the request (quantity must be at least 1)
     * 2. Retrieves current book information from BookInventoryService
     * 3. Validates inventory availability
     * 4. Updates or adds the cart item with current pricing
     * 5. Recalculates cart total
     *
     * If the item already exists in the cart, the quantity is incremented and pricing is refreshed.
     *
     * @param customerId The unique identifier of the customer
     * @param request The cart item request containing book ID and quantity
     * @return The updated cart with the new/updated item
     * @throws OrderManagementServiceException if validation fails or inventory is insufficient
     */
    @Transactional
    fun addItemToCart(customerId: String, request: CartItemRequestDto): Cart {
        logger.info("Adding item to cart - customer: {}, bookId: {}, quantity: {}",
                   customerId, request.bookId, request.quantity)

        validateCartItemRequest(request)

        val book = bookClient.getBookById(request.bookId)
            ?: throw OrderManagementServiceException(OrderManagementServiceException.Operation.INVALID_CART_ITEM)

        logger.debug("Retrieved book: {} with price: {} and available quantity: {}",
                    book.id, book.price, book.quantity)

        val cart = getCartByCustomerId(customerId)
        val existingItem = cart.cartItems.find { it.bookId == request.bookId }

        // Calculate the total quantity that would be in the cart after this operation
        val totalQuantity = if (existingItem != null) {
            existingItem.quantity + request.quantity
        } else {
            request.quantity
        }

        logger.debug("Total quantity after operation would be: {} (available: {})", totalQuantity, book.quantity)

        // Validate that the total quantity doesn't exceed available stock
        if (totalQuantity > book.quantity) {
            logger.warn("Insufficient stock for book: {} - requested: {}, available: {}",
                       request.bookId, totalQuantity, book.quantity)
            throw OrderManagementServiceException(OrderManagementServiceException.Operation.INSUFFICIENT_STOCK)
        }

        // Now it's safe to modify the cart - always use book price
        if (existingItem != null) {
            logger.debug("Updating existing cart item - bookId: {}, old quantity: {}, new quantity: {}",
                        request.bookId, existingItem.quantity, totalQuantity)
            existingItem.quantity = totalQuantity
            existingItem.price = book.price  // Update to current book price
            existingItem.subtotal = book.price.multiply(BigDecimal(totalQuantity))
        } else {
            logger.debug("Adding new cart item - bookId: {}, quantity: {}, price: {}",
                        request.bookId, request.quantity, book.price)
            val newItem = CartItem(
                cart = cart,
                bookId = request.bookId,
                quantity = request.quantity,
                price = book.price,  // Get price from book service
                subtotal = book.price.multiply(BigDecimal(request.quantity))
            )
            cart.cartItems.add(newItem)
        }

        updateCartTotal(cart)
        val savedCart = cartRepository.save(cart)
        logger.info("Successfully added item to cart - customer: {}, bookId: {}, final cart total: {}",
                   customerId, request.bookId, savedCart.totalAmount)
        return savedCart
    }

    /**
     * Updates the quantity of a specific item in a customer's cart.
     *
     * This operation:
     * 1. Validates the new quantity (must be at least 1)
     * 2. Checks if the item exists in the cart
     * 3. Retrieves current book information for inventory validation
     * 4. Updates the item quantity and refreshes pricing
     * 5. Recalculates cart total
     *
     * @param customerId The unique identifier of the customer
     * @param bookId The unique identifier of the book to update
     * @param quantity The new quantity for the cart item
     * @return The updated cart
     * @throws OrderManagementServiceException if validation fails, inventory is insufficient, or item not found
     */
    @Transactional
    fun updateCartItemQuantity(customerId: String, bookId: Long, quantity: Int): Cart {
        logger.info("Updating cart item quantity - customer: {}, bookId: {}, new quantity: {}",
                   customerId, bookId, quantity)

        if (quantity < 1) {
            logger.warn("Invalid quantity requested: {} for book: {}", quantity, bookId)
            throw OrderManagementServiceException(OrderManagementServiceException.Operation.INVALID_CART_QUANTITY)
        }

        val cart = getCartByCustomerId(customerId)
        val item = cart.cartItems.find { it.bookId == bookId }
            ?: throw OrderManagementServiceException(OrderManagementServiceException.Operation.ITEM_NOT_FOUND_IN_CART)

        logger.debug("Found cart item - bookId: {}, old quantity: {}, old price: {}",
                    bookId, item.quantity, item.price)

        val book = bookClient.getBookById(bookId)
            ?: throw OrderManagementServiceException(OrderManagementServiceException.Operation.INVALID_CART_ITEM)

        logger.debug("Retrieved book: {} with available quantity: {}", book.id, book.quantity)

        // Validate that the new quantity doesn't exceed available stock
        if (quantity > book.quantity) {
            logger.warn("Insufficient stock for book: {} - requested: {}, available: {}",
                       bookId, quantity, book.quantity)
            throw OrderManagementServiceException(OrderManagementServiceException.Operation.INSUFFICIENT_STOCK)
        }

        // Update price to current book price for consistency
        item.price = book.price
        item.quantity = quantity
        item.subtotal = book.price.multiply(BigDecimal(quantity))
        updateCartTotal(cart)
        val savedCart = cartRepository.save(cart)
        logger.info("Successfully updated cart item quantity - customer: {}, bookId: {}, new total: {}",
                   customerId, bookId, savedCart.totalAmount)
        return savedCart
    }

    /**
     * Removes a specific item from a customer's cart.
     *
     * @param customerId The unique identifier of the customer
     * @param bookId The unique identifier of the book to remove
     * @return The updated cart without the specified item
     * @throws OrderManagementServiceException if the item is not found in the cart
     */
    @Transactional
    fun removeItemFromCart(customerId: String, bookId: Long): Cart {
        logger.info("Removing item from cart - customer: {}, bookId: {}", customerId, bookId)

        val cart = getCartByCustomerId(customerId)
        val removed = cart.cartItems.removeIf { it.bookId == bookId }

        if (!removed) {
            logger.warn("Item not found in cart for removal - bookId: {}", bookId)
            throw OrderManagementServiceException(OrderManagementServiceException.Operation.ITEM_NOT_FOUND_IN_CART)
        }

        logger.debug("Item removed from cart - bookId: {}", bookId)
        updateCartTotal(cart)
        val savedCart = cartRepository.save(cart)
        logger.info("Successfully removed item from cart - customer: {}, bookId: {}, new total: {}",
                   customerId, bookId, savedCart.totalAmount)
        return savedCart
    }

    /**
     * Clears all items from a customer's cart, resetting the total to zero.
     *
     * @param customerId The unique identifier of the customer
     * @return The cleared cart with no items
     */
    @Transactional
    fun clearCart(customerId: String): Cart {
        logger.info("Clearing cart for customer: {}", customerId)

        val cart = getCartByCustomerId(customerId)
        val itemCount = cart.cartItems.size
        cart.cartItems.clear()
        cart.totalAmount = BigDecimal.ZERO
        val savedCart = cartRepository.save(cart)

        logger.info("Successfully cleared cart - customer: {}, removed {} items", customerId, itemCount)
        return savedCart
    }

    /**
     * Processes the checkout of a customer's cart, creating an order and processing payment.
     *
     * This is a complex transactional operation that:
     * 1. Validates that the cart is not empty
     * 2. Validates inventory availability for all items
     * 3. Creates an order from the cart contents
     * 4. Updates book quantities in the inventory
     * 5. Processes payment (simulated)
     * 6. Updates order status based on payment result
     * 7. Clears the cart after successful processing
     *
     * If any step fails, the entire transaction is rolled back to maintain data consistency.
     *
     * @param customerId The unique identifier of the customer
     * @return The created order with final status
     * @throws OrderManagementServiceException if checkout fails (empty cart, insufficient stock, payment failure)
     */
    @Transactional
    fun checkoutCart(customerId: String): Order {
        logger.info("Starting checkout process for customer: {}", customerId)

        val cart = getCartByCustomerId(customerId)

        if (cart.cartItems.isEmpty()) {
            logger.warn("Attempted checkout with empty cart - customer: {}", customerId)
            throw OrderManagementServiceException(OrderManagementServiceException.Operation.EMPTY_CART)
        }

        logger.debug("Cart contains {} items with total amount: {}", cart.cartItems.size, cart.totalAmount)

        // Validate inventory availability and update prices
        logger.debug("Validating inventory and updating prices")
        validateAndUpdateCartPrices(cart)

        // Create order from cart
        logger.debug("Creating order from cart")
        val order = createOrderFromCart(cart)
        logger.info("Order created successfully - orderId: {}, customer: {}", order.id, customerId)

        // Update book quantities after successful order creation
        logger.debug("Updating book quantities in inventory")
        updateBookQuantities(cart)

        // Process payment
        logger.debug("Processing payment for order: {}", order.id)
        try {
            paymentService.processPayment(order.id, order.totalAmount)
            logger.info("Payment processed successfully for order: {}", order.id)

            // Update order status to CONFIRMED after successful payment
            orderService.updateOrderStatus(order.id, OrderStatus.CONFIRMED)
            logger.info("Order status updated to CONFIRMED - orderId: {}", order.id)
        } catch (e: OrderManagementServiceException) {
            logger.error("Payment failed for order: {} - {}", order.id, e.message)
            // If payment fails, restore book quantities and cancel order
            restoreBookQuantities(cart)
            orderService.updateOrderStatus(order.id, OrderStatus.CANCELLED)
            logger.info("Order cancelled and inventory restored - orderId: {}", order.id)
            throw OrderManagementServiceException(OrderManagementServiceException.Operation.PAYMENT_FAILED, e)
        } catch (e: Exception) {
            logger.error("Unexpected error during payment processing for order: {}", order.id, e)
            // Handle any other unexpected errors
            restoreBookQuantities(cart)
            orderService.updateOrderStatus(order.id, OrderStatus.CANCELLED)
            logger.info("Order cancelled and inventory restored due to unexpected error - orderId: {}", order.id)
            throw OrderManagementServiceException(OrderManagementServiceException.Operation.PAYMENT_FAILED, e)
        }

        // Clear the cart after successful order creation and payment
        logger.debug("Clearing cart after successful checkout")
        clearCart(customerId)

        val finalOrder = orderService.getOrderById(order.id)
        logger.info("Checkout completed successfully - orderId: {}, customer: {}, total: {}",
                   finalOrder.id, customerId, finalOrder.totalAmount)
        return finalOrder
    }

    private fun validateAndUpdateCartPrices(cart: Cart) {
        logger.debug("Validating and updating prices for {} cart items", cart.cartItems.size)

        cart.cartItems.forEach { cartItem ->
            val book = bookClient.getBookById(cartItem.bookId)
                ?: throw OrderManagementServiceException(OrderManagementServiceException.Operation.INVALID_CART_ITEM)

            logger.debug("Validating book: {} - cart quantity: {}, available: {}",
                        cartItem.bookId, cartItem.quantity, book.quantity)

            // Validate inventory
            if (cartItem.quantity > book.quantity) {
                logger.warn("Insufficient stock for book: {} - cart quantity: {}, available: {}",
                           cartItem.bookId, cartItem.quantity, book.quantity)
                throw OrderManagementServiceException(OrderManagementServiceException.Operation.INSUFFICIENT_STOCK)
            }

            // Update price to current book price for consistency
            if (cartItem.price != book.price) {
                logger.info("Updating price for book {} from {} to {}", cartItem.bookId, cartItem.price, book.price)
                cartItem.price = book.price
                cartItem.subtotal = book.price.multiply(BigDecimal(cartItem.quantity))
            }
        }

        // Update cart total after price changes
        updateCartTotal(cart)
        logger.debug("Cart total updated to: {}", cart.totalAmount)
    }

    private fun updateBookQuantities(cart: Cart) {
        logger.debug("Updating book quantities for {} items", cart.cartItems.size)

        cart.cartItems.forEach { cartItem ->
            val book = bookClient.getBookById(cartItem.bookId)
                ?: throw OrderManagementServiceException(OrderManagementServiceException.Operation.INVALID_CART_ITEM)

            val newQuantity = book.quantity - cartItem.quantity
            logger.debug("Updating book: {} quantity from {} to {}", cartItem.bookId, book.quantity, newQuantity)

            val success = bookClient.updateBookQuantity(cartItem.bookId, newQuantity)

            if (!success) {
                logger.error("Failed to update book quantity - bookId: {}, requested quantity: {}",
                            cartItem.bookId, newQuantity)
                throw OrderManagementServiceException(OrderManagementServiceException.Operation.INVENTORY_RESERVATION_FAILED)
            }

            logger.debug("Successfully updated book quantity - bookId: {}, new quantity: {}",
                        cartItem.bookId, newQuantity)
        }
    }

    private fun restoreBookQuantities(cart: Cart) {
        logger.warn("Restoring book quantities for {} items after payment failure", cart.cartItems.size)

        cart.cartItems.forEach { cartItem ->
            try {
                val book = bookClient.getBookById(cartItem.bookId)
                if (book != null) {
                    val restoredQuantity = book.quantity + cartItem.quantity
                    logger.debug("Restoring book: {} quantity from {} to {}",
                                cartItem.bookId, book.quantity, restoredQuantity)

                    val success = bookClient.updateBookQuantity(cartItem.bookId, restoredQuantity)
                    if (!success) {
                        logger.error("Failed to restore quantity for book {} - API call returned false", cartItem.bookId)
                    } else {
                        logger.debug("Successfully restored quantity for book: {}", cartItem.bookId)
                    }
                } else {
                    logger.error("Failed to restore quantity for book {} - book not found", cartItem.bookId)
                }
            } catch (e: Exception) {
                logger.error("Failed to restore quantity for book {}", cartItem.bookId, e)
                // Consider alerting/monitoring for this critical error
            }
        }
    }

    private fun createOrderFromCart(cart: Cart): Order {
        logger.debug("Creating order from cart - customer: {}, items: {}, total: {}",
                   cart.customerId, cart.cartItems.size, cart.totalAmount)

        val order = Order(
            customerId = cart.customerId,
            status = OrderStatus.PENDING,
            totalAmount = cart.totalAmount
        )

        // Convert cart items to order items
        cart.cartItems.forEach { cartItem ->
            val orderItem = OrderItem(
                order = order,
                bookId = cartItem.bookId,
                quantity = cartItem.quantity,
                price = cartItem.price,
                subtotal = cartItem.subtotal
            )
            order.orderItems.add(orderItem)
            logger.debug("Added order item - bookId: {}, quantity: {}, price: {}",
                        cartItem.bookId, cartItem.quantity, cartItem.price)
        }

        val createdOrder = orderService.createOrder(order)
        logger.info("Order created successfully - orderId: {}, customer: {}, total: {}",
                   createdOrder.id, cart.customerId, createdOrder.totalAmount)
        return createdOrder
    }

    private fun updateCartTotal(cart: Cart) {
        val oldTotal = cart.totalAmount
        cart.totalAmount = cart.cartItems.sumOf { it.subtotal }
        logger.debug("Updated cart total from {} to {}", oldTotal, cart.totalAmount)
    }

    private fun validateCartItemRequest(request: CartItemRequestDto) {
        logger.debug("Validating cart item request - bookId: {}, quantity: {}", request.bookId, request.quantity)

        if (request.quantity < 1) {
            logger.warn("Invalid quantity in request: {} for book: {}", request.quantity, request.bookId)
            throw OrderManagementServiceException(OrderManagementServiceException.Operation.INVALID_CART_QUANTITY)
        }

        logger.debug("Cart item request validation passed")
    }
}
