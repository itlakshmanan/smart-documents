package com.smartdocument.ordermanagement.model

import jakarta.persistence.*
import java.math.BigDecimal

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
) 