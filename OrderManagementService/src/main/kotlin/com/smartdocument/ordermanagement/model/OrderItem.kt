package com.smartdocument.ordermanagement.model

import jakarta.persistence.*
import java.math.BigDecimal

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