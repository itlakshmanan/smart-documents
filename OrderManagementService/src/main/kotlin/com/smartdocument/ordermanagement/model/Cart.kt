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
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as Cart
        if (id != 0L && id == other.id) return true
        return customerId == other.customerId
    }

    override fun hashCode(): Int {
        return if (id != 0L) id.hashCode() else customerId.hashCode()
    }
}
