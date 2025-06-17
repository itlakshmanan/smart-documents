package com.smartdocument.ordermanagement.repository

import com.smartdocument.ordermanagement.model.Cart
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CartRepository : JpaRepository<Cart, Long> {
    fun findByCustomerId(customerId: String): Cart?
} 