package com.smartdocument.ordermanagement.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Entity representing an order in the Order Management Service.
 *
 * An order represents a customer's purchase transaction and contains:
 * - Customer identification
 * - Order status tracking
 * - Total amount calculation
 * - List of order items
 * - Creation and update timestamps
 *
 * The order lifecycle follows these statuses:
 * 1. PENDING - Order created, awaiting payment
 * 2. CONFIRMED - Payment successful, order confirmed
 * 3. SHIPPED - Order has been shipped to customer
 * 4. DELIVERED - Order has been delivered to customer
 * 5. CANCELLED - Order has been cancelled
 *
 * @property id Unique identifier for the order (auto-generated)
 * @property customerId Unique identifier for the customer who placed the order
 * @property status Current status of the order in the lifecycle
 * @property totalAmount Total monetary amount of the order
 * @property orderItems List of items included in this order
 * @property createdAt Timestamp when the order was created
 * @property updatedAt Timestamp when the order was last updated
 */
@Entity
@Table(name = "orders")
data class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var customerId: String,

    @Column(nullable = false)
    var status: OrderStatus,

    @Column(nullable = false)
    var totalAmount: BigDecimal,

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    var orderItems: MutableList<OrderItem> = mutableListOf(),

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column
    var updatedAt: LocalDateTime? = null
)

/**
 * Enumeration representing the possible states of an order in the system.
 *
 * The order status follows a specific lifecycle that tracks the progress
 * of an order from creation to completion or cancellation.
 *
 * Status transitions:
 * - PENDING → CONFIRMED (after successful payment)
 * - CONFIRMED → SHIPPED (when order is shipped)
 * - SHIPPED → DELIVERED (when order is delivered)
 * - Any status → CANCELLED (if order is cancelled)
 *
 * Business rules:
 * - DELIVERED orders cannot be cancelled
 * - CANCELLED orders cannot be reactivated
 * - Status transitions are validated in the service layer
 */
enum class OrderStatus {
    /** Order has been created and is awaiting payment confirmation */
    PENDING,

    /** Payment has been processed successfully and order is confirmed */
    CONFIRMED,

    /** Order has been shipped to the customer */
    SHIPPED,

    /** Order has been successfully delivered to the customer */
    DELIVERED,

    /** Order has been cancelled (cannot be reactivated) */
    CANCELLED
}
