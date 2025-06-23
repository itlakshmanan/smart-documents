package com.smartdocument.bookinventory.service

import com.smartdocument.bookinventory.event.OrderCancelledEvent
import com.smartdocument.bookinventory.config.RabbitMQConfig
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory

@Service
class OrderCancelledEventListener(
    private val bookService: BookService
) {
    private val logger = LoggerFactory.getLogger(OrderCancelledEventListener::class.java)

    @RabbitListener(queues = [RabbitMQConfig.ORDER_CANCELLED_QUEUE])
    fun handleOrderCancelledEvent(event: OrderCancelledEvent) {
        try {
            logger.info("[OrderCancelledEvent] Listener triggered for orderId={}, items={}", event.orderId, event.items)
            event.items.forEach { item ->
                try {
                    logger.info("Restoring inventory for bookId: {} (type: {}) by quantity: {}", item.bookId, item.bookId::class.simpleName, item.quantity)
                    bookService.incrementBookQuantity(item.bookId.toLong(), item.quantity)
                } catch (ex: Exception) {
                    logger.error("Failed to restore inventory for bookId: {}: {}", item.bookId, ex.message, ex)
                }
            }
        } catch (ex: Exception) {
            logger.error("[OrderCancelledEvent] Listener error: {}", ex.message, ex)
        }
    }
}
