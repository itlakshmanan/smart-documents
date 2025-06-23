package com.smartdocument.bookinventory.event

data class OrderCancelledEvent(
    val orderId: String,
    val items: List<Item>
) {
    data class Item(
        val bookId: String,
        val quantity: Int
    )
}
