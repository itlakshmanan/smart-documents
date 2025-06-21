package com.smartdocument.ordermanagement.model

import jakarta.persistence.*
import java.math.BigDecimal

/**
 * Entity representing a shopping cart in the Order Management Service.
 *
 * A cart represents a customer's temporary shopping basket that contains:
 * - Customer identification
 * - List of cart items (books with quantities)
 * - Total amount calculation
 *
 * The cart is used to:
 * - Store items before checkout
 * - Calculate total costs
 * - Validate inventory availability
 * - Convert to orders during checkout
 *
 * Business rules:
 * - Each customer can have only one active cart
 * - Cart items are validated against inventory
 * - Cart is cleared after successful checkout
 * - Cart can be cleared manually by customer
 *
 * @property id Unique identifier for the cart (auto-generated)
 * @property customerId Unique identifier for the customer who owns this cart
 * @property cartItems List of items in the cart with quantities and pricing
 * @property totalAmount Total monetary amount of all items in the cart
 */
@Entity
@Table(name = "carts")
data class Cart(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var customerId: String,

    @OneToMany(mappedBy = "cart", cascade = [CascadeType.ALL], orphanRemoval = true)
    var cartItems: MutableList<CartItem> = mutableListOf(),

    @Column(nullable = false)
    var totalAmount: BigDecimal = BigDecimal.ZERO
) {
    /**
     * Custom equals implementation for Cart entities.
     *
     * Two carts are considered equal if:
     * - They are the same object reference, OR
     * - They have the same non-zero ID, OR
     * - They belong to the same customer (for new carts without ID)
     *
     * This implementation ensures that:
     * - New carts can be compared by customer ID
     * - Persisted carts are compared by ID
     * - JPA entity equality works correctly
     *
     * @param other The object to compare with
     * @return true if the carts are equal, false otherwise
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as Cart
        if (id != 0L && id == other.id) return true
        return customerId == other.customerId
    }

    /**
     * Custom hashCode implementation for Cart entities.
     *
     * The hash code is based on:
     * - The cart ID if it's a persisted entity (ID != 0)
     * - The customer ID if it's a new entity (ID == 0)
     *
     * This ensures consistent hashing behavior with the equals method.
     *
     * @return Hash code for this cart
     */
    override fun hashCode(): Int {
        return if (id != 0L) id.hashCode() else customerId.hashCode()
    }
}
