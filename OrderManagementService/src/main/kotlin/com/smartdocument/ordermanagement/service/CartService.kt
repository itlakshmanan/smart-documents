package com.smartdocument.ordermanagement.service

import com.smartdocument.ordermanagement.model.*
import com.smartdocument.ordermanagement.repository.CartRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

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
    fun addItemToCart(customerId: String, bookId: Long, quantity: Int, price: BigDecimal): Cart {
        val cart = getCartByCustomerId(customerId)
        val existingItem = cart.cartItems.find { it.bookId == bookId }

        if (existingItem != null) {
            existingItem.quantity += quantity
            existingItem.subtotal = existingItem.price.multiply(BigDecimal(existingItem.quantity))
        } else {
            val newItem = CartItem(
                cart = cart,
                bookId = bookId,
                quantity = quantity,
                price = price,
                subtotal = price.multiply(BigDecimal(quantity))
            )
            cart.cartItems.add(newItem)
        }

        updateCartTotal(cart)
        return cartRepository.save(cart)
    }

    @Transactional
    fun updateCartItemQuantity(customerId: String, bookId: Long, quantity: Int): Cart {
        val cart = getCartByCustomerId(customerId)
        val item = cart.cartItems.find { it.bookId == bookId }
            ?: throw NoSuchElementException("Item not found in cart")

        if (quantity <= 0) {
            cart.cartItems.remove(item)
        } else {
            item.quantity = quantity
            item.subtotal = item.price.multiply(BigDecimal(quantity))
        }

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
} 