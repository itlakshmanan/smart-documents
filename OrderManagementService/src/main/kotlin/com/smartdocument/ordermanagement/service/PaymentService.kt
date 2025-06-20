package com.smartdocument.ordermanagement.service

import com.smartdocument.ordermanagement.exception.OrderManagementServiceException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.*

@Service
class PaymentService {

    fun processPayment(orderId: Long, amount: BigDecimal): Boolean {
        // Simulate payment processing
        // In a real application, this would integrate with a payment gateway

        // Simulate 95% success rate
        val random = Random()
        val success = random.nextDouble() < 0.95

        if (!success) {
            throw OrderManagementServiceException(OrderManagementServiceException.Operation.PAYMENT_FAILED)
        }

        return true
    }

    fun refundPayment(orderId: Long, amount: BigDecimal): Boolean {
        // Simulate refund processing
        // In a real application, this would integrate with a payment gateway

        // Simulate 98% success rate for refunds
        val random = Random()
        val success = random.nextDouble() < 0.98

        if (!success) {
            throw OrderManagementServiceException(OrderManagementServiceException.Operation.PAYMENT_FAILED)
        }

        return true
    }
}
