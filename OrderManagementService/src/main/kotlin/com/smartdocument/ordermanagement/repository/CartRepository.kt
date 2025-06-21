package com.smartdocument.ordermanagement.repository

import com.smartdocument.ordermanagement.model.Cart
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Repository interface for Cart entity persistence operations.
 *
 * This repository extends JpaRepository to provide standard CRUD operations
 * for Cart entities. It also includes custom query methods for finding
 * carts by customer ID.
 *
 * The repository provides:
 * - Standard CRUD operations (Create, Read, Update, Delete)
 * - Custom query methods for business-specific operations
 * - Automatic query generation based on method names
 * - Transaction management through Spring Data JPA
 *
 * All operations are automatically wrapped in transactions and
 * the repository is managed by Spring's dependency injection container.
 *
 * @property Cart The entity type managed by this repository
 * @property Long The type of the entity's primary key
 */
@Repository
interface CartRepository : JpaRepository<Cart, Long> {

    /**
     * Finds a cart by customer ID.
     *
     * Retrieves the cart associated with the specified customer ID.
     * Since each customer can have only one cart, this method returns
     * a single Cart entity or null if no cart exists for the customer.
     *
     * The cart is automatically created when the first item is added,
     * so this method is typically used to check if a cart exists or
     * to retrieve the current cart contents.
     *
     * @param customerId Unique identifier for the customer
     * @return Cart entity if found, null otherwise
     */
    fun findByCustomerId(customerId: String): Cart?
}
