package com.smartdocument.ordermanagement.event

data class OrderPlacedEvent(
    val orderId: String,
    val items: List<Item>
) {
    data class Item(
        val bookId: String,
        val quantity: Int
    )
}
