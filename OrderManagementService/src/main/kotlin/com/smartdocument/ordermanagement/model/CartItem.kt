package com.smartdocument.ordermanagement.model

import jakarta.persistence.*
import java.math.BigDecimal

/**
 * Entity representing an item in a shopping cart.
 *
 * A cart item represents a book that a customer has added to their cart with:
 * - Reference to the parent cart
 * - Book identification
 * - Quantity requested
 * - Current price at time of addition
 * - Calculated subtotal
 *
 * The cart item is used to:
 * - Track which books are in a customer's cart
 * - Maintain pricing information (price may change over time)
 * - Calculate individual item costs
 * - Validate inventory availability
 *
 * Business rules:
 * - Quantity must be at least 1
 * - Price is captured at time of addition to cart
 * - Subtotal is calculated as price × quantity
 * - Book ID must reference a valid book in the inventory
 *
 * @property id Unique identifier for the cart item (auto-generated)
 * @property cart The parent cart that contains this item
 * @property bookId Unique identifier of the book in this cart item
 * @property quantity Number of copies of the book requested
 * @property price Price per unit of the book at time of cart addition
 * @property subtotal Total cost for this item (price × quantity)
 */
@Entity
@Table(name = "cart_items")
data class CartItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    var cart: Cart,

    @Column(nullable = false)
    var bookId: Long,

    @Column(nullable = false)
    var quantity: Int,

    @Column(nullable = false)
    var price: BigDecimal,

    @Column(nullable = false)
    var subtotal: BigDecimal
) {
    /**
     * Custom equals implementation for CartItem entities.
     *
     * Two cart items are considered equal if:
     * - They are the same object reference, OR
     * - They have the same non-zero ID, OR
     * - They represent the same book in the same cart (for new items without ID)
     *
     * This implementation ensures that:
     * - New cart items can be compared by book ID and cart
     * - Persisted cart items are compared by ID
     * - JPA entity equality works correctly
     *
     * @param other The object to compare with
     * @return true if the cart items are equal, false otherwise
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as CartItem
        if (id != 0L && id == other.id) return true
        return bookId == other.bookId && cart.id == other.cart.id
    }

    /**
     * Custom hashCode implementation for CartItem entities.
     *
     * The hash code is based on:
     * - The cart item ID if it's a persisted entity (ID != 0)
     * - A combination of book ID and cart ID if it's a new entity (ID == 0)
     *
     * This ensures consistent hashing behavior with the equals method.
     *
     * @return Hash code for this cart item
     */
    override fun hashCode(): Int {
        return if (id != 0L) id.hashCode() else 31 * bookId.hashCode() + cart.id.hashCode()
    }
}
