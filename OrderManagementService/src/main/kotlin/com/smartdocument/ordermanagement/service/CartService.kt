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

@Service
class CartService(private val cartRepository: CartRepository) {

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
        val cart = getCartByCustomerId(customerId)
        val existingItem = cart.cartItems.find { it.bookId == request.bookId }

        if (existingItem != null) {
            existingItem.quantity += request.quantity!!
            existingItem.subtotal = existingItem.price.multiply(BigDecimal(existingItem.quantity))
        } else {
            val newItem = CartItem(
                cart = cart,
                bookId = request.bookId!!,
                quantity = request.quantity!!,
                price = request.price!!,
                subtotal = request.price.multiply(BigDecimal(request.quantity!!))
            )
            cart.cartItems.add(newItem)
        }

        updateCartTotal(cart)
        return cartRepository.save(cart)
    }

    @Transactional
    fun updateCartItemQuantity(customerId: String, bookId: Long, quantity: Int): Cart {
        if (quantity < 1) throw OrderManagementServiceException(OrderManagementServiceException.Operation.INVALID_CART_QUANTITY)
        val cart = getCartByCustomerId(customerId)
        val item = cart.cartItems.find { it.bookId == bookId }
            ?: throw OrderManagementServiceException(OrderManagementServiceException.Operation.ITEM_NOT_FOUND_IN_CART)

        item.quantity = quantity
        item.subtotal = item.price.multiply(BigDecimal(quantity))

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

    private fun updateCartTotal(cart: Cart) {
        cart.totalAmount = cart.cartItems.sumOf { it.subtotal }
    }

    fun toCartResponseDto(cart: Cart): CartResponseDto = CartResponseDto(
        customerId = cart.customerId,
        totalAmount = cart.totalAmount,
        items = cart.cartItems.map { toCartItemResponseDto(it) }
    )

    fun toCartItemResponseDto(item: CartItem): CartItemResponseDto = CartItemResponseDto(
        bookId = item.bookId,
        quantity = item.quantity,
        price = item.price,
        subtotal = item.subtotal
    )

    private fun validateCartItemRequest(request: CartItemRequestDto) {
        if (request.bookId == null) throw OrderManagementServiceException(OrderManagementServiceException.Operation.INVALID_CART_ITEM)
        if (request.quantity == null || request.quantity < 1) throw OrderManagementServiceException(OrderManagementServiceException.Operation.INVALID_CART_QUANTITY)
        if (request.price == null || request.price <= BigDecimal.ZERO) throw OrderManagementServiceException(OrderManagementServiceException.Operation.INVALID_CART_PRICE)
    }
}
