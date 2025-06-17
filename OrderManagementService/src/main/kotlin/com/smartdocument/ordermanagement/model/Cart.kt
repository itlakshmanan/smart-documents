package com.smartdocument.ordermanagement.model

import jakarta.persistence.*
import java.math.BigDecimal

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
) 