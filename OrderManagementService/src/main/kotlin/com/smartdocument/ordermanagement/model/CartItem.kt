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
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as CartItem
        if (id != 0L && id == other.id) return true
        return bookId == other.bookId && cart.id == other.cart.id
    }

    override fun hashCode(): Int {
        return if (id != 0L) id.hashCode() else 31 * bookId.hashCode() + cart.id.hashCode()
    }
}
