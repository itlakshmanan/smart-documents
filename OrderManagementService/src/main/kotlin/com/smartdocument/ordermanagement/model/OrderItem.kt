package com.smartdocument.ordermanagement.model

import jakarta.persistence.*
import java.math.BigDecimal

/**
 * Entity representing an item in an order.
 *
 * An order item represents a book that was purchased as part of an order with:
 * - Reference to the parent order
 * - Book identification
 * - Quantity purchased
 * - Price at time of order (immutable after order creation)
 * - Calculated subtotal
 *
 * The order item is used to:
 * - Track which books were purchased in an order
 * - Maintain historical pricing information
 * - Calculate order totals
 * - Provide order history and receipts
 *
 * Business rules:
 * - Quantity must be at least 1
 * - Price is captured at time of order creation and remains fixed
 * - Subtotal is calculated as price × quantity
 * - Book ID must reference a valid book in the inventory
 * - Order items are immutable after order creation
 *
 * @property id Unique identifier for the order item (auto-generated)
 * @property order The parent order that contains this item
 * @property bookId Unique identifier of the book in this order item
 * @property quantity Number of copies of the book purchased
 * @property price Price per unit of the book at time of order creation
 * @property subtotal Total cost for this item (price × quantity)
 */
@Entity
@Table(name = "order_items")
data class OrderItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    var order: Order,

    @Column(nullable = false)
    var bookId: Long,

    @Column(nullable = false)
    var quantity: Int,

    @Column(nullable = false)
    var price: BigDecimal,

    @Column(nullable = false)
    var subtotal: BigDecimal
)
