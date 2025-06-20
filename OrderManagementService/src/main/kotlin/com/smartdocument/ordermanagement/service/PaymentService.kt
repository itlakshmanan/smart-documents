package com.smartdocument.ordermanagement.service

import com.smartdocument.ordermanagement.exception.OrderManagementServiceException
import org.springframework.stereotype.Service
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.util.*

@Service
class PaymentService {

    private val logger: Logger = LoggerFactory.getLogger(PaymentService::class.java)

    fun processPayment(orderId: Long, amount: BigDecimal): Boolean {
        logger.info("Processing payment for order: {} with amount: {}", orderId, amount)

        // Simulate payment processing
        // In a real application, this would integrate with a payment gateway

        // Simulate 95% success rate
        val random = Random()
        val success = random.nextDouble() < 0.95

        if (!success) {
            logger.error("Payment failed for order: {} with amount: {}", orderId, amount)
            throw OrderManagementServiceException(OrderManagementServiceException.Operation.PAYMENT_FAILED)
        }

        logger.info("Payment processed successfully for order: {} with amount: {}", orderId, amount)
        return true
    }

    fun refundPayment(orderId: Long, amount: BigDecimal): Boolean {
        logger.info("Processing refund for order: {} with amount: {}", orderId, amount)

        // Simulate refund processing
        // In a real application, this would integrate with a payment gateway

        // Simulate 98% success rate for refunds
        val random = Random()
        val success = random.nextDouble() < 0.98

        if (!success) {
            logger.error("Refund failed for order: {} with amount: {}", orderId, amount)
            throw OrderManagementServiceException(OrderManagementServiceException.Operation.PAYMENT_FAILED)
        }

        logger.info("Refund processed successfully for order: {} with amount: {}", orderId, amount)
        return true
    }
}
