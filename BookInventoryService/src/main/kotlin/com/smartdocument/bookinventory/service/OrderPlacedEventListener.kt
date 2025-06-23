package com.smartdocument.bookinventory.service

import com.smartdocument.bookinventory.event.OrderPlacedEvent
import com.smartdocument.bookinventory.config.RabbitMQConfig
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory

@Service
class OrderPlacedEventListener(
    private val bookService: BookService
) {
    private val logger = LoggerFactory.getLogger(OrderPlacedEventListener::class.java)

    @RabbitListener(queues = [RabbitMQConfig.ORDER_PLACED_QUEUE])
    fun handleOrderPlacedEvent(event: OrderPlacedEvent) {
        logger.info("Received OrderPlacedEvent for orderId: {} with {} items", event.orderId, event.items.size)
        event.items.forEach { item ->
            try {
                logger.info("Updating inventory for bookId: {} by quantity: {}", item.bookId, item.quantity)
                bookService.decrementBookQuantity(item.bookId.toLong(), item.quantity)
            } catch (ex: Exception) {
                logger.error("Failed to update inventory for bookId: {}: {}", item.bookId, ex.message)
            }
        }
    }
}
