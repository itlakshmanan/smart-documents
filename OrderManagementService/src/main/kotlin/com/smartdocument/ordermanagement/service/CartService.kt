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

@Service
class CartService(
    private val cartRepository: CartRepository,
    private val bookClient: BookClient,
    private val orderService: OrderService,
    private val paymentService: PaymentService
) {

    private val logger: Logger = LoggerFactory.getLogger(CartService::class.java)

    fun getCartByCustomerId(customerId: String): Cart = cartRepository.findByCustomerId(customerId)
        ?: createCart(customerId)

    @Transactional
    fun createCart(customerId: String): Cart {
        val cart = Cart(customerId = customerId)
        return cartRepository.save(cart)
    }

    @Transactional
    fun addItemToCart(customerId: String, request: CartItemRequestDto): Cart {
        validateCartItemRequest(request)

        val book = bookClient.getBookById(request.bookId)
            ?: throw OrderManagementServiceException(OrderManagementServiceException.Operation.INVALID_CART_ITEM)

        val cart = getCartByCustomerId(customerId)
        val existingItem = cart.cartItems.find { it.bookId == request.bookId }

        // Calculate the total quantity that would be in the cart after this operation
        val totalQuantity = if (existingItem != null) {
            existingItem.quantity + request.quantity
        } else {
            request.quantity
        }

        // Validate that the total quantity doesn't exceed available stock
        if (totalQuantity > book.quantity) {
            throw OrderManagementServiceException(OrderManagementServiceException.Operation.INSUFFICIENT_STOCK)
        }

        // Now it's safe to modify the cart - always use book price
        if (existingItem != null) {
            existingItem.quantity = totalQuantity
            existingItem.price = book.price  // Update to current book price
            existingItem.subtotal = book.price.multiply(BigDecimal(totalQuantity))
        } else {
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
        return cartRepository.save(cart)
    }

    @Transactional
    fun updateCartItemQuantity(customerId: String, bookId: Long, quantity: Int): Cart {
        if (quantity < 1) throw OrderManagementServiceException(OrderManagementServiceException.Operation.INVALID_CART_QUANTITY)

        val book = bookClient.getBookById(bookId)
            ?: throw OrderManagementServiceException(OrderManagementServiceException.Operation.INVALID_CART_ITEM)

        // Validate that the new quantity doesn't exceed available stock
        if (quantity > book.quantity) {
            throw OrderManagementServiceException(OrderManagementServiceException.Operation.INSUFFICIENT_STOCK)
        }

        val cart = getCartByCustomerId(customerId)
        val item = cart.cartItems.find { it.bookId == bookId }
            ?: throw OrderManagementServiceException(OrderManagementServiceException.Operation.ITEM_NOT_FOUND_IN_CART)

        // Update price to current book price for consistency
        item.price = book.price
        item.quantity = quantity
        item.subtotal = book.price.multiply(BigDecimal(quantity))
        updateCartTotal(cart)
        return cartRepository.save(cart)
    }

    @Transactional
    fun removeItemFromCart(customerId: String, bookId: Long): Cart {
        val cart = getCartByCustomerId(customerId)
        cart.cartItems.removeIf { it.bookId == bookId }
        updateCartTotal(cart)
        return cartRepository.save(cart)
    }

    @Transactional
    fun clearCart(customerId: String): Cart {
        val cart = getCartByCustomerId(customerId)
        cart.cartItems.clear()
        cart.totalAmount = BigDecimal.ZERO
        return cartRepository.save(cart)
    }

    @Transactional
    fun checkoutCart(customerId: String): Order {
        val cart = getCartByCustomerId(customerId)

        if (cart.cartItems.isEmpty()) {
            throw OrderManagementServiceException(OrderManagementServiceException.Operation.EMPTY_CART)
        }

        // Validate inventory availability and update prices
        validateAndUpdateCartPrices(cart)

        // Create order from cart
        val order = createOrderFromCart(cart)

        // Update book quantities after successful order creation
        updateBookQuantities(cart)

        // Process payment
        try {
            paymentService.processPayment(order.id, order.totalAmount)
            // Update order status to CONFIRMED after successful payment
            orderService.updateOrderStatus(order.id, OrderStatus.CONFIRMED)
        } catch (e: OrderManagementServiceException) {
            // If payment fails, restore book quantities and cancel order
            restoreBookQuantities(cart)
            orderService.updateOrderStatus(order.id, OrderStatus.CANCELLED)
            throw OrderManagementServiceException(OrderManagementServiceException.Operation.PAYMENT_FAILED, e)
        } catch (e: Exception) {
            // Handle any other unexpected errors
            restoreBookQuantities(cart)
            orderService.updateOrderStatus(order.id, OrderStatus.CANCELLED)
            throw OrderManagementServiceException(OrderManagementServiceException.Operation.PAYMENT_FAILED, e)
        }

        // Clear the cart after successful order creation and payment
        clearCart(customerId)

        return orderService.getOrderById(order.id)
    }

    private fun validateAndUpdateCartPrices(cart: Cart) {
        cart.cartItems.forEach { cartItem ->
            val book = bookClient.getBookById(cartItem.bookId)
                ?: throw OrderManagementServiceException(OrderManagementServiceException.Operation.INVALID_CART_ITEM)

            // Validate inventory
            if (cartItem.quantity > book.quantity) {
                throw OrderManagementServiceException(OrderManagementServiceException.Operation.INSUFFICIENT_STOCK)
            }

            // Update price to current book price for consistency
            if (cartItem.price != book.price) {
                logger.info("Updating price for book ${cartItem.bookId} from ${cartItem.price} to ${book.price}")
                cartItem.price = book.price
                cartItem.subtotal = book.price.multiply(BigDecimal(cartItem.quantity))
            }
        }

        // Update cart total after price changes
        updateCartTotal(cart)
    }

    private fun updateBookQuantities(cart: Cart) {
        cart.cartItems.forEach { cartItem ->
            val book = bookClient.getBookById(cartItem.bookId)
                ?: throw OrderManagementServiceException(OrderManagementServiceException.Operation.INVALID_CART_ITEM)

            val newQuantity = book.quantity - cartItem.quantity
            val success = bookClient.updateBookQuantity(cartItem.bookId, newQuantity)

            if (!success) {
                throw OrderManagementServiceException(OrderManagementServiceException.Operation.INVENTORY_RESERVATION_FAILED)
            }
        }
    }

    private fun restoreBookQuantities(cart: Cart) {
        cart.cartItems.forEach { cartItem ->
            try {
                val book = bookClient.getBookById(cartItem.bookId)
                if (book != null) {
                    val restoredQuantity = book.quantity + cartItem.quantity
                    val success = bookClient.updateBookQuantity(cartItem.bookId, restoredQuantity)
                    if (!success) {
                        logger.error("Failed to restore quantity for book ${cartItem.bookId} - API call returned false")
                    }
                } else {
                    logger.error("Failed to restore quantity for book ${cartItem.bookId} - book not found")
                }
            } catch (e: Exception) {
                logger.error("Failed to restore quantity for book ${cartItem.bookId}", e)
                // Consider alerting/monitoring for this critical error
            }
        }
    }

    private fun createOrderFromCart(cart: Cart): Order {
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
        }

        return orderService.createOrder(order)
    }

    private fun updateCartTotal(cart: Cart) {
        cart.totalAmount = cart.cartItems.sumOf { it.subtotal }
    }

    private fun validateCartItemRequest(request: CartItemRequestDto) {
        if (request.quantity < 1)
            throw OrderManagementServiceException(OrderManagementServiceException.Operation.INVALID_CART_QUANTITY)
    }
}
