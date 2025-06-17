package com.smartdocument.ordermanagement.repository

import com.smartdocument.ordermanagement.model.Order
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderRepository : JpaRepository<Order, Long> {
    fun findByCustomerId(customerId: String): List<Order>
    fun findByCustomerIdAndStatus(customerId: String, status: OrderStatus): List<Order>
} 